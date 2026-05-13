package com.sd.arcuit.logic

import kotlin.math.abs
import kotlin.math.sqrt

/**
 * Builds electrical nets by analyzing spatial relationships between detected nodes.
 *
 * This class performs multi-pass connection analysis:
 * 1. Rail and component connectivity
 * 2. Wire continuity (same wire endpoints)
 * 3. Wire endpoint to IC pin mapping
 */
class NetBuilder {

    private val TAG = "NET_DEBUG"

    // Maximum allowed distances for different connection types
    private val RAIL_TO_RAIL_DISTANCE = 35f
    private val ENDPOINT_TO_COMPONENT_DISTANCE = 24f
    private val SAME_RAIL_VERTICAL_TOLERANCE = 20f

    // Endpoint to rail connection threshold
    private val ENDPOINT_TO_RAIL_THRESHOLD = 28f

    // IC pin matching constraints for endpoint connection
    private val PIN_MATCH_MAX_DX = 14f
    private val PIN_MATCH_MAX_DY = 120f

    /**
     * Main entry point for building nets from detected nodes.
     */
    fun buildNets(nodes: List<Node>): Map<Int, List<Node>> {
        if (nodes.isEmpty()) return emptyMap()

        val normalized = nodes.map { normalizeNode(it) }
        val uf = UnionFind(normalized.size)

        detectConnections(normalized, uf)

        val nets = groupNets(normalized, uf)

        return nets
    }

    /**
     * Detects all valid connections between nodes using multiple passes.
     */
    private fun detectConnections(nodes: List<Node>, uf: UnionFind) {

        // PASS 1:
        // Handles:
        // - rail ↔ rail
        // - endpoint ↔ rail
        // - endpoint ↔ component
        for (i in nodes.indices) {
            for (j in i + 1 until nodes.size) {

                val a = nodes[i]
                val b = nodes[j]

                // Prevent direct short between power rails
                if ((isPositive(a.type) && isNegative(b.type)) ||
                    (isNegative(a.type) && isPositive(b.type))) {
                    continue
                }

                // Prevent component-to-component merging
                if (isComponent(a.type) && isComponent(b.type)) {
                    continue
                }

                // Prevent rail-to-component direct connection
                if ((isRail(a.type) && isComponent(b.type)) ||
                    (isComponent(a.type) && isRail(b.type))) {
                    continue
                }

                // IC pin-to-pin connections are not handled here
                if (isICPin(a.type) && isICPin(b.type)) {
                    continue
                }

                // Endpoint-to-pin handled in PASS 3
                if ((isWireEndpoint(a.type) && isICPin(b.type)) ||
                    (isICPin(a.type) && isWireEndpoint(b.type))) {
                    continue
                }

                // Wire continuity handled separately in PASS 2
                if (isWireEndpoint(a.type) && isWireEndpoint(b.type)) {
                    continue
                }

                val distance = distance(a, b)

                val shouldConnect = when {

                    // Rail to rail connection rules
                    isRail(a.type) && isRail(b.type) -> {
                        canConnectRailToRail(a, b, distance)
                    }

                    // Endpoint to rail connection rules
                    (isWireEndpoint(a.type) && isRail(b.type)) ||
                            (isRail(a.type) && isWireEndpoint(b.type)) -> {
                        canConnectEndpointToRail(a, b)
                    }

                    // Endpoint to component connection rules
                    (isWireEndpoint(a.type) && isComponent(b.type)) ||
                            (isComponent(a.type) && isWireEndpoint(b.type)) -> {
                        canConnectEndpointToComponent(distance)
                    }

                    else -> false
                }

                if (shouldConnect) {
                    // Store rail association metadata
                    if (isWireEndpoint(a.type) && isRail(b.type)) {
                        a.metaRailType = b.type
                    }
                    if (isWireEndpoint(b.type) && isRail(a.type)) {
                        b.metaRailType = a.type
                    }

                    uf.union(i, j)
                }
            }
        }

        // PASS 2:
        // Connect endpoints belonging to the same physical wire
        connectSameWireEndpoints(nodes, uf)

        // PASS 3:
        // Connect each endpoint to the nearest valid IC pin
        connectEndpointsToNearestPins(nodes, uf)
    }

