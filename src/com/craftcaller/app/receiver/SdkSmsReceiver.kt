package com.craftcaller.app.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.telephony.SmsMessage
import com.craftcaller.app.data.Db
import com.craftcaller.app.data.Repo
import com.craftcaller.app.data.SmsEntry
import com.craftcaller.app.data.isOtpSms
import com.craftcaller.app.data.isSpamSms
import com.craftcaller.app.util.PhoneUtils

/**
 * Smart SMS engine: stores every incoming SMS in the local inbox, detects
 * one-time codes and scores spam/phishing texts, and refreshes the Messages
 * tab. (Manifest guard: android:permission="BROADCAST_SMS" so only the
 * telephony framework can deliver these broadcasts.)
 */
class SdkSmsReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action ?: return
        if (action != "android.provider.Telephony.SMS_RECEIVED" &&
            action != "android.provider.Telephony.SMS_DELIVER"
        ) return
        val pdus = intent.extras?.get("pdus") as Array<ByteArray>? ?: return
        var handled = false
        for (p in pdus) {
            val msg = parsePdu(p, intent)
            if (msg == null) continue
            handled = true
            val number = PhoneUtils.normalize(msg.originatingAddress ?: "")
            val body = msg.messageBody ?: ""
            Db.putSms(
                SmsEntry(
                    id = System.currentTimeMillis() + pdus.indexOf(p),
                    number = number,
                    name = "",
                    body = body,
                    ts = msg.timestampMillis.takeIf { it > 0 } ?: System.currentTimeMillis(),
                    otp = isOtpSms(body),
                    spam = isSpamSms(body)
                )
            )
        }
        if (handled) Repo.refresh()
    }

    private fun parsePdu(pdu: ByteArray, intent: Intent): SmsMessage? = try {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            SmsMessage.createFromPdu(pdu, intent.getStringExtra("format"))
        } else {
            @Suppress("DEPRECATION")
            SmsMessage.createFromPdu(pdu)
        }
    } catch (_: Throwable) {
        null
    }
}
