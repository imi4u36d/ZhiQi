package com.zhiqi.app.ui

import android.graphics.Paint
import android.graphics.Path
import android.graphics.PathMeasure
import android.graphics.RectF
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Hotel
import androidx.compose.material.icons.filled.InvertColors
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.SentimentSatisfied
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.zhiqi.app.ui.components.AppArrowAction
import com.zhiqi.app.ui.components.AppBadge
import com.zhiqi.app.ui.components.AppIconBadge
import com.zhiqi.app.ui.components.AppPanel
import com.zhiqi.app.ui.components.AppQuickActionTile
import com.zhiqi.app.ui.components.AppSurfaceCard
import com.zhiqi.app.data.DailyIndicatorEntity
import com.zhiqi.app.data.DailyIndicatorRepository
import com.zhiqi.app.data.RecordEntity
import com.zhiqi.app.data.RecordRepository
import com.zhiqi.app.ui.designsystem.AppSpacing
import com.zhiqi.app.ui.patterns.AppInfoRow
import com.zhiqi.app.security.PinManager
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import kotlin.math.abs
import kotlin.math.roundToInt

private const val PERIOD_STATUS_KEY = "月经状态"
private const val PERIOD_STARTED = "start"
private const val PERIOD_ENDED = "end"
private const val MAX_PERIOD_DAYS = 10
private const val RING_START_ANGLE = 145f
private const val RING_GAP_ANGLE = 0f
private const val TODAY_ANCHOR_ANGLE = 270f
private val HOME_RING_PERIOD_COLOR = Color(0xFFF2A7B8)
private val HOME_RING_FOLLICULAR_COLOR = Color(0xFFB8C9E2)
private val HOME_RING_FERTILE_COLOR = Color(0xFFFFC32A)
private val HOME_RING_LUTEAL_COLOR = Color(0xFFE7D9BD)

private enum class HomePeriodAction {
    START,
    END
}

private data class HomeRingSegment(
    val label: String,
    val days: Int,
    val color: Color
)

private data class HomeRingArc(
    val segment: HomeRingSegment,
    val startAngle: Float,
    val sweepAngle: Float
)

private data class HomeCycleOverview(
    val configured: Boolean,
    val cycleLength: Int,
    val periodLength: Int,
    val daysToNext: Int,
    val expectedStartMillis: Long,
    val cycleDay: Int,
    val fertileWindowText: String,
    val phaseTitle: String,
    val phaseDescription: String,
    val ringSegments: List<HomeRingSegment>
)

private data class HomeLogEntry(
    val title: String,
    val value: String,
    val metricKey: String?,
    val icon: androidx.compose.ui.graphics.vector.ImageVector,
    val tint: Color
)

private data class HomePeriodReminder(
    val daysToNext: Int,
    val text: String
)

private data class HomePhaseMood(
    val label: String,
    val advice: String,
    val accent: Color,
    val orbStart: Color,
    val orbEnd: Color,
    val halo: Color,
    val icon: androidx.compose.ui.graphics.vector.ImageVector
)

@Composable
fun HomeScreen(
    repository: RecordRepository,
    indicatorRepository: DailyIndicatorRepository,
    pinManager: PinManager,
    cycleSettingsVersion: Int,
    filterState: FilterState,
    onAddRecord: (String?) -> Unit,
    onOpenTrends: () -> Unit,
    onOpenJournal: () -> Unit,
    onOpenCycleSettings: () -> Unit
) {
    val context = LocalContext.current
    val allRecords by repository.records().collectAsState(initial = emptyList())
    val allIndicators by indicatorRepository.allIndicators().collectAsState(initial = emptyList())
    val visibleRecords = if (pinManager.isHidden()) emptyList() else allRecords
    val records = applyFilters(visibleRecords, filterState)
    val todayIndicators by indicatorRepository.indicatorsByDate(dayKey(System.currentTimeMillis()))
        .collectAsState(initial = emptyList())

    val cycleManager = remember { CycleSettingsManager(context) }
    val reminderPrefs = remember { ReminderPrefsManager(context) }
    val cycleTip = remember(cycleSettingsVersion, records, allIndicators) {
        buildCycleTip(cycleManager, records, allIndicators)
    }
    val cycleOverview = remember(cycleSettingsVersion, allIndicators) {
        buildHomeCycleOverview(cycleManager, allIndicators)
    }
    val todayLogs = remember(todayIndicators, visibleRecords) {
        buildTodayLogEntries(todayIndicators, visibleRecords)
    }
    val reminderAdvanceDays = reminderPrefs.reminderAdvanceDays()
    val periodReminder = remember(cycleOverview, reminderAdvanceDays) {
        buildHomePeriodReminder(cycleOverview, reminderAdvanceDays)
    }
    val todayText = remember {
        SimpleDateFormat("M月d日 EEEE", Locale.CHINA).format(Date())
    }
    val phaseMood = remember(cycleOverview.phaseTitle, cycleOverview.configured) {
        buildHomePhaseMood(cycleOverview)
    }

    GlassBackground {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = AppSpacing.bottomBarInset),
            verticalArrangement = Arrangement.spacedBy(AppSpacing.large)
        ) {
            item {
                HomeGreetingHeader(
                    todayText = todayText,
                    onBellClick = onOpenTrends
                )
            }

            item {
                HomeCycleRingCard(
                    overview = cycleOverview,
                    mood = phaseMood,
                    onPrimaryClick = {
                        if (cycleOverview.configured) onOpenTrends() else onOpenCycleSettings()
                    }
                )
            }

            item {
                HomePhaseOverviewCard(
                    overview = cycleOverview,
                    cycleTip = cycleTip,
                    mood = phaseMood,
                    periodReminder = periodReminder,
                    onLogFlow = { onAddRecord("流量") },
                    onLogSymptom = { onAddRecord("症状") },
                    onLogMood = { onAddRecord("心情") }
                )
            }

            item {
                HomeMinimalLinksCard(
                    todayLogs = todayLogs,
                    configured = cycleOverview.configured,
                    onOpenJournal = onOpenJournal,
                    onOpenTrends = onOpenTrends,
                    onOpenCycleSettings = onOpenCycleSettings
                )
            }
        }
    }
}

