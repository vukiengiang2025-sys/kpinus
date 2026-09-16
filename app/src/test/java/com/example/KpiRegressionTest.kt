package com.example

import com.example.lib.KpiCalculator
import com.example.lib.SkuTierHelper
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Unit regression tests verifying:
 * 1. KpiCalculator regression on all calculation boundaries
 *    - salesActual >= salesTarget & salesActual < salesTarget (and < 80% threshold)
 *    - coverageActual >= coverageTarget & coverageActual < coverageTarget (and < 80% threshold)
 *    - SKU boundaries (< 3.3, = 3.3, = 3.5, = 4.7, > 4.7)
 *    - Bonus calculation & coefficient tiers
 * 2. SkuTierHelper threshold stepping & dynamic gaps
 * 3. KPI Gap edge cases (gap >= 0, gap = 0 on achievement)
 */
class KpiRegressionTest {

    // ==========================================
    // 1. SALES KPI BOUNDARY TESTS
    // Rule: ratio < 0.8 -> 0.0; ratio >= 0.8 -> min(ratio * 80.0, 150.0)
    // ==========================================
    @Test
    fun testSalesKpi_below80Percent_returnsZero() {
        val target = 100_000.0
        val actual79 = 79_999.0
        assertEquals(0.0, KpiCalculator.calcSalesKPI(actual79, target), 0.001)
    }

    @Test
    fun testSalesKpi_at80Percent_returns64Points() {
        val target = 100_000.0
        val actual80 = 80_000.0
        // 0.8 * 80 = 64.0
        assertEquals(64.0, KpiCalculator.calcSalesKPI(actual80, target), 0.001)
    }

    @Test
    fun testSalesKpi_underTarget_actualLessThanTarget() {
        val target = 100_000.0
        val actual90 = 90_000.0
        // 0.9 * 80 = 72.0
        assertEquals(72.0, KpiCalculator.calcSalesKPI(actual90, target), 0.001)
    }

    @Test
    fun testSalesKpi_at100Percent_returns80Points() {
        val target = 100_000.0
        val actual100 = 100_000.0
        // 1.0 * 80 = 80.0
        assertEquals(80.0, KpiCalculator.calcSalesKPI(actual100, target), 0.001)
    }

    @Test
    fun testSalesKpi_overTarget_actualGreaterThanTarget_capsAt150() {
        val target = 100_000.0
        val actual150 = 150_000.0
        assertEquals(120.0, KpiCalculator.calcSalesKPI(actual150, target), 0.001)

        val actual200 = 200_000.0 // 2.0 * 80 = 160 -> cap at 150.0
        assertEquals(150.0, KpiCalculator.calcSalesKPI(actual200, target), 0.001)
    }

    @Test
    fun testSalesKpi_zeroOrNegativeTarget_returnsZero() {
        assertEquals(0.0, KpiCalculator.calcSalesKPI(100_000.0, 0.0), 0.001)
        assertEquals(0.0, KpiCalculator.calcSalesKPI(100_000.0, -10.0), 0.001)
    }

    // ==========================================
    // 2. COVERAGE KPI BOUNDARY TESTS
    // Rule: ratio < 0.8 -> 0.0; ratio >= 0.8 -> min(ratio * 10.0, 15.0)
    // ==========================================
    @Test
    fun testCoverageKpi_below80Percent_returnsZero() {
        val target = 100.0
        val actual79 = 79.0
        assertEquals(0.0, KpiCalculator.calcCoverageKPI(actual79, target), 0.001)
    }

    @Test
    fun testCoverageKpi_at80Percent_returns8Points() {
        val target = 100.0
        val actual80 = 80.0
        assertEquals(8.0, KpiCalculator.calcCoverageKPI(actual80, target), 0.001)
    }

    @Test
    fun testCoverageKpi_underTarget_actualLessThanTarget() {
        val target = 100.0
        val actual95 = 95.0
        // 0.95 * 10 = 9.5
        assertEquals(9.5, KpiCalculator.calcCoverageKPI(actual95, target), 0.001)
    }

