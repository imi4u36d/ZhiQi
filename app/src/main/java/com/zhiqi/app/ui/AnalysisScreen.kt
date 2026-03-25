package com.zhiqi.app.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoGraph
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.TipsAndUpdates
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.zhiqi.app.R
import com.zhiqi.app.data.DailyIndicatorEntity
import com.zhiqi.app.data.RecordEntity
import com.zhiqi.app.ui.components.AppArrowAction
import com.zhiqi.app.ui.components.AppBadge
import com.zhiqi.app.ui.components.AppSurfaceCard
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private const val ANALYSIS_WINDOW_DAYS = 30
private const val PERIOD_STATUS_KEY = "月经状态"
private const val PERIOD_STARTED = "start"
private const val DAY_MILLIS = 24L * 60L * 60L * 1000L
private const val MAX_PIE_SLICES = 6

@Composable
fun AnalysisScreen(
    records: List<RecordEntity>,
    indicators: List<DailyIndicatorEntity>,
    onOpenRecordPage: () -> Unit = {}
) {
    val context = LocalContext.current
    val cycleManager = remember { CycleSettingsManager(context) }
    val state = remember(records, indicators) {
        buildAnalysisOverview(records, indicators)
    }
    val currentPhase = remember(indicators, cycleManager) {
        detectCurrentPhase(cycleManager, indicators)
    }

    val knowledgeCards = remember {
        listOf(
            KnowledgeCard(
                title = "经前容易疲惫怎么办？",
                summary = "从睡眠、饮食和运动强度三个方面，给出 3 条可执行建议。",
                tag = "经前综合征",
                relatedPhases = setOf("黄体期")
            ),
            KnowledgeCard(
                title = "白带变化如何观察",
                summary = "不同阶段的常见变化与需要关注的信号，用通俗语言说明。",
                tag = "周期科普",
                relatedPhases = setOf("卵泡期", "易孕窗")
            ),
            KnowledgeCard(
                title = "经痛缓解小贴士",
                summary = "热敷、拉伸、作息调整的组合方案，适合日常自我管理。",
                tag = "日常护理",
                relatedPhases = setOf("经期")
            ),
            KnowledgeCard(
                title = "什么时候需要验孕或就医？",
                summary = "月经明显推迟、出血异常或伴随剧烈腹痛时，建议尽快线下就诊。",
                tag = "风险识别",
                relatedPhases = setOf("黄体期", "经期")
            ),
            KnowledgeCard(
                title = "如何建立稳定记录习惯",
                summary = "固定记录时点、先记录核心项、每周复盘 1 次，准确度提升更明显。",
                tag = "记录方法",
                relatedPhases = setOf("经期", "卵泡期", "易孕窗", "黄体期")
            )
        )
    }
    val phaseGuides = remember(currentPhase) {
        listOf(
            CyclePhaseGuide("经期", "第1-5天", "保暖与休息优先，观察流量与疼痛等级。", ZhiQiTokens.PhaseMenstrual),
            CyclePhaseGuide("卵泡期", "第6-13天", "逐步恢复运动强度，维持规律作息与饮水。", ZhiQiTokens.PhaseFollicular),
            CyclePhaseGuide("易孕窗", "排卵前后约6天", "重点关注白带与体温变化，若无计划建议加强防护。", ZhiQiTokens.PhaseFertile),
            CyclePhaseGuide("黄体期", "排卵后至下次月经前", "容易疲惫或情绪波动，尽量降低高压安排。", ZhiQiTokens.PhaseLuteal)
        ).prioritizeGuidesByCurrentPhase(currentPhase)
    }
    val sortedKnowledgeCards = remember(knowledgeCards, currentPhase) {
        knowledgeCards.prioritizeCardsByCurrentPhase(currentPhase)
    }
    val warningSignals = remember {
        listOf(
            WarningSignal("经量突然明显增多", "连续 2 小时每小时都需更换卫生用品，或出现大血块。"),
            WarningSignal("疼痛影响正常生活", "止痛后仍明显不缓解，伴随恶心、发热或晕厥感。"),
            WarningSignal("周期持续紊乱", "连续 3 个周期波动很大，或月经推迟超过 7 天。"),
            WarningSignal("异常分泌物变化", "颜色、气味明显异常并伴瘙痒/灼痛等不适。")
        )
    }

    GlassBackground {
        LazyColumn(
            contentPadding = PaddingValues(bottom = 112.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            if (state.slices.isEmpty()) {
                item { EmptyInsightCard(onOpenRecordPage = onOpenRecordPage) }
            } else {
                item { TrendOverviewCard(state = state) }
                item { PersonalizedAdviceCard(insight = state.insight) }
            }

            item {
                RecordCoverageCard(
                    summary = state.summary,
                    slices = state.slices,
                    onOpenRecordPage = onOpenRecordPage
                )
            }
            item { CycleGuideSection(guides = phaseGuides, currentPhase = currentPhase) }
            item { KnowledgeSection(cards = sortedKnowledgeCards, currentPhase = currentPhase) }
            item { WarningSignalsSection(signals = warningSignals) }
            item { FaqSection() }
            item { InsightDisclaimerCard() }
            item { Spacer(modifier = Modifier.height(8.dp)) }
        }
    }
}

@Composable
private fun InsightHeaderCard(
    summary: AnalysisSummary,
    hasData: Boolean,
    onOpenRecordPage: () -> Unit
) {
    AppSurfaceCard(
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        AppBadge(text = "TREND NOTE", background = ZhiQiTokens.PrimarySoft, textColor = ZhiQiTokens.PrimaryStrong)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Top
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    text = "风感趋势",
                    style = MaterialTheme.typography.headlineSmall,
                    color = ZhiQiTokens.TextPrimary,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "${summary.startLabel} - ${summary.endLabel}",
                    style = MaterialTheme.typography.bodySmall,
                    color = ZhiQiTokens.TextMuted
                )
            }

            Box(
                modifier = Modifier
                    .size(60.dp)
                    .glassPanel(shape = RoundedCornerShape(24.dp), backgroundAlpha = 0.86f, borderAlpha = 0.8f),
                contentAlignment = Alignment.Center
            ) {
                Image(
                    painter = painterResource(id = R.drawable.ill_brand_blossom),
                    contentDescription = "品牌插画",
                    modifier = Modifier.size(40.dp)
                )
            }
        }

        Text(
            text = if (hasData) {
                "最近${summary.totalDays}天共记录 ${summary.totalEntries} 条，活跃天数 ${summary.activeDays} 天。"
            } else {
                "当前记录样本不足，先完成几天记录，趋势会更准确。"
            },
            style = MaterialTheme.typography.bodyMedium,
            color = ZhiQiTokens.TextSecondary
        )
        AppArrowAction(label = "去记录", onClick = onOpenRecordPage)
    }
}

