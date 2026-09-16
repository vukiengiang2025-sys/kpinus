package com.example

import com.example.analytics.AnalyticsEngine
import com.example.analytics.BottleneckCategory
import com.example.analytics.Period
import com.example.data.MonthEntity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Phase 3A: Comprehensive regression tests for AnalyticsEngine
 * Verifies period aggregation logic across:
 * - Month (individual month calculation)
 * - Quarters (Q1, Q2, Q3, Q4)
 * - Halves (H1, H2)
 * - Year (Full 12 months)
 * - Empty/zero state robustness
 * - Gap & KPI scoring calculation alignment
 */
class AnalyticsEngineTest {

    private fun createSampleYear(): List<MonthEntity> {
        // Create 12 months:
        // Months 0..2 (Q1): Target = 100K each (Total 300K), Actual = 90K each (Total 270K, 90%)
        //                   Cov Target = 50 each (Total 150), Cov Actual = 40 each (Total 120, 80%)
        //                   SKU = 3.5 each -> avg = 3.5
        // Months 3..5 (Q2): Target = 120K each (Total 360K), Actual = 120K each (Total 360K, 100%)
        //                   Cov Target = 60 each (Total 180), Cov Actual = 60 each (Total 180, 100%)
        //                   SKU = 4.3 each -> avg = 4.3
        // Months 6..8 (Q3): Target = 150K each (Total 450K), Actual = 100K each (Total 300K, 66.7% < 80%)
        //                   Cov Target = 70 each (Total 210), Cov Actual = 70 each (Total 210, 100%)
        //                   SKU = 4.7 each -> avg = 4.7
        // Months 9..11 (Q4): Target = 150K each (Total 450K), Actual = 180K each (Total 540K, 120%)
        //                   Cov Target = 70 each (Total 210), Cov Actual = 84 each (Total 252, 120%)
        //                   SKU = 4.8 each -> avg = 4.8
        return (0..11).map { index ->
            when (index) {
                in 0..2 -> MonthEntity(
                    monthIndex = index,
                    salesTarget = 100_000.0,
                    salesActual = 90_000.0,
                    coverageTarget = 50.0,
                    coverageActual = 40.0,
                    skuActual = 3.5
                )
                in 3..5 -> MonthEntity(
                    monthIndex = index,
                    salesTarget = 120_000.0,
                    salesActual = 120_000.0,
                    coverageTarget = 60.0,
                    coverageActual = 60.0,
                    skuActual = 4.3
                )
                in 6..8 -> MonthEntity(
                    monthIndex = index,
                    salesTarget = 150_000.0,
                    salesActual = 100_000.0,
                    coverageTarget = 70.0,
                    coverageActual = 70.0,
                    skuActual = 4.7
                )
                else -> MonthEntity(
                    monthIndex = index,
                    salesTarget = 150_000.0,
                    salesActual = 180_000.0,
                    coverageTarget = 70.0,
                    coverageActual = 84.0,
                    skuActual = 4.8
                )
            }
        }
    }

    // ==========================================
    // 1. MONTH-LEVEL ANALYTICS
    // ==========================================
    @Test
    fun testAnalyze_singleMonth() {
        val months = createSampleYear()
        val resultM0 = AnalyticsEngine.analyze(months, Period.Month(0))

        assertEquals(1, resultM0.monthsCount)
        assertEquals(100_000.0, resultM0.sales.target, 0.001)
        assertEquals(90_000.0, resultM0.sales.actual, 0.001)
        assertEquals(90.0, resultM0.sales.achievementPercentage, 0.001)
        assertEquals(10_000.0, resultM0.sales.gap, 0.001)
        // 0.9 * 80 = 72.0 points
        assertEquals(72.0, resultM0.sales.kpiScore, 0.001)

        assertEquals(50.0, resultM0.coverage.target, 0.001)
        assertEquals(40.0, resultM0.coverage.actual, 0.001)
        assertEquals(80.0, resultM0.coverage.achievementPercentage, 0.001)
        assertEquals(10.0, resultM0.coverage.gap, 0.001)
        // 0.8 * 10 = 8.0 points
        assertEquals(8.0, resultM0.coverage.kpiScore, 0.001)

        assertEquals(3.5, resultM0.sku.skuActual, 0.001)
        // SKU 3.5 -> 4.5 points
        assertEquals(4.5, resultM0.sku.kpiScore, 0.001)
        assertEquals(3.7, resultM0.sku.nextThreshold!!, 0.001)
        assertEquals(0.2, resultM0.sku.skuGap, 0.001)

        // Total KPI = 72.0 + 8.0 + 4.5 = 84.5
        assertEquals(84.5, resultM0.totalKpiScore, 0.001)
    }

