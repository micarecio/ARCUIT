package com.sd.arcuit

import android.graphics.Color
import android.graphics.RectF

data class BoundingBox(
    val left: Float,
    val top: Float,
    val right: Float,
    val bottom: Float,
    val label: String,
    val color: Int
) {
    fun toRectF() = RectF(left, top, right, bottom)
}

fun colorForLabel(label: String): Int {
    return when (label.lowercase()) {
        "ic_body" -> Color.BLUE
        "led" -> Color.CYAN
        "wire_endpoint" -> Color.YELLOW
        "pos_rail" -> Color.RED
        "neg_rail" -> Color.BLACK
        "switch" -> Color.GREEN
        "push_button" -> Color.DKGRAY
        else -> Color.MAGENTA
    }
}