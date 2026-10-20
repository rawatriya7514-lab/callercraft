package com.craftcaller.app.util

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/** App-wide singletons the UI and services share (initialized in [App]). */
object Cctx {
    lateinit var app: Context
}

object PhoneUtils {

    /** Normalize any raw number into the canonical community-database key. */
    fun normalize(raw: String?): String {
        var s = raw?.trim().orEmpty()
        if (s.isEmpty()) return ""
        s = s.replace("-", " ")
            .replace("(", " ")
            .replace(")", " ")
            .replace(" ", "")
            .filter { it.isDigit() || it == '+' || it == '*' || it == '#' }
        if (s.startsWith("+")) return s
        if (s.startsWith("00")) return "+" + s.drop(2)
        if (s.length > 10) return "+" + s
        return s
    }

    /** Mask middle digits for the sweep list (privacy, Truecaller-style). */
    fun emask(number: String): String {
        if (number.length <= 6) return number
        val head = number.take(number.length - 6)
        val tail = number.takeLast(2)
        return head + "✱✱✱✱" + tail
    }

    fun displayNumber(n: String): String = n.ifEmpty { "Unknown number" }

    fun isValidNumber(n: String): Boolean {
        val digits = n.filter { it.isDigit() }
        return digits.length >= 7 && n.any { it.isDigit() }
    }
}

object TimeAgo {
    fun fmt(ts: Long): String {
        val d = Date(ts)
        val now = System.currentTimeMillis()
        val diff = (now - ts) / 1000
        return when {
            diff < 60 -> "now"
            diff < 3600 -> "${diff / 60}m"
            diff < 86400 -> "${diff / 3600}h"
            else -> SimpleDateFormat("dd MMM", Locale.getDefault()).format(d)
        }
    }

    fun day(ts: Long): String =
        SimpleDateFormat("EEEE, d MMMM", Locale.getDefault()).format(Date(ts))
}

object Perms {
    val phoneCore = listOf(
        Manifest.permission.READ_PHONE_STATE,
        Manifest.permission.READ_CALL_LOG,
        Manifest.permission.READ_CONTACTS,
        Manifest.permission.CALL_PHONE,
        Manifest.permission.ANSWER_PHONE_CALLS
    )
    val sms = listOf(
        Manifest.permission.READ_SMS,
        Manifest.permission.RECEIVE_SMS
    )
    val notifications = if (Build.VERSION.SDK_INT >= 33) {
        listOf(Manifest.permission.POST_NOTIFICATIONS)
    } else emptyList()
    val microphone = listOf(Manifest.permission.RECORD_AUDIO)

    fun granted(ctx: Context, p: String): Boolean =
        ctx.checkSelfPermission(p) == PackageManager.PERMISSION_GRANTED

    fun allGranted(ctx: Context, ps: List<String>): Boolean =
        ps.all { granted(ctx, it) }
}
