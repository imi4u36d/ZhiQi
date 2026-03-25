package com.zhiqi.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoGraph
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.zhiqi.app.data.DatabaseProvider
import com.zhiqi.app.data.DailyIndicatorEntity
import com.zhiqi.app.data.DailyIndicatorRepository
import com.zhiqi.app.data.RecordRepository
import com.zhiqi.app.security.AppLockManager
import com.zhiqi.app.security.PinManager
import kotlinx.coroutines.launch

private const val ROUTE_HOME = "home"
private const val ROUTE_JOURNAL = "journal"
private const val ROUTE_TRENDS = "trends"
private const val ROUTE_ME = "me"

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ZhiQiApp(lockManager: AppLockManager) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val pinManager = remember { PinManager(context) }
    val cycleManager = remember { CycleSettingsManager(context) }
    val onboardingPrefs = remember { context.getSharedPreferences("zhiqi_onboarding", android.content.Context.MODE_PRIVATE) }
    val database = remember { DatabaseProvider.get(context) }
    val repository = remember {
        RecordRepository(database.recordDao())
    }
    val indicatorRepository = remember {
        DailyIndicatorRepository(database.dailyIndicatorDao())
    }
    val allRecords by repository.records().collectAsState(initial = emptyList())
    val allIndicators by indicatorRepository.allIndicators().collectAsState(initial = emptyList())

    var showRecordSheet by remember { mutableStateOf(false) }
    var showIndicatorSheet by remember { mutableStateOf(false) }
    var showLoveRecordPage by remember { mutableStateOf(false) }
    var recordEntryContext by remember { mutableStateOf<String?>(null) }
    var recordEntryDateMillis by remember { mutableStateOf(System.currentTimeMillis()) }
    var editingLoveRecordId by remember { mutableStateOf<Long?>(null) }
    var showDetailSheet by remember { mutableStateOf(false) }
    var detailRecordId by remember { mutableStateOf<Long?>(null) }
    var filterState by remember { mutableStateOf(FilterState()) }
    var showCycleSheet by remember { mutableStateOf(false) }
    var showCycleSavedDialog by remember { mutableStateOf(false) }
    var cycleSettingsVersion by remember { mutableStateOf(0) }
    var showSplash by remember { mutableStateOf(true) }
    var showFirstGuide by remember {
        mutableStateOf(
            !onboardingPrefs.getBoolean("completed", false) && !cycleManager.isConfigured()
        )
    }
    var currentRoute by remember { mutableStateOf(ROUTE_HOME) }
    val isUnlocked by lockManager.isUnlocked.collectAsState()
    val pinConfigured by pinManager.pinConfigured.collectAsState()
    val passwordEnabled by pinManager.passwordEnabled.collectAsState()

    LaunchedEffect(passwordEnabled, pinConfigured) {
        if (!passwordEnabled || !pinConfigured) {
            lockManager.unlock()
        }
    }

    LaunchedEffect(Unit) {
        ReminderScheduler.syncFromPrefs(context)
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        if (showSplash) {
            SplashScreen(onFinished = { showSplash = false })
        } else if (passwordEnabled && pinConfigured && !isUnlocked) {
            UnlockScreen(
                pinManager = pinManager,
                onUnlocked = { lockManager.unlock() }
            )
        } else {
            Scaffold(
                containerColor = Color.Transparent,
                contentWindowInsets = WindowInsets(0, 0, 0, 0),
                bottomBar = {
                    AppBottomBar(
                        currentRoute = currentRoute,
                        onNavigate = { route -> currentRoute = route }
                    )
                }
            ) { innerPadding ->
                Box(modifier = Modifier.padding(innerPadding)) {
                    when (currentRoute) {
                        ROUTE_HOME -> {
                    HomeScreen(
                        repository = repository,
                        indicatorRepository = indicatorRepository,
                        pinManager = pinManager,
                        cycleSettingsVersion = cycleSettingsVersion,
                        filterState = filterState,
                        onAddRecord = { entry ->
                            recordEntryContext = entry
                            recordEntryDateMillis = System.currentTimeMillis()
                            if (entry == "爱爱") {
                                showLoveRecordPage = true
                            } else {
                                showIndicatorSheet = true
                            }
                        },
                        onOpenCycleSettings = { showCycleSheet = true },
                        onOpenTrends = {
                            currentRoute = ROUTE_TRENDS
                        },
                        onOpenJournal = {
                            currentRoute = ROUTE_JOURNAL
                        }
                    )
                        }
                        ROUTE_TRENDS -> {
                        AnalysisScreen(
                            records = allRecords,
                            indicators = allIndicators,
                            onOpenRecordPage = { currentRoute = ROUTE_JOURNAL }
                        )
                        }
                        ROUTE_JOURNAL -> {
                        InsightsScreen(
                            repository = repository,
                            indicatorRepository = indicatorRepository,
                            cycleSettingsVersion = cycleSettingsVersion,
                            onAddRecord = { entry ->
                                recordEntryContext = entry
                                if (entry == "爱爱") {
                                    showLoveRecordPage = true
                                } else {
                                    showIndicatorSheet = true
                                }
                            },
                            onSelectDateForEntry = { dateMillis ->
                                recordEntryDateMillis = dateMillis
                            },
                            onSaveIndicator = { indicator ->
                                indicatorRepository.save(indicator)
                            },
                            onDeletePeriodStatus = { dateMillis ->
                                val key = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault())
                                    .format(java.util.Date(dateMillis))
                                indicatorRepository.deleteByDateAndMetric(key, "月经状态")
                            },
                            onCycleChanged = {
                                cycleSettingsVersion += 1
                            }
                        )
                        }
                        ROUTE_ME -> {
                        MeScreen(
                            repository = repository,
                            indicatorRepository = indicatorRepository,
                            pinManager = pinManager,
                            cycleSettingsVersion = cycleSettingsVersion,
                            onOpenCycleSettings = { showCycleSheet = true }
                        )
                    }
                        else -> Unit
                    }
                }
            }
        }
 
        if (showLoveRecordPage && isUnlocked && !showSplash) {
            LoveRecordListPage(
                targetDateMillis = recordEntryDateMillis,
                records = allRecords,
                onBack = {
                    showLoveRecordPage = false
                    editingLoveRecordId = null
                    recordEntryContext = null
                },
                onAdd = {
                    recordEntryContext = "爱爱"
                    editingLoveRecordId = null
                    showRecordSheet = true
                },
                onEdit = { record ->
                    recordEntryContext = "爱爱"
                    editingLoveRecordId = record.id
                    showRecordSheet = true
                }
            )
        }

        if (showRecordSheet && isUnlocked && !showSplash) {
            ZhiQiModalSheet(
                onDismissRequest = {
                    showRecordSheet = false
                    editingLoveRecordId = null
                }
            ) {
                RecordSheet(
                    initialRecord = editingLoveRecordId?.let { id -> allRecords.firstOrNull { it.id == id } },
                    initialTimeMillis = recordEntryDateMillis,
                    entryContext = recordEntryContext,
                    onSave = { record ->
                        scope.launch {
                            if (record.id == 0L) repository.add(record) else repository.update(record)
                            editingLoveRecordId = null
                            showRecordSheet = false
                        }
                    },
                    onCancel = {
                        editingLoveRecordId = null
                        showRecordSheet = false
                    }
                )
            }
        }

        if (showIndicatorSheet && isUnlocked && !showSplash && recordEntryContext != null) {
            ZhiQiModalSheet(
                onDismissRequest = {
                    recordEntryContext = null
                    showIndicatorSheet = false
                }
            ) {
                IndicatorSheet(
                    metricKey = recordEntryContext!!,
                    targetDateMillis = recordEntryDateMillis,
                    initialIndicator = allIndicators.firstOrNull {
                        it.metricKey == recordEntryContext &&
                            it.dateKey == java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault()).format(java.util.Date(recordEntryDateMillis))
                    },
                    onSave = { indicator ->
                        scope.launch {
                            indicatorRepository.save(indicator)
                            recordEntryContext = null
                            showIndicatorSheet = false
                        }
                    },
                    onClear = { dateKey, metricKey ->
                        scope.launch {
                            indicatorRepository.deleteByDateAndMetric(dateKey, metricKey)
                            recordEntryContext = null
                            showIndicatorSheet = false
                        }
                    },
                    onCancel = {
                        recordEntryContext = null
                        showIndicatorSheet = false
                    }
                )
            }
        }

        if (showCycleSheet && isUnlocked && !showSplash) {
            ZhiQiModalSheet(onDismissRequest = { showCycleSheet = false }) {
                CycleSettingsSheet(
                    onSave = {
                        cycleSettingsVersion += 1
                        showCycleSavedDialog = true
                        showCycleSheet = false
                        onboardingPrefs.edit().putBoolean("completed", true).apply()
                    },
                    onCancel = { showCycleSheet = false }
                )
            }
        }

        if (showFirstGuide && isUnlocked && !showSplash) {
            ZhiQiModalSheet(onDismissRequest = { showFirstGuide = false }) {
                FirstUseGuideSheet(
                    onLater = {
                        onboardingPrefs.edit().putBoolean("completed", true).apply()
                        showFirstGuide = false
                    },
                    onStartSetup = {
                        showFirstGuide = false
                        showCycleSheet = true
                    }
                )
            }
        }

        if (showCycleSavedDialog && isUnlocked && !showSplash) {
            ZhiQiConfirmDialog(
                title = "保存成功",
                message = "生理周期已更新，首页提醒已刷新。",
                onDismissRequest = { showCycleSavedDialog = false },
                onConfirm = { showCycleSavedDialog = false },
                dismissText = null
            )
        }

        if (showDetailSheet && detailRecordId != null && isUnlocked && !showSplash) {
            ZhiQiModalSheet(
                onDismissRequest = {
                    showDetailSheet = false
                    detailRecordId = null
                }
            ) {
                RecordDetailSheet(
                    repository = repository,
                    recordId = detailRecordId!!,
                    onClose = {
                        showDetailSheet = false
                        detailRecordId = null
                    }
                )
            }
        }
    }
}

