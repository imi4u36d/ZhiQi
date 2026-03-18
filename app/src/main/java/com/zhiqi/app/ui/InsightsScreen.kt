package com.zhiqi.app.ui

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoGraph
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.HealthAndSafety
import androidx.compose.material.icons.filled.Hotel
import androidx.compose.material.icons.filled.InvertColors
import androidx.compose.material.icons.filled.LocalHospital
import androidx.compose.material.icons.filled.Mood
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Thermostat
import androidx.compose.material.icons.filled.ThumbUp
import androidx.compose.material.icons.filled.Today
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.zhiqi.app.data.DailyIndicatorEntity
import com.zhiqi.app.data.DailyIndicatorRepository
import com.zhiqi.app.data.RecordEntity
import com.zhiqi.app.data.RecordRepository
import com.zhiqi.app.R
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

private const val PERIOD_STATUS_KEY = "月经状态"
private const val PERIOD_STARTED = "start"
private const val PERIOD_ENDED = "end"
private const val MAX_PERIOD_DAYS = 10
private const val CALENDAR_PAGER_TOTAL_PAGES = 20_000
private const val CALENDAR_PAGER_START_PAGE = CALENDAR_PAGER_TOTAL_PAGES / 2
private const val CALENDAR_ROW_HEIGHT_DP = 52
private const val CALENDAR_BODY_EXTRA_DP = 70

private val PHASE_COLOR_PERIOD = Color(0xFF8F4145)
private val PHASE_COLOR_PREDICTED_PERIOD = Color(0xFFF2D9D7)
private val PHASE_COLOR_FERTILE = Color(0xFF6F7E63)
private val PHASE_COLOR_OVULATION = Color(0xFF53624A)
private val PHASE_COLOR_LUTEAL = Color(0xFFD7B44A)

private val CALENDAR_BG_SELECTED = Color(0xFFF6EEE6)
private val CALENDAR_BG_ACTUAL_PERIOD = Color(0xFFF2D9D7)
private val CALENDAR_BG_PREDICTED_PERIOD = Color(0xFFF7EFE8)
private val CALENDAR_BG_OVULATION = Color(0xFFE3E8DC)

