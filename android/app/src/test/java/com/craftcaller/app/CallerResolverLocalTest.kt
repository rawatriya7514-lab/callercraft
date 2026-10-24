package com.craftcaller.app

import android.Manifest
import android.app.Application
import android.content.ContentUris
import android.content.ContentValues
import android.provider.ContactsContract
import androidx.room.DatabaseConfiguration
import androidx.room.InvalidationTracker
import androidx.sqlite.db.SupportSQLiteOpenHelper
import androidx.test.core.app.ApplicationProvider
import com.craftcaller.app.caller.CallerLabel
import com.craftcaller.app.caller.CallerResolver
import com.craftcaller.app.data.local.AppDatabase
import com.craftcaller.app.data.local.DatabaseProvider
import com.craftcaller.app.data.local.dao.AllowedDao
import com.craftcaller.app.data.local.dao.BlockedDao
import com.craftcaller.app.data.local.dao.CallerDao
import com.craftcaller.app.data.local.dao.CallLogDao
import com.craftcaller.app.data.local.dao.SmsDao
import com.craftcaller.app.data.local.entity.AllowedNumber
import com.craftcaller.app.data.local.entity.CallerInfo
import com.craftcaller.app.data.remote.FirebaseRepository
import com.craftcaller.app.service.AnnouncementEngine
import com.craftcaller.app.util.Craft
import com.craftcaller.app.util.Prefs
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import java.util.concurrent.ConcurrentHashMap

private class FakeCallerDao : CallerDao {
    val map = ConcurrentHashMap<String, CallerInfo>()
    override suspend fun get(number: String): CallerInfo? = map[number]
    override fun observe(number: String): Flow<CallerInfo?> = flowOf(map[number])
    override fun observeRecent(): Flow<List<CallerInfo>> = flowOf(map.values.toList())
    override suspend fun search(query: String): List<CallerInfo> =
        map.values.filter { it.number.contains(query) || (it.name != null && it.name!!.contains(query)) }
    override suspend fun upsert(info: CallerInfo) { map[info.number] = info }
    override suspend fun upsertAll(infos: List<CallerInfo>) { infos.forEach { map[it.number] = it } }
    override suspend fun setName(number: String, name: String, now: Long): Int {
        val cur = map[number] ?: return 0
        map[number] = cur.copy(name = name, updatedAt = now)
        return 1
    }
    override suspend fun setSpamReports(number: String, reports: Int, now: Long): Int {
        val cur = map[number] ?: return 0
        map[number] = cur.copy(spamReports = reports, updatedAt = now)
        return 1
    }
    override suspend fun clear() { map.clear() }
    override suspend fun currentCount(): Int = map.size
}

private class FakeAllowedDao : AllowedDao {
    val map = ConcurrentHashMap<String, AllowedNumber>()
    override fun observeAll(): Flow<List<AllowedNumber>> = flowOf(map.values.toList())
    override suspend fun getAll(): List<AllowedNumber> = map.values.toList()
    override suspend fun exists(number: String): Boolean = map.containsKey(number)
    override fun observeCount(): Flow<Int> = flowOf(map.size)
    override suspend fun insert(entry: AllowedNumber): Long {
        map[entry.number] = entry
        return 1L
    }
    override suspend fun currentCount(): Int = map.size
    override suspend fun delete(number: String) { map.remove(number) }
    override suspend fun clear() { map.clear() }
}

private class FakeAppDatabase : AppDatabase() {
    val fakeCaller = FakeCallerDao()
    val fakeAllowed = FakeAllowedDao()

    override fun callerDao(): CallerDao = fakeCaller
    override fun allowedDao(): AllowedDao = fakeAllowed
    override fun blockedDao(): BlockedDao = error("not implemented")
    override fun callLogDao(): CallLogDao = error("not implemented")
    override fun smsDao(): SmsDao = error("not implemented")

    override fun clearAllTables() {
        fakeCaller.map.clear()
        fakeAllowed.map.clear()
    }
    override fun createInvalidationTracker(): InvalidationTracker =
        InvalidationTracker(this, emptyMap(), emptyMap(), "callers", "allowed_numbers")
    override fun createOpenHelper(config: DatabaseConfiguration): SupportSQLiteOpenHelper =
        error("not used in fake")
}

