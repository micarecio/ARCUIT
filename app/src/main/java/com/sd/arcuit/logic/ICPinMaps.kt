package com.sd.arcuit.logic

/**
 * Defines pin roles and pin mappings for common DIP-14 ICs.
 *
 * Each IC entry maps:
 * - pin number → electrical role (INPUT, OUTPUT, VCC, GND)
 *
 * This is used to interpret IC functionality from detected layouts.
 */
object ICPinMaps {

    /**
     * Complete mapping of DIP-14 ICs to their pin role definitions.
     * Each entry represents a specific 74xx logic IC.
     */
    val DIP14 = mapOf(

        // 7400 – Quad 2-input NAND gate
        "7400" to mapOf(
            1 to PinRole.INPUT,     14 to PinRole.VCC,
            2 to PinRole.INPUT,     13 to PinRole.INPUT,
            3 to PinRole.OUTPUT,    12 to PinRole.INPUT,
            4 to PinRole.INPUT,     11 to PinRole.OUTPUT,
            5 to PinRole.INPUT,     10 to PinRole.INPUT,
            6 to PinRole.OUTPUT,     9 to PinRole.INPUT,
            7 to PinRole.GND,        8 to PinRole.OUTPUT
        ),

        // 7401 – Quad 2-input open-collector NAND gates
        "7401" to mapOf(
            1 to PinRole.OUTPUT,    14 to PinRole.VCC,
            2 to PinRole.INPUT,     13 to PinRole.OUTPUT,
            3 to PinRole.INPUT,     12 to PinRole.INPUT,
            4 to PinRole.OUTPUT,    11 to PinRole.INPUT,
            5 to PinRole.INPUT,     10 to PinRole.OUTPUT,
            6 to PinRole.INPUT,      9 to PinRole.INPUT,
            7 to PinRole.GND,        8 to PinRole.INPUT
        ),

        // 7402 – Quad 2-input NOR gate
        "7402" to mapOf(
            1 to PinRole.OUTPUT,    14 to PinRole.VCC,
            2 to PinRole.INPUT,     13 to PinRole.OUTPUT,
            3 to PinRole.INPUT,     12 to PinRole.INPUT,
            4 to PinRole.OUTPUT,    11 to PinRole.INPUT,
            5 to PinRole.INPUT,     10 to PinRole.OUTPUT,
            6 to PinRole.INPUT,      9 to PinRole.INPUT,
            7 to PinRole.GND,        8 to PinRole.INPUT
        ),

        // 7403 – Quad 2-input open-collector NAND gates
        "7403" to mapOf(
            1 to PinRole.INPUT,     14 to PinRole.VCC,
            2 to PinRole.INPUT,     13 to PinRole.INPUT,
            3 to PinRole.OUTPUT,    12 to PinRole.INPUT,
            4 to PinRole.INPUT,     11 to PinRole.OUTPUT,
            5 to PinRole.INPUT,     10 to PinRole.INPUT,
            6 to PinRole.OUTPUT,     9 to PinRole.INPUT,
            7 to PinRole.GND,        8 to PinRole.OUTPUT
        ),

        // 7404 – Hex inverter/NOT gate
        "7404" to mapOf(
            1 to PinRole.INPUT,     14 to PinRole.VCC,
            2 to PinRole.OUTPUT,    13 to PinRole.INPUT,
            3 to PinRole.INPUT,     12 to PinRole.OUTPUT,
            4 to PinRole.OUTPUT,    11 to PinRole.INPUT,
            5 to PinRole.INPUT,     10 to PinRole.OUTPUT,
            6 to PinRole.OUTPUT,     9 to PinRole.INPUT,
            7 to PinRole.GND,        8 to PinRole.OUTPUT
        ),

        // 7405 – Hex open-collector inverters
        "7405" to mapOf(
            1 to PinRole.INPUT,     14 to PinRole.VCC,
            2 to PinRole.OUTPUT,    13 to PinRole.INPUT,
            3 to PinRole.INPUT,     12 to PinRole.OUTPUT,
            4 to PinRole.OUTPUT,    11 to PinRole.INPUT,
            5 to PinRole.INPUT,     10 to PinRole.OUTPUT,
            6 to PinRole.OUTPUT,     9 to PinRole.INPUT,
            7 to PinRole.GND,        8 to PinRole.OUTPUT
        ),

        // 7406 – Hex open-collector inverters
        "7406" to mapOf(
            1 to PinRole.INPUT,     14 to PinRole.VCC,
            2 to PinRole.OUTPUT,    13 to PinRole.INPUT,
            3 to PinRole.INPUT,     12 to PinRole.OUTPUT,
            4 to PinRole.OUTPUT,    11 to PinRole.INPUT,
            5 to PinRole.INPUT,     10 to PinRole.OUTPUT,
            6 to PinRole.OUTPUT,     9 to PinRole.INPUT,
            7 to PinRole.GND,        8 to PinRole.OUTPUT
        ),

        // 7407 – Hex buffer (open-collector)
        "7407" to mapOf(
            1 to PinRole.INPUT,     14 to PinRole.VCC,
            2 to PinRole.OUTPUT,    13 to PinRole.INPUT,
            3 to PinRole.INPUT,     12 to PinRole.OUTPUT,
            4 to PinRole.OUTPUT,    11 to PinRole.INPUT,
            5 to PinRole.INPUT,     10 to PinRole.OUTPUT,
            6 to PinRole.OUTPUT,     9 to PinRole.INPUT,
            7 to PinRole.GND,        8 to PinRole.OUTPUT
        ),

        // 7408 – Quad 2-input AND gate
        "7408" to mapOf(
            1 to PinRole.INPUT,     14 to PinRole.VCC,
            2 to PinRole.INPUT,     13 to PinRole.INPUT,
            3 to PinRole.OUTPUT,    12 to PinRole.INPUT,
            4 to PinRole.INPUT,     11 to PinRole.OUTPUT,
            5 to PinRole.INPUT,     10 to PinRole.INPUT,
            6 to PinRole.OUTPUT,     9 to PinRole.INPUT,
            7 to PinRole.GND,        8 to PinRole.OUTPUT
        ),

        // 7409 – Quad 2-input AND (open-collector)
        "7409" to mapOf(
            1 to PinRole.INPUT,     14 to PinRole.VCC,
            2 to PinRole.INPUT,     13 to PinRole.INPUT,
            3 to PinRole.OUTPUT,    12 to PinRole.INPUT,
            4 to PinRole.INPUT,     11 to PinRole.OUTPUT,
            5 to PinRole.INPUT,     10 to PinRole.INPUT,
            6 to PinRole.OUTPUT,     9 to PinRole.INPUT,
            7 to PinRole.GND,        8 to PinRole.OUTPUT
        ),

        // 7410 – Triple 3-input NAND gates
        "7410" to mapOf(
            1 to PinRole.INPUT,     14 to PinRole.VCC,
            2 to PinRole.INPUT,     13 to PinRole.INPUT,
            3 to PinRole.INPUT,     12 to PinRole.OUTPUT,
            4 to PinRole.INPUT,     11 to PinRole.INPUT,
            5 to PinRole.INPUT,     10 to PinRole.INPUT,
            6 to PinRole.OUTPUT,     9 to PinRole.INPUT,
            7 to PinRole.GND,        8 to PinRole.OUTPUT
        ),

        // 7411 – Triple 3-input AND gates
        "7411" to mapOf(
            1 to PinRole.INPUT,     14 to PinRole.VCC,
            2 to PinRole.INPUT,     13 to PinRole.INPUT,
            3 to PinRole.INPUT,     12 to PinRole.OUTPUT,
            4 to PinRole.INPUT,     11 to PinRole.INPUT,
            5 to PinRole.INPUT,     10 to PinRole.INPUT,
            6 to PinRole.OUTPUT,     9 to PinRole.INPUT,
            7 to PinRole.GND,        8 to PinRole.OUTPUT
        ),

        // 7412 – Triple 3-input open-collector NAND gates
        "7412" to mapOf(
            1 to PinRole.INPUT,     14 to PinRole.VCC,
            2 to PinRole.INPUT,     13 to PinRole.INPUT,
            3 to PinRole.INPUT,     12 to PinRole.OUTPUT,
            4 to PinRole.INPUT,     11 to PinRole.INPUT,
            5 to PinRole.INPUT,     10 to PinRole.INPUT,
            6 to PinRole.OUTPUT,     9 to PinRole.INPUT,
            7 to PinRole.GND,        8 to PinRole.OUTPUT
        ),

        // 7413 – Dual 4-input NAND Schmitt trigger
        "7413" to mapOf(
            1 to PinRole.INPUT,     14 to PinRole.VCC,
            2 to PinRole.INPUT,     13 to PinRole.INPUT,
            12 to PinRole.INPUT,
            4 to PinRole.INPUT,
            5 to PinRole.INPUT,     10 to PinRole.INPUT,
            6 to PinRole.OUTPUT,     9 to PinRole.INPUT,
            7 to PinRole.GND,        8 to PinRole.OUTPUT
        ),

        // 7414 – Hex Schmitt-trigger inverters
        "7414" to mapOf(
            1 to PinRole.INPUT,     14 to PinRole.VCC,
            2 to PinRole.OUTPUT,    13 to PinRole.INPUT,
            3 to PinRole.INPUT,     12 to PinRole.OUTPUT,
            4 to PinRole.OUTPUT,    11 to PinRole.INPUT,
            5 to PinRole.INPUT,     10 to PinRole.OUTPUT,
            6 to PinRole.OUTPUT,     9 to PinRole.INPUT,
            7 to PinRole.GND,        8 to PinRole.OUTPUT
        ),

        // 7415 – Triple 3-input open-collector AND gates
        "7415" to mapOf(
            1 to PinRole.INPUT,     14 to PinRole.VCC,
            2 to PinRole.INPUT,     13 to PinRole.INPUT,
            3 to PinRole.INPUT,     12 to PinRole.OUTPUT,
            4 to PinRole.INPUT,     11 to PinRole.INPUT,
            5 to PinRole.INPUT,     10 to PinRole.INPUT,
            6 to PinRole.OUTPUT,     9 to PinRole.INPUT,
            7 to PinRole.GND,        8 to PinRole.OUTPUT
        ),

        // 7416 – Hex Schmitt-trigger inverters
        "7416" to mapOf(
            1 to PinRole.INPUT,     14 to PinRole.VCC,
            2 to PinRole.OUTPUT,    13 to PinRole.INPUT,
            3 to PinRole.INPUT,     12 to PinRole.OUTPUT,
            4 to PinRole.OUTPUT,    11 to PinRole.INPUT,
            5 to PinRole.INPUT,     10 to PinRole.OUTPUT,
            6 to PinRole.OUTPUT,     9 to PinRole.INPUT,
            7 to PinRole.GND,        8 to PinRole.OUTPUT
        ),

        // 7417 – Hex open-collector buffers
        "7417" to mapOf(
            1 to PinRole.INPUT,     14 to PinRole.VCC,
            2 to PinRole.OUTPUT,    13 to PinRole.INPUT,
            3 to PinRole.INPUT,     12 to PinRole.OUTPUT,
            4 to PinRole.OUTPUT,    11 to PinRole.INPUT,
            5 to PinRole.INPUT,     10 to PinRole.OUTPUT,
            6 to PinRole.OUTPUT,     9 to PinRole.INPUT,
            7 to PinRole.GND,        8 to PinRole.OUTPUT
        ),

        // 7418 – Dual 4-input NAND Schmitt trigger
        "7418" to mapOf(
            1 to PinRole.INPUT,     14 to PinRole.VCC,
            2 to PinRole.INPUT,     13 to PinRole.INPUT,
            12 to PinRole.INPUT,
            4 to PinRole.INPUT,
            5 to PinRole.INPUT,     10 to PinRole.INPUT,
            6 to PinRole.OUTPUT,     9 to PinRole.INPUT,
            7 to PinRole.GND,        8 to PinRole.OUTPUT
        ),

        // 7419 – Hex Schmitt-trigger inverters
        "7419" to mapOf(
            1 to PinRole.INPUT,     14 to PinRole.VCC,
            2 to PinRole.OUTPUT,    13 to PinRole.INPUT,
            3 to PinRole.INPUT,     12 to PinRole.OUTPUT,
            4 to PinRole.OUTPUT,    11 to PinRole.INPUT,
            5 to PinRole.INPUT,     10 to PinRole.OUTPUT,
            6 to PinRole.OUTPUT,     9 to PinRole.INPUT,
            7 to PinRole.GND,        8 to PinRole.OUTPUT
        ),

        // 7420 – Dual 4-input NAND gates
        "7420" to mapOf(
            1 to PinRole.INPUT,     14 to PinRole.VCC,
            2 to PinRole.INPUT,     13 to PinRole.INPUT,
            12 to PinRole.INPUT,
            4 to PinRole.INPUT,
            5 to PinRole.INPUT,     10 to PinRole.INPUT,
            6 to PinRole.OUTPUT,     9 to PinRole.INPUT,
            7 to PinRole.GND,        8 to PinRole.OUTPUT
        ),

        // 7421 – Dual 4-input AND gates
        "7421" to mapOf(
            1 to PinRole.INPUT,     14 to PinRole.VCC,
            2 to PinRole.INPUT,     13 to PinRole.INPUT,
            12 to PinRole.INPUT,
            4 to PinRole.INPUT,
            5 to PinRole.INPUT,     10 to PinRole.INPUT,
            6 to PinRole.OUTPUT,     9 to PinRole.INPUT,
            7 to PinRole.GND,        8 to PinRole.OUTPUT
        ),

        // 7422 – Dual 4-input AND gates
        "7422" to mapOf(
            1 to PinRole.INPUT,     14 to PinRole.VCC,
            2 to PinRole.INPUT,     13 to PinRole.INPUT,
            12 to PinRole.INPUT,
            4 to PinRole.INPUT,
            5 to PinRole.INPUT,     10 to PinRole.INPUT,
            6 to PinRole.OUTPUT,     9 to PinRole.INPUT,
            7 to PinRole.GND,        8 to PinRole.OUTPUT
        ),

        // 7424 – Quad 2-input NAND Schmitt trigger
        "7424" to mapOf(
            1 to PinRole.INPUT,     14 to PinRole.VCC,
            2 to PinRole.INPUT,     13 to PinRole.INPUT,
            3 to PinRole.OUTPUT,    12 to PinRole.INPUT,
            4 to PinRole.INPUT,     11 to PinRole.OUTPUT,
            5 to PinRole.INPUT,     10 to PinRole.INPUT,
            6 to PinRole.OUTPUT,     9 to PinRole.INPUT,
            7 to PinRole.GND,        8 to PinRole.OUTPUT
        ),

        // 7425 – Dual 4-input NOR gates with enable input
        "7425" to mapOf(
            1 to PinRole.INPUT,     14 to PinRole.VCC,
            2 to PinRole.INPUT,     13 to PinRole.INPUT,
            3 to PinRole.INPUT,     12 to PinRole.INPUT,
            4 to PinRole.INPUT,     11 to PinRole.INPUT,
            5 to PinRole.INPUT,     10 to PinRole.INPUT,
            6 to PinRole.OUTPUT,     9 to PinRole.INPUT,
            7 to PinRole.GND,        8 to PinRole.OUTPUT
        ),

        // 7426 – Quad 2-input open-collector NAND gates
        "7426" to mapOf(
            1 to PinRole.INPUT,     14 to PinRole.VCC,
            2 to PinRole.INPUT,     13 to PinRole.INPUT,
            3 to PinRole.OUTPUT,    12 to PinRole.INPUT,
            4 to PinRole.INPUT,     11 to PinRole.OUTPUT,
            5 to PinRole.INPUT,     10 to PinRole.INPUT,
            6 to PinRole.OUTPUT,     9 to PinRole.INPUT,
            7 to PinRole.GND,        8 to PinRole.OUTPUT
        ),

        // 7427 – Triple 3-input NOR gates
        "7427" to mapOf(
            1 to PinRole.INPUT,     14 to PinRole.VCC,
            2 to PinRole.INPUT,     13 to PinRole.INPUT,
            3 to PinRole.INPUT,     12 to PinRole.OUTPUT,
            4 to PinRole.INPUT,     11 to PinRole.INPUT,
            5 to PinRole.INPUT,     10 to PinRole.INPUT,
            6 to PinRole.OUTPUT,     9 to PinRole.INPUT,
            7 to PinRole.GND,        8 to PinRole.OUTPUT
        ),

        // 7428 – Quad 2-input NOR gates
        "7428" to mapOf(
            1 to PinRole.OUTPUT,    14 to PinRole.VCC,
            2 to PinRole.INPUT,     13 to PinRole.OUTPUT,
            3 to PinRole.INPUT,     12 to PinRole.INPUT,
            4 to PinRole.OUTPUT,    11 to PinRole.INPUT,
            5 to PinRole.INPUT,     10 to PinRole.OUTPUT,
            6 to PinRole.INPUT,      9 to PinRole.INPUT,
            7 to PinRole.GND,        8 to PinRole.INPUT
        ),

        // 7430 – 8-input NAND gate
        "7430" to mapOf(
            1 to PinRole.INPUT,     14 to PinRole.VCC,
            2 to PinRole.INPUT,     13 to PinRole.INPUT,
            3 to PinRole.INPUT,     12 to PinRole.INPUT,
            4 to PinRole.INPUT,
            5 to PinRole.INPUT,
            6 to PinRole.INPUT,
            7 to PinRole.GND,        8 to PinRole.OUTPUT
        ),

        // 7432 – Quad 2-input OR gates
        "7432" to mapOf(
            1 to PinRole.INPUT,     14 to PinRole.VCC,
            2 to PinRole.INPUT,     13 to PinRole.INPUT,
            3 to PinRole.OUTPUT,    12 to PinRole.INPUT,
            4 to PinRole.INPUT,     11 to PinRole.OUTPUT,
            5 to PinRole.INPUT,     10 to PinRole.INPUT,
            6 to PinRole.OUTPUT,     9 to PinRole.INPUT,
            7 to PinRole.GND,        8 to PinRole.OUTPUT
        ),

        // 7433 – Quad 2-input NOR (open-collector)
        "7433" to mapOf(
            1 to PinRole.OUTPUT,    14 to PinRole.VCC,
            2 to PinRole.INPUT,     13 to PinRole.OUTPUT,
            3 to PinRole.INPUT,     12 to PinRole.INPUT,
            4 to PinRole.OUTPUT,    11 to PinRole.INPUT,
            5 to PinRole.INPUT,     10 to PinRole.OUTPUT,
            6 to PinRole.INPUT,      9 to PinRole.INPUT,
            7 to PinRole.GND,        8 to PinRole.INPUT
        ),

        // 7437 – Quad 2-input NAND gates
        "7437" to mapOf(
            1 to PinRole.INPUT,     14 to PinRole.VCC,
            2 to PinRole.INPUT,     13 to PinRole.INPUT,
            3 to PinRole.OUTPUT,    12 to PinRole.INPUT,
            4 to PinRole.INPUT,     11 to PinRole.OUTPUT,
            5 to PinRole.INPUT,     10 to PinRole.INPUT,
            6 to PinRole.OUTPUT,     9 to PinRole.INPUT,
            7 to PinRole.GND,        8 to PinRole.OUTPUT
        ),

        // 7438 – Quad 2-input NAND (open-collector)
        "7438" to mapOf(
            1 to PinRole.INPUT,     14 to PinRole.VCC,
            2 to PinRole.INPUT,     13 to PinRole.INPUT,
            3 to PinRole.OUTPUT,    12 to PinRole.INPUT,
            4 to PinRole.INPUT,     11 to PinRole.OUTPUT,
            5 to PinRole.INPUT,     10 to PinRole.INPUT,
            6 to PinRole.OUTPUT,     9 to PinRole.INPUT,
            7 to PinRole.GND,        8 to PinRole.OUTPUT
        ),

        // 7440 – Dual 4-input AND gates
        "7440" to mapOf(
            1 to PinRole.INPUT,     14 to PinRole.VCC,
            2 to PinRole.INPUT,     13 to PinRole.INPUT,
            12 to PinRole.INPUT,
            4 to PinRole.INPUT,
            5 to PinRole.INPUT,     10 to PinRole.INPUT,
            6 to PinRole.OUTPUT,     9 to PinRole.INPUT,
            7 to PinRole.GND,        8 to PinRole.OUTPUT
        ),

        // 7486 – Quad 2-input XOR gates
        "7486" to mapOf(
            1 to PinRole.INPUT,     14 to PinRole.VCC,
            2 to PinRole.INPUT,     13 to PinRole.INPUT,
            3 to PinRole.OUTPUT,    12 to PinRole.INPUT,
            4 to PinRole.INPUT,     11 to PinRole.OUTPUT,
            5 to PinRole.INPUT,     10 to PinRole.INPUT,
            6 to PinRole.OUTPUT,     9 to PinRole.INPUT,
            7 to PinRole.GND,        8 to PinRole.OUTPUT
        ),

        // 74132 – Quad NAND Schmitt trigger
        "74132" to mapOf(
            1 to PinRole.INPUT,     14 to PinRole.VCC,
            2 to PinRole.INPUT,     13 to PinRole.INPUT,
            3 to PinRole.OUTPUT,    12 to PinRole.INPUT,
            4 to PinRole.INPUT,     11 to PinRole.OUTPUT,
            5 to PinRole.INPUT,     10 to PinRole.INPUT,
            6 to PinRole.OUTPUT,     9 to PinRole.INPUT,
            7 to PinRole.GND,        8 to PinRole.OUTPUT
        ),

        // 74136 – Quad NAND Schmitt trigger
        "74136" to mapOf(
            1 to PinRole.INPUT,     14 to PinRole.VCC,
            2 to PinRole.INPUT,     13 to PinRole.INPUT,
            3 to PinRole.OUTPUT,    12 to PinRole.INPUT,
            4 to PinRole.INPUT,     11 to PinRole.OUTPUT,
            5 to PinRole.INPUT,     10 to PinRole.INPUT,
            6 to PinRole.OUTPUT,     9 to PinRole.INPUT,
            7 to PinRole.GND,        8 to PinRole.OUTPUT
        ),

        // 74266 – Quad 2-input XNOR gates
        "74266" to mapOf(
            1 to PinRole.INPUT,     14 to PinRole.VCC,
            2 to PinRole.INPUT,     13 to PinRole.INPUT,
            3 to PinRole.OUTPUT,    12 to PinRole.INPUT,
            4 to PinRole.OUTPUT,    11 to PinRole.OUTPUT,
            5 to PinRole.INPUT,     10 to PinRole.OUTPUT,
            6 to PinRole.INPUT,      9 to PinRole.INPUT,
            7 to PinRole.GND,        8 to PinRole.INPUT
        ),

        // 747001 – Quad Schmitt AND gates
        "747001" to mapOf(
            1 to PinRole.INPUT,     14 to PinRole.VCC,
            2 to PinRole.INPUT,     13 to PinRole.INPUT,
            3 to PinRole.OUTPUT,    12 to PinRole.INPUT,
            4 to PinRole.INPUT,     11 to PinRole.OUTPUT,
            5 to PinRole.INPUT,     10 to PinRole.INPUT,
            6 to PinRole.OUTPUT,     9 to PinRole.INPUT,
            7 to PinRole.GND,        8 to PinRole.OUTPUT
        ),

        // 747002 – Quad Schmitt NOR gates
        "747002" to mapOf(
            1 to PinRole.INPUT,     14 to PinRole.VCC,
            2 to PinRole.INPUT,     13 to PinRole.INPUT,
            3 to PinRole.OUTPUT,    12 to PinRole.INPUT,
            4 to PinRole.INPUT,     11 to PinRole.OUTPUT,
            5 to PinRole.INPUT,     10 to PinRole.INPUT,
            6 to PinRole.OUTPUT,     9 to PinRole.INPUT,
            7 to PinRole.GND,        8 to PinRole.OUTPUT
        ),

        // 747266 – Quad XNOR gates
        "747266" to mapOf(
            1 to PinRole.INPUT,     14 to PinRole.VCC,
            2 to PinRole.INPUT,     13 to PinRole.INPUT,
            3 to PinRole.OUTPUT,    12 to PinRole.INPUT,
            4 to PinRole.OUTPUT,    11 to PinRole.OUTPUT,
            5 to PinRole.INPUT,     10 to PinRole.OUTPUT,
            6 to PinRole.INPUT,      9 to PinRole.INPUT,
            7 to PinRole.GND,        8 to PinRole.INPUT
        )
    )
}