@Composable
private fun EmptyInsightCard(onOpenRecordPage: () -> Unit) {
    AppSurfaceCard(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        AppBadge(text = "EMPTY", background = ZhiQiTokens.SecondarySoft, textColor = ZhiQiTokens.Secondary)
        Image(
            painter = painterResource(id = R.drawable.ill_empty_journal),
            contentDescription = "空状态插画",
            modifier = Modifier.size(164.dp)
        )
        Text("暂无可分析数据", style = MaterialTheme.typography.titleMedium, color = ZhiQiTokens.TextPrimary)
        Text(
            "建议先记录流量、疼痛、情绪、睡眠等高频项，连续 7 天后会生成趋势洞察。",
            style = MaterialTheme.typography.bodyMedium,
            color = ZhiQiTokens.TextSecondary
        )
        AppArrowAction(label = "去记录今天", onClick = onOpenRecordPage)
    }
}

@Composable
private fun TrendOverviewCard(state: AnalysisOverviewState) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .glassCard()
            .padding(horizontal = 18.dp, vertical = 18.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        SoftBadge(text = "TREND")
        Text("最近 30 天的小波动", style = MaterialTheme.typography.titleMedium, color = ZhiQiTokens.TextPrimary)

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier.size(150.dp),
                contentAlignment = Alignment.Center
            ) {
                Canvas(modifier = Modifier.size(140.dp)) {
                    var startAngle = -90f
                    state.slices.forEach { slice ->
                        val sweep = (slice.ratio * 360f).coerceAtLeast(0f)
                        drawArc(
                            color = slice.color,
                            startAngle = startAngle,
                            sweepAngle = sweep,
                            useCenter = true
                        )
                        startAngle += sweep
                    }
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("总记录", style = MaterialTheme.typography.labelMedium, color = ZhiQiTokens.TextMuted)
                    Text(
                        "${state.summary.totalEntries}",
                        style = MaterialTheme.typography.headlineMedium,
                        color = ZhiQiTokens.TextPrimary,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                state.slices.take(4).forEach { slice ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(9.dp)
                                    .background(slice.color, CircleShape)
                            )
                            Text(
                                text = " ${slice.label}",
                                style = MaterialTheme.typography.bodyMedium,
                                color = ZhiQiTokens.TextSecondary
                            )
                        }
                        Text(
                            text = "${(slice.ratio * 100).toInt()}%",
                            style = MaterialTheme.typography.bodySmall,
                            color = ZhiQiTokens.TextMuted
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun PersonalizedAdviceCard(insight: ComprehensiveInsight) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .glassCard()
            .padding(horizontal = 16.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        SoftBadge(text = "CARE TIP", background = ZhiQiTokens.TertiarySoft, textColor = ZhiQiTokens.Tertiary)
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(
                Icons.Filled.TipsAndUpdates,
                contentDescription = "建议",
                tint = MaterialTheme.colorScheme.tertiary
            )
            Text("个性化建议", style = MaterialTheme.typography.titleMedium, color = ZhiQiTokens.TextPrimary)
        }

        Text(
            text = insight.conclusion,
            style = MaterialTheme.typography.bodyMedium,
            color = ZhiQiTokens.TextSecondary
        )
        Text(
            text = "建议：${insight.suggestion}",
            style = MaterialTheme.typography.bodyMedium,
            color = ZhiQiTokens.TextSecondary
        )
        Text(
            text = "提示：建议仅用于健康管理参考，如持续不适请及时就医。",
            style = MaterialTheme.typography.bodySmall,
            color = ZhiQiTokens.TextMuted
        )
    }
}

@Composable
private fun RecordCoverageCard(
    summary: AnalysisSummary,
    slices: List<PieSlice>,
    onOpenRecordPage: () -> Unit
) {
    val coreLabels = remember {
        listOf("流量", "疼痛", "情绪", "睡眠", "白带", "爱爱")
    }
    val coveredLabels = remember(slices) {
        slices.map { it.label }.toSet()
    }
    val missing = remember(coveredLabels) {
        coreLabels.filterNot { coveredLabels.contains(it) }
    }
    val continuity = if (summary.totalDays <= 0) 0f else summary.activeDays.toFloat() / summary.totalDays.toFloat()
    val continuityText = when {
        continuity >= 0.65f -> "记录连续性：优秀（${summary.activeDays}/${summary.totalDays} 天）"
        continuity >= 0.35f -> "记录连续性：中等（${summary.activeDays}/${summary.totalDays} 天）"
        else -> "记录连续性：偏低（${summary.activeDays}/${summary.totalDays} 天）"
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .glassCard()
            .padding(horizontal = 16.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        SoftBadge(
            text = "CHECK LIST",
            background = ZhiQiTokens.SecondarySoft,
            textColor = ZhiQiTokens.Secondary
        )
        Text("记录提升建议", style = MaterialTheme.typography.titleMedium, color = ZhiQiTokens.TextPrimary)
        Text(
            text = "核心指标覆盖：${coreLabels.size - missing.size}/${coreLabels.size} 项",
            style = MaterialTheme.typography.bodyMedium,
            color = ZhiQiTokens.TextSecondary
        )
        Text(
            text = continuityText,
            style = MaterialTheme.typography.bodySmall,
            color = ZhiQiTokens.TextMuted
        )
        Text(
            text = if (missing.isEmpty()) {
                "核心项已覆盖，建议继续保持，优先记录有波动的指标。"
            } else {
                "建议补充：${missing.joinToString("、")}。这些指标对周期判断和建议影响更大。"
            },
            style = MaterialTheme.typography.bodyMedium,
            color = ZhiQiTokens.TextSecondary
        )
        Text(
            text = "去补充记录",
            style = MaterialTheme.typography.labelLarge,
            color = ZhiQiTokens.Primary,
            modifier = Modifier.clickable(onClick = onOpenRecordPage)
        )
    }
}

@Composable
private fun CycleGuideSection(guides: List<CyclePhaseGuide>, currentPhase: String?) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .glassCard()
            .padding(horizontal = 16.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        SoftBadge(text = "PHASE GUIDE", background = ZhiQiTokens.TertiarySoft, textColor = ZhiQiTokens.Tertiary)
        Text("周期阶段指南", style = MaterialTheme.typography.titleMedium, color = ZhiQiTokens.TextPrimary)
        if (!currentPhase.isNullOrBlank()) {
            Text(
                text = "已按当前阶段（$currentPhase）优先展示",
                style = MaterialTheme.typography.bodySmall,
                color = ZhiQiTokens.TextMuted
            )
        }
        guides.forEach { guide ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .glassPanel(shape = RoundedCornerShape(18.dp), backgroundAlpha = 0.78f, borderAlpha = 0.82f)
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.Top
            ) {
                Box(
                    modifier = Modifier
                        .size(10.dp)
                        .background(guide.color, CircleShape)
                )
                Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                    Text(
                        text = "${guide.phase} · ${guide.window}",
                        style = MaterialTheme.typography.titleSmall,
                        color = ZhiQiTokens.TextPrimary
                    )
                    Text(
                        text = guide.tip,
                        style = MaterialTheme.typography.bodySmall,
                        color = ZhiQiTokens.TextSecondary
                    )
                }
            }
        }
    }
}

