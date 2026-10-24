package com.craftcaller.app

import android.Manifest
import android.app.Application
import android.content.ContentUris
import android.content.ContentValues
import android.content.Context
import android.provider.ContactsContract
import androidx.test.core.app.ApplicationProvider
import com.craftcaller.app.service.AnnouncementEngine
import com.craftcaller.app.util.Prefs
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import com.craftcaller.app.util.Craft

/**
 * Contact matching + denied permission + consent gating for the announcement
 * engine's contact lookup:
 *   consent OFF -> never read contacts (even with permission),
 *   permission denied -> no read,
 *   both on -> the real contact display name is returned.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ContactConsentTest {

    private lateinit var app: Application
    private val rawNumber = "+919876543210"
    private val contactsMap = mutableMapOf<String, String>()

    @Before
    fun setup() {
        app = ApplicationProvider.getApplicationContext()
        app.getSharedPreferences("craftcaller_prefs", 0).edit().clear().commit()
        app.getSharedPreferences("craftcaller_profile_secure", 0).edit().clear().commit()
        contactsMap.clear()
        AnnouncementEngine.contactLookupOverride = { _, num ->
            contactsMap[num] ?: contactsMap[Craft.normalizeNumber(num)]
        }
    }

    @After
    fun tearDown() {
        AnnouncementEngine.contactLookupOverride = null
    }

    private fun insertContact(name: String, number: String) {
        contactsMap[number] = name
        contactsMap[Craft.normalizeNumber(number)] = name
    }

    @Test
    fun `consent off never reads contacts even when the permission exists`() {
        insertContact("Rahul Sharma", rawNumber)
        shadowOf(app).grantPermissions(Manifest.permission.READ_CONTACTS)
        val prefs = Prefs(app)
        prefs.announceUseContacts = false

        assertNull(
            AnnouncementEngine.lookupContactName(app, rawNumber)
        )
    }

    @Test
    fun `denied permission returns null contact without crashing`() {
        val prefs = Prefs(app)
        prefs.announceUseContacts = true
        // Robolectric default: runtime permission NOT granted.
        assertNull(AnnouncementEngine.lookupContactName(app, rawNumber))
    }

    @Test
    fun `consent plus permission resolves the real display name`() {
        insertContact("Rahul Sharma", rawNumber)
        shadowOf(app).grantPermissions(Manifest.permission.READ_CONTACTS)
        val prefs = Prefs(app)
        prefs.announceUseContacts = true

        assertEquals(
            "Rahul Sharma",
            AnnouncementEngine.lookupContactName(app, rawNumber)
        )
    }

    @Test
    fun `unmatched number yields null (never a guessed name)`() {
        shadowOf(app).grantPermissions(Manifest.permission.READ_CONTACTS)
        val prefs = Prefs(app)
        prefs.announceUseContacts = true
        assertNull(
            AnnouncementEngine.lookupContactName(app, "+919100000000")
        )
    }
}
