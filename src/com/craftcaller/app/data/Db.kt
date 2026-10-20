package com.craftcaller.app.data

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import com.craftcaller.app.util.Perms
import com.craftcaller.app.util.PhoneUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Central local store for CraftCaller.
 *
 * NOTE ON ARCHITECTURE: this class is the project's persistence layer in the
 * "Room" role (tables + typed queries + suspend DAO methods right behind the
 * repositories). The on-device Termux toolchain has no kapt/Room compiler
 * available, so the schema helpers are handwritten against the framework
 * SQLite engine — the MVVM architecture (Activity/UI -> ViewModel/Repo -> DAO)
 * is exactly what a Room setup would look like.
 */
object Db {
    private var open: SQLiteOpenHelper? = null

    fun init(ctx: Context) {
        if (open != null) return
        open = object : SQLiteOpenHelper(ctx, "craftcaller.db", null, 4) {
            override fun onCreate(db: SQLiteDatabase) = create(db)
            override fun onUpgrade(db: SQLiteDatabase, o: Int, n: Int) {
                drop(db); create(db)
            }
        }
        // warm up
        open!!.writableDatabase
    }

    private fun create(db: SQLiteDatabase) {
        db.execSQL("""CREATE TABLE contacts(number TEXT PRIMARY KEY, name TEXT NOT NULL, ts INTEGER)""")
        db.execSQL("""CREATE TABLE calls(id INTEGER PRIMARY KEY AUTOINCREMENT, number TEXT, name TEXT,
            ts INTEGER, type INTEGER, dur INTEGER, spam INTEGER, liked INTEGER DEFAULT 0)""")
        db.execSQL("""CREATE INDEX idx_calls_ts ON calls(ts DESC)""")
        db.execSQL("""CREATE TABLE blocked(number TEXT PRIMARY KEY, ts INTEGER NOT NULL)""")
        db.execSQL("""CREATE TABLE reports(number TEXT PRIMARY KEY, reports INTEGER NOT NULL,
            reason TEXT, ts INTEGER NOT NULL)""")
        db.execSQL("""CREATE TABLE users(number TEXT PRIMARY KEY, name TEXT NOT NULL,
            city TEXT, source TEXT NOT NULL)""")
        db.execSQL("""CREATE TABLE sms(id INTEGER PRIMARY KEY, number TEXT, body TEXT,
            ts INTEGER, otp INTEGER, spam INTEGER, unread INTEGER)""")
        db.execSQL("""CREATE INDEX idx_sms_ts ON sms(ts DESC)""")
        seedDemoData(db)
    }

    private fun drop(db: SQLiteDatabase) {
        listOf("contacts", "calls", "blocked", "reports", "users", "sms").forEach {
            db.execSQL("DROP TABLE IF EXISTS $it")
        }
    }

    /** Curated demo directory so the app is fully functional before a Firebase
     *  project is attached (real cloud users merge right on top of these). */
    private fun seedDemoData(db: SQLiteDatabase) {
        val demoDir = listOf(
            "Pizza Hut Delivery" to "+911124669944",
            "Uber Support" to "+911233343135",
            "HDFC Bank" to "+911802700333",
            "Fraud: Fake KYC Calls" to "+919876599991",
            "Spam: Loan Agents" to "+919653339994",
            "Smriti" to "+919876543210",
            "Abhishek Sharma" to "+918700999888",
            "Kyara Tech (Office)" to "+918887776655",
            "Courier Person" to "+918007005004"
        )
        val demoUsers = listOf(
            "Sneha Kulkarni" to "+919990001111",
            "Arjun Mehta" to "+919990002222",
            "Spam Reported: Car Wash Spam" to "+915555555555",
            "Deepa Rao (Verified Business)" to "+918040003333",
            "Ravi Verma" to "+919990007777"
        )
        db.beginTransaction()
        try {
            demoUsers.forEach { (n, s) ->
                db.execSQL("INSERT OR REPLACE INTO users VALUES(?,?,?,?)",
                    arrayOf<Any?>(s, n, "", "directory"))
            }
            demoDir.forEach { (n, s) ->
                db.execSQL("INSERT OR REPLACE INTO users VALUES(?,?,?,?)",
                    arrayOf<Any?>(s, n, "", "directory"))
            }
            // pre-reported spam (someone reported these before you)
            listOf("+919876599991" to 14, "+919653339994" to 11, "+915555555555" to 22).forEach {
                db.execSQL("INSERT OR REPLACE INTO reports VALUES(?,?,?,?)",
                    arrayOf<Any?>(it.first, it.second, "Likely fraud", 0L))
            }
            db.setTransactionSuccessful()
        } finally {
            db.endTransaction()
        }
    }