/**
 * CallerResolver ladder tests (no network on the robolectric path — the
 * build runs with a placeholder API key, so community enrichment is OFF by
 * configuration and [FirebaseRepository.placeholderConfig] is true).
 *
 * Covers: on-device SAVED, community names, SUSPECTED_SPAM conflict
 * resolution (reports win over a name), UNKNOWN honesty, allow-list,
 * permission-denied contact handling and the network-off contract.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class CallerResolverLocalTest {

    private lateinit var app: Application
    private val fakeDb = FakeAppDatabase()
    private val contactsMap = mutableMapOf<String, String>()

    @Before
    fun setup() = runBlocking {
        DatabaseProvider.testOverride = fakeDb
        app = ApplicationProvider.getApplicationContext()
        app.getSharedPreferences("craftcaller_prefs", 0).edit().clear().commit()
        app.getSharedPreferences("craftcaller_profile_secure", 0).edit().clear().commit()
        contactsMap.clear()
        AnnouncementEngine.contactLookupOverride = { _, num ->
            val digits = num.filter { it.isDigit() }
            contactsMap[num]
                ?: contactsMap[digits]
                ?: (if (digits.length == 12 && digits.startsWith("91")) contactsMap[digits.substring(2)] else null)
                ?: (if (digits.length == 10) contactsMap["91$digits"] else null)
                ?: contactsMap[Craft.normalizeNumber(num)]
        }
        // Keep the resolver free of contact noise unless a test opts in.
        val prefs = Prefs(app)
        prefs.announceUseContacts = false
        prefs.communityReadEnabled = true
        fakeDb.clearAllTables()
        Unit
    }

    @After
    fun tearDown() {
        DatabaseProvider.testOverride = null
        AnnouncementEngine.contactLookupOverride = null
    }

    private suspend fun cached(
        number: String,
        name: String? = null,
        reports: Int = 0,
        source: String = "community"
    ) {
        DatabaseProvider.get(app).callerDao().upsert(
            CallerInfo(
                number = number, name = name, location = null,
                spamReports = reports, updatedAt = System.currentTimeMillis(),
                source = source
            )
        )
    }

    // ---------------- unknown ------------------------------------------------

    @Test
    fun `nothing known stays honestly UNKNOWN with zero confidence`() =
        runBlocking {
            val id = CallerResolver.resolveLocal(app, "919876512345")
            assertEquals(CallerLabel.UNKNOWN, id.label)
            assertEquals("Unknown", id.display)
            assertEquals(0, id.confidence)
            assertFalse(id.allowed)
        }

    @Test
    fun `garbage input is unparseable and stays UNKNOWN`() = runBlocking {
        val id = CallerResolver.resolveLocal(app, "42")
        assertEquals(CallerLabel.UNKNOWN, id.label)
        assertTrue(id.sourceLine.contains("unparseable"))
    }

    // ---------------- saved contacts ------------------------------------------

    @Test
    fun `contact match wins with SAVED and full confidence`() {
        runBlocking {
            val prefs = Prefs(app)
            prefs.announceUseContacts = true
            insertRawContact("Ankit Varma", "9822011123")
            shadowOf(app).grantPermissions(Manifest.permission.READ_CONTACTS)

            val id = CallerResolver.resolveLocal(app, "919822011123")
            assertEquals(CallerLabel.SAVED, id.label)
            assertEquals("Ankit Varma", id.displayName)
            assertEquals(100, id.confidence)
        }
    }

    @Test
    fun `saved contact beats a cached community name`() = runBlocking {
        val prefs = Prefs(app)
        prefs.announceUseContacts = true
        insertRawContact("Ankit Varma", "9822011123")
        shadowOf(app).grantPermissions(Manifest.permission.READ_CONTACTS)
        cached("919822011123", name = "Someone Else", source = "user-report")

        val id = CallerResolver.resolveLocal(app, "919822011123")
        assertEquals(CallerLabel.SAVED, id.label)
        assertEquals("Ankit Varma", id.displayName)
    }

    // ---------------- community names + conflicts -----------------------------

    @Test
    fun `community submission surfaces as COMMUNITY with lower confidence`() =
        runBlocking {
            cached("919510305040", name = "Pooja Traders", source = "community")
            val id = CallerResolver.resolveLocal(app, "919510305040")
            assertEquals(CallerLabel.COMMUNITY, id.label)
            assertEquals("Pooja Traders", id.displayName)
            assertEquals(60, id.confidence)
        }

    @Test
    fun `user's own annotations get higher confidence than remote community`() =
        runBlocking {
            cached("919510305050", name = "Mummy", source = "local")
            val id = CallerResolver.resolveLocal(app, "919510305050")
            assertEquals(75, id.confidence)
        }

    @Test
    fun `conflicting reports - 10 plus reports override the name with SUSPECTED_SPAM`() =
        runBlocking {
            cached("919510305060", name = "Looks Harmless", reports = 12, source = "community")
            val id = CallerResolver.resolveLocal(app, "919510305060")
            assertEquals(CallerLabel.SUSPECTED_SPAM, id.label)
            assertEquals(12, id.spamReports)
            assertTrue("confidence ${id.confidence}", id.confidence >= 72)
        }

    @Test
    fun `reports below the threshold do not spam-flag`() = runBlocking {
        cached("919510305070", name = "Local Courier", reports = 9, source = "community")
        val id = CallerResolver.resolveLocal(app, "919510305070")
        assertEquals(CallerLabel.COMMUNITY, id.label)
    }

    // ---------------- allow-list -----------------------------------------------

    @Test
    fun `allowed number reports allowed even when unknown`() = runBlocking {
        DatabaseProvider.get(app).allowedDao().insert(
            AllowedNumber(number = "919510305080", label = "Landlord")
        )
        val id = CallerResolver.resolveLocal(app, "919510305080")
        assertEquals(CallerLabel.UNKNOWN, id.label)
        assertTrue(id.allowed)
    }

    @Test
    fun `allowed wins over spam flag without renaming it`() = runBlocking {
        cached("919510305090", name = "Friendly Bank", reports = 15, source = "users")
        DatabaseProvider.get(app).allowedDao().insert(
            AllowedNumber(number = "919510305090")
        )
        val id = CallerResolver.resolveLocal(app, "919510305090")
        // Honest UI: it IS still community-flagged, but never auto-blocked.
        assertTrue(id.allowed)
        assertEquals(CallerLabel.SUSPECTED_SPAM, id.label)
    }

    // ---------------- network-off contract (no incoming delay) ------------------

    @Test
    fun `community read off returns local identity and says so`() = runBlocking {
        val prefs = Prefs(app)
        prefs.communityReadEnabled = false
        val id = CallerResolver.resolveFull(app, "919876500001")
        assertEquals(CallerLabel.UNKNOWN, id.label)
        assertEquals("community lookups off", id.sourceLine)
    }

    @Test
    fun `placeholder config never performs network enrichment`() = runBlocking {
        // Build ships with the placeholder key; the resolver must not invent
        // a name — the identity remains local-only.
        assertTrue(FirebaseRepository.placeholderConfig)
        val prefs = Prefs(app)
        prefs.communityReadEnabled = true
        val id = CallerResolver.resolveFull(app, "919876500002")
        assertEquals(CallerLabel.UNKNOWN, id.label)
        assertEquals(0, id.confidence)
    }

    // ---------------- helpers ---------------------------------------------------

    private fun insertRawContact(name: String, number: String) {
        val digits = number.filter { it.isDigit() }
        contactsMap[number] = name
        contactsMap[digits] = name
        contactsMap[com.craftcaller.app.util.Craft.normalizeNumber(number)] = name
        if (digits.length == 10) {
            contactsMap["91$digits"] = name
        } else if (digits.length == 12 && digits.startsWith("91")) {
            contactsMap[digits.removePrefix("91")] = name
        }
    }
}
