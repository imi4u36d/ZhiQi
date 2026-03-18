package com.zhiqi.app.ui

import android.Manifest
import android.graphics.Color as AndroidColor
import android.graphics.drawable.ColorDrawable
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.widget.EditText
import android.widget.NumberPicker
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.ColumnScope
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.filled.Upload
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.zhiqi.app.R
import com.zhiqi.app.data.RecordRepository
import com.zhiqi.app.data.DailyIndicatorRepository
import com.zhiqi.app.security.CryptoManager
import com.zhiqi.app.security.PinManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MeScreen(
    repository: RecordRepository,
    indicatorRepository: DailyIndicatorRepository,
    pinManager: PinManager,
    cycleSettingsVersion: Int,
    onOpenCycleSettings: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val cycleManager = remember { CycleSettingsManager(context) }
    val reminderPrefs = remember { ReminderPrefsManager(context) }
    val backupManager = remember { BackupManager(context, repository, indicatorRepository, cycleManager, pinManager) }
    val records by repository.records().collectAsState(initial = emptyList())
    val pinConfigured by pinManager.pinConfigured.collectAsState()
    val passwordEnabled by pinManager.passwordEnabled.collectAsState()

    var remind by remember { mutableStateOf(reminderPrefs.isEnabled()) }
    var hideSensitiveWords by remember { mutableStateOf(reminderPrefs.hideSensitiveWords()) }
    var clearStep by remember { mutableStateOf(0) }
    var message by remember { mutableStateOf("") }
    var isWorking by remember { mutableStateOf(false) }
    var pendingImportUri by remember { mutableStateOf<Uri?>(null) }
    var showAlgorithmDialog by remember { mutableStateOf(false) }
    val reminderTime = remember { mutableStateOf(reminderPrefs.reminderTime()) }
    val reminderAdvanceDays = remember { mutableStateOf(reminderPrefs.reminderAdvanceDays()) }

    var showReminderSheet by remember { mutableStateOf(false) }
    var showUnlockSheet by remember { mutableStateOf(false) }

    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            remind = true
            reminderPrefs.saveEnabled(true)
            ReminderScheduler.schedule(context, reminderTime.value)
            message = "经期提醒已开启"
        } else {
            remind = false
            reminderPrefs.saveEnabled(false)
            ReminderScheduler.cancel(context)
            message = "未授予通知权限，提醒未开启"
        }
    }

    val exportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/json")
    ) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        isWorking = true
        scope.launch {
            runCatching { withContext(Dispatchers.IO) { backupManager.exportTo(uri) } }
                .onSuccess { summary ->
                    message = "已导出 ${summary.recordCount} 条记录、${summary.indicatorCount} 条指标"
                }
                .onFailure { error ->
                    message = error.message ?: "导出失败"
                }
            isWorking = false
        }
    }

    val importLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        pendingImportUri = uri
    }

    GlassBackground {
        LazyColumn(
            modifier = Modifier.fillMaxWidth(),
            contentPadding = PaddingValues(bottom = 112.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item {
                ProfileHeader(
                    recordCount = records.size,
                    cycleConfigured = cycleManager.isConfigured(),
                    pinConfigured = pinConfigured && passwordEnabled
                )
            }

            item {
                SectionCard(title = "隐私与提醒") {
                    MenuRow(
                        icon = Icons.Filled.CalendarMonth,
                        title = "生理期设置",
                        subtitle = cycleSummary(cycleManager, cycleSettingsVersion),
                        onClick = onOpenCycleSettings
                    )
                    PasswordToggleRow(
                        enabled = passwordEnabled,
                        pinConfigured = pinConfigured,
                        onToggle = { enabled ->
                            pinManager.setPasswordEnabled(enabled)
                            if (!enabled) {
                                showUnlockSheet = false
                                message = "密码功能已关闭"
                            } else {
                                message = if (pinConfigured) {
                                    "密码功能已开启"
                                } else {
                                    "密码功能已开启，请先设置解锁密码"
                                }
                            }
                        }
                    )
                    MenuRow(
                        icon = Icons.Filled.Lock,
                        title = "解锁方式",
                        subtitle = when {
                            !passwordEnabled -> "密码功能已关闭"
                            pinConfigured -> "已设置数字密码"
                            else -> "未设置密码"
                        },
                        enabled = passwordEnabled,
                        onClick = { showUnlockSheet = true }
                    )
                    ReminderRow(
                        remind = remind,
                        reminderTime = reminderTime.value,
                        reminderAdvanceDays = reminderAdvanceDays.value,
                        onToggle = { enabled ->
                            if (enabled) {
                                val needsPermission =
                                    Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                                        ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
                                if (needsPermission) {
                                    notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                                } else {
                                    remind = true
                                    reminderPrefs.saveEnabled(true)
                                    ReminderScheduler.schedule(context, reminderTime.value)
                                    message = "经期提醒已开启"
                                }
                            } else {
                                remind = false
                                reminderPrefs.saveEnabled(false)
                                ReminderScheduler.cancel(context)
                                message = "经期提醒已关闭"
                            }
                        },
                        onOpenConfig = { showReminderSheet = true }
                    )
                    ReminderPrivacyRow(
                        hidden = hideSensitiveWords,
                        onToggle = { hidden ->
                            hideSensitiveWords = hidden
                            reminderPrefs.saveSensitiveHidden(hidden)
                            message = if (hidden) {
                                "通知将隐藏敏感词"
                            } else {
                                "通知将显示完整提醒内容"
                            }
                        }
                    )
                    MenuRow(
                        icon = Icons.Filled.Storage,
                        title = "算法说明",
                        subtitle = "查看预测依据、误差来源与提升准确度方式",
                        onClick = { showAlgorithmDialog = true }
                    )
                }
            }

            item {
                SectionCard(title = "备份与设备") {
                    MenuRow(
                        icon = Icons.Filled.Person,
                        title = "账号与设备同步",
                        subtitle = "当前为本地模式，后续支持跨设备同步",
                        enabled = false
                    )
                    MenuRow(
                        icon = Icons.Filled.Download,
                        title = "数据导出",
                        subtitle = if (isWorking) "处理中..." else "导出记录、指标和周期设置（不含密码）",
                        enabled = !isWorking,
                        onClick = {
                            exportLauncher.launch(defaultBackupFileName())
                        }
                    )
                    MenuRow(
                        icon = Icons.Filled.Upload,
                        title = "数据导入",
                        subtitle = if (isWorking) "处理中..." else "从备份文件恢复并覆盖本地记录与周期",
                        enabled = !isWorking,
                        onClick = {
                            importLauncher.launch(arrayOf("application/json", "text/plain", "*/*"))
                        }
                    )
                    DataStatusBlock(
                        recordCount = records.size,
                        cycleConfigured = cycleManager.isConfigured(),
                        pinConfigured = pinConfigured,
                        passwordEnabled = passwordEnabled,
                        hideSensitiveWords = hideSensitiveWords
                    )
                }
            }

            item {
                SectionCard(title = "危险操作") {
                    Text(
                        text = "一键清除所有数据",
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { clearStep = 1 }
                            .padding(vertical = 12.dp)
                    )
                }
            }

            if (message.isNotBlank()) {
                item {
                    Text(
                        text = message,
                        color = ZhiQiTokens.TextSecondary,
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(horizontal = 4.dp)
                    )
                }
            }

            item { Spacer(modifier = Modifier.height(12.dp)) }
        }
    }

    if (showAlgorithmDialog) {
        AlertDialog(
            onDismissRequest = { showAlgorithmDialog = false },
            title = { Text("预测依据说明") },
            text = {
                Text(
                    "系统会结合最近经期开始日、周期长度、经期天数和历史记录进行区间预测。\\n\\n" +
                        "当连续漏记或近期波动较大时，预测范围会扩大并提示补充记录。\\n\\n" +
                        "建议连续记录 3 个周期，以获得更稳定的趋势结论。"
                )
            },
            confirmButton = {
                Button(onClick = { showAlgorithmDialog = false }) { Text("我知道了") }
            }
        )
    }

    if (pendingImportUri != null) {
        AlertDialog(
            onDismissRequest = { pendingImportUri = null },
            title = { Text("导入备份") },
            text = { Text("导入会覆盖当前本地记录、指标和周期设置。旧版备份可能同时覆盖密码，是否继续？") },
            confirmButton = {
                Button(
                    onClick = {
                        val uri = pendingImportUri ?: return@Button
                        pendingImportUri = null
                        isWorking = true
                        scope.launch {
                            runCatching { withContext(Dispatchers.IO) { backupManager.importFrom(uri) } }
                                .onSuccess { summary ->
                                    message = "导入完成，恢复 ${summary.recordCount} 条记录、${summary.indicatorCount} 条指标"
                                }
                                .onFailure { error ->
                                    message = error.message ?: "导入失败"
                                }
                            isWorking = false
                        }
                    }
                ) { Text("继续导入") }
            },
            dismissButton = {
                Button(onClick = { pendingImportUri = null }) { Text("取消") }
            }
        )
    }

    if (clearStep in 1..3) {
        val title = when (clearStep) {
            1 -> "确认清除所有数据？"
            2 -> "再次确认，数据不可恢复"
            else -> "最后一次确认，是否继续？"
        }
        AlertDialog(
            onDismissRequest = { clearStep = 0 },
            title = { Text(title) },
            text = { Text("此操作会清除所有记录、密码和设置，且无法恢复。") },
            confirmButton = {
                Button(onClick = {
                    if (clearStep < 3) {
                        clearStep += 1
                    } else {
                        isWorking = true
                        scope.launch {
                            withContext(Dispatchers.IO) {
                                repository.clearAll()
                                indicatorRepository.clearAll()
                                pinManager.clearAll()
                                CryptoManager(context).clearAll()
                                cycleManager.restoreSnapshot(null)
                            }
                            reminderPrefs.save(false, reminderTime.value)
                            reminderPrefs.saveAdvanceDays(3)
                            ReminderScheduler.cancel(context)
                            remind = false
                            clearStep = 0
                            message = "数据已清除"
                            isWorking = false
                        }
                    }
                }) { Text("继续") }
            },
            dismissButton = {
                Button(onClick = { clearStep = 0 }) { Text("取消") }
            }
        )
    }

    if (showUnlockSheet && passwordEnabled) {
        val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        var oldPin by remember { mutableStateOf("") }
        var newPin by remember { mutableStateOf("") }
        var confirmPin by remember { mutableStateOf("") }
        var pinError by remember { mutableStateOf<String?>(null) }

        ModalBottomSheet(
            onDismissRequest = { showUnlockSheet = false },
            sheetState = sheetState,
            containerColor = MaterialTheme.colorScheme.surface
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text("解锁方式", style = MaterialTheme.typography.titleMedium)
                if (pinConfigured) {
                    OutlinedTextField(
                        value = oldPin,
                        onValueChange = { if (it.length <= 6 && it.all(Char::isDigit)) oldPin = it },
                        label = { Text("当前密码") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        visualTransformation = PasswordVisualTransformation()
                    )
                }
                OutlinedTextField(
                    value = newPin,
                    onValueChange = { if (it.length <= 6 && it.all(Char::isDigit)) newPin = it },
                    label = { Text("新密码（4-6位数字）") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    visualTransformation = PasswordVisualTransformation()
                )
                OutlinedTextField(
                    value = confirmPin,
                    onValueChange = { if (it.length <= 6 && it.all(Char::isDigit)) confirmPin = it },
                    label = { Text("确认新密码") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    visualTransformation = PasswordVisualTransformation()
                )

                if (pinError != null) {
                    Text(pinError!!, color = MaterialTheme.colorScheme.error)
                }

                Button(
                    onClick = {
                        if (pinConfigured && !pinManager.verifyPin(oldPin)) {
                            pinError = "当前密码错误"
                            return@Button
                        }
                        if (newPin.length < 4) {
                            pinError = "新密码至少4位"
                            return@Button
                        }
                        if (newPin != confirmPin) {
                            pinError = "两次输入不一致"
                            return@Button
                        }
                        pinManager.setPin(newPin)
                        pinError = null
                        message = "解锁密码已更新"
                        showUnlockSheet = false
                    },
                    modifier = Modifier.fillMaxWidth()
                ) { Text("保存") }
            }
        }
    }

    if (showReminderSheet) {
        val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        ModalBottomSheet(
            onDismissRequest = { showReminderSheet = false },
            sheetState = sheetState,
            containerColor = MaterialTheme.colorScheme.surface
        ) {
            PeriodReminderConfigSheet(
                initialTime = reminderTime.value,
                initialAdvanceDays = reminderAdvanceDays.value,
                cycleManager = cycleManager,
                onSave = { hour, minute, advanceDays ->
                    val formatted = String.format(Locale.getDefault(), "%02d:%02d", hour, minute)
                    reminderTime.value = formatted
                    reminderAdvanceDays.value = advanceDays
                    reminderPrefs.saveTime(formatted)
                    reminderPrefs.saveAdvanceDays(advanceDays)
                    if (remind) {
                        ReminderScheduler.schedule(context, formatted)
                        message = "经期提醒设置已更新"
                    } else {
                        message = "已保存经期提醒参数，开启提醒后生效"
                    }
                    showReminderSheet = false
                },
                onCancel = { showReminderSheet = false }
            )
        }
    }
}

@Composable
private fun ProfileHeader(
    recordCount: Int,
    cycleConfigured: Boolean,
    pinConfigured: Boolean
) {
    val cardShape = RoundedCornerShape(34.dp)
    val brush = Brush.linearGradient(
        listOf(
            ZhiQiTokens.PrimarySoft,
            Color.White,
            ZhiQiTokens.AccentStrongerSoft
        )
    )
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(brush, cardShape)
            .border(1.dp, Color.White.copy(alpha = 0.78f), cardShape)
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.Top
        ) {
            Row(
                modifier = Modifier.weight(1f),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Image(
                    painter = painterResource(id = R.drawable.ill_brand_blossom),
                    contentDescription = "品牌插画",
                    modifier = Modifier.size(58.dp)
                )
                Column {
                    Text(
                        "VAULT MODE",
                        color = ZhiQiTokens.PrimaryStrong,
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        "你的私密花园",
                        color = ZhiQiTokens.TextPrimary,
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        "隐私、提醒与备份都集中在这里，保持轻量，也保持可控。",
                        color = ZhiQiTokens.TextSecondary,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
            Box(
                modifier = Modifier
                    .background(Color.White.copy(alpha = 0.82f), RoundedCornerShape(999.dp))
                    .padding(horizontal = 12.dp, vertical = 6.dp)
            ) {
                Text(
                    text = if (pinConfigured) "已保护" else "待加固",
                    color = ZhiQiTokens.TextPrimary,
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    softWrap = false,
                    overflow = TextOverflow.Clip
                )
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            SummaryPill(label = "记录", value = recordCount.toString(), modifier = Modifier.weight(1f))
            SummaryPill(label = "周期", value = if (cycleConfigured) "已配置" else "未配置", modifier = Modifier.weight(1f))
            SummaryPill(label = "保护", value = if (pinConfigured) "开启" else "关闭", modifier = Modifier.weight(1f))
        }
    }
}

@Composable
private fun SummaryPill(label: String, value: String, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .background(Color.White.copy(alpha = 0.72f), RoundedCornerShape(22.dp))
            .border(1.dp, Color.White.copy(alpha = 0.72f), RoundedCornerShape(22.dp))
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        Text(label, color = ZhiQiTokens.TextSecondary, style = MaterialTheme.typography.labelSmall)
        Text(value, color = ZhiQiTokens.TextPrimary, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
    }
}

@Composable
private fun SectionCard(
    title: String,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .glassCard()
            .padding(18.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
        content = {
            Box(
                modifier = Modifier
                    .background(Color.White.copy(alpha = 0.82f), RoundedCornerShape(999.dp))
                    .padding(horizontal = 12.dp, vertical = 6.dp)
            ) {
                Text(title, style = MaterialTheme.typography.labelMedium, color = ZhiQiTokens.TextPrimary, fontWeight = FontWeight.SemiBold)
            }
            content()
        }
    )
}

@Composable
private fun ReminderRow(
    remind: Boolean,
    reminderTime: String,
    reminderAdvanceDays: Int,
    onToggle: (Boolean) -> Unit,
    onOpenConfig: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(ZhiQiTokens.SurfaceSoft, RoundedCornerShape(20.dp))
            .border(1.dp, ZhiQiTokens.Border, RoundedCornerShape(20.dp))
            .clickable { onOpenConfig() }
            .padding(horizontal = 12.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(38.dp)
                    .background(ZhiQiTokens.AccentStrongerSoft, RoundedCornerShape(14.dp))
                    .border(1.dp, ZhiQiTokens.Border, RoundedCornerShape(14.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Filled.Notifications, contentDescription = "提醒", tint = ZhiQiTokens.Primary)
            }
            Column {
                Text("经期提醒", color = ZhiQiTokens.TextPrimary, style = MaterialTheme.typography.bodyMedium)
                Text(
                    if (remind) "每日 $reminderTime · 提前${reminderAdvanceDays}天" else "当前未开启 · 提前${reminderAdvanceDays}天",
                    color = ZhiQiTokens.TextSecondary,
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = "设置",
                color = ZhiQiTokens.Primary,
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.clickable { onOpenConfig() }
            )
            Switch(checked = remind, onCheckedChange = onToggle)
        }
    }
}

@Composable
private fun PeriodReminderConfigSheet(
    initialTime: String,
    initialAdvanceDays: Int,
    cycleManager: CycleSettingsManager,
    onSave: (hour: Int, minute: Int, advanceDays: Int) -> Unit,
    onCancel: () -> Unit
) {
    val initialHour = remember(initialTime) { initialTime.split(":").getOrNull(0)?.toIntOrNull()?.coerceIn(0, 23) ?: 21 }
    val initialMinute = remember(initialTime) { initialTime.split(":").getOrNull(1)?.toIntOrNull()?.coerceIn(0, 59) ?: 0 }
    var selectedHour by remember(initialHour) { mutableStateOf(initialHour) }
    var selectedMinute by remember(initialMinute) { mutableStateOf(initialMinute) }
    var selectedAdvanceDays by remember(initialAdvanceDays) { mutableStateOf(initialAdvanceDays.coerceIn(0, 7)) }
    val reminderPreview = remember(selectedHour, selectedMinute, selectedAdvanceDays, cycleManager) {
        buildPeriodReminderPreview(
            cycleManager = cycleManager,
            reminderHour = selectedHour,
            reminderMinute = selectedMinute
        )
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
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
                text = "经期提醒",
                style = MaterialTheme.typography.titleLarge,
                color = ZhiQiTokens.TextPrimary
            )
            Text(
                text = "确定",
                style = MaterialTheme.typography.titleMedium,
                color = ZhiQiTokens.Primary,
                modifier = Modifier.noRippleClickable {
                    onSave(selectedHour, selectedMinute, selectedAdvanceDays)
                }
            )
        }

        SectionCard(title = "提醒时间") {
            ReminderTimeWheelPicker(
                selectedHour = selectedHour,
                selectedMinute = selectedMinute,
                onHourChange = { selectedHour = it },
                onMinuteChange = { selectedMinute = it }
            )
        }

        SectionCard(title = "提前提醒") {
            Text(
                text = "月经来前几天开始提醒",
                style = MaterialTheme.typography.bodySmall,
                color = ZhiQiTokens.TextSecondary
            )
            Spacer(modifier = Modifier.height(8.dp))
            ReminderAdvanceDayWheelPicker(
                selectedDays = selectedAdvanceDays,
                onDaysChange = { selectedAdvanceDays = it }
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "提醒内容：$reminderPreview",
                style = MaterialTheme.typography.bodySmall,
                color = ZhiQiTokens.TextSecondary
            )
        }
    }
}

@Composable
private fun ReminderTimeWheelPicker(
    selectedHour: Int,
    selectedMinute: Int,
    onHourChange: (Int) -> Unit,
    onMinuteChange: (Int) -> Unit
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
            ReminderTimeWheelColumn(
                value = selectedHour,
                range = 0..23,
                onValueChange = onHourChange,
                modifier = Modifier.weight(1f)
            )
            Text(
                text = ":",
                style = MaterialTheme.typography.headlineSmall,
                color = ZhiQiTokens.TextPrimary,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(horizontal = 4.dp)
            )
            ReminderTimeWheelColumn(
                value = selectedMinute,
                range = 0..59,
                onValueChange = onMinuteChange,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun ReminderTimeWheelColumn(
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
                stripReminderPickerDecoration(this)
                setFormatter { number -> "%02d".format(number) }
                styleReminderPickerText(this)
            }
        },
        modifier = modifier
            .fillMaxWidth()
            .height(170.dp),
        update = { picker ->
            stripReminderPickerDecoration(picker)
            styleReminderPickerText(picker)
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
        }
    )
}

@Composable
private fun ReminderAdvanceDayWheelPicker(
    selectedDays: Int,
    onDaysChange: (Int) -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(154.dp),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(42.dp)
                .background(Color(0xFFEFF0F3), RoundedCornerShape(12.dp))
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            AndroidView(
                factory = { context ->
                    NumberPicker(context).apply {
                        descendantFocusability = NumberPicker.FOCUS_BLOCK_DESCENDANTS
                        wrapSelectorWheel = true
                        minValue = 0
                        maxValue = 7
                        setBackgroundColor(AndroidColor.TRANSPARENT)
                        stripReminderPickerDecoration(this)
                        setFormatter { number -> number.toString() }
                        styleReminderPickerText(this)
                    }
                },
                modifier = Modifier
                    .size(width = 120.dp, height = 154.dp),
                update = { picker ->
                    stripReminderPickerDecoration(picker)
                    styleReminderPickerText(picker)
                    if (picker.minValue != 0 || picker.maxValue != 7) {
                        picker.minValue = 0
                        picker.maxValue = 7
                    }
                    val normalized = selectedDays.coerceIn(0, 7)
                    if (picker.value != normalized) {
                        picker.value = normalized
                    }
                    picker.setOnValueChangedListener { _, _, newValue ->
                        if (newValue != selectedDays) onDaysChange(newValue.coerceIn(0, 7))
                    }
                }
            )
            Text(
                text = "天",
                style = MaterialTheme.typography.titleMedium,
                color = ZhiQiTokens.TextSecondary,
                modifier = Modifier.padding(start = 8.dp)
            )
        }
    }
}

