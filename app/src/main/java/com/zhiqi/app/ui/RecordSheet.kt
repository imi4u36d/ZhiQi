package com.zhiqi.app.ui

import android.graphics.Color as AndroidColor
import android.graphics.drawable.ColorDrawable
import android.widget.EditText
import android.widget.NumberPicker
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.viewinterop.AndroidView
import com.zhiqi.app.data.RecordEntity
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

private data class ProtectionUi(
    val value: String,
    val label: String,
    val glyph: ProtectionGlyph
)

private enum class ProtectionGlyph {
    NONE,
    CONDOM,
    WITHDRAWAL,
    NO_EJACULATION,
    EMERGENCY,
    SHORT_TERM,
    LONG_TERM,
    IUD,
    OTHER
}

private val protectionOptions = listOf(
    ProtectionUi("无措施", "无措施", ProtectionGlyph.NONE),
    ProtectionUi("避孕套", "避孕套", ProtectionGlyph.CONDOM),
    ProtectionUi("体外排", "体外排精", ProtectionGlyph.WITHDRAWAL),
    ProtectionUi("未射精", "未射精", ProtectionGlyph.NO_EJACULATION),
    ProtectionUi("紧急避", "紧急避孕药", ProtectionGlyph.EMERGENCY),
    ProtectionUi("短效避", "短效避孕药", ProtectionGlyph.SHORT_TERM),
    ProtectionUi("长效避", "长效避孕药", ProtectionGlyph.LONG_TERM),
    ProtectionUi("节育环", "节育环", ProtectionGlyph.IUD),
    ProtectionUi("其他措", "其他措施", ProtectionGlyph.OTHER)
)