    @Test
    fun testCoverageKpi_at100Percent_returns10Points() {
        val target = 100.0
        val actual100 = 100.0
        assertEquals(10.0, KpiCalculator.calcCoverageKPI(actual100, target), 0.001)
    }

    @Test
    fun testCoverageKpi_overTarget_actualGreaterThanTarget_capsAt15() {
        val target = 100.0
        val actual120 = 120.0
        assertEquals(12.0, KpiCalculator.calcCoverageKPI(actual120, target), 0.001)

        val actual180 = 180.0 // 1.8 * 10 = 18 -> cap at 15.0
        assertEquals(15.0, KpiCalculator.calcCoverageKPI(actual180, target), 0.001)
    }

    @Test
    fun testCoverageKpi_zeroOrNegativeTarget_returnsZero() {
        assertEquals(0.0, KpiCalculator.calcCoverageKPI(50.0, 0.0), 0.001)
        assertEquals(0.0, KpiCalculator.calcCoverageKPI(50.0, -5.0), 0.001)
    }

    // ==========================================
    // 3. SKU KPI BOUNDARY TESTS (as requested)
    // SKU < 3.3 -> 0.0
    // SKU = 3.3 -> 4.0
    // SKU = 3.5 -> 4.5
    // SKU = 3.7 -> 5.0
    // SKU = 3.9 -> 5.5
    // SKU = 4.1 -> 6.0
    // SKU = 4.3 -> 6.5
    // SKU = 4.5 -> 7.0
    // SKU = 4.7 -> 7.5
    // SKU > 4.7 -> 7.5
    // ==========================================
    @Test
    fun testSkuKpi_below3_3_returnsZero() {
        assertEquals(0.0, KpiCalculator.calcSKUKPI(0.0), 0.001)
        assertEquals(0.0, KpiCalculator.calcSKUKPI(2.5), 0.001)
        assertEquals(0.0, KpiCalculator.calcSKUKPI(3.29), 0.001)
    }

    @Test
    fun testSkuKpi_at3_3_returns4_0() {
        assertEquals(4.0, KpiCalculator.calcSKUKPI(3.3), 0.001)
        assertEquals(4.0, KpiCalculator.calcSKUKPI(3.4), 0.001)
    }

    @Test
    fun testSkuKpi_at3_5_returns4_5() {
        assertEquals(4.5, KpiCalculator.calcSKUKPI(3.5), 0.001)
        assertEquals(4.5, KpiCalculator.calcSKUKPI(3.69), 0.001)
    }

    @Test
    fun testSkuKpi_intermediateTiers() {
        assertEquals(5.0, KpiCalculator.calcSKUKPI(3.7), 0.001)
        assertEquals(5.5, KpiCalculator.calcSKUKPI(3.9), 0.001)
        assertEquals(6.0, KpiCalculator.calcSKUKPI(4.1), 0.001)
        assertEquals(6.5, KpiCalculator.calcSKUKPI(4.3), 0.001)
        assertEquals(7.0, KpiCalculator.calcSKUKPI(4.5), 0.001)
    }

    @Test
    fun testSkuKpi_at4_7_returns7_5() {
        assertEquals(7.5, KpiCalculator.calcSKUKPI(4.7), 0.001)
    }

    @Test
    fun testSkuKpi_greaterThan4_7_returns7_5() {
        assertEquals(7.5, KpiCalculator.calcSKUKPI(4.8), 0.001)
        assertEquals(7.5, KpiCalculator.calcSKUKPI(5.5), 0.001)
        assertEquals(7.5, KpiCalculator.calcSKUKPI(10.0), 0.001)
    }

    // ==========================================
    // 4. SKU TIER HELPER DYNAMIC RESOLUTION
    // ==========================================
    @Test
    fun testSkuTierHelper_getNextThreshold() {
        assertEquals(3.3, SkuTierHelper.getNextThreshold(0.0)!!, 0.001)
        assertEquals(3.3, SkuTierHelper.getNextThreshold(3.0)!!, 0.001)
        assertEquals(3.5, SkuTierHelper.getNextThreshold(3.3)!!, 0.001)
        assertEquals(3.5, SkuTierHelper.getNextThreshold(3.4)!!, 0.001)
        assertEquals(4.7, SkuTierHelper.getNextThreshold(4.5)!!, 0.001)
        assertEquals(4.7, SkuTierHelper.getNextThreshold(4.6)!!, 0.001)
        assertNull(SkuTierHelper.getNextThreshold(4.7))
        assertNull(SkuTierHelper.getNextThreshold(5.0))
    }

