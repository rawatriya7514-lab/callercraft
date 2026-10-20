package com.craftcaller.app.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.telephony.TelephonyManager
import com.craftcaller.app.service.CallerIDService
import com.craftcaller.app.util.PhoneUtils

/**
 * Manifest-registered PHONE_STATE receiver (works before the app is ever
 * opened). Two important jobs:
 *  1. start the foreground caller-ID service when the phone starts ringing,
 *  2. if the service is already alive, still forward every ring number to it
 *     (on Android 12+ only broadcast extras reliably carry the number).
 */
class CallReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != TelephonyManager.ACTION_PHONE_STATE_CHANGED) return
        val state = intent.getStringExtra(TelephonyManager.EXTRA_STATE) ?: return
        if (state != TelephonyManager.EXTRA_STATE_RINGING) return
        val raw = intent.getStringExtra(TelephonyManager.EXTRA_INCOMING_NUMBER) ?: return
        val number = PhoneUtils.normalize(raw)
        if (number.isEmpty()) return
        try {
            CallerIDService.pushRing(context, number)
        } catch (_: Throwable) {
            // receiver must never crash the radio path
        }
    }
}
