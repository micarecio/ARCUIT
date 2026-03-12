package com.sd.arcuit

import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import com.sd.arcuit.logic.ICComponent
import com.sd.arcuit.logic.ICPin
import com.sd.arcuit.logic.Net

class OverlayView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : View(context, attrs) {

    // -------------------- Layer Support --------------------

    enum class Layer {
        BOUNDING_BOX,
        AR_2D,
        CIRCUIT_DIAGRAM
    }

    private var currentLayer = Layer.BOUNDING_BOX

    val currentLayerPublic: Layer
        get() = currentLayer

    fun setLayer(layer: Layer) {
        currentLayer = layer
        postInvalidateOnAnimation()
    }

    // -------------------- Paints --------------------

    private val boxPaint = Paint().apply {
        style = Paint.Style.STROKE
        strokeWidth = 6f
        color = Color.GREEN
    }

    private val detectionTextPaint = Paint().apply {
        textSize = 36f
        color = Color.WHITE
        isAntiAlias = true
    }

    private val gateTextPaint = Paint().apply {
        textSize = 42f
        color = Color.WHITE
        textAlign = Paint.Align.CENTER
        isFakeBoldText = true
        isAntiAlias = true
    }

    private val neonPaint = Paint(Paint.ANTI_ALIAS_FLAG)

    private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.CYAN
        textSize = 32f
        typeface = Typeface.DEFAULT_BOLD
    }

    private val wirePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.GREEN
        strokeWidth = 6f
        style = Paint.Style.STROKE
    }

    // -------------------- Data --------------------

    private var boxes: List<BoundingBox> = emptyList()
    private var icBodies: List<ICComponent> = emptyList()
    private var icLabels: Map<String, String> = emptyMap()

    private var nets: List<Net> = emptyList()

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
        postInvalidateOnAnimation()
    }

    // -------------------- Draw --------------------

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        when (currentLayer) {
            Layer.BOUNDING_BOX -> drawBoundingBoxes(canvas)
            Layer.AR_2D -> drawNeonAR(canvas)
            Layer.CIRCUIT_DIAGRAM -> drawCircuitDiagram(canvas)
        }
    }

    // -------------------- Bounding Boxes --------------------

    private fun drawBoundingBoxes(canvas: Canvas) {

        for (box in boxes) {

            boxPaint.color = box.color

            if (box.label.endsWith("_endpoint")) {
                val cx = (box.left + box.right) / 2f
                val cy = (box.top + box.bottom) / 2f
                canvas.drawCircle(cx, cy, 13f, boxPaint)
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

    // -------------------- AR Layer --------------------

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

    // -------------------- Circuit Diagram --------------------

    private fun drawCircuitDiagram(canvas: Canvas) {

        // Draw IC bodies
        icBodies.forEach { ic ->

            boxPaint.color = Color.YELLOW
            boxPaint.style = Paint.Style.STROKE
            boxPaint.strokeWidth = 4f

            canvas.drawRect(ic.boundingBox, boxPaint)

            val label = icLabels[ic.id] ?: "IC"

            canvas.drawText(
                label,
                ic.boundingBox.centerX(),
                ic.boundingBox.top - 12f,
                textPaint
            )

            ic.pins.forEach { pin ->

                neonPaint.style = Paint.Style.FILL
                neonPaint.color = Color.CYAN
                neonPaint.maskFilter = BlurMaskFilter(10f, BlurMaskFilter.Blur.OUTER)

                canvas.drawCircle(pin.point.x, pin.point.y, 8f, neonPaint)
            }
        }

        // Draw connections between net points
        nets.forEach { net ->

            if (net.points.size < 2) return@forEach

            for (i in 0 until net.points.size - 1) {

                val a = net.points[i]
                val b = net.points[i + 1]

                canvas.drawLine(
                    a.x,
                    a.y,
                    b.x,
                    b.y,
                    wirePaint
                )
            }
        }
    }

    // -------------------- Touch --------------------

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
}
