package com.toobit.platform.data

import android.content.Context
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey

/** Only non-exchange data is stored: server URL, our own refresh token, a UI lock flag. There are NO exchange credentials on the phone. */
interface SecureStore {
    var baseUrl: String?
    var refreshToken: String?
    var biometricRequired: Boolean
    fun clearSession()
}

class EncryptedStore(context: Context) : SecureStore {
    private val prefs = EncryptedSharedPreferences.create(
        context, "secure_prefs",
        MasterKey.Builder(context).setKeyScheme(MasterKey.KeyScheme.AES256_GCM).build(),
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM,
    )
    override var baseUrl: String?
        get() = prefs.getString("base_url", null)
        set(v) = prefs.edit().putString("base_url", v).apply()
    override var refreshToken: String?
        get() = prefs.getString("refresh", null)
        set(v) = prefs.edit().putString("refresh", v).apply()
    override var biometricRequired: Boolean
        get() = prefs.getBoolean("bio", false)
        set(v) = prefs.edit().putBoolean("bio", v).apply()
    override fun clearSession() { prefs.edit().remove("refresh").apply() }
}