private fun buildHomePeriodReminder(
    overview: HomeCycleOverview,
    advanceDays: Int
): HomePeriodReminder? {
    if (!overview.configured || overview.expectedStartMillis <= 0L) return null
    val days = overview.daysToNext
    if (days < 0) return null
    if (days > advanceDays.coerceIn(0, 7)) return null
    val text = if (days == 0) {
        "月经预计今天来，出门注意带卫生巾。"
    } else {
        "月经预计还有${days}天来，出门注意带卫生巾。"
    }
    return HomePeriodReminder(daysToNext = days, text = text)
}

@Composable
private fun HomeGreetingHeader(
    todayText: String,
    onBellClick: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(
                text = "知期",
                style = MaterialTheme.typography.headlineSmall,
                color = ZhiQiTokens.TextPrimary,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "YOUR RHYTHMIC SOULMATE",
                style = MaterialTheme.typography.labelSmall,
                color = ZhiQiTokens.TextMuted,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "今天：$todayText",
                style = MaterialTheme.typography.bodyMedium,
                color = ZhiQiTokens.TextSecondary
            )
        }

        Box(
            modifier = Modifier.noRippleClickable(onBellClick)
        ) {
            AppIconBadge(
                icon = Icons.Filled.Notifications,
                contentDescription = "提醒",
                tint = ZhiQiTokens.Tertiary,
                modifier = Modifier.size(42.dp),
                shape = CircleShape
            )
        }
    }
}

@Composable
private fun HomeCycleRingCard(
    overview: HomeCycleOverview,
    mood: HomePhaseMood,
    onPrimaryClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onPrimaryClick)
            .padding(top = 4.dp, bottom = 6.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(300.dp),
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .size(244.dp)
                    .background(mood.halo.copy(alpha = 0.3f), CircleShape)
            )
            Canvas(modifier = Modifier.size(276.dp)) {
                drawCircle(
                    color = Color.White.copy(alpha = 0.78f),
                    radius = size.minDimension / 2f,
                    style = Stroke(
                        width = 2.dp.toPx(),
                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(12f, 10f))
                    )
                )
            }
            Box(
                modifier = Modifier
                    .size(176.dp)
                    .shadow(
                        elevation = 28.dp,
                        shape = CircleShape,
                        ambientColor = mood.accent.copy(alpha = 0.28f),
                        spotColor = Color(0xFFD9C16D).copy(alpha = 0.18f)
                    )
                    .background(
                        Brush.linearGradient(listOf(mood.orbStart, mood.orbEnd)),
                        CircleShape
                    )
                    .border(4.dp, Color.White.copy(alpha = 0.74f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "第",
                        style = MaterialTheme.typography.labelMedium,
                        color = ZhiQiTokens.TextPrimary.copy(alpha = 0.78f),
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = overview.cycleDay.takeIf { overview.configured }?.toString() ?: "--",
                        fontSize = 64.sp,
                        lineHeight = 66.sp,
                        color = ZhiQiTokens.TextPrimary,
                        fontWeight = FontWeight.Black
                    )
                    Text(
                        text = "天",
                        style = MaterialTheme.typography.labelMedium,
                        color = ZhiQiTokens.TextPrimary.copy(alpha = 0.78f),
                        fontWeight = FontWeight.Bold
                    )
                }
            }
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .offset(y = (-8).dp)
                    .background(Color.White.copy(alpha = 0.64f), RoundedCornerShape(18.dp))
                    .border(1.dp, Color.White.copy(alpha = 0.8f), RoundedCornerShape(18.dp))
                    .padding(horizontal = 14.dp, vertical = 8.dp)
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = mood.icon,
                        contentDescription = mood.label,
                        tint = mood.accent,
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = mood.label,
                        style = MaterialTheme.typography.labelLarge,
                        color = ZhiQiTokens.TextPrimary,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }
    }
}

@Composable
private fun HomePhaseOverviewCard(
    overview: HomeCycleOverview,
    cycleTip: CycleTip,
    mood: HomePhaseMood,
    periodReminder: HomePeriodReminder?,
    onLogFlow: () -> Unit,
    onLogSymptom: () -> Unit,
    onLogMood: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .glassCard()
            .background(Brush.verticalGradient(listOf(Color.White.copy(alpha = 0.18f), mood.orbEnd.copy(alpha = 0.12f), Color.Transparent)))
            .padding(horizontal = 18.dp, vertical = 20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Top
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                AppBadge(
                    text = "TODAY RHYTHM",
                    background = ZhiQiTokens.PrimarySoft,
                    textColor = mood.accent
                )
                Text(
                    text = mood.label,
                    style = MaterialTheme.typography.headlineMedium,
                    color = ZhiQiTokens.TextPrimary,
                    fontWeight = FontWeight.Bold
                )
            }
            AppIconBadge(
                icon = mood.icon,
                contentDescription = mood.label,
                tint = mood.accent,
                modifier = Modifier.size(54.dp),
                shape = RoundedCornerShape(22.dp)
            )
        }

        Text(
            text = "“${mood.advice}”",
            style = MaterialTheme.typography.bodyLarge,
            color = ZhiQiTokens.TextSecondary,
            fontStyle = FontStyle.Italic
        )

        HomeQuickActionRow(
            onLogFlow = onLogFlow,
            onLogSymptom = onLogSymptom,
            onLogMood = onLogMood,
            accent = mood.accent
        )

        HomeQuietSummaryCard(
            overview = overview,
            cycleTip = cycleTip,
            accent = mood.accent
        )

        if (periodReminder != null) {
            HomePeriodReminderCard(reminder = periodReminder)
        }
    }
}

