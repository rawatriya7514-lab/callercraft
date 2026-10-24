package com.craftcaller.app.repository

import android.content.Context
import android.provider.Telephony
import com.craftcaller.app.data.local.AppDatabase
import com.craftcaller.app.data.local.entity.SmsMessage
import com.craftcaller.app.util.Craft
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** Uploads the SMS inbox into Room with OTP + spam classification. */
class SmsRepository(
    private val context: Context,
    private val db: AppDatabase
) {

    companion object {
        /** 4-8 digit code preceded by OTP-ish wording. */
        private val OTP_CONTEXT = Regex(
            pattern = "(?i)(otp|code|pin|password|verification|verify)[^\\d]{0,30}(\\d{4,8})",
            options = setOf()
        )
        private val BARE_OTP = Regex("(?<![0-9])(\\d{6})(?![0-9])")

        private val SPAM_WORDS = listOf(
            "congratulations", "winner", "lottery", "prize", "claim now",
            "limited time", "click here", "urgent action", "loan approved",
            "kyc expired", "verify your account", "free recharge",
            "investment opportunity", "act now", "exclusive offer"
        )
    }

    fun detectOtp(body: String): String? {
        val contextual = OTP_CONTEXT.find(body)?.groupValues?.getOrNull(2)
        return contextual ?: BARE_OTP.find(body)?.groupValues?.getOrNull(1)
    }

    fun detectSpam(body: String): Boolean {
        val lower = body.lowercase()
        return SPAM_WORDS.any { lower.contains(it) }
    }

    suspend fun syncInbox(maxEntries: Int = 200): List<SmsMessage> = withContext(Dispatchers.IO) {
        val items = mutableListOf<SmsMessage>()
        runCatching {
            context.contentResolver.query(
                Telephony.Sms.CONTENT_URI,
                arrayOf(
                    Telephony.Sms._ID,
                    Telephony.Sms.ADDRESS,
                    Telephony.Sms.BODY,
                    Telephony.Sms.DATE
                ),
                null,
                null,
                Telephony.Sms.DATE + " DESC"
            )?.use { cursor ->
                val idIx = cursor.getColumnIndexOrThrow(Telephony.Sms._ID)
                val addrIx = cursor.getColumnIndexOrThrow(Telephony.Sms.ADDRESS)
                val bodyIx = cursor.getColumnIndexOrThrow(Telephony.Sms.BODY)
                val dateIx = cursor.getColumnIndexOrThrow(Telephony.Sms.DATE)

                while (cursor.moveToNext() && items.size < maxEntries) {
                    val body = cursor.getString(bodyIx)?.trim().orEmpty()
                    val raw = cursor.getString(addrIx)?.trim().orEmpty()
                    if (raw.isEmpty()) continue
                    items += SmsMessage(
                        id = cursor.getLong(idIx),
                        address = Craft.normalizeNumber(raw),
                        senderName = null,
                        body = body,
                        timestamp = cursor.getLong(dateIx),
                        otpCode = detectOtp(body),
                        isSpam = detectSpam(body)
                    )
                }
            }
            db.smsDao().upsertAll(items)
        }.getOrNull()

        items
    }
}
