package com.craftcaller.app.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/** Blocked caller: auto rejected via CallScreeningService / TelecomManager. */
@Entity(tableName = "blocked_numbers")
data class BlockedNumber(
    @PrimaryKey val number: String,
    val savedName: String? = null,
    val reason: String? = null,
    val addedAt: Long = System.currentTimeMillis()
)
