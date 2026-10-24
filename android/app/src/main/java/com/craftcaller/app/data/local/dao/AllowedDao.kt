package com.craftcaller.app.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.craftcaller.app.data.local.entity.AllowedNumber
import kotlinx.coroutines.flow.Flow

@Dao
interface AllowedDao {

    @Query("SELECT * FROM allowed_numbers ORDER BY addedAt DESC")
    fun observeAll(): Flow<List<AllowedNumber>>

    @Query("SELECT * FROM allowed_numbers ORDER BY addedAt DESC")
    suspend fun getAll(): List<AllowedNumber>

    @Query("SELECT EXISTS(SELECT 1 FROM allowed_numbers WHERE number = :number)")
    suspend fun exists(number: String): Boolean

    @Query("SELECT COUNT(*) FROM allowed_numbers")
    fun observeCount(): Flow<Int>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entry: AllowedNumber): Long

    @Query("SELECT COUNT(*) FROM allowed_numbers")
    suspend fun currentCount(): Int

    @Query("DELETE FROM allowed_numbers WHERE number = :number")
    suspend fun delete(number: String)

    @Query("DELETE FROM allowed_numbers")
    suspend fun clear()
}