private fun stripReminderPickerDecoration(picker: NumberPicker) {
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

private fun styleReminderPickerText(picker: NumberPicker) {
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
private fun ReminderPrivacyRow(
    hidden: Boolean,
    onToggle: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(ZhiQiTokens.SurfaceSoft, RoundedCornerShape(20.dp))
            .border(1.dp, ZhiQiTokens.Border, RoundedCornerShape(20.dp))
            .padding(horizontal = 12.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(38.dp)
                    .background(ZhiQiTokens.AccentSoft, RoundedCornerShape(14.dp))
                    .border(1.dp, ZhiQiTokens.Border, RoundedCornerShape(14.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Filled.Notifications, contentDescription = "通知隐私", tint = ZhiQiTokens.Primary)
            }
            Column {
                Text("通知内容隐私", color = ZhiQiTokens.TextPrimary, style = MaterialTheme.typography.bodyMedium)
                Text(
                    if (hidden) "隐藏“经期/排卵”等敏感词" else "显示完整提醒内容",
                    color = ZhiQiTokens.TextSecondary,
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }
        Switch(checked = hidden, onCheckedChange = onToggle)
    }
}

@Composable
private fun PasswordToggleRow(
    enabled: Boolean,
    pinConfigured: Boolean,
    onToggle: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(ZhiQiTokens.SurfaceSoft, RoundedCornerShape(20.dp))
            .border(1.dp, ZhiQiTokens.Border, RoundedCornerShape(20.dp))
            .padding(horizontal = 12.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(38.dp)
                    .background(ZhiQiTokens.PrimarySoft, RoundedCornerShape(14.dp))
                    .border(1.dp, ZhiQiTokens.Border, RoundedCornerShape(14.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Filled.Lock, contentDescription = "密码解锁", tint = ZhiQiTokens.Primary)
            }
            Column {
                Text("密码解锁", color = ZhiQiTokens.TextPrimary, style = MaterialTheme.typography.bodyMedium)
                Text(
                    text = when {
                        !enabled -> "默认关闭"
                        pinConfigured -> "已开启"
                        else -> "已开启，请设置密码"
                    },
                    color = ZhiQiTokens.TextSecondary,
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }
        Switch(checked = enabled, onCheckedChange = onToggle)
    }
}

@Composable
private fun DataStatusBlock(
    recordCount: Int,
    cycleConfigured: Boolean,
    pinConfigured: Boolean,
    passwordEnabled: Boolean,
    hideSensitiveWords: Boolean
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(ZhiQiTokens.SurfaceSoft, RoundedCornerShape(18.dp))
            .border(1.dp, ZhiQiTokens.Border, RoundedCornerShape(18.dp))
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Text("当前数据概况", color = ZhiQiTokens.TextPrimary, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
        Text("记录数：$recordCount", color = ZhiQiTokens.TextSecondary, style = MaterialTheme.typography.bodySmall)
        Text("周期配置：${if (cycleConfigured) "已保存" else "未保存"}", color = ZhiQiTokens.TextSecondary, style = MaterialTheme.typography.bodySmall)
        Text("密码功能：${if (passwordEnabled) "开启" else "关闭"}", color = ZhiQiTokens.TextSecondary, style = MaterialTheme.typography.bodySmall)
        Text("解锁密码：${if (pinConfigured) "已设置" else "未设置"}", color = ZhiQiTokens.TextSecondary, style = MaterialTheme.typography.bodySmall)
        Text("通知隐私：${if (hideSensitiveWords) "隐藏敏感词" else "显示完整内容"}", color = ZhiQiTokens.TextSecondary, style = MaterialTheme.typography.bodySmall)
    }
}

@Composable
private fun MenuRow(
    icon: ImageVector,
    title: String,
    subtitle: String? = null,
    enabled: Boolean = true,
    onClick: (() -> Unit)? = null
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(ZhiQiTokens.SurfaceSoft, RoundedCornerShape(20.dp))
            .border(1.dp, ZhiQiTokens.Border, RoundedCornerShape(20.dp))
            .padding(horizontal = 12.dp, vertical = 12.dp)
            .clickable(enabled = enabled && onClick != null) { onClick?.invoke() },
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(38.dp)
                    .background(ZhiQiTokens.PrimarySoft, RoundedCornerShape(14.dp))
                    .border(1.dp, ZhiQiTokens.Border, RoundedCornerShape(14.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, contentDescription = title, tint = ZhiQiTokens.Primary)
            }
            Column {
                Text(title, color = ZhiQiTokens.TextPrimary, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
                if (!subtitle.isNullOrBlank()) {
                    Text(subtitle, color = ZhiQiTokens.TextSecondary, style = MaterialTheme.typography.bodySmall)
                }
            }
        }
        Icon(
            Icons.Filled.ChevronRight,
            contentDescription = title,
            tint = if (enabled) Color(0xFFB3B8C2) else Color(0xFFD0D4DA)
        )
    }
}

@Suppress("UNUSED_PARAMETER")
private fun cycleSummary(cycleManager: CycleSettingsManager, cycleSettingsVersion: Int): String {
    if (!cycleManager.isConfigured()) return "未设置"
    val lastStart = cycleManager.lastPeriodStartMillis()
    val dateText = if (lastStart > 0L) {
        SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date(lastStart))
    } else {
        "未记录"
    }
    return "${cycleManager.cycleLengthDays()}天周期 · 最近 $dateText"
}

private enum class CycleSettingsEditor {
    LAST_START_DATE,
    CYCLE_LENGTH,
    PERIOD_LENGTH
}

private data class CycleDateParts(
    val year: Int,
    val month: Int,
    val day: Int
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CycleSettingsSheet(
    onSave: () -> Unit,
    onCancel: () -> Unit
) {
    val context = LocalContext.current
    val cycleManager = remember { CycleSettingsManager(context) }
    val initialCycleDays = remember { cycleManager.cycleLengthDays() }
    val initialPeriodDays = remember { cycleManager.periodLengthDays() }
    val initialDateMillis = remember {
        cycleManager.lastPeriodStartMillis().takeIf { it > 0L } ?: System.currentTimeMillis()
    }
    val todayDateParts = remember { cycleDatePartsFromMillis(System.currentTimeMillis()) }
    val initialDateParts = remember {
        normalizeCycleDateParts(cycleDatePartsFromMillis(initialDateMillis), todayDateParts)
    }

    var cycleDays by remember { mutableStateOf(initialCycleDays) }
    var periodDays by remember { mutableStateOf(initialPeriodDays) }
    var selectedDateParts by remember { mutableStateOf(initialDateParts) }
    var expandedEditor by remember { mutableStateOf<CycleSettingsEditor?>(null) }
    val selectedDateMillis = cycleDatePartsToMillis(selectedDateParts)
    val initialDateText = formatCycleDate(initialDateMillis)
    val selectedDateText = formatCycleDate(selectedDateMillis)
    val hasChanges = cycleDays != initialCycleDays ||
        periodDays != initialPeriodDays ||
        selectedDateText != initialDateText

    fun saveCycleSettings() {
        cycleManager.saveAll(
            cycleLengthDays = cycleDays,
            periodLengthDays = periodDays,
            lastPeriodStartMillis = selectedDateMillis
        )
        onSave()
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "取消",
                style = MaterialTheme.typography.titleMedium,
                color = Color(0xFF666D79),
                modifier = Modifier.clickable { onCancel() }.padding(vertical = 6.dp)
            )
            Text("生理周期", style = MaterialTheme.typography.titleMedium, color = ZhiQiTokens.TextPrimary)
            Box(modifier = Modifier.size(40.dp))
        }

        CycleSummaryCard(
            cycleDays = cycleDays,
            periodDays = periodDays,
            lastPeriodStartMillis = selectedDateMillis
        )

        SectionCard(title = "基础设置") {
            CycleSettingRow(
                title = "最近一次开始日",
                value = formatCycleDate(selectedDateMillis),
                onClick = {
                    expandedEditor = if (expandedEditor == CycleSettingsEditor.LAST_START_DATE) {
                        null
                    } else {
                        CycleSettingsEditor.LAST_START_DATE
                    }
                }
            )
            if (expandedEditor == CycleSettingsEditor.LAST_START_DATE) {
                Text(
                    "上下滚动选择年月日",
                    style = MaterialTheme.typography.bodySmall,
                    color = ZhiQiTokens.TextSecondary
                )
                CycleDateWheelPicker(
                    value = selectedDateParts,
                    maxValue = todayDateParts,
                    onChange = { selectedDateParts = it }
                )
            }

            CycleSettingRow(
                title = "周期天数",
                value = "$cycleDays 天",
                onClick = {
                    expandedEditor = if (expandedEditor == CycleSettingsEditor.CYCLE_LENGTH) {
                        null
                    } else {
                        CycleSettingsEditor.CYCLE_LENGTH
                    }
                }
            )
            if (expandedEditor == CycleSettingsEditor.CYCLE_LENGTH) {
                Text(
                    "上下滚动选择 21 到 45 天",
                    style = MaterialTheme.typography.bodySmall,
                    color = ZhiQiTokens.TextSecondary
                )
                CycleDayWheelPicker(
                    value = cycleDays,
                    range = 21..45,
                    onValueChange = { cycleDays = it }
                )
            }

            CycleSettingRow(
                title = "经期天数",
                value = "$periodDays 天",
                onClick = {
                    expandedEditor = if (expandedEditor == CycleSettingsEditor.PERIOD_LENGTH) {
                        null
                    } else {
                        CycleSettingsEditor.PERIOD_LENGTH
                    }
                }
            )
            if (expandedEditor == CycleSettingsEditor.PERIOD_LENGTH) {
                Text(
                    "上下滚动选择 2 到 10 天",
                    style = MaterialTheme.typography.bodySmall,
                    color = ZhiQiTokens.TextSecondary
                )
                CycleDayWheelPicker(
                    value = periodDays,
                    range = 2..10,
                    onValueChange = { periodDays = it }
                )
            }

            Text(
                text = "恢复推荐值",
                color = ZhiQiTokens.Primary,
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable {
                        cycleDays = 28
                        periodDays = 5
                    }
                    .padding(vertical = 10.dp)
            )
        }

        Button(
            onClick = { saveCycleSettings() },
            enabled = hasChanges,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(if (hasChanges) "保存设置" else "已保存")
        }
    }
}

@Composable
private fun CycleSummaryCard(
    cycleDays: Int,
    periodDays: Int,
    lastPeriodStartMillis: Long
) {
    val nextStart = calculateNextCycleStart(lastPeriodStartMillis, cycleDays)
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .glassCard()
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Text(
            text = "${cycleDays}天周期 · 经期${periodDays}天",
            style = MaterialTheme.typography.titleMedium,
            color = ZhiQiTokens.TextPrimary,
            fontWeight = FontWeight.Medium
        )
        Text(
            text = "最近开始：${formatCycleDate(lastPeriodStartMillis)}",
            style = MaterialTheme.typography.bodySmall,
            color = ZhiQiTokens.TextSecondary
        )
        Text(
            text = "下次预计：${formatCycleMonthDay(nextStart)}",
            style = MaterialTheme.typography.bodySmall,
            color = ZhiQiTokens.TextSecondary
        )
    }
}

