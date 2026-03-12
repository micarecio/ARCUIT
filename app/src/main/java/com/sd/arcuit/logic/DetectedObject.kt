package com.sd.arcuit.logic

import android.graphics.RectF

enum class ObjectType {
    IC_BODY,
    WIRE_ENDPOINT,
    LED,
    RESISTOR,
    PUSH_BUTTON,
    SWITCH,
    VCC,
    GND;

    companion object {
        fun fromLabel(label: String): ObjectType {
            return when (label) {
                "ic_body" -> IC_BODY
                "wire_endpoint" -> WIRE_ENDPOINT
                "led" -> LED
                "resistor" -> RESISTOR
                "push_button" -> PUSH_BUTTON
                "switch" -> SWITCH
                "pos_rail" -> VCC
                "neg_rail" -> GND
                else -> WIRE_ENDPOINT
            }
        }
    }
}

data class DetectedObject(
    val id: String,
    val type: ObjectType,
    val left: Float,
    val top: Float,
    val right: Float,
    val bottom: Float
)

enum class PinRole {
    INPUT,
    OUTPUT,
    VCC,
    GND,
    UNKNOWN
}

data class ICPin(
    val index: Int,
    val role: PinRole,
    val point: ConnectionPoint
)

class ICComponent(
    val id: String,
    val boundingBox: RectF
) {
    val pins = mutableListOf<ICPin>()
    var type: String? = null
}