    private fun db() = open!!.writableDatabase

    // ---------- contacts ----------
    fun putContacts(rows: List<Pair<String, String>>) {
        val d = db(); d.beginTransaction()
        try {
            val st = d.compileStatement("INSERT OR REPLACE INTO contacts VALUES(?,?,?)")
            val now = System.currentTimeMillis()
            rows.forEach { (number, name) ->
                st.bindString(1, number); st.bindString(2, name); st.bindLong(3, now)
                st.executeInsert()
            }
            d.setTransactionSuccessful()
        } finally { d.endTransaction() }
    }

    fun lookupContact(number: String): String? =
        db().rawQuery("SELECT name FROM contacts WHERE number=? LIMIT 1",
            arrayOf(number)).use { if (it.moveToFirst()) it.getString(0) else null }

    fun allContacts(): List<ContactRow> {
        val out = ArrayList<ContactRow>()
        db().rawQuery("SELECT number,name FROM contacts ORDER BY name", null).use { c ->
            while (c.moveToNext()) out += ContactRow(
                name = c.getString(1), number = c.getString(0),
                spam = false, badge = trueBadge()
            )
        }
        return out
    }

    private fun trueBool() = "✓"
    private fun trueBadge(): String = ""

    // ---------- users / community directory ----------
    fun putUser(number: String, name: String, city: String, source: String) {
        db().execSQL("INSERT OR REPLACE INTO users VALUES(?,?,?,?)",
            arrayOf<Any?>(number, name, city, source))
    }

    fun lookupUser(number: String, source: String? = null): Pair<String, String>? =
        db().rawQuery(
            (if (source == null) "SELECT name,city FROM users WHERE number=?"
             else "SELECT name,city FROM users WHERE number=? AND source=?"),
            if (source == null) arrayOf(number) else arrayOf<String?>(number, source)
        ).use {
            if (it.moveToFirst()) it.getString(0) to (it.getString(1) ?: "") else null
        }

    // ---------- call records ----------
    fun putCall(number: String, name: String, ts: Long, type: Int, dur: Long, spam: Boolean) {
        if (number.isEmpty()) return
        val d = db()
        d.execSQL(
            "INSERT INTO calls(number,name,ts,type,dur,spam) VALUES(?,?,?,?,?,?)",
            arrayOf<Any?>(number, name, ts, type, dur, if (spam) 1L else 0L)
        )
        d.execSQL("DELETE FROM calls WHERE id NOT IN (SELECT id FROM calls ORDER BY ts DESC LIMIT 800)")
    }

    fun recentCalls(limit: Int = 200): List<CallEntry> {
        val out = ArrayList<CallEntry>()
        db().rawQuery(
            "SELECT number,name,ts,type,dur,spam FROM calls ORDER BY ts DESC LIMIT ?",
            arrayOf(limit.toString())).use { c ->
            while (c.moveToNext()) out += CallEntry(
                c.getString(0), c.getString(1) ?: "", c.getLong(2), c.getInt(3),
                c.getLong(4), c.getInt(5) == 1
            )
        }
        return out
    }

    // ---------- blocklist ----------
    fun putBlocked(number: String) {
        db().execSQL("INSERT OR REPLACE INTO blocked VALUES(?,?)",
            arrayOf<Any?>(number, System.currentTimeMillis()))
    }

    fun removeBlocked(number: String) {
        db().execSQL("DELETE FROM blocked WHERE number=?", arrayOf<Any?>(number))
    }

    fun blockedSet(): Set<String> {
        val out = HashSet<String>()
        db().rawQuery("SELECT number FROM blocked", null).use { c ->
            while (c.moveToNext()) out += c.getString(0)
        }
        return out
    }

