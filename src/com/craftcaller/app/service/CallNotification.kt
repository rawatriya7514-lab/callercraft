package com.craftcaller.app.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.craftcaller.app.CallCardActivity
import com.craftcaller.app.MainActivity
import com.craftcaller.app.R
import com.craftcaller.app.data.CallerInfo
import com.craftcaller.app.data.Cloud
import com.craftcaller.app.data.Prefs
import com.craftcaller.app.util.Perms
import com.craftcaller.app.util.PhoneUtils

/** Every notification and channel CraftCaller uses. */
object CallNotification {
    const val CH_IDLE = "caller_id_status"
    const val CH_RING = "incoming_calls"
    const val CH_ALERTS = "call_alerts"
    const val FG_ID = 7001
    const val KRingId = 7100
    const val KAfterId = 7110

    fun channels(ctx: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val nm = ctx.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val idle = NotificationChannel(
            CH_IDLE, "Caller ID guard", NotificationManager.IMPORTANCE_MIN
        ).apply {
            description = "Persistent status while CraftCaller identifies calls"
            setShowBadge(false)
        }
        val ring = NotificationChannel(
            CH_RING, "Incoming calls", NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = "Caller ID for ringing calls"
            setBypassDnd(false)
            lockscreenVisibility = Notification.VISIBILITY_PUBLIC
        }
        val alerts = NotificationChannel(
            CH_ALERTS, "Call & spam alerts", NotificationManager.IMPORTANCE_DEFAULT
        )
        nm.createNotificationChannels(listOf(idle, ring, alerts))
    }

    private fun smallIcon() = R.drawable.ic_stat_callerid

    fun idleNotification(ctx: Context): Notification {
        val open = Intent(ctx, MainActivity::class.java)
        val pi = PendingIntent.getActivity(ctx, 2, open,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
        return NotificationCompat.Builder(ctx, CH_IDLE)
            .setSmallIcon(smallIcon())
            .setContentTitle("CraftCaller is protecting you")
            .setContentText("Caller ID · spam defence · ${Cloud.repo.label}")
            .setOngoing(true)
            .setContentIntent(pi)
            .setPriority(NotificationCompat.PRIORITY_MIN)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .build()
    }

    fun incoming(ctx: Context, info: CallerInfo): Notification? {
        if (!Perms.granted(ctx, android.Manifest.permission.POST_NOTIFICATIONS) &&
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU
        ) return null
        val tap = PendingIntent.getActivity(
            ctx, 10, CallCardActivity.ringIntent(ctx, info),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        val m = ctx.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val title = if (info.spam) "🚩 Spam call — ${info.displayName}"
        else "Incoming call${if (info.name.isEmpty()) "" else " — ${info.name}"}"
        val text = buildString {
            append(PhoneUtils.emask(info.number))
            if (info.location.isNotEmpty()) append(" · ").append(info.location)
            if (info.spam) {
                append("\n")
                append(info.spamReason)
            } else if (!info.isContact && !info.spam) {
                append("\nNot in contacts — identify below")
            }
        }
        val not = NotificationCompat.Builder(ctx, CH_RING)
            .setSmallIcon(smallIcon())
            .setContentTitle(title)
            .setContentText(text)
            .setStyle(NotificationCompat.BigTextStyle().bigText(text))
            .setWhen(System.currentTimeMillis())
            .setUsesChronometer(true)
            .setOngoing(true)
            .setCategory(NotificationCompat.CATEGORY_CALL)
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setColor(if (info.spam) 0xFFE53935.toInt() else 0xFF0087FF.toInt())
            .setFullScreenIntent(tap, true)
            .setContentIntent(tap)
            .addAction(0, "Open details", tap)
            .build()
        m.notify(KRingId, not)
        return not
    }

    fun cancelRing(ctx: Context) {
        (ctx.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager).cancel(KRingId)
    }

    fun afterCall(ctx: Context, info: CallerInfo) {
        val tap = PendingIntent.getActivity(
            ctx, 11, CallCardActivity.afterIntent(ctx, info),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        val title = "Call ended — ${info.displayName}"
        val text = if (info.spam)
            "${info.spamReason} — block or report"
        else
            "Was this call fine? Save, report or block."
        (ctx.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager).notify(
            KAfterId,
            NotificationCompat.Builder(ctx, CH_ALERTS)
                .setSmallIcon(smallIcon())
                .setContentTitle(title)
                .setContentText(text)
                .setStyle(NotificationCompat.BigTextStyle().bigText(text))
                .setContentIntent(tap)
                .setAutoCancel(true)
                .setLocalOnly(true)
                .build()
        )
    }

    fun blocked(ctx: Context, info: CallerInfo) {
        (ctx.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager).notify(
            7121,
            NotificationCompat.Builder(ctx, CH_ALERTS)
                .setSmallIcon(smallIcon())
                .setContentTitle("🚫 Blocked spam call")
                .setContentText("${info.displayName} (${info.number}) — ${info.spamReason}")
                .setAutoCancel(true)
                .build()
        )
    }

    /** Shown when CraftCaller blocks an incoming ring from the screening service. */
    fun screeningBlocked(ctx: Context, info: CallerInfo) {
        blocked(ctx, info)
    }

    fun premiumWished(ctx: Context) {
        val pi = PendingIntent.getActivity(
            ctx, 12, Intent(ctx, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE
        )
        (ctx.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager).notify(
            7130,
            NotificationCompat.Builder(ctx, CH_ALERTS)
                .setSmallIcon(smallIcon())
                .setContentTitle("CraftCaller Premium is ready when you are")
                .setContentText("Blocking, recording & UI-ads free — open Premium to activate locally.")
                .setContentIntent(pi)
                .setAutoCancel(true)
                .build()
        )
    }
}
