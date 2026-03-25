package com.zhiqi.app.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.zhiqi.app.data.DailyIndicatorEntity
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun IndicatorSheet(
    metricKey: String,
    targetDateMillis: Long = System.currentTimeMillis(),
    initialIndicator: DailyIndicatorEntity? = null,
    onSave: (DailyIndicatorEntity) -> Unit,
    onClear: (dateKey: String, metricKey: String) -> Unit,
    onCancel: () -> Unit
) {
    val accent = metricAccent(metricKey)
    val options = metricOptions(metricKey)
    val usesFreeTextInput = supportsFreeTextInput(metricKey)
    var selected by remember(metricKey, initialIndicator?.optionValue) {
        mutableStateOf(initialIndicator?.optionValue)
    }
    var selectedLabel by remember(metricKey, initialIndicator?.displayLabel) {
        mutableStateOf(initialIndicator?.displayLabel ?: "")
    }
    var customValue by remember(metricKey, initialIndicator?.optionValue) {
        mutableStateOf(
            when (metricKey) {
                "体温" -> initialIndicator?.optionValue ?: ""
                "体重" -> initialIndicator?.optionValue ?: ""
                "日记" -> initialIndicator?.optionValue ?: ""
                else -> ""
            }
        )
    }
    val scrollState = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(scrollState)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("取消", color = ZhiQiTokens.TextSecondary, modifier = Modifier.noRippleClickable(onCancel))
            Text("今日${metricTitle(metricKey)}", style = MaterialTheme.typography.titleLarge, color = ZhiQiTokens.TextPrimary)
            Text(
                "确定",
                color = accent,
                modifier = Modifier.noRippleClickable {
                    val dateKey = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date(targetDateMillis))
                    val value = when (metricKey) {
                        "体温", "体重", "日记" -> customValue.trim().takeIf { it.isNotBlank() } ?: return@noRippleClickable
                        else -> selected
                    }
                    // 只有下拉/卡片类指标允许“清空即删除”；体温、体重、日记需要保留文本内容。
                    if (!usesFreeTextInput && value == null) {
                        onClear(dateKey, metricKey)
                        return@noRippleClickable
                    }
                    val label = when (metricKey) {
                        "体温" -> "${value}°C"
                        "体重" -> "${value} 公斤"
                        "日记" -> value ?: ""
                        else -> selectedLabel
                    }
                    onSave(
                        DailyIndicatorEntity(
                            dateKey = dateKey,
                            metricKey = metricKey,
                            optionValue = value ?: "",
                            displayLabel = label,
                            updatedAt = System.currentTimeMillis()
                        )
                    )
                }
            )
        }

        if (usesFreeTextInput) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .glassCard()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(metricTitle(metricKey), style = MaterialTheme.typography.titleMedium, color = ZhiQiTokens.TextPrimary)
                OutlinedTextField(
                    value = customValue,
                    onValueChange = {
                        customValue = when (metricKey) {
                            "日记" -> it.take(80)
                            "体温" -> it.filter { ch -> ch.isDigit() || ch == '.' }.take(4)
                            "体重" -> it.filter { ch -> ch.isDigit() || ch == '.' }.take(5)
                            else -> it
                        }
                    },
                    label = {
                        Text(
                            when (metricKey) {
                                "体温" -> "输入体温，例如 36.5"
                                "体重" -> "输入体重，例如 52.3"
                                else -> "输入今天的日记内容"
                            }
                        )
                    },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = if (metricKey == "日记") 4 else 1
                )
            }
        } else if (metricKey != "爱爱") {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .glassCard()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Text(metricTitle(metricKey), style = MaterialTheme.typography.titleMedium, color = ZhiQiTokens.TextPrimary)
                options.chunked(3).forEach { row ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        row.forEach { option ->
                            IndicatorChoice(
                                metricKey = metricKey,
                                option = option,
                                selected = selected == option.value,
                                modifier = Modifier.weight(1f),
                                onClick = {
                                    if (selected == option.value) {
                                        selected = null
                                        selectedLabel = ""
                                    } else {
                                        selected = option.value
                                        selectedLabel = option.label
                                    }
                                }
                            )
                        }
                        repeat(3 - row.size) {
                            Box(modifier = Modifier.weight(1f))
                        }
                    }
                }
            }
        }
    }
}

