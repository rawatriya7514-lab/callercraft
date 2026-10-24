package com.craftcaller.app.repository

import android.content.Context
import android.os.Build
import android.provider.CallLog
import com.craftcaller.app.data.local.AppDatabase
import com.craftcaller.app.data.local.entity.CallLogEntry
import com.craftcaller.app.util.Craft
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.concurrent.TimeUnit

/** Syncs the system call log into Room and enriches it with local DB knowledge. */
class CallLogRepository(
    private val context: Context,
    private val db: AppDatabase
) {

    /** Pull the last [maxEntries] call-log rows; annotate with known names/spam. */
    suspend fun syncRecent(maxEntries: Int = 400): List<CallLogEntry> = withContext(Dispatchers.IO) {
        val entries = mutableListOf<CallLogEntry>()
        runCatching {
            val contacts = ContactsRepository(context).loadDeviceContacts()
                .associateBy({ it.number }, { it.name })

            context.contentResolver.query(
                CallLog.Calls.CONTENT_URI,
                arrayOf(
                    CallLog.Calls._ID,
                    CallLog.Calls.NUMBER,
                    CallLog.Calls.TYPE,
                    CallLog.Calls.DATE,
                    CallLog.Calls.DURATION
                ),
                null,
                null,
                CallLog.Calls.DATE + " DESC"
            )?.use { cursor ->
                val idIx = cursor.getColumnIndexOrThrow(CallLog.Calls._ID)
                val numIx = cursor.getColumnIndexOrThrow(CallLog.Calls.NUMBER)
                val typeIx = cursor.getColumnIndexOrThrow(CallLog.Calls.TYPE)
                val dateIx = cursor.getColumnIndexOrThrow(CallLog.Calls.DATE)
                val durIx = cursor.getColumnIndexOrThrow(CallLog.Calls.DURATION)

                val now = System.currentTimeMillis()
                while (cursor.moveToNext() && entries.size < maxEntries) {
                    val number = cursor.getString(numIx)?.trim().orEmpty()
                    if (number.isEmpty()) continue
                    val key = Craft.normalizeNumber(number)
                    entries += CallLogEntry(
                        id = cursor.getLong(idIx),
                        number = key,
                        resolvedName = contacts[key],
                        callType = cursor.getInt(typeIx),
                        timestamp = cursor.getLong(dateIx),
                        duration = TimeUnit.SECONDS.toMillis(cursor.getLong(durIx).coerceAtLeast(0))
                    )
                }
                cursor.close()
            }

            db.callLogDao().deleteOlderThan(System.currentTimeMillis() - TimeUnit.DAYS.toMillis(30))
            db.callLogDao().upsertAll(entries)
            applyCallerKnowledge(entries)
        }.getOrNull()

        entries
    }

    /** Merge cached caller names + spam flags from `callers` table into the log. */
    suspend fun applyCallerKnowledge(entries: List<CallLogEntry>) = withContext(Dispatchers.IO) {
        val numbers = entries.map { it.number }.distinct()
        numbers.forEach { num ->
            val known = db.callerDao().get(num)
            if (known != null) {
                db.callLogDao().annotate(
                    number = num,
                    name = known.name,
                    spam = known.isSpam
                )
            }
        }
    }
}
