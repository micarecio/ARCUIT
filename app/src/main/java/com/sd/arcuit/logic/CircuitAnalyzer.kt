package com.sd.arcuit.logic

import android.util.Log

object CircuitAnalyzer {

    fun analyze(
        detections: List<Detection>,
        icBodies: List<ICComponent>
    ) {

        // Convert YOLO detections → DetectedObject
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

        // Build connection points
        val connectionPoints = detectedObjects.map { obj ->

            ConnectionPoint(
                label = obj.type.name.lowercase(),
                x = (obj.left + obj.right) / 2f,
                y = (obj.top + obj.bottom) / 2f
            )
        }

        // Convert connectionPoints → Nodes
        val nodes = connectionPoints.mapIndexed { index, pt ->

            Node(
                id = index,
                x = pt.x,
                y = pt.y,
                type = pt.label
            )
        }

        // 4️⃣ Build nets
        val nets = NetBuilder().buildNets(nodes)

        // 5️⃣ Debug print
        nets.forEach { (id, nodeList) ->

            Log.d("NET", "NET_$id")

            nodeList.forEach { node ->

                Log.d(
                    "NET",
                    " ${node.type} (${node.x}, ${node.y})"
                )
            }
        }
    }

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
}