private fun supportsFreeTextInput(metricKey: String): Boolean {
    return metricKey == "体温" || metricKey == "体重" || metricKey == "日记"
}

@Composable
private fun IndicatorChoice(
    metricKey: String,
    option: IndicatorOption,
    selected: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val visual = indicatorOptionVisual(metricKey = metricKey, optionValue = option.value)
    val activeColor = visual.tint
    Box(
        modifier = modifier
            .background(if (selected) activeColor.copy(alpha = 0.12f) else ZhiQiTokens.Surface, RoundedCornerShape(18.dp))
            .border(1.dp, if (selected) activeColor else ZhiQiTokens.Border, RoundedCornerShape(18.dp))
            .noRippleClickable(onClick)
            .padding(horizontal = 8.dp, vertical = 10.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .background(
                        if (selected) visual.tint.copy(alpha = 0.18f) else ZhiQiTokens.SurfaceSoft,
                        CircleShape
                    )
                    .border(1.dp, if (selected) visual.tint else ZhiQiTokens.Border, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                if (visual.flowLevel != null) {
                    FlowLevelIcon(
                        fillLevel = visual.flowLevel,
                        tint = visual.tint,
                        selected = selected
                    )
                } else {
                    IndicatorGlyphIcon(
                        glyph = visual.glyph,
                        tint = if (selected) visual.tint else visual.tint.copy(alpha = 0.86f),
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
            Text(
                text = option.label,
                color = if (selected) activeColor else ZhiQiTokens.TextSecondary,
                style = MaterialTheme.typography.bodySmall,
                fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium
            )
        }
    }
}

private data class IndicatorOptionVisual(
    val glyph: IndicatorGlyph,
    val tint: Color,
    val flowLevel: Float? = null
)

private enum class IndicatorGlyph {
    CHECK_RING,
    PAIN_LIGHT,
    PAIN_MEDIUM,
    PAIN_SEVERE,
    PAIN_CRAMP,
    PAIN_DULL,
    MOOD_HAPPY,
    MOOD_CALM,
    MOOD_SENSITIVE,
    MOOD_IRRITABLE,
    MOOD_SAD,
    DISCHARGE_CLEAR,
    DISCHARGE_STRETCHY,
    DISCHARGE_MILKY,
    DISCHARGE_YELLOW,
    DISCHARGE_ODOR,
    SLEEP_SHORT,
    SLEEP_MID,
    SLEEP_GOOD,
    SLEEP_LONG,
    SLEEP_POOR,
    SLEEP_BROKEN,
    STOOL_NORMAL,
    STOOL_DRY,
    STOOL_LOOSE,
    STOOL_HARD,
    STOOL_MANY,
    MED_NONE,
    MED_PAIN,
    MED_ANTISPASMODIC,
    MED_HORMONE,
    MED_SUPPLEMENT,
    MED_OTHER,
    DEFAULT
}

private fun indicatorOptionVisual(
    metricKey: String,
    optionValue: String
): IndicatorOptionVisual {
    val primary = ZhiQiTokens.Primary
    val primaryStrong = ZhiQiTokens.PrimaryStrong
    return when (metricKey) {
        "流量" -> IndicatorOptionVisual(
            glyph = IndicatorGlyph.DISCHARGE_CLEAR,
            tint = primaryStrong,
            flowLevel = when (optionValue) {
                "spotting" -> 0.18f
                "light" -> 0.34f
                "medium" -> 0.56f
                "heavy" -> 0.76f
                "very_heavy" -> 0.94f
                else -> 0.4f
            }
        )
        "症状" -> IndicatorOptionVisual(
            glyph = when (optionValue) {
                "none" -> IndicatorGlyph.CHECK_RING
                "light_pain" -> IndicatorGlyph.PAIN_LIGHT
                "moderate_pain" -> IndicatorGlyph.PAIN_MEDIUM
                "severe_pain" -> IndicatorGlyph.PAIN_SEVERE
                "cramp" -> IndicatorGlyph.PAIN_CRAMP
                "dull_ache" -> IndicatorGlyph.PAIN_DULL
                else -> IndicatorGlyph.PAIN_MEDIUM
            },
            tint = primaryStrong
        )
        "心情" -> IndicatorOptionVisual(
            glyph = when (optionValue) {
                "happy" -> IndicatorGlyph.MOOD_HAPPY
                "calm" -> IndicatorGlyph.MOOD_CALM
                "sensitive" -> IndicatorGlyph.MOOD_SENSITIVE
                "irritable" -> IndicatorGlyph.MOOD_IRRITABLE
                "sad" -> IndicatorGlyph.MOOD_SAD
                else -> IndicatorGlyph.MOOD_CALM
            },
            tint = primary
        )
        "白带" -> IndicatorOptionVisual(
            glyph = when (optionValue) {
                "clear" -> IndicatorGlyph.DISCHARGE_CLEAR
                "stretchy" -> IndicatorGlyph.DISCHARGE_STRETCHY
                "milky" -> IndicatorGlyph.DISCHARGE_MILKY
                "yellow" -> IndicatorGlyph.DISCHARGE_YELLOW
                "odor" -> IndicatorGlyph.DISCHARGE_ODOR
                else -> IndicatorGlyph.DISCHARGE_CLEAR
            },
            tint = when (optionValue) {
                "yellow" -> primaryStrong
                "odor" -> primaryStrong.copy(alpha = 0.9f)
                else -> primary
            }
        )
        "好习惯" -> IndicatorOptionVisual(
            glyph = when (optionValue) {
                "sleep_lt5" -> IndicatorGlyph.SLEEP_SHORT
                "sleep_5_6" -> IndicatorGlyph.SLEEP_MID
                "sleep_6_8" -> IndicatorGlyph.SLEEP_GOOD
                "sleep_gt8" -> IndicatorGlyph.SLEEP_LONG
                "sleep_poor" -> IndicatorGlyph.SLEEP_POOR
                "sleep_broken" -> IndicatorGlyph.SLEEP_BROKEN
                else -> IndicatorGlyph.SLEEP_MID
            },
            tint = primary
        )
        "便便" -> IndicatorOptionVisual(
            glyph = when (optionValue) {
                "normal" -> IndicatorGlyph.STOOL_NORMAL
                "dry" -> IndicatorGlyph.STOOL_DRY
                "loose" -> IndicatorGlyph.STOOL_LOOSE
                "hard" -> IndicatorGlyph.STOOL_HARD
                "many" -> IndicatorGlyph.STOOL_MANY
                else -> IndicatorGlyph.STOOL_NORMAL
            },
            tint = primaryStrong
        )
        "计划" -> IndicatorOptionVisual(
            glyph = when (optionValue) {
                "none" -> IndicatorGlyph.MED_NONE
                "painkiller" -> IndicatorGlyph.MED_PAIN
                "antispasmodic" -> IndicatorGlyph.MED_ANTISPASMODIC
                "hormone" -> IndicatorGlyph.MED_HORMONE
                "supplement" -> IndicatorGlyph.MED_SUPPLEMENT
                "other_medicine" -> IndicatorGlyph.MED_OTHER
                else -> IndicatorGlyph.MED_PAIN
            },
            tint = primaryStrong
        )
        else -> IndicatorOptionVisual(
            glyph = IndicatorGlyph.DEFAULT,
            tint = primary
        )
    }
}

@Composable
private fun IndicatorGlyphIcon(
    glyph: IndicatorGlyph,
    tint: Color,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val stroke = Stroke(
            width = size.minDimension * 0.12f,
            cap = StrokeCap.Round,
            join = StrokeJoin.Round
        )
        val thinStroke = Stroke(
            width = size.minDimension * 0.09f,
            cap = StrokeCap.Round,
            join = StrokeJoin.Round
        )

        fun drawFace(mouthStart: Float, mouthSweep: Float, eyebrow: Boolean = false) {
            drawCircle(color = tint, radius = w * 0.38f, center = Offset(w * 0.5f, h * 0.5f), style = stroke)
            drawCircle(color = tint, radius = w * 0.04f, center = Offset(w * 0.38f, h * 0.42f))
            drawCircle(color = tint, radius = w * 0.04f, center = Offset(w * 0.62f, h * 0.42f))
            drawArc(
                color = tint,
                startAngle = mouthStart,
                sweepAngle = mouthSweep,
                useCenter = false,
                topLeft = Offset(w * 0.34f, h * 0.40f),
                size = Size(w * 0.32f, h * 0.32f),
                style = thinStroke
            )
            if (eyebrow) {
                drawLine(color = tint, start = Offset(w * 0.32f, h * 0.31f), end = Offset(w * 0.43f, h * 0.35f), strokeWidth = thinStroke.width)
                drawLine(color = tint, start = Offset(w * 0.68f, h * 0.31f), end = Offset(w * 0.57f, h * 0.35f), strokeWidth = thinStroke.width)
            }
        }

        fun drawDrop(withTail: Boolean = false) {
            val drop = Path().apply {
                moveTo(w * 0.50f, h * 0.12f)
                cubicTo(w * 0.70f, h * 0.30f, w * 0.84f, h * 0.50f, w * 0.50f, h * 0.88f)
                cubicTo(w * 0.16f, h * 0.50f, w * 0.30f, h * 0.30f, w * 0.50f, h * 0.12f)
                close()
            }
            drawPath(path = drop, color = tint, style = stroke)
            if (withTail) {
                drawArc(
                    color = tint,
                    startAngle = 200f,
                    sweepAngle = 120f,
                    useCenter = false,
                    topLeft = Offset(w * 0.18f, h * 0.48f),
                    size = Size(w * 0.62f, h * 0.42f),
                    style = thinStroke
                )
            }
        }

        fun drawBed() {
            drawRoundRect(
                color = tint,
                topLeft = Offset(w * 0.14f, h * 0.50f),
                size = Size(w * 0.72f, h * 0.22f),
                cornerRadius = CornerRadius(w * 0.06f, w * 0.06f),
                style = thinStroke
            )
            drawRoundRect(
                color = tint,
                topLeft = Offset(w * 0.18f, h * 0.36f),
                size = Size(w * 0.22f, h * 0.14f),
                cornerRadius = CornerRadius(w * 0.04f, w * 0.04f),
                style = thinStroke
            )
            drawLine(color = tint, start = Offset(w * 0.14f, h * 0.72f), end = Offset(w * 0.14f, h * 0.86f), strokeWidth = thinStroke.width)
            drawLine(color = tint, start = Offset(w * 0.86f, h * 0.72f), end = Offset(w * 0.86f, h * 0.86f), strokeWidth = thinStroke.width)
        }

        when (glyph) {
            IndicatorGlyph.CHECK_RING -> {
                drawCircle(color = tint, radius = w * 0.36f, center = Offset(w * 0.5f, h * 0.5f), style = stroke)
                drawLine(color = tint, start = Offset(w * 0.34f, h * 0.52f), end = Offset(w * 0.46f, h * 0.64f), strokeWidth = stroke.width)
                drawLine(color = tint, start = Offset(w * 0.46f, h * 0.64f), end = Offset(w * 0.68f, h * 0.38f), strokeWidth = stroke.width)
            }
            IndicatorGlyph.PAIN_LIGHT -> {
                drawCircle(color = tint, radius = w * 0.35f, center = Offset(w * 0.5f, h * 0.5f), style = stroke)
                drawCircle(color = tint, radius = w * 0.05f, center = Offset(w * 0.5f, h * 0.5f))
            }
            IndicatorGlyph.PAIN_MEDIUM -> {
                drawCircle(color = tint, radius = w * 0.35f, center = Offset(w * 0.5f, h * 0.5f), style = stroke)
                drawLine(color = tint, start = Offset(w * 0.34f, h * 0.5f), end = Offset(w * 0.66f, h * 0.5f), strokeWidth = stroke.width)
            }
            IndicatorGlyph.PAIN_SEVERE -> {
                val triangle = Path().apply {
                    moveTo(w * 0.5f, h * 0.16f)
                    lineTo(w * 0.84f, h * 0.78f)
                    lineTo(w * 0.16f, h * 0.78f)
                    close()
                }
                drawPath(path = triangle, color = tint, style = stroke)
                drawLine(color = tint, start = Offset(w * 0.5f, h * 0.38f), end = Offset(w * 0.5f, h * 0.58f), strokeWidth = stroke.width)
                drawCircle(color = tint, radius = w * 0.04f, center = Offset(w * 0.5f, h * 0.68f))
            }
            IndicatorGlyph.PAIN_CRAMP -> {
                val zigzag = Path().apply {
                    moveTo(w * 0.22f, h * 0.56f)
                    lineTo(w * 0.38f, h * 0.34f)
                    lineTo(w * 0.52f, h * 0.62f)
                    lineTo(w * 0.70f, h * 0.40f)
                }
                drawPath(path = zigzag, color = tint, style = stroke)
            }
            IndicatorGlyph.PAIN_DULL -> {
                drawCircle(color = tint, radius = w * 0.35f, center = Offset(w * 0.5f, h * 0.5f), style = stroke)
                drawLine(color = tint, start = Offset(w * 0.5f, h * 0.5f), end = Offset(w * 0.5f, h * 0.34f), strokeWidth = thinStroke.width)
                drawLine(color = tint, start = Offset(w * 0.5f, h * 0.5f), end = Offset(w * 0.63f, h * 0.5f), strokeWidth = thinStroke.width)
            }
            IndicatorGlyph.MOOD_HAPPY -> drawFace(20f, 140f)
            IndicatorGlyph.MOOD_CALM -> drawFace(0f, 180f)
            IndicatorGlyph.MOOD_SENSITIVE -> {
                val heart = Path().apply {
                    moveTo(w * 0.50f, h * 0.80f)
                    cubicTo(w * 0.12f, h * 0.54f, w * 0.20f, h * 0.24f, w * 0.42f, h * 0.34f)
                    cubicTo(w * 0.47f, h * 0.38f, w * 0.53f, h * 0.38f, w * 0.58f, h * 0.34f)
                    cubicTo(w * 0.80f, h * 0.24f, w * 0.88f, h * 0.54f, w * 0.50f, h * 0.80f)
                    close()
                }
                drawPath(path = heart, color = tint, style = stroke)
            }
            IndicatorGlyph.MOOD_IRRITABLE -> drawFace(200f, 140f, eyebrow = true)
            IndicatorGlyph.MOOD_SAD -> drawFace(210f, 120f)
            IndicatorGlyph.DISCHARGE_CLEAR -> drawDrop()
            IndicatorGlyph.DISCHARGE_STRETCHY -> {
                drawDrop(withTail = true)
                drawLine(color = tint, start = Offset(w * 0.20f, h * 0.78f), end = Offset(w * 0.80f, h * 0.78f), strokeWidth = thinStroke.width)
            }
            IndicatorGlyph.DISCHARGE_MILKY -> {
                drawDrop()
                drawCircle(color = tint, radius = w * 0.06f, center = Offset(w * 0.50f, h * 0.58f))
            }
            IndicatorGlyph.DISCHARGE_YELLOW -> {
                drawDrop()
                drawCircle(color = tint, radius = w * 0.05f, center = Offset(w * 0.72f, h * 0.26f))
                drawLine(color = tint, start = Offset(w * 0.72f, h * 0.16f), end = Offset(w * 0.72f, h * 0.10f), strokeWidth = thinStroke.width)
            }
            IndicatorGlyph.DISCHARGE_ODOR -> {
                drawDrop()
                drawLine(color = tint, start = Offset(w * 0.70f, h * 0.26f), end = Offset(w * 0.84f, h * 0.20f), strokeWidth = thinStroke.width)
                drawLine(color = tint, start = Offset(w * 0.70f, h * 0.34f), end = Offset(w * 0.86f, h * 0.34f), strokeWidth = thinStroke.width)
            }
            IndicatorGlyph.SLEEP_SHORT -> {
                drawBed()
                drawLine(color = tint, start = Offset(w * 0.62f, h * 0.22f), end = Offset(w * 0.78f, h * 0.22f), strokeWidth = thinStroke.width)
            }
            IndicatorGlyph.SLEEP_MID -> {
                drawBed()
                drawArc(color = tint, startAngle = 220f, sweepAngle = 220f, useCenter = false, topLeft = Offset(w * 0.60f, h * 0.12f), size = Size(w * 0.18f, h * 0.18f), style = thinStroke)
            }
            IndicatorGlyph.SLEEP_GOOD -> {
                drawBed()
                drawCircle(color = tint, radius = w * 0.04f, center = Offset(w * 0.70f, h * 0.20f))
                drawCircle(color = tint, radius = w * 0.03f, center = Offset(w * 0.78f, h * 0.14f))
            }
            IndicatorGlyph.SLEEP_LONG -> {
                drawBed()
                drawArc(color = tint, startAngle = 0f, sweepAngle = 360f, useCenter = false, topLeft = Offset(w * 0.62f, h * 0.10f), size = Size(w * 0.20f, h * 0.20f), style = thinStroke)
            }
            IndicatorGlyph.SLEEP_POOR -> {
                drawBed()
                drawLine(color = tint, start = Offset(w * 0.60f, h * 0.14f), end = Offset(w * 0.84f, h * 0.30f), strokeWidth = thinStroke.width)
            }
            IndicatorGlyph.SLEEP_BROKEN -> {
                drawBed()
                drawLine(color = tint, start = Offset(w * 0.66f, h * 0.14f), end = Offset(w * 0.74f, h * 0.24f), strokeWidth = thinStroke.width)
                drawLine(color = tint, start = Offset(w * 0.74f, h * 0.14f), end = Offset(w * 0.82f, h * 0.24f), strokeWidth = thinStroke.width)
            }
            IndicatorGlyph.STOOL_NORMAL -> {
                drawCircle(color = tint, radius = w * 0.34f, center = Offset(w * 0.5f, h * 0.5f), style = stroke)
                drawLine(color = tint, start = Offset(w * 0.36f, h * 0.52f), end = Offset(w * 0.46f, h * 0.62f), strokeWidth = thinStroke.width)
                drawLine(color = tint, start = Offset(w * 0.46f, h * 0.62f), end = Offset(w * 0.66f, h * 0.40f), strokeWidth = thinStroke.width)
            }
            IndicatorGlyph.STOOL_DRY -> {
                drawRoundRect(color = tint, topLeft = Offset(w * 0.24f, h * 0.30f), size = Size(w * 0.52f, h * 0.44f), cornerRadius = CornerRadius(w * 0.14f, w * 0.14f), style = stroke)
                drawLine(color = tint, start = Offset(w * 0.34f, h * 0.42f), end = Offset(w * 0.60f, h * 0.66f), strokeWidth = thinStroke.width)
            }
            IndicatorGlyph.STOOL_LOOSE -> {
                drawArc(color = tint, startAngle = 200f, sweepAngle = 140f, useCenter = false, topLeft = Offset(w * 0.22f, h * 0.26f), size = Size(w * 0.56f, h * 0.32f), style = thinStroke)
                drawArc(color = tint, startAngle = 200f, sweepAngle = 140f, useCenter = false, topLeft = Offset(w * 0.22f, h * 0.44f), size = Size(w * 0.56f, h * 0.32f), style = thinStroke)
            }
            IndicatorGlyph.STOOL_HARD -> {
                drawRoundRect(color = tint, topLeft = Offset(w * 0.24f, h * 0.30f), size = Size(w * 0.52f, h * 0.44f), cornerRadius = CornerRadius(w * 0.06f, w * 0.06f), style = stroke)
            }
            IndicatorGlyph.STOOL_MANY -> {
                drawCircle(color = tint, radius = w * 0.09f, center = Offset(w * 0.34f, h * 0.52f))
                drawCircle(color = tint, radius = w * 0.09f, center = Offset(w * 0.50f, h * 0.40f))
                drawCircle(color = tint, radius = w * 0.09f, center = Offset(w * 0.66f, h * 0.52f))
            }
            IndicatorGlyph.MED_NONE -> {
                drawCircle(color = tint, radius = w * 0.34f, center = Offset(w * 0.5f, h * 0.5f), style = stroke)
                drawLine(color = tint, start = Offset(w * 0.34f, h * 0.5f), end = Offset(w * 0.66f, h * 0.5f), strokeWidth = stroke.width)
            }
            IndicatorGlyph.MED_PAIN -> {
                drawRoundRect(color = tint, topLeft = Offset(w * 0.20f, h * 0.34f), size = Size(w * 0.60f, h * 0.30f), cornerRadius = CornerRadius(w * 0.16f, w * 0.16f), style = stroke)
                drawLine(color = tint, start = Offset(w * 0.50f, h * 0.34f), end = Offset(w * 0.50f, h * 0.64f), strokeWidth = thinStroke.width)
            }
            IndicatorGlyph.MED_ANTISPASMODIC -> {
                drawCircle(color = tint, radius = w * 0.34f, center = Offset(w * 0.5f, h * 0.5f), style = stroke)
                drawPath(
                    path = Path().apply {
                        moveTo(w * 0.34f, h * 0.50f)
                        lineTo(w * 0.45f, h * 0.64f)
                        lineTo(w * 0.66f, h * 0.38f)
                    },
                    color = tint,
                    style = thinStroke
                )
            }
            IndicatorGlyph.MED_HORMONE -> {
                drawDrop()
                drawLine(color = tint, start = Offset(w * 0.72f, h * 0.20f), end = Offset(w * 0.72f, h * 0.34f), strokeWidth = thinStroke.width)
                drawLine(color = tint, start = Offset(w * 0.65f, h * 0.27f), end = Offset(w * 0.79f, h * 0.27f), strokeWidth = thinStroke.width)
            }
            IndicatorGlyph.MED_SUPPLEMENT -> {
                drawLine(color = tint, start = Offset(w * 0.50f, h * 0.18f), end = Offset(w * 0.50f, h * 0.74f), strokeWidth = thinStroke.width)
                drawLine(color = tint, start = Offset(w * 0.22f, h * 0.46f), end = Offset(w * 0.78f, h * 0.46f), strokeWidth = thinStroke.width)
                drawCircle(color = tint, radius = w * 0.06f, center = Offset(w * 0.50f, h * 0.46f))
            }
            IndicatorGlyph.MED_OTHER -> {
                drawRoundRect(color = tint, topLeft = Offset(w * 0.24f, h * 0.26f), size = Size(w * 0.52f, h * 0.50f), cornerRadius = CornerRadius(w * 0.08f, w * 0.08f), style = stroke)
                drawLine(color = tint, start = Offset(w * 0.34f, h * 0.46f), end = Offset(w * 0.66f, h * 0.46f), strokeWidth = thinStroke.width)
                drawLine(color = tint, start = Offset(w * 0.50f, h * 0.34f), end = Offset(w * 0.50f, h * 0.58f), strokeWidth = thinStroke.width)
            }
            IndicatorGlyph.DEFAULT -> {
                drawCircle(color = tint, radius = w * 0.34f, center = Offset(w * 0.5f, h * 0.5f), style = stroke)
            }
        }
    }
}

@Composable
private fun FlowLevelIcon(
    fillLevel: Float,
    tint: Color,
    selected: Boolean
) {
    val level = fillLevel.coerceIn(0.08f, 1f)
    Box(
        modifier = Modifier
            .size(width = 18.dp, height = 24.dp)
            .background(Color.White, RoundedCornerShape(6.dp))
            .border(1.dp, Color(0xFFE2D9EA), RoundedCornerShape(6.dp))
    ) {
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth(0.6f)
                .fillMaxHeight(level)
                .background(
                    color = tint.copy(alpha = if (selected) 0.95f else 0.72f),
                    shape = RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp, bottomStart = 4.dp, bottomEnd = 4.dp)
                )
        )
    }
}