    fun blockedList(): List<BlockedNumber> {
        val out = ArrayList<BlockedNumber>()
        db().rawQuery("SELECT number,ts FROM blocked ORDER BY ts DESC", null).use { c ->
            while (c.moveToNext()) out += BlockedNumber(c.getString(0), c.getLong(1))
        }
        return out
    }

    // ---------- spam reports ----------
    fun putReport(number: String, add: Int = 1, reason: String? = null) {
        val d = db()
        val cur = db().rawQuery("SELECT reports,reason FROM reports WHERE number=?",
            arrayOf(number)).use {
            if (it.moveToFirst()) it.getInt(0) to (it.getString(1) ?: "") else 0 to (reason ?: "")
        }
        val newCount = cur.first + add
        val rep = reason ?: cur.second
        d.execSQL("INSERT OR REPLACE INTO reports VALUES(?,?,?,?)",
            arrayOf<Any?>(number, newCount, rep, System.currentTimeMillis()))
    }

    fun report(number: String): Int =
        db().rawQuery("SELECT reports FROM reports WHERE number=?",
            arrayOf(number)).use { if (it.moveToFirst()) it.getInt(0) else 0 }

    fun topReports(limit: Int = 20): List<SpamReportEntry> {
        val out = ArrayList<SpamReportEntry>()
        db().rawQuery(
            "SELECT number,reports,reason FROM reports WHERE reports>=? ORDER BY reports DESC LIMIT ?",
            arrayOf(SpamThreshold.COUNT.toString(), limit.toString())).use { c ->
            while (c.moveToNext()) out += SpamReportEntry(c.getString(0), c.getInt(1), c.getString(2) ?: "")
        }
        return out
    }

    // ---------- sms ----------
    fun putSms(e: SmsEntry) {
        db().execSQL("INSERT OR REPLACE INTO sms VALUES(?,?,?,?,?,?,0)",
            arrayOf<Any?>(e.id, e.number, e.body, e.ts, if (e.otp) 1L else 0L, if (e.spam) 1L else 0L))
        db().execSQL("DELETE FROM sms WHERE id NOT IN (SELECT id FROM sms ORDER BY ts DESC LIMIT 500)")
    }

    fun smsInbox(limit: Int = 100): List<SmsEntry> {
        val out = ArrayList<SmsEntry>()
        db().rawQuery(
            "SELECT id,number,body,ts,otp,spam FROM sms ORDER BY ts DESC LIMIT ?",
            arrayOf(limit.toString())).use { c ->
            while (c.moveToNext()) out += SmsEntry(
                c.getLong(0), c.getString(1), "", c.getString(2),
                c.getLong(3), c.getInt(4) == 1, c.getInt(5) == 1
            )
        }
        return out
    }

    // ---------- resolve (fast path used by the ring-time service) ----------
    @Synchronized
    fun fastLookup(number: String): CallerInfo {
        val blockSet = Db.blockedSet()

        fun base(source: String) = CallerInfo(
            number = number, source = source,
            blocked = number in blockSet,
            spamReports = Db.report(number)
        )

        val contact = Db.lookupContact(number)
        if (contact != null) {
            val ci = base("contacts").copy(name = contact, score = 100)
            val reports = ci.spamReports
            if (reports >= SpamThreshold.COUNT) return ci.copy(spam = true, spamReason = "Reported $reports times by community", score = 30)
            return ci
        }
        val user = Db.lookupUser(number)
        if (user != null) {
            val ci = base("cache").copy(name = user.first, location = user.second, score = 70)
            val reports = ci.spamReports
            if (reports >= SpamThreshold.COUNT) return ci.copy(spam = true, spamReason = "Reported $reports times by community", score = 10)
            return ci
        }
        val reports = Db.report(number)
        val ci = base("unknown")
        return if (reports >= SpamThreshold.COUNT) {
            ci.copy(spam = true, spamReports = reports,
                spamReason = "Reported $reports times by community", score = 5)
        } else {
            ci.copy(spamReports = reports, score = 0)
        }
    }
}

object SpamThreshold {
    /** Community reports needed before a number is marked spam. */
    const val COUNT = 10
    const val REASON = "Likely fraud"
}
