package com.sd.arcuit.logic

import org.opencv.core.Core
import org.opencv.core.Mat
import org.opencv.core.MatOfPoint
import org.opencv.core.Point
import org.opencv.core.Rect
import org.opencv.core.Scalar
import org.opencv.core.Size
import org.opencv.imgproc.Imgproc

object PinConnectionDetector {

    private const val PIN_TOUCH_RADIUS = 18
    private const val OBJECT_TOUCH_RADIUS = 25
    private const val MIN_WIRE_AREA = 400.0
    private const val MIN_PIXELS_NEAR_POINT = 5

    data class PinConnection(
        val icId: String,
        val pinIndex: Int,
        val objectId: String,
        val objectType: ObjectType,
        val objectX: Float,
        val objectY: Float,
        val wireColor: WireColor
    )

    data class WireTrace(
        val wireColor: WireColor,
        val points: List<TracePoint>
    )

    data class TracePoint(
        val x: Float,
        val y: Float
    )

    data class DetectionResult(
        val connections: List<PinConnection>,
        val wireTraces: List<WireTrace>
    )

    enum class WireColor {
        RED,
        BLUE,
        GREEN,
        YELLOW
    }

    fun detect(
        frame: Mat,
        icList: List<ICComponent>,
        objects: List<DetectedObject>,
        scale: Float,
        dx: Float,
        dy: Float
    ): DetectionResult {

        val connections = mutableListOf<PinConnection>()
        val wireTraces = mutableListOf<WireTrace>()

        val validObjects = objects.filter {
            it.type == ObjectType.WIRE_ENDPOINT ||
                    it.type == ObjectType.VCC ||
                    it.type == ObjectType.GND
        }

        if (frame.empty() || icList.isEmpty()) {
            return DetectionResult(emptyList(), emptyList())
        }

        val hsvFrame = Mat()
        val rgbFrame = Mat()

        try {
            Imgproc.cvtColor(
                frame,
                rgbFrame,
                Imgproc.COLOR_RGBA2RGB
            )

            Imgproc.cvtColor(
                rgbFrame,
                hsvFrame,
                Imgproc.COLOR_RGB2HSV
            )

            val wireMasks = createWireColorMasks(hsvFrame)

            val circuitRoi = buildCircuitRoi(
                frame = frame,
                icList = icList,
                objects = validObjects,
                scale = scale,
                dx = dx,
                dy = dy
            )

            for ((wireColor, rawMask) in wireMasks) {

                val cleanedMask = applyRoiMask(
                    cleanMask(rawMask),
                    circuitRoi
                )

                val contours = mutableListOf<MatOfPoint>()
                val hierarchy = Mat()
                val contourInput = cleanedMask.clone()

                Imgproc.findContours(
                    contourInput,
                    contours,
                    hierarchy,
                    Imgproc.RETR_EXTERNAL,
                    Imgproc.CHAIN_APPROX_SIMPLE
                )

                contourInput.release()

                for (contour in contours) {

                    val area = Imgproc.contourArea(contour)

                    if (area < MIN_WIRE_AREA) {
                        contour.release()
                        continue
                    }

                    val wireRect = Imgproc.boundingRect(contour)

                    val contourMask = Mat.zeros(
                        cleanedMask.size(),
                        cleanedMask.type()
                    )

                    Imgproc.drawContours(
                        contourMask,
                        listOf(contour),
                        -1,
                        Scalar(255.0),
                        Imgproc.FILLED
                    )

                    val touchesPinOrObject = contourTouchesPinOrObject(
                        contourMask = contourMask,
                        icList = icList,
                        validObjects = validObjects,
                        scale = scale,
                        dx = dx,
                        dy = dy
                    )

                    if (!touchesPinOrObject) {
                        contourMask.release()
                        contour.release()
                        continue
                    }

                    val tracePoints = contour.toArray().map {
                        TracePoint(
                            x = toOverlayX(it.x.toFloat(), scale, dx),
                            y = toOverlayY(it.y.toFloat(), scale, dy)
                        )
                    }

                    if (tracePoints.size >= 3) {
                        wireTraces.add(
                            WireTrace(
                                wireColor = wireColor,
                                points = tracePoints
                            )
                        )
                    }

                    for (ic in icList) {
                        for (pin in ic.pins) {

                            val pinX = toImageX(pin.point.x, scale, dx)
                            val pinY = toImageY(pin.point.y, scale, dy)

                            val pinTouchesWire = isWireNearPoint(
                                contourMask,
                                pinX,
                                pinY,
                                PIN_TOUCH_RADIUS
                            )

                            if (!pinTouchesWire) {
                                continue
                            }

                            for (obj in validObjects) {

                                val objCenterOverlayX = (obj.left + obj.right) / 2f
                                val objCenterOverlayY = (obj.top + obj.bottom) / 2f

                                val objCenterX = toImageX(objCenterOverlayX, scale, dx)
                                val objCenterY = toImageY(objCenterOverlayY, scale, dy)

                                val objectTouchesWire = isWireNearPoint(
                                    contourMask,
                                    objCenterX,
                                    objCenterY,
                                    OBJECT_TOUCH_RADIUS
                                )

                                val objectInsideWireArea = wireRect.contains(
                                    Point(
                                        objCenterX.toDouble(),
                                        objCenterY.toDouble()
                                    )
                                )

                                if (objectTouchesWire || objectInsideWireArea) {
                                    connections.add(
                                        PinConnection(
                                            icId = ic.id,
                                            pinIndex = pin.index,
                                            objectId = obj.id,
                                            objectType = obj.type,
                                            objectX = objCenterOverlayX,
                                            objectY = objCenterOverlayY,
                                            wireColor = wireColor
                                        )
                                    )
                                }
                            }
                        }
                    }

                    contourMask.release()
                    contour.release()
                }

                hierarchy.release()
                cleanedMask.release()
                rawMask.release()
            }

        } finally {
            rgbFrame.release()
            hsvFrame.release()
        }

        return DetectionResult(
            connections = connections.distinctBy {
                "${it.icId}-${it.pinIndex}-${it.objectId}-${it.wireColor}"
            },
            wireTraces = wireTraces
        )
    }

