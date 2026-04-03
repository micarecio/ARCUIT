package com.sd.arcuit.logic

data class TruthTableRow(
    val inputs: List<String>,
    val output: String
)

data class TruthTableData(
    val title: String,
    val headers: List<String>,
    val rows: List<TruthTableRow>
)

object TruthTables {

    private fun oneInputRows(gate: (Int) -> Int): List<TruthTableRow> {
        return listOf(
            TruthTableRow(listOf("0"), gate(0).toString()),
            TruthTableRow(listOf("1"), gate(1).toString())
        )
    }

    private fun twoInputRows(gate: (Int, Int) -> Int): List<TruthTableRow> {
        return listOf(
            TruthTableRow(listOf("0", "0"), gate(0, 0).toString()),
            TruthTableRow(listOf("0", "1"), gate(0, 1).toString()),
            TruthTableRow(listOf("1", "0"), gate(1, 0).toString()),
            TruthTableRow(listOf("1", "1"), gate(1, 1).toString())
        )
    }

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

    private fun eightInputRows(gate: (List<Int>) -> Int): List<TruthTableRow> {
        val rows = mutableListOf<TruthTableRow>()

        for (i in 0 until 256) {
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

    fun get(icCode: String): TruthTableData? {
        return when (icCode) {

            // ---------------- NOT / INVERTER / BUFFER-LIKE ----------------

            "7404", "7405", "7406", "7414", "7416", "7419" -> TruthTableData(
                title = "$icCode NOT",
                headers = listOf("A", "Y"),
                rows = oneInputRows { a ->
                    if (a == 0) 1 else 0
                }
            )

            "7407", "7417" -> TruthTableData(
                title = "$icCode BUFFER",
                headers = listOf("A", "Y"),
                rows = oneInputRows { a -> a }
            )

            // ---------------- 2-INPUT AND ----------------

            "7408", "7409", "747001" -> TruthTableData(
                title = "$icCode AND",
                headers = listOf("A", "B", "Y"),
                rows = twoInputRows { a, b ->
                    if (a == 1 && b == 1) 1 else 0
                }
            )

            // ---------------- 2-INPUT NAND ----------------

            "7400", "7403", "7424", "7426", "7437", "7438", "74132", "74136" -> TruthTableData(
                title = "$icCode NAND",
                headers = listOf("A", "B", "Y"),
                rows = twoInputRows { a, b ->
                    if (a == 1 && b == 1) 0 else 1
                }
            )

            // ---------------- 2-INPUT NAND (alt family in your map) ----------------

            "7401" -> TruthTableData(
                title = "$icCode NAND",
                headers = listOf("A", "B", "Y"),
                rows = twoInputRows { a, b ->
                    if (a == 1 && b == 1) 0 else 1
                }
            )

            // ---------------- 2-INPUT NOR ----------------

            "7402", "7428", "7433", "747002" -> TruthTableData(
                title = "$icCode NOR",
                headers = listOf("A", "B", "Y"),
                rows = twoInputRows { a, b ->
                    if (a == 1 || b == 1) 0 else 1
                }
            )

            // ---------------- 2-INPUT OR ----------------

            "7432", "747032" -> TruthTableData(
                title = "$icCode OR",
                headers = listOf("A", "B", "Y"),
                rows = twoInputRows { a, b ->
                    if (a == 1 || b == 1) 1 else 0
                }
            )

            // ---------------- 2-INPUT XOR ----------------

            "7486" -> TruthTableData(
                title = "$icCode XOR",
                headers = listOf("A", "B", "Y"),
                rows = twoInputRows { a, b ->
                    if (a != b) 1 else 0
                }
            )

            // ---------------- 2-INPUT XNOR ----------------

            "74266", "747266" -> TruthTableData(
                title = "$icCode XNOR",
                headers = listOf("A", "B", "Y"),
                rows = twoInputRows { a, b ->
                    if (a == b) 1 else 0
                }
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