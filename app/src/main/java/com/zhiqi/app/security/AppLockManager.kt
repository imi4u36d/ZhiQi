package com.zhiqi.app.security

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

class AppLockManager(context: Context) {
    private val appContext = context.applicationContext
    private val prefs: SharedPreferences = appContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    private val _isUnlocked = MutableStateFlow(false)
    val isUnlocked = _isUnlocked.asStateFlow()

    fun unlock() {
        setUnlocked(true)
    }

    fun lock() {
        setUnlocked(false)
    }

    fun onAppBackgrounded() {
        prefs.editAndApply {
            putLong(KEY_LAST_BACKGROUND_AT, System.currentTimeMillis())
        }
    }

    fun onAppForegrounded() {
        if (shouldLockAfterBackground()) {
            lock()
        }
    }

    private fun setUnlocked(unlocked: Boolean) {
        _isUnlocked.value = unlocked
    }

    private fun shouldLockAfterBackground(): Boolean {
        val lastBackgroundAt = prefs.getLong(KEY_LAST_BACKGROUND_AT, 0L)
        if (lastBackgroundAt == 0L) return false
        return System.currentTimeMillis() - lastBackgroundAt > LOCK_TIMEOUT_MS
    }

    companion object {
        private const val PREFS_NAME = "zhiqi_lock"
        private const val KEY_LAST_BACKGROUND_AT = "last_bg"
        private const val LOCK_TIMEOUT_MS = 5 * 60 * 1000L
    }
}
