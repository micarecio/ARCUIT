package com.sd.arcuit.logic

data class GateGroup(
    val inputPins: List<Int>,
    val outputPin: Int
)

object ICGateGroups {

    val DIP14 = mapOf(

        // 7400 – Quad 2-input NAND
        "7400" to listOf(
            GateGroup(inputPins = listOf(1, 2), outputPin = 3),
            GateGroup(inputPins = listOf(4, 5), outputPin = 6),
            GateGroup(inputPins = listOf(9, 10), outputPin = 8),
            GateGroup(inputPins = listOf(12, 13), outputPin = 11)
        ),

        // 7408 – Quad 2-input AND
        "7408" to listOf(
            GateGroup(inputPins = listOf(1, 2), outputPin = 3),
            GateGroup(inputPins = listOf(4, 5), outputPin = 6),
            GateGroup(inputPins = listOf(9, 10), outputPin = 8),
            GateGroup(inputPins = listOf(12, 13), outputPin = 11)
        ),

        // 7432 – Quad 2-input OR
        "7432" to listOf(
            GateGroup(inputPins = listOf(1, 2), outputPin = 3),
            GateGroup(inputPins = listOf(4, 5), outputPin = 6),
            GateGroup(inputPins = listOf(9, 10), outputPin = 8),
            GateGroup(inputPins = listOf(12, 13), outputPin = 11)
        ),

        // 7486 – Quad 2-input XOR
        "7486" to listOf(
            GateGroup(inputPins = listOf(1, 2), outputPin = 3),
            GateGroup(inputPins = listOf(4, 5), outputPin = 6),
            GateGroup(inputPins = listOf(9, 10), outputPin = 8),
            GateGroup(inputPins = listOf(12, 13), outputPin = 11)
        ),

        // 7403 – Quad 2-input NAND
        "7403" to listOf(
            GateGroup(inputPins = listOf(1, 2), outputPin = 3),
            GateGroup(inputPins = listOf(4, 5), outputPin = 6),
            GateGroup(inputPins = listOf(9, 10), outputPin = 8),
            GateGroup(inputPins = listOf(12, 13), outputPin = 11)
        ),

        // 7409 – Quad 2-input AND
        "7409" to listOf(
            GateGroup(inputPins = listOf(1, 2), outputPin = 3),
            GateGroup(inputPins = listOf(4, 5), outputPin = 6),
            GateGroup(inputPins = listOf(9, 10), outputPin = 8),
            GateGroup(inputPins = listOf(12, 13), outputPin = 11)
        ),

        // 7424 – Quad 2-input NAND
        "7424" to listOf(
            GateGroup(inputPins = listOf(1, 2), outputPin = 3),
            GateGroup(inputPins = listOf(4, 5), outputPin = 6),
            GateGroup(inputPins = listOf(9, 10), outputPin = 8),
            GateGroup(inputPins = listOf(12, 13), outputPin = 11)
        ),

        // 7426 – Quad 2-input NAND
        "7426" to listOf(
            GateGroup(inputPins = listOf(1, 2), outputPin = 3),
            GateGroup(inputPins = listOf(4, 5), outputPin = 6),
            GateGroup(inputPins = listOf(9, 10), outputPin = 8),
            GateGroup(inputPins = listOf(12, 13), outputPin = 11)
        ),

        // 7437 – Quad 2-input NAND
        "7437" to listOf(
            GateGroup(inputPins = listOf(1, 2), outputPin = 3),
            GateGroup(inputPins = listOf(4, 5), outputPin = 6),
            GateGroup(inputPins = listOf(9, 10), outputPin = 8),
            GateGroup(inputPins = listOf(12, 13), outputPin = 11)
        ),

        // 7438 – Quad 2-input NAND
        "7438" to listOf(
            GateGroup(inputPins = listOf(1, 2), outputPin = 3),
            GateGroup(inputPins = listOf(4, 5), outputPin = 6),
            GateGroup(inputPins = listOf(9, 10), outputPin = 8),
            GateGroup(inputPins = listOf(12, 13), outputPin = 11)
        ),

        // 74132 – Quad 2-input NAND
        "74132" to listOf(
            GateGroup(inputPins = listOf(1, 2), outputPin = 3),
            GateGroup(inputPins = listOf(4, 5), outputPin = 6),
            GateGroup(inputPins = listOf(9, 10), outputPin = 8),
            GateGroup(inputPins = listOf(12, 13), outputPin = 11)
        ),

        // 74136 – Quad 2-input NAND
        "74136" to listOf(
            GateGroup(inputPins = listOf(1, 2), outputPin = 3),
            GateGroup(inputPins = listOf(4, 5), outputPin = 6),
            GateGroup(inputPins = listOf(9, 10), outputPin = 8),
            GateGroup(inputPins = listOf(12, 13), outputPin = 11)
        )
    )
}