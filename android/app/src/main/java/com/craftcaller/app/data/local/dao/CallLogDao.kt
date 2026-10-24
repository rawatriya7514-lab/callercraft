package com.craftcaller.app.data.local.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.craftcaller.app.data.local.entity.CallLogEntry
import kotlinx.coroutines.flow.Flow

@Dao
interface CallLogDao {

    @Query("SELECT * FROM call_log ORDER BY timestamp DESC")
    fun observeAll(): Flow<List<CallLogEntry>>

    @Query("SELECT COUNT(*) FROM call_log")
    fun observeCount(): Flow<Int>

    @Query("SELECT * FROM call_log WHERE number = :number ORDER BY timestamp DESC LIMIT 1")
    suspend fun latestFor(number: String): CallLogEntry?

    @Query(
        """
        SELECT * FROM call_log
        WHERE number LIKE '%' || :query || '%'
           OR resolvedName LIKE '%' || :query || '%'
        ORDER BY timestamp DESC LIMIT 25
        """
    )
    suspend fun search(query: String): List<CallLogEntry>

    @Upsert
    suspend fun upsertAll(entries: List<CallLogEntry>)

    @Query("UPDATE call_log SET resolvedName = :name, isSpam = :spam WHERE number = :number")
    suspend fun annotate(number: String, name: String?, spam: Boolean): Int

    @Query("DELETE FROM call_log WHERE timestamp < :before")
    suspend fun deleteOlderThan(before: Long)

    @Query("DELETE FROM call_log")
    suspend fun clear()

    @Query("SELECT COUNT(*) FROM call_log")
    suspend fun currentCount(): Int
}
