package com.craftcaller.app.data.local.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.craftcaller.app.data.local.entity.SmsMessage
import kotlinx.coroutines.flow.Flow

@Dao
interface SmsDao {

    @Query("SELECT * FROM sms_messages ORDER BY timestamp DESC")
    fun observeAll(): Flow<List<SmsMessage>>

    @Upsert
    suspend fun upsertAll(items: List<SmsMessage>)

    @Upsert
    suspend fun upsert(item: SmsMessage)

    @Query("UPDATE sms_messages SET senderName = :name WHERE address = :address")
    suspend fun annotate(address: String, name: String)

    @Query("DELETE FROM sms_messages")
    suspend fun clear()

    @Query("SELECT COUNT(*) FROM sms_messages")
    suspend fun currentCount(): Int
}
