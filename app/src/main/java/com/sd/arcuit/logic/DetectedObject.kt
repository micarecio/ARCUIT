package com.sd.arcuit.logic

import android.graphics.RectF

/**
 * Represents all detectable object types in the circuit system.
 *
 * Each type corresponds to a physical component or electrical reference
 * detected in the image (IC, LED, resistor, rails, etc.).
 */
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

        /**
         * Converts model output label strings into ObjectType enum values.
         */
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

/**
 * Represents a detected object from the ML model.
 *
 * Stored as bounding box coordinates plus classified type.
 */
data class DetectedObject(
    val id: String,
    val type: ObjectType,
    val left: Float,
    val top: Float,
    val right: Float,
    val bottom: Float
)

/**
 * Electrical role of an IC pin.
 */
enum class PinRole {
    INPUT,
    OUTPUT,
    VCC,
    GND,
    UNKNOWN
}

/**
 * Represents a single pin on an IC component.
 *
 * Each pin has:
 * - index (pin number)
 * - electrical role
 * - physical position in image space
 */
data class ICPin(
    val index: Int,
    val role: PinRole,
    val point: ConnectionPoint
)

/**
 * Represents an IC component detected in the image.
 *
 * Contains:
 * - bounding box of the IC body
 * - list of detected pins
 * - optional IC type classification
 */
class ICComponent(
    val id: String,
    val boundingBox: RectF
) {
    // List of all pins belonging to this IC
    val pins = mutableListOf<ICPin>()

    // Optional classification (e.g., 7408, 555 timer, etc.)
    var type: String? = null
}
