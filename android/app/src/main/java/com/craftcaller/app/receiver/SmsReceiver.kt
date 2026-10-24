package com.craftcaller.app.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.provider.Telephony
import android.telephony.SmsMessage
import com.craftcaller.app.data.local.DatabaseProvider
import com.craftcaller.app.data.local.entity.SmsMessage as SmsEntity
import com.craftcaller.app.repository.SmsRepository
import com.craftcaller.app.util.Craft
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import java.util.UUID

/** Live SMS interception: classify OTP/spam and store into the Smart inbox. */
class SmsReceiver : BroadcastReceiver() {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Telephony.Sms.Intents.SMS_RECEIVED_ACTION) return
        val bundle = intent.extras ?: return

        val pdus = runCatching {
            @Suppress("DEPRECATION")
            bundle.get("pdus") as? Array<*> ?: return
        }.getOrNull() ?: return

        val fullBody = StringBuilder()
        var address = ""
        var timestamp = System.currentTimeMillis()

        pdus.forEach { pdu ->
            val bytes = pdu as? ByteArray ?: return@forEach
            val sms = runCatching {
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M)
                    SmsMessage.createFromPdu(bytes, "3gpp")
                else SmsMessage.createFromPdu(bytes)
            }.getOrNull() ?: return@forEach
            fullBody.append(sms.messageBody.orEmpty())
            address = sms.originatingAddress ?: address
            sms.timestampMillis.takeIf { it > 0 }?.let { timestamp = it }
        }

        if (address.isEmpty() || fullBody.isBlank()) return
        val detector = SmsRepository(context, DatabaseProvider.get(context))
        val entry = SmsEntity(
            id = UUID.randomUUID().mostSignificantBits and Long.MAX_VALUE,
            address = Craft.normalizeNumber(address),
            senderName = null,
            body = fullBody.toString(),
            timestamp = timestamp,
            otpCode = detector.detectOtp(fullBody.toString()),
            isSpam = detector.detectSpam(fullBody.toString())
        )
        scope.launch { DatabaseProvider.get(context).smsDao().upsert(entry) }
    }
}
