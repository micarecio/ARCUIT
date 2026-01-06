package com.sd.arcuit.logic

object ConnectionBuilder {

    fun build(objects: List<DetectedObject>): List<Connection> {
        val connections = mutableListOf<Connection>()

        for (i in objects.indices) {
            for (j in i + 1 until objects.size) {
                val a = objects[i]
                val b = objects[j]

                if (BreadboardRules.areConnected(a, b)) {
                    connections.add(Connection(a.id, b.id))
                    connections.add(Connection(b.id, a.id))
                }
            }
        }

        return connections
    }
}