@Composable
private fun HomeQuickActionRow(
    onLogFlow: () -> Unit,
    onLogSymptom: () -> Unit,
    onLogMood: () -> Unit,
    accent: Color
) {
    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        AppQuickActionTile(
            modifier = Modifier.weight(1f),
            icon = Icons.Filled.InvertColors,
            label = "记录流量",
            iconTint = ZhiQiTokens.Tertiary,
            highlighted = false,
            onClick = onLogFlow
        )
        AppQuickActionTile(
            modifier = Modifier.weight(1f),
            icon = Icons.Filled.Favorite,
            label = "记录症状",
            iconTint = accent,
            highlighted = true,
            onClick = onLogSymptom
        )
        AppQuickActionTile(
            modifier = Modifier.weight(1f),
            icon = Icons.Filled.SentimentSatisfied,
            label = "记录心情",
            iconTint = ZhiQiTokens.Tertiary,
            highlighted = false,
            onClick = onLogMood
        )
    }
}

@Composable
private fun HomeQuietSummaryCard(
    overview: HomeCycleOverview,
    cycleTip: CycleTip,
    accent: Color
) {
    AppPanel(
        modifier = Modifier
            .fillMaxWidth()
            .fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        backgroundAlpha = 0.58f,
        borderAlpha = 0.76f
    ) {
        AppInfoRow(
            icon = Icons.Filled.Favorite,
            iconTint = accent,
            label = "窗口提示",
            text = if (overview.configured) "本周期：${overview.fertileWindowText}" else "完成周期设置后自动显示。"
        )
        AppInfoRow(
            icon = Icons.Filled.Lightbulb,
            iconTint = accent,
            label = "身体留言",
            text = cycleTip.tip
        )
    }
}

@Composable
private fun HomeMinimalLinksCard(
    todayLogs: List<HomeLogEntry>,
    configured: Boolean,
    onOpenJournal: () -> Unit,
    onOpenTrends: () -> Unit,
    onOpenCycleSettings: () -> Unit
) {
    val previewText = when {
        todayLogs.isEmpty() -> "今天还没有留下记录。"
        todayLogs.size == 1 -> "已记录：${todayLogs.first().title} · ${todayLogs.first().value}"
        else -> "已记录：${todayLogs.take(2).joinToString(" / ") { "${it.title} ${it.value}" }}"
    }
    AppSurfaceCard(
        verticalArrangement = Arrangement.spacedBy(AppSpacing.medium)
    ) {
        AppBadge(text = "TODAY", background = ZhiQiTokens.PrimarySoft, textColor = ZhiQiTokens.PrimaryStrong)
        Text(
            text = "接下来",
            style = MaterialTheme.typography.titleLarge,
            color = ZhiQiTokens.TextPrimary,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = previewText,
            style = MaterialTheme.typography.bodyMedium,
            color = ZhiQiTokens.TextSecondary
        )
        Row(horizontalArrangement = Arrangement.spacedBy(AppSpacing.small)) {
            AppArrowAction(
                modifier = Modifier.weight(1f),
                label = "记录中心",
                onClick = onOpenJournal
            )
            AppArrowAction(
                modifier = Modifier.weight(1f),
                label = "趋势复盘",
                onClick = onOpenTrends
            )
        }
        if (!configured) {
            AppArrowAction(
                modifier = Modifier.fillMaxWidth(),
                label = "先完成周期设置",
                onClick = onOpenCycleSettings
            )
        }
    }
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawPhaseTextOnOuterRing(
    arcs: List<HomeRingArc>,
    diameter: Float
) {
    val basePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = android.graphics.Color.parseColor("#7E6E69")
        textSize = 10.dp.toPx()
        isFakeBoldText = true
        typeface = android.graphics.Typeface.create(
            android.graphics.Typeface.DEFAULT,
            android.graphics.Typeface.BOLD
        )
    }

    // Keep text baseline on the ring midline, then center glyphs with font metrics.
    val textDiameter = diameter
    val centerX = size.width / 2f
    val centerY = size.height / 2f
    val labelOval = RectF(
        centerX - textDiameter / 2f,
        centerY - textDiameter / 2f,
        centerX + textDiameter / 2f,
        centerY + textDiameter / 2f
    )

    drawIntoCanvas { canvas ->
        val nativeCanvas = canvas.nativeCanvas
        arcs.forEach { arc ->
            val text = phaseExplainText(arc.segment.label)
            if (text.isBlank()) return@forEach

            val textStartAngle = if (arc.sweepAngle < 0f) arc.startAngle + arc.sweepAngle else arc.startAngle
            val textSweep = abs(arc.sweepAngle)
            val textPadding = 5f
            val drawableSweep = (textSweep - textPadding * 2f).coerceAtLeast(0f)
            if (drawableSweep < 10f) return@forEach

            val midAngle = normalizeRingAngle(textStartAngle + textSweep / 2f)
            val shouldReverse = shouldReverseTextPath(midAngle)
            val path = Path().apply {
                if (shouldReverse) {
                    addArc(
                        labelOval,
                        textStartAngle + textSweep - textPadding,
                        -drawableSweep
                    )
                } else {
                    addArc(
                        labelOval,
                        textStartAngle + textPadding,
                        drawableSweep
                    )
                }
            }

            val pathLength = PathMeasure(path, false).length
            if (pathLength <= 1f) return@forEach

            val textPaint = Paint(basePaint)
            val maxTextWidth = pathLength * 0.86f
            val baseTextWidth = textPaint.measureText(text)
            if (baseTextWidth > maxTextWidth) {
                val scale = (maxTextWidth / baseTextWidth).coerceIn(0.76f, 1f)
                textPaint.textSize = textPaint.textSize * scale
            }
            val textWidth = textPaint.measureText(text)
            if (textWidth > pathLength * 0.98f) return@forEach

            val hOffset = ((pathLength - textWidth) / 2f).coerceAtLeast(0f)
            val metrics = textPaint.fontMetrics
            val centeredOffset = -((metrics.ascent + metrics.descent) / 2f)
            val vOffset = if (shouldReverse) -centeredOffset else centeredOffset
            nativeCanvas.drawTextOnPath(text, path, hOffset, vOffset, textPaint)
        }
    }
}