@Composable
private fun AppBottomBar(
    currentRoute: String,
    onNavigate: (String) -> Unit
) {
    val items = listOf(
        Triple(ROUTE_HOME, "今日", Icons.Filled.Home),
        Triple(ROUTE_JOURNAL, "记录", Icons.Filled.CalendarMonth),
        Triple(ROUTE_TRENDS, "趋势", Icons.Filled.AutoGraph),
        Triple(ROUTE_ME, "我的", Icons.Filled.Person)
    )
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(horizontal = 14.dp, vertical = 8.dp),
        contentAlignment = Alignment.BottomCenter
    ) {
        val barShape = RoundedCornerShape(32.dp)
        Box(
            modifier = Modifier
                .fillMaxWidth(0.94f)
                .widthIn(max = 392.dp)
                .height(74.dp)
                .shadow(
                    elevation = 24.dp,
                    shape = barShape,
                    ambientColor = ZhiQiTokens.Primary.copy(alpha = 0.14f),
                    spotColor = ZhiQiTokens.Secondary.copy(alpha = 0.12f)
                )
                .clip(barShape)
                .background(
                    Brush.verticalGradient(
                        listOf(
                            Color.White.copy(alpha = 0.94f),
                            ZhiQiTokens.Surface.copy(alpha = 0.84f),
                            Color(0xFFFFF4E9).copy(alpha = 0.78f)
                        )
                    )
                )
                .border(1.dp, Color.White.copy(alpha = 0.7f), barShape)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(74.dp)
                    .padding(horizontal = 10.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                items.forEach { (route, label, icon) ->
                    BottomNavItem(
                        route = route,
                        label = label,
                        icon = icon,
                        selected = currentRoute == route,
                        modifier = Modifier.weight(1f),
                        onNavigate = onNavigate
                    )
                }
            }
        }
    }
}

