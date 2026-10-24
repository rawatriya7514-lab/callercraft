package com.craftcaller.app.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

/**
 * Boot hook. The app relies on manifest-registered PHONE_STATE broadcasts
 * (exempt from implicit-broadcast restrictions), so nothing critical happens
 * here — we only refresh services bookkeeping after reboots.
 */
class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED) return
        // Reserved for future state restoration. No-op on purpose.
    }
}