    private fun contourTouchesPinOrObject(
        contourMask: Mat,
        icList: List<ICComponent>,
        validObjects: List<DetectedObject>,
        scale: Float,
        dx: Float,
        dy: Float
    ): Boolean {

        for (ic in icList) {
            for (pin in ic.pins) {
                val pinX = toImageX(pin.point.x, scale, dx)
                val pinY = toImageY(pin.point.y, scale, dy)

                if (isWireNearPoint(contourMask, pinX, pinY, PIN_TOUCH_RADIUS)) {
                    return true
                }
            }
        }

        for (obj in validObjects) {
            val objCenterOverlayX = (obj.left + obj.right) / 2f
            val objCenterOverlayY = (obj.top + obj.bottom) / 2f

            val objCenterX = toImageX(objCenterOverlayX, scale, dx)
            val objCenterY = toImageY(objCenterOverlayY, scale, dy)

            if (isWireNearPoint(contourMask, objCenterX, objCenterY, OBJECT_TOUCH_RADIUS)) {
                return true
            }
        }

        return false
    }

    private fun toImageX(
        overlayX: Float,
        scale: Float,
        dx: Float
    ): Int {
        return ((overlayX - dx) / scale).toInt()
    }

    private fun toImageY(
        overlayY: Float,
        scale: Float,
        dy: Float
    ): Int {
        return ((overlayY - dy) / scale).toInt()
    }

    private fun toOverlayX(
        imageX: Float,
        scale: Float,
        dx: Float
    ): Float {
        return imageX * scale + dx
    }

    private fun toOverlayY(
        imageY: Float,
        scale: Float,
        dy: Float
    ): Float {
        return imageY * scale + dy
    }

