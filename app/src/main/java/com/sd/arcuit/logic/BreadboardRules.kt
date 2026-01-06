package com.sd.arcuit.logic

object BreadboardRules {

    /**
     * Returns true if two objects are electrically connected
     * based on spatial proximity and breadboard topology.
     */
    fun areConnected(a: DetectedObject, b: DetectedObject): Boolean {
        return sameRow(a, b) || touching(a, b)
    }

    /**
     * A–E are connected, F–J are connected.
     * The gap breaks connectivity.
     */
    private fun sameRow(a: DetectedObject, b: DetectedObject): Boolean {
        val rowTolerance = 15f

        val aCenterY = (a.top + a.bottom) / 2
        val bCenterY = (b.top + b.bottom) / 2

        return kotlin.math.abs(aCenterY - bCenterY) < rowTolerance
    }

    /**
     * For cases like:
     * wire touching IC pin
     * wire touching LED leg
     */
    private fun touching(a: DetectedObject, b: DetectedObject): Boolean {
        val tolerance = 20f

        return a.left < b.right + tolerance &&
                a.right > b.left - tolerance &&
                a.top < b.bottom + tolerance &&
                a.bottom > b.top - tolerance
    }
}
