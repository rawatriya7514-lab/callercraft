package com.craftcaller.app.util

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey

/**
 * Encrypted SharedPreferences for personal profile data (name, honorific,
 * announcement language, chosen announcer voice).
 *
 * - Keys and values are encrypted at rest (AES256-SIV keys / AES256-GCM values)
 *   with a device Keystore-backed master key.
 * - If the Keystore is unavailable or corrupted on a device, we degrade
 *   gracefully to a private (MODE_PRIVATE) SharedPreferences file with the same
 *   API so the app keeps working — the fallback file is app-private either way.
 */
object SecurePrefs {

    private const val FILE = "craftcaller_profile_secure"

    @Volatile
    private var cached: SharedPreferences? = null

    /** Single SharedPreferences handle for the process; encrypted when possible. */
    fun get(context: Context): SharedPreferences {
        cached?.let { return it }
        return synchronized(this) {
            cached ?: run {
                val appCtx = context.applicationContext
                val secure = runCatching {
                    val masterKey = MasterKey.Builder(appCtx)
                        .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                        .build()
                    EncryptedSharedPreferences.create(
                        appCtx,
                        FILE,
                        masterKey,
                        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
                    )
                }.getOrNull()
                val prefs = secure ?: appCtx.getSharedPreferences(FILE, Context.MODE_PRIVATE)
                cached = prefs
                prefs
            }
        }
    }

    fun string(context: Context, key: String, def: String = ""): String =
        get(context).getString(key, def) ?: def

    fun putString(context: Context, key: String, value: String) {
        get(context).edit().putString(key, value).apply()
    }

    fun int(context: Context, key: String, def: Int = 0): Int =
        get(context).getInt(key, def)

    fun putInt(context: Context, key: String, value: Int) {
        get(context).edit().putInt(key, value).apply()
    }

    fun bool(context: Context, key: String, def: Boolean = false): Boolean =
        get(context).getBoolean(key, def)

    fun putBool(context: Context, key: String, value: Boolean) {
        get(context).edit().putBoolean(key, value).apply()
    }

    fun float(context: Context, key: String, def: Float = 1f): Float =
        get(context).getFloat(key, def)

    fun putFloat(context: Context, key: String, value: Float) {
        get(context).edit().putFloat(key, value).apply()
    }

    /** Wipes the whole secure profile store (used by "delete all app data"). */
    fun clearAll(context: Context) {
        get(context).edit().clear().apply()
    }
}