private val CALENDAR_TEXT_DEFAULT = Color(0xFF5B5049)
private val CALENDAR_TEXT_SELECTED = Color(0xFF6E2D31)
private val CALENDAR_TEXT_PERIOD = Color(0xFF6E2D31)
private val CALENDAR_TEXT_PREDICTED_PERIOD = Color(0xFF8F4145)
private val CALENDAR_TEXT_FERTILE = Color(0xFF53624A)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InsightsScreen(
    repository: RecordRepository,
    indicatorRepository: DailyIndicatorRepository,
    cycleSettingsVersion: Int,
    onAddRecord: (String?) -> Unit,
    onSelectDateForEntry: (Long) -> Unit,
    onSaveIndicator: suspend (DailyIndicatorEntity) -> Unit,
    onDeletePeriodStatus: suspend (Long) -> Unit,
    onCycleChanged: () -> Unit
) {
    val context = LocalContext.current
    val cycleManager = remember { CycleSettingsManager(context) }
    val records by repository.records().collectAsState(initial = emptyList())
    val allIndicators by indicatorRepository.allIndicators().collectAsState(initial = emptyList())
    var showAnalysisSheet by remember { mutableStateOf(false) }
    var selectedDateMillis by remember(cycleSettingsVersion) { mutableLongStateOf(startOfDay(System.currentTimeMillis())) }
    var periodActionFeedback by remember { mutableStateOf<String?>(null) }
    var pendingOverwrite by remember { mutableStateOf<PeriodOverwriteRequest?>(null) }

    val monthStateCache = remember(cycleSettingsVersion, records, allIndicators, selectedDateMillis) {
        mutableMapOf<Int, CycleMonthState>()
    }
    val monthStateProvider: (Int) -> CycleMonthState = remember(
        monthStateCache,
        cycleManager,
        records,
        allIndicators,
        selectedDateMillis
    ) {
        { offset ->
            monthStateCache.getOrPut(offset) {
                buildCycleMonthState(
                    cycleManager = cycleManager,
                    monthOffset = offset,
                    records = records,
                    indicators = allIndicators,
                    selectedDateMillis = selectedDateMillis
                )
            }
        }
    }
    val cycleInsight = remember(cycleSettingsVersion, allIndicators, records) {
        buildCycleInsight(cycleManager, allIndicators)
    }
    val selectedDateKey = remember(selectedDateMillis) { dayKey(selectedDateMillis) }
    val selectedIndicators = remember(allIndicators, selectedDateKey) {
        allIndicators.filter { it.dateKey == selectedDateKey }
    }
    val selectedLoveRecords = remember(records, selectedDateKey) {
        records.filter { it.type == "同房" && dayKey(it.timeMillis) == selectedDateKey }
    }
    val statusUi = remember(selectedDateMillis, allIndicators, cycleSettingsVersion) {
        buildPeriodStatusUi(
            cycleManager = cycleManager,
            indicators = allIndicators,
            selectedDateMillis = selectedDateMillis
        )
    }

    suspend fun persistPeriodStatus(normalized: Long, saveStart: Boolean) {
        onSaveIndicator(
            DailyIndicatorEntity(
                dateKey = dayKey(normalized),
                metricKey = PERIOD_STATUS_KEY,
                optionValue = if (saveStart) PERIOD_STARTED else PERIOD_ENDED,
                displayLabel = if (saveStart) "月经来了" else "月经走了",
                updatedAt = System.currentTimeMillis()
            )
        )
        periodActionFeedback = if (saveStart) "已记录：月经来了" else "已记录：月经走了"
        if (saveStart) {
            cycleManager.saveAll(
                cycleLengthDays = cycleManager.cycleLengthDays(),
                periodLengthDays = cycleManager.periodLengthDays(),
                lastPeriodStartMillis = normalized
            )
        }
        onCycleChanged()
    }

    GlassBackground {
        LazyColumn(
            contentPadding = PaddingValues(bottom = 112.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item {
                CalendarPanel(
                    monthStateProvider = monthStateProvider,
                    selectedDateMillis = selectedDateMillis,
                    onBackToToday = {
                        selectedDateMillis = startOfDay(System.currentTimeMillis())
                        periodActionFeedback = null
                    },
                    onSelectDate = {
                        selectedDateMillis = it
                        periodActionFeedback = null
                    }
                )
            }
            item {
                InsightActionCard(onClick = { showAnalysisSheet = true })
            }
            item {
                PeriodStatusSection(
                    selectedDateMillis = selectedDateMillis,
                    statusUi = statusUi,
                    feedback = periodActionFeedback,
                    onConfirm = {
                        val normalized = startOfDay(selectedDateMillis)
                        val saveStart = statusUi.cardTitle == "月经来了"
                        val existing = allIndicators.firstOrNull {
                            it.dateKey == dayKey(normalized) && it.metricKey == PERIOD_STATUS_KEY
                        }
                        if (existing != null) {
                            pendingOverwrite = PeriodOverwriteRequest(
                                dateMillis = normalized,
                                oldOption = existing.optionValue,
                                newOption = if (saveStart) PERIOD_STARTED else PERIOD_ENDED
                            )
                        } else {
                            persistPeriodStatus(normalized, saveStart)
                        }
                    },
                    onUndo = {
                        val normalized = startOfDay(selectedDateMillis)
                        if (!statusUi.canUndo) {
                            periodActionFeedback = "当前日期没有可撤销的月经状态"
                        } else {
                            onDeletePeriodStatus(normalized)
                            if (statusUi.recordedOption == PERIOD_STARTED && dayKey(cycleManager.lastPeriodStartMillis()) == dayKey(normalized)) {
                                val previousStart = allIndicators
                                    .asSequence()
                                    .filter { it.metricKey == PERIOD_STATUS_KEY && it.optionValue == PERIOD_STARTED }
                                    .map { parseDayKey(it.dateKey) }
                                    .filter { it in 1L until normalized }
                                    .maxOrNull() ?: 0L
                                cycleManager.setLastPeriodStartMillis(previousStart)
                            }
                            periodActionFeedback = "已撤销本日月经状态"
                            onCycleChanged()
                        }
                    }
                )
            }
            item {
                RecordEntryList(
                    onAddRecord = { entry ->
                        onSelectDateForEntry(selectedDateMillis)
                        onAddRecord(entry)
                    },
                    indicators = selectedIndicators,
                    loveRecords = selectedLoveRecords
                )
            }
            item { Spacer(modifier = Modifier.height(10.dp)) }
        }
    }

    if (pendingOverwrite != null) {
        val request = pendingOverwrite!!
        val oldLabel = if (request.oldOption == PERIOD_STARTED) "月经来了" else "月经走了"
        val newLabel = if (request.newOption == PERIOD_STARTED) "月经来了" else "月经走了"
        val message = if (oldLabel == newLabel) {
            "${formatMonthDay(request.dateMillis)} 已设置为$newLabel，是否重新覆盖？"
        } else {
            "${formatMonthDay(request.dateMillis)} 已设置为$oldLabel，是否覆盖为$newLabel？"
        }
        val scope = androidx.compose.runtime.rememberCoroutineScope()
        AlertDialog(
            onDismissRequest = { pendingOverwrite = null },
            title = { Text("覆盖已有记录") },
            text = { Text(message) },
            confirmButton = {
                TextButton(
                    onClick = {
                        scope.launch {
                            persistPeriodStatus(request.dateMillis, request.newOption == PERIOD_STARTED)
                            pendingOverwrite = null
                        }
                    }
                ) {
                    Text("覆盖")
                }
            },
            dismissButton = {
                TextButton(onClick = { pendingOverwrite = null }) {
                    Text("取消")
                }
            }
        )
    }

    if (showAnalysisSheet) {
        val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        ModalBottomSheet(
            onDismissRequest = { showAnalysisSheet = false },
            sheetState = sheetState,
            containerColor = MaterialTheme.colorScheme.surface
        ) {
            CycleInsightSheet(
                insight = cycleInsight,
                onClose = { showAnalysisSheet = false }
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun CalendarPanel(
    monthStateProvider: (Int) -> CycleMonthState,
    selectedDateMillis: Long,
    onBackToToday: () -> Unit,
    onSelectDate: (Long) -> Unit
) {
    val scope = rememberCoroutineScope()
    val pagerState = rememberPagerState(
        initialPage = CALENDAR_PAGER_START_PAGE,
        pageCount = { CALENDAR_PAGER_TOTAL_PAGES }
    )
    val currentOffset = pagerState.currentPage - CALENDAR_PAGER_START_PAGE
    val headerState = remember(currentOffset, monthStateProvider) {
        monthStateProvider(currentOffset)
    }
    val pagerBodyHeight by animateDpAsState(
        targetValue = calendarBodyHeightByRows(headerState.weekRows),
        animationSpec = tween(durationMillis = 140),
        label = "calendar-body-height"
    )

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .glassCard()
            .padding(horizontal = 18.dp, vertical = 18.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Top
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    text = "JOURNAL",
                    style = MaterialTheme.typography.labelLarge,
                    color = ZhiQiTokens.PrimaryStrong,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "花园日历",
                    style = MaterialTheme.typography.headlineSmall,
                    color = ZhiQiTokens.TextPrimary,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "左右滑动切换月份，像翻阅情绪和身体的小档案。",
                    style = MaterialTheme.typography.bodySmall,
                    color = ZhiQiTokens.TextSecondary
                )
            }
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .background(Color.White.copy(alpha = 0.84f), RoundedCornerShape(22.dp))
                    .border(1.dp, Color.White.copy(alpha = 0.8f), RoundedCornerShape(22.dp)),
                contentAlignment = Alignment.Center
            ) {
                Image(
                    painter = painterResource(id = R.drawable.ill_brand_blossom),
                    contentDescription = "品牌插画",
                    modifier = Modifier.size(36.dp)
                )
            }
        }
        RecordCalendarHeader(
            title = headerState.title,
            selectedDateMillis = selectedDateMillis,
            onBackToToday = {
                onBackToToday()
                scope.launch {
                    pagerState.scrollToPage(CALENDAR_PAGER_START_PAGE)
                }
            }
        )

        HorizontalPager(
            state = pagerState,
            verticalAlignment = Alignment.Top,
            modifier = Modifier
                .fillMaxWidth()
                .height(pagerBodyHeight)
        ) { page ->
            val monthOffset = page - CALENDAR_PAGER_START_PAGE
            val monthState = remember(monthOffset, monthStateProvider) {
                monthStateProvider(monthOffset)
            }
            CalendarMonthBody(
                state = monthState,
                onSelectDate = onSelectDate
            )
        }
    }
}

@Composable
private fun CalendarMonthBody(
    state: CycleMonthState,
    onSelectDate: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(10.dp)) {
        CalendarWeekHeader()
        CalendarGrid(monthState = state, onSelectDate = onSelectDate)
        CalendarLegend()
    }
}

@Composable
private fun RecordCalendarHeader(
    title: String,
    selectedDateMillis: Long,
    onBackToToday: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp),
        verticalArrangement = Arrangement.spacedBy(5.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = "记录中心",
                    style = MaterialTheme.typography.headlineSmall,
                    color = ZhiQiTokens.TextPrimary,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyMedium,
                    color = ZhiQiTokens.TextSecondary
                )
            }
            Box(
                modifier = Modifier
                    .background(ZhiQiTokens.PrimarySoft, RoundedCornerShape(16.dp))
                    .border(1.dp, ZhiQiTokens.Border, RoundedCornerShape(16.dp))
                    .noRippleClickable(onBackToToday)
                    .padding(horizontal = 12.dp, vertical = 8.dp)
            ) {
                Text(
                    text = "回到今天",
                    style = MaterialTheme.typography.labelMedium,
                    color = ZhiQiTokens.Primary
                )
            }
        }
        Text(
            text = "当前日期：${formatMonthDay(selectedDateMillis)} · 左右滑动切换月份",
            style = MaterialTheme.typography.bodySmall,
            color = ZhiQiTokens.TextMuted
        )
    }
}

@Composable
private fun CalendarWeekHeader() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 2.dp)
    ) {
        listOf("日", "一", "二", "三", "四", "五", "六").forEach { label ->
            Text(
                text = label,
                modifier = Modifier.weight(1f),
                textAlign = TextAlign.Center,
                color = ZhiQiTokens.TextSecondary,
                style = MaterialTheme.typography.titleMedium
            )
        }
    }
}