@Composable
private fun KnowledgeSection(cards: List<KnowledgeCard>, currentPhase: String?) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .glassCard()
            .padding(horizontal = 16.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        SoftBadge(text = "READING")
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("健康知识", style = MaterialTheme.typography.titleMedium, color = ZhiQiTokens.TextPrimary)
            Icon(
                imageVector = Icons.Filled.ChevronRight,
                contentDescription = "查看更多",
                tint = ZhiQiTokens.TextMuted
            )
        }
        if (!currentPhase.isNullOrBlank()) {
            Text(
                text = "优先推荐当前阶段相关内容：$currentPhase",
                style = MaterialTheme.typography.bodySmall,
                color = ZhiQiTokens.TextMuted
            )
        }

        cards.forEach { card ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .glassPanel(shape = RoundedCornerShape(18.dp), backgroundAlpha = 0.78f, borderAlpha = 0.82f)
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .background(ZhiQiTokens.SecondarySoft, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.MenuBook,
                        contentDescription = card.tag,
                        tint = ZhiQiTokens.Primary,
                        modifier = Modifier.size(18.dp)
                    )
                }
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(card.title, style = MaterialTheme.typography.titleSmall, color = ZhiQiTokens.TextPrimary)
                    Text(card.summary, style = MaterialTheme.typography.bodySmall, color = ZhiQiTokens.TextSecondary)
                    Text(card.tag, style = MaterialTheme.typography.labelSmall, color = ZhiQiTokens.TextMuted)
                }
            }
        }
    }
}