private fun phaseExplainText(phaseLabel: String): String {
    return when (phaseLabel) {
        "经期" -> "经期护理"
        "卵泡期" -> "卵泡恢复"
        "易孕窗" -> "易孕关注"
        "黄体期" -> "黄体调养"
        else -> phaseLabel
    }
}

private fun shouldReverseTextPath(midAngle: Float): Boolean {
    val tangent = normalizeTo180(midAngle + 90f)
    return tangent > 90f || tangent < -90f
}

private fun normalizeTo180(angle: Float): Float {
    var normalized = (angle + 180f) % 360f
    if (normalized < 0f) normalized += 360f
    return normalized - 180f
}

private fun normalizeRingAngle(angle: Float): Float {
    var normalized = angle % 360f
    if (normalized < 0f) normalized += 360f
    return normalized
}

@Composable
private fun HomePeriodReminderCard(reminder: HomePeriodReminder) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .glassPanel(shape = RoundedCornerShape(24.dp), backgroundAlpha = 0.58f, borderAlpha = 0.76f)
            .padding(horizontal = 16.dp, vertical = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(38.dp)
                .background(ZhiQiTokens.AccentStrongerSoft, RoundedCornerShape(14.dp))
                .border(1.dp, ZhiQiTokens.Border, RoundedCornerShape(14.dp)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Filled.Notifications,
                contentDescription = "经期提醒",
                tint = MaterialTheme.colorScheme.tertiary,
                modifier = Modifier.size(18.dp)
            )
        }
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Text(
                text = if (reminder.daysToNext == 0) "今日提醒" else "温柔提醒",
                style = MaterialTheme.typography.titleMedium,
                color = ZhiQiTokens.TextPrimary
            )
            Text(
                text = reminder.text,
                style = MaterialTheme.typography.bodyMedium,
                color = ZhiQiTokens.TextSecondary
            )
        }
    }
}

private fun buildHomeCycleOverview(
    manager: CycleSettingsManager,
    indicators: List<DailyIndicatorEntity>
): HomeCycleOverview {
    val defaultSegments = listOf(
        HomeRingSegment(label = "经期", days = 5, color = HOME_RING_PERIOD_COLOR),
        HomeRingSegment(label = "卵泡期", days = 9, color = HOME_RING_FOLLICULAR_COLOR),
        HomeRingSegment(label = "易孕窗", days = 6, color = HOME_RING_FERTILE_COLOR),
        HomeRingSegment(label = "黄体期", days = 8, color = HOME_RING_LUTEAL_COLOR)
    )

    if (!manager.isConfigured() || manager.lastPeriodStartMillis() <= 0L) {
        return HomeCycleOverview(
            configured = false,
            cycleLength = 28,
            periodLength = 5,
            daysToNext = 0,
            expectedStartMillis = 0L,
            cycleDay = 0,
            fertileWindowText = "未设置",
            phaseTitle = "未设置",
            phaseDescription = "请先完成周期设置",
            ringSegments = defaultSegments
        )
    }

    val dayMillis = 24L * 60L * 60L * 1000L
    val cycleLength = manager.cycleLengthDays().coerceAtLeast(21)
    val periodLength = manager.periodLengthDays().coerceIn(2, MAX_PERIOD_DAYS)
    val configuredStart = startOfDay(manager.lastPeriodStartMillis())
    val actualRanges = buildActualPeriodRanges(manager, indicators)
    val anchorStart = resolveAnchorStart(configuredStart, actualRanges)
    val todayStart = startOfDay(System.currentTimeMillis())
    val daysSince = ((todayStart - anchorStart) / dayMillis).toInt().coerceAtLeast(0)
    val cycleDay = (daysSince % cycleLength) + 1
    val expectedStart = anchorStart + cycleLength * dayMillis
    val daysToNext = ((expectedStart - todayStart) / dayMillis).toInt()

    val ovulationDay = (cycleLength - 14).coerceAtLeast(periodLength + 1).coerceAtMost(cycleLength - 1)
    val ovulationOffset = ovulationDay - 1
    val fertileStart = (ovulationOffset - 5).coerceAtLeast(periodLength)
    val fertileEnd = (ovulationOffset + 1).coerceAtMost(cycleLength - 1)

    val follicularDays = (fertileStart - periodLength).coerceAtLeast(1)
    val fertileDays = (fertileEnd - fertileStart + 1).coerceAtLeast(1)
    val lutealDays = (cycleLength - periodLength - follicularDays - fertileDays).coerceAtLeast(1)

    val phaseTitle = when {
        cycleDay <= periodLength -> "经期"
        cycleDay - 1 in fertileStart..fertileEnd -> if (cycleDay - 1 == ovulationOffset) "排卵日" else "易孕窗口"
        cycleDay - 1 < fertileStart -> "卵泡期"
        else -> "黄体期"
    }

    val phaseDescription = when (phaseTitle) {
        "经期" -> "注意休息，观察身体状态。"
        "排卵日" -> "今天接近排卵高点，注意防护。"
        "易孕窗口" -> "当前处于易孕窗口，建议更重视措施。"
        "卵泡期" -> "身体状态通常更平稳。"
        else -> "容易疲劳，建议规律作息。"
    }

    val fertileStartMillis = anchorStart + fertileStart * dayMillis
    val fertileEndMillis = anchorStart + fertileEnd * dayMillis
    val fertileWindowText = "${formatMonthDay(fertileStartMillis)} - ${formatMonthDay(fertileEndMillis)}"

    val segments = listOf(
        HomeRingSegment(label = "经期", days = periodLength, color = HOME_RING_PERIOD_COLOR),
        HomeRingSegment(label = "卵泡期", days = follicularDays, color = HOME_RING_FOLLICULAR_COLOR),
        HomeRingSegment(label = "易孕窗", days = fertileDays, color = HOME_RING_FERTILE_COLOR),
        HomeRingSegment(label = "黄体期", days = lutealDays, color = HOME_RING_LUTEAL_COLOR)
    )

    return HomeCycleOverview(
        configured = true,
        cycleLength = cycleLength,
        periodLength = periodLength,
        daysToNext = daysToNext,
        expectedStartMillis = expectedStart,
        cycleDay = cycleDay,
        fertileWindowText = fertileWindowText,
        phaseTitle = phaseTitle,
        phaseDescription = phaseDescription,
        ringSegments = segments
    )
}