@Composable
private fun CalendarGrid(
    monthState: CycleMonthState,
    onSelectDate: (Long) -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth()
    ) {
        monthState.days.chunked(7).forEach { week ->
            Row(modifier = Modifier.fillMaxWidth()) {
                week.forEach { day ->
                    CalendarDayCell(
                        day = day,
                        modifier = Modifier.weight(1f),
                        onSelect = onSelectDate
                    )
                }
            }
        }
    }
}

@Composable
private fun CalendarDayCell(
    day: CycleCalendarDay,
    modifier: Modifier = Modifier,
    onSelect: (Long) -> Unit
) {
    val state = day.phaseState
    val isFuture = !day.isBlank && day.dateMillis > startOfDay(System.currentTimeMillis())
    val isSelected = day.isSelected && !day.isBlank
    val isTodayOnly = day.isToday && !isSelected
    val background = when {
        day.isBlank -> Color.Transparent
        isSelected && day.isToday -> Color.Transparent
        isSelected -> CALENDAR_BG_SELECTED
        state == PhaseState.ACTUAL_PERIOD -> CALENDAR_BG_ACTUAL_PERIOD
        state == PhaseState.PREDICTED_PERIOD -> CALENDAR_BG_PREDICTED_PERIOD
        state == PhaseState.OVULATION_DAY -> CALENDAR_BG_OVULATION
        else -> Color.Transparent
    }
    val numberColor = when {
        day.isBlank -> Color.Transparent
        isSelected -> CALENDAR_TEXT_SELECTED
        state == PhaseState.ACTUAL_PERIOD -> CALENDAR_TEXT_PERIOD
        state == PhaseState.PREDICTED_PERIOD -> CALENDAR_TEXT_PREDICTED_PERIOD
        state == PhaseState.FERTILE || state == PhaseState.OVULATION_DAY -> CALENDAR_TEXT_FERTILE
        else -> CALENDAR_TEXT_DEFAULT
    }.let { if (isFuture) it.copy(alpha = 0.66f) else it }
    val emphasizeNumber = day.isToday || isSelected ||
        state == PhaseState.ACTUAL_PERIOD ||
        state == PhaseState.PREDICTED_PERIOD ||
        state == PhaseState.OVULATION_DAY

    Box(
        modifier = modifier
            .height(52.dp)
            .padding(horizontal = 1.dp, vertical = 1.dp)
            .background(background, RoundedCornerShape(12.dp))
            .border(
                width = when {
                    isSelected -> 1.6.dp
                    isTodayOnly -> 1.dp
                    else -> 0.dp
                },
                color = when {
                    isSelected -> PHASE_COLOR_PERIOD
                    isTodayOnly -> ZhiQiTokens.Primary
                    else -> Color.Transparent
                },
                shape = RoundedCornerShape(12.dp)
            )
            .then(
                if (!day.isBlank && !isFuture) Modifier.noRippleClickable { onSelect(day.dateMillis) } else Modifier
            ),
        contentAlignment = Alignment.Center
    ) {
        if (!day.isBlank) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                Spacer(modifier = Modifier.height(3.dp))
                Box(
                    modifier = Modifier
                        .width(20.dp)
                        .height(16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = day.dayNumber.toString(),
                        color = numberColor,
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = if (emphasizeNumber) FontWeight.SemiBold else FontWeight.Medium,
                        textAlign = TextAlign.Center,
                        maxLines = 1
                    )
                }
                Box(
                    modifier = Modifier.size(12.dp),
                    contentAlignment = Alignment.Center
                ) {
                    when {
                        state == PhaseState.OVULATION_DAY -> {
                            Text("✿", color = PHASE_COLOR_OVULATION, style = MaterialTheme.typography.labelSmall)
                        }
                        day.periodMarker == PeriodMarker.START -> {
                            PeriodMarkerIcon(
                                icon = Icons.Filled.PlayArrow,
                                tint = PHASE_COLOR_PERIOD,
                                background = ZhiQiTokens.PrimarySoft
                            )
                        }
                        day.periodMarker == PeriodMarker.END -> {
                            PeriodMarkerIcon(
                                icon = Icons.Filled.Pause,
                                tint = ZhiQiTokens.PrimaryStrong,
                                background = ZhiQiTokens.SurfaceSoft
                            )
                        }
                        day.hasRecord -> {
                            RecordCheckMarkerIcon()
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun RecordCheckMarkerIcon() {
    Canvas(modifier = Modifier.size(11.dp)) {
        val stroke = size.minDimension * 0.24f
        val tint = PHASE_COLOR_FERTILE
        drawLine(
            color = tint,
            start = Offset(size.width * 0.20f, size.height * 0.55f),
            end = Offset(size.width * 0.42f, size.height * 0.78f),
            strokeWidth = stroke,
            cap = StrokeCap.Round
        )
        drawLine(
            color = tint,
            start = Offset(size.width * 0.42f, size.height * 0.78f),
            end = Offset(size.width * 0.82f, size.height * 0.26f),
            strokeWidth = stroke,
            cap = StrokeCap.Round
        )
    }
}

@Composable
private fun PeriodMarkerIcon(
    icon: ImageVector,
    tint: Color,
    background: Color
) {
    Box(
        modifier = Modifier
            .size(12.dp)
            .background(background, CircleShape),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = tint,
            modifier = Modifier.size(9.dp)
        )
    }
}

@Composable
private fun CalendarLegend() {
    val items = listOf(
        LegendItem("月经期", PHASE_COLOR_PERIOD),
        LegendItem("预测经期", PHASE_COLOR_PREDICTED_PERIOD),
        LegendItem("排卵期", PHASE_COLOR_FERTILE),
        LegendItem("排卵日", PHASE_COLOR_OVULATION)
    )
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 2.dp, bottom = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        items.forEach { item ->
            Row(
                modifier = Modifier.weight(1f),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(10.dp)
                        .background(item.color, RoundedCornerShape(3.dp))
                )
                Text(
                    text = " ${item.label}",
                    color = ZhiQiTokens.TextSecondary,
                    style = MaterialTheme.typography.labelMedium
                )
            }
        }
    }
}

@Composable
private fun InsightActionCard(onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .glassCard()
            .background(
                Color.White.copy(alpha = 0.14f),
                RoundedCornerShape(34.dp)
            )
            .padding(horizontal = 18.dp, vertical = 18.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .background(Color.White.copy(alpha = 0.82f), RoundedCornerShape(18.dp))
                    .border(1.dp, Color.White.copy(alpha = 0.76f), RoundedCornerShape(18.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Filled.AutoGraph, contentDescription = "规律解读", tint = PHASE_COLOR_FERTILE)
            }
            Column {
                Text("周期趋势解读", color = ZhiQiTokens.TextPrimary, style = MaterialTheme.typography.titleMedium)
                Text("看最近周期有没有波动，再决定要不要补记。", color = ZhiQiTokens.TextSecondary, style = MaterialTheme.typography.bodySmall)
            }
        }
        Box(
            modifier = Modifier
                .background(Color.White.copy(alpha = 0.86f), RoundedCornerShape(20.dp))
                .border(1.dp, Color.White.copy(alpha = 0.82f), RoundedCornerShape(20.dp))
                .noRippleClickable(onClick)
                .padding(horizontal = 16.dp, vertical = 10.dp)
        ) {
            Text("去查看", color = PHASE_COLOR_FERTILE, style = MaterialTheme.typography.titleSmall)
        }
    }
}

@Composable
private fun PeriodStatusSection(
    selectedDateMillis: Long,
    statusUi: PeriodStatusUi,
    feedback: String?,
    onConfirm: suspend () -> Unit,
    onUndo: suspend () -> Unit
) {
    val scope = androidx.compose.runtime.rememberCoroutineScope()
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .glassCard()
            .padding(horizontal = 18.dp, vertical = 18.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Text(
            text = "身体在说什么？",
            style = MaterialTheme.typography.headlineSmall,
            color = ZhiQiTokens.TextPrimary,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = "${formatMonthDay(selectedDateMillis)} 状态",
            style = MaterialTheme.typography.titleMedium,
            color = ZhiQiTokens.TextPrimary
        )
        Text(
            text = "当前判断：${statusUi.phaseLabel}",
            style = MaterialTheme.typography.bodySmall,
            color = ZhiQiTokens.TextSecondary
        )
        if (!statusUi.recordedLabel.isNullOrBlank()) {
            Text(
                text = statusUi.recordedLabel,
                style = MaterialTheme.typography.bodySmall,
                color = ZhiQiTokens.TextSecondary
            )
        }
        if (!feedback.isNullOrBlank()) {
            Text(
                text = feedback,
                style = MaterialTheme.typography.bodySmall,
                color = ZhiQiTokens.Primary
            )
        }
        PeriodToggleCard(
            title = statusUi.cardTitle,
            active = statusUi.active,
            canUndo = statusUi.canUndo,
            onYes = { scope.launch { onConfirm() } },
            onUndo = { scope.launch { onUndo() } }
        )
    }
}

@Composable
private fun PeriodToggleCard(
    title: String,
    active: Boolean,
    canUndo: Boolean,
    onYes: () -> Unit,
    onUndo: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(ZhiQiTokens.SurfaceSoft, RoundedCornerShape(18.dp))
            .border(1.dp, ZhiQiTokens.Border, RoundedCornerShape(18.dp))
            .padding(horizontal = 14.dp, vertical = 14.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(title, style = MaterialTheme.typography.titleMedium, color = ZhiQiTokens.TextPrimary)
            Text("先记录月经状态，再补充当天指标。", style = MaterialTheme.typography.bodySmall, color = ZhiQiTokens.TextSecondary)
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            ToggleButton(text = if (active) "已记录" else "确定", active = !active, onClick = onYes)
            ToggleButton(text = "撤销", active = canUndo, onClick = onUndo)
        }
    }
}

