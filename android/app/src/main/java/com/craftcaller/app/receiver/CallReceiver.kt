package com.craftcaller.app.receiver

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.telephony.TelephonyManager
import com.craftcaller.app.MainActivity
import com.craftcaller.app.R
import com.craftcaller.app.aftercall.AfterCallActivity
import com.craftcaller.app.data.local.DatabaseProvider
import com.craftcaller.app.data.local.entity.CallerInfo
import com.craftcaller.app.data.remote.FirebaseRepository
import com.craftcaller.app.service.AnnouncementEngine
import com.craftcaller.app.service.CallerIDService
import com.craftcaller.app.service.CraftCallScreeningService
import com.craftcaller.app.util.Craft
import com.craftcaller.app.util.Prefs
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull

/**
 * Incoming-call flow (no screening role required for basics):
 *  RINGING -> announce (100% local data; only when screening role is NOT held,
 *             because the role keeps the decision + announcement itself),
 *            resolve local Room + async Firebase -> CallerIDService overlay
 *  OFFHOOK -> hide overlay + silence any in-flight announcement
 *  IDLE    -> after-call notification (add name / report spam)
 */
class CallReceiver : BroadcastReceiver() {

    companion object {
        private const val AFTER_CALL_CHANNEL = "after_call_channel"
        private const val AFTER_CALL_ID = 4242
    }

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onReceive(context: Context, intent: Intent) {
        val state = intent.getStringExtra(TelephonyManager.EXTRA_STATE) ?: return
        val prefs = Prefs(context)

        when (state) {
            TelephonyManager.EXTRA_STATE_RINGING -> {
                val raw = intent.getStringExtra(TelephonyManager.EXTRA_INCOMING_NUMBER) ?: return
                val number = Craft.normalizeNumber(raw)
                if (number.isEmpty()) return
                prefs.lastRingingNumber = number
                prefs.lastRingingTime = System.currentTimeMillis()
                prefs.callWasAnswered = false

                val pending = goAsync()
                scope.launch {
                    try {
                        // Voice announcement when the screening role is NOT held:
                        // 100% local resolution (contacts + Room), no network
                        // wait — the announcement speaks as soon as data is ready.
                        if (!CraftCallScreeningService.isRoleHeld(context)) {
                            val cached = runCatching {
                                DatabaseProvider.get(context).callerDao().get(number)
                            }.getOrNull()
                            val displayName = AnnouncementEngine.resolveDisplayNameLocal(context, number, cached)
                            AnnouncementEngine.maybeAnnounce(context, number, displayName, cached?.spamReports ?: 0)
                        }

                        // Overlay (existing flow; may resolve via network async).
                        val info = resolveCaller(context, number)
                        runCatching {
                            context.startForegroundService(
                                Intent(context, CallerIDService::class.java).apply {
                                    action = CallerIDService.ACTION_SHOW
                                    putExtra(CallerIDService.EXTRA_NUMBER, number)
                                    putExtra(CallerIDService.EXTRA_NAME, info.name)
                                    putExtra(CallerIDService.EXTRA_LOCATION, info.location)
                                    putExtra(CallerIDService.EXTRA_IS_SPAM, info.isSpam)
                                    putExtra(CallerIDService.EXTRA_REPORTS, info.spamReports)
                                }
                            )
                        }.onFailure { e ->
                            // Background-FOREGROUND start restrictions (API 31+)
                            // can reject opportunistic starts; overlay lost but
                            // call flow + announcements continue.
                            android.util.Log.w("CallReceiver", "overlay FGS start rejected: $e")
                        }
                    } finally {
                        pending.finish()
                    }
                }
            }

            TelephonyManager.EXTRA_STATE_OFFHOOK -> {
                prefs.callWasAnswered = true
                // Silence any in-flight alert + announcement when answered.
                com.craftcaller.app.audio.AnnouncerAudio.stopAll()
                context.stopService(Intent(context, CallerIDService::class.java))
            }

            TelephonyManager.EXTRA_STATE_IDLE -> {
                context.stopService(Intent(context, CallerIDService::class.java))
                val since = System.currentTimeMillis() - prefs.lastRingingTime
                if (since <= Craft.AFTER_CALL_WINDOW_MS && prefs.lastRingingNumber.isNotEmpty()) {
                    postAfterCallNotification(context, prefs.lastRingingNumber)
                    prefs.lastRingingTime = 0L
                }
            }
        }
    }

    /** Local Room cache -> Firebase community lookup, then persist to Room. */
    private suspend fun resolveCaller(context: Context, number: String): CallerInfo {
        val db = DatabaseProvider.get(context)
        db.callerDao().get(number)?.let { return it }

        val fresh = runCatching { withTimeoutOrNull(5000L) { FirebaseRepository.lookup(number) } }
            .getOrNull()
            ?: return CallerInfo(number = number, name = null, spamReports = 0, source = "none")

        val info = CallerInfo(
            number = number,
            name = fresh.name,
            location = fresh.location,
            spamReports = fresh.spamReports,
            updatedAt = System.currentTimeMillis(),
            source = fresh.source
        )
        db.callerDao().upsert(info)
        return info
    }

    private fun postAfterCallNotification(context: Context, number: String) {
        val manager = context.getSystemService(NotificationManager::class.java)
        val builder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            manager.createNotificationChannel(
                NotificationChannel(
                    AFTER_CALL_CHANNEL,
                    "After call",
                    NotificationManager.IMPORTANCE_HIGH
                )
            )
            Notification.Builder(context, AFTER_CALL_CHANNEL)
        } else {
            @Suppress("DEPRECATION")
            Notification.Builder(context)
        }

        val openApp = PendingIntent.getActivity(
            context, 0,
            Intent(context, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        val openAfterCall = PendingIntent.getActivity(
            context, 1,
            Intent(context, AfterCallActivity::class.java).apply {
                putExtra(AfterCallActivity.EXTRA_NUMBER, number)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            },
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val notification = builder
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle("Who was that call from?")
            .setContentText("Add a name or report spam for ${Craft.prettyNumber(number)}")
            .setContentIntent(openAfterCall)
            .setDeleteIntent(openApp)
            .setAutoCancel(true)
            .build()

        runCatching { manager.notify(AFTER_CALL_ID, notification) }
    }
}
