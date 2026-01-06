package com.sd.arcuit

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import com.sd.arcuit.logic.ICComponent

class OverlayView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : View(context, attrs) {

    private val boxPaint = Paint().apply {
        color = Color.GREEN
        style = Paint.Style.STROKE
        strokeWidth = 6f
    }

    private val textPaint = Paint().apply {
        color = Color.GREEN
        textSize = 40f
        style = Paint.Style.FILL
    }

    private var boxes: List<BoundingBox> = emptyList()
    private var icBodies: List<ICComponent> = emptyList()

    var listener: ICClickListener? = null

    fun setBoxes(newBoxes: List<BoundingBox>) {
        boxes = newBoxes
        invalidate()
    }

    fun setICBodies(newICs: List<ICComponent>) {
        icBodies = newICs
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        // Draw bounding boxes
        for (box in boxes) {
            canvas.drawRect(box.left, box.top, box.right, box.bottom, boxPaint)
            canvas.drawText(box.label, box.left, box.top - 10, textPaint)
        }

        // Draw gate labels
        for (ic in icBodies) {
            ic.gateType?.let {
                canvas.drawText(
                    it.name,
                    ic.boundingBox.left,
                    ic.boundingBox.top - 20,
                    textPaint
                )
            }
        }
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (event.action == MotionEvent.ACTION_DOWN) {
            val x = event.x
            val y = event.y

            val tappedIC = icBodies.find {
                it.boundingBox.contains(x, y)
            }

            if (tappedIC != null) {
                listener?.onICClicked(tappedIC)
                return true
            }
        }
        return false
    }

    interface ICClickListener {
        fun onICClicked(ic: ICComponent)
    }
}
