package com.sd.arcuit.logic

/**
 * Represents a single row in a truth table.
 * inputs -> binary input values (as strings for UI display)
 * output -> resulting gate output (0 or 1 as string)
 */
data class TruthTableRow(
    val inputs: List<String>,
    val output: String
)

/**
 * Full truth table structure for an IC gate.
 */
data class TruthTableData(
    val title: String,
    val headers: List<String>,
    val rows: List<TruthTableRow>
)

object TruthTables {

    /**
     * Generates truth table rows for single-input logic gates.
     */
    private fun oneInputRows(gate: (Int) -> Int): List<TruthTableRow> {
        return listOf(
            TruthTableRow(listOf("0"), gate(0).toString()),
            TruthTableRow(listOf("1"), gate(1).toString())
        )
    }

    /**
     * Generates truth table rows for 2-input logic gates.
     */
    private fun twoInputRows(gate: (Int, Int) -> Int): List<TruthTableRow> {
        return listOf(
            TruthTableRow(listOf("0", "0"), gate(0, 0).toString()),
            TruthTableRow(listOf("0", "1"), gate(0, 1).toString()),
            TruthTableRow(listOf("1", "0"), gate(1, 0).toString()),
            TruthTableRow(listOf("1", "1"), gate(1, 1).toString())
        )
    }

    /**
     * Generates truth table rows for 3-input logic gates.
     * Covers all 8 input combinations (2^3).
     */
    private fun threeInputRows(gate: (Int, Int, Int) -> Int): List<TruthTableRow> {
        return listOf(
            TruthTableRow(listOf("0", "0", "0"), gate(0, 0, 0).toString()),
            TruthTableRow(listOf("0", "0", "1"), gate(0, 0, 1).toString()),
            TruthTableRow(listOf("0", "1", "0"), gate(0, 1, 0).toString()),
            TruthTableRow(listOf("0", "1", "1"), gate(0, 1, 1).toString()),
            TruthTableRow(listOf("1", "0", "0"), gate(1, 0, 0).toString()),
            TruthTableRow(listOf("1", "0", "1"), gate(1, 0, 1).toString()),
            TruthTableRow(listOf("1", "1", "0"), gate(1, 1, 0).toString()),
            TruthTableRow(listOf("1", "1", "1"), gate(1, 1, 1).toString())
        )
    }

    /**
     * Generates truth table rows for 4-input logic gates.
     * Uses nested loops for clarity and scalability.
     */
    private fun fourInputRows(gate: (Int, Int, Int, Int) -> Int): List<TruthTableRow> {
        val rows = mutableListOf<TruthTableRow>()

        for (a in 0..1) {
            for (b in 0..1) {
                for (c in 0..1) {
                    for (d in 0..1) {
                        rows.add(
                            TruthTableRow(
                                listOf(a.toString(), b.toString(), c.toString(), d.toString()),
                                gate(a, b, c, d).toString()
                            )
                        )
                    }
                }
            }
        }

        return rows
    }

    /**
     * Generates truth table rows for 8-input logic gates.
     * Iterates over all 256 combinations (2^8).
     */
    private fun eightInputRows(gate: (List<Int>) -> Int): List<TruthTableRow> {
        val rows = mutableListOf<TruthTableRow>()

        for (i in 0 until 256) {

            // Convert integer to 8-bit binary list
            val bits = (7 downTo 0).map { bit ->
                if ((i shr bit) and 1 == 1) 1 else 0
            }

            rows.add(
                TruthTableRow(
                    bits.map { it.toString() },
                    gate(bits).toString()
                )
            )
        }

        return rows
    }