@Composable
private fun WarningSignalsSection(signals: List<WarningSignal>) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .glassCard()
            .padding(horizontal = 16.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(9.dp)
    ) {
        SoftBadge(
            text = "RED FLAG",
            background = ZhiQiTokens.PrimarySoft.copy(alpha = 0.72f),
            textColor = ZhiQiTokens.PrimaryStrong
        )
        Text("需要关注的红旗信号", style = MaterialTheme.typography.titleMedium, color = ZhiQiTokens.TextPrimary)
        Text(
            text = "出现以下情况建议尽快线下就诊，避免自行长期观察。",
            style = MaterialTheme.typography.bodySmall,
            color = ZhiQiTokens.TextMuted
        )
        signals.forEach { signal ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .glassPanel(shape = RoundedCornerShape(18.dp), backgroundAlpha = 0.78f, borderAlpha = 0.82f)
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .background(ZhiQiTokens.PrimaryStrong, CircleShape)
                )
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(signal.title, style = MaterialTheme.typography.titleSmall, color = ZhiQiTokens.TextPrimary)
                    Text(signal.detail, style = MaterialTheme.typography.bodySmall, color = ZhiQiTokens.TextSecondary)
                }
            }
        }
    }
}

@Composable
private fun FaqSection() {
    val faqs = remember {
        listOf(
            FaqItem(
                question = "为什么预测日期会波动？",
                answer = "周期会受作息、压力、运动和生病影响，出现 2-5 天波动都较常见。"
            ),
            FaqItem(
                question = "连续漏记会影响准确度吗？",
                answer = "会。建议优先补流量、疼痛、情绪、睡眠这些核心项，预测会更稳定。"
            ),
            FaqItem(
                question = "经前情绪波动如何缓解？",
                answer = "优先保证睡眠、减少咖啡因摄入，并提前安排低压力任务。"
            )
        )
    }
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .glassCard()
            .padding(horizontal = 16.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        SoftBadge(text = "FAQ")
        Text("常见问题", style = MaterialTheme.typography.titleMedium, color = ZhiQiTokens.TextPrimary)
        faqs.forEach { faq ->
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .glassPanel(shape = RoundedCornerShape(18.dp), backgroundAlpha = 0.78f, borderAlpha = 0.82f)
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = "Q：${faq.question}",
                    style = MaterialTheme.typography.titleSmall,
                    color = ZhiQiTokens.TextPrimary
                )
                Text(
                    text = "A：${faq.answer}",
                    style = MaterialTheme.typography.bodySmall,
                    color = ZhiQiTokens.TextSecondary
                )
            }
        }
    }
}