    // ==========================================
    // 2. QUARTER-LEVEL ANALYTICS (Q1 .. Q4)
    // ==========================================
    @Test
    fun testAnalyze_q1() {
        val months = createSampleYear()
        val q1Result = AnalyticsEngine.analyze(months, Period.Q1)

        assertEquals(3, q1Result.monthsCount)
        // Sum of Sales: 3 * 100K target = 300K, 3 * 90K actual = 270K
        assertEquals(300_000.0, q1Result.sales.target, 0.001)
        assertEquals(270_000.0, q1Result.sales.actual, 0.001)
        assertEquals(90.0, q1Result.sales.achievementPercentage, 0.001)
        assertEquals(30_000.0, q1Result.sales.gap, 0.001)
        // 270K / 300K = 0.9 * 80 = 72.0 points
        assertEquals(72.0, q1Result.sales.kpiScore, 0.001)

        // Sum of Coverage: 3 * 50 = 150 target, 3 * 40 = 120 actual
        assertEquals(150.0, q1Result.coverage.target, 0.001)
        assertEquals(120.0, q1Result.coverage.actual, 0.001)
        assertEquals(80.0, q1Result.coverage.achievementPercentage, 0.001)
        assertEquals(30.0, q1Result.coverage.gap, 0.001)
        assertEquals(8.0, q1Result.coverage.kpiScore, 0.001)

        // Avg SKU: 3.5
        assertEquals(3.5, q1Result.sku.skuActual, 0.001)
        assertEquals(4.5, q1Result.sku.kpiScore, 0.001)

        assertEquals(84.5, q1Result.totalKpiScore, 0.001)
    }

    @Test
    fun testAnalyze_q2_100Percent() {
        val months = createSampleYear()
        val q2Result = AnalyticsEngine.analyze(months, Period.Q2)

        assertEquals(360_000.0, q2Result.sales.target, 0.001)
        assertEquals(360_000.0, q2Result.sales.actual, 0.001)
        assertEquals(100.0, q2Result.sales.achievementPercentage, 0.001)
        assertEquals(0.0, q2Result.sales.gap, 0.001)
        assertEquals(80.0, q2Result.sales.kpiScore, 0.001)

        assertEquals(180.0, q2Result.coverage.target, 0.001)
        assertEquals(180.0, q2Result.coverage.actual, 0.001)
        assertEquals(100.0, q2Result.coverage.achievementPercentage, 0.001)
        assertEquals(0.0, q2Result.coverage.gap, 0.001)
        assertEquals(10.0, q2Result.coverage.kpiScore, 0.001)

        // SKU 4.3 -> 6.5 points
        assertEquals(4.3, q2Result.sku.skuActual, 0.001)
        assertEquals(6.5, q2Result.sku.kpiScore, 0.001)
        assertEquals(4.5, q2Result.sku.nextThreshold!!, 0.001)
        assertEquals(0.2, q2Result.sku.skuGap, 0.001)

        // Total = 80 + 10 + 6.5 = 96.5
        assertEquals(96.5, q2Result.totalKpiScore, 0.001)
    }

