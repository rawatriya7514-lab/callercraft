package com.craftcaller.app.receiver

import android.os.Build
import android.os.Handler
import android.os.Looper
import android.telecom.CallScreeningService
import android.telecom.Call.Details
import com.craftcaller.app.data.Db
import com.craftcaller.app.data.Prefs
import com.craftcaller.app.service.CallNotification
import com.craftcaller.app.service.CallerIDService
import com.craftcaller.app.util.CallerHub
import com.craftcaller.app.util.PhoneUtils
import java.util.concurrent.Executor
import java.util.concurrent.Executors

/**
 * System-level screening (Android 10+): when the user grants CraftCaller the
 * "Caller ID & spam" role, every ringing call is offered here. CraftCaller
 * still returns data on the regular phone UI for allowed calls, rejects
 * blocked/spam calls via TelecomResponse, and shows a popup for the allowed
 * ones via the always-on service. No call audio or contact payloads leave the
 * device.
 */
class CraftCallScreeningService : CallScreeningService() {

    private val exec: Executor = Executors.newSingleThreadExecutor()

    override fun onScreenCall(callDetails: Details) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) return
        if (callDetails.callDirection != Details.DIRECTION_INCOMING) return
        val raw = callDetails.handle?.schemeSpecificPart ?: ""
        val number = PhoneUtils.normalize(raw)
        if (number.isEmpty()) return

        exec.execute {
            val info = Db.fastLookup(number)
            CallerHub.lastRingingNumber = number
            decide(callDetails, number, info)
        }
    }

    private fun decide(callDetails: Details, number: String, info: com.craftcaller.app.data.CallerInfo) {
        val block = info.blocked || (info.spam && Prefs.autoBlockSpam)
        val ui = Handler(Looper.getMainLooper())
        if (block) {
            respondToCall(
                callDetails,
                CallScreeningService.CallResponse.Builder()
                    .setDisallowCall(true)
                    .setRejectCall(true)
                    .setSkipCallLog(false)
                    .setSkipNotification(true)
                    .build()
            )
            Db.putCall(number, info.displayName, System.currentTimeMillis(), 3, 0, true)
            ui.post { CallNotification.screeningBlocked(this, info) }
        } else {
            respondToCall(
                callDetails,
                CallScreeningService.CallResponse.Builder().setDisallowCall(false).build()
            )
            ui.post {
                if (CallerIDService.running) {
                    CallerIDService.pushRing(this, number)
                }
            }
        }
    }
}
