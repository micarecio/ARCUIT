package com.sd.arcuit

import android.graphics.Color
import android.graphics.RectF

/**
 * Represents a detected bounding box from the model output.
 * Used for drawing overlays and mapping detected components in the circuit.
 */
data class BoundingBox(
    val left: Float,
    val top: Float,
    val right: Float,
    val bottom: Float,
    val label: String,
    val color: Int
) {
    /**
     * Converts this bounding box into Android's RectF format
     * for rendering and collision detection.
     */
    fun toRectF(): RectF = RectF(left, top, right, bottom)
}

/**
 * Returns a display color based on the detected label type.
 * Used for visual debugging and UI overlay differentiation.
 */
fun colorForLabel(label: String): Int {
    return when (label.lowercase()) {

        // Core components
        "ic_body" -> Color.BLUE
        "led" -> Color.CYAN
        "switch" -> Color.GREEN
        "push_button" -> Color.DKGRAY

        // Circuit nodes
        "wire_endpoint" -> Color.YELLOW
        "pos_rail" -> Color.RED
        "neg_rail" -> Color.BLACK

        // fallback for unknown or unsupported labels
        else -> Color.MAGENTA
    }
}