@Composable
private fun ToggleButton(text: String, active: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .background(
                if (active) ZhiQiTokens.Primary else ZhiQiTokens.Surface,
                RoundedCornerShape(14.dp)
            )
            .border(1.dp, if (active) ZhiQiTokens.Primary else ZhiQiTokens.Border, RoundedCornerShape(14.dp))
            .noRippleClickable(onClick)
            .padding(horizontal = 18.dp, vertical = 10.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(text, color = if (active) Color.White else ZhiQiTokens.TextMuted, style = MaterialTheme.typography.titleSmall)
    }
}

@Composable
private fun RecordEntryList(
    onAddRecord: (String?) -> Unit,
    indicators: List<DailyIndicatorEntity>,
    loveRecords: List<RecordEntity>
) {
    val items = listOf(
        RecordEntryItem(title = "流量", metricKey = "流量", glyph = RecordEntryGlyph.FLOW, accentColor = ZhiQiTokens.Primary),
        RecordEntryItem(title = "疼痛", metricKey = "症状", glyph = RecordEntryGlyph.PAIN, accentColor = ZhiQiTokens.PrimaryStrong),
        RecordEntryItem(title = "情绪", metricKey = "心情", glyph = RecordEntryGlyph.MOOD, accentColor = ZhiQiTokens.Primary),
        RecordEntryItem(title = "睡眠", metricKey = "好习惯", glyph = RecordEntryGlyph.SLEEP, accentColor = ZhiQiTokens.PrimaryStrong),
        RecordEntryItem(title = "白带", metricKey = "白带", glyph = RecordEntryGlyph.DISCHARGE, accentColor = ZhiQiTokens.Primary),
        RecordEntryItem(title = "药物", metricKey = "计划", glyph = RecordEntryGlyph.MEDICINE, accentColor = ZhiQiTokens.PrimaryStrong),
        RecordEntryItem(title = "爱爱", metricKey = "爱爱", glyph = RecordEntryGlyph.LOVE, accentColor = ZhiQiTokens.Primary)
    )
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .glassCard()
            .padding(horizontal = 18.dp, vertical = 18.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Text(
            text = "一键记录",
            style = MaterialTheme.typography.headlineSmall,
            color = ZhiQiTokens.TextPrimary
        )
        Text(
            text = "把高频项做成卡片入口，像参考稿那样，保持轻触即可记录。",
            style = MaterialTheme.typography.bodySmall,
            color = ZhiQiTokens.TextSecondary
        )
        items.chunked(2).forEach { rowItems ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 2.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                rowItems.forEach { item ->
                    RecordEntryCard(
                        item = item,
                        indicators = indicators,
                        loveRecords = loveRecords,
                        modifier = Modifier.weight(1f),
                        onAddRecord = onAddRecord
                    )
                }
                if (rowItems.size == 1) {
                    Spacer(modifier = Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
private fun RecordEntryCard(
    item: RecordEntryItem,
    indicators: List<DailyIndicatorEntity>,
    loveRecords: List<RecordEntity>,
    modifier: Modifier = Modifier,
    onAddRecord: (String?) -> Unit
) {
    val saved = indicators.firstOrNull { it.metricKey == item.metricKey }
    val summary = when {
        item.metricKey == "爱爱" && loveRecords.isNotEmpty() -> {
            if (loveRecords.size > 1) "已记录 ${loveRecords.size} 次" else formatLoveProtectionLabel(loveRecords.first())
        }
        saved != null -> saved.displayLabel
        else -> "点击记录"
    }

    Column(
        modifier = modifier
            .background(ZhiQiTokens.SurfaceSoft, RoundedCornerShape(22.dp))
            .border(1.dp, ZhiQiTokens.Border, RoundedCornerShape(22.dp))
            .noRippleClickable { onAddRecord(item.metricKey) }
            .padding(horizontal = 14.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .background(item.accentColor.copy(alpha = 0.14f), CircleShape)
                    .border(1.dp, item.accentColor.copy(alpha = 0.22f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                RecordEntryGlyphIcon(
                    glyph = item.glyph,
                    tint = item.accentColor,
                    modifier = Modifier.size(22.dp)
                )
            }
            Box(
                modifier = Modifier
                    .size(30.dp)
                    .background(ZhiQiTokens.Surface, CircleShape)
                    .border(1.dp, ZhiQiTokens.BorderStrong, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Filled.Add, contentDescription = item.title, tint = ZhiQiTokens.Primary, modifier = Modifier.size(16.dp))
            }
        }
        Text(item.title, color = ZhiQiTokens.TextPrimary, style = MaterialTheme.typography.titleMedium)
        Text(
            text = summary,
            color = if (saved != null || (item.metricKey == "爱爱" && loveRecords.isNotEmpty())) ZhiQiTokens.TextSecondary else ZhiQiTokens.TextMuted,
            style = MaterialTheme.typography.bodySmall,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )
    }
}

private fun formatLoveProtectionLabel(record: RecordEntity): String {
    val protections = record.protections
        .split("|")
        .map { it.trim() }
        .filter { it.isNotBlank() }
    if (protections.isNotEmpty()) return protections.joinToString(" / ")
    val other = record.otherProtection?.trim().orEmpty()
    return other.ifBlank { "未记录措施" }
}

private data class RecordEntryItem(
    val title: String,
    val metricKey: String,
    val glyph: RecordEntryGlyph,
    val accentColor: Color
)

private enum class RecordEntryGlyph {
    FLOW,
    PAIN,
    MOOD,
    SLEEP,
    DISCHARGE,
    MEDICINE,
    LOVE
}

@Composable
private fun RecordEntryGlyphIcon(
    glyph: RecordEntryGlyph,
    tint: Color,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val stroke = Stroke(
            width = size.minDimension * 0.1f,
            cap = StrokeCap.Round,
            join = StrokeJoin.Round
        )
        when (glyph) {
            RecordEntryGlyph.FLOW -> {
                val drop = Path().apply {
                    moveTo(w * 0.50f, h * 0.08f)
                    cubicTo(w * 0.68f, h * 0.28f, w * 0.82f, h * 0.46f, w * 0.50f, h * 0.88f)
                    cubicTo(w * 0.18f, h * 0.46f, w * 0.32f, h * 0.28f, w * 0.50f, h * 0.08f)
                    close()
                }
                drawPath(drop, tint, style = stroke)
            }

            RecordEntryGlyph.PAIN -> {
                drawCircle(color = tint, radius = w * 0.36f, center = Offset(w * 0.5f, h * 0.5f), style = stroke)
                drawLine(
                    color = tint,
                    start = Offset(w * 0.35f, h * 0.5f),
                    end = Offset(w * 0.65f, h * 0.5f),
                    strokeWidth = stroke.width,
                    cap = StrokeCap.Round
                )
                drawLine(
                    color = tint,
                    start = Offset(w * 0.5f, h * 0.35f),
                    end = Offset(w * 0.5f, h * 0.65f),
                    strokeWidth = stroke.width,
                    cap = StrokeCap.Round
                )
            }

            RecordEntryGlyph.MOOD -> {
                drawCircle(color = tint, radius = w * 0.36f, center = Offset(w * 0.5f, h * 0.5f), style = stroke)
                drawCircle(color = tint, radius = w * 0.04f, center = Offset(w * 0.40f, h * 0.42f))
                drawCircle(color = tint, radius = w * 0.04f, center = Offset(w * 0.60f, h * 0.42f))
                drawArc(
                    color = tint,
                    startAngle = 20f,
                    sweepAngle = 140f,
                    useCenter = false,
                    topLeft = Offset(w * 0.32f, h * 0.38f),
                    size = Size(w * 0.36f, h * 0.36f),
                    style = Stroke(width = stroke.width * 0.9f, cap = StrokeCap.Round)
                )
            }

            RecordEntryGlyph.SLEEP -> {
                drawRoundRect(
                    color = tint,
                    topLeft = Offset(w * 0.14f, h * 0.48f),
                    size = Size(w * 0.72f, h * 0.26f),
                    cornerRadius = CornerRadius(w * 0.06f, w * 0.06f),
                    style = stroke
                )
                drawRoundRect(
                    color = tint,
                    topLeft = Offset(w * 0.18f, h * 0.34f),
                    size = Size(w * 0.22f, h * 0.15f),
                    cornerRadius = CornerRadius(w * 0.04f, w * 0.04f),
                    style = stroke
                )
                drawLine(
                    color = tint,
                    start = Offset(w * 0.14f, h * 0.74f),
                    end = Offset(w * 0.14f, h * 0.88f),
                    strokeWidth = stroke.width * 0.85f,
                    cap = StrokeCap.Round
                )
                drawLine(
                    color = tint,
                    start = Offset(w * 0.86f, h * 0.74f),
                    end = Offset(w * 0.86f, h * 0.88f),
                    strokeWidth = stroke.width * 0.85f,
                    cap = StrokeCap.Round
                )
            }

            RecordEntryGlyph.DISCHARGE -> {
                drawArc(
                    color = tint,
                    startAngle = 200f,
                    sweepAngle = 140f,
                    useCenter = false,
                    topLeft = Offset(w * 0.18f, h * 0.28f),
                    size = Size(w * 0.64f, h * 0.44f),
                    style = stroke
                )
                drawArc(
                    color = tint,
                    startAngle = 20f,
                    sweepAngle = 140f,
                    useCenter = false,
                    topLeft = Offset(w * 0.18f, h * 0.42f),
                    size = Size(w * 0.64f, h * 0.36f),
                    style = Stroke(width = stroke.width * 0.85f, cap = StrokeCap.Round)
                )
            }

            RecordEntryGlyph.MEDICINE -> {
                drawRoundRect(
                    color = tint,
                    topLeft = Offset(w * 0.18f, h * 0.34f),
                    size = Size(w * 0.64f, h * 0.32f),
                    cornerRadius = CornerRadius(w * 0.16f, w * 0.16f),
                    style = stroke
                )
                drawLine(
                    color = tint,
                    start = Offset(w * 0.50f, h * 0.34f),
                    end = Offset(w * 0.50f, h * 0.66f),
                    strokeWidth = stroke.width * 0.9f,
                    cap = StrokeCap.Round
                )
            }

            RecordEntryGlyph.LOVE -> {
                val heart = Path().apply {
                    moveTo(w * 0.50f, h * 0.78f)
                    cubicTo(w * 0.08f, h * 0.50f, w * 0.18f, h * 0.20f, w * 0.40f, h * 0.30f)
                    cubicTo(w * 0.47f, h * 0.34f, w * 0.53f, h * 0.34f, w * 0.60f, h * 0.30f)
                    cubicTo(w * 0.82f, h * 0.20f, w * 0.92f, h * 0.50f, w * 0.50f, h * 0.78f)
                    close()
                }
                drawPath(heart, tint, style = stroke)
            }
        }
    }
}
private data class LegendItem(val label: String, val color: Color)
private data class CycleInsight(
    val summaryTitle: String,
    val summaryText: String,
    val cycleLengthText: String,
    val nextPeriodText: String,
    val fertileText: String,
    val guidance: List<String>,
    val segments: List<CycleSegment>
)
private data class CycleSegment(val label: String, val color: Color, val days: Int)
private data class CycleMonthState(
    val title: String,
    val days: List<CycleCalendarDay>,
    val weekRows: Int
)
private data class CycleCalendarDay(
    val dateMillis: Long,
    val dayNumber: Int,
    val isToday: Boolean,
    val isSelected: Boolean,
    val isBlank: Boolean,
    val hasRecord: Boolean,
    val phaseState: PhaseState,
    val periodMarker: PeriodMarker
)
private data class PeriodStatusUi(
    val phaseLabel: String,
    val cardTitle: String,
    val active: Boolean,
    val canUndo: Boolean,
    val recordedOption: String?,
    val recordedLabel: String?
)
private data class PeriodOverwriteRequest(
    val dateMillis: Long,
    val oldOption: String,
    val newOption: String
)

private enum class PhaseState {
    NONE,
    ACTUAL_PERIOD,
    PREDICTED_PERIOD,
    FERTILE,
    OVULATION_DAY
}

private enum class PeriodMarker {
    NONE,
    START,
    END
}

@Composable
private fun CycleInsightSheet(
    insight: CycleInsight,
    onClose: () -> Unit
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("月经规律解读", style = MaterialTheme.typography.titleLarge, color = ZhiQiTokens.TextPrimary)
                Text("关闭", color = ZhiQiTokens.Primary, modifier = Modifier.noRippleClickable(onClose))
            }
        }
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .glassCard()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(insight.summaryTitle, style = MaterialTheme.typography.titleLarge, color = ZhiQiTokens.Primary)
                Text(insight.summaryText, style = MaterialTheme.typography.bodyMedium, color = ZhiQiTokens.TextSecondary)
            }
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                InsightMetricCard("平均周期", insight.cycleLengthText, Modifier.weight(1f))
                InsightMetricCard("预计下次", insight.nextPeriodText, Modifier.weight(1f))
                InsightMetricCard("易孕窗口", insight.fertileText, Modifier.weight(1f))
            }
        }
        item { CycleSegmentChart(insight.segments) }
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .glassCard()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text("温和建议", style = MaterialTheme.typography.titleMedium, color = ZhiQiTokens.TextPrimary)
                insight.guidance.forEach { tip ->
                    Text("• $tip", style = MaterialTheme.typography.bodyMedium, color = ZhiQiTokens.TextSecondary)
                }
            }
        }
        item { Spacer(modifier = Modifier.height(8.dp)) }
    }
}

