package com.sd.arcuit.logic

import android.util.Log
import kotlin.math.abs
import kotlin.math.sqrt

class NetBuilder {

    private val TAG = "NET_DEBUG"

    private val RAIL_TO_RAIL_DISTANCE = 35f
    private val ENDPOINT_TO_COMPONENT_DISTANCE = 24f
    private val SAME_RAIL_VERTICAL_TOLERANCE = 20f

    // endpoint -> rail
    private val ENDPOINT_TO_RAIL_THRESHOLD = 28f

    // endpoint -> pin
    private val PIN_MATCH_MAX_DX = 14f
    private val PIN_MATCH_MAX_DY = 120f

    fun buildNets(nodes: List<Node>): Map<Int, List<Node>> {
        if (nodes.isEmpty()) return emptyMap()

        val normalized = nodes.map { normalizeNode(it) }
        val uf = UnionFind(normalized.size)

        detectConnections(normalized, uf)

        val nets = groupNets(normalized, uf)
        printNets(nets)
        printNetSummary(nets)

        return nets
    }

    private fun detectConnections(nodes: List<Node>, uf: UnionFind) {

        // PASS 1:
        // rail ↔ rail
        // endpoint ↔ rail
        // endpoint ↔ component
        for (i in nodes.indices) {
            for (j in i + 1 until nodes.size) {

                val a = nodes[i]
                val b = nodes[j]

                // prevent pos_rail <-> neg_rail
                if ((isPositive(a.type) && isNegative(b.type)) ||
                    (isNegative(a.type) && isPositive(b.type))) {
                    continue
                }

                // prevent component <-> component
                if (isComponent(a.type) && isComponent(b.type)) {
                    continue
                }

                // prevent rail <-> component
                if ((isRail(a.type) && isComponent(b.type)) ||
                    (isComponent(a.type) && isRail(b.type))) {
                    continue
                }

                // prevent IC pin <-> IC pin
                if (isICPin(a.type) && isICPin(b.type)) {
                    continue
                }

                // endpoint <-> pin handled in PASS 3
                if ((isWireEndpoint(a.type) && isICPin(b.type)) ||
                    (isICPin(a.type) && isWireEndpoint(b.type))) {
                    continue
                }

                // DO NOT auto-connect random endpoints by distance here.
                // Same-wire continuity is handled in PASS 2 using wireId.
                if (isWireEndpoint(a.type) && isWireEndpoint(b.type)) {
                    continue
                }

                val distance = distance(a, b)

                val shouldConnect = when {
                    // rail ↔ rail
                    isRail(a.type) && isRail(b.type) -> {
                        canConnectRailToRail(a, b, distance)
                    }

                    // endpoint ↔ rail
                    (isWireEndpoint(a.type) && isRail(b.type)) ||
                            (isRail(a.type) && isWireEndpoint(b.type)) -> {
                        canConnectEndpointToRail(a, b)
                    }

                    // endpoint ↔ component
                    (isWireEndpoint(a.type) && isComponent(b.type)) ||
                            (isComponent(a.type) && isWireEndpoint(b.type)) -> {
                        canConnectEndpointToComponent(distance)
                    }

                    else -> false
                }

                if (shouldConnect) {
                    if (isWireEndpoint(a.type) && isRail(b.type)) {
                        a.metaRailType = b.type
                    }
                    if (isWireEndpoint(b.type) && isRail(a.type)) {
                        b.metaRailType = a.type
                    }

                    uf.union(i, j)
                    Log.d(TAG, "CONNECT: ${a.type}[${a.id}] <-> ${b.type}[${b.id}]")
                }
            }
        }

        // PASS 2:
        // connect only endpoints that belong to the SAME wire
        connectSameWireEndpoints(nodes, uf)

        // PASS 3:
        // connect each endpoint to only ONE nearest IC pin
        connectEndpointsToNearestPins(nodes, uf)
    }

    private fun connectSameWireEndpoints(nodes: List<Node>, uf: UnionFind) {
        val endpoints = nodes.withIndex()
            .filter { it.value.type == "wire_endpoint" && it.value.wireId != null }

        val grouped = endpoints.groupBy { it.value.wireId }

        for ((wireId, group) in grouped) {
            if (group.size == 2) {
                val a = group[0].index
                val b = group[1].index

                uf.union(a, b)

                Log.d(
                    TAG,
                    "WIRE CONNECT: $wireId -> ${group[0].value.id}, ${group[1].value.id}"
                )
            } else {
                Log.d(TAG, "WIRE WARNING: $wireId has ${group.size} endpoints")
            }
        }
    }

