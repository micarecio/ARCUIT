package com.sd.arcuit

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import com.sd.arcuit.logic.ICComponent

class OverlayView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : View(context, attrs) {

    private val boxPaint = Paint().apply {
        style = Paint.Style.STROKE
        strokeWidth = 6f
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

    private var boxes: List<BoundingBox> = emptyList()
    private var icBodies: List<ICComponent> = emptyList()
    private var icLabels: Map<String, String> = emptyMap()

    var listener: ICClickListener? = null

    fun setBoxes(newBoxes: List<BoundingBox>) {
        boxes = newBoxes
        invalidate()
    }

    fun setICBodies(newICs: List<ICComponent>) {
        icBodies = newICs
        invalidate()
    }

    fun setICLabels(labels: Map<String, String>) {
        icLabels = labels
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        for (box in boxes) {
            boxPaint.color = box.color

            if (box.label.endsWith("_endpoint")) {
                val cx = (box.left + box.right) / 2f
                val cy = (box.top + box.bottom) / 2f
                canvas.drawCircle(cx, cy, 12f, boxPaint)
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

        // 🔒 Frozen IC labels (centered)
        for (ic in icBodies) {
            val label = icLabels[ic.id] ?: continue

            val cx = ic.boundingBox.centerX()
            val cy = ic.boundingBox.centerY()
            val offset = (gateTextPaint.descent() + gateTextPaint.ascent()) / 2f

            canvas.drawText(label, cx, cy - offset, gateTextPaint)
        }
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (event.action == MotionEvent.ACTION_DOWN) {
            icBodies.firstOrNull {
                it.id !in icLabels.keys &&
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