@Composable
private fun InsightMetricCard(title: String, value: String, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .glassCard()
            .padding(horizontal = 12.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Text(title, style = MaterialTheme.typography.bodySmall, color = ZhiQiTokens.TextMuted)
        Text(value, style = MaterialTheme.typography.titleMedium, color = ZhiQiTokens.TextPrimary, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun CycleSegmentChart(segments: List<CycleSegment>) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .glassCard()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text("周期结构示意", style = MaterialTheme.typography.titleMedium, color = ZhiQiTokens.TextPrimary)
        BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
            val totalDays = segments.sumOf { it.days }.coerceAtLeast(1)
            Row(modifier = Modifier.fillMaxWidth()) {
                segments.forEach { segment ->
                    val fraction = segment.days.toFloat() / totalDays.toFloat()
                    Box(
                        modifier = Modifier
                            .weight(fraction)
                            .height(16.dp)
                            .background(segment.color, RoundedCornerShape(10.dp))
                    )
                    if (segment != segments.last()) {
                        Spacer(modifier = Modifier.width(4.dp))
                    }
                }
            }
        }
        segments.forEach { segment ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .background(segment.color, CircleShape)
                    )
                    Text(" ${segment.label}", color = ZhiQiTokens.TextSecondary, style = MaterialTheme.typography.bodyMedium)
                }
                Text("${segment.days} 天", color = ZhiQiTokens.TextPrimary, style = MaterialTheme.typography.bodyMedium)
            }
        }
    }
}

