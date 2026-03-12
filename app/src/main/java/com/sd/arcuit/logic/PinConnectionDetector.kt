package com.sd.arcuit.logic

import kotlin.math.hypot

object PinConnectionDetector {

    private const val PIN_RADIUS = 45f

    data class PinConnection(
        val icId: String,
        val pinIndex: Int,
        val objectId: String  // <- changed from Int to String
    )

    fun detect(
        icList: List<ICComponent>,
        objects: List<DetectedObject>
    ): List<PinConnection> {

        val connections = mutableListOf<PinConnection>()

        for (ic in icList) {
            for (pin in ic.pins) {

                val nearest = objects
                    .filter { it.type != ObjectType.IC_BODY }
                    .minByOrNull { obj ->
                        distance(pin.point.x, pin.point.y, obj)
                    }

                if (nearest != null) {
                    val dist = distance(pin.point.x, pin.point.y, nearest)

                    if (dist < PIN_RADIUS) {
                        connections.add(
                            PinConnection(
                                icId = ic.id,
                                pinIndex = pin.index,
                                objectId = nearest.id
                            )
                        )
                    }
                }
            }
        }

        return connections
    }

    private fun distance(
        px: Float,
        py: Float,
        obj: DetectedObject
    ): Float {

        val cx = (obj.left + obj.right) / 2f
        val cy = (obj.top + obj.bottom) / 2f

        return hypot(px - cx, py - cy)
    }
}
