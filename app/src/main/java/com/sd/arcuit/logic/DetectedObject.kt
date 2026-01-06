package com.sd.arcuit.logic

import android.graphics.RectF

enum class ObjectType {
    IC_BODY,
    WIRE_ENDPOINT,
    LED,
    RESISTOR,
    VCC,
    GND
}

data class DetectedObject(
    val id: String,               // unique ID (UUID or incremental)
    val type: ObjectType,
    val left: Float,
    val top: Float,
    val right: Float,
    val bottom: Float
)

data class ICComponent(
    val id: String,
    val boundingBox: RectF,
    var gateType: GateType? = null
)