package com.zhiqi.app.security

import android.content.Context
import android.util.Base64
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.security.SecureRandom
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec

class PinManager(context: Context) {
    private val appContext = context.applicationContext
    private val prefs by lazy {
        SecurePreferencesFactory.create(appContext, PREFS_NAME)
    }
    private val _pinConfigured = MutableStateFlow(hasStoredPin())
    private val _passwordEnabled = MutableStateFlow(readPasswordEnabled())
    val pinConfigured = _pinConfigured.asStateFlow()
    val passwordEnabled = _passwordEnabled.asStateFlow()

    fun isPinSet(): Boolean = _pinConfigured.value

    fun isPasswordEnabled(): Boolean = _passwordEnabled.value

    fun setPasswordEnabled(enabled: Boolean) {
        prefs.editAndApply {
            putBoolean(KEY_PASSWORD_ENABLED, enabled)
            if (!enabled) {
                putInt(KEY_FAIL_COUNT, 0)
                putBoolean(KEY_HIDDEN, false)
            }
        }
        _passwordEnabled.value = enabled
    }

    fun setPin(pin: String) {
        val salt = ByteArray(16)
        SecureRandom().nextBytes(salt)
        val hash = hashPin(pin, salt)
        prefs.editAndApply {
            putString(KEY_PIN_HASH, Base64.encodeToString(hash, Base64.NO_WRAP))
            putString(KEY_PIN_SALT, Base64.encodeToString(salt, Base64.NO_WRAP))
            putInt(KEY_FAIL_COUNT, 0)
            putBoolean(KEY_HIDDEN, false)
        }
        _pinConfigured.value = hasStoredPin()
    }

    fun verifyPin(pin: String): Boolean {
        val hashB64 = prefs.getString(KEY_PIN_HASH, null) ?: return false
        val saltB64 = prefs.getString(KEY_PIN_SALT, null) ?: return false
        val expected = Base64.decode(hashB64, Base64.NO_WRAP)
        val salt = Base64.decode(saltB64, Base64.NO_WRAP)
        val actual = hashPin(pin, salt)
        val ok = expected.contentEquals(actual)

        if (ok) {
            prefs.editAndApply {
                putInt(KEY_FAIL_COUNT, 0)
                putBoolean(KEY_HIDDEN, false)
            }
        } else {
            val fails = prefs.getInt(KEY_FAIL_COUNT, 0) + 1
            prefs.editAndApply {
                putInt(KEY_FAIL_COUNT, fails)
                if (fails >= MAX_FAILED_ATTEMPTS) {
                    putBoolean(KEY_HIDDEN, true)
                }
            }
        }
        return ok
    }

    fun isHidden(): Boolean = prefs.getBoolean(KEY_HIDDEN, false)

    fun isBiometricEnabled(): Boolean = prefs.getBoolean(KEY_BIOMETRIC, false)

    fun setBiometricEnabled(enabled: Boolean) {
        prefs.editAndApply { putBoolean(KEY_BIOMETRIC, enabled) }
    }

    fun exportSnapshot(): PinSnapshot {
        return PinSnapshot(
            hashBase64 = prefs.getString(KEY_PIN_HASH, null),
            saltBase64 = prefs.getString(KEY_PIN_SALT, null),
            failCount = prefs.getInt(KEY_FAIL_COUNT, 0),
            hidden = prefs.getBoolean(KEY_HIDDEN, false),
            biometricEnabled = prefs.getBoolean(KEY_BIOMETRIC, false),
            passwordEnabled = isPasswordEnabled()
        )
    }

    fun restoreSnapshot(snapshot: PinSnapshot?) {
        if (snapshot == null || snapshot.hashBase64.isNullOrBlank() || snapshot.saltBase64.isNullOrBlank()) {
            clearAll()
            return
        }
        prefs.editAndApply {
            putString(KEY_PIN_HASH, snapshot.hashBase64)
            putString(KEY_PIN_SALT, snapshot.saltBase64)
            putInt(KEY_FAIL_COUNT, snapshot.failCount.coerceAtLeast(0))
            putBoolean(KEY_HIDDEN, snapshot.hidden)
            putBoolean(KEY_BIOMETRIC, snapshot.biometricEnabled)
            putBoolean(KEY_PASSWORD_ENABLED, snapshot.passwordEnabled)
        }
        _pinConfigured.value = hasStoredPin()
        _passwordEnabled.value = snapshot.passwordEnabled
    }

    fun clearAll() {
        prefs.editAndApply { clear() }
        _pinConfigured.value = false
        _passwordEnabled.value = false
    }

    private fun readPasswordEnabled(): Boolean {
        if (!prefs.contains(KEY_PASSWORD_ENABLED) && hasStoredPin()) {
            prefs.editAndApply { putBoolean(KEY_PASSWORD_ENABLED, true) }
            return true
        }
        return prefs.getBoolean(KEY_PASSWORD_ENABLED, false)
    }

    private fun hasStoredPin(): Boolean {
        return prefs.contains(KEY_PIN_HASH) && prefs.contains(KEY_PIN_SALT)
    }

    private fun hashPin(pin: String, salt: ByteArray): ByteArray {
        val spec = PBEKeySpec(pin.toCharArray(), salt, 120_000, 256)
        val skf = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
        return skf.generateSecret(spec).encoded
    }

    companion object {
        private const val PREFS_NAME = "zhiqi_secure"
        private const val KEY_PIN_HASH = "pin_hash"
        private const val KEY_PIN_SALT = "pin_salt"
        private const val KEY_FAIL_COUNT = "pin_fail_count"
        private const val KEY_HIDDEN = "records_hidden"
        private const val KEY_BIOMETRIC = "biometric_enabled"
        private const val KEY_PASSWORD_ENABLED = "password_enabled"
        private const val MAX_FAILED_ATTEMPTS = 5
    }
}

data class PinSnapshot(
    val hashBase64: String?,
    val saltBase64: String?,
    val failCount: Int,
    val hidden: Boolean,
    val biometricEnabled: Boolean,
    val passwordEnabled: Boolean = false
)
