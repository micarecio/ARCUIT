package com.sd.arcuit.logic

object BreadboardRules {

    /**
     * Determines whether two detected objects are electrically connected.
     *
     * Connection is based on:
     * - being in the same breadboard row
     * - or physically touching within tolerance
     */
    fun areConnected(a: DetectedObject, b: DetectedObject): Boolean {
        return sameRow(a, b) || touching(a, b)
    }

    /**
     * Checks if two objects are on the same breadboard row.
     *
     * Assumes that components aligned vertically within a small
     * tolerance belong to the same electrical row.
     */
    private fun sameRow(a: DetectedObject, b: DetectedObject): Boolean {
        val rowTolerance = 15f

        val aCenterY = (a.top + a.bottom) / 2
        val bCenterY = (b.top + b.bottom) / 2

        return kotlin.math.abs(aCenterY - bCenterY) < rowTolerance
    }

    /**
     * Checks if two objects are physically touching or overlapping.
     *
     * Used for detecting direct connections such as:
     * - wire to IC pin
     * - wire to LED leg
     * - component-to-component contact
     */
    private fun touching(a: DetectedObject, b: DetectedObject): Boolean {
        val tolerance = 20f

        return a.left < b.right + tolerance &&
                a.right > b.left - tolerance &&
                a.top < b.bottom + tolerance &&
                a.bottom > b.top - tolerance
    }
}
