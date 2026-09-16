package com.example.lib

import com.example.data.MonthEntity
import java.util.Calendar

/**
 * Deterministic sales forecast for the current month.
 * Uses actual daily run-rate only; KPI scoring remains in KpiCalculator.
 */
data class SalesForecast(
    val elapsedDays: Int,
    val totalDays: Int,
    val projectedSales: Double,
    val projectedAchievement: Double,
    val probabilityPercent: Int,
    val dailyRunRate: Double
)

object SalesForecastEngine {
    fun forecast(month: MonthEntity, calendar: Calendar = Calendar.getInstance()): SalesForecast {
        val totalDays = calendar.getActualMaximum(Calendar.DAY_OF_MONTH)
        val elapsedDays = calendar.get(Calendar.DAY_OF_MONTH).coerceIn(1, totalDays)
        val runRate = month.salesActual / elapsedDays
        val projected = runRate * totalDays
        val achievement = if (month.salesTarget > 0) projected / month.salesTarget * 100.0 else 0.0
        val probability = when {
            month.salesTarget <= 0.0 -> 0
            achievement >= 110.0 -> 95
            achievement >= 100.0 -> 85
            achievement >= 90.0 -> 60
            achievement >= 80.0 -> 35
            else -> 15
        }
        return SalesForecast(
            elapsedDays = elapsedDays,
            totalDays = totalDays,
            projectedSales = projected,
            projectedAchievement = achievement,
            probabilityPercent = probability,
            dailyRunRate = runRate
        )
    }
}
