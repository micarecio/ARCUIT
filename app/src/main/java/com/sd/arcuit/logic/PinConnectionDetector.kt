package com.sd.arcuit.logic

import kotlin.math.abs

object PinConnectionDetector {

    private const val FORWARD_DISTANCE = 100f
    private const val SIDE_TOLERANCE = 18f
    private const val MIN_FORWARD_OFFSET = 4f

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
            val icWidth = ic.boundingBox.width()
            val icHeight = ic.boundingBox.height()

            val isVerticalIc = icHeight > icWidth

            for (pin in ic.pins) {
                val pinX = pin.point.x
                val pinY = pin.point.y

                val candidates = validObjects.mapNotNull { obj ->
                    val objCenterX = (obj.left + obj.right) / 2f
                    val objCenterY = (obj.top + obj.bottom) / 2f

                    if (isVerticalIc) {
                        // IC is vertical on screen -> pins are left/right columns
                        val isLeftColumn = pinX < icCenterX

                        val forward = if (isLeftColumn) {
                            pinX - objCenterX
                        } else {
                            objCenterX - pinX
                        }

                        val sideOffset = abs(objCenterY - pinY)
                        val forwardOk = forward >= MIN_FORWARD_OFFSET && forward <= FORWARD_DISTANCE
                        val sideOk = sideOffset <= SIDE_TOLERANCE
                        val directionalDominant = forward > sideOffset * 2f

                        if (!forwardOk || !sideOk || !directionalDominant) {
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
                        // IC is horizontal on screen -> pins are top/bottom rows
                        val isTopRow = pinY < icCenterY

                        val forward = if (isTopRow) {
                            pinY - objCenterY
                        } else {
                            objCenterY - pinY
                        }

                        val sideOffset = abs(objCenterX - pinX)
                        val forwardOk = forward >= MIN_FORWARD_OFFSET && forward <= FORWARD_DISTANCE
                        val sideOk = sideOffset <= SIDE_TOLERANCE
                        val directionalDominant = forward > sideOffset * 2f

                        if (!forwardOk || !sideOk || !directionalDominant) {
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
                }

                val best = candidates.minByOrNull { candidate ->
                    candidate.sideOffset * 5f + candidate.forward
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