package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.example.analytics.BottleneckCategory
import com.example.analytics.MetricSummary
import com.example.analytics.Period
import com.example.analytics.SkuSummary
import com.example.ui.MainViewModel
import java.text.NumberFormat
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AnalyticsScreen(viewModel: MainViewModel, navController: NavController) {
    val selectedPeriod by viewModel.selectedAnalyticsPeriod.collectAsStateWithLifecycle()
    val analyticsResult by viewModel.analyticsResult.collectAsStateWithLifecycle()

    val availablePeriods = listOf(
        Period.Year,
        Period.Q1,
        Period.Q2,
        Period.Q3,
        Period.Q4,
        Period.H1,
        Period.H2
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "PHÂN TÍCH KPI",
                        fontWeight = FontWeight.Black,
                        modifier = Modifier.testTag("analytics_title")
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant,
                    titleContentColor = MaterialTheme.colorScheme.onSurfaceVariant
                )
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .testTag("analytics_list"),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // 1. Period Selector Chips
            item {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "CHỌN KỲ PHÂN TÍCH",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("period_selector_row")
                    ) {
                        items(availablePeriods) { period ->
                            val isSelected = period == selectedPeriod
                            FilterChip(
                                selected = isSelected,
                                onClick = { viewModel.selectAnalyticsPeriod(period) },
                                label = { Text(period.displayName, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal) },
                                leadingIcon = if (isSelected) {
                                    { Icon(Icons.AutoMirrored.Filled.TrendingUp, contentDescription = null, modifier = Modifier.size(16.dp)) }
                                } else null,
                                modifier = Modifier.testTag("chip_period_${period.displayName.replace(" ", "_")}")
                            )
                        }
                    }
                }
            }

            // 2. Empty State / No Data Alert vs Active Period
            if (!analyticsResult.hasData) {
                item {
                    NoDataCard(period = selectedPeriod, onNavigateToYearly = { navController.navigate("yearly") })
                }
            } else {
                // 3. KPI Score Card (Aggregated Core Score)
                item {
                    PeriodKpiScoreCard(
                        periodName = selectedPeriod.displayName,
                        totalKpi = analyticsResult.totalKpiScore,
                        monthsCount = analyticsResult.monthsCount
                    )
                }

                // 4. Focus / Bottleneck Banner (Domain-driven)
                item {
                    FocusBannerCard(
                        category = analyticsResult.bottleneckCategory,
                        message = analyticsResult.bottleneckMessage
                    )
                }

                // 5. Sales Metric Card
                item {
                    MetricDetailCard(
                        title = "DOANH SỐ",
                        unit = "K",
                        metric = analyticsResult.sales,
                        color = MaterialTheme.colorScheme.primary,
                        tagPrefix = "sales"
                    )
                }

                // 6. Coverage Metric Card
                item {
                    MetricDetailCard(
                        title = "BAO PHỦ",
                        unit = "NT",
                        metric = analyticsResult.coverage,
                        color = MaterialTheme.colorScheme.secondary,
                        tagPrefix = "coverage"
                    )
                }

                // 7. SKU Metric Card
                item {
                    SkuDetailCard(
                        sku = analyticsResult.sku,
                        tagPrefix = "sku"
                    )
                }
            }
        }
    }
}

@Composable
fun PeriodKpiScoreCard(periodName: String, totalKpi: Double, monthsCount: Int) {
    val scoreColor = when {
        totalKpi >= 100.0 -> MaterialTheme.colorScheme.primary
        totalKpi >= 80.0 -> MaterialTheme.colorScheme.tertiary
        else -> MaterialTheme.colorScheme.error
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("card_period_kpi_score"),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(
                text = "TỔNG ĐIỂM KPI — $periodName",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
            Text(
                text = "%.1f".format(totalKpi),
                style = MaterialTheme.typography.displayMedium,
                fontWeight = FontWeight.Black,
                color = scoreColor
            )
            Text(
                text = "Tính theo tổng hợp $monthsCount tháng trong kỳ (Chuẩn Core Freeze)",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
            )
        }
    }
}

@Composable
fun FocusBannerCard(category: BottleneckCategory, message: String) {
    val (bgColor, icon, title) = when (category) {
        BottleneckCategory.COVERAGE -> Triple(
            MaterialTheme.colorScheme.errorContainer,
            Icons.Default.Warning,
            "ĐIỂM NGHẼN: BAO PHỦ"
        )
        BottleneckCategory.SALES -> Triple(
            MaterialTheme.colorScheme.tertiaryContainer,
            Icons.Default.Warning,
            "ĐIỂM NGHẼN: DOANH SỐ"
        )
        BottleneckCategory.SKU -> Triple(
            MaterialTheme.colorScheme.secondaryContainer,
            Icons.Default.Info,
            "ƯU TIÊN: ĐẨY SKU FOCUS"
        )
        BottleneckCategory.BALANCED -> Triple(
            MaterialTheme.colorScheme.primaryContainer,
            Icons.Default.CheckCircle,
            "PHONG ĐỘ XUẤT SẮC"
        )
        BottleneckCategory.NO_DATA -> Triple(
            MaterialTheme.colorScheme.surfaceVariant,
            Icons.Default.Info,
            "CHƯA CÓ DỮ LIỆU"
        )
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("card_focus_banner"),
        colors = CardDefaults.cardColors(containerColor = bgColor)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.size(28.dp)
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Black
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = message,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}

@Composable
fun MetricDetailCard(
    title: String,
    unit: String,
    metric: MetricSummary,
    color: Color,
    tagPrefix: String
) {
    val formatter = NumberFormat.getNumberInstance(Locale.forLanguageTag("vi-VN"))

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("card_metric_$tagPrefix"),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Black,
                    color = color
                )
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = color.copy(alpha = 0.15f)
                ) {
                    Text(
                        text = "KPI: ${"%.1f".format(metric.kpiScore)} đ",
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = color
                    )
                }
            }

            // Progress bar
            LinearProgressIndicator(
                progress = { (metric.achievementPercentage / 100f).toFloat().coerceIn(0f, 1f) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(RoundedCornerShape(4.dp)),
                color = color
            )

            // Details 2x2 Grid
            Row(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("Target", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.outline)
                    Text(
                        "${formatter.format(metric.target.toLong())} $unit",
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.SemiBold
                    )
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text("Actual", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.outline)
                    Text(
                        "${formatter.format(metric.actual.toLong())} $unit",
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Row(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("Đạt (%)", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.outline)
                    Text(
                        "${"%.1f".format(metric.achievementPercentage)}%",
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Bold,
                        color = if (metric.achievementPercentage >= 80.0) color else MaterialTheme.colorScheme.error
                    )
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text("Khoảng cách (Gap)", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.outline)
                    val gapText = if (metric.gap <= 0.0) "Đã hoàn thành" else "-${formatter.format(metric.gap.toLong())} $unit"
                    Text(
                        gapText,
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Bold,
                        color = if (metric.gap <= 0.0) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
                    )
                }
            }
        }
    }
}

@Composable
fun SkuDetailCard(sku: SkuSummary, tagPrefix: String) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("card_metric_$tagPrefix"),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "SKU FOCUS",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Black,
                    color = MaterialTheme.colorScheme.tertiary
                )
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.tertiary.copy(alpha = 0.15f)
                ) {
                    Text(
                        text = "KPI: ${"%.1f".format(sku.kpiScore)} đ",
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.tertiary
                    )
                }
            }

            Row(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("Trung bình kỳ", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.outline)
                    Text(
                        "%.2f".format(sku.skuActual),
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Black,
                        color = MaterialTheme.colorScheme.tertiary
                    )
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text("Mốc thưởng kế tiếp", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.outline)
                    val nextText = sku.nextThreshold?.let { "$it (Thiếu ${"%.1f".format(sku.skuGap)})" } ?: "Đạt mốc tối đa (7.5đ)"
                    Text(
                        nextText,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        color = if (sku.skuGap > 0.0) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
                    )
                }
            }
        }
    }
}

@Composable
fun NoDataCard(period: Period, onNavigateToYearly: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("card_no_data"),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Info,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(40.dp)
            )
            Text(
                text = "Chưa có dữ liệu cho ${period.displayName}",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = "Vui lòng nhập mục tiêu và thực đạt của các tháng trong kỳ tại màn hình Mục tiêu năm để hệ thống phân tích.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                modifier = Modifier.fillMaxWidth()
            )
            Button(
                onClick = onNavigateToYearly,
                modifier = Modifier.testTag("btn_navigate_yearly")
            ) {
                Text("Nhập mục tiêu năm")
            }
        }
    }
}