@Composable
private fun CycleSettingRow(
    title: String,
    value: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(vertical = 10.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(title, style = MaterialTheme.typography.titleMedium, color = ZhiQiTokens.TextPrimary)
        Row(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(value, color = ZhiQiTokens.TextSecondary, style = MaterialTheme.typography.bodyMedium)
            Icon(
                Icons.Filled.ChevronRight,
                contentDescription = title,
                tint = Color(0xFFB3B8C2)
            )
        }
    }
}

@Composable
private fun CycleDateWheelPicker(
    value: CycleDateParts,
    maxValue: CycleDateParts,
    onChange: (CycleDateParts) -> Unit
) {
    val yearRange = 1900..maxValue.year
    val monthMax = if (value.year == maxValue.year) maxValue.month else 12
    val monthRange = 1..monthMax
    val dayMaxByMonth = maxDayOfMonth(value.year, value.month)
    val dayMax = if (value.year == maxValue.year && value.month == maxValue.month) {
        minOf(dayMaxByMonth, maxValue.day)
    } else {
        dayMaxByMonth
    }
    val dayRange = 1..dayMax

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        CycleWheelColumn(
            label = "年",
            value = value.year,
            range = yearRange,
            onValueChange = { year ->
                onChange(normalizeCycleDateParts(value.copy(year = year), maxValue))
            },
            modifier = Modifier.weight(1.5f)
        )
        CycleWheelColumn(
            label = "月",
            value = value.month,
            range = monthRange,
            onValueChange = { month ->
                onChange(normalizeCycleDateParts(value.copy(month = month), maxValue))
            },
            modifier = Modifier.weight(1f)
        )
        CycleWheelColumn(
            label = "日",
            value = value.day,
            range = dayRange,
            onValueChange = { day ->
                onChange(normalizeCycleDateParts(value.copy(day = day), maxValue))
            },
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun CycleWheelColumn(
    label: String,
    value: Int,
    range: IntRange,
    onValueChange: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Text(
            text = label,
            color = ZhiQiTokens.TextSecondary,
            style = MaterialTheme.typography.bodySmall
        )
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
            AndroidView(
                factory = { context ->
                    NumberPicker(context).apply {
                        descendantFocusability = NumberPicker.FOCUS_BLOCK_DESCENDANTS
                        wrapSelectorWheel = false
                        setBackgroundColor(AndroidColor.TRANSPARENT)
                        stripReminderPickerDecoration(this)
                        styleReminderPickerText(this)
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(170.dp),
                update = { picker ->
                    stripReminderPickerDecoration(picker)
                    styleReminderPickerText(picker)
                    val safeRange = if (range.first <= range.last) range else value..value
                    if (picker.minValue != safeRange.first || picker.maxValue != safeRange.last) {
                        picker.minValue = safeRange.first
                        picker.maxValue = safeRange.last
                    }
                    val normalizedValue = value.coerceIn(safeRange.first, safeRange.last)
                    if (picker.value != normalizedValue) {
                        picker.value = normalizedValue
                    }
                    picker.setOnValueChangedListener { _, _, newValue ->
                        if (newValue != value) onValueChange(newValue)
                    }
                }
            )
        }
    }
}

@Composable
private fun CycleDayWheelPicker(
    value: Int,
    range: IntRange,
    onValueChange: (Int) -> Unit
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
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            AndroidView(
                factory = { context ->
                    NumberPicker(context).apply {
                        descendantFocusability = NumberPicker.FOCUS_BLOCK_DESCENDANTS
                        wrapSelectorWheel = false
                        setBackgroundColor(AndroidColor.TRANSPARENT)
                        stripReminderPickerDecoration(this)
                        setFormatter { number -> number.toString() }
                        styleReminderPickerText(this)
                    }
                },
                modifier = Modifier.size(width = 120.dp, height = 170.dp),
                update = { picker ->
                    stripReminderPickerDecoration(picker)
                    styleReminderPickerText(picker)
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
                }
            )
            Text(
                text = "天",
                style = MaterialTheme.typography.titleMedium,
                color = ZhiQiTokens.TextSecondary,
                modifier = Modifier.padding(start = 8.dp)
            )
        }
    }
}

