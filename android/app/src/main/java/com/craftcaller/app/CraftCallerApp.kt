package com.craftcaller.app

import android.app.Application
import android.util.Log
import com.craftcaller.app.data.remote.FirebaseRepository
import com.craftcaller.app.util.CrashReporter
import com.craftcaller.app.util.Prefs
import com.craftcaller.app.ui.theme.ThemeState

/**
 * Application class:
 *  - creates the single Prefs instance
 *  - warms up the Firebase SDK (fails silently with placeholder config)
 *  - restores the persisted theme
 */
class CraftCallerApp : Application() {

    lateinit var prefs: Prefs
        private set

    override fun onCreate() {
        super.onCreate()
        instance = this
        CrashReporter.install(this)
        prefs = Prefs(this)

        // Warm Firebase (defensive: placeholder google-services.json still parses).
        runCatching { FirebaseRepository }
            .onFailure { Log.w("CraftCallerApp", "Firebase warmup: ${it.message}") }

        ThemeState.isDark = prefs.darkTheme
    }

    companion object {
        lateinit var instance: CraftCallerApp
    }
}
