package com.craftcaller.app.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/** Synced system call log entry with resolved caller name + spam tag. */
@Entity(tableName = "call_log")
data class CallLogEntry(
    @PrimaryKey val id: Long,
    val number: String,
    val resolvedName: String?,
    /** CallLog.Calls.INCOMING / OUTGOING / MISSED / REJECTED / BLOCKED */
    val callType: Int,
    val timestamp: Long,
    val duration: Long,
    val isSpam: Boolean = false
)
