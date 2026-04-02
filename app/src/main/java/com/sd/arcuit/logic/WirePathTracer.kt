package com.sd.arcuit.logic

import android.graphics.Bitmap
import android.graphics.RectF
import org.opencv.android.Utils
import org.opencv.core.*
import org.opencv.imgproc.Imgproc
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sqrt

object WirePathTracer {

    data class Endpoint(
        val id: Int,
        val box: RectF
    ) {
        fun center(): Point {
            return Point(
                ((box.left + box.right) / 2f).toDouble(),
                ((box.top + box.bottom) / 2f).toDouble()
            )
        }
    }

    data class TraceResult(
        val endpointAId: Int,
        val endpointBId: Int,
        val connected: Boolean,
        val confidence: Float,
        val componentArea: Int = 0,
        val colorName: String = "shape"
    )

    fun findConnectedEndpointPairs(
        bitmap: Bitmap,
        endpoints: List<Endpoint>,
        padding: Int = 45,
        minComponentArea: Int = 40
    ): List<TraceResult> {
        if (endpoints.size < 2) return emptyList()

        val allResults = mutableListOf<TraceResult>()

        val src = Mat()
        Utils.bitmapToMat(bitmap, src)

        val bgr = Mat()
        Imgproc.cvtColor(src, bgr, Imgproc.COLOR_RGBA2BGR)

        try {
            for (i in endpoints.indices) {
                for (j in i + 1 until endpoints.size) {
                    val dx = endpoints[i].center().x - endpoints[j].center().x
                    val dy = endpoints[i].center().y - endpoints[j].center().y
                    val dist = sqrt(dx * dx + dy * dy)

                    if (dist > 260.0) continue

                    val result = areEndpointsConnectedByVisibleWire(
                        bgr = bgr,
                        endpointA = endpoints[i],
                        endpointB = endpoints[j],
                        padding = padding,
                        minComponentArea = minComponentArea
                    )

                    if (result.connected) {
                        allResults.add(result)
                    }
                }
            }
        } finally {
            bgr.release()
            src.release()
        }

        if (allResults.isEmpty()) return emptyList()

        return allResults
    }

    private fun areEndpointsConnectedByVisibleWire(
        bgr: Mat,
        endpointA: Endpoint,
        endpointB: Endpoint,
        padding: Int = 45,
        minComponentArea: Int = 40
    ): TraceResult {

        val roiRect = buildRoi(
            imageWidth = bgr.width(),
            imageHeight = bgr.height(),
            a = endpointA.box,
            b = endpointB.box,
            padding = padding
        )

        if (roiRect.width <= 0 || roiRect.height <= 0) {
            return TraceResult(endpointA.id, endpointB.id, false, 0f)
        }

        val roi = Mat(bgr, roiRect)

        val localA = Point(
            endpointA.center().x - roiRect.x,
            endpointA.center().y - roiRect.y
        )
        val localB = Point(
            endpointB.center().x - roiRect.x,
            endpointB.center().y - roiRect.y
        )

        val dx = abs(localA.x - localB.x)
        val dy = abs(localA.y - localB.y)
        val distance = sqrt(dx * dx + dy * dy)

        // reject tiny noise
        if (distance < 18.0) {
            roi.release()
            return TraceResult(endpointA.id, endpointB.id, false, 0f)
        }

        // =========================
        // 🔥 STRICT BREADBOARD GEOMETRY
        // =========================

        val isVertical = dx < 55.0 && dy > 18.0
        val isHorizontal = dy < 55.0 && dx > 18.0
        val isDiagonal = dx > 18.0 && dy > 18.0 && dx < 120.0 && dy < 160.0

        if (!isVertical && !isHorizontal && !isDiagonal) {
            roi.release()
            return TraceResult(endpointA.id, endpointB.id, false, 0f)
        }

        // =========================
        // 🔥 MAX DISTANCE FILTER
        // =========================

        if (isVertical && dy > 200.0) {
            roi.release()
            return TraceResult(endpointA.id, endpointB.id, false, 0f)
        }

        if (isHorizontal && dx > 450.0) {
            roi.release()
            return TraceResult(endpointA.id, endpointB.id, false, 0f)
        }

        // =========================
        // 🔥 SHAPE-BASED MASK (NO COLOR)
        // =========================

        val shapeMask = buildShapeMask(roi)

        val shapeArea = connectedAreaContainingBothPoints(
            shapeMask,
            localA,
            localB
        )

        // =========================
        // 🔥 MAIN CONNECTION CHECK
        // =========================

        val connected =
            shapeArea in minComponentArea..6000 &&
                    isPathContinuousInCorridor(
                        mask = shapeMask,
                        pointA = localA,
                        pointB = localB,
                        vertical = isVertical || isDiagonal
                    )

        val result = if (connected) {
            TraceResult(
                endpointAId = endpointA.id,
                endpointBId = endpointB.id,
                connected = true,
                confidence = confidenceFromArea(shapeArea),
                componentArea = shapeArea,
                colorName = "shape"
            )
        } else {
            TraceResult(endpointA.id, endpointB.id, false, 0f)
        }

        shapeMask.release()
        roi.release()

        return result
    }

