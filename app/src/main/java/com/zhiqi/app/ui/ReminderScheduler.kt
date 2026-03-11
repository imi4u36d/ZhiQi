package com.zhiqi.app.ui

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.zhiqi.app.data.DatabaseProvider
import com.zhiqi.app.MainActivity
import com.zhiqi.app.R
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import java.util.concurrent.TimeUnit

class ReminderPrefsManager(context: Context) {
    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun isEnabled(): Boolean = prefs.getBoolean(KEY_ENABLED, false)

    fun reminderTime(): String = prefs.getString(KEY_TIME, DEFAULT_TIME) ?: DEFAULT_TIME

    fun hideSensitiveWords(): Boolean = prefs.getBoolean(KEY_HIDE_SENSITIVE, true)

    fun reminderAdvanceDays(): Int = prefs.getInt(KEY_ADVANCE_DAYS, DEFAULT_ADVANCE_DAYS).coerceIn(0, 7)

    fun save(enabled: Boolean, time: String) {
        prefs.edit()
            .putBoolean(KEY_ENABLED, enabled)
            .putString(KEY_TIME, normalizeTime(time))
            .apply()
    }

    fun saveEnabled(enabled: Boolean) {
        save(enabled, reminderTime())
    }

    fun saveTime(time: String) {
        save(isEnabled(), time)
    }

    fun saveSensitiveHidden(hidden: Boolean) {
        prefs.edit().putBoolean(KEY_HIDE_SENSITIVE, hidden).apply()
    }

    fun saveAdvanceDays(days: Int) {
        prefs.edit().putInt(KEY_ADVANCE_DAYS, days.coerceIn(0, 7)).apply()
    }

    private fun normalizeTime(time: String): String {
        val parts = time.split(":")
        val hour = parts.getOrNull(0)?.toIntOrNull()?.coerceIn(0, 23) ?: 21
        val minute = parts.getOrNull(1)?.toIntOrNull()?.coerceIn(0, 59) ?: 0
        return String.format("%02d:%02d", hour, minute)
    }

    companion object {
        private const val PREFS_NAME = "zhiqi_reminder"
        private const val KEY_ENABLED = "enabled"
        private const val KEY_TIME = "time"
        private const val KEY_HIDE_SENSITIVE = "hide_sensitive_words"
        private const val KEY_ADVANCE_DAYS = "advance_days"
        private const val DEFAULT_TIME = "21:00"
        private const val DEFAULT_ADVANCE_DAYS = 3
    }
}

object ReminderScheduler {
    private const val WORK_NAME = "zhiqi_daily_reminder_work"
    private const val CHANNEL_ID = "zhiqi_daily_reminder_channel"
    private const val CHANNEL_NAME = "经期提醒"
    private const val NOTIFICATION_ID = 10086
    private const val DAY_MILLIS = 24L * 60L * 60L * 1000L
    private const val REMINDER_REPEAT_HOURS = 24L
    private const val REMINDER_FLEX_MINUTES = 15L

    fun schedule(context: Context, reminderTime: String) {
        ensureChannel(context)
        val initialDelay = computeInitialDelayMillis(reminderTime)
        val request = PeriodicWorkRequestBuilder<DailyReminderWorker>(
            REMINDER_REPEAT_HOURS,
            TimeUnit.HOURS,
            REMINDER_FLEX_MINUTES,
            TimeUnit.MINUTES
        )
            .setInitialDelay(initialDelay, TimeUnit.MILLISECONDS)
            .build()
        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            WORK_NAME,
            ExistingPeriodicWorkPolicy.CANCEL_AND_REENQUEUE,
            request
        )
    }

    fun cancel(context: Context) {
        WorkManager.getInstance(context).cancelUniqueWork(WORK_NAME)
    }

    fun syncFromPrefs(context: Context) {
        val prefs = ReminderPrefsManager(context)
        if (prefs.isEnabled()) {
            schedule(context, prefs.reminderTime())
        } else {
            cancel(context)
        }
    }

    fun ensureChannel(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager ?: return
        val channel = NotificationChannel(CHANNEL_ID, CHANNEL_NAME, NotificationManager.IMPORTANCE_DEFAULT).apply {
            description = "每日经期提醒"
        }
        manager.createNotificationChannel(channel)
    }

    private fun computeInitialDelayMillis(reminderTime: String): Long {
        val parts = reminderTime.split(":")
        val hour = parts.getOrNull(0)?.toIntOrNull()?.coerceIn(0, 23) ?: 21
        val minute = parts.getOrNull(1)?.toIntOrNull()?.coerceIn(0, 59) ?: 0
        val now = Calendar.getInstance()
        val target = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, minute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
            if (!after(now)) add(Calendar.DAY_OF_MONTH, 1)
        }
        return (target.timeInMillis - now.timeInMillis).coerceAtLeast(1_000L)
    }

    class DailyReminderWorker(
        appContext: Context,
        params: WorkerParameters
    ) : CoroutineWorker(appContext, params) {
        override suspend fun doWork(): Result {
            val prefs = ReminderPrefsManager(applicationContext)
            if (!prefs.isEnabled()) return Result.success()
            if (
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                ContextCompat.checkSelfPermission(
                    applicationContext,
                    Manifest.permission.POST_NOTIFICATIONS
                ) != android.content.pm.PackageManager.PERMISSION_GRANTED
            ) {
                return Result.success()
            }

            ensureChannel(applicationContext)
            val manager = applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
                ?: return Result.retry()
            val daysUntil = computeDaysUntilNextPeriod(applicationContext, System.currentTimeMillis())
            val inAdvanceWindow = daysUntil != null && daysUntil in 0..prefs.reminderAdvanceDays()
            if (!inAdvanceWindow) return Result.success()

            val intent = Intent(applicationContext, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            }
            val pendingIntent = PendingIntent.getActivity(
                applicationContext,
                0,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            val notification = NotificationCompat.Builder(applicationContext, CHANNEL_ID)
                .setSmallIcon(R.mipmap.ic_launcher_default)
                .setContentTitle(if (prefs.hideSensitiveWords()) "知期 · 今日提醒" else "知期 · 经期提醒")
                .setContentText(formatPeriodReminderContent(daysUntil))
                .setAutoCancel(true)
                .setContentIntent(pendingIntent)
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .build()

            manager.notify(NOTIFICATION_ID, notification)
            return Result.success()
        }
    }

    private suspend fun computeDaysUntilNextPeriod(context: Context, nowMillis: Long): Int? {
        val cycleManager = CycleSettingsManager(context)
        if (!cycleManager.isConfigured()) return null
        val anchorStart = resolveReminderAnchorStart(context, cycleManager, nowMillis) ?: return null
        val cycleDays = cycleManager.cycleLengthDays().coerceIn(21, 45)
        val today = startOfDay(nowMillis)
        var nextStart = anchorStart
        var guard = 0
        while (nextStart < today && guard < 240) {
            nextStart += cycleDays * DAY_MILLIS
            guard += 1
        }
        if (guard >= 240) return null
        return ((nextStart - today) / DAY_MILLIS).toInt().coerceAtLeast(0)
    }

    private suspend fun resolveReminderAnchorStart(
        context: Context,
        cycleManager: CycleSettingsManager,
        nowMillis: Long
    ): Long? {
        val configuredStart = cycleManager.lastPeriodStartMillis()
            .takeIf { it > 0L }
            ?.let(::startOfDay)
            ?: 0L
        val today = startOfDay(nowMillis)
        val latestRecordStart = latestPeriodStartFromIndicators(context, today)
        val anchor = maxOf(configuredStart, latestRecordStart)
        return anchor.takeIf { it > 0L }
    }

    private suspend fun latestPeriodStartFromIndicators(context: Context, maxDayMillis: Long): Long {
        val dao = DatabaseProvider.get(context).dailyIndicatorDao()
        return dao.getAll()
            .asSequence()
            .filter { it.metricKey == "月经状态" && it.optionValue == "start" }
            .map { parseDayKey(it.dateKey) }
            .filter { it in 1L..maxDayMillis }
            .maxOrNull() ?: 0L
    }

    private fun parseDayKey(dayKey: String): Long {
        return runCatching {
            SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).parse(dayKey)?.time?.let(::startOfDay) ?: 0L
        }.getOrElse { 0L }
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
}

fun formatPeriodReminderContent(daysUntil: Int?): String {
    return if (daysUntil != null) {
        "月经预计还有${daysUntil}天开始，出门注意带卫生巾。"
    } else {
        "月经预计还有X天开始，出门注意带卫生巾。"
    }
}