@Composable
private fun InsightDisclaimerCard() {
    Text(
        text = "内容仅供健康管理参考，不替代医疗建议。",
        style = MaterialTheme.typography.bodySmall,
        color = ZhiQiTokens.TextMuted,
        modifier = Modifier
            .glassPanel(shape = RoundedCornerShape(18.dp), backgroundAlpha = 0.68f, borderAlpha = 0.72f)
            .padding(horizontal = 12.dp, vertical = 10.dp)
    )
}

@Composable
private fun SoftBadge(
    text: String,
    background: Color = ZhiQiTokens.PrimarySoft,
    textColor: Color = ZhiQiTokens.Primary
) {
    AppBadge(text = text, background = background, textColor = textColor)
}

private fun buildAnalysisOverview(
    records: List<RecordEntity>,
    indicators: List<DailyIndicatorEntity>
): AnalysisOverviewState {
    val endDay = analysisStartOfDay(System.currentTimeMillis())
    val startDay = endDay - (ANALYSIS_WINDOW_DAYS - 1L) * DAY_MILLIS

    val indicatorWindow = indicators.filter {
        it.metricKey != PERIOD_STATUS_KEY && analysisParseDayKey(it.dateKey) in startDay..endDay
    }
    val recordWindow = records.filter { analysisStartOfDay(it.timeMillis) in startDay..endDay }

    val countByMetric = linkedMapOf<String, Int>()

    indicatorWindow.forEach { indicator ->
        val key = indicator.metricKey
        countByMetric[key] = (countByMetric[key] ?: 0) + 1
    }
    recordWindow.forEach { record ->
        val key = if (record.type == "同房") "爱爱" else record.type
        countByMetric[key] = (countByMetric[key] ?: 0) + 1
    }

    val totalEntries = countByMetric.values.sum()
    val activeDays = buildSet {
        indicatorWindow.forEach { add(it.dateKey) }
        recordWindow.forEach { add(analysisDayKey(it.timeMillis)) }
    }.size

    val summary = AnalysisSummary(
        startLabel = analysisFormatMonthDay(startDay),
        endLabel = analysisFormatMonthDay(endDay),
        totalDays = ANALYSIS_WINDOW_DAYS,
        totalEntries = totalEntries,
        activeDays = activeDays
    )

    val slices = buildPieSlices(countByMetric)
    val insight = buildComprehensiveInsight(
        summary = summary,
        slices = slices,
        indicatorWindow = indicatorWindow
    )

    return AnalysisOverviewState(
        summary = summary,
        slices = slices,
        insight = insight
    )
}