    private fun buildShapeMask(bgr: Mat): Mat {

        val gray = Mat()
        Imgproc.cvtColor(bgr, gray, Imgproc.COLOR_BGR2GRAY)

        val blurred = Mat()
        Imgproc.GaussianBlur(gray, blurred, Size(5.0, 5.0), 0.0)

        val binary = Mat()
        Imgproc.adaptiveThreshold(
            blurred,
            binary,
            255.0,
            Imgproc.ADAPTIVE_THRESH_MEAN_C,
            Imgproc.THRESH_BINARY_INV,
            15,
            5.0
        )

        val kernelOpen = Imgproc.getStructuringElement(
            Imgproc.MORPH_RECT,
            Size(3.0, 3.0)
        )
        val opened = Mat()
        Imgproc.morphologyEx(binary, opened, Imgproc.MORPH_OPEN, kernelOpen)

        val kernelClose = Imgproc.getStructuringElement(
            Imgproc.MORPH_RECT,
            Size(5.0, 5.0)
        )
        val closed = Mat()
        Imgproc.morphologyEx(opened, closed, Imgproc.MORPH_CLOSE, kernelClose)

        gray.release()
        blurred.release()
        binary.release()
        opened.release()
        kernelOpen.release()
        kernelClose.release()

        return closed
    }

    private fun connectedAreaContainingBothPoints(
        binaryMask: Mat,
        pointA: Point,
        pointB: Point
    ): Int {
        val radius = 10

        fun isNearMask(point: Point): Boolean {
            val x0 = point.x.toInt()
            val y0 = point.y.toInt()

            for (dx in -radius..radius) {
                for (dy in -radius..radius) {
                    val x = x0 + dx
                    val y = y0 + dy

                    if (x in 0 until binaryMask.cols() && y in 0 until binaryMask.rows()) {
                        val value = binaryMask.get(y, x)?.getOrNull(0)?.toInt() ?: 0
                        if (value > 0) return true
                    }
                }
            }
            return false
        }

        if (!isNearMask(pointA) || !isNearMask(pointB)) return 0

        val labels = Mat()
        val stats = Mat()
        val centroids = Mat()

        Imgproc.connectedComponentsWithStats(
            binaryMask,
            labels,
            stats,
            centroids,
            8,
            CvType.CV_32S
        )

        val labelA = labels.get(pointA.y.toInt(), pointA.x.toInt())?.getOrNull(0)?.toInt() ?: 0
        val labelB = labels.get(pointB.y.toInt(), pointB.x.toInt())?.getOrNull(0)?.toInt() ?: 0

        if (labelA <= 0 || labelA != labelB) {
            labels.release()
            stats.release()
            centroids.release()
            return 0
        }

        val area = stats.get(labelA, Imgproc.CC_STAT_AREA)?.getOrNull(0)?.toInt() ?: 0

        labels.release()
        stats.release()
        centroids.release()

        return area
    }

    private fun isPathContinuousInCorridor(
        mask: Mat,
        pointA: Point,
        pointB: Point,
        vertical: Boolean
    ): Boolean {

        val steps = 24
        var hits = 0

        val dxTotal = pointB.x - pointA.x
        val dyTotal = pointB.y - pointA.y

        val isDiagonal = abs(dxTotal) > 15 && abs(dyTotal) > 15

        for (i in 0..steps) {
            val t = i.toDouble() / steps
            val x = (pointA.x * (1.0 - t) + pointB.x * t).toInt()
            val y = (pointA.y * (1.0 - t) + pointB.y * t).toInt()

            var found = false

            if (isDiagonal) {
                // 🔥 NEW: diagonal corridor (square search)
                for (dx in -4..4) {
                    for (dy in -4..4) {
                        val xx = x + dx
                        val yy = y + dy
                        if (xx in 0 until mask.cols() && yy in 0 until mask.rows()) {
                            val value = mask.get(yy, xx)?.getOrNull(0)?.toInt() ?: 0
                            if (value > 0) {
                                found = true
                                break
                            }
                        }
                    }
                    if (found) break
                }

            } else if (vertical) {
                for (dx in -4..4) {
                    val xx = x + dx
                    val yy = y
                    if (xx in 0 until mask.cols() && yy in 0 until mask.rows()) {
                        val value = mask.get(yy, xx)?.getOrNull(0)?.toInt() ?: 0
                        if (value > 0) {
                            found = true
                            break
                        }
                    }
                }

            } else {
                for (dy in -4..4) {
                    val xx = x
                    val yy = y + dy
                    if (xx in 0 until mask.cols() && yy in 0 until mask.rows()) {
                        val value = mask.get(yy, xx)?.getOrNull(0)?.toInt() ?: 0
                        if (value > 0) {
                            found = true
                            break
                        }
                    }
                }
            }

            if (found) hits++
        }

        return hits >= 5
    }

    private fun confidenceFromArea(area: Int): Float {
        return when {
            area >= 600 -> 0.95f
            area >= 300 -> 0.85f
            area >= 150 -> 0.75f
            area >= 80 -> 0.65f
            else -> 0.55f
        }
    }

    private fun buildRoi(
        imageWidth: Int,
        imageHeight: Int,
        a: RectF,
        b: RectF,
        padding: Int
    ): Rect {
        val left = max(0, min(a.left, b.left).toInt() - padding)
        val top = max(0, min(a.top, b.top).toInt() - padding)
        val right = min(imageWidth, max(a.right, b.right).toInt() + padding)
        val bottom = min(imageHeight, max(a.bottom, b.bottom).toInt() + padding)

        return Rect(left, top, max(1, right - left), max(1, bottom - top))
    }
}