private fun buildHomePhaseMood(overview: HomeCycleOverview): HomePhaseMood {
    if (!overview.configured) {
        return HomePhaseMood(
            label = "待设置 · 花园初见",
            advice = "先填入最近一次经期开始日，首页会为你生成节律与提醒。",
            accent = ZhiQiTokens.Primary,
            orbStart = ZhiQiTokens.SecondarySoft,
            orbEnd = Color(0xFFFFE59A),
            halo = ZhiQiTokens.SecondarySoft.copy(alpha = 0.54f),
            icon = Icons.Filled.Lightbulb
        )
    }
    return when (overview.phaseTitle) {
        "经期" -> HomePhaseMood(
            label = "休养期 · 萌芽",
            advice = "身体正在安静地换季，喝一杯热饮，今天慢一点也没有关系。",
            accent = ZhiQiTokens.PrimaryStrong,
            orbStart = Color(0xFFFFC3D0),
            orbEnd = Color(0xFFFFEDF2),
            halo = ZhiQiTokens.PrimarySoft.copy(alpha = 0.62f),
            icon = Icons.Filled.Hotel
        )
        "卵泡期" -> HomePhaseMood(
            label = "生长期 · 晨曦",
            advice = "轻盈的能量正在回升，适合安排一点新的尝试和轻快节奏。",
            accent = ZhiQiTokens.Tertiary,
            orbStart = Color(0xFFD3DEEE),
            orbEnd = ZhiQiTokens.TertiarySoft,
            halo = ZhiQiTokens.TertiarySoft.copy(alpha = 0.66f),
            icon = Icons.Filled.Lightbulb
        )
        "排卵日", "易孕窗口" -> HomePhaseMood(
            label = "盛放期 · 暖阳",
            advice = "你正处在更明亮的阶段，留意身体信号，也别忘了认真防护。",
            accent = Color(0xFFEA9300),
            orbStart = Color(0xFFFFC32A),
            orbEnd = Color(0xFFFFDE74),
            halo = Color(0xFFFFF0B8).copy(alpha = 0.7f),
            icon = Icons.Filled.Favorite
        )
        else -> HomePhaseMood(
            label = "沉淀期 · 暮色",
            advice = "节律慢慢回落，适合收一收情绪和安排，给自己一点温柔缓冲。",
            accent = Color(0xFF8894A9),
            orbStart = Color(0xFFF0E8D8),
            orbEnd = Color(0xFFFFFAF0),
            halo = Color(0xFFFFF8EB).copy(alpha = 0.86f),
            icon = Icons.Filled.SentimentSatisfied
        )
    }
}

private fun buildTodayLogEntries(
    indicators: List<DailyIndicatorEntity>,
    records: List<RecordEntity>
): List<HomeLogEntry> {
    val todayKey = dayKey(System.currentTimeMillis())
    val indicatorEntries = indicators
        .asSequence()
        .filter { it.displayLabel.isNotBlank() }
        .map { indicator ->
            indicator.updatedAt to HomeLogEntry(
                title = metricTitle(indicator.metricKey),
                value = indicator.displayLabel.trim(),
                metricKey = indicator.metricKey.takeIf(::supportsQuickRecordMetric),
                icon = metricIcon(indicator.metricKey),
                tint = metricAccent(indicator.metricKey)
            )
        }
        .toList()

    val todayLoveRecords = records
        .asSequence()
        .filter { dayKey(it.timeMillis) == todayKey && it.type == "同房" }
        .sortedByDescending { it.timeMillis }
        .toList()

    val behaviorEntries = if (todayLoveRecords.isEmpty()) {
        emptyList()
    } else {
        val latest = todayLoveRecords.first()
        listOf(
            latest.timeMillis to HomeLogEntry(
                title = "爱爱",
                value = if (todayLoveRecords.size > 1) "${todayLoveRecords.size}次" else buildBehaviorSummary(latest),
                metricKey = "爱爱",
                icon = Icons.Filled.Favorite,
                tint = metricAccent("爱爱")
            )
        )
    }

    return (indicatorEntries + behaviorEntries)
        .sortedByDescending { it.first }
        .map { it.second }
}

private fun supportsQuickRecordMetric(metricKey: String): Boolean {
    return metricKey in setOf("流量", "症状", "心情", "白带", "体温", "体重", "日记", "好习惯", "便便", "计划", "爱爱")
}

private fun metricIcon(metricKey: String): androidx.compose.ui.graphics.vector.ImageVector {
    return when (metricKey) {
        "流量", PERIOD_STATUS_KEY -> Icons.Filled.InvertColors
        "心情" -> Icons.Filled.SentimentSatisfied
        "好习惯" -> Icons.Filled.Hotel
        "爱爱" -> Icons.Filled.Favorite
        else -> Icons.Filled.Notifications
    }
}

private fun buildBehaviorSummary(record: RecordEntity): String {
    val protections = record.protections.split("|")
        .map { it.trim() }
        .filter { it.isNotBlank() }
    val other = record.otherProtection?.trim().orEmpty().takeIf { it.isNotBlank() }
    val note = record.note?.trim().orEmpty().takeIf { it.isNotBlank() }
    val summaryParts = buildList {
        add(if (record.type == "同房") "爱爱" else record.type)
        if (protections.isNotEmpty()) {
            add(protections.joinToString(" / "))
        } else if (other == null) {
            add("未记录措施")
        }
        if (other != null) {
            add(other)
        }
        if (note != null) {
            add(note)
        }
    }
    return summaryParts.joinToString(" · ").ifBlank { "已记录" }
}
private data class CycleTip(
    val title: String,
    val phase: String,
    val tip: String,
    val needSetup: Boolean,
    val isMenstrualNow: Boolean,
    val overdueWarning: String? = null,
    val heroTitle: String,
    val heroSubtitle: String,
    val pregnancyChance: String,
    val pregnancyConfidence: String,
    val pregnancyDetail: String
)

