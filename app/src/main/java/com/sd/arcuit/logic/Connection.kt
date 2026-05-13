package com.sd.arcuit.logic

/**
 * Represents a logical connection between two nodes in the circuit graph.
 *
 * Each connection defines an edge between:
 * - a source node (fromId)
 * - a target node (toId)
 *
 * Used for building and analyzing circuit nets.
 */
data class Connection(
    val fromId: String,
    val toId: String
)
