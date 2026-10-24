package com.craftcaller.app.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.craftcaller.app.data.local.entity.BlockedNumber
import kotlinx.coroutines.flow.Flow

@Dao
interface BlockedDao {

    @Query("SELECT * FROM blocked_numbers ORDER BY addedAt DESC")
    fun observeAll(): Flow<List<BlockedNumber>>

    @Query("SELECT * FROM blocked_numbers")
    suspend fun getAll(): List<BlockedNumber>

    @Query("SELECT EXISTS(SELECT 1 FROM blocked_numbers WHERE number = :number)")
    suspend fun exists(number: String): Boolean

    @Query("SELECT EXISTS(SELECT 1 FROM blocked_numbers WHERE number = :number)")
    fun existsFlow(number: String): Flow<Boolean>

    @Query("SELECT COUNT(*) FROM blocked_numbers")
    fun observeCount(): Flow<Int>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(entry: BlockedNumber): Long

    @Query("SELECT COUNT(*) FROM blocked_numbers")
    suspend fun currentCount(): Int

    @Query("DELETE FROM blocked_numbers WHERE number = :number")
    suspend fun delete(number: String)

    @Query("DELETE FROM blocked_numbers")
    suspend fun clear()
}
