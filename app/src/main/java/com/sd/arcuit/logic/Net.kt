package com.sd.arcuit.logic

data class Net(
    val id: String,
    val points: MutableList<ConnectionPoint>
)

data class ConnectionPoint(
    var label: String,
    var x: Float,
    var y: Float
)
