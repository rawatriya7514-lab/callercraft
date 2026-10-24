package com.craftcaller.app.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/** Smart SMS inbox entry: OTP detection + spam classification. */
@Entity(tableName = "sms_messages")
data class SmsMessage(
    @PrimaryKey val id: Long,
    val address: String,
    val senderName: String?,
    val body: String,
    val timestamp: Long,
    val otpCode: String? = null,
    val isSpam: Boolean = false
)
