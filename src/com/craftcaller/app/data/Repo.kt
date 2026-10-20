package com.craftcaller.app.data

import com.craftcaller.app.util.CallerHub
import com.craftcaller.app.util.PhoneUtils
import android.content.Context
import com.craftcaller.app.util.Cctx
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext

/** One observable snapshot of everything the UI renders (MVVM Model layer). */
data class CraftState(
    val calls: List<CallEntry> = emptyList(),
    val contacts: List<ContactRow> = emptyList(),
    val sms: List<SmsEntry> = emptyList(),
    val blocklist: List<BlockedNumber> = emptyList(),
    val topSpam: List<SpamReportEntry> = emptyList(),
    val cloudLabel: String = Cloud.repo.label,
    val cloudConfigured: Boolean = Cloud.repo.configured
)

/**
 * Single repository the ViewModels talk to. Wraps Db, system providers and the
 * Cloud community directory behind uniform suspend functions.
 */
object Repo {
    private val _state = MutableStateFlow(CraftState())
    val state: StateFlow<CraftState> = _state.asStateFlow()

    fun refresh() {
        _state.value = CraftState(
            calls = mergeCallHistory(),
            contacts = contacts(),
            sms = Db.smsInbox(),
            blocklist = Db.blockedList(),
            topSpam = Db.topReports(),
            cloudLabel = Cloud.repo.label,
            cloudConfigured = Cloud.repo.configured
        )
    }

    // ---------------- resolution ----------------

    suspend fun lookup(number: String): CallerInfo = withContext(Dispatchers.IO) {
        val n = PhoneUtils.normalize(number)
        if (n.isEmpty()) return@withContext CallerInfo(number = n)
        val fast = Db.fastLookup(n)
        if (fast.name.isNotEmpty() || fast.spam) return@withContext fast

        // not known locally → community database
        val cloud = runCatching { Cloud.repo.lookup(n) }.getOrNull()
        if (cloud != null) {
            Db.putUser(n, cloud.first, cloud.second, "cloud")
            Db.fastLookup(n).copy(score = (fast.score + 70).coerceAtMost(95))
        } else {
            fast
        }
    }

    // ---------------- misc UI actions ----------------

    suspend fun reportSpam(number: String, reason: String): Int = withContext(Dispatchers.IO) {
        val n = PhoneUtils.normalize(number)
        Db.putReport(n, 1, reason)
        // notify the community
        val serverCount = runCatching { Cloud.repo.reportSpam(n, reason) }.getOrNull()
        if (serverCount != null && serverCount > 0) {
            Db.putReport(n, 0, reason)
        }
        refresh()
        Db.report(n)
    }

    suspend fun block(number: String) = withContext(Dispatchers.IO) {
        val n = PhoneUtils.normalize(number)
        Db.putBlocked(n)
        CallerHub.forceRefresh()
        refresh()
    }

    suspend fun unblock(number: String) = withContext(Dispatchers.IO) {
        Db.removeBlocked(number)
        CallerHub.forceRefresh()
        refresh()
    }

    suspend fun addManualNumber(number: String, name: String): Boolean =
        withContext(Dispatchers.IO) {
            val n = PhoneUtils.normalize(number)
            if (n.isEmpty() || name.isEmpty()) return@withContext false
            Db.putUser(n, name, "", "manual")
            runCatching {
                Cloud.repo.publishDirectory(listOf(PublishEntry(number = n, name = name)))
            }
            refresh()
            true
        }

    // ---------------- contacts sync ----------------

    suspend fun syncContacts() = withContext(Dispatchers.IO) {
        val pairs = readSystemContacts(Cctx.app)
        Db.putContacts(pairs.distinctBy { it.second })
        val entries = pairs.map { PublishEntry(number = it.second, name = it.first) }
            .take(300)
        runCatching { Cloud.repo.publishDirectory(entries) }
        refresh()
    }

    fun contacts(): List<ContactRow> {
        val myNumber = Prefs.phoneNumber
        return Db.allContacts().map { row ->
            val info = Db.fastLookup(row.number)
            val badge = when {
                row.number == myNumber -> "You"
                info.spam -> "Spam"
                row.name.isNotEmpty() -> "Friend"
                else -> "Unknown"
            }
            row.copy(spam = info.spam, badge = badge)
        }
    }

    // ---------------- calls (system log + in-app log, deduped) ----------------

    private fun mergeCallHistory(): List<CallEntry> {
        val mine = Db.recentCalls()
        val blocklist = Db.blockedSet()
        return mine.map { entry ->
            val info = Db.fastLookup(entry.number)
            entry.copy(
                name = entry.name.ifEmpty { info.displayName.takeIf { it != "Unknown" } ?: "" },
                spam = info.spam || entry.number in blocklist
            )
        }
    }

    suspend fun importSystemCallLog() = withContext(Dispatchers.IO) {
        val sys = readSystemCallLog(Cctx.app, 120)
        val here = Db.recentCalls().mapTo(HashSet()) { it.number }
        val newOnes = sys.filter { it.number !in here }
        newOnes.forEach { e ->
            val info = Db.fastLookup(e.number)
            Db.putCall(e.number, e.name.ifEmpty { info.name }, e.ts, e.type, e.duration, info.spam)
        }
        refresh()
    }

    suspend fun recordFinishedCall(number: String, type: Int, duration: Long) =
        withContext(Dispatchers.IO) {
            val n = PhoneUtils.normalize(number)
            if (n.isNotEmpty()) {
                val info = Db.fastLookup(n)
                Db.putCall(n, info.name, System.currentTimeMillis(), type, duration, info.spam)
                refresh()
            }
        }
}

/** Tiny SharedPreferences wrapper (profile + settings). */
object Prefs {
    private val sp by lazy {
        Cctx.app.getSharedPreferences("craftcaller.prefs", Context.MODE_PRIVATE)
    }

    var darkMode: Boolean
        get() = sp.getBoolean("dark", true)
        set(v) = sp.edit().putBoolean("dark", v).apply()

    var onboarded: Boolean
        get() = sp.getBoolean("onboarded", false)
        set(v) = sp.edit().putBoolean("onboarded", v).apply()

    var premium: Boolean
        get() = sp.getBoolean("premium", false)
        set(v) = sp.edit().putBoolean("premium", v).apply()

    var autoBlockSpam: Boolean
        get() = sp.getBoolean("autoblock", true)
        set(v) = sp.edit().putBoolean("autoblock", v).apply()

    var who: String?
        get() = sp.getString("who", null)
        set(v) = sp.edit().putString("who", v).apply()

    var email: String?
        get() = sp.getString("email", null)
        set(v) = sp.edit().putString("email", v).apply()

    var phoneNumber: String?
        get() = sp.getString("phone", null)
        set(v) = sp.edit().putString("phone", v).apply()

    /** Six-digit verification code pending confirmation. */
    var pendingCode: String?
        get() = sp.getString("pending_code", null)
        set(v) = sp.edit().putString("pending_code", v).apply()
}
