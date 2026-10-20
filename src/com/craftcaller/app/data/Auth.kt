package com.craftcaller.app.data

import android.content.Context
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions

/**
 * Authentication layer.
 *
 * When a Firebase project is attached (assets/firebase_config.json provides
 * api_key + project_id), CraftCaller initializes the real Firebase App so the
 * community directory (Retrofit REST clients) talks to live Firestore.
 *
 * The login screen then runs the Truecaller-style phone-number+OTP flow; until
 * cloud OTP providers are configured, OTP works in "local verification" mode
 * (code displayed on the request screen) so the app is fully usable with no
 * console setup. Profile, contact sync and caller-ID data flow identically in
 * both modes.
 */
object Auth {
    enum class Mode { LOCAL, FIREBASE }

    @Volatile var mode: Mode = Mode.LOCAL
        private set

    fun init(ctx: Context): Mode {
        val cfg = FireConfig.load(ctx) ?: return Mode.LOCAL
        return try {
            if (FirebaseApp.getApps(ctx).isEmpty()) {
                val options = FirebaseOptions.Builder()
                    .setApplicationId("1:123456789000:android:6f2f2b6d94b14c8c")
                    .setApiKey(cfg.apiKey)
                    .setProjectId(cfg.projectId)
                    .build()
                FirebaseApp.initializeApp(ctx, options)
            }
            FirebaseApp.getInstance()
            mode = Mode.FIREBASE
            Mode.FIREBASE
        } catch (_: Throwable) {
            mode = Mode.LOCAL
            Mode.LOCAL
        }
    }

    /** Truecaller-style six-digit code; pending code kept until verified. */
    fun savePendingCode(phone: String, code: String) {
        Prefs.pendingCode = code
    }
}
