package com.sd.arcuit.logic

import kotlin.math.abs

/**
 * Detects and assigns connections between IC pins and detected objects
 * (wire endpoints, VCC, GND) based on spatial positioning.
 *
 * The algorithm uses directional constraints:
 * - Forward distance (along expected pin direction)
 * - Side tolerance (alignment perpendicular to direction)
 * - Direction strength (ensures correct orientation)
 */
object PinConnectionDetector {

    // Maximum allowed distance in the forward direction from pin to object
    private const val FORWARD_DISTANCE = 150f
 
    // Maximum allowed sideways deviation from ideal alignment
    private const val SIDE_TOLERANCE = 20f

    // Minimum forward offset to avoid self or near-pin noise detection
    private const val MIN_FORWARD_OFFSET = 4f

    /**
     * Represents a valid pin-to-object connection.
     */
    data class PinConnection(
        val icId: String,
        val pinIndex: Int,
        val objectId: String,
        val objectType: ObjectType,
        val objectX: Float,
        val objectY: Float
    )

    /**
     * Detects valid connections between IC pins and circuit objects.
     */
    fun detect(
        icList: List<ICComponent>,
        objects: List<DetectedObject>
    ): List<PinConnection> {

        val connections = mutableListOf<PinConnection>()

        // Only consider electrically relevant objects
        val validObjects = objects.filter {
            it.type == ObjectType.WIRE_ENDPOINT ||
                    it.type == ObjectType.VCC ||
                    it.type == ObjectType.GND
        }

        for (ic in icList) {
            val icCenterX = ic.boundingBox.centerX()
            val icCenterY = ic.boundingBox.centerY()

            // Determine IC orientation (horizontal vs vertical on screen)
            val isHorizontalIc = ic.boundingBox.width() >= ic.boundingBox.height()

            for (pin in ic.pins) {
                val pinX = pin.point.x
                val pinY = pin.point.y

                // Evaluate each candidate object for connection
                val best = validObjects.mapNotNull { obj ->

                    val objCenterX = (obj.left + obj.right) / 2f
                    val objCenterY = (obj.top + obj.bottom) / 2f

                    if (isHorizontalIc) {

                        // Horizontal IC layout:
                        // Top pins connect upward
                        // Bottom pins connect downward
                        val isTopPin = pinY < icCenterY

                        val forward = if (isTopPin) {
                            pinY - objCenterY
                        } else {
                            objCenterY - pinY
                        }

                        val sideOffset = abs(objCenterX - pinX)

                        val forwardOk = forward in MIN_FORWARD_OFFSET..FORWARD_DISTANCE
                        val sideOk = sideOffset <= SIDE_TOLERANCE

                        // Ensures object is primarily in the forward direction
                        val strongDirection = forward > sideOffset * 2.5f

                        if (!forwardOk || !sideOk || !strongDirection) {
                            null
                        } else {
                            Candidate(
                                obj = obj,
                                centerX = objCenterX,
                                centerY = objCenterY,
                                forward = forward,
                                sideOffset = sideOffset
                            )
                        }

                    } else {

                        // Vertical IC layout:
                        // Left pins connect leftward
                        // Right pins connect rightward
                        val isLeftPin = pinX < icCenterX

                        val forward = if (isLeftPin) {
                            pinX - objCenterX
                        } else {
                            objCenterX - pinX
                        }

                        val sideOffset = abs(objCenterY - pinY)

                        val forwardOk = forward in MIN_FORWARD_OFFSET..FORWARD_DISTANCE
                        val sideOk = sideOffset <= SIDE_TOLERANCE
                        val strongDirection = forward > sideOffset * 2.5f

                        if (!forwardOk || !sideOk || !strongDirection) {
                            null
                        } else {
                            Candidate(
                                obj = obj,
                                centerX = objCenterX,
                                centerY = objCenterY,
                                forward = forward,
                                sideOffset = sideOffset
                            )
                        }
                    }

                    // Select best candidate based on alignment quality
                }.minByOrNull { candidate ->
                    candidate.sideOffset * 8f + candidate.forward
                }

                // Register connection if a valid match is found
                if (best != null) {
                    connections.add(
                        PinConnection(
                            icId = ic.id,
                            pinIndex = pin.index,
                            objectId = best.obj.id,
                            objectType = best.obj.type,
                            objectX = best.centerX,
                            objectY = best.centerY
                        )
                    )
                }
            }
        }

        return connections
    }

    /**
     * Internal candidate used for ranking possible matches.
     */
    private data class Candidate(
        val obj: DetectedObject,
        val centerX: Float,
        val centerY: Float,
        val forward: Float,
        val sideOffset: Float
    )
}
