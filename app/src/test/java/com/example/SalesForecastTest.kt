package com.example

import com.example.data.MonthEntity
import com.example.lib.SalesForecastEngine
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Calendar

class SalesForecastTest {
    @Test
    fun forecast_usesCurrentRunRate() {
        val calendar = Calendar.getInstance().apply {
            set(Calendar.DAY_OF_MONTH, 10)
            set(Calendar.MONTH, Calendar.JANUARY)
        }
        val result = SalesForecastEngine.forecast(
            MonthEntity(monthIndex = 0, salesTarget = 3_100.0, salesActual = 1_000.0),
            calendar
        )
        assertEquals(31, result.totalDays)
        assertEquals(10, result.elapsedDays)
        assertEquals(3_100.0, result.projectedSales, 0.001)
        assertEquals(100.0, result.projectedAchievement, 0.001)
        assertTrue(result.probabilityPercent >= 80)
    }

    @Test
    fun forecast_withoutTarget_returnsZeroProbability() {
        val result = SalesForecastEngine.forecast(MonthEntity(monthIndex = 0, salesActual = 100.0))
        assertEquals(0, result.probabilityPercent)
    }
}