    private fun connectEndpointsToNearestPins(nodes: List<Node>, uf: UnionFind) {
        val endpoints = nodes.withIndex().filter { it.value.type == "wire_endpoint" }
        val pins = nodes.withIndex().filter { isICPin(it.value.type) }

        for ((endpointIndex, endpoint) in endpoints) {

            var bestPinIndex = -1
            var bestScore = Float.MAX_VALUE

            for ((pinIndex, pin) in pins) {
                val dx = abs(endpoint.x - pin.x)
                val dy = abs(endpoint.y - pin.y)

                // keep horizontal match tight so adjacent pins do not leak
                if (dx > PIN_MATCH_MAX_DX) continue

                // allow vertical gap across the breadboard / IC row
                if (dy > PIN_MATCH_MAX_DY) continue

                // heavily prioritize horizontal alignment
                val score = dx * 10f + dy

                if (score < bestScore) {
                    bestScore = score
                    bestPinIndex = pinIndex
                }
            }

            if (bestPinIndex != -1) {
                uf.union(endpointIndex, bestPinIndex)

                Log.d(
                    TAG,
                    "PIN MATCH: endpoint[${endpoint.id}] (${endpoint.x}, ${endpoint.y}) -> ${nodes[bestPinIndex].type}[${nodes[bestPinIndex].id}] score=$bestScore"
                )
            } else {
                Log.d(
                    TAG,
                    "NO PIN MATCH: endpoint[${endpoint.id}] (${endpoint.x}, ${endpoint.y})"
                )
            }
        }
    }

    private fun canConnectRailToRail(a: Node, b: Node, distance: Float): Boolean {
        if (a.type != b.type) return false

        val sameBand = abs(a.y - b.y) <= SAME_RAIL_VERTICAL_TOLERANCE
        if (!sameBand) return false

        return distance <= RAIL_TO_RAIL_DISTANCE
    }

    private fun canConnectEndpointToRail(a: Node, b: Node): Boolean {
        val endpoint = if (isWireEndpoint(a.type)) a else b
        val rail = if (isRail(a.type)) a else b

        // nearest point on rail rectangle to endpoint
        val closestX = endpoint.x.coerceIn(rail.left, rail.right)
        val closestY = endpoint.y.coerceIn(rail.top, rail.bottom)

        val dx = endpoint.x - closestX
        val dy = endpoint.y - closestY
        val distance = sqrt(dx * dx + dy * dy)

        if (distance > ENDPOINT_TO_RAIL_THRESHOLD) {
            return false
        }

        // if endpoint was already tagged to one rail, reject opposite rail
        if (endpoint.metaRailType != null && endpoint.metaRailType != rail.type) {
            return false
        }

        return true
    }

    private fun canConnectEndpointToComponent(distance: Float): Boolean {
        return distance <= ENDPOINT_TO_COMPONENT_DISTANCE
    }

    private fun groupNets(nodes: List<Node>, uf: UnionFind): Map<Int, List<Node>> {
        val nets = mutableMapOf<Int, MutableList<Node>>()

        for (i in nodes.indices) {
            val root = uf.find(i)
            nets.getOrPut(root) { mutableListOf() }.add(nodes[i])
        }

        return nets
    }

    private fun printNets(nets: Map<Int, List<Node>>) {
        for ((netId, members) in nets) {
            Log.d(
                TAG,
                "NET_$netId -> ${members.joinToString { "${it.type}[${it.id}]" }}"
            )
        }
    }

    private fun printNetSummary(nets: Map<Int, List<Node>>) {
        for ((netId, members) in nets) {

            val pins = members.filter { isICPin(it.type) }
            val rails = members.filter { isRail(it.type) }
            val endpoints = members.filter { isWireEndpoint(it.type) }

            if (pins.isEmpty()) continue

            val pinNames = pins.map { it.type }
            val hasPos = rails.any { it.type == "pos_rail" }
            val hasNeg = rails.any { it.type == "neg_rail" }

            val status = when {
                hasPos && hasNeg -> "⚠ VCC + GND SHORT"
                hasPos -> "→ VCC"
                hasNeg -> "→ GND"
                else -> "❌ NO RAIL"
            }

            Log.d(
                TAG,
                "SUMMARY: net=$netId pins=${pinNames.joinToString()} status=$status endpoints=${endpoints.size}"
            )
        }
    }

    fun debugPowerPins(nets: Map<Int, List<Node>>) {
        debugSpecificPin(nets, "ic:7")
        debugSpecificPin(nets, "ic:14")
    }

    private fun debugSpecificPin(nets: Map<Int, List<Node>>, pinType: String) {
        val entry = nets.entries.firstOrNull { (_, members) ->
            members.any { it.type == pinType }
        }

        if (entry == null) {
            Log.d(TAG, "PIN DEBUG: $pinType -> NOT FOUND IN ANY NET")
            return
        }

        val members = entry.value
        Log.d(
            TAG,
            "PIN DEBUG: $pinType -> ${members.joinToString { "${it.type}[${it.id}]" }}"
        )
    }

    private fun distance(a: Node, b: Node): Float {
        val dx = a.x - b.x
        val dy = a.y - b.y
        return sqrt(dx * dx + dy * dy)
    }

    private fun normalizeNode(node: Node): Node {
        return node.copy(type = normalizeType(node.type))
    }

    private fun normalizeType(type: String): String {
        return when (type.lowercase()) {
            "vcc" -> "pos_rail"
            "gnd" -> "neg_rail"
            else -> type.lowercase()
        }
    }

    private fun isPositive(type: String) = type == "pos_rail"

    private fun isNegative(type: String) = type == "neg_rail"

    private fun isRail(type: String) =
        isPositive(type) || isNegative(type)

    private fun isWireEndpoint(type: String) =
        type == "wire_endpoint"

    private fun isICPin(type: String) =
        type.contains(":")

    private fun isComponent(type: String): Boolean {
        return type == "led" ||
                type == "resistor" ||
                type == "switch" ||
                type == "push_button"
    }
}