package com.sd.arcuit.logic

import android.graphics.RectF

/**
 * Represents a single object detection result produced by the ML model.
 *
 * Each detection contains:
 * - the predicted class label
 * - the confidence score (0.0 to 1.0)
 * - the bounding box location in image coordinates
 */
data class Detection(
    val label: String,
    val confidence: Float,
    val boundingBox: RectF
)