private fun buildPieSlices(countByMetric: Map<String, Int>): List<PieSlice> {
    val total = countByMetric.values.sum()
    if (total <= 0) return emptyList()

    val sorted = countByMetric.entries
        .filter { it.value > 0 }
        .sortedByDescending { it.value }

    val primary = sorted.take(MAX_PIE_SLICES)
    val extra = sorted.drop(MAX_PIE_SLICES)
    val slices = mutableListOf<PieSlice>()

    primary.forEach { entry ->
        val key = entry.key
        val label = metricTitle(key)
        slices += PieSlice(
            label = label,
            count = entry.value,
            ratio = entry.value.toFloat() / total.toFloat(),
            color = metricAccent(key)
        )
    }
    if (extra.isNotEmpty()) {
        val count = extra.sumOf { it.value }
        slices += PieSlice(
            label = "其他",
            count = count,
            ratio = count.toFloat() / total.toFloat(),
            color = ZhiQiTokens.BorderStrong
        )
    }
    return slices
}

private fun buildComprehensiveInsight(
    summary: AnalysisSummary,
    slices: List<PieSlice>,
    indicatorWindow: List<DailyIndicatorEntity>
): ComprehensiveInsight {
    if (summary.totalEntries == 0 || slices.isEmpty()) {
        return ComprehensiveInsight(
            conclusion = "当前记录样本不足，暂时无法形成稳定趋势判断。",
            suggestion = "建议先连续记录 7 到 14 天，再查看趋势与建议。"
        )
    }

    val top = slices.maxByOrNull { it.count }!!
    val continuity = summary.activeDays.toFloat() / summary.totalDays.toFloat()

    val moodRecords = indicatorWindow.filter { it.metricKey == "心情" }
    val moodWaveRate = if (moodRecords.isEmpty()) 0f else {
        moodRecords.count { it.optionValue in setOf("sensitive", "irritable", "sad") }.toFloat() / moodRecords.size.toFloat()
    }

    val symptomRecords = indicatorWindow.filter { it.metricKey == "症状" }
    val symptomRate = if (symptomRecords.isEmpty()) 0f else {
        symptomRecords.count { it.optionValue != "none" }.toFloat() / symptomRecords.size.toFloat()
    }

    val conclusion = buildString {
        append("最近${summary.totalDays}天以${top.label}记录为主，占比${(top.ratio * 100).toInt()}%。")
        append(
            when {
                continuity >= 0.6f -> "记录连续性较好。"
                continuity >= 0.3f -> "记录连续性中等。"
                else -> "记录连续性偏低。"
            }
        )
        when {
            moodWaveRate >= 0.5f && symptomRate >= 0.5f -> append("情绪波动与身体不适记录都偏高。")
            moodWaveRate >= 0.5f -> append("情绪波动记录偏多。")
            symptomRate >= 0.5f -> append("身体不适记录偏多。")
            else -> append("整体状态相对平稳。")
        }
    }

    val suggestion = when {
        continuity < 0.3f -> "建议固定一个每日记录时间，提升连续性后预测会更稳定。"
        moodWaveRate >= 0.5f && symptomRate >= 0.5f -> "建议优先保证睡眠与休息，减少高强度安排，并持续观察 1 到 2 周。"
        moodWaveRate >= 0.5f -> "建议增加放松活动和规律作息，记录触发情绪波动的场景。"
        symptomRate >= 0.5f -> "建议减少刺激性饮食，记录不适触发因素，必要时咨询专业医生。"
        else -> "建议继续保持当前记录节奏，每周回看一次变化。"
    }

    return ComprehensiveInsight(
        conclusion = conclusion,
        suggestion = suggestion
    )
}

