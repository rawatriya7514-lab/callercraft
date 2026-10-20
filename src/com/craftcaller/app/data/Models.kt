package com.craftcaller.app.data

import android.os.Parcelable
import androidx.annotation.DrawableRes

/** Own model classes for the whole app (no room-compat annotations; private schema). */

data class CallerInfo(
    val number: String = "",
    val name: String = "",
    val location: String = "",
    val spam: Boolean = false,
    val spamReason: String = "",
    val spamReports: Int = 0,
    val score: Int = 0,             // 0..100 trust/confidence
    val blocked: Boolean = false,
    val source: String = "unknown"  // contacts | cache | cloud | demo | unknown
) {
    val displayName: String get() = name.ifEmpty { "Unknown" }
    val isContact: Boolean get() = source == "contacts"
}

data class CallEntry(
    val number: String,
    val name: String,
    val ts: Long,
    val type: Int,          // 1=out, 2=in, 3=missed
    val duration: Long,
    val spam: Boolean
)

data class BlockedNumber(val number: String, val ts: Long)

data class SmsEntry(
    val id: Long,
    val number: String,
    val name: String,
    val body: String,
    val ts: Long,
    val otp: Boolean,
    val spam: Boolean
)

data class ContactRow(
    val name: String,
    val number: String,
    val spam: Boolean,
    val badge: String     // "Friend" | "Contact" | "Spam" | "Unknown"
)

data class SpamReportEntry(
    val number: String,
    val reports: Int,
    val topReason: String
)
