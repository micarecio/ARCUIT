package com.sd.arcuit.logic

import android.util.Log
import kotlin.math.sqrt

class NetBuilder {

    private val TAG = "NET_DEBUG"
    private val CONNECTION_THRESHOLD = 45f

    fun buildNets(nodes: List<Node>): Map<Int, List<Node>> {

        val uf = UnionFind(nodes.size)

        detectConnections(nodes, uf)

        val nets = groupNets(nodes, uf)

        printNets(nets)

        return nets
    }

    private fun detectConnections(nodes: List<Node>, uf: UnionFind) {

        for (i in nodes.indices) {

            for (j in i + 1 until nodes.size) {

                val a = nodes[i]
                val b = nodes[j]

                // Prevent VCC ↔ GND short
                if (
                    (a.type == "pos_rail" && b.type == "neg_rail") ||
                    (a.type == "neg_rail" && b.type == "pos_rail")
                ) continue

                // Prevent component ↔ component
                if (isComponent(a.type) && isComponent(b.type)) {
                    continue
                }

                val dx = a.x - b.x
                val dy = a.y - b.y

                val distance = sqrt(dx * dx + dy * dy)

                if (distance < CONNECTION_THRESHOLD) {

                    uf.union(i, j)

                    Log.d(
                        TAG,
                        "CONNECT: ${a.type} <-> ${b.type} distance=$distance"
                    )
                }
            }
        }
    }

    private fun isComponent(type: String): Boolean {

        return type == "led"
                || type == "resistor"
                || type == "switch"
                || type == "push_button"
    }

    private fun groupNets(
        nodes: List<Node>,
        uf: UnionFind
    ): Map<Int, List<Node>> {

        val nets = mutableMapOf<Int, MutableList<Node>>()

        for (i in nodes.indices) {

            val root = uf.find(i)

            nets.putIfAbsent(root, mutableListOf())

            nets[root]?.add(nodes[i])
        }

        return nets
    }

    private fun printNets(nets: Map<Int, List<Node>>) {

        var index = 1

        for ((_, nodes) in nets) {

            val types = nodes.map { it.type }

            Log.d(TAG, "NET_$index -> $types")

            index++
        }
    }
}