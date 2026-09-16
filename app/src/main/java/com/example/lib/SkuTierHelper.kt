package com.example.lib

/**
 * Helper to determine SKU thresholds and target tiers.
 * Bridges KpiCalculator.calcSKUKPI without duplicating logic or modifying Core files.
 *
 * Core KpiCalculator.calcSKUKPI logic:
 *   sku >= 4.7 -> 7.5
 *   sku >= 4.5 -> 7.0
 *   sku >= 4.3 -> 6.5
 *   sku >= 4.1 -> 6.0
 *   sku >= 3.9 -> 5.5
 *   sku >= 3.7 -> 5.0
 *   sku >= 3.5 -> 4.5
 *   sku >= 3.3 -> 4.0
 *   else -> 0.0
 */
object SkuTierHelper {
    val THRESHOLDS = listOf(3.3, 3.5, 3.7, 3.9, 4.1, 4.3, 4.5, 4.7)

    fun getNextThreshold(currentSku: Double): Double? {
        return THRESHOLDS.firstOrNull { it > currentSku }
    }

    fun getSkuGap(currentSku: Double): Double {
        val next = getNextThreshold(currentSku) ?: return 0.0
        return maxOf(0.0, next - currentSku)
    }
}
