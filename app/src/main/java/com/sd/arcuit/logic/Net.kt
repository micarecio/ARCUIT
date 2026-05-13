package com.sd.arcuit.logic

/**
 * Represents an electrical net in the circuit graph.
 *
 * A net is a collection of connected points that are electrically linked.
 * Each net groups together all nodes that share connectivity.
 */
data class Net(
    val id: String,
    val points: MutableList<ConnectionPoint>
)

/**
 * Represents a single connection point in the circuit graph.
 *
 * Each point has:
 * - a label identifying the component/pin
 * - x and y coordinates in image space
 *
 * Used as a node inside a Net.
 */
data class ConnectionPoint(
    var label: String,
    var x: Float,
    var y: Float
)
