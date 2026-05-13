package com.sd.arcuit.logic

/**
 * Represents the visual/validation state of a pin in the circuit UI.
 *
 * Used to indicate whether a pin connection is:
 * - RED: invalid or missing connection
 * - YELLOW: partially connected or incomplete
 * - GREEN: correctly connected and valid
 */
enum class PinColorState {
    RED,
    YELLOW,
    GREEN
}
