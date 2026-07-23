package com.notifier.client

import android.content.Context
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey

class AppPrefs(context: Context) {
    private val masterKey = MasterKey.Builder(context)
        .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
        .build()

    private val prefs = EncryptedSharedPreferences.create(
        context,
        "notifier_secure_prefs",
        masterKey,
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM,
    )

    var serverUrl: String?
        get() = prefs.getString(KEY_SERVER_URL, null)
        set(value) = prefs.edit().putString(KEY_SERVER_URL, value).apply()

    var adminToken: String?
        get() = prefs.getString(KEY_ADMIN_TOKEN, null)
        set(value) = prefs.edit().putString(KEY_ADMIN_TOKEN, value).apply()

    fun isConfigured(): Boolean = !serverUrl.isNullOrBlank() && !adminToken.isNullOrBlank()

    companion object {
        private const val KEY_SERVER_URL = "server_url"
        private const val KEY_ADMIN_TOKEN = "admin_token"
    }
}