    private fun buildCircuitRoi(
        frame: Mat,
        icList: List<ICComponent>,
        objects: List<DetectedObject>,
        scale: Float,
        dx: Float,
        dy: Float
    ): Rect? {

        val xs = mutableListOf<Int>()
        val ys = mutableListOf<Int>()

        icList.forEach { ic ->
            xs.add(toImageX(ic.boundingBox.left, scale, dx))
            xs.add(toImageX(ic.boundingBox.right, scale, dx))
            ys.add(toImageY(ic.boundingBox.top, scale, dy))
            ys.add(toImageY(ic.boundingBox.bottom, scale, dy))
        }

        objects.forEach { obj ->
            xs.add(toImageX(obj.left, scale, dx))
            xs.add(toImageX(obj.right, scale, dx))
            ys.add(toImageY(obj.top, scale, dy))
            ys.add(toImageY(obj.bottom, scale, dy))
        }

        if (xs.isEmpty() || ys.isEmpty()) {
            return null
        }

        val padding = 80

        val left = (xs.minOrNull()!! - padding).coerceAtLeast(0)
        val top = (ys.minOrNull()!! - padding).coerceAtLeast(0)
        val right = (xs.maxOrNull()!! + padding).coerceAtMost(frame.cols() - 1)
        val bottom = (ys.maxOrNull()!! + padding).coerceAtMost(frame.rows() - 1)

        if (right <= left || bottom <= top) {
            return null
        }

        return Rect(
            left,
            top,
            right - left,
            bottom - top
        )
    }

    private fun applyRoiMask(
        mask: Mat,
        roi: Rect?
    ): Mat {

        if (roi == null) {
            return mask
        }

        val roiOnlyMask = Mat.zeros(mask.size(), mask.type())

        Imgproc.rectangle(
            roiOnlyMask,
            roi,
            Scalar(255.0),
            Imgproc.FILLED
        )

        Core.bitwise_and(mask, roiOnlyMask, mask)

        roiOnlyMask.release()

        return mask
    }

    private fun createWireColorMasks(hsvFrame: Mat): Map<WireColor, Mat> {

        val masks = mutableMapOf<WireColor, Mat>()

        val redMask1 = Mat()
        val redMask2 = Mat()
        val redMask = Mat()

        Core.inRange(
            hsvFrame,
            Scalar(0.0, 90.0, 60.0),
            Scalar(10.0, 255.0, 255.0),
            redMask1
        )

        Core.inRange(
            hsvFrame,
            Scalar(170.0, 90.0, 60.0),
            Scalar(180.0, 255.0, 255.0),
            redMask2
        )

        Core.bitwise_or(redMask1, redMask2, redMask)
        masks[WireColor.RED] = redMask

        val blueMask = Mat()

        Core.inRange(
            hsvFrame,
            Scalar(100.0, 100.0, 80.0),
            Scalar(125.0, 255.0, 255.0),
            blueMask
        )

        masks[WireColor.BLUE] = blueMask

        val greenMask = Mat()

        Core.inRange(
            hsvFrame,
            Scalar(40.0, 80.0, 70.0),
            Scalar(85.0, 255.0, 255.0),
            greenMask
        )

        masks[WireColor.GREEN] = greenMask

        val yellowMask = Mat()

        Core.inRange(
            hsvFrame,
            Scalar(22.0, 100.0, 100.0),
            Scalar(35.0, 255.0, 255.0),
            yellowMask
        )

        masks[WireColor.YELLOW] = yellowMask

        redMask1.release()
        redMask2.release()

        return masks
    }

    private fun cleanMask(mask: Mat): Mat {

        val cleaned = Mat()

        val kernel = Imgproc.getStructuringElement(
            Imgproc.MORPH_RECT,
            Size(5.0, 5.0)
        )

        Imgproc.morphologyEx(
            mask,
            cleaned,
            Imgproc.MORPH_OPEN,
            kernel
        )

        Imgproc.morphologyEx(
            cleaned,
            cleaned,
            Imgproc.MORPH_CLOSE,
            kernel
        )

        kernel.release()

        return cleaned
    }

    private fun isWireNearPoint(
        mask: Mat,
        x: Int,
        y: Int,
        radius: Int
    ): Boolean {

        if (mask.empty()) {
            return false
        }

        val safeX = x.coerceIn(0, mask.cols() - 1)
        val safeY = y.coerceIn(0, mask.rows() - 1)

        val left = (safeX - radius).coerceAtLeast(0)
        val top = (safeY - radius).coerceAtLeast(0)
        val right = (safeX + radius).coerceAtMost(mask.cols() - 1)
        val bottom = (safeY + radius).coerceAtMost(mask.rows() - 1)

        if (right <= left || bottom <= top) {
            return false
        }

        val roi = mask.submat(
            Rect(
                left,
                top,
                right - left,
                bottom - top
            )
        )

        val nonZeroPixels = Core.countNonZero(roi)

        roi.release()

        return nonZeroPixels > MIN_PIXELS_NEAR_POINT
    }
}