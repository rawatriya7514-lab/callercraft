package com.craftcaller.app.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.craftcaller.app.data.local.dao.AllowedDao
import com.craftcaller.app.data.local.dao.BlockedDao
import com.craftcaller.app.data.local.dao.CallerDao
import com.craftcaller.app.data.local.dao.CallLogDao
import com.craftcaller.app.data.local.dao.SmsDao
import com.craftcaller.app.data.local.entity.AllowedNumber
import com.craftcaller.app.data.local.entity.BlockedNumber
import com.craftcaller.app.data.local.entity.CallerInfo
import com.craftcaller.app.data.local.entity.CallLogEntry
import com.craftcaller.app.data.local.entity.SmsMessage

@Database(
    entities = [
        CallerInfo::class,
        BlockedNumber::class,
        CallLogEntry::class,
        SmsMessage::class,
        AllowedNumber::class
    ],
    version = 2,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun callerDao(): CallerDao
    abstract fun blockedDao(): BlockedDao
    abstract fun callLogDao(): CallLogDao
    abstract fun smsDao(): SmsDao
    abstract fun allowedDao(): AllowedDao
}

/** Process-wide database singleton (also used by services/receivers). */
object DatabaseProvider {
    @Volatile
    private var instance: AppDatabase? = null

    @Volatile
    var testOverride: AppDatabase? = null

    /**
     * v1 -> v2: add the explicit allow-list table. Written by hand so existing
     * user data is preserved (destructive migration removed — audit P1).
     */
    private val MIGRATION_1_2 = object : Migration(1, 2) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL(
                "CREATE TABLE IF NOT EXISTS `allowed_numbers` (" +
                    "`number` TEXT NOT NULL, " +
                    "`label` TEXT, " +
                    "`addedAt` INTEGER NOT NULL, " +
                    "PRIMARY KEY(`number`))"
            )
        }
    }

    fun get(context: Context): AppDatabase =
        testOverride ?: instance ?: synchronized(this) {
            instance ?: Room.databaseBuilder(
                context.applicationContext,
                AppDatabase::class.java,
                "craftcaller.db"
            )
                .addMigrations(MIGRATION_1_2)
                .build()
                .also { instance = it }
        }
}