private fun buildCycleMonthState(
    cycleManager: CycleSettingsManager,
    monthOffset: Int,
    records: List<RecordEntity>,
    indicators: List<DailyIndicatorEntity>,
    selectedDateMillis: Long
): CycleMonthState {
    val monthCalendar = Calendar.getInstance().apply {
        add(Calendar.MONTH, monthOffset)
        set(Calendar.DAY_OF_MONTH, 1)
    }
    val title = SimpleDateFormat("yyyy年M月", Locale.getDefault()).format(Date(monthCalendar.timeInMillis))
    val cycleLength = cycleManager.cycleLengthDays()
    val periodLength = cycleManager.periodLengthDays()
    val actualRanges = buildActualPeriodRanges(cycleManager, indicators)
    val lastStart = resolveAnchorStart(cycleManager, actualRanges)
    val hasCycle = cycleManager.isConfigured() && lastStart > 0L
    val predictedRanges = buildPredictedPeriodRanges(
        anchorStartMillis = lastStart,
        cycleLength = cycleLength,
        periodLength = periodLength,
        todayStart = startOfDay(System.currentTimeMillis())
    )
    val periodStatusByDate = indicators
        .filter { it.metricKey == PERIOD_STATUS_KEY }
        .associateBy { it.dateKey }
    val dayRecordKeys = (records.map { dayKey(it.timeMillis) } + indicators.map { it.dateKey }).toSet()
    val todayKey = dayKey(System.currentTimeMillis())
    val selectedKey = dayKey(selectedDateMillis)

    val firstDayOfWeek = monthCalendar.get(Calendar.DAY_OF_WEEK) - 1
    val maxDay = monthCalendar.getActualMaximum(Calendar.DAY_OF_MONTH)
    val days = mutableListOf<CycleCalendarDay>()

    repeat(firstDayOfWeek) {
        days += CycleCalendarDay(0L, 0, false, false, true, false, PhaseState.NONE, PeriodMarker.NONE)
    }

    for (day in 1..maxDay) {
        monthCalendar.set(Calendar.DAY_OF_MONTH, day)
        val time = startOfDay(monthCalendar.timeInMillis)
        val key = dayKey(time)
        days += CycleCalendarDay(
            dateMillis = time,
            dayNumber = day,
            isToday = key == todayKey,
            isSelected = key == selectedKey,
            isBlank = false,
            hasRecord = dayRecordKeys.contains(key),
            phaseState = phaseForDate(
                dateMillis = time,
                cycleLength = cycleLength,
                periodLength = periodLength,
                lastStartMillis = lastStart,
                hasCycle = hasCycle,
                actualRanges = actualRanges,
                predictedRanges = predictedRanges
            ),
            periodMarker = when (periodStatusByDate[key]?.optionValue) {
                PERIOD_STARTED -> PeriodMarker.START
                PERIOD_ENDED -> PeriodMarker.END
                else -> PeriodMarker.NONE
            }
        )
    }

    while (days.size % 7 != 0) {
        days += CycleCalendarDay(0L, 0, false, false, true, false, PhaseState.NONE, PeriodMarker.NONE)
    }

    return CycleMonthState(
        title = title,
        days = days,
        weekRows = (days.size / 7).coerceAtLeast(4)
    )
}

private fun calendarBodyHeightByRows(weekRows: Int) =
    (weekRows.coerceIn(4, 6) * CALENDAR_ROW_HEIGHT_DP + CALENDAR_BODY_EXTRA_DP).dp