private fun cycleDatePartsFromMillis(timeMillis: Long): CycleDateParts {
    val calendar = Calendar.getInstance().apply { timeInMillis = timeMillis }
    return CycleDateParts(
        year = calendar.get(Calendar.YEAR),
        month = calendar.get(Calendar.MONTH) + 1,
        day = calendar.get(Calendar.DAY_OF_MONTH)
    )
}

private fun cycleDatePartsToMillis(parts: CycleDateParts): Long {
    val calendar = Calendar.getInstance()
    calendar.clear()
    calendar.set(parts.year, parts.month - 1, parts.day, 12, 0, 0)
    calendar.set(Calendar.MILLISECOND, 0)
    return calendar.timeInMillis
}

private fun normalizeCycleDateParts(
    parts: CycleDateParts,
    maxValue: CycleDateParts
): CycleDateParts {
    val year = parts.year.coerceIn(1900, maxValue.year)
    var month = parts.month.coerceIn(1, 12)
    if (year == maxValue.year) {
        month = month.coerceAtMost(maxValue.month)
    }
    var day = parts.day.coerceAtLeast(1)
    day = day.coerceAtMost(maxDayOfMonth(year, month))
    if (year == maxValue.year && month == maxValue.month) {
        day = day.coerceAtMost(maxValue.day)
    }
    return CycleDateParts(year = year, month = month, day = day)
}