@Composable
private fun BottomNavItem(
    route: String,
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    selected: Boolean,
    modifier: Modifier = Modifier,
    onNavigate: (String) -> Unit
) {
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(22.dp))
            .background(
                if (selected) ZhiQiTokens.PrimarySoft.copy(alpha = 0.88f) else Color.Transparent,
                RoundedCornerShape(22.dp)
            )
            .border(
                width = 1.dp,
                color = if (selected) Color.White.copy(alpha = 0.72f) else Color.Transparent,
                shape = RoundedCornerShape(22.dp)
            )
            .noRippleClickable { onNavigate(route) }
            .padding(horizontal = 10.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = if (selected) ZhiQiTokens.PrimaryStrong else ZhiQiTokens.TextSecondary,
            modifier = Modifier.padding(end = 6.dp)
        )
        Text(
            text = label,
            color = if (selected) ZhiQiTokens.PrimaryStrong else ZhiQiTokens.TextSecondary,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium
        )
    }
}

data class FilterState(
    val types: Set<String> = emptySet(),
    val protections: Set<String> = emptySet()
)

@Composable
private fun FirstUseGuideSheet(
    onLater: () -> Unit,
    onStartSetup: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text("欢迎使用知期", style = MaterialTheme.typography.titleLarge, color = ZhiQiTokens.TextPrimary)
        Text(
            "首次仅需填写 3 项基础信息：最近一次经期开始日、平均周期长度、经期天数。",
            style = MaterialTheme.typography.bodyMedium,
            color = ZhiQiTokens.TextSecondary
        )
        Text(
            "完成后即可获得首页预测、日历阶段着色和提醒建议。",
            style = MaterialTheme.typography.bodyMedium,
            color = ZhiQiTokens.TextSecondary
        )
        Spacer(modifier = Modifier.height(2.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(
                text = "稍后设置",
                color = ZhiQiTokens.TextSecondary,
                modifier = Modifier
                    .weight(1f)
                    .border(1.dp, ZhiQiTokens.Border, RoundedCornerShape(14.dp))
                    .padding(vertical = 12.dp)
                    .noRippleClickable(onLater),
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
            Text(
                text = "立即设置",
                color = Color.White,
                modifier = Modifier
                    .weight(1f)
                    .background(ZhiQiTokens.PrimaryStrong, RoundedCornerShape(14.dp))
                    .padding(vertical = 12.dp)
                    .noRippleClickable(onStartSetup),
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
        }
    }
}