private fun buildPeriodStatusUi(
    cycleManager: CycleSettingsManager,
    indicators: List<DailyIndicatorEntity>,
    selectedDateMillis: Long
): PeriodStatusUi {
    val normalized = startOfDay(selectedDateMillis)
    val actualRanges = buildActualPeriodRanges(cycleManager, indicators)
    val dateIndicators = indicators.filter { it.dateKey == dayKey(normalized) && it.metricKey == PERIOD_STATUS_KEY }
    val hasStartOnDay = dateIndicators.any { it.optionValue == PERIOD_STARTED }
    val hasEndOnDay = dateIndicators.any { it.optionValue == PERIOD_ENDED }
    val recordedOption = when {
        hasStartOnDay -> PERIOD_STARTED
        hasEndOnDay -> PERIOD_ENDED
        else -> null
    }
    val recordedLabel = when (recordedOption) {
        PERIOD_STARTED -> "已记录：月经来了"
        PERIOD_ENDED -> "已记录：月经走了"
        else -> null
    }
    val latestStart = indicators
        .filter { it.metricKey == PERIOD_STATUS_KEY && it.optionValue == PERIOD_STARTED }
        .map { parseDayKey(it.dateKey) }
        .filter { it in 1L..normalized }
        .maxOrNull()
        ?: cycleManager.lastPeriodStartMillis().takeIf { it > 0L }?.let(::startOfDay)
    val latestEnd = indicators
        .filter { it.metricKey == PERIOD_STATUS_KEY && it.optionValue == PERIOD_ENDED }
        .map { parseDayKey(it.dateKey) }
        .filter { it in 1L..normalized }
        .maxOrNull()
    val dayMillis = 24L * 60L * 60L * 1000L
    val hasOpenPeriod =
        latestStart != null &&
            (latestEnd == null || latestEnd < latestStart) &&
            normalized in latestStart..(latestStart + (MAX_PERIOD_DAYS - 1) * dayMillis)
    val cardTitle = if (hasOpenPeriod) "月经走了" else "月经来了"
    val isCurrentActionSaved = when (cardTitle) {
        "月经走了" -> hasEndOnDay
        else -> hasStartOnDay
    }
    val anchorStart = resolveAnchorStart(cycleManager, actualRanges)
    val predictedRanges = buildPredictedPeriodRanges(
        anchorStartMillis = anchorStart,
        cycleLength = cycleManager.cycleLengthDays(),
        periodLength = cycleManager.periodLengthDays(),
        todayStart = startOfDay(System.currentTimeMillis())
    )

    return PeriodStatusUi(
        phaseLabel = cyclePhaseLabel(
            dateMillis = normalized,
            cycleLength = cycleManager.cycleLengthDays(),
            periodLength = cycleManager.periodLengthDays(),
            lastStartMillis = anchorStart,
            hasCycle = cycleManager.isConfigured() && anchorStart > 0L,
            actualRanges = actualRanges,
            predictedRanges = predictedRanges
        ),
        cardTitle = cardTitle,
        active = isCurrentActionSaved,
        canUndo = recordedOption != null,
        recordedOption = recordedOption,
        recordedLabel = recordedLabel
    )
}

private data class ActualPeriodRange(val startMillis: Long, val endMillis: Long)

private fun buildActualPeriodRanges(
    cycleManager: CycleSettingsManager,
    indicators: List<DailyIndicatorEntity>
): List<ActualPeriodRange> {
    val dayMillis = 24L * 60L * 60L * 1000L
    val periodLength = cycleManager.periodLengthDays().coerceIn(1, MAX_PERIOD_DAYS)
    val startEvents = indicators
        .filter { it.metricKey == PERIOD_STATUS_KEY && it.optionValue == PERIOD_STARTED }
        .map { parseDayKey(it.dateKey) }
        .toMutableList()
    val endEvents = indicators
        .filter { it.metricKey == PERIOD_STATUS_KEY && it.optionValue == PERIOD_ENDED }
        .map { parseDayKey(it.dateKey) }
        .sorted()
    val managerStart = startOfDay(cycleManager.lastPeriodStartMillis())
    if (cycleManager.isConfigured() && managerStart > 0L && managerStart !in startEvents) {
        startEvents += managerStart
    }
    val starts = startEvents.distinct().sorted()
    val ranges = mutableListOf<ActualPeriodRange>()

    starts.forEachIndexed { index, start ->
        val nextStart = starts.getOrNull(index + 1)
        val hardMaxEnd = start + (MAX_PERIOD_DAYS - 1L) * dayMillis
        val defaultEnd = start + (periodLength - 1L) * dayMillis
        val explicitEnd = endEvents.firstOrNull {
            it >= start &&
                it <= hardMaxEnd &&
                (nextStart == null || it < nextStart)
        }
        val cappedEnd = if (explicitEnd != null) {
            listOfNotNull(
                explicitEnd,
                hardMaxEnd,
                nextStart?.minus(dayMillis)
            ).minOrNull() ?: explicitEnd
        } else {
            listOfNotNull(
                defaultEnd,
                hardMaxEnd,
                nextStart?.minus(dayMillis)
            ).minOrNull() ?: defaultEnd
        }
        ranges += ActualPeriodRange(startMillis = start, endMillis = cappedEnd)
    }

    return ranges
}

private fun buildPredictedPeriodRanges(
    anchorStartMillis: Long,
    cycleLength: Int,
    periodLength: Int,
    todayStart: Long
): List<ActualPeriodRange> {
    if (anchorStartMillis <= 0L) return emptyList()
    val dayMillis = 24L * 60L * 60L * 1000L
    val safeCycleLength = cycleLength.coerceAtLeast(21)
    val safePeriodLength = periodLength.coerceIn(1, MAX_PERIOD_DAYS)
    var nextStart = anchorStartMillis + safeCycleLength * dayMillis

    // Keep stepping forward until we reach a range that is not fully behind today.
    while (nextStart + (safePeriodLength - 1L) * dayMillis < todayStart) {
        nextStart += safeCycleLength * dayMillis
    }

    return (0..18).map { index ->
        val start = nextStart + index * safeCycleLength * dayMillis
        ActualPeriodRange(
            startMillis = start,
            endMillis = start + (safePeriodLength - 1L) * dayMillis
        )
    }
}

private fun cyclePhaseLabel(
    dateMillis: Long,
    cycleLength: Int,
    periodLength: Int,
    lastStartMillis: Long,
    hasCycle: Boolean,
    actualRanges: List<ActualPeriodRange>,
    predictedRanges: List<ActualPeriodRange>
): String {
    return when (
        phaseForDate(
            dateMillis = dateMillis,
            cycleLength = cycleLength,
            periodLength = periodLength,
            lastStartMillis = lastStartMillis,
            hasCycle = hasCycle,
            actualRanges = actualRanges,
            predictedRanges = predictedRanges
        )
    ) {
        PhaseState.ACTUAL_PERIOD -> "经期"
        PhaseState.PREDICTED_PERIOD -> "预测经期"
        PhaseState.FERTILE -> "排卵期"
        PhaseState.OVULATION_DAY -> "排卵日"
        else -> "周期中"
    }
}

