package com.craftcaller.app.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Explicit allow-list entry: this number is NEVER auto-blocked by the
 * screening service and its identity is trusted over community spam reports
 * (still shown honestly, but never rejected automatically).
 */
@Entity(tableName = "allowed_numbers")
data class AllowedNumber(
    @PrimaryKey val number: String,
    val label: String? = null,
    val addedAt: Long = System.currentTimeMillis()
)