    /**
     * Main lookup function:
     * Returns truth table for a given IC chip code.
     */
    fun get(icCode: String): TruthTableData? {
        return when (icCode) {

            // ---------------- NOT / INVERTER ----------------
            "7404", "7405", "7406", "7414", "7416", "7419" -> TruthTableData(
                title = "$icCode NOT",
                headers = listOf("A", "Y"),
                rows = oneInputRows { a -> if (a == 0) 1 else 0 }
            )

            // ---------------- BUFFER ----------------
            "7407", "7417" -> TruthTableData(
                title = "$icCode BUFFER",
                headers = listOf("A", "Y"),
                rows = oneInputRows { a -> a }
            )

            // ---------------- AND GATES ----------------
            "7408", "7409", "747001" -> TruthTableData(
                title = "$icCode AND",
                headers = listOf("A", "B", "Y"),
                rows = twoInputRows { a, b -> if (a == 1 && b == 1) 1 else 0 }
            )

            // ---------------- NAND GATES ----------------
            "7400", "7403", "7424", "7426", "7437", "7438", "74132", "74136",
            "7401" -> TruthTableData(
                title = "$icCode NAND",
                headers = listOf("A", "B", "Y"),
                rows = twoInputRows { a, b -> if (a == 1 && b == 1) 0 else 1 }
            )

            // ---------------- NOR GATES ----------------
            "7402", "7428", "7433", "747002" -> TruthTableData(
                title = "$icCode NOR",
                headers = listOf("A", "B", "Y"),
                rows = twoInputRows { a, b -> if (a == 1 || b == 1) 0 else 1 }
            )

            // ---------------- OR GATES ----------------
            "7432", "747032" -> TruthTableData(
                title = "$icCode OR",
                headers = listOf("A", "B", "Y"),
                rows = twoInputRows { a, b -> if (a == 1 || b == 1) 1 else 0 }
            )

            // ---------------- XOR ----------------
            "7486" -> TruthTableData(
                title = "$icCode XOR",
                headers = listOf("A", "B", "Y"),
                rows = twoInputRows { a, b -> if (a != b) 1 else 0 }
            )

            // ---------------- XNOR ----------------
            "74266", "747266" -> TruthTableData(
                title = "$icCode XNOR",
                headers = listOf("A", "B", "Y"),
                rows = twoInputRows { a, b -> if (a == b) 1 else 0 }
            )

            // ---------------- 3-INPUT NAND ----------------
            "7410", "7412" -> TruthTableData(
                title = "$icCode 3-INPUT NAND",
                headers = listOf("A", "B", "C", "Y"),
                rows = threeInputRows { a, b, c ->
                    if (a == 1 && b == 1 && c == 1) 0 else 1
                }
            )

            // ---------------- 3-INPUT AND ----------------
            "7411", "7415" -> TruthTableData(
                title = "$icCode 3-INPUT AND",
                headers = listOf("A", "B", "C", "Y"),
                rows = threeInputRows { a, b, c ->
                    if (a == 1 && b == 1 && c == 1) 1 else 0
                }
            )

            // ---------------- 3-INPUT NOR ----------------
            "7427" -> TruthTableData(
                title = "$icCode 3-INPUT NOR",
                headers = listOf("A", "B", "C", "Y"),
                rows = threeInputRows { a, b, c ->
                    if (a == 1 || b == 1 || c == 1) 0 else 1
                }
            )

            // ---------------- 4-INPUT NAND ----------------
            "7413", "7418", "7420" -> TruthTableData(
                title = "$icCode 4-INPUT NAND",
                headers = listOf("A", "B", "C", "D", "Y"),
                rows = fourInputRows { a, b, c, d ->
                    if (a == 1 && b == 1 && c == 1 && d == 1) 0 else 1
                }
            )

            // ---------------- 4-INPUT AND ----------------
            "7421", "7422", "7440" -> TruthTableData(
                title = "$icCode 4-INPUT AND",
                headers = listOf("A", "B", "C", "D", "Y"),
                rows = fourInputRows { a, b, c, d ->
                    if (a == 1 && b == 1 && c == 1 && d == 1) 1 else 0
                }
            )

            // ---------------- 4-INPUT NOR ----------------
            "7425" -> TruthTableData(
                title = "$icCode 4-INPUT NOR",
                headers = listOf("A", "B", "C", "D", "Y"),
                rows = fourInputRows { a, b, c, d ->
                    if (a == 1 || b == 1 || c == 1 || d == 1) 0 else 1
                }
            )

            // ---------------- 8-INPUT NAND ----------------
            "7430" -> TruthTableData(
                title = "$icCode 8-INPUT NAND",
                headers = listOf("A", "B", "C", "D", "E", "F", "G", "H", "Y"),
                rows = eightInputRows { bits ->
                    if (bits.all { it == 1 }) 0 else 1
                }
            )

            else -> null
        }
    }
}
