package com.zhiqi.app.ui

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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoGraph
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.HealthAndSafety
import androidx.compose.material.icons.filled.Hotel
import androidx.compose.material.icons.filled.InvertColors
import androidx.compose.material.icons.filled.LocalHospital
import androidx.compose.material.icons.filled.Mood
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.SentimentSatisfied
import androidx.compose.material.icons.filled.Thermostat
import androidx.compose.material.icons.filled.ThumbUp
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material3.Icon
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
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
    onCancel: () -> Unit
) {
    val accent = metricAccent(metricKey)
    val options = metricOptions(metricKey)
    var selected by remember(metricKey, initialIndicator?.optionValue) {
        mutableStateOf(initialIndicator?.optionValue ?: options.firstOrNull()?.value)
    }
    var selectedLabel by remember(metricKey, initialIndicator?.displayLabel) {
        mutableStateOf(initialIndicator?.displayLabel ?: options.firstOrNull()?.label ?: "")
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
                    val value = when (metricKey) {
                        "体温", "体重", "日记" -> customValue.trim().takeIf { it.isNotBlank() } ?: return@noRippleClickable
                        else -> selected ?: return@noRippleClickable
                    }
                    val label = when (metricKey) {
                        "体温" -> "${value}°C"
                        "体重" -> "${value} 公斤"
                        "日记" -> value
                        else -> selectedLabel
                    }
                    onSave(
                        DailyIndicatorEntity(
                            dateKey = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date(targetDateMillis)),
                            metricKey = metricKey,
                            optionValue = value,
                            displayLabel = label,
                            updatedAt = System.currentTimeMillis()
                        )
                    )
                }
            )
        }

        if (metricKey == "体温" || metricKey == "体重" || metricKey == "日记") {
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
                                accent = accent,
                                modifier = Modifier.weight(1f),
                                onClick = {
                                    selected = option.value
                                    selectedLabel = option.label
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

@Composable
private fun IndicatorChoice(
    metricKey: String,
    option: IndicatorOption,
    selected: Boolean,
    accent: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val visual = indicatorOptionVisual(metricKey = metricKey, optionValue = option.value, accent = accent)
    Box(
        modifier = modifier
            .background(if (selected) accent.copy(alpha = 0.12f) else ZhiQiTokens.Surface, RoundedCornerShape(18.dp))
            .border(1.dp, if (selected) accent else ZhiQiTokens.Border, RoundedCornerShape(18.dp))
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
                    Icon(
                        imageVector = visual.icon,
                        contentDescription = option.label,
                        tint = if (selected) visual.tint else visual.tint.copy(alpha = 0.78f),
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
            Text(
                text = option.label,
                color = if (selected) accent else ZhiQiTokens.TextSecondary,
                style = MaterialTheme.typography.bodySmall,
                fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium
            )
        }
    }
}

private data class IndicatorOptionVisual(
    val icon: ImageVector,
    val tint: Color,
    val flowLevel: Float? = null
)

private fun indicatorOptionVisual(
    metricKey: String,
    optionValue: String,
    accent: Color
): IndicatorOptionVisual {
    return when (metricKey) {
        "流量" -> IndicatorOptionVisual(
            icon = Icons.Filled.InvertColors,
            tint = Color(0xFFE46E96),
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
            icon = when (optionValue) {
                "none" -> Icons.Filled.ThumbUp
                "light_pain" -> Icons.Filled.HealthAndSafety
                "moderate_pain" -> Icons.Filled.LocalHospital
                "severe_pain" -> Icons.Filled.WarningAmber
                "cramp" -> Icons.Filled.Thermostat
                "dull_ache" -> Icons.Filled.Schedule
                else -> Icons.Filled.HealthAndSafety
            },
            tint = Color(0xFF5FA6D4)
        )
        "心情" -> IndicatorOptionVisual(
            icon = when (optionValue) {
                "happy" -> Icons.Filled.SentimentSatisfied
                "calm" -> Icons.Filled.Mood
                "sensitive" -> Icons.Filled.Favorite
                "irritable" -> Icons.Filled.WarningAmber
                "sad" -> Icons.Filled.Pause
                else -> Icons.Filled.Mood
            },
            tint = Color(0xFFE4B15A)
        )
        "白带" -> IndicatorOptionVisual(
            icon = Icons.Filled.InvertColors,
            tint = when (optionValue) {
                "clear" -> Color(0xFF8BC6F1)
                "stretchy" -> Color(0xFF77AFE7)
                "milky" -> Color(0xFFBFB6D5)
                "yellow" -> Color(0xFFE4BA5A)
                "odor" -> Color(0xFFCC7A72)
                else -> Color(0xFFA488D7)
            }
        )
        "好习惯" -> IndicatorOptionVisual(
            icon = when (optionValue) {
                "sleep_poor" -> Icons.Filled.WarningAmber
                "sleep_broken" -> Icons.Filled.Schedule
                else -> Icons.Filled.Hotel
            },
            tint = Color(0xFF63B2CD)
        )
        "便便" -> IndicatorOptionVisual(
            icon = when (optionValue) {
                "normal" -> Icons.Filled.ThumbUp
                "dry" -> Icons.Filled.Palette
                "loose" -> Icons.Filled.InvertColors
                "hard" -> Icons.Filled.WarningAmber
                "many" -> Icons.Filled.AutoGraph
                else -> Icons.Filled.Palette
            },
            tint = Color(0xFFD3A45E)
        )
        "计划" -> IndicatorOptionVisual(
            icon = when (optionValue) {
                "none" -> Icons.Filled.Pause
                "painkiller" -> Icons.Filled.LocalHospital
                "antispasmodic" -> Icons.Filled.HealthAndSafety
                "hormone" -> Icons.Filled.Thermostat
                "supplement" -> Icons.Filled.ThumbUp
                "other_medicine" -> Icons.Filled.Edit
                else -> Icons.Filled.LocalHospital
            },
            tint = Color(0xFF709FD3)
        )
        else -> IndicatorOptionVisual(
            icon = Icons.Filled.Mood,
            tint = accent
        )
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
