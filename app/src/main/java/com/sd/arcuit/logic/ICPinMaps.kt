package com.sd.arcuit.logic

object ICPinMaps {

    val DIP14 = mapOf(

        // 7400 – Quad NAND
        "7400" to mapOf(
            1 to PinRole.INPUT, 2 to PinRole.INPUT, 3 to PinRole.OUTPUT,
            4 to PinRole.INPUT, 5 to PinRole.INPUT, 6 to PinRole.OUTPUT,
            7 to PinRole.GND,
            8 to PinRole.OUTPUT, 9 to PinRole.INPUT, 10 to PinRole.INPUT,
            11 to PinRole.OUTPUT, 12 to PinRole.INPUT, 13 to PinRole.INPUT,
            14 to PinRole.VCC
        ),

        // 7402 – Quad NOR
        "7402" to mapOf(
            1 to PinRole.OUTPUT, 2 to PinRole.INPUT, 3 to PinRole.INPUT,
            4 to PinRole.OUTPUT, 5 to PinRole.INPUT, 6 to PinRole.INPUT,
            7 to PinRole.GND,
            8 to PinRole.INPUT, 9 to PinRole.INPUT, 10 to PinRole.OUTPUT,
            11 to PinRole.INPUT, 12 to PinRole.INPUT, 13 to PinRole.OUTPUT,
            14 to PinRole.VCC
        ),

        // 7403 – Quad NAND (Open Collector)
        "7403" to mapOf(
            1 to PinRole.INPUT, 2 to PinRole.INPUT, 3 to PinRole.OUTPUT,
            4 to PinRole.INPUT, 5 to PinRole.INPUT, 6 to PinRole.OUTPUT,
            7 to PinRole.GND,
            8 to PinRole.OUTPUT, 9 to PinRole.INPUT, 10 to PinRole.INPUT,
            11 to PinRole.OUTPUT, 12 to PinRole.INPUT, 13 to PinRole.INPUT,
            14 to PinRole.VCC
        ),

        // 7404 – Hex NOT
        "7404" to mapOf(
            1 to PinRole.INPUT, 2 to PinRole.OUTPUT,
            3 to PinRole.INPUT, 4 to PinRole.OUTPUT,
            5 to PinRole.INPUT, 6 to PinRole.OUTPUT,
            7 to PinRole.GND,
            8 to PinRole.OUTPUT, 9 to PinRole.INPUT,
            10 to PinRole.OUTPUT, 11 to PinRole.INPUT,
            12 to PinRole.OUTPUT, 13 to PinRole.INPUT,
            14 to PinRole.VCC
        ),

        // 7405 – Hex NOT (Open Collector)
        "7405" to mapOf(
            1 to PinRole.INPUT, 2 to PinRole.OUTPUT,
            3 to PinRole.INPUT, 4 to PinRole.OUTPUT,
            5 to PinRole.INPUT, 6 to PinRole.OUTPUT,
            7 to PinRole.GND,
            8 to PinRole.OUTPUT, 9 to PinRole.INPUT,
            10 to PinRole.OUTPUT, 11 to PinRole.INPUT,
            12 to PinRole.OUTPUT, 13 to PinRole.INPUT,
            14 to PinRole.VCC
        ),

        // 7406 – Hex NOT (High Voltage Open Collector)
        "7406" to mapOf(
            1 to PinRole.INPUT, 2 to PinRole.OUTPUT,
            3 to PinRole.INPUT, 4 to PinRole.OUTPUT,
            5 to PinRole.INPUT, 6 to PinRole.OUTPUT,
            7 to PinRole.GND,
            8 to PinRole.OUTPUT, 9 to PinRole.INPUT,
            10 to PinRole.OUTPUT, 11 to PinRole.INPUT,
            12 to PinRole.OUTPUT, 13 to PinRole.INPUT,
            14 to PinRole.VCC
        ),

        // 7408 – Quad AND
        "7408" to mapOf(
            1 to PinRole.INPUT, 2 to PinRole.INPUT, 3 to PinRole.OUTPUT,
            4 to PinRole.INPUT, 5 to PinRole.INPUT, 6 to PinRole.OUTPUT,
            7 to PinRole.GND,
            8 to PinRole.OUTPUT, 9 to PinRole.INPUT, 10 to PinRole.INPUT,
            11 to PinRole.OUTPUT, 12 to PinRole.INPUT, 13 to PinRole.INPUT,
            14 to PinRole.VCC
        ),

        // 7410 – Triple 3‑Input NAND
        "7410" to mapOf(
            1 to PinRole.INPUT, 2 to PinRole.INPUT, 3 to PinRole.INPUT,
            4 to PinRole.OUTPUT, 5 to PinRole.INPUT, 6 to PinRole.INPUT,
            7 to PinRole.GND,
            8 to PinRole.INPUT, 9 to PinRole.OUTPUT, 10 to PinRole.INPUT,
            11 to PinRole.INPUT, 12 to PinRole.INPUT, 13 to PinRole.OUTPUT,
            14 to PinRole.VCC
        ),

        // 7420 – Dual 4‑Input NAND
        "7420" to mapOf(
            1 to PinRole.INPUT, 2 to PinRole.INPUT, 3 to PinRole.INPUT,
            4 to PinRole.INPUT, 5 to PinRole.OUTPUT, 6 to PinRole.INPUT,
            7 to PinRole.GND,
            8 to PinRole.INPUT, 9 to PinRole.OUTPUT, 10 to PinRole.INPUT,
            11 to PinRole.INPUT, 12 to PinRole.INPUT, 13 to PinRole.INPUT,
            14 to PinRole.VCC
        ),

        // 7427 – Triple 3‑Input NOR
        "7427" to mapOf(
            1 to PinRole.INPUT, 2 to PinRole.INPUT, 3 to PinRole.INPUT,
            4 to PinRole.OUTPUT, 5 to PinRole.INPUT, 6 to PinRole.INPUT,
            7 to PinRole.GND,
            8 to PinRole.INPUT, 9 to PinRole.OUTPUT, 10 to PinRole.INPUT,
            11 to PinRole.INPUT, 12 to PinRole.INPUT, 13 to PinRole.OUTPUT,
            14 to PinRole.VCC
        ),

        // 7430 – 8‑Input NAND
        "7430" to mapOf(
            1 to PinRole.INPUT, 2 to PinRole.INPUT, 3 to PinRole.INPUT,
            4 to PinRole.INPUT, 5 to PinRole.INPUT, 6 to PinRole.INPUT,
            7 to PinRole.GND,
            8 to PinRole.INPUT, 9 to PinRole.INPUT, 10 to PinRole.OUTPUT,
            11 to PinRole.INPUT, 12 to PinRole.INPUT, 13 to PinRole.INPUT,
            14 to PinRole.VCC
        ),

        // 7432 – Quad OR
        "7432" to mapOf(
            1 to PinRole.INPUT, 2 to PinRole.INPUT, 3 to PinRole.OUTPUT,
            4 to PinRole.INPUT, 5 to PinRole.INPUT, 6 to PinRole.OUTPUT,
            7 to PinRole.GND,
            8 to PinRole.OUTPUT, 9 to PinRole.INPUT, 10 to PinRole.INPUT,
            11 to PinRole.OUTPUT, 12 to PinRole.INPUT, 13 to PinRole.INPUT,
            14 to PinRole.VCC
        ),

        // 7486 – Quad XOR
        "7486" to mapOf(
            1 to PinRole.INPUT, 2 to PinRole.INPUT, 3 to PinRole.OUTPUT,
            4 to PinRole.INPUT, 5 to PinRole.INPUT, 6 to PinRole.OUTPUT,
            7 to PinRole.GND,
            8 to PinRole.OUTPUT, 9 to PinRole.INPUT, 10 to PinRole.INPUT,
            11 to PinRole.OUTPUT, 12 to PinRole.INPUT, 13 to PinRole.INPUT,
            14 to PinRole.VCC
        )
    )
}
