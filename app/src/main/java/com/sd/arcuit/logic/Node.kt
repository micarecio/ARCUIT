package com.sd.arcuit.logic

data class Node(
    val id: Int,
    val x: Float,
    val y: Float,
    val type: String,
    val left: Float,
    val top: Float,
    val right: Float,
    val bottom: Float,

    val wireId: String? = null,

    // ✅ ADD THIS
    var metaRailType: String? = null
)