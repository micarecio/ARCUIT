package com.sd.arcuit.logic

import kotlin.math.hypot
import kotlin.math.max
import kotlin.math.min

object PinConnectionDetector {

    private const val PIN_RADIUS = 28f

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
            it.type != ObjectType.IC_BODY
        }

        for (ic in icList) {
            for (pin in ic.pins) {

                val nearest = validObjects.minByOrNull { obj ->
                    distanceToNearestPoint(pin.point.x, pin.point.y, obj)
                }

                if (nearest != null) {
                    val dist = distanceToNearestPoint(pin.point.x, pin.point.y, nearest)

                    if (dist <= PIN_RADIUS) {
                        connections.add(
                            PinConnection(
                                icId = ic.id,
                                pinIndex = pin.index,
                                objectId = nearest.id,
                                objectType = nearest.type,
                                objectX = (nearest.left + nearest.right) / 2f,
                                objectY = (nearest.top + nearest.bottom) / 2f
                            )
                        )
                    }
                }
            }
        }

        return connections
    }

    private fun distanceToNearestPoint(
        px: Float,
        py: Float,
        obj: DetectedObject
    ): Float {

        val nearestX = px.coerceIn(obj.left, obj.right)
        val nearestY = py.coerceIn(obj.top, obj.bottom)

        return hypot(px - nearestX, py - nearestY)
    }
}