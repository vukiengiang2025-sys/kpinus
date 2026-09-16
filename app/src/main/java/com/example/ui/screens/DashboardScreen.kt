package com.example.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.example.data.MonthEntity
import com.example.lib.KpiCalculator
import com.example.lib.SalesForecastEngine
import com.example.lib.SkuTierHelper
import com.example.ui.MainViewModel
import java.util.Calendar

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(viewModel: MainViewModel, navController: NavController) {
    val months by viewModel.months.collectAsStateWithLifecycle()
    val tasks by viewModel.tasks.collectAsStateWithLifecycle()

    val currentMonthIndex = viewModel.getCurrentMonthIndex()
    val currentYear = Calendar.getInstance().get(Calendar.YEAR)
    val currentMonth = months.find { it.monthIndex == currentMonthIndex } ?: MonthEntity(monthIndex = currentMonthIndex)
    val salesForecast = SalesForecastEngine.forecast(currentMonth)

    // Call KpiCalculator directly to ensure 100% adherence to Core rules
    val salesKpi = KpiCalculator.calcSalesKPI(currentMonth.salesActual, currentMonth.salesTarget)
    val coverageKpi = KpiCalculator.calcCoverageKPI(currentMonth.coverageActual, currentMonth.coverageTarget)
    val skuKpi = KpiCalculator.calcSKUKPI(currentMonth.skuActual)
    val totalKpi = KpiCalculator.calcTotalKPI(
        currentMonth.salesActual,
        currentMonth.salesTarget,
        currentMonth.coverageActual,
        currentMonth.coverageTarget,
        currentMonth.skuActual
    )

    val salesProgress = if (currentMonth.salesTarget > 0) 
        (currentMonth.salesActual / currentMonth.salesTarget * 100).toFloat()
    else 0f

    val coverageProgress = if (currentMonth.coverageTarget > 0)
        (currentMonth.coverageActual / currentMonth.coverageTarget * 100).toFloat()
    else 0f

    // Real KPI Gap calculations
    val salesGap = maxOf(0.0, currentMonth.salesTarget - currentMonth.salesActual)
    val coverageGap = maxOf(0.0, currentMonth.coverageTarget - currentMonth.coverageActual)

    // Dynamic next SKU threshold calculated via SkuTierHelper
    val nextSkuThreshold = SkuTierHelper.getNextThreshold(currentMonth.skuActual)
    val skuGap = SkuTierHelper.getSkuGap(currentMonth.skuActual)

    // Determine Bottleneck / Focus
    val isSalesBottleneck = salesProgress < 80f || (salesProgress < coverageProgress && salesProgress < 100f)
    val isCoverageBottleneck = coverageProgress < 80f || (coverageProgress < salesProgress && coverageProgress < 100f)
    val bottleneckMessage = when {
        coverageProgress < 80f -> "Bao phủ đang dưới 80% (ngưỡng đạt điểm). Đây là điểm nghẽn ưu tiên số 1 cần cải thiện ngay."
        salesProgress < 80f -> "Doanh số đang dưới 80% (ngưỡng đạt điểm). Cần tập trung các đơn hàng trọng điểm."
        coverageProgress < salesProgress && coverageProgress < 100f -> "Bao phủ đang chậm hơn doanh số (${coverageProgress.toInt()}% vs ${salesProgress.toInt()}%). Cần mở rộng thêm khách hàng ghé thăm."
        salesProgress < 100f -> "Doanh số còn thiếu ${"%,.0f".format(salesGap)}K để về đích tháng."
        skuGap > 0 -> "Doanh số và bao phủ đạt tốt. Tập trung đẩy thêm $skuGap SKU để nhảy mốc điểm tiếp theo ($nextSkuThreshold)."
        else -> "Chúc mừng! Tất cả các chỉ số KPI tháng đang đạt phong độ xuất sắc."
    }

    var showAddTaskDialog by remember { mutableStateOf(false) }
    var newTaskText by remember { mutableStateOf("") }
    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()
    
    Scaffold(
        modifier = Modifier.fillMaxSize(),
        snackbarHost = { SnackbarHost(snackbarHostState) },
        floatingActionButtonPosition = FabPosition.Center,
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { showAddTaskDialog = true },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                shape = RoundedCornerShape(32.dp),
                modifier = Modifier.padding(bottom = 16.dp)
            ) {
                Icon(Icons.Default.Add, contentDescription = "Thêm nhiệm vụ", modifier = Modifier.size(24.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("GIAO VIỆC MỚI", fontWeight = FontWeight.Black, style = MaterialTheme.typography.labelLarge)
            }
        },
        containerColor = MaterialTheme.colorScheme.surface
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 20.dp),
            contentPadding = PaddingValues(top = 24.dp, bottom = 120.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Header
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Xin chào, ${viewModel.settingsManager.userName}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontWeight = FontWeight.Medium
                        )
                        Text(
                            text = "NAM DƯỢC KPI",
                            style = MaterialTheme.typography.headlineSmall,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Black
                        )
                        Text(
                            text = "Tháng ${(currentMonthIndex + 1).toString().padStart(2, '0')} / $currentYear • Trung tâm điều hành",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        AssistChip(
                            onClick = { navController.navigate("analytics") },
                            label = { Text("Phân tích", fontWeight = FontWeight.Bold) },
                            leadingIcon = {
                                Icon(Icons.AutoMirrored.Filled.TrendingUp, contentDescription = null, modifier = Modifier.size(16.dp))
                            }
                        )
                        AssistChip(
                            onClick = { navController.navigate("yearly") },
                            label = { Text("12 Tháng", fontWeight = FontWeight.Bold) }
                        )
                    }
                }
            }

            // Command Center Hero: Total KPI Score
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "TỔNG ĐIỂM KPI THÁNG",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(verticalAlignment = Alignment.Bottom) {
                            Text(
                                text = "%.1f".format(totalKpi),
                                style = MaterialTheme.typography.displayMedium,
                                fontWeight = FontWeight.Black,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = " / 100+",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f),
                                modifier = Modifier.padding(bottom = 8.dp)
                            )
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                        LinearProgressIndicator(
                            progress = { (totalKpi.toFloat() / 100f).coerceIn(0f, 1f) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(8.dp)
                                .clip(CircleShape),
                            color = MaterialTheme.colorScheme.primary,
                            trackColor = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.15f)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceAround
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("Doanh số", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onPrimaryContainer)
                                Text("%.1f đ".format(salesKpi), fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onPrimaryContainer)
                            }
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("Bao phủ", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onPrimaryContainer)
                                Text("%.1f đ".format(coverageKpi), fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onPrimaryContainer)
                            }
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("SKU", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onPrimaryContainer)
                                Text("%.1f đ".format(skuKpi), fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onPrimaryContainer)
                            }
                        }
                    }
                }
            }

            // Key Metrics: Sales & Coverage
            item {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    KpiCard(
                        modifier = Modifier.weight(1f),
                        title = "DOANH SỐ",
                        actual = currentMonth.salesActual,
                        target = currentMonth.salesTarget,
                        progress = salesProgress,
                        unit = "K",
                        isCurrency = true
                    )
                    KpiCard(
                        modifier = Modifier.weight(1f),
                        title = "BAO PHỦ",
                        actual = currentMonth.coverageActual,
                        target = currentMonth.coverageTarget,
                        progress = coverageProgress,
                        unit = "NT",
                        isCurrency = false
                    )
                }
            }

            // SKU Focus Card (Dynamic threshold)
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "SKU FOCUS",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            val skuTargetLabel = if (nextSkuThreshold != null) "Mốc tiếp: $nextSkuThreshold" else "Đã đạt tối đa"
                            Text(
                                text = skuTargetLabel,
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.Bottom
                        ) {
                            Text(
                                text = "%.1f".format(currentMonth.skuActual),
                                style = MaterialTheme.typography.headlineMedium,
                                fontWeight = FontWeight.Black,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            if (nextSkuThreshold != null) {
                                Text(
                                    text = "Còn thiếu +%.1f SKU để lên mốc $nextSkuThreshold".format(skuGap),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontWeight = FontWeight.Medium
                                )
                            } else {
                                Text(
                                    text = "Đạt mức thưởng tối đa 7.5 điểm",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.primary,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        val skuProgress = if (nextSkuThreshold != null) {
                            (currentMonth.skuActual / nextSkuThreshold).toFloat().coerceIn(0f, 1f)
                        } else 1f
                        LinearProgressIndicator(
                            progress = { skuProgress },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(6.dp)
                                .clip(CircleShape),
                            color = MaterialTheme.colorScheme.primary,
                            trackColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f)
                        )
                    }
                }
            }

            // 🎯 KPI GAP CARD
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f))
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "🎯 KPI GAP (KHOẢNG CÁCH MỤC TIÊU)",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Black,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            GapItem(
                                label = "Doanh số",
                                value = if (salesGap > 0) "Còn %,.0f K".format(salesGap) else "Đã đạt 100%",
                                isAchieved = salesGap <= 0
                            )
                            GapItem(
                                label = "Bao phủ",
                                value = if (coverageGap > 0) "Còn %,.0f NT".format(coverageGap) else "Đã đạt 100%",
                                isAchieved = coverageGap <= 0
                            )
                            GapItem(
                                label = "SKU",
                                value = if (skuGap > 0) "Cần +%.1f".format(skuGap) else "Đạt đỉnh",
                                isAchieved = skuGap <= 0
                            )
                        }
                    }
                }
            }

            // ⚠️ FOCUS / BOTTLENECK CARD
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (isCoverageBottleneck || isSalesBottleneck) 
                            MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.8f)
                        else 
                            MaterialTheme.colorScheme.secondaryContainer
                    )
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = if (isCoverageBottleneck || isSalesBottleneck) Icons.Default.WarningAmber else Icons.Default.CheckCircle,
                                contentDescription = null,
                                tint = if (isCoverageBottleneck || isSalesBottleneck) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = if (isCoverageBottleneck || isSalesBottleneck) "ĐIỂM NGHẼN CẦN TẬP TRUNG" else "TRẠNG THÁI TỐT",
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.Black,
                                color = if (isCoverageBottleneck || isSalesBottleneck) MaterialTheme.colorScheme.onErrorContainer else MaterialTheme.colorScheme.onSecondaryContainer
                            )
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = bottleneckMessage,
                            style = MaterialTheme.typography.bodyMedium,
                            color = if (isCoverageBottleneck || isSalesBottleneck) MaterialTheme.colorScheme.onErrorContainer else MaterialTheme.colorScheme.onSecondaryContainer,
                            fontWeight = FontWeight.Medium
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        FilledTonalButton(
                            onClick = { navController.navigate("aicoach") },
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.align(Alignment.End)
                        ) {
                            Text("XEM PHÂN TÍCH VỚI AI COACH", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelMedium)
                            Spacer(modifier = Modifier.width(6.dp))
                            Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, modifier = Modifier.size(16.dp))
                        }
                    }
                }
            }

            // DỰ BÁO DOANH SỐ CUỐI THÁNG — dựa trên tốc độ bán hiện tại
            item {
                SalesForecastCard(forecast = salesForecast, target = currentMonth.salesTarget)
            }

            // BIỂU ĐỒ DOANH SỐ (TARGET VS ACTUAL) VỚI CÁC TAB BỘ LỌC
            item {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "BIỂU ĐỒ DOANH SỐ (TARGET VS ACTUAL)",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.Black,
                )
                
                var chartFilter by remember { mutableStateOf(0) }
                val filters = listOf("Năm", "Q1", "Q2", "Q3", "Q4", "H1", "H2")
                
                ScrollableTabRow(
                    selectedTabIndex = chartFilter,
                    edgePadding = 0.dp,
                    containerColor = MaterialTheme.colorScheme.surface,
                    modifier = Modifier.padding(vertical = 8.dp)
                ) {
                    filters.forEachIndexed { index, title ->
                        Tab(
                            selected = chartFilter == index,
                            onClick = { chartFilter = index },
                            text = { Text(title, fontWeight = FontWeight.Bold) }
                        )
                    }
                }
                
                val displayMonths = when (chartFilter) {
                    1 -> months.filter { it.monthIndex in 0..2 }
                    2 -> months.filter { it.monthIndex in 3..5 }
                    3 -> months.filter { it.monthIndex in 6..8 }
                    4 -> months.filter { it.monthIndex in 9..11 }
                    5 -> months.filter { it.monthIndex in 0..5 }
                    6 -> months.filter { it.monthIndex in 6..11 }
                    else -> months
                }
                
                val maxSales = (displayMonths.maxOfOrNull { it.salesActual.coerceAtLeast(it.salesTarget) } ?: 100.0).coerceAtLeast(100.0).toFloat()
                
                val primaryColor = MaterialTheme.colorScheme.primary
                val targetColor = MaterialTheme.colorScheme.tertiary
                
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(210.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Box(modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp)) {
                        Canvas(modifier = Modifier.fillMaxSize()) {
                            val count = displayMonths.size
                            if (count > 0) {
                                val totalSlotWidth = size.width / count
                                val barWidth = (totalSlotWidth * 0.35f).coerceAtMost(28.dp.toPx())
                                
                                displayMonths.forEachIndexed { index, month ->
                                    val actualHeight = (month.salesActual.toFloat() / maxSales) * (size.height - 20.dp.toPx())
                                    val targetHeight = (month.salesTarget.toFloat() / maxSales) * (size.height - 20.dp.toPx())
                                    
                                    val slotCenterX = (index * totalSlotWidth) + (totalSlotWidth / 2f)
                                    val targetX = slotCenterX - barWidth
                                    val actualX = slotCenterX + 2.dp.toPx()
                                    
                                    // Target Bar
                                    drawRoundRect(
                                        color = targetColor.copy(alpha = 0.45f),
                                        topLeft = Offset(targetX, size.height - targetHeight),
                                        size = Size(barWidth, targetHeight),
                                        cornerRadius = CornerRadius(4.dp.toPx())
                                    )
                                    // Actual Bar
                                    drawRoundRect(
                                        color = primaryColor,
                                        topLeft = Offset(actualX, size.height - actualHeight),
                                        size = Size(barWidth, actualHeight),
                                        cornerRadius = CornerRadius(4.dp.toPx())
                                    )
                                }
                            }
                        }
                    }
                }
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp),
                    horizontalArrangement = Arrangement.Center
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(modifier = Modifier
                            .size(12.dp)
                            .background(targetColor.copy(alpha = 0.5f), RoundedCornerShape(2.dp)))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Target (Kế hoạch)", style = MaterialTheme.typography.bodySmall)
                    }
                    Spacer(modifier = Modifier.width(20.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(modifier = Modifier
                            .size(12.dp)
                            .background(primaryColor, RoundedCornerShape(2.dp)))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Actual (Thực đạt)", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
                    }
                }
            }

            // HÀNH ĐỘNG HÔM NAY (TASKS)
            item {
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "HÀNH ĐỘNG HÔM NAY",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                        fontWeight = FontWeight.Black,
                    )
                    Text(
                        text = "${tasks.count { it.isCompleted }} / ${tasks.size} xong",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            item {
                TodayActionPlan(
                    salesGap = salesGap,
                    coverageGap = coverageGap,
                    skuGap = skuGap,
                    onAddAction = { text, type -> viewModel.addTask(text, priority = "high", type = type) }
                )
            }

            items(tasks) { task ->
                TaskRow(
                    text = task.text,
                    isCompleted = task.isCompleted,
                    onToggle = { viewModel.toggleTask(task.id, task.isCompleted) }
                )
            }
            
            if (tasks.isEmpty()) {
                item {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(Icons.Default.Info, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(40.dp))
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("Chưa có chiến dịch nào. Bấm 'Giao việc mới' để tạo.", color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }
        }
    }

    if (showAddTaskDialog) {
        AlertDialog(
            onDismissRequest = { showAddTaskDialog = false },
            title = { Text("GIAO VIỆC MỚI", fontWeight = FontWeight.Bold) },
            text = {
                OutlinedTextField(
                    value = newTaskText,
                    onValueChange = { newTaskText = it },
                    label = { Text("Nội dung công việc (ví dụ: Chăm sóc 15 NT)") },
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                Button(onClick = {
                    if (newTaskText.isNotBlank()) {
                        viewModel.addTask(newTaskText)
                        newTaskText = ""
                        showAddTaskDialog = false
                    }
                }) {
                    Text("TẠO")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddTaskDialog = false }) {
                    Text("HỦY")
                }
            }
        )
    }
}

