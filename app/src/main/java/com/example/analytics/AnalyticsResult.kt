package com.example.analytics

/**
 * High-level bottleneck category classified by AnalyticsEngine.
 */
enum class BottleneckCategory {
    NO_DATA,
    SALES,
    COVERAGE,
    SKU,
    BALANCED
}

/**
 * Metric summary containing Target, Actual, Achievement %, KPI Score, and Gap.
 */
data class MetricSummary(
    val target: Double,
    val actual: Double,
    val achievementRatio: Double, // actual / target
    val achievementPercentage: Double, // ratio * 100
    val kpiScore: Double, // Calculated via KpiCalculator
    val gap: Double // max(0, target - actual)
)

/**
 * SKU Summary for a period:
 * - skuActual: average SKU over the period months
 * - kpiScore: KpiCalculator.calcSKUKPI(skuActual)
 * - nextThreshold: next reward tier
 * - skuGap: difference to next threshold
 */
data class SkuSummary(
    val skuActual: Double,
    val kpiScore: Double,
    val nextThreshold: Double?,
    val skuGap: Double
)

/**
 * Complete Analytics Result for a given Period.
 * Derived entirely from Room MonthEntity list without altering database schema.
 *
 * Rules:
 * - hasData: true if at least one month has positive target or actual
 * - bottleneckCategory: domain classified bottleneck (NO_DATA, SALES, COVERAGE, SKU, BALANCED)
 * - bottleneckMessage: centralized domain message (UI only renders)
 */
data class AnalyticsResult(
    val period: Period,
    val monthsCount: Int,
    val hasData: Boolean,
    val bottleneckCategory: BottleneckCategory,
    val sales: MetricSummary,
    val coverage: MetricSummary,
    val sku: SkuSummary,
    val totalKpiScore: Double,
    val isSalesBottleneck: Boolean,
    val isCoverageBottleneck: Boolean,
    val bottleneckMessage: String
)