private fun phaseForDate(
    dateMillis: Long,
    cycleLength: Int,
    periodLength: Int,
    lastStartMillis: Long,
    hasCycle: Boolean,
    actualRanges: List<ActualPeriodRange>,
    predictedRanges: List<ActualPeriodRange>
): PhaseState {
    val normalized = startOfDay(dateMillis)
    if (actualRanges.any { normalized in it.startMillis..it.endMillis }) {
        return PhaseState.ACTUAL_PERIOD
    }
    if (!hasCycle || lastStartMillis <= 0L) return PhaseState.NONE

    if (predictedRanges.any { normalized in it.startMillis..it.endMillis }) {
        return PhaseState.PREDICTED_PERIOD
    }

    val dayMillis = 24L * 60L * 60L * 1000L
    val lastStart = startOfDay(lastStartMillis)
    val diffDays = ((normalized - lastStart) / dayMillis).toInt()
    if (diffDays < 0) return PhaseState.NONE

    val cycleDay = diffDays % cycleLength
    val ovulationOffset = (cycleLength - 14 - 1).coerceAtLeast(periodLength).coerceAtMost(cycleLength - 1)
    val fertileStart = (ovulationOffset - 5).coerceAtLeast(periodLength)
    val fertileEnd = (ovulationOffset + 1).coerceAtMost(cycleLength - 1)
    return when {
        cycleDay == ovulationOffset -> PhaseState.OVULATION_DAY
        cycleDay in fertileStart..fertileEnd -> PhaseState.FERTILE
        else -> PhaseState.NONE
    }
}

private fun buildCycleInsight(
    cycleManager: CycleSettingsManager,
    indicators: List<DailyIndicatorEntity>
): CycleInsight {
    val actualRanges = buildActualPeriodRanges(cycleManager, indicators)
    if (!cycleManager.isConfigured() || cycleManager.lastPeriodStartMillis() <= 0L) {
        return CycleInsight(
            summaryTitle = "尚未形成完整解读",
            summaryText = "设置最近一次生理期开始日和周期天数后，这里会生成经期、排卵期和下次月经的结构化解读。",
            cycleLengthText = "--",
            nextPeriodText = "--",
            fertileText = "--",
            guidance = listOf(
                "先完成生理周期设置，记录会更准确。",
                "若周期波动明显，建议连续记录 2 到 3 个周期后再判断。",
                "如果经期长期推迟或异常疼痛，建议及时咨询医生。"
            ),
            segments = listOf(
                CycleSegment("经期", PHASE_COLOR_PERIOD, 5),
                CycleSegment("卵泡期", PHASE_COLOR_PREDICTED_PERIOD, 9),
                CycleSegment("排卵期", PHASE_COLOR_FERTILE, 6),
                CycleSegment("黄体期", PHASE_COLOR_LUTEAL, 8)
            )
        )
    }

    val cycleLength = cycleManager.cycleLengthDays()
    val periodLength = cycleManager.periodLengthDays()
    val lastStart = resolveAnchorStart(cycleManager, actualRanges)
    val today = startOfDay(System.currentTimeMillis())
    val predictedRanges = buildPredictedPeriodRanges(
        anchorStartMillis = lastStart,
        cycleLength = cycleLength,
        periodLength = periodLength,
        todayStart = today
    )
    val dayMillis = 24L * 60L * 60L * 1000L
    val nextPeriod = lastStart + cycleLength * dayMillis
    val ovulationOffset = (cycleLength - 14 - 1).coerceAtLeast(periodLength).coerceAtMost(cycleLength - 1)
    val fertileStart = (ovulationOffset - 5).coerceAtLeast(periodLength)
    val fertileEnd = (ovulationOffset + 1).coerceAtMost(cycleLength - 1)
    val fertileLength = (fertileEnd - fertileStart + 1).coerceAtLeast(1)
    val follicularLength = (fertileStart - periodLength).coerceAtLeast(1)
    val lutealLength = (cycleLength - fertileEnd - 1).coerceAtLeast(1)
    val phase = cyclePhaseLabel(
        dateMillis = today,
        cycleLength = cycleLength,
        periodLength = periodLength,
        lastStartMillis = lastStart,
        hasCycle = true,
        actualRanges = actualRanges,
        predictedRanges = predictedRanges
    )
    val latestActualRange = actualRanges.maxByOrNull { it.startMillis }
    val recordedPeriodLength = latestActualRange?.let {
        ((it.endMillis - it.startMillis) / dayMillis).toInt() + 1
    }

    val summaryText = when {
        phase == "经期" -> "当前处于经期阶段，建议以休息、保暖和温和记录为主。若本次经期较平时明显提前、延后或持续时间变化较大，可继续观察下个周期。"
        phase == "排卵期" || phase == "排卵日" -> "当前已经进入易孕窗口，受孕概率相对更高。如果近期有行为记录，建议重点留意防护方式和身体变化。"
        today > nextPeriod -> "本周期已超过预计长度，建议先观察经期是否启动。若持续推迟且伴随胸胀、疲惫或其他变化，可考虑进一步排查。"
        else -> "从当前设置来看，周期长度处于常见区间，今天不属于经期高峰。继续保持稳定记录，有助于后续识别节律变化。"
    }

    val guidance = buildList {
        add("你当前设置为 $cycleLength 天周期、约 $periodLength 天经期，作为基础推算模板。")
        add("预计下次月经开始时间为 ${formatMonthDay(nextPeriod)}，建议提前 2 到 3 天留意身体感受。")
        if (recordedPeriodLength != null) {
            add("最近一次实际记录经期约 $recordedPeriodLength 天，可和默认经期长度一起观察是否稳定。")
        }
        if (phase == "排卵期" || phase == "排卵日") {
            add("当前位于易孕窗口，若暂无怀孕计划，建议重点关注避孕措施是否稳定。")
        } else {
            add("若后续出现连续 2 个周期以上明显提前或推迟，建议重新校正周期或咨询医生。")
        }
    }

    return CycleInsight(
        summaryTitle = "当前属于$phase",
        summaryText = summaryText,
        cycleLengthText = "$cycleLength 天",
        nextPeriodText = formatMonthDay(nextPeriod),
        fertileText = "${fertileLength} 天窗口",
        guidance = guidance,
        segments = listOf(
            CycleSegment("经期", PHASE_COLOR_PERIOD, periodLength),
            CycleSegment("卵泡期", PHASE_COLOR_PREDICTED_PERIOD, follicularLength),
            CycleSegment("排卵期", PHASE_COLOR_FERTILE, fertileLength),
            CycleSegment("黄体期", PHASE_COLOR_LUTEAL, lutealLength)
        )
    )
}

private fun resolveAnchorStart(
    cycleManager: CycleSettingsManager,
    actualRanges: List<ActualPeriodRange>
): Long {
    val latestRangeStart = actualRanges.maxByOrNull { it.startMillis }?.startMillis ?: 0L
    return maxOf(startOfDay(cycleManager.lastPeriodStartMillis()), latestRangeStart)
}

private fun startOfDay(timeMillis: Long): Long {
    if (timeMillis <= 0L) return 0L
    val calendar = Calendar.getInstance().apply {
        timeInMillis = timeMillis
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }
    return calendar.timeInMillis
}

private fun dayKey(timeMillis: Long): String {
    return SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date(timeMillis))
}

private fun parseDayKey(dateKey: String): Long {
    return SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).parse(dateKey)?.time?.let(::startOfDay) ?: 0L
}

private fun formatMonthDay(timeMillis: Long): String {
    return SimpleDateFormat("M月d日", Locale.getDefault()).format(Date(timeMillis))
}
