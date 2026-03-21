package com.sd.arcuit

import android.content.Context
import android.graphics.*
import android.graphics.BitmapFactory
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import com.sd.arcuit.logic.ConnectionPoint
import com.sd.arcuit.logic.ICComponent
import com.sd.arcuit.logic.ICPin
import com.sd.arcuit.logic.Net
import kotlin.math.pow
import kotlin.math.sqrt

class OverlayView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : View(context, attrs) {

    enum class Layer {
        BOUNDING_BOX,
        AR_2D,
        CIRCUIT_DIAGRAM
    }

    private var currentLayer = Layer.BOUNDING_BOX

    fun setLayer(layer: Layer) {
        currentLayer = layer
        postInvalidateOnAnimation()
    }

    val currentLayerPublic: Layer
        get() = currentLayer

    private val boxPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 6f
        color = Color.GREEN
    }

    private val detectionTextPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        textSize = 36f
        color = Color.WHITE
    }

    private val gateTextPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        textSize = 42f
        color = Color.WHITE
        textAlign = Paint.Align.CENTER
        isFakeBoldText = true
    }

    private val neonPaint = Paint(Paint.ANTI_ALIAS_FLAG)

    private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.CYAN
        textSize = 32f
        typeface = Typeface.DEFAULT_BOLD
        textAlign = Paint.Align.CENTER
    }

    private val connectedPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.GREEN
        style = Paint.Style.FILL
    }

    private val missingPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.RED
        style = Paint.Style.FILL
    }

    private val correctPathPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.GREEN
        strokeWidth = 6f
        style = Paint.Style.STROKE
    }

    private val wrongPathPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.RED
        strokeWidth = 6f
        style = Paint.Style.STROKE
    }

    private val pinStrokePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        style = Paint.Style.STROKE
        strokeWidth = 2f
    }

    private val basePinPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        style = Paint.Style.FILL
        alpha = 180
    }

    private val ic7404Bitmap: Bitmap by lazy {
        BitmapFactory.decodeResource(resources, R.drawable.ic_7404)
    }

    private val ic7400Bitmap: Bitmap by lazy {
        BitmapFactory.decodeResource(resources, R.drawable.ic_7400)
    }

    private var boxes: List<BoundingBox> = emptyList()
    private var icBodies: List<ICComponent> = emptyList()
    private var icLabels: Map<String, String> = emptyMap()
    private var nets: List<Net> = emptyList()

    data class ConnectionMarker(
        val x: Float,
        val y: Float,
        val isConnected: Boolean
    )

    data class GuideSegment(
        val startX: Float,
        val startY: Float,
        val endX: Float,
        val endY: Float,
        val isCorrect: Boolean
    )

    private var connectionMarkers: List<ConnectionMarker> = emptyList()
    private var guideSegments: List<GuideSegment> = emptyList()

    var listener: ICClickListener? = null

    fun update(
        newBoxes: List<BoundingBox>,
        newICs: List<ICComponent>,
        labels: Map<String, String>
    ) {
        boxes = newBoxes
        icBodies = newICs
        icLabels = labels
        postInvalidateOnAnimation()
    }

    fun setNets(newNets: List<Net>) {
        nets = newNets

        val newMarkers = mutableListOf<ConnectionMarker>()
        val newSegments = mutableListOf<GuideSegment>()

        newNets.forEach { net ->

            val pins = net.points.filter { it.label.contains(":") }
            val endpoints = net.points.filter { it.label == "wire_endpoint" }

            val hasEndpoint = endpoints.isNotEmpty()

            pins.forEach { pinPoint ->

                if (hasEndpoint) {
                    newMarkers.add(
                        ConnectionMarker(
                            x = pinPoint.x,
                            y = pinPoint.y,
                            isConnected = true
                        )
                    )

                    val pathPoints = buildPathPoints(pinPoint, endpoints, emptyList())

                    for (i in 0 until pathPoints.size - 1) {
                        newSegments.add(
                            GuideSegment(
                                startX = pathPoints[i].first,
                                startY = pathPoints[i].second,
                                endX = pathPoints[i + 1].first,
                                endY = pathPoints[i + 1].second,
                                isCorrect = true
                            )
                        )
                    }
                }
            }
        }

        connectionMarkers = newMarkers
        guideSegments = newSegments

        postInvalidateOnAnimation()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        when (currentLayer) {
            Layer.BOUNDING_BOX -> drawBoundingBoxes(canvas)
            Layer.AR_2D -> drawNeonAR(canvas)
            Layer.CIRCUIT_DIAGRAM -> drawCircuitDiagram(canvas)
        }
    }

    private fun drawBoundingBoxes(canvas: Canvas) {
        for (box in boxes) {
            boxPaint.color = box.color

            if (box.label.endsWith("_endpoint")) {
                val cx = (box.left + box.right) / 2f
                val cy = (box.top + box.bottom) / 2f
                canvas.drawCircle(cx, cy, 4f, boxPaint)
                continue
            }

            canvas.drawRect(box.left, box.top, box.right, box.bottom, boxPaint)

            if (box.label != "ic_body") {
                canvas.drawText(
                    box.label,
                    box.left + 6f,
                    box.top - 10f,
                    detectionTextPaint
                )
            }
        }

        for (ic in icBodies) {
            val label = icLabels[ic.id] ?: continue

            val cx = ic.boundingBox.centerX()
            val cy = ic.boundingBox.centerY()
            val offset = (gateTextPaint.descent() + gateTextPaint.ascent()) / 2f

            canvas.drawText(label, cx, cy - offset, gateTextPaint)
        }
    }

    private fun drawNeonAR(canvas: Canvas) {
        icBodies.forEach { ic ->
            neonPaint.style = Paint.Style.STROKE
            neonPaint.strokeWidth = 6f
            neonPaint.color = Color.CYAN
            neonPaint.maskFilter = BlurMaskFilter(15f, BlurMaskFilter.Blur.OUTER)

            canvas.drawRect(ic.boundingBox, neonPaint)

            val label = icLabels[ic.id] ?: "IC"

            canvas.drawText(
                label,
                ic.boundingBox.left,
                ic.boundingBox.top - 10f,
                textPaint
            )

            ic.pins.forEach { pin: ICPin ->
                neonPaint.style = Paint.Style.FILL
                neonPaint.color = Color.RED
                neonPaint.maskFilter = BlurMaskFilter(12f, BlurMaskFilter.Blur.OUTER)

                canvas.drawCircle(pin.point.x, pin.point.y, 12f, neonPaint)
            }
        }
    }

    private fun drawCircuitDiagram(canvas: Canvas) {
        icBodies.forEach { ic ->
            val box = ic.boundingBox
            val label = icLabels[ic.id] ?: "IC"

            val selectedBitmap = when (label) {
                "7404" -> ic7404Bitmap
                "7400" -> ic7400Bitmap
                else -> null
            }

            if (selectedBitmap != null) {
                val bmpW = selectedBitmap.width.toFloat()
                val bmpH = selectedBitmap.height.toFloat()
                val bmpAspect = bmpW / bmpH

                val boxW = box.width()
                val boxH = box.height()
                val boxAspect = boxW / boxH

                val destRect = if (bmpAspect > boxAspect) {
                    val targetW = boxH * bmpAspect
                    val left = box.left - (targetW - boxW) / 2f
                    RectF(left, box.top, left + targetW, box.bottom)
                } else {
                    val targetH = boxW / bmpAspect
                    val top = box.top - (targetH - boxH) / 2f
                    RectF(box.left, top, box.right, top + targetH)
                }

                val saveCount = canvas.save()
                canvas.clipRect(box)
                canvas.drawBitmap(selectedBitmap, null, destRect, null)
                canvas.restoreToCount(saveCount)
            }

            // 🔥 ALWAYS DRAW BOUNDING BOX (on top)
            boxPaint.color = Color.YELLOW
            boxPaint.style = Paint.Style.STROKE
            boxPaint.strokeWidth = 4f
            canvas.drawRect(box, boxPaint)

            // 🔹 pins (still visible)
            ic.pins.forEach { pin ->
                canvas.drawCircle(pin.point.x, pin.point.y, 4f, basePinPaint)
                canvas.drawCircle(pin.point.x, pin.point.y, 6f, pinStrokePaint)
            }
        }

        // 🔹 wires
        guideSegments.forEach { segment ->
            val paint = if (segment.isCorrect) correctPathPaint else wrongPathPaint
            canvas.drawLine(
                segment.startX,
                segment.startY,
                segment.endX,
                segment.endY,
                paint
            )
        }

        // 🔹 connection markers
        connectionMarkers.forEach { marker ->
            val fillPaint = if (marker.isConnected) connectedPaint else missingPaint
            canvas.drawCircle(marker.x, marker.y, 8f, fillPaint)
            canvas.drawCircle(marker.x, marker.y, 10f, pinStrokePaint)
        }
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (event.action == MotionEvent.ACTION_DOWN) {
            icBodies.firstOrNull {
                it.boundingBox.contains(event.x, event.y)
            }?.let {
                listener?.onICClicked(it)
                return true
            }
        }
        return false
    }

    interface ICClickListener {
        fun onICClicked(ic: ICComponent)
    }

    private fun buildPathPoints(
        pinPoint: ConnectionPoint,
        endpoints: List<ConnectionPoint>,
        rails: List<ConnectionPoint>
    ): List<Pair<Float, Float>> {

        val result = mutableListOf<Pair<Float, Float>>()
        val remainingEndpoints = endpoints.toMutableList()

        result.add(pinPoint.x to pinPoint.y)

        var currentX = pinPoint.x
        var currentY = pinPoint.y

        while (remainingEndpoints.isNotEmpty()) {
            val next = remainingEndpoints.minByOrNull { point ->
                distance(currentX, currentY, point.x, point.y)
            } ?: break

            result.add(next.x to next.y)
            currentX = next.x
            currentY = next.y
            remainingEndpoints.remove(next)
        }

        if (rails.isNotEmpty()) {
            val nearestRail = rails.minByOrNull { point ->
                distance(currentX, currentY, point.x, point.y)
            }

            if (nearestRail != null) {
                result.add(nearestRail.x to nearestRail.y)
            }
        }

        return result
    }

    private fun distance(x1: Float, y1: Float, x2: Float, y2: Float): Float {
        return sqrt((x1 - x2).pow(2) + (y1 - y2).pow(2))
    }
}