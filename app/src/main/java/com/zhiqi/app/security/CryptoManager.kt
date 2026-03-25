package com.zhiqi.app.security

import android.content.Context
import android.content.SharedPreferences
import android.util.Base64
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import java.security.SecureRandom

class CryptoManager(private val context: Context) {
    private val appContext = context.applicationContext
    private val prefs by lazy {
        SecurePreferencesFactory.create(appContext, PREFS_NAME)
    }

    fun getOrCreateDbPassphrase(): String {
        prefs.getString(KEY_DB_PASSPHRASE, null)?.let { return it }

        val passphrase = generateDbPassphrase()
        prefs.editAndApply {
            putString(KEY_DB_PASSPHRASE, passphrase)
        }
        return passphrase
    }

    fun clearAll() {
        prefs.editAndApply { clear() }
    }

    private fun generateDbPassphrase(): String {
        val random = ByteArray(32)
        SecureRandom().nextBytes(random)
        return Base64.encodeToString(random, Base64.NO_WRAP)
    }

    companion object {
        private const val PREFS_NAME = "zhiqi_secure"
        private const val KEY_DB_PASSPHRASE = "db_passphrase"
    }
}

internal object SecurePreferencesFactory {
    fun create(context: Context, name: String): SharedPreferences {
        val masterKey = MasterKey.Builder(context.applicationContext)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()
        return EncryptedSharedPreferences.create(
            context.applicationContext,
            name,
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
    }
}

internal inline fun SharedPreferences.editAndApply(block: SharedPreferences.Editor.() -> Unit) {
    edit().apply(block).apply()
}