    @Test
    fun testSkuTierHelper_getSkuGap() {
        // When sku is 3.0, next is 3.3, gap is 0.3
        assertEquals(0.3, SkuTierHelper.getSkuGap(3.0), 0.001)
        // When sku is 3.3, next is 3.5, gap is 0.2
        assertEquals(0.2, SkuTierHelper.getSkuGap(3.3), 0.001)
        // When sku is 4.7 (max achieved), gap is 0.0
        assertEquals(0.0, SkuTierHelper.getSkuGap(4.7), 0.001)
        // When sku is 5.0 (> max), gap is 0.0
        assertEquals(0.0, SkuTierHelper.getSkuGap(5.0), 0.001)
    }

    // ==========================================
    // 5. TOTAL KPI CALCULATION INTEGRATION
    // ==========================================
    @Test
    fun testCalcTotalKPI_sumOfAllThreeComponents() {
        val salesActual = 100_000.0
        val salesTarget = 100_000.0 // 80.0 points
        val covActual = 100.0
        val covTarget = 100.0       // 10.0 points
        val sku = 4.7               // 7.5 points

        val total = KpiCalculator.calcTotalKPI(salesActual, salesTarget, covActual, covTarget, sku)
        assertEquals(97.5, total, 0.001)
    }

    // ==========================================
    // 6. BONUS TIER REGRESSION
    // ==========================================
    @Test
    fun testCalculateBonus_allTiers() {
        // Base bonus: 10_000 * 0.03 + 10_000 * 0.05 + 10_000 * 0.08 = 300 + 500 + 800 = 1600.0
        val n1 = 10_000.0
        val n2 = 10_000.0
        val n3 = 10_000.0

        // kpi < 70 -> coeff = 0, bonus = 0
        assertEquals(0.0, KpiCalculator.calculateBonus(n1, n2, n3, 69.9), 0.001)

        // kpi = 70 -> coeff = 0.5, extra = 0 -> 1600 * 0.5 = 800.0
        assertEquals(800.0, KpiCalculator.calculateBonus(n1, n2, n3, 70.0), 0.001)

        // kpi = 100 -> coeff = 1.0, extra = 0 -> 1600 * 1.0 = 1600.0
        assertEquals(1600.0, KpiCalculator.calculateBonus(n1, n2, n3, 100.0), 0.001)

        // kpi = 120 -> coeff = 1.2, extra = 500_000 -> 1600 * 1.2 + 500_000 = 501920.0
        assertEquals(501_920.0, KpiCalculator.calculateBonus(n1, n2, n3, 120.0), 0.001)

        // kpi = 130 -> coeff = 1.3, extra = 1_000_000 -> 1600 * 1.3 + 1_000_000 = 1_002_080.0
        assertEquals(1_002_080.0, KpiCalculator.calculateBonus(n1, n2, n3, 130.0), 0.001)

        // kpi = 150 -> coeff = 1.5, extra = 1_500_000 -> 1600 * 1.5 + 1_500_000 = 1_502_400.0
        assertEquals(1_502_400.0, KpiCalculator.calculateBonus(n1, n2, n3, 150.0), 0.001)
    }

    // ==========================================
    // 7. GAP CALCULATIONS EDGE CASES (UI Formula logic)
    // ==========================================
    @Test
    fun testGapFormulas_cannotBeNegative() {
        val targetSales = 100.0
        val actualSalesOver = 120.0
        val salesGapOver = maxOf(0.0, targetSales - actualSalesOver)
        assertEquals(0.0, salesGapOver, 0.001)

        val actualSalesUnder = 80.0
        val salesGapUnder = maxOf(0.0, targetSales - actualSalesUnder)
        assertEquals(20.0, salesGapUnder, 0.001)
    }
}