    @Test
    fun testAnalyze_q3_salesUnder80Percent_isBottleneck() {
        val months = createSampleYear()
        val q3Result = AnalyticsEngine.analyze(months, Period.Q3)

        // Sales: 300K actual / 450K target = 66.67% < 80%
        assertEquals(450_000.0, q3Result.sales.target, 0.001)
        assertEquals(300_000.0, q3Result.sales.actual, 0.001)
        assertEquals(0.0, q3Result.sales.kpiScore, 0.001) // Below 80% -> 0 points
        assertEquals(150_000.0, q3Result.sales.gap, 0.001)

        assertTrue(q3Result.isSalesBottleneck)
        // Coverage reached 100%
        assertEquals(10.0, q3Result.coverage.kpiScore, 0.001)
        // SKU = 4.7 -> 7.5 points
        assertEquals(7.5, q3Result.sku.kpiScore, 0.001)
        assertEquals(0.0, q3Result.sku.skuGap, 0.001)

        // Total = 0 + 10 + 7.5 = 17.5
        assertEquals(17.5, q3Result.totalKpiScore, 0.001)
    }

    @Test
    fun testAnalyze_q4_over100Percent() {
        val months = createSampleYear()
        val q4Result = AnalyticsEngine.analyze(months, Period.Q4)

        // Sales: 540K / 450K = 120% -> 1.2 * 80 = 96.0 points
        assertEquals(450_000.0, q4Result.sales.target, 0.001)
        assertEquals(540_000.0, q4Result.sales.actual, 0.001)
        assertEquals(120.0, q4Result.sales.achievementPercentage, 0.001)
        assertEquals(0.0, q4Result.sales.gap, 0.001)
        assertEquals(96.0, q4Result.sales.kpiScore, 0.001)

        // Coverage: 252 / 210 = 120% -> 1.2 * 10 = 12.0 points
        assertEquals(210.0, q4Result.coverage.target, 0.001)
        assertEquals(252.0, q4Result.coverage.actual, 0.001)
        assertEquals(120.0, q4Result.coverage.achievementPercentage, 0.001)
        assertEquals(0.0, q4Result.coverage.gap, 0.001)
        assertEquals(12.0, q4Result.coverage.kpiScore, 0.001)

        // SKU = 4.8 (> 4.7) -> 7.5 points
        assertEquals(4.8, q4Result.sku.skuActual, 0.001)
        assertEquals(7.5, q4Result.sku.kpiScore, 0.001)
        assertEquals(0.0, q4Result.sku.skuGap, 0.001)

        // Total = 96 + 12 + 7.5 = 115.5
        assertEquals(115.5, q4Result.totalKpiScore, 0.001)
    }

    // ==========================================
    // 3. HALF-YEAR ANALYTICS (H1, H2)
    // ==========================================
    @Test
    fun testAnalyze_halfYears() {
        val months = createSampleYear()
        val h1Result = AnalyticsEngine.analyze(months, Period.H1)
        val h2Result = AnalyticsEngine.analyze(months, Period.H2)

        assertEquals(6, h1Result.monthsCount)
        assertEquals(6, h2Result.monthsCount)

        // H1 target = 300K (Q1) + 360K (Q2) = 660K
        // H1 actual = 270K (Q1) + 360K (Q2) = 630K
        assertEquals(660_000.0, h1Result.sales.target, 0.001)
        assertEquals(630_000.0, h1Result.sales.actual, 0.001)
        // Ratio = 630 / 660 = 0.9545 -> Score = 0.954545 * 80 = 76.3636
        assertEquals((630.0 / 660.0) * 80.0, h1Result.sales.kpiScore, 0.001)

        // H2 target = 450K (Q3) + 450K (Q4) = 900K
        // H2 actual = 300K (Q3) + 540K (Q4) = 840K
        assertEquals(900_000.0, h2Result.sales.target, 0.001)
        assertEquals(840_000.0, h2Result.sales.actual, 0.001)
        // Ratio = 840 / 900 = 0.9333 -> Score = (840/900) * 80 = 74.666
        assertEquals((840.0 / 900.0) * 80.0, h2Result.sales.kpiScore, 0.001)
    }