private data class HomePeriodRange(val startMillis: Long, val endMillis: Long)

private fun buildCycleTip(
    manager: CycleSettingsManager,
    records: List<RecordEntity>,
    indicators: List<DailyIndicatorEntity>
): CycleTip {
    if (!manager.isConfigured() || manager.lastPeriodStartMillis() <= 0L) {
        return CycleTip(
            title = "周期建议",
            phase = "未设置生理周期",
            tip = "设置周期后可获得安全期、排卵期、黄体期建议和怀孕概率提示。",
            needSetup = true,
            isMenstrualNow = false,
            overdueWarning = null,
            heroTitle = "记录周期",
            heroSubtitle = "设置最近一次经期开始日，首页会自动展示经期状态和提醒。",
            pregnancyChance = "--",
            pregnancyConfidence = "无估算",
            pregnancyDetail = "设置后可显示动态估算。"
        )
    }

    val cycleLength = manager.cycleLengthDays()
    val periodLength = manager.periodLengthDays()
    val dayMillis = 24L * 60 * 60 * 1000
    val todayStart = startOfDay(System.currentTimeMillis())
    val configuredStart = startOfDay(manager.lastPeriodStartMillis())
    val actualRanges = buildActualPeriodRanges(manager, indicators)
    val anchorStart = resolveAnchorStart(configuredStart, actualRanges)
    val currentRange = actualRanges.firstOrNull { todayStart in it.startMillis..it.endMillis }

    if (anchorStart > todayStart) {
        return CycleTip(
            title = "周期建议",
            phase = "周期设置异常",
            tip = "最近一次经期开始日不能晚于今天，请重新设置。",
            needSetup = true,
            isMenstrualNow = false,
            overdueWarning = null,
            heroTitle = "请修正周期日期",
            heroSubtitle = "当前设置的开始日比今天更晚，无法推断周期状态。",
            pregnancyChance = "--",
            pregnancyConfidence = "无估算",
            pregnancyDetail = "请先修正最近一次开始日。"
        )
    }

    val daysSinceLastStart = ((todayStart - anchorStart) / dayMillis).toInt()
    val expectedNextStart = anchorStart + cycleLength * dayMillis
    val ovulationDay = (cycleLength - 14).coerceAtLeast(periodLength + 1).coerceAtMost(cycleLength - 1)
    val ovulationOffset = ovulationDay - 1
    val fertileStartOffset = (ovulationOffset - 5).coerceAtLeast(periodLength)
    val fertileEndOffset = (ovulationOffset + 1).coerceAtMost(cycleLength - 1)
    val overdueDays = (daysSinceLastStart - cycleLength).coerceAtLeast(0)
    val dayMillisLong = 24L * 60L * 60L * 1000L
    val recentLoveRecords = records.filter {
        it.type == "同房" && startOfDay(it.timeMillis) in (todayStart - 6 * dayMillisLong)..todayStart
    }
    val chancePercent = estimatePregnancyChancePercent(
        todayStart = todayStart,
        lastStart = anchorStart,
        cycleLength = cycleLength,
        periodLength = periodLength,
        records = records
    )
    val pregnancyChance = formatPregnancyChance(
        chancePercent
    )
    val pregnancyConfidence = if (recentLoveRecords.isEmpty()) "仅周期估算" else "周期+记录估算"
    val pregnancyDetail = buildPregnancyDetail(
        daysSinceLastStart = daysSinceLastStart,
        cycleLength = cycleLength,
        recentLoveRecords = recentLoveRecords,
        chancePercent = chancePercent
    )

    return when {
        currentRange != null -> {
            val dayInPeriod = ((todayStart - currentRange.startMillis) / dayMillis).toInt() + 1
            CycleTip(
                title = "今日周期提醒",
                phase = "经期（第${dayInPeriod}天）",
                tip = "建议温和沟通，注意情绪安抚与休息。",
                needSetup = false,
                isMenstrualNow = true,
                overdueWarning = null,
                heroTitle = "经期第${dayInPeriod}天",
                heroSubtitle = "当前处于经期，预计本次经期在${formatMonthDay(currentRange.endMillis)}结束。",
                pregnancyChance = pregnancyChance,
                pregnancyConfidence = pregnancyConfidence,
                pregnancyDetail = pregnancyDetail
            )
        }

        daysSinceLastStart > cycleLength -> CycleTip(
            title = "今日周期提醒",
            phase = "月经推迟${overdueDays}天",
            tip = if (overdueDays >= 7) {
                "已经明显超出预计周期，若近期有行为记录，建议尽快验孕或关注身体变化。"
            } else {
                "月经已较预计开始日推迟，建议先观察并留意身体状态。"
            },
            needSetup = false,
            isMenstrualNow = false,
            overdueWarning = "距离上次月经开始已${daysSinceLastStart}天，已超过预计${overdueDays}天。",
            heroTitle = "距离上次开始已${daysSinceLastStart}天",
            heroSubtitle = "预计本次月经应在${formatMonthDay(expectedNextStart)}开始，目前处于超期状态。",
            pregnancyChance = pregnancyChance,
            pregnancyConfidence = pregnancyConfidence,
            pregnancyDetail = pregnancyDetail
        )

        daysSinceLastStart == cycleLength -> CycleTip(
            title = "今日周期提醒",
            phase = "预计经期开始日",
            tip = "按设置周期推算，今天是下一次月经的预计开始日，可留意是否来潮。",
            needSetup = false,
            isMenstrualNow = false,
            overdueWarning = null,
            heroTitle = "距离上次开始已${daysSinceLastStart}天",
            heroSubtitle = "今天是预计经期开始日，如未开始可继续观察1到2天。",
            pregnancyChance = pregnancyChance,
            pregnancyConfidence = pregnancyConfidence,
            pregnancyDetail = pregnancyDetail
        )

        daysSinceLastStart in fertileStartOffset..fertileEndOffset -> {
            val fertileDay = daysSinceLastStart + 1
            val isOvulationDay = daysSinceLastStart == ovulationOffset
            CycleTip(
                title = "今日周期提醒",
                phase = if (isOvulationDay) "排卵日" else "排卵期",
                tip = "当前受孕概率相对更高，建议重点关注避孕措施与身体变化。",
                needSetup = false,
                isMenstrualNow = false,
                overdueWarning = null,
                heroTitle = if (isOvulationDay) "排卵日" else "排卵期",
                heroSubtitle = if (isOvulationDay) {
                    "今天接近排卵高点，属于本周期的重点关注日。"
                } else {
                    "当前是易孕窗口期第${fertileDay - fertileStartOffset}天，建议重视防护。"
                },
                pregnancyChance = pregnancyChance,
                pregnancyConfidence = pregnancyConfidence,
                pregnancyDetail = pregnancyDetail
            )
        }

        daysSinceLastStart < fertileStartOffset -> CycleTip(
            title = "今日周期提醒",
            phase = "卵泡期",
            tip = "身体状态通常更平稳，适合保持原有节奏和适度运动。",
            needSetup = false,
            isMenstrualNow = false,
            overdueWarning = null,
            heroTitle = "卵泡期",
            heroSubtitle = "预计排卵日约在${formatMonthDay(anchorStart + ovulationOffset * dayMillis)}，当前仍在排卵前阶段。",
            pregnancyChance = pregnancyChance,
            pregnancyConfidence = pregnancyConfidence,
            pregnancyDetail = pregnancyDetail
        )

        else -> CycleTip(
            title = "今日周期提醒",
            phase = "黄体期",
            tip = "可能更容易疲惫或情绪敏感，建议规律休息并减少高压安排。",
            needSetup = false,
            isMenstrualNow = false,
            overdueWarning = null,
            heroTitle = "黄体期",
            heroSubtitle = "当前已进入排卵后阶段，预计下次月经在${formatMonthDay(expectedNextStart)}开始。",
            pregnancyChance = pregnancyChance,
            pregnancyConfidence = pregnancyConfidence,
            pregnancyDetail = pregnancyDetail
        )
    }
}

