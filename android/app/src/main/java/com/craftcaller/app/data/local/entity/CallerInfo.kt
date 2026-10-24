package com.craftcaller.app.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/** Cached caller info from Firebase / own API / user-provided names. */
@Entity(tableName = "callers")
data class CallerInfo(
    @PrimaryKey val number: String,
    val name: String?,
    val location: String? = null,
    val spamReports: Int = 0,
    val updatedAt: Long = System.currentTimeMillis(),
    val source: String = "local"
) {
    val isSpam: Boolean get() = spamReports >= 10
}