    // ==========================================
    // 4. FULL YEAR ANALYTICS (Year)
    // ==========================================
    @Test
    fun testAnalyze_fullYear() {
        val months = createSampleYear()
        val yearResult = AnalyticsEngine.analyze(months, Period.Year)

        assertEquals(12, yearResult.monthsCount)

        // Year Target = 660K (H1) + 900K (H2) = 1_560_000.0
        // Year Actual = 630K (H1) + 840K (H2) = 1_470_000.0
        assertEquals(1_560_000.0, yearResult.sales.target, 0.001)
        assertEquals(1_470_000.0, yearResult.sales.actual, 0.001)
        assertEquals(90_000.0, yearResult.sales.gap, 0.001)

        val yearSalesRatio = 1470.0 / 1560.0
        assertEquals(yearSalesRatio * 80.0, yearResult.sales.kpiScore, 0.001)

        // Year Cov Target = 150 (Q1) + 180 (Q2) + 210 (Q3) + 210 (Q4) = 750
        // Year Cov Actual = 120 (Q1) + 180 (Q2) + 210 (Q3) + 252 (Q4) = 762
        assertEquals(750.0, yearResult.coverage.target, 0.001)
        assertEquals(762.0, yearResult.coverage.actual, 0.001)
        assertEquals(0.0, yearResult.coverage.gap, 0.001) // Achieved, gap = 0
        val yearCovRatio = 762.0 / 750.0 // 1.016
        assertEquals(yearCovRatio * 10.0, yearResult.coverage.kpiScore, 0.001)

        // Avg SKU = (3.5*3 + 4.3*3 + 4.7*3 + 4.8*3) / 12 = (10.5 + 12.9 + 14.1 + 14.4) / 12 = 51.9 / 12 = 4.325
        assertEquals(4.325, yearResult.sku.skuActual, 0.001)
        // SKU 4.325 >= 4.3 -> 6.5 points
        assertEquals(6.5, yearResult.sku.kpiScore, 0.001)
    }

    // ==========================================
    // 5. HELPER LIST METHODS
    // ==========================================
    @Test
    fun testAnalyzeAllQuarters_returns4Items() {
        val months = createSampleYear()
        val quarters = AnalyticsEngine.analyzeAllQuarters(months)
        assertEquals(4, quarters.size)
        assertEquals(Period.Q1, quarters[0].period)
        assertEquals(Period.Q2, quarters[1].period)
        assertEquals(Period.Q3, quarters[2].period)
        assertEquals(Period.Q4, quarters[3].period)
    }

    @Test
    fun testAnalyzeAllHalves_returns2Items() {
        val months = createSampleYear()
        val halves = AnalyticsEngine.analyzeAllHalves(months)
        assertEquals(2, halves.size)
        assertEquals(Period.H1, halves[0].period)
        assertEquals(Period.H2, halves[1].period)
    }

    // ==========================================
    // 6. ROBUSTNESS WITH EMPTY/PARTIAL MONTHS & NO_DATA
    // ==========================================
    @Test
    fun testAnalyze_emptyMonths_doesNotCrash() {
        val emptyList = emptyList<MonthEntity>()
        val result = AnalyticsEngine.analyze(emptyList, Period.Year)

        assertEquals(0, result.monthsCount)
        assertFalse(result.hasData)
        assertEquals(BottleneckCategory.NO_DATA, result.bottleneckCategory)
        assertEquals(0.0, result.sales.target, 0.001)
        assertEquals(0.0, result.sales.actual, 0.001)
        assertEquals(0.0, result.sales.kpiScore, 0.001)
        assertEquals(0.0, result.coverage.target, 0.001)
        assertEquals(0.0, result.coverage.actual, 0.001)
        assertEquals(0.0, result.sku.skuActual, 0.001)
        assertEquals(0.0, result.sku.kpiScore, 0.001)
        assertEquals(0.0, result.totalKpiScore, 0.001)
    }

    @Test
    fun testAnalyze_allZeroMonths_isNoData() {
        val zeroMonths = (0..11).map { MonthEntity(monthIndex = it) }
        val result = AnalyticsEngine.analyze(zeroMonths, Period.Q1)

        assertEquals(3, result.monthsCount)
        assertFalse(result.hasData)
        assertEquals(BottleneckCategory.NO_DATA, result.bottleneckCategory)
        assertTrue(result.bottleneckMessage.contains("Chưa có dữ liệu"))
    }
}
