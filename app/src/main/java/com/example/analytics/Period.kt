package com.example.analytics

/**
 * Supported time periods for Analytics Engine calculations:
 * - MONTH: Individual month (0..11)
 * - Q1..Q4: Calendar quarters
 * - H1, H2: Half-years (6-month periods)
 * - YEAR: Full calendar year (12 months)
 */
sealed class Period(val displayName: String, val monthIndices: List<Int>) {
    data class Month(val index: Int) : Period(
        displayName = "Tháng ${(index + 1).toString().padStart(2, '0')}",
        monthIndices = listOf(index)
    ) {
        init {
            require(index in 0..11) { "Month index must be between 0 and 11, got $index" }
        }
    }

    object Q1 : Period("Quý 1", listOf(0, 1, 2))
    object Q2 : Period("Quý 2", listOf(3, 4, 5))
    object Q3 : Period("Quý 3", listOf(6, 7, 8))
    object Q4 : Period("Quý 4", listOf(9, 10, 11))

    object H1 : Period("6 Tháng Đầu (H1)", (0..5).toList())
    object H2 : Period("6 Tháng Cuối (H2)", (6..11).toList())

    object Year : Period("Cả Năm", (0..11).toList())

    companion object {
        fun allQuarters(): List<Period> = listOf(Q1, Q2, Q3, Q4)
        fun allHalves(): List<Period> = listOf(H1, H2)
        fun standardPeriods(): List<Period> = listOf(Year, Q1, Q2, Q3, Q4, H1, H2)
    }
}
