package com.sd.arcuit.logic

data class GateGroup(
    val inputPins: List<Int>,
    val outputPin: Int
)

object ICGateGroups {

    // Common presets
    private fun quad2(): List<GateGroup> = listOf(
        GateGroup(inputPins = listOf(1, 2), outputPin = 3),
        GateGroup(inputPins = listOf(4, 5), outputPin = 6),
        GateGroup(inputPins = listOf(9, 10), outputPin = 8),
        GateGroup(inputPins = listOf(12, 13), outputPin = 11)
    )

    // For 7401 / 7402 / 7428 / 7433 style layout from your ICPinMaps
    private fun quad2Alt(): List<GateGroup> = listOf(
        GateGroup(inputPins = listOf(2, 3), outputPin = 1),
        GateGroup(inputPins = listOf(5, 6), outputPin = 4),
        GateGroup(inputPins = listOf(9, 10), outputPin = 8),
        GateGroup(inputPins = listOf(11, 12), outputPin = 13)
    )

    private fun hexInverter(): List<GateGroup> = listOf(
        GateGroup(inputPins = listOf(1), outputPin = 2),
        GateGroup(inputPins = listOf(3), outputPin = 4),
        GateGroup(inputPins = listOf(5), outputPin = 6),
        GateGroup(inputPins = listOf(9), outputPin = 8),
        GateGroup(inputPins = listOf(11), outputPin = 10),
        GateGroup(inputPins = listOf(13), outputPin = 12)
    )

    // 7410 / 7411 / 7412 / 7415 / 7427 style from your ICPinMaps
    private fun triple3(): List<GateGroup> = listOf(
        GateGroup(inputPins = listOf(1, 2, 13), outputPin = 12),
        GateGroup(inputPins = listOf(3, 4, 5), outputPin = 6),
        GateGroup(inputPins = listOf(9, 10, 11), outputPin = 8)
    )

    // 7413 / 7418 / 7420 / 7421 / 7422 / 7440 style from your ICPinMaps
    private fun dual4(): List<GateGroup> = listOf(
        GateGroup(inputPins = listOf(1, 2, 4, 5), outputPin = 6),
        GateGroup(inputPins = listOf(9, 10, 12, 13), outputPin = 8)
    )

    // 7425 uses 6 inputs total per your ICPinMaps layout
    private fun dual3PlusEnable(): List<GateGroup> = listOf(
        GateGroup(inputPins = listOf(1, 2, 3, 4, 5), outputPin = 6),
        GateGroup(inputPins = listOf(9, 10, 11, 12, 13), outputPin = 8)
    )

    private fun single8(): List<GateGroup> = listOf(
        GateGroup(inputPins = listOf(1, 2, 3, 4, 5, 6, 12, 13), outputPin = 8)
    )

    val DIP14 = mapOf(

        // Quad 2-input standard layout
        "7400" to quad2(),
        "7403" to quad2(),
        "7408" to quad2(),
        "7409" to quad2(),
        "7424" to quad2(),
        "7426" to quad2(),
        "7432" to quad2(),
        "7437" to quad2(),
        "7438" to quad2(),
        "7486" to quad2(),
        "74132" to quad2(),
        "74136" to quad2(),
        "747001" to quad2(),
        "747002" to quad2(),

        // Quad 2-input alternate layout
        "7401" to quad2Alt(),
        "7402" to quad2Alt(),
        "7428" to quad2Alt(),
        "7433" to quad2Alt(),

        // Hex inverter / buffer style
        "7404" to hexInverter(),
        "7405" to hexInverter(),
        "7406" to hexInverter(),
        "7407" to hexInverter(),
        "7414" to hexInverter(),
        "7416" to hexInverter(),
        "7417" to hexInverter(),
        "7419" to hexInverter(),

        // Triple 3-input gates
        "7410" to triple3(),
        "7411" to triple3(),
        "7412" to triple3(),
        "7415" to triple3(),
        "7427" to triple3(),

        // Dual 4-input gates
        "7413" to dual4(),
        "7418" to dual4(),
        "7420" to dual4(),
        "7421" to dual4(),
        "7422" to dual4(),
        "7440" to dual4(),

        // Dual multi-input NOR with enable-style layout from your map
        "7425" to dual3PlusEnable(),

        // Single 8-input gate
        "7430" to single8(),

        // XNOR from your current ICPinMaps layout
        "74266" to listOf(
            GateGroup(inputPins = listOf(1, 2), outputPin = 3),
            GateGroup(inputPins = listOf(5, 6), outputPin = 4),
            GateGroup(inputPins = listOf(9, 10), outputPin = 11),
            GateGroup(inputPins = listOf(12, 13), outputPin = 8)
        ),
        "747266" to listOf(
            GateGroup(inputPins = listOf(1, 2), outputPin = 3),
            GateGroup(inputPins = listOf(5, 6), outputPin = 4),
            GateGroup(inputPins = listOf(9, 10), outputPin = 11),
            GateGroup(inputPins = listOf(12, 13), outputPin = 8)
        )
    )
}