@Composable
fun GapItem(label: String, value: String, isAchieved: Boolean) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Bold,
            color = if (isAchieved) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
        )
    }
}

@Composable
fun KpiCard(
    modifier: Modifier = Modifier,
    title: String,
    actual: Double,
    target: Double,
    progress: Float,
    unit: String = "",
    isCurrency: Boolean = true
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSecondaryContainer
                )
                Icon(
                    Icons.AutoMirrored.Filled.TrendingUp,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
            }
            Spacer(modifier = Modifier.height(12.dp))
            val actualFormatted = if (isCurrency) "%,.0f".format(actual) else "%,.0f".format(actual)
            val targetFormatted = if (isCurrency) "%,.0f".format(target) else "%,.0f".format(target)
            
            Text(
                text = "$actualFormatted $unit".trim(),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Black,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(8.dp))
            LinearProgressIndicator(
                progress = { (progress / 100f).coerceIn(0f, 1f) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(CircleShape),
                color = MaterialTheme.colorScheme.primary,
                trackColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f)
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "${progress.toInt()}% / $targetFormatted $unit".trim(),
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSecondaryContainer
            )
        }
    }
}

@Composable
fun TaskRow(text: String, isCompleted: Boolean, onToggle: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        onClick = onToggle,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = if (isCompleted) 0.5f else 1f)
        )
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(24.dp)
                    .clip(CircleShape)
                    .background(if (isCompleted) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f)),
                contentAlignment = Alignment.Center
            ) {
                if (isCompleted) {
                    Icon(Icons.Default.CheckCircle, contentDescription = null, tint = MaterialTheme.colorScheme.onPrimary, modifier = Modifier.size(16.dp))
                }
            }
            Spacer(modifier = Modifier.width(16.dp))
            Text(
                text = text, 
                style = MaterialTheme.typography.bodyLarge,
                color = if (isCompleted) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface,
                fontWeight = if (isCompleted) FontWeight.Normal else FontWeight.SemiBold
            )
        }
    }
}

@Composable
fun SalesForecastCard(forecast: com.example.lib.SalesForecast, target: Double) {
    val forecastColor = when {
        forecast.projectedAchievement >= 100.0 -> MaterialTheme.colorScheme.primary
        forecast.projectedAchievement >= 80.0 -> MaterialTheme.colorScheme.tertiary
        else -> MaterialTheme.colorScheme.error
    }
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Column {
                    Text("DỰ BÁO CUỐI THÁNG", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Black)
                    Text(
                        "Theo tốc độ bán ${forecast.elapsedDays}/${forecast.totalDays} ngày",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.75f)
                    )
                }
                Text("${forecast.probabilityPercent}%", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Black, color = forecastColor)
            }
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                ForecastMetric("Dự kiến đạt", "${forecast.projectedAchievement.toInt()}%", forecastColor)
                ForecastMetric("Dự kiến DS", "%,.0f K".format(forecast.projectedSales), MaterialTheme.colorScheme.onPrimaryContainer)
                ForecastMetric("Chạy/ngày", "%,.0f K".format(forecast.dailyRunRate), MaterialTheme.colorScheme.onPrimaryContainer)
            }
            LinearProgressIndicator(
                progress = { (forecast.projectedAchievement / 100.0).toFloat().coerceIn(0f, 1f) },
                modifier = Modifier.fillMaxWidth().height(7.dp).clip(CircleShape),
                color = forecastColor,
                trackColor = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.15f)
            )
            Text(
                text = if (target <= 0.0) "Nhập mục tiêu doanh số để bật dự báo." else "Xác suất đạt mục tiêu theo run-rate hiện tại: ${forecast.probabilityPercent}%",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
            )
        }
    }
}

