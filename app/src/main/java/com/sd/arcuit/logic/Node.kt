package com.sd.arcuit.logic

/**
 * Represents a graph node in the circuit system.
 *
 * Each node carries both:
 * - geometric position (x, y, bounding box)
 * - logical type information (type, wireId, metadata)
 */
data class Node(
    val id: Int,

    // Center position of the detected node
    val x: Float,
    val y: Float,

    // Logical classification of the node (e.g., wire_endpoint, led, ic:7)
    val type: String,

    // Bounding box of the detected object in image space
    val left: Float,
    val top: Float,
    val right: Float,
    val bottom: Float,

    // Optional identifier used to group endpoints belonging to the same wire
    val wireId: String? = null,

    // Stores rail association metadata after detection (pos_rail or neg_rail)
    var metaRailType: String? = null
)
