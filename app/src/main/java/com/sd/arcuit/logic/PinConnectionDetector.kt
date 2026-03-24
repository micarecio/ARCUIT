package com.sd.arcuit.logic

import kotlin.math.abs

object PinConnectionDetector {

    private const val FORWARD_DISTANCE = 110f
    private const val SIDE_TOLERANCE = 22f
    private const val MIN_FORWARD_OFFSET = 6f

    data class PinConnection(
        val icId: String,
        val pinIndex: Int,
        val objectId: String,
        val objectType: ObjectType,
        val objectX: Float,
        val objectY: Float
    )

    fun detect(
        icList: List<ICComponent>,
        objects: List<DetectedObject>
    ): List<PinConnection> {

        val connections = mutableListOf<PinConnection>()

        val validObjects = objects.filter {
            it.type == ObjectType.WIRE_ENDPOINT ||
                    it.type == ObjectType.VCC ||
                    it.type == ObjectType.GND
        }

        for (ic in icList) {
            val icCenterX = ic.boundingBox.centerX()
            val icCenterY = ic.boundingBox.centerY()
            val isHorizontalIc = ic.boundingBox.width() >= ic.boundingBox.height()

            for (pin in ic.pins) {
                val pinX = pin.point.x
                val pinY = pin.point.y

                val best = validObjects.mapNotNull { obj ->
                    val objCenterX = (obj.left + obj.right) / 2f
                    val objCenterY = (obj.top + obj.bottom) / 2f

                    if (isHorizontalIc) {
                        // Horizontal IC on screen:
                        // top pins -> upward only
                        // bottom pins -> downward only
                        val isTopPin = pinY < icCenterY

                        val forward = if (isTopPin) {
                            pinY - objCenterY
                        } else {
                            objCenterY - pinY
                        }

                        val sideOffset = abs(objCenterX - pinX)

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
                    } else {
                        // Vertical IC on screen:
                        // left pins -> left only
                        // right pins -> right only
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
                }.minByOrNull { candidate ->
                    candidate.sideOffset * 8f + candidate.forward
                }

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

    private data class Candidate(
        val obj: DetectedObject,
        val centerX: Float,
        val centerY: Float,
        val forward: Float,
        val sideOffset: Float
    )
}