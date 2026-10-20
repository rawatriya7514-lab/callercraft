package com.craftcaller.app.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.craftcaller.app.service.CallerIDService

/** Restart the guard after reboots (RECEIVE_BOOT_COMPLETED). */
class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            CallerIDService.start(context)
        }
    }
}