private fun estimatePregnancyChancePercent(
    todayStart: Long,
    lastStart: Long,
    cycleLength: Int,
    periodLength: Int,
    records: List<RecordEntity>
): Int {
    val phaseOnlyPercent = fertilityPercentForDate(
        dateMillis = todayStart,
        lastStart = lastStart,
        cycleLength = cycleLength,
        periodLength = periodLength
    )
    val dayMillis = 24L * 60L * 60L * 1000L
    val recentLoveRecords = records.filter { record ->
        record.type == "同房" && startOfDay(record.timeMillis) in (todayStart - 6 * dayMillis)..todayStart
    }
    if (recentLoveRecords.isEmpty()) return phaseOnlyPercent

    val combinedRisk = recentLoveRecords.fold(0.0) { accumulated, record ->
        val dailyFertility = fertilityPercentForDate(
            dateMillis = startOfDay(record.timeMillis),
            lastStart = lastStart,
            cycleLength = cycleLength,
            periodLength = periodLength
        ) / 100.0
        val protectionFactor = protectionRiskFactor(record.protections)
        val eventRisk = (dailyFertility * protectionFactor).coerceIn(0.0, 0.95)
        1 - ((1 - accumulated) * (1 - eventRisk))
    }

    // Keep a small cycle baseline while letting protection-adjusted behavior dominate.
    val baseline = (phaseOnlyPercent / 100.0) * 0.25
    val blended = baseline + (1 - baseline) * combinedRisk
    return (blended * 100).roundToInt().coerceIn(1, 95)
}

private fun fertilityPercentForDate(
    dateMillis: Long,
    lastStart: Long,
    cycleLength: Int,
    periodLength: Int
): Int {
    val dayMillis = 24L * 60L * 60L * 1000L
    val diffDays = ((startOfDay(dateMillis) - startOfDay(lastStart)) / dayMillis).toInt()
    if (diffDays < 0) return 1

    val cycleDay = diffDays % cycleLength
    val ovulationOffset = (cycleLength - 14 - 1).coerceAtLeast(periodLength).coerceAtMost(cycleLength - 1)
    val fertileStart = (ovulationOffset - 5).coerceAtLeast(periodLength)
    val fertileEnd = (ovulationOffset + 1).coerceAtMost(cycleLength - 1)

    return when {
        cycleDay < periodLength -> (2 + cycleDay).coerceAtMost(6)
        cycleDay == ovulationOffset -> 32
        cycleDay in fertileStart..fertileEnd -> {
            when (abs(cycleDay - ovulationOffset)) {
                1 -> 26
                2 -> 21
                3 -> 16
                4 -> 12
                5 -> 9
                else -> 18
            }
        }
        cycleDay < fertileStart -> {
            val span = (fertileStart - periodLength).coerceAtLeast(1)
            val progress = (cycleDay - periodLength).toFloat() / span.toFloat()
            (5 + progress * 6f).roundToInt()
        }
        else -> {
            val lutealSpan = (cycleLength - fertileEnd - 1).coerceAtLeast(1)
            val progress = (cycleDay - fertileEnd).toFloat() / lutealSpan.toFloat()
            (10 - progress * 7f).roundToInt()
        }
    }.coerceIn(1, 35)
}

