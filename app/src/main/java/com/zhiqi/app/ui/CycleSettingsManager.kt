package com.zhiqi.app.ui

import android.content.Context
import com.zhiqi.app.widget.CycleWidgetProvider

class CycleSettingsManager(context: Context) {
    private val appContext = context.applicationContext
    private val prefs = appContext.getSharedPreferences("cycle_settings", Context.MODE_PRIVATE)

    fun isConfigured(): Boolean = prefs.getBoolean(KEY_CONFIGURED, false)
    fun cycleLengthDays(): Int = prefs.getInt(KEY_CYCLE_LENGTH, 28)
    fun periodLengthDays(): Int = prefs.getInt(KEY_PERIOD_LENGTH, 5)
    fun lastPeriodStartMillis(): Long = prefs.getLong(KEY_LAST_PERIOD_START, 0L)

    fun setCycleLengthDays(value: Int) {
        prefs.edit().putInt(KEY_CYCLE_LENGTH, value.coerceIn(21, 45)).apply()
        CycleWidgetProvider.refresh(appContext)
    }

    fun setPeriodLengthDays(value: Int) {
        prefs.edit().putInt(KEY_PERIOD_LENGTH, value.coerceIn(2, 10)).apply()
        CycleWidgetProvider.refresh(appContext)
    }

    fun setLastPeriodStartMillis(value: Long) {
        prefs.edit().putLong(KEY_LAST_PERIOD_START, value).apply()
        CycleWidgetProvider.refresh(appContext)
    }

    fun saveAll(cycleLengthDays: Int, periodLengthDays: Int, lastPeriodStartMillis: Long) {
        prefs.edit()
            .putInt(KEY_CYCLE_LENGTH, cycleLengthDays.coerceIn(21, 45))
            .putInt(KEY_PERIOD_LENGTH, periodLengthDays.coerceIn(2, 10))
            .putLong(KEY_LAST_PERIOD_START, lastPeriodStartMillis)
            .putBoolean(KEY_CONFIGURED, true)
            .apply()
        CycleWidgetProvider.refresh(appContext)
    }

    fun exportSnapshot(): CycleSettingsSnapshot {
        return CycleSettingsSnapshot(
            configured = isConfigured(),
            cycleLengthDays = cycleLengthDays(),
            periodLengthDays = periodLengthDays(),
            lastPeriodStartMillis = lastPeriodStartMillis()
        )
    }

    fun restoreSnapshot(snapshot: CycleSettingsSnapshot?) {
        // 空快照表示“未配置”或旧备份缺少周期设置，恢复时要显式清空本地值。
        if (snapshot == null || !snapshot.configured) {
            prefs.edit().clear().apply()
            CycleWidgetProvider.refresh(appContext)
            return
        }
        saveAll(
            cycleLengthDays = snapshot.cycleLengthDays,
            periodLengthDays = snapshot.periodLengthDays,
            lastPeriodStartMillis = snapshot.lastPeriodStartMillis
        )
    }

    companion object {
        private const val KEY_CONFIGURED = "configured"
        private const val KEY_CYCLE_LENGTH = "cycle_length"
        private const val KEY_PERIOD_LENGTH = "period_length"
        private const val KEY_LAST_PERIOD_START = "last_period_start"
    }
}

data class CycleSettingsSnapshot(
    val configured: Boolean,
    val cycleLengthDays: Int,
    val periodLengthDays: Int,
    val lastPeriodStartMillis: Long
)