@Composable
fun RecordSheet(
    initialRecord: RecordEntity? = null,
    initialTimeMillis: Long = System.currentTimeMillis(),
    entryContext: String? = null,
    onSave: (RecordEntity) -> Unit,
    onCancel: () -> Unit
) {
    val context = LocalContext.current
    var type by remember { mutableStateOf(initialRecord?.type ?: if (entryContext == "爱爱") "同房" else null) }
    var protections by remember {
        mutableStateOf(
            initialRecord
                ?.protections
                ?.split("|")
                ?.map { normalizeProtectionName(it) }
                ?.filter { it.isNotBlank() }
                ?.toSet()
                ?: emptySet()
        )
    }
    var otherProtection by remember { mutableStateOf(initialRecord?.otherProtection ?: "") }
    var note by remember { mutableStateOf(initialRecord?.note ?: "") }
    var error by remember { mutableStateOf<String?>(null) }
    val initialSelectedTimeMillis = remember(initialRecord?.id, initialTimeMillis, entryContext) {
        when {
            initialRecord != null -> initialRecord.timeMillis
            entryContext == "爱爱" -> System.currentTimeMillis()
            else -> initialTimeMillis
        }
    }
    var selectedTimeMillis by remember(initialSelectedTimeMillis) {
        mutableStateOf(initialSelectedTimeMillis)
    }
    val scrollState = rememberScrollState()
    val entryTitle = entryContext?.let { metricTitle(it) }
    val isLoveEntry = entryContext == "爱爱" || type == "同房"

    LaunchedEffect(isLoveEntry, initialRecord?.id, initialTimeMillis) {
        if (isLoveEntry && initialRecord == null) {
            selectedTimeMillis = alignTimeToDate(selectedTimeMillis, initialTimeMillis)
        }
    }

    val title = when {
        initialRecord != null -> "编辑记录"
        !entryContext.isNullOrBlank() -> "今日${entryTitle}记录"
        else -> "新增记录"
    }

    fun submit() {
        if (type == null) {
            error = "请选择行为类型"
            return
        }
        val finalTimeMillis = if (isLoveEntry && initialRecord == null) {
            alignTimeToDate(selectedTimeMillis, initialTimeMillis)
        } else {
            selectedTimeMillis
        }
        val oneYearAgo = System.currentTimeMillis() - 365L * 24 * 60 * 60 * 1000
        if (finalTimeMillis !in oneYearAgo..System.currentTimeMillis()) {
            error = "记录时间需在最近一年内"
            return
        }
        error = null
        onSave(
            RecordEntity(
                id = initialRecord?.id ?: 0,
                type = type!!,
                protections = protections.joinToString("|"),
                otherProtection = otherProtection.ifBlank { null },
                timeMillis = finalTimeMillis,
                note = if (isLoveEntry) null else note.ifBlank { null }
            )
        )
    }

    Column(
        modifier = Modifier
            .background(MaterialTheme.colorScheme.surface)
            .verticalScroll(scrollState)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "取消",
                style = MaterialTheme.typography.titleMedium,
                color = ZhiQiTokens.TextSecondary,
                modifier = Modifier.noRippleClickable(onCancel)
            )
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge,
                color = ZhiQiTokens.TextPrimary
            )
            Text(
                text = "确定",
                style = MaterialTheme.typography.titleMedium,
                color = ZhiQiTokens.Primary,
                modifier = Modifier.noRippleClickable { submit() }
            )
        }

        if (entryContext != "爱爱") {
            SectionCard(highlighted = false) {
                Text("行为", style = MaterialTheme.typography.titleMedium, color = ZhiQiTokens.TextPrimary)
                Spacer(modifier = Modifier.height(10.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                    SelectButton("爱爱", selected = type == "同房", modifier = Modifier.weight(1f)) { type = "同房" }
                    SelectButton("导管", selected = type == "导管", modifier = Modifier.weight(1f)) { type = "导管" }
                }
            }
        }

        SectionCard(
            highlighted = entryContext == "流量" || entryContext == "颜色" || entryContext == "痛经" || entryContext == "导管"
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text("措施", style = MaterialTheme.typography.titleMedium, color = ZhiQiTokens.TextPrimary)
                Icon(
                    imageVector = Icons.Filled.Info,
                    contentDescription = "措施说明",
                    tint = ZhiQiTokens.TextMuted,
                    modifier = Modifier.size(18.dp)
                )
            }
            Spacer(modifier = Modifier.height(10.dp))
            ProtectionOptionGrid(
                options = protectionOptions,
                selected = protections,
                onToggle = { value ->
                    protections = if (protections.contains(value)) protections - value else protections + value
                }
            )
            if (protections.contains("其他措")) {
                OutlinedTextField(
                    value = otherProtection,
                    onValueChange = { if (it.length <= 10) otherProtection = it },
                    label = { Text("其他说明（10字内）") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }

        SectionCard(highlighted = entryContext == "记录具体时间") {
            Text("时间", style = MaterialTheme.typography.titleMedium, color = ZhiQiTokens.TextPrimary)
            Spacer(modifier = Modifier.height(10.dp))
            if (isLoveEntry) {
                TimeWheelPicker(
                    selectedTimeMillis = selectedTimeMillis,
                    onTimeChange = { hour, minute ->
                        val cal = Calendar.getInstance().apply {
                            timeInMillis = selectedTimeMillis
                            set(Calendar.HOUR_OF_DAY, hour)
                            set(Calendar.MINUTE, minute)
                            set(Calendar.SECOND, 0)
                            set(Calendar.MILLISECOND, 0)
                        }
                        selectedTimeMillis = cal.timeInMillis
                    }
                )
            } else {
                DateTimeCard(
                    selectedTimeMillis = selectedTimeMillis,
                    onPick = {
                        val cal = Calendar.getInstance().apply { timeInMillis = selectedTimeMillis }
                        android.app.DatePickerDialog(
                            context,
                            { _, year, month, day ->
                                cal.set(Calendar.YEAR, year)
                                cal.set(Calendar.MONTH, month)
                                cal.set(Calendar.DAY_OF_MONTH, day)
                                android.app.TimePickerDialog(
                                    context,
                                    { _, hour, minute ->
                                        cal.set(Calendar.HOUR_OF_DAY, hour)
                                        cal.set(Calendar.MINUTE, minute)
                                        cal.set(Calendar.MILLISECOND, 0)
                                        selectedTimeMillis = cal.timeInMillis
                                    },
                                    cal.get(Calendar.HOUR_OF_DAY),
                                    cal.get(Calendar.MINUTE),
                                    true
                                ).show()
                            },
                            cal.get(Calendar.YEAR),
                            cal.get(Calendar.MONTH),
                            cal.get(Calendar.DAY_OF_MONTH)
                        ).show()
                    }
                )
            }
        }

        if (!isLoveEntry) {
            SectionCard(highlighted = entryContext == "日记" || entryContext == "心情" || entryContext == "症状") {
                Text("备注", style = MaterialTheme.typography.titleMedium, color = ZhiQiTokens.TextPrimary)
                Spacer(modifier = Modifier.height(10.dp))
                OutlinedTextField(
                    value = note,
                    onValueChange = { if (it.length <= 10) note = it },
                    label = {
                        Text(
                            when (entryContext) {
                                "心情" -> "记录一下当前情绪"
                                "症状" -> "补充身体感受"
                                else -> "备注（10字内）"
                            }
                        )
                    },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }

        if (error != null) {
            Text(text = error!!, color = MaterialTheme.colorScheme.error)
        }

    }
}

@Composable
private fun ProtectionOptionGrid(
    options: List<ProtectionUi>,
    selected: Set<String>,
    onToggle: (String) -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        options.chunked(PROTECTION_COLUMNS).forEach { rowItems ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.Top
            ) {
                rowItems.forEach { item ->
                    ProtectionOptionChip(
                        item = item,
                        selected = selected.contains(item.value),
                        modifier = Modifier.weight(1f),
                        onClick = { onToggle(item.value) }
                    )
                }
                repeat(PROTECTION_COLUMNS - rowItems.size) {
                    Spacer(modifier = Modifier.weight(1f))
                }
            }
        }
    }
}

private const val PROTECTION_COLUMNS = 4
private val PROTECTION_CARD_HEIGHT = 96.dp

@Composable
private fun TimeWheelPicker(
    selectedTimeMillis: Long,
    onTimeChange: (hour: Int, minute: Int) -> Unit
) {
    val calendar = remember(selectedTimeMillis) {
        Calendar.getInstance().apply { timeInMillis = selectedTimeMillis }
    }
    val hour = calendar.get(Calendar.HOUR_OF_DAY)
    val minute = calendar.get(Calendar.MINUTE)

    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(170.dp),
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(44.dp)
                    .background(Color(0xFFEFF0F3), RoundedCornerShape(12.dp))
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                TimeWheelColumn(
                    value = hour,
                    range = 0..23,
                    onValueChange = { onTimeChange(it, minute) },
                    modifier = Modifier.weight(1f)
                )
                Text(
                    text = ":",
                    style = MaterialTheme.typography.headlineSmall,
                    color = ZhiQiTokens.TextPrimary,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(horizontal = 4.dp)
                )
                TimeWheelColumn(
                    value = minute,
                    range = 0..59,
                    onValueChange = { onTimeChange(hour, it) },
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
private fun TimeWheelColumn(
    value: Int,
    range: IntRange,
    onValueChange: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    AndroidView(
        factory = { context ->
            NumberPicker(context).apply {
                descendantFocusability = NumberPicker.FOCUS_BLOCK_DESCENDANTS
                wrapSelectorWheel = true
                setBackgroundColor(AndroidColor.TRANSPARENT)
                stripPickerDecoration(this)
                setFormatter { number -> "%02d".format(number) }
                stylePickerText(this)
            }
        },
        modifier = modifier
            .fillMaxWidth()
            .height(170.dp),
        update = { picker ->
            stripPickerDecoration(picker)
            stylePickerText(picker)
            val safeRange = if (range.first <= range.last) range else value..value
            if (picker.minValue != safeRange.first || picker.maxValue != safeRange.last) {
                picker.minValue = safeRange.first
                picker.maxValue = safeRange.last
            }
            val normalized = value.coerceIn(safeRange.first, safeRange.last)
            if (picker.value != normalized) {
                picker.value = normalized
            }
            picker.setOnValueChangedListener { _, _, newValue ->
                if (newValue != value) onValueChange(newValue)
            }
        },
    )
}

private fun stripPickerDecoration(picker: NumberPicker) {
    runCatching {
        val dividerField = NumberPicker::class.java.getDeclaredField("mSelectionDivider")
        dividerField.isAccessible = true
        dividerField.set(picker, ColorDrawable(AndroidColor.TRANSPARENT))
    }
    runCatching {
        val dividerHeightField = NumberPicker::class.java.getDeclaredField("mSelectionDividerHeight")
        dividerHeightField.isAccessible = true
        dividerHeightField.setInt(picker, 0)
    }
    runCatching {
        val dividerDistanceField = NumberPicker::class.java.getDeclaredField("mSelectionDividersDistance")
        dividerDistanceField.isAccessible = true
        dividerDistanceField.setInt(picker, 0)
    }
    runCatching {
        val method = NumberPicker::class.java.getDeclaredMethod("setSelectionDividerHeight", Int::class.javaPrimitiveType)
        method.isAccessible = true
        method.invoke(picker, 0)
    }
    picker.invalidate()
}

private fun stylePickerText(picker: NumberPicker) {
    val textColor = AndroidColor.parseColor("#2F2A35")
    runCatching {
        val selectorWheelPaintField = NumberPicker::class.java.getDeclaredField("mSelectorWheelPaint")
        selectorWheelPaintField.isAccessible = true
        val paint = selectorWheelPaintField.get(picker) as android.graphics.Paint
        paint.color = textColor
        paint.textSize = 58f
    }
    runCatching {
        val inputTextField = NumberPicker::class.java.getDeclaredField("mInputText")
        inputTextField.isAccessible = true
        val editText = inputTextField.get(picker) as? EditText
        editText?.setTextColor(textColor)
        editText?.setHintTextColor(textColor)
        editText?.alpha = 1f
    }
    for (index in 0 until picker.childCount) {
        val child = picker.getChildAt(index)
        if (child is EditText) {
            child.setTextColor(textColor)
            child.setHintTextColor(textColor)
            child.alpha = 1f
        }
    }
    picker.invalidate()
}

@Composable
private fun SectionCard(
    highlighted: Boolean,
    content: @Composable () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(ZhiQiTokens.Surface, RoundedCornerShape(20.dp))
            .border(
                width = 1.dp,
                color = if (highlighted) ZhiQiTokens.BorderStrong else ZhiQiTokens.Border,
                shape = RoundedCornerShape(20.dp)
            )
            .padding(14.dp)
    ) {
        content()
    }
}

@Composable
private fun SelectButton(text: String, selected: Boolean, modifier: Modifier = Modifier, onClick: () -> Unit) {
    val bg = if (selected) ZhiQiTokens.PrimaryStrong else ZhiQiTokens.SurfaceSoft
    val fg = if (selected) Color.White else ZhiQiTokens.TextSecondary
    Box(
        modifier = modifier
            .background(bg, RoundedCornerShape(14.dp))
            .noRippleClickable(onClick)
            .padding(vertical = 12.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(text = text, color = fg, fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium)
    }
}

@Composable
private fun DateTimeCard(
    selectedTimeMillis: Long,
    onPick: () -> Unit
) {
    val dateText = SimpleDateFormat("yyyy.MM.dd", Locale.getDefault()).format(Date(selectedTimeMillis))
    val timeText = SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date(selectedTimeMillis))
    Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
        TimeToken(icon = Icons.Filled.CalendarMonth, text = dateText, modifier = Modifier.weight(1f))
        TimeToken(icon = Icons.Filled.AccessTime, text = timeText, modifier = Modifier.weight(1f))
    }
    Spacer(modifier = Modifier.height(10.dp))
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(ZhiQiTokens.AccentSoft, RoundedCornerShape(14.dp))
            .noRippleClickable(onPick)
            .padding(vertical = 12.dp),
        contentAlignment = Alignment.Center
    ) {
        Text("选择日期与时间", color = ZhiQiTokens.Primary, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun TimeToken(
    icon: ImageVector,
    text: String,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .background(ZhiQiTokens.SurfaceSoft, RoundedCornerShape(14.dp))
            .border(1.dp, ZhiQiTokens.Border, RoundedCornerShape(14.dp))
            .padding(horizontal = 12.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Icon(icon, contentDescription = text, tint = ZhiQiTokens.Primary, modifier = Modifier.size(18.dp))
        Text(text, color = ZhiQiTokens.TextPrimary, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun ProtectionOptionChip(
    item: ProtectionUi,
    selected: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val baseTint = ZhiQiTokens.Primary
    val cardBg = if (selected) baseTint.copy(alpha = 0.10f) else ZhiQiTokens.Surface
    val cardBorder = if (selected) baseTint else ZhiQiTokens.Border
    val iconBg = if (selected) baseTint.copy(alpha = 0.16f) else ZhiQiTokens.SurfaceSoft
    val iconTint = baseTint
    val textColor = if (selected) baseTint else ZhiQiTokens.TextSecondary

    Column(
        modifier = modifier
            .height(PROTECTION_CARD_HEIGHT)
            .background(cardBg, RoundedCornerShape(14.dp))
            .border(1.dp, cardBorder, RoundedCornerShape(14.dp))
            .noRippleClickable(onClick)
            .padding(horizontal = 6.dp, vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(7.dp, alignment = Alignment.CenterVertically)
    ) {
        Box(
            modifier = Modifier
                .size(44.dp)
                .background(iconBg, CircleShape)
                .border(1.dp, baseTint.copy(alpha = if (selected) 0.5f else 0.2f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            ProtectionGlyphIcon(
                glyph = item.glyph,
                tint = iconTint,
                modifier = Modifier.size(22.dp)
            )
        }
        Text(
            text = item.label,
            color = textColor,
            style = MaterialTheme.typography.bodySmall,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
            textAlign = TextAlign.Center,
            maxLines = 2
        )
    }
}

@Composable
private fun ProtectionGlyphIcon(
    glyph: ProtectionGlyph,
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
            ProtectionGlyph.NONE -> {
                drawRoundRect(
                    color = tint,
                    topLeft = Offset(w * 0.24f, h * 0.46f),
                    size = Size(w * 0.52f, h * 0.34f),
                    cornerRadius = CornerRadius(w * 0.09f, w * 0.09f),
                    style = stroke
                )
                drawArc(
                    color = tint,
                    startAngle = 215f,
                    sweepAngle = 190f,
                    useCenter = false,
                    topLeft = Offset(w * 0.24f, h * 0.16f),
                    size = Size(w * 0.34f, h * 0.36f),
                    style = stroke
                )
                drawCircle(color = tint, radius = w * 0.04f, center = Offset(w * 0.50f, h * 0.60f))
                drawLine(
                    color = tint,
                    start = Offset(w * 0.50f, h * 0.64f),
                    end = Offset(w * 0.50f, h * 0.72f),
                    strokeWidth = stroke.width,
                    cap = StrokeCap.Round
                )
            }

            ProtectionGlyph.CONDOM -> {
                drawRoundRect(
                    color = tint,
                    topLeft = Offset(w * 0.40f, h * 0.14f),
                    size = Size(w * 0.20f, h * 0.62f),
                    cornerRadius = CornerRadius(w * 0.12f, w * 0.12f),
                    style = stroke
                )
                drawLine(
                    color = tint,
                    start = Offset(w * 0.40f, h * 0.75f),
                    end = Offset(w * 0.60f, h * 0.75f),
                    strokeWidth = stroke.width,
                    cap = StrokeCap.Round
                )
            }

            ProtectionGlyph.WITHDRAWAL -> {
                drawRoundRect(
                    color = tint,
                    topLeft = Offset(w * 0.18f, h * 0.40f),
                    size = Size(w * 0.34f, h * 0.20f),
                    cornerRadius = CornerRadius(w * 0.06f, w * 0.06f),
                    style = stroke
                )
                val nozzle = Path().apply {
                    moveTo(w * 0.50f, h * 0.34f)
                    lineTo(w * 0.78f, h * 0.50f)
                    lineTo(w * 0.50f, h * 0.66f)
                    close()
                }
                drawPath(path = nozzle, color = tint, style = stroke)
                drawCircle(color = tint, radius = w * 0.04f, center = Offset(w * 0.83f, h * 0.40f))
                drawCircle(color = tint, radius = w * 0.03f, center = Offset(w * 0.89f, h * 0.34f))
            }

            ProtectionGlyph.NO_EJACULATION -> {
                drawCircle(
                    color = tint,
                    radius = w * 0.30f,
                    center = Offset(w * 0.50f, h * 0.50f),
                    style = stroke
                )
                drawLine(
                    color = tint,
                    start = Offset(w * 0.30f, h * 0.70f),
                    end = Offset(w * 0.70f, h * 0.30f),
                    strokeWidth = stroke.width,
                    cap = StrokeCap.Round
                )
            }

            ProtectionGlyph.EMERGENCY -> {
                drawRoundRect(
                    color = tint,
                    topLeft = Offset(w * 0.20f, h * 0.30f),
                    size = Size(w * 0.60f, h * 0.34f),
                    cornerRadius = CornerRadius(w * 0.16f, w * 0.16f),
                    style = stroke
                )
                val bolt = Path().apply {
                    moveTo(w * 0.52f, h * 0.24f)
                    lineTo(w * 0.42f, h * 0.52f)
                    lineTo(w * 0.56f, h * 0.52f)
                    lineTo(w * 0.46f, h * 0.78f)
                }
                drawPath(path = bolt, color = tint, style = stroke)
            }

            ProtectionGlyph.SHORT_TERM -> {
                drawCircle(
                    color = tint,
                    radius = w * 0.30f,
                    center = Offset(w * 0.50f, h * 0.50f),
                    style = stroke
                )
                drawCircle(
                    color = tint.copy(alpha = 0.6f),
                    radius = w * 0.40f,
                    center = Offset(w * 0.50f, h * 0.50f),
                    style = Stroke(width = stroke.width * 0.75f, cap = StrokeCap.Round)
                )
                drawLine(
                    color = tint,
                    start = Offset(w * 0.34f, h * 0.50f),
                    end = Offset(w * 0.66f, h * 0.50f),
                    strokeWidth = stroke.width,
                    cap = StrokeCap.Round
                )
            }

            ProtectionGlyph.LONG_TERM -> {
                drawCircle(
                    color = tint.copy(alpha = 0.75f),
                    radius = w * 0.32f,
                    center = Offset(w * 0.50f, h * 0.50f),
                    style = stroke
                )
                drawArc(
                    color = tint,
                    startAngle = 28f,
                    sweepAngle = 128f,
                    useCenter = false,
                    topLeft = Offset(w * 0.18f, h * 0.18f),
                    size = Size(w * 0.64f, h * 0.64f),
                    style = stroke
                )
                drawArc(
                    color = tint,
                    startAngle = 208f,
                    sweepAngle = 128f,
                    useCenter = false,
                    topLeft = Offset(w * 0.18f, h * 0.18f),
                    size = Size(w * 0.64f, h * 0.64f),
                    style = stroke
                )
            }

            ProtectionGlyph.IUD -> {
                drawLine(
                    color = tint,
                    start = Offset(w * 0.26f, h * 0.30f),
                    end = Offset(w * 0.74f, h * 0.30f),
                    strokeWidth = stroke.width,
                    cap = StrokeCap.Round
                )
                drawLine(
                    color = tint,
                    start = Offset(w * 0.50f, h * 0.30f),
                    end = Offset(w * 0.50f, h * 0.74f),
                    strokeWidth = stroke.width,
                    cap = StrokeCap.Round
                )
                drawLine(
                    color = tint,
                    start = Offset(w * 0.50f, h * 0.74f),
                    end = Offset(w * 0.40f, h * 0.88f),
                    strokeWidth = stroke.width * 0.8f,
                    cap = StrokeCap.Round
                )
                drawLine(
                    color = tint,
                    start = Offset(w * 0.50f, h * 0.74f),
                    end = Offset(w * 0.60f, h * 0.88f),
                    strokeWidth = stroke.width * 0.8f,
                    cap = StrokeCap.Round
                )
            }

            ProtectionGlyph.OTHER -> {
                drawArc(
                    color = tint,
                    startAngle = 180f,
                    sweepAngle = 180f,
                    useCenter = false,
                    topLeft = Offset(w * 0.18f, h * 0.22f),
                    size = Size(w * 0.64f, h * 0.42f),
                    style = stroke
                )
                drawLine(
                    color = tint,
                    start = Offset(w * 0.50f, h * 0.43f),
                    end = Offset(w * 0.50f, h * 0.80f),
                    strokeWidth = stroke.width,
                    cap = StrokeCap.Round
                )
                drawArc(
                    color = tint,
                    startAngle = 0f,
                    sweepAngle = 170f,
                    useCenter = false,
                    topLeft = Offset(w * 0.46f, h * 0.70f),
                    size = Size(w * 0.16f, h * 0.16f),
                    style = stroke
                )
            }
        }
    }
}

private fun normalizeProtectionName(name: String): String {
    return when (name.trim()) {
        "无防护", "无措施" -> "无措施"
        "短效避孕药", "短效避", "短效避孕" -> "短效避"
        "长效避孕", "长效避孕药", "长效避" -> "长效避"
        "体外", "体外排精", "体外排" -> "体外排"
        "紧急避孕药", "紧急避" -> "紧急避"
        "其他", "其他措施", "其他措" -> "其他措"
        else -> name.trim()
    }
}

private fun alignTimeToDate(sourceMillis: Long, targetDateMillis: Long): Long {
    val source = Calendar.getInstance().apply { timeInMillis = sourceMillis }
    val target = Calendar.getInstance().apply {
        timeInMillis = targetDateMillis
        set(Calendar.HOUR_OF_DAY, source.get(Calendar.HOUR_OF_DAY))
        set(Calendar.MINUTE, source.get(Calendar.MINUTE))
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }
    return target.timeInMillis
}
