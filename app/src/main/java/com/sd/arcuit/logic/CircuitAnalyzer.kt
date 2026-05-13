package com.sd.arcuit.logic

import android.util.Log

object CircuitAnalyzer {

    /**
     * Main entry point for circuit analysis.
     *
     * Converts detection outputs into a graph of nodes,
     * adds IC pin nodes, builds electrical nets,
     * and logs the resulting connectivity groups.
     */
    fun analyze(
        detections: List<Detection>,
        icBodies: List<ICComponent>
    ) {

        /**
         * Convert model detections into internal object representations.
         * Each detected bounding box becomes a DetectedObject.
         */
        val detectedObjects = detections.mapIndexed { index, d ->
            DetectedObject(
                id = "OBJ_$index",
                type = mapLabelToType(d.label),
                left = d.boundingBox.left,
                top = d.boundingBox.top,
                right = d.boundingBox.right,
                bottom = d.boundingBox.bottom
            )
        }

        /**
         * Convert detected objects into graph nodes.
         * Each node represents the center point of an object.
         */
        val objectNodes = detectedObjects.mapIndexed { index, obj ->
            Node(
                id = index,
                x = (obj.left + obj.right) / 2f,
                y = (obj.top + obj.bottom) / 2f,
                type = mapObjectTypeToNodeLabel(obj.type),
                left = obj.left,
                top = obj.top,
                right = obj.right,
                bottom = obj.bottom
            )
        }

        /**
         * Offset used to avoid ID collisions between object nodes and pin nodes.
         */
        val startId = objectNodes.size

        /**
         * Create nodes for IC pins.
         * Each pin is treated as a connection point in the circuit graph.
         */
        val pinNodes = icBodies.flatMapIndexed { icIndex, ic ->
            ic.pins.mapIndexed { pinIndex, pin ->
                Node(
                    id = startId + icIndex * 100 + pinIndex,
                    x = pin.point.x,
                    y = pin.point.y,
                    type = "${ic.id}:${pin.index}",
                    left = pin.point.x - 4f,
                    top = pin.point.y - 4f,
                    right = pin.point.x + 4f,
                    bottom = pin.point.y + 4f
                )
            }
        }

        // Combine all nodes into a single graph input
        val nodes = objectNodes + pinNodes

        /**
         * Build electrical networks (nets) from connected nodes.
         */
        val nets = NetBuilder().buildNets(nodes)

        /**
         * Debug output: print each net and its connected components.
         */
        nets.forEach { (id, nodeList) ->
            Log.d("NET", "NET_$id")
            nodeList.forEach { node ->
                Log.d("NET", " ${node.type} (${node.x}, ${node.y})")
            }
        }
    }

    /**
     * Maps model label strings to internal ObjectType enum.
     */
    private fun mapLabelToType(label: String): ObjectType {
        return when (label.lowercase()) {
            "ic_body" -> ObjectType.IC_BODY
            "wire_endpoint" -> ObjectType.WIRE_ENDPOINT
            "led" -> ObjectType.LED
            "resistor" -> ObjectType.RESISTOR
            "push_button" -> ObjectType.PUSH_BUTTON
            "switch" -> ObjectType.SWITCH
            "pos_rail" -> ObjectType.VCC
            "neg_rail" -> ObjectType.GND
            else -> ObjectType.WIRE_ENDPOINT
        }
    }

    /**
     * Converts ObjectType back into string labels used by graph nodes.
     */
    private fun mapObjectTypeToNodeLabel(type: ObjectType): String {
        return when (type) {
            ObjectType.IC_BODY -> "ic_body"
            ObjectType.WIRE_ENDPOINT -> "wire_endpoint"
            ObjectType.LED -> "led"
            ObjectType.RESISTOR -> "resistor"
            ObjectType.PUSH_BUTTON -> "push_button"
            ObjectType.SWITCH -> "switch"
            ObjectType.VCC -> "pos_rail"
            ObjectType.GND -> "neg_rail"
        }
    }
}
