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

    private val warningPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.YELLOW
        style = Paint.Style.FILL
    }

    private val neutralPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.GRAY
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

    private val ic7400Bitmap: Bitmap by lazy {
        BitmapFactory.decodeResource(resources, R.drawable.ic_7400)
    }

    private val ic7401Bitmap: Bitmap by lazy {
        BitmapFactory.decodeResource(resources, R.drawable.ic_7401)
    }

    private val ic7402Bitmap: Bitmap by lazy {
        BitmapFactory.decodeResource(resources, R.drawable.ic_7402)
    }

    private val ic7403Bitmap: Bitmap by lazy {
        BitmapFactory.decodeResource(resources, R.drawable.ic_7403)
    }

    private val ic7404Bitmap: Bitmap by lazy {
        BitmapFactory.decodeResource(resources, R.drawable.ic_7404)
    }

    private val ic7405Bitmap: Bitmap by lazy {
        BitmapFactory.decodeResource(resources, R.drawable.ic_7405)
    }

    private val ic7406Bitmap: Bitmap by lazy {
        BitmapFactory.decodeResource(resources, R.drawable.ic_7406)
    }

    private val ic7407Bitmap: Bitmap by lazy {
        BitmapFactory.decodeResource(resources, R.drawable.ic_7407)
    }

    private val ic7408Bitmap: Bitmap by lazy {
        BitmapFactory.decodeResource(resources, R.drawable.ic_7408)
    }

    private val ic7409Bitmap: Bitmap by lazy {
        BitmapFactory.decodeResource(resources, R.drawable.ic_7409)
    }

    private val ic7410Bitmap: Bitmap by lazy {
        BitmapFactory.decodeResource(resources, R.drawable.ic_7410)
    }

    private val ic7411Bitmap: Bitmap by lazy {
        BitmapFactory.decodeResource(resources, R.drawable.ic_7411)
    }

    private val ic7412Bitmap: Bitmap by lazy {
        BitmapFactory.decodeResource(resources, R.drawable.ic_7412)
    }

    private val ic7413Bitmap: Bitmap by lazy {
        BitmapFactory.decodeResource(resources, R.drawable.ic_7413)
    }

    private val ic7414Bitmap: Bitmap by lazy {
        BitmapFactory.decodeResource(resources, R.drawable.ic_7414)
    }

    private val ic7415Bitmap: Bitmap by lazy {
        BitmapFactory.decodeResource(resources, R.drawable.ic_7415)
    }

    private val ic7416Bitmap: Bitmap by lazy {
        BitmapFactory.decodeResource(resources, R.drawable.ic_7416)
    }

    private val ic7417Bitmap: Bitmap by lazy {
        BitmapFactory.decodeResource(resources, R.drawable.ic_7417)
    }

    private val ic7418Bitmap: Bitmap by lazy {
        BitmapFactory.decodeResource(resources, R.drawable.ic_7418)
    }

    private val ic7419Bitmap: Bitmap by lazy {
        BitmapFactory.decodeResource(resources, R.drawable.ic_7419)
    }

    private val ic7420Bitmap: Bitmap by lazy {
        BitmapFactory.decodeResource(resources, R.drawable.ic_7420)
    }

    private val ic7421Bitmap: Bitmap by lazy {
        BitmapFactory.decodeResource(resources, R.drawable.ic_7421)
    }

    private val ic7422Bitmap: Bitmap by lazy {
        BitmapFactory.decodeResource(resources, R.drawable.ic_7422)
    }

    private val ic7424Bitmap: Bitmap by lazy {
        BitmapFactory.decodeResource(resources, R.drawable.ic_7424)
    }

    private val ic7425Bitmap: Bitmap by lazy {
        BitmapFactory.decodeResource(resources, R.drawable.ic_7425)
    }

    private val ic7426Bitmap: Bitmap by lazy {
        BitmapFactory.decodeResource(resources, R.drawable.ic_7426)
    }

    private val ic7427Bitmap: Bitmap by lazy {
        BitmapFactory.decodeResource(resources, R.drawable.ic_7427)
    }

    private val ic7428Bitmap: Bitmap by lazy {
        BitmapFactory.decodeResource(resources, R.drawable.ic_7428)
    }

    private val ic7430Bitmap: Bitmap by lazy {
        BitmapFactory.decodeResource(resources, R.drawable.ic_7430)
    }

    private val ic7432Bitmap: Bitmap by lazy {
        BitmapFactory.decodeResource(resources, R.drawable.ic_7432)
    }

    private val ic7433Bitmap: Bitmap by lazy {
        BitmapFactory.decodeResource(resources, R.drawable.ic_7433)
    }

    private val ic7437Bitmap: Bitmap by lazy {
        BitmapFactory.decodeResource(resources, R.drawable.ic_7437)
    }

    private val ic7438Bitmap: Bitmap by lazy {
        BitmapFactory.decodeResource(resources, R.drawable.ic_7438)
    }

    private val ic7440Bitmap: Bitmap by lazy {
        BitmapFactory.decodeResource(resources, R.drawable.ic_7440)
    }

    private val ic7486Bitmap: Bitmap by lazy {
        BitmapFactory.decodeResource(resources, R.drawable.ic_7486)
    }

    private val ic74132Bitmap: Bitmap by lazy {
        BitmapFactory.decodeResource(resources, R.drawable.ic_74132)
    }

    private val ic74136Bitmap: Bitmap by lazy {
        BitmapFactory.decodeResource(resources, R.drawable.ic_74136)
    }

    private val ic74266Bitmap: Bitmap by lazy {
        BitmapFactory.decodeResource(resources, R.drawable.ic_74266)
    }

    private val ic747001Bitmap: Bitmap by lazy {
        BitmapFactory.decodeResource(resources, R.drawable.ic_747001)
    }

    private val ic747002Bitmap: Bitmap by lazy {
        BitmapFactory.decodeResource(resources, R.drawable.ic_747002)
    }

    private val ic747032Bitmap: Bitmap by lazy {
        BitmapFactory.decodeResource(resources, R.drawable.ic_747032)
    }

    private val ic747266Bitmap: Bitmap by lazy {
        BitmapFactory.decodeResource(resources, R.drawable.ic_747266)
    }


    private var boxes: List<BoundingBox> = emptyList()
    private var icBodies: List<ICComponent> = emptyList()
    private var icLabels: Map<String, String> = emptyMap()
    private var nets: List<Net> = emptyList()

    enum class PinVisualState {
        RED,
        YELLOW,
        GREEN,
        GRAY
    }

    data class ConnectionMarker(
        val x: Float,
        val y: Float,
        val state: PinVisualState
    )

    data class GuideSegment(
        val startX: Float,
        val startY: Float,
        val endX: Float,
        val endY: Float,
        val isCorrect: Boolean
    )

    data class WireEndpointSegment(
        val startX: Float,
        val startY: Float,
        val endX: Float,
        val endY: Float,
        val color: Int
    )

    private var connectionMarkers: List<ConnectionMarker> = emptyList()
    private var guideSegments: List<GuideSegment> = emptyList()
    private var wireEndpointSegments: List<WireEndpointSegment> = emptyList()

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

    fun setWireEndpointSegments(newSegments: List<WireEndpointSegment>) {
        wireEndpointSegments = newSegments
        postInvalidateOnAnimation()
    }

    fun setNets(newNets: List<Net>) {
        nets = newNets

        val newMarkers = mutableListOf<ConnectionMarker>()
        val newSegments = mutableListOf<GuideSegment>()

        val connectedPinsByIc = mutableMapOf<String, MutableSet<Int>>()

        // 🔹 Detect connected pins (ONLY endpoint-based)
        val detectedObjects = boxes.mapIndexedNotNull { index, box ->

            val mappedType = when (box.label) {
                "ic_body" -> com.sd.arcuit.logic.ObjectType.IC_BODY
                "wire_endpoint" -> com.sd.arcuit.logic.ObjectType.WIRE_ENDPOINT
                "resistor" -> com.sd.arcuit.logic.ObjectType.RESISTOR
                "led" -> com.sd.arcuit.logic.ObjectType.LED
                "switch" -> com.sd.arcuit.logic.ObjectType.SWITCH
                "push_button" -> com.sd.arcuit.logic.ObjectType.PUSH_BUTTON
                "vcc_pin" -> com.sd.arcuit.logic.ObjectType.VCC
                "gnd_pin" -> com.sd.arcuit.logic.ObjectType.GND
                else -> null
            }

            if (mappedType == null) {
                null
            } else {
                com.sd.arcuit.logic.DetectedObject(
                    id = index.toString(),
                    type = mappedType,
                    left = box.left,
                    top = box.top,
                    right = box.right,
                    bottom = box.bottom
                )
            }
        }

        val connections = com.sd.arcuit.logic.PinConnectionDetector.detect(icBodies, detectedObjects)

        connections.forEach { conn ->

            connectedPinsByIc
                .getOrPut(conn.icId) { mutableSetOf() }
                .add(conn.pinIndex)

            val ic = icBodies.firstOrNull { it.id == conn.icId } ?: return@forEach
            val pin = ic.pins.firstOrNull { it.index == conn.pinIndex } ?: return@forEach

            newSegments.add(
                GuideSegment(
                    startX = pin.point.x,
                    startY = pin.point.y,
                    endX = conn.objectX,
                    endY = conn.objectY,
                    isCorrect = true
                )
            )
        }


        // 🔹 Apply gate logic
        icBodies.forEach { ic ->

            val icType = icLabels[ic.id] ?: return@forEach
            val groups = com.sd.arcuit.logic.ICGateGroups.DIP14[icType] ?: return@forEach
            val connectedPins = connectedPinsByIc[ic.id] ?: emptySet<Int>()

            ic.pins.forEach { pin ->

                when (pin.role) {
                    com.sd.arcuit.logic.PinRole.VCC,
                    com.sd.arcuit.logic.PinRole.GND -> {

                        val isConnected = pin.index in connectedPins

                        newMarkers.add(
                            ConnectionMarker(
                                x = pin.point.x,
                                y = pin.point.y,
                                state = if (isConnected) PinVisualState.GREEN else PinVisualState.GRAY
                            )
                        )
                    }

                    else -> Unit
                }
            }

            groups.forEach { group ->

                val inputStatus = group.inputPins.map {
                    it to (it in connectedPins)
                }

                val allInputsPresent = inputStatus.all { it.second }

                // INPUTS
                inputStatus.forEach { (pinIndex, isConnected) ->

                    val pin = ic.pins.firstOrNull { it.index == pinIndex } ?: return@forEach

                    newMarkers.add(
                        ConnectionMarker(
                            x = pin.point.x,
                            y = pin.point.y,
                            state = if (isConnected) PinVisualState.GREEN else PinVisualState.GRAY
                        )
                    )
                }

                // OUTPUT
                val outputPin = ic.pins.firstOrNull { it.index == group.outputPin }

                if (outputPin != null) {
                    val outputHasWire = group.outputPin in connectedPins

                    val state = when {
                        allInputsPresent && outputHasWire -> PinVisualState.GREEN
                        allInputsPresent && !outputHasWire -> PinVisualState.YELLOW
                        else -> PinVisualState.RED
                    }

                    newMarkers.add(
                        ConnectionMarker(
                            x = outputPin.point.x,
                            y = outputPin.point.y,
                            state = state
                        )
                    )
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

        wireEndpointSegments.forEach { segment ->
            val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = segment.color
                strokeWidth = 8f
                style = Paint.Style.STROKE
            }

            canvas.drawLine(
                segment.startX,
                segment.startY,
                segment.endX,
                segment.endY,
                paint
            )
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
                "7400" -> ic7400Bitmap
                "7401" -> ic7401Bitmap
                "7402" -> ic7402Bitmap
                "7405" -> ic7405Bitmap
                "7404" -> ic7404Bitmap
                "7403" -> ic7403Bitmap
                "7406" -> ic7406Bitmap
                "7407" -> ic7407Bitmap
                "7408" -> ic7408Bitmap
                "7409" -> ic7409Bitmap
                "7410" -> ic7410Bitmap
                "7411" -> ic7411Bitmap
                "7412" -> ic7412Bitmap
                "7413" -> ic7413Bitmap
                "7414" -> ic7414Bitmap
                "7415" -> ic7415Bitmap
                "7416" -> ic7416Bitmap
                "7417" -> ic7417Bitmap
                "7418" -> ic7418Bitmap
                "7419" -> ic7419Bitmap
                "7420" -> ic7420Bitmap
                "7421" -> ic7421Bitmap
                "7422" -> ic7422Bitmap
                "7424" -> ic7424Bitmap
                "7425" -> ic7425Bitmap
                "7426" -> ic7426Bitmap
                "7427" -> ic7427Bitmap
                "7428" -> ic7428Bitmap
                "7430" -> ic7430Bitmap
                "7432" -> ic7432Bitmap
                "7433" -> ic7433Bitmap
                "7437" -> ic7437Bitmap
                "7438" -> ic7438Bitmap
                "7440" -> ic7440Bitmap
                "7486" -> ic7486Bitmap
                "74132" -> ic74132Bitmap
                "74136" -> ic74136Bitmap
                "74266" -> ic74266Bitmap
                "747001" -> ic747001Bitmap
                "747002" -> ic747002Bitmap
                "747032" -> ic747032Bitmap
                "747266" -> ic747266Bitmap
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
            val fillPaint = when (marker.state) {
                PinVisualState.GREEN -> connectedPaint
                PinVisualState.YELLOW -> warningPaint
                PinVisualState.RED -> missingPaint
                PinVisualState.GRAY -> neutralPaint
            }
            canvas.drawCircle(marker.x, marker.y, 8f, fillPaint)
            canvas.drawCircle(marker.x, marker.y, 10f, pinStrokePaint)
        }

        wireEndpointSegments.forEach { segment ->
            val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = segment.color
                strokeWidth = 8f
                style = Paint.Style.STROKE
            }

            canvas.drawLine(
                segment.startX,
                segment.startY,
                segment.endX,
                segment.endY,
                paint
            )
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