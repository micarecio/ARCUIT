package com.sd.arcuit.detector

import android.graphics.Bitmap

/**
 * Holds the result of a letterbox preprocessing operation.
 *
 * This includes:
 * - the resized + padded bitmap used for model input
 * - the scale factor used during resizing
 * - horizontal and vertical padding applied to center the image
 *
 * These values are needed to correctly map model output
 * coordinates back to the original image space.
 */
data class LetterboxResult(
    val bitmap: Bitmap,
    val scale: Float,
    val padX: Float,
    val padY: Float
)
