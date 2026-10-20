package com.craftcaller.app.data

import android.content.ContentResolver
import android.content.Context
import android.provider.CallLog
import android.provider.ContactsContract
import android.provider.Telephony
import com.craftcaller.app.util.PhoneUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** Read-only helpers over the system content providers. */

suspend fun readSystemCallLog(ctx: Context, limit: Int = 200): List<CallEntry> =
    withContext(Dispatchers.IO) {
        val out = ArrayList<CallEntry>(limit)
        val resolver: ContentResolver = ctx.contentResolver
        val proj = arrayOf(
            CallLog.Calls.NUMBER, CallLog.Calls.CACHED_NAME, CallLog.Calls.DATE,
            CallLog.Calls.TYPE, CallLog.Calls.DURATION, CallLog.Calls.NEW
        )
        try {
            resolver.query(
                CallLog.Calls.CONTENT_URI, proj, null, null,
                CallLog.Calls.DATE + " DESC"
            )?.use { c ->
                val numberIx = c.getColumnIndexOrThrow(CallLog.Calls.NUMBER)
                val nameIx = c.getColumnIndexOrThrow(CallLog.Calls.CACHED_NAME)
                val dateIx = c.getColumnIndexOrThrow(CallLog.Calls.DATE)
                val typeIx = c.getColumnIndexOrThrow(CallLog.Calls.TYPE)
                val durIx = c.getColumnIndexOrThrow(CallLog.Calls.DURATION)
                while (c.moveToNext() && out.size < limit) {
                    val number = PhoneUtils.normalize(c.getString(numberIx))
                    out += CallEntry(
                        number = number,
                        name = c.getString(nameIx) ?: "",
                        ts = c.getLong(dateIx),
                        type = c.getInt(typeIx),
                        duration = c.getLong(durIx),
                        spam = false
                    )
                }
            }
        } catch (_: SecurityException) {
        }
        out
    }

suspend fun readSystemContacts(ctx: Context): List<Pair<String, String>> =
    withContext(Dispatchers.IO) {
        val out = ArrayList<Pair<String, String>>()
        try {
            ctx.contentResolver.query(
                ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                arrayOf(
                    ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME,
                    ContactsContract.CommonDataKinds.Phone.NUMBER
                ),
                null, null,
                ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME + " ASC"
            )?.use { c ->
                val nIx = c.getColumnIndexOrThrow(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME)
                val pIx = c.getColumnIndexOrThrow(ContactsContract.CommonDataKinds.Phone.NUMBER)
                while (c.moveToNext()) {
                    val num = PhoneUtils.normalize(c.getString(pIx))
                    if (num.isNotEmpty()) out += (c.getString(nIx) ?: "") to num
                }
            }
        } catch (_: SecurityException) {
        }
        out
    }

suspend fun readSystemSms(ctx: Context, limit: Int = 100): List<SmsEntry> =
    withContext(Dispatchers.IO) {
        val out = ArrayList<SmsEntry>(limit)
        try {
            ctx.contentResolver.query(
                Telephony.Sms.CONTENT_URI,
                arrayOf(
                    Telephony.Sms._ID, Telephony.Sms.ADDRESS, Telephony.Sms.BODY,
                    Telephony.Sms.DATE, Telephony.Sms.READ
                ),
                null, null, Telephony.Sms.DATE + " DESC"
            )?.use { c ->
                val idIx = c.getColumnIndexOrThrow(Telephony.Sms._ID)
                val addrIx = c.getColumnIndexOrThrow(Telephony.Sms.ADDRESS)
                val bodyIx = c.getColumnIndexOrThrow(Telephony.Sms.BODY)
                val dateIx = c.getColumnIndexOrThrow(Telephony.Sms.DATE)
                while (c.moveToNext() && out.size < limit) {
                    val num = PhoneUtils.normalize(c.getString(addrIx))
                    val body = c.getString(bodyIx) ?: ""
                    out += SmsEntry(
                        id = c.getLong(idIx),
                        number = num,
                        name = "",
                        body = body,
                        ts = c.getLong(dateIx),
                        otp = isOtpSms(body),
                        spam = isSpamSms(body)
                    )
                }
            }
        } catch (_: Exception) {
            // READ_SMS not granted yet
        }
        out
    }

fun isOtpSms(body: String): Boolean {
    val patterns = listOf(
        Regex("\\b\\d{4,8}\\b"),
        Regex("(?i)(otp|verification|code)"),
        Regex("\\b\\d{4,8}\\b is your")
    )
    return (patterns[1].containsMatchIn(body) && patterns[0].containsMatchIn(body)) ||
            patterns[2].containsMatchIn(body)
}

fun isSpamSms(body: String): Boolean {
    val redFlags = listOf(
        "win", "prize", "lottery", "free recharge", "kyc", "otp",
        "click this link", "verify your account", "urgent", "double your"
    )
    val low = body.lowercase()
    var score = 0
    for (f in redFlags) if (low.contains(f)) score++
    // OTP passcode texts are legit; don't flag
    if ((low.contains("otp") || low.contains("one time password")) && !low.contains("world")) score = score.coerceAtMost(1)
    return score >= 3
}
