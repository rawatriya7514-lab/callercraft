package com.craftcaller.app.repository

import android.content.Context
import android.provider.ContactsContract
import com.craftcaller.app.data.model.DeviceContact
import com.craftcaller.app.util.Craft
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** Reads the device address book. */
class ContactsRepository(private val context: Context) {

    suspend fun loadDeviceContacts(): List<DeviceContact> = withContext(Dispatchers.IO) {
        val result = LinkedHashMap<String, DeviceContact>() // number -> first contact (no dup rows)
        runCatching {
            context.contentResolver.query(
                ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                arrayOf(
                    ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME,
                    ContactsContract.CommonDataKinds.Phone.NUMBER
                ),
                null,
                null,
                ContactsContract.CommonDataKinds.Phone.SORT_KEY_PRIMARY
            )?.use { cursor ->
                while (cursor.moveToNext()) {
                    val name = cursor.getString(0)?.trim().orEmpty()
                    val raw = cursor.getString(1)?.trim().orEmpty()
                    if (raw.isEmpty()) continue
                    val key = Craft.normalizeNumber(raw)
                    if (key.length >= 8 && !result.containsKey(key)) {
                        result[key] = DeviceContact(name = name.ifBlank { "Unknown" }, number = key)
                    }
                }
            }
        }
        result.values.toList()
    }

    /** Phonebook name for a number (for call-log enrichment). */
    suspend fun nameFor(number: String): String? = withContext(Dispatchers.IO) {
        runCatching {
            context.contentResolver.query(
                ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                arrayOf(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME),
                ContactsContract.CommonDataKinds.Phone.NUMBER + " LIKE ?",
                arrayOf("%" + number.takeLast(8)),
                null
            )?.use { cursor ->
                if (cursor.moveToFirst()) cursor.getString(0) else null
            }
        }.getOrNull()
    }
}
