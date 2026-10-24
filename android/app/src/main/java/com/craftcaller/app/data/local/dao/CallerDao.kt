package com.craftcaller.app.data.local.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.craftcaller.app.data.local.entity.CallerInfo
import kotlinx.coroutines.flow.Flow

@Dao
interface CallerDao {

    @Query("SELECT * FROM callers WHERE number = :number")
    suspend fun get(number: String): CallerInfo?

    @Query("SELECT * FROM callers WHERE number = :number")
    fun observe(number: String): Flow<CallerInfo?>

    @Query("SELECT * FROM callers ORDER BY updatedAt DESC LIMIT 50")
    fun observeRecent(): Flow<List<CallerInfo>>

    @Query(
        """
        SELECT * FROM callers
        WHERE number LIKE '%' || :query || '%'
           OR (name IS NOT NULL AND name LIKE '%' || :query || '%')
        ORDER BY updatedAt DESC LIMIT 10
        """
    )
    suspend fun search(query: String): List<CallerInfo>

    @Upsert
    suspend fun upsert(info: CallerInfo)

    @Upsert
    suspend fun upsertAll(infos: List<CallerInfo>)

    @Query("UPDATE callers SET name = :name, updatedAt = :now WHERE number = :number")
    suspend fun setName(number: String, name: String, now: Long): Int

    @Query("UPDATE callers SET spamReports = :reports, updatedAt = :now WHERE number = :number")
    suspend fun setSpamReports(number: String, reports: Int, now: Long): Int

    @Query("DELETE FROM callers")
    suspend fun clear()

    @Query("SELECT COUNT(*) FROM callers")
    suspend fun currentCount(): Int
}