private fun protectionRiskFactor(protectionsRaw: String): Double {
    val protections = protectionsRaw.split("|").filter { it.isNotBlank() }.toSet()
    if (protections.isEmpty()) return 1.0
    if ("无防护" in protections || "无措施" in protections) return 1.0

    val factors = protections.mapNotNull { protection ->
        when (protection) {
            "避孕套" -> 0.13
            "短效避", "短效避孕药" -> 0.08
            "长效避", "长效避孕", "长效避孕药", "节育环" -> 0.02
            "紧急避", "紧急避孕药" -> 0.18
            "体外排", "体外", "体外排精", "未射精" -> 0.45
            "其他措", "其他" -> 0.65
            else -> null
        }
    }
    if (factors.isEmpty()) return 0.75

    val strongest = factors.minOrNull() ?: 0.75
    val hasStackedMethods = factors.size >= 2
    val stackedFactor = if (hasStackedMethods) strongest * 0.8 else strongest
    return stackedFactor.coerceIn(0.01, 1.0)
}

private fun formatPregnancyChance(percent: Int): String {
    return if (percent <= 1) "<1%" else "$percent%"
}

private fun buildPregnancyDetail(
    daysSinceLastStart: Int,
    cycleLength: Int,
    recentLoveRecords: List<RecordEntity>,
    chancePercent: Int
): String {
    val protectionSummary = recentLoveRecords
        .flatMap { it.protections.split("|") }
        .filter { it.isNotBlank() }
        .groupingBy { it }
        .eachCount()
        .maxByOrNull { it.value }
        ?.key
        ?: "无近7天行为记录"
    return "估算依据：周期第${daysSinceLastStart + 1}天/$cycleLength，近7天行为${recentLoveRecords.size}次，主要措施：$protectionSummary（已纳入折算），估算值${formatPregnancyChance(chancePercent)}。"
}

private fun buildActualPeriodRanges(
    manager: CycleSettingsManager,
    indicators: List<DailyIndicatorEntity>
): List<HomePeriodRange> {
    val dayMillis = 24L * 60L * 60L * 1000L
    val periodLength = manager.periodLengthDays().coerceIn(1, MAX_PERIOD_DAYS)
    val starts = indicators
        .filter { it.metricKey == PERIOD_STATUS_KEY && it.optionValue == PERIOD_STARTED }
        .map { parseDayKey(it.dateKey) }
        .filter { it > 0L }
        .toMutableList()
    val ends = indicators
        .filter { it.metricKey == PERIOD_STATUS_KEY && it.optionValue == PERIOD_ENDED }
        .map { parseDayKey(it.dateKey) }
        .filter { it > 0L }
        .sorted()
    val configuredStart = startOfDay(manager.lastPeriodStartMillis())
    if (configuredStart > 0L && configuredStart !in starts) {
        starts += configuredStart
    }
    val sortedStarts = starts.distinct().sorted()
    if (sortedStarts.isEmpty()) return emptyList()

    return sortedStarts.mapIndexed { index, start ->
        val nextStart = sortedStarts.getOrNull(index + 1)
        val hardMaxEnd = start + (MAX_PERIOD_DAYS - 1L) * dayMillis
        val defaultEnd = start + (periodLength - 1L) * dayMillis
        val explicitEnd = ends.firstOrNull { end ->
            end >= start && end <= hardMaxEnd && (nextStart == null || end < nextStart)
        }
        val resolvedEnd = if (explicitEnd != null) {
            listOfNotNull(explicitEnd, hardMaxEnd, nextStart?.minus(dayMillis)).minOrNull() ?: explicitEnd
        } else {
            listOfNotNull(defaultEnd, hardMaxEnd, nextStart?.minus(dayMillis)).minOrNull() ?: defaultEnd
        }
        HomePeriodRange(startMillis = start, endMillis = resolvedEnd)
    }
}

private fun resolveAnchorStart(configuredStart: Long, ranges: List<HomePeriodRange>): Long {
    val latestActualStart = ranges.maxByOrNull { it.startMillis }?.startMillis ?: 0L
    return maxOf(configuredStart, latestActualStart)
}

private fun resolveHomePeriodAction(
    manager: CycleSettingsManager,
    indicators: List<DailyIndicatorEntity>,
    targetDateMillis: Long
): HomePeriodAction {
    val normalized = startOfDay(targetDateMillis)
    val dayMillis = 24L * 60L * 60L * 1000L
    val latestStart = indicators
        .asSequence()
        .filter { it.metricKey == PERIOD_STATUS_KEY && it.optionValue == PERIOD_STARTED }
        .map { parseDayKey(it.dateKey) }
        .filter { it in 1L..normalized }
        .maxOrNull()
        ?: manager.lastPeriodStartMillis().takeIf { it > 0L }?.let(::startOfDay)
    val latestEnd = indicators
        .asSequence()
        .filter { it.metricKey == PERIOD_STATUS_KEY && it.optionValue == PERIOD_ENDED }
        .map { parseDayKey(it.dateKey) }
        .filter { it in 1L..normalized }
        .maxOrNull()
    val hasOpenPeriod =
        latestStart != null &&
            (latestEnd == null || latestEnd < latestStart) &&
            normalized in latestStart..(latestStart + (MAX_PERIOD_DAYS - 1) * dayMillis)
    return if (hasOpenPeriod) HomePeriodAction.END else HomePeriodAction.START
}

private fun startOfDay(timeMillis: Long): Long {
    val calendar = Calendar.getInstance().apply {
        timeInMillis = timeMillis
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }
    return calendar.timeInMillis
}

private fun formatMonthDay(timeMillis: Long): String {
    return SimpleDateFormat("M月d日", Locale.getDefault()).format(Date(timeMillis))
}

private fun dayKey(timeMillis: Long): String {
    return SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date(timeMillis))
}

private fun parseDayKey(dateKey: String): Long {
    return SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).parse(dateKey)?.time?.let(::startOfDay) ?: 0L
}

private fun applyFilters(records: List<RecordEntity>, state: FilterState): List<RecordEntity> {
    if (state.types.isEmpty() && state.protections.isEmpty()) return records
    return records.filter { record ->
        val typeOk = state.types.isEmpty() || state.types.contains(record.type)
        val protections = record.protections.split("|").filter { it.isNotBlank() }.toSet()
        val protectionOk = state.protections.isEmpty() || protections.any { state.protections.contains(it) }
        typeOk && protectionOk
    }
}