private fun maxDayOfMonth(year: Int, month: Int): Int {
    val calendar = Calendar.getInstance()
    calendar.clear()
    calendar.set(year, month - 1, 1)
    return calendar.getActualMaximum(Calendar.DAY_OF_MONTH)
}

private fun formatCycleDate(timeMillis: Long): String {
    return SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date(timeMillis))
}

private fun formatCycleMonthDay(timeMillis: Long): String {
    return SimpleDateFormat("M月d日", Locale.getDefault()).format(Date(timeMillis))
}

private fun calculateNextCycleStart(lastPeriodStartMillis: Long, cycleDays: Int): Long {
    return lastPeriodStartMillis + cycleDays * 24L * 60L * 60L * 1000L
}

private fun buildPeriodReminderPreview(
    cycleManager: CycleSettingsManager,
    reminderHour: Int,
    reminderMinute: Int
): String {
    val referenceMillis = nextReminderReferenceMillis(reminderHour, reminderMinute)
    val daysUntil = calculateDaysUntilNextPeriod(cycleManager, referenceMillis)
    return formatPeriodReminderContent(daysUntil)
}

private fun nextReminderReferenceMillis(hour: Int, minute: Int): Long {
    val now = Calendar.getInstance()
    val target = Calendar.getInstance().apply {
        set(Calendar.HOUR_OF_DAY, hour.coerceIn(0, 23))
        set(Calendar.MINUTE, minute.coerceIn(0, 59))
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
        if (!after(now)) {
            add(Calendar.DAY_OF_MONTH, 1)
        }
    }
    return target.timeInMillis
}

private fun calculateDaysUntilNextPeriod(
    cycleManager: CycleSettingsManager,
    nowMillis: Long
): Int? {
    if (!cycleManager.isConfigured()) return null
    val lastStart = cycleManager.lastPeriodStartMillis()
    if (lastStart <= 0L) return null
    val cycleDays = cycleManager.cycleLengthDays().coerceIn(21, 45)
    val dayMillis = 24L * 60L * 60L * 1000L
    val today = startOfDayMillis(nowMillis)
    var nextStart = startOfDayMillis(lastStart)
    var guard = 0
    while (nextStart < today && guard < 240) {
        nextStart += cycleDays * dayMillis
        guard += 1
    }
    if (guard >= 240) return null
    return ((nextStart - today) / dayMillis).toInt().coerceAtLeast(0)
}

private fun startOfDayMillis(timeMillis: Long): Long {
    return Calendar.getInstance().apply {
        timeInMillis = timeMillis
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }.timeInMillis
}

private fun defaultBackupFileName(): String {
    val time = SimpleDateFormat("yyyyMMdd-HHmmss", Locale.getDefault()).format(Date())
    return "zhiqi-backup-$time.json"
}