private fun detectCurrentPhase(
    cycleManager: CycleSettingsManager,
    indicators: List<DailyIndicatorEntity>
): String? {
    val cycleLength = cycleManager.cycleLengthDays().coerceAtLeast(21)
    val periodLength = cycleManager.periodLengthDays().coerceIn(2, 10)
    val today = analysisStartOfDay(System.currentTimeMillis())
    val configuredStart = analysisStartOfDay(cycleManager.lastPeriodStartMillis())
    val latestPeriodStart = indicators
        .asSequence()
        .filter { it.metricKey == PERIOD_STATUS_KEY && it.optionValue == PERIOD_STARTED }
        .map { analysisParseDayKey(it.dateKey) }
        .filter { it in 1L..today }
        .maxOrNull()
        ?: 0L
    val anchorStart = maxOf(configuredStart, latestPeriodStart)
    if (anchorStart <= 0L || anchorStart > today) return null

    val dayOffset = ((today - anchorStart) / DAY_MILLIS).toInt().coerceAtLeast(0)
    val cycleDay = dayOffset % cycleLength
    val ovulationOffset = (cycleLength - 14 - 1).coerceAtLeast(periodLength).coerceAtMost(cycleLength - 1)
    val fertileStart = (ovulationOffset - 5).coerceAtLeast(periodLength)
    val fertileEnd = (ovulationOffset + 1).coerceAtMost(cycleLength - 1)

    return when {
        cycleDay < periodLength -> "经期"
        cycleDay in fertileStart..fertileEnd -> "易孕窗"
        cycleDay < fertileStart -> "卵泡期"
        else -> "黄体期"
    }
}

private fun List<CyclePhaseGuide>.prioritizeGuidesByCurrentPhase(currentPhase: String?): List<CyclePhaseGuide> {
    if (currentPhase.isNullOrBlank()) return this
    val first = filter { it.phase == currentPhase }
    if (first.isEmpty()) return this
    val rest = filterNot { it.phase == currentPhase }
    return first + rest
}

private fun List<KnowledgeCard>.prioritizeCardsByCurrentPhase(currentPhase: String?): List<KnowledgeCard> {
    if (currentPhase.isNullOrBlank()) return this
    val first = filter { currentPhase in it.relatedPhases }
    if (first.isEmpty()) return this
    val rest = filterNot { currentPhase in it.relatedPhases }
    return first + rest
}

private fun analysisDayKey(timeMillis: Long): String = DAY_KEY_FORMAT.format(Date(timeMillis))

private fun analysisParseDayKey(key: String): Long {
    return runCatching { DAY_KEY_FORMAT.parse(key)?.time ?: 0L }.getOrDefault(0L)
}

private fun analysisStartOfDay(timeMillis: Long): Long {
    return runCatching {
        DAY_KEY_FORMAT.parse(DAY_KEY_FORMAT.format(Date(timeMillis)))?.time ?: timeMillis
    }.getOrDefault(timeMillis)
}

private fun analysisFormatMonthDay(timeMillis: Long): String = MONTH_DAY_FORMAT.format(Date(timeMillis))

private data class AnalysisOverviewState(
    val summary: AnalysisSummary,
    val slices: List<PieSlice>,
    val insight: ComprehensiveInsight
)

private data class AnalysisSummary(
    val startLabel: String,
    val endLabel: String,
    val totalDays: Int,
    val totalEntries: Int,
    val activeDays: Int
)

private data class PieSlice(
    val label: String,
    val count: Int,
    val ratio: Float,
    val color: Color
)

private data class ComprehensiveInsight(
    val conclusion: String,
    val suggestion: String
)

private data class KnowledgeCard(
    val title: String,
    val summary: String,
    val tag: String,
    val relatedPhases: Set<String>
)

private data class CyclePhaseGuide(
    val phase: String,
    val window: String,
    val tip: String,
    val color: Color
)

private data class WarningSignal(
    val title: String,
    val detail: String
)

private data class FaqItem(
    val question: String,
    val answer: String
)

private val DAY_KEY_FORMAT = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
private val MONTH_DAY_FORMAT = SimpleDateFormat("M月d日", Locale.getDefault())