@Composable
private fun ForecastMetric(label: String, value: String, color: Color) {
    Column(modifier = Modifier.widthIn(min = 80.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f))
        Text(value, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold, color = color)
    }
}

@Composable
fun TodayActionPlan(
    salesGap: Double,
    coverageGap: Double,
    skuGap: Double,
    onAddAction: (String, String) -> Unit
) {
    val actions = buildList {
        if (salesGap > 0) add("Chốt thêm %,.0f K doanh số để thu hẹp khoảng thiếu tháng này".format(salesGap) to "sales")
        if (coverageGap > 0) add("Mở thêm %,.0f điểm bao phủ từ nhóm khách hàng tiềm năng".format(coverageGap) to "coverage")
        if (skuGap > 0) add("Đẩy thêm %.1f SKU để chạm mốc thưởng tiếp theo".format(skuGap) to "sku")
        if (isEmpty()) add("Duy trì nhịp bán và chăm sóc khách hàng trọng điểm hôm nay" to "other")
    }
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.CheckCircle, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Column {
                    Text("ACTION PLAN HÔM NAY", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Black)
                    Text("Biến KPI Gap thành việc có thể làm ngay", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.75f))
                }
            }
            actions.forEachIndexed { index, action ->
                Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Text("${index + 1}", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Black, modifier = Modifier.width(24.dp))
                    Text(action.first, modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                    TextButton(onClick = { onAddAction(action.first, action.second) }) { Text("GIAO") }
                }
            }
        }
    }
}