    /**
     * Connects wire endpoints that belong to the same wire ID.
     */
    private fun connectSameWireEndpoints(nodes: List<Node>, uf: UnionFind) {
        val endpoints = nodes.withIndex()
            .filter { it.value.type == "wire_endpoint" && it.value.wireId != null }

        val grouped = endpoints.groupBy { it.value.wireId }

        for ((wireId, group) in grouped) {
            if (group.size == 2) {
                val a = group[0].index
                val b = group[1].index

                uf.union(a, b)
            }
        }
    }

    /**
     * Matches wire endpoints to the closest IC pin based on spatial scoring.
     */
    private fun connectEndpointsToNearestPins(nodes: List<Node>, uf: UnionFind) {
        val endpoints = nodes.withIndex().filter { it.value.type == "wire_endpoint" }
        val pins = nodes.withIndex().filter { isICPin(it.value.type) }

        for ((endpointIndex, endpoint) in endpoints) {

            var bestPinIndex = -1
            var bestScore = Float.MAX_VALUE

            for ((pinIndex, pin) in pins) {
                val dx = abs(endpoint.x - pin.x)
                val dy = abs(endpoint.y - pin.y)

                // Reject horizontally misaligned pins
                if (dx > PIN_MATCH_MAX_DX) continue

                // Reject vertically too far pins
                if (dy > PIN_MATCH_MAX_DY) continue

                val score = dx * 10f + dy

                if (score < bestScore) {
                    bestScore = score
                    bestPinIndex = pinIndex
                }
            }

            if (bestPinIndex != -1) {
                uf.union(endpointIndex, bestPinIndex)
            }
        }
    }

    /**
     * Determines if two rails can be connected.
     */
    private fun canConnectRailToRail(a: Node, b: Node, distance: Float): Boolean {
        if (a.type != b.type) return false

        val sameBand = abs(a.y - b.y) <= SAME_RAIL_VERTICAL_TOLERANCE
        if (!sameBand) return false

        return distance <= RAIL_TO_RAIL_DISTANCE
    }

    /**
     * Determines if a wire endpoint can connect to a rail.
     */
    private fun canConnectEndpointToRail(a: Node, b: Node): Boolean {
        val endpoint = if (isWireEndpoint(a.type)) a else b
        val rail = if (isRail(a.type)) a else b

        val closestX = endpoint.x.coerceIn(rail.left, rail.right)
        val closestY = endpoint.y.coerceIn(rail.top, rail.bottom)

        val dx = endpoint.x - closestX
        val dy = endpoint.y - closestY
        val distance = sqrt(dx * dx + dy * dy)

        if (distance > ENDPOINT_TO_RAIL_THRESHOLD) {
            return false
        }

        if (endpoint.metaRailType != null && endpoint.metaRailType != rail.type) {
            return false
        }

        return true
    }

    /**
     * Determines if endpoint can connect to a component.
     */
    private fun canConnectEndpointToComponent(distance: Float): Boolean {
        return distance <= ENDPOINT_TO_COMPONENT_DISTANCE
    }

    /**
     * Groups nodes into nets using Union-Find results.
     */
    private fun groupNets(nodes: List<Node>, uf: UnionFind): Map<Int, List<Node>> {
        val nets = mutableMapOf<Int, MutableList<Node>>()

        for (i in nodes.indices) {
            val root = uf.find(i)
            nets.getOrPut(root) { mutableListOf() }.add(nodes[i])
        }

        return nets
    }

    /**
     * Debug helper for power pins.
     */
    fun debugPowerPins(nets: Map<Int, List<Node>>) {
        debugSpecificPin(nets, "ic:7")
        debugSpecificPin(nets, "ic:14")
    }

    private fun debugSpecificPin(nets: Map<Int, List<Node>>, pinType: String) {
        val entry = nets.entries.firstOrNull { (_, members) ->
            members.any { it.type == pinType }
        }
    }

    /**
     * Calculates Euclidean distance between two nodes.
     */
    private fun distance(a: Node, b: Node): Float {
        val dx = a.x - b.x
        val dy = a.y - b.y
        return sqrt(dx * dx + dy * dy)
    }

    /**
     * Normalizes node type naming.
     */
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
