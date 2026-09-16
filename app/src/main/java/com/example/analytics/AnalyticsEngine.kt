package com.example.analytics

import com.example.data.MonthEntity
import com.example.lib.KpiCalculator
import com.example.lib.SkuTierHelper

/**
 * AnalyticsEngine: Pure calculation engine that computes derived analytics
 * across various time periods (Month, Q1..Q4, H1, H2, Year) from existing Room data.
 *
 * Core Principles:
 * 1. ZERO alterations to Room Database schema (zero new columns).
 * 2. Strict delegation to KpiCalculator.kt for KPI scores.
 * 3. Strict delegation to SkuTierHelper.kt for SKU threshold logic.
 * 4. Deterministic aggregation:
 *    - Sales & Coverage: Sum of targets and actuals across months in the period.
 *    - SKU: Arithmetic mean across months in the period.
 *    - KPI Scores: Evaluated by KpiCalculator using the aggregated targets and actuals.
 *    - NEVER sum up monthly KPI scores for a period; KPI is calculated on aggregated totals.
 * 5. Distinct Empty State: Differentiates NO_DATA from 0% achievement with actual targets.
 * 6. Domain-level Bottleneck Classification: UI only renders result messages.
 */
object AnalyticsEngine {

    fun analyze(months: List<MonthEntity>, period: Period): AnalyticsResult {
        // Filter months relevant to this period
        val targetIndices = period.monthIndices.toSet()
        val periodMonths = months.filter { it.monthIndex in targetIndices }

        val monthsCount = periodMonths.size

        // Check if there is meaningful data in this period
        val hasData = periodMonths.any {
            it.salesTarget > 0.0 || it.salesActual > 0.0 ||
            it.coverageTarget > 0.0 || it.coverageActual > 0.0 ||
            it.skuActual > 0.0
        }

        // 1. Sales Aggregation
        val salesTarget = periodMonths.sumOf { it.salesTarget }
        val salesActual = periodMonths.sumOf { it.salesActual }
        val salesRatio = if (salesTarget > 0.0) salesActual / salesTarget else 0.0
        val salesPercentage = salesRatio * 100.0
        val salesKpi = KpiCalculator.calcSalesKPI(salesActual, salesTarget)
        val salesGap = maxOf(0.0, salesTarget - salesActual)

        val salesSummary = MetricSummary(
            target = salesTarget,
            actual = salesActual,
            achievementRatio = salesRatio,
            achievementPercentage = salesPercentage,
            kpiScore = salesKpi,
            gap = salesGap
        )

        // 2. Coverage Aggregation
        val covTarget = periodMonths.sumOf { it.coverageTarget }
        val covActual = periodMonths.sumOf { it.coverageActual }
        val covRatio = if (covTarget > 0.0) covActual / covTarget else 0.0
        val covPercentage = covRatio * 100.0
        val covKpi = KpiCalculator.calcCoverageKPI(covActual, covTarget)
        val covGap = maxOf(0.0, covTarget - covActual)

        val coverageSummary = MetricSummary(
            target = covTarget,
            actual = covActual,
            achievementRatio = covRatio,
            achievementPercentage = covPercentage,
            kpiScore = covKpi,
            gap = covGap
        )

        // 3. SKU Aggregation (Average across months in the period)
        val avgSku = if (monthsCount > 0) {
            periodMonths.sumOf { it.skuActual } / monthsCount
        } else {
            0.0
        }
        val skuKpi = KpiCalculator.calcSKUKPI(avgSku)
        val nextSkuThreshold = SkuTierHelper.getNextThreshold(avgSku)
        val skuGap = SkuTierHelper.getSkuGap(avgSku)

        val skuSummary = SkuSummary(
            skuActual = avgSku,
            kpiScore = skuKpi,
            nextThreshold = nextSkuThreshold,
            skuGap = skuGap
        )

        // 4. Total KPI Score (Evaluated strictly via Core sum of component scores on period aggregates)
        val totalKpi = salesKpi + covKpi + skuKpi

        // 5. Bottleneck Analysis for Period
        if (!hasData) {
            return AnalyticsResult(
                period = period,
                monthsCount = monthsCount,
                hasData = false,
                bottleneckCategory = BottleneckCategory.NO_DATA,
                sales = salesSummary,
                coverage = coverageSummary,
                sku = skuSummary,
                totalKpiScore = 0.0,
                isSalesBottleneck = false,
                isCoverageBottleneck = false,
                bottleneckMessage = "${period.displayName}: Chưa có dữ liệu thiết lập cho kỳ này."
            )
        }

        val isSalesBottleneck = salesPercentage < 80.0 || (salesPercentage < covPercentage && salesPercentage < 100.0)
        val isCoverageBottleneck = covPercentage < 80.0 || (covPercentage < salesPercentage && covPercentage < 100.0)

        val (category, bottleneckMsg) = when {
            covPercentage < 80.0 -> Pair(
                BottleneckCategory.COVERAGE,
                "${period.displayName}: Bao phủ dưới 80% (${"%.1f".format(covPercentage)}%). Đây là điểm nghẽn nghiêm trọng."
            )
            salesPercentage < 80.0 -> Pair(
                BottleneckCategory.SALES,
                "${period.displayName}: Doanh số dưới 80% (${"%.1f".format(salesPercentage)}%). Cần kích hoạt các đơn hàng trọng điểm."
            )
            covPercentage < salesPercentage && covPercentage < 100.0 -> Pair(
                BottleneckCategory.COVERAGE,
                "${period.displayName}: Bao phủ (${"%.0f".format(covPercentage)}%) đang chậm nhịp so với doanh số (${"%.0f".format(salesPercentage)}%)."
            )
            salesPercentage < 100.0 -> Pair(
                BottleneckCategory.SALES,
                "${period.displayName}: Doanh số còn thiếu ${"%,.0f".format(salesGap)}K để hoàn thành kế hoạch kỳ."
            )
            skuGap > 0.0 -> Pair(
                BottleneckCategory.SKU,
                "${period.displayName}: Doanh số và bao phủ hoàn thành tốt. Đẩy thêm ${"%.1f".format(skuGap)} SKU để tăng điểm thưởng (mốc $nextSkuThreshold)."
            )
            else -> Pair(
                BottleneckCategory.BALANCED,
                "${period.displayName}: Tất cả chỉ số KPI trong kỳ đạt phong độ xuất sắc và cân bằng."
            )
        }

        return AnalyticsResult(
            period = period,
            monthsCount = monthsCount,
            hasData = true,
            bottleneckCategory = category,
            sales = salesSummary,
            coverage = coverageSummary,
            sku = skuSummary,
            totalKpiScore = totalKpi,
            isSalesBottleneck = isSalesBottleneck,
            isCoverageBottleneck = isCoverageBottleneck,
            bottleneckMessage = bottleneckMsg
        )
    }

    /**
     * Compute analytics across all 4 Quarters.
     */
    fun analyzeAllQuarters(months: List<MonthEntity>): List<AnalyticsResult> {
        return Period.allQuarters().map { analyze(months, it) }
    }

    /**
     * Compute analytics across both Halves (H1, H2).
     */
    fun analyzeAllHalves(months: List<MonthEntity>): List<AnalyticsResult> {
        return Period.allHalves().map { analyze(months, it) }
    }
}
