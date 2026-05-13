package com.sd.arcuit.logic

/**
 * Represents a logical gate group inside an IC.
 *
 * Each group defines:
 * - input pins used by the gate
 * - output pin produced by the gate
 */
data class GateGroup(
    val inputPins: List<Int>,
    val outputPin: Int
)

/**
 * Predefined pin group mappings for common DIP-14 ICs.
 *
 * These mappings define how pins are logically grouped
 * into functional gates (AND, OR, NAND, etc.).
 */
object ICGateGroups {

    /**
     * Standard quad 2-input gate layout (common 74xx ICs).
     */
    private fun quad2(): List<GateGroup> = listOf(
        GateGroup(inputPins = listOf(1, 2), outputPin = 3),
        GateGroup(inputPins = listOf(4, 5), outputPin = 6),
        GateGroup(inputPins = listOf(9, 10), outputPin = 8),
        GateGroup(inputPins = listOf(12, 13), outputPin = 11)
    )

    /**
     * Alternate quad 2-input layout for specific IC variants.
     */
    private fun quad2Alt(): List<GateGroup> = listOf(
        GateGroup(inputPins = listOf(2, 3), outputPin = 1),
        GateGroup(inputPins = listOf(5, 6), outputPin = 4),
        GateGroup(inputPins = listOf(8, 9), outputPin = 10),
        GateGroup(inputPins = listOf(11, 12), outputPin = 13)
    )

    /**
     * Hex inverter / buffer layout (6 single-input gates).
     */
    private fun hexInverter(): List<GateGroup> = listOf(
        GateGroup(inputPins = listOf(1), outputPin = 2),
        GateGroup(inputPins = listOf(3), outputPin = 4),
        GateGroup(inputPins = listOf(5), outputPin = 6),
        GateGroup(inputPins = listOf(9), outputPin = 8),
        GateGroup(inputPins = listOf(11), outputPin = 10),
        GateGroup(inputPins = listOf(13), outputPin = 12)
    )

    /**
     * Triple 3-input gate layout (common in NAND/NOR variants).
     */
    private fun triple3(): List<GateGroup> = listOf(
        GateGroup(inputPins = listOf(1, 2, 13), outputPin = 12),
        GateGroup(inputPins = listOf(3, 4, 5), outputPin = 6),
        GateGroup(inputPins = listOf(9, 10, 11), outputPin = 8)
    )

    /**
     * Dual 4-input gate layout.
     */
    private fun dual4(): List<GateGroup> = listOf(
        GateGroup(inputPins = listOf(1, 2, 4, 5), outputPin = 6),
        GateGroup(inputPins = listOf(9, 10, 12, 13), outputPin = 8)
    )

    /**
     * Multi-input gate variant used in specific ICs with enable-like structure.
     */
    private fun dual3PlusEnable(): List<GateGroup> = listOf(
        GateGroup(inputPins = listOf(1, 2, 3, 4, 5), outputPin = 6),
        GateGroup(inputPins = listOf(9, 10, 11, 12, 13), outputPin = 8)
    )

    /**
     * Single large gate IC layout (8-input gate style).
     */
    private fun single8(): List<GateGroup> = listOf(
        GateGroup(inputPins = listOf(1, 2, 3, 4, 5, 6, 12, 13), outputPin = 8)
    )

    /**
     * Mapping of DIP-14 IC part numbers to their gate configurations.
     *
     * Each IC is mapped to a list of GateGroups that define its logic structure.
     */
    val DIP14 = mapOf(

        // Quad 2-input gate ICs (standard layout)
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
        "747032" to quad2(),

        // Alternate quad layout ICs
        "7401" to quad2Alt(),
        "7402" to quad2Alt(),
        "7428" to quad2Alt(),
        "7433" to quad2Alt(),

        // Hex inverter / buffer ICs
        "7404" to hexInverter(),
        "7405" to hexInverter(),
        "7406" to hexInverter(),
        "7407" to hexInverter(),
        "7414" to hexInverter(),
        "7416" to hexInverter(),
        "7417" to hexInverter(),
        "7419" to hexInverter(),

        // Triple 3-input gate ICs
        "7410" to triple3(),
        "7411" to triple3(),
        "7412" to triple3(),
        "7415" to triple3(),
        "7427" to triple3(),

        // Dual 4-input gate ICs
        "7413" to dual4(),
        "7418" to dual4(),
        "7420" to dual4(),
        "7421" to dual4(),
        "7422" to dual4(),
        "7440" to dual4(),

        // Special multi-input IC
        "7425" to dual3PlusEnable(),

        // Large input gate IC
        "7430" to single8(),

        // XNOR IC variants
        "74266" to listOf(
            GateGroup(inputPins = listOf(1, 2), outputPin = 3),
            GateGroup(inputPins = listOf(5, 6), outputPin = 4),
            GateGroup(inputPins = listOf(8, 9), outputPin = 10),
            GateGroup(inputPins = listOf(12, 13), outputPin = 11)
        ),
        "747266" to listOf(
            GateGroup(inputPins = listOf(1, 2), outputPin = 3),
            GateGroup(inputPins = listOf(5, 6), outputPin = 4),
            GateGroup(inputPins = listOf(8, 9), outputPin = 10),
            GateGroup(inputPins = listOf(12, 13), outputPin = 11)
        )
    )
}
