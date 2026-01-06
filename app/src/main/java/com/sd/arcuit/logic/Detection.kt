package com.sd.arcuit.logic

import android.graphics.RectF

data class Detection(
    val label: String,
    val confidence: Float,
    val boundingBox: RectF
)
