package com.craftcaller.app;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

/**
 * CraftCaller local caller-ID database.
 *
 * Community-driven offline intelligence:
 * - numbers : user/community reports (tag + report counter)
 * - rules   : structural heuristics (own rules, e.g. the Indian "140"
 *             telemarketing prefix; no third-party datasets)
 * - blocked : personal blocklist honored by the screening service
 * - screenlog : decisions made for every screened call
 *
 * Score model (v1, offline):
 *   score = ruleWeight + reports
 *   >= 5  -> "Spam"           (verified by several reports)
 *   3..4  -> "Suspected Spam"
 *   else  -> reported, low trust
 */
public final class CallerDb extends SQLiteOpenHelper {

    public static final String TAG_SPAM = "Spam";
    public static final String TAG_SCAM = "Scam";
    public static final String TAG_TELEMARKETING = "Telemarketing";
    public static final String TAG_BUSINESS = "Business";
    public static final String TAG_REPORTED = "Reported";

    public static class Entry {
        public final String number;
        public final String name;
        public final String tag;      // nullable
        public final int reports;
        public final boolean blocked;
        public Entry(String number, String name, String tag, int reports, boolean blocked) {
            this.number = number; this.name = name; this.tag = tag;
            this.reports = reports; this.blocked = blocked;
        }
        public int score() { return reports; }
    }

    private static final String DB_NAME = "craftcaller.db";
    private static final int DB_VERSION = 2;

    private final Context ctx;

    public CallerDb(Context context) {
        super(context, DB_NAME, null, DB_VERSION);
        this.ctx = context;
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE numbers(" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                "number TEXT UNIQUE NOT NULL," +
                "name TEXT," +
                "tag TEXT," +
                "reports INTEGER DEFAULT 0," +
                "firstTs INTEGER," +
                "lastTs INTEGER)");
        db.execSQL("CREATE TABLE blocked(" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                "number TEXT UNIQUE NOT NULL," +
                "name TEXT," +
                "ts INTEGER)");
        db.execSQL("CREATE TABLE screenlog(" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                "ts INTEGER," +
                "number TEXT," +
                "decision TEXT," +
                "detail TEXT)");
        db.execSQL("CREATE TABLE rules(" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                "pattern TEXT," +       // prefix pattern, digits only
                "mode TEXT," +           // "prefix"
                "tag TEXT," +
                "weight INTEGER)");
        db.execSQL("CREATE TABLE settings(" +
                "key TEXT PRIMARY KEY," +
                "value TEXT)");
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldV, int newV) {
        if (oldV < 2) {
            db.execSQL("CREATE TABLE rules(" +
                    "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                    "pattern TEXT," +
                    "mode TEXT," +
                    "tag TEXT," +
                    "weight INTEGER)");
        }
    }

    /* ---------------- normalization ---------------- */

    /** Digits-only key. Keeps the last 10 digits for IN-style matching plus the full form. */
    public static String normalize(String raw) {
        if (raw == null) return "";
        String digits = raw.replaceAll("[^0-9]", "");
        if (digits.length() > 10) return digits.substring(digits.length() - 10);
        return digits;
    }

    /* ---------------- rules ---------------- */

    private void loadRules(SQLiteDatabase db) {
        db.execSQL("DELETE FROM rules");
        // Own structural heuristics only — no fabricated numbers.
        addRule(db, "140", TAG_TELEMARKETING, 3); // IN fixed-line telemarketing prefix
        addRule(db, "155", TAG_TELEMARKETING, 2); // IN telecom service short-series
    }

    private void addRule(SQLiteDatabase db, String pattern, String tag, int weight) {
        ContentValues cv = new ContentValues();
        cv.put("pattern", pattern);
        cv.put("mode", "prefix");
        cv.put("tag", tag);
        cv.put("weight", weight);
        db.insert("rules", null, cv);
    }

    /* ---------------- lookup & reporting ---------------- */

    private String matchedRuleTag(SQLiteDatabase db, String key) {
        Cursor c = db.rawQuery("SELECT tag, weight FROM rules WHERE ? LIKE pattern || '%'",
                new String[]{key});
        String tag = null;
        int best = -1;
        while (c != null && c.moveToNext()) {
            int w = c.getInt(1);
            if (w > best) { best = w; tag = c.getString(0); }
        }
        if (c != null) c.close();
        return tag != null ? tag : null;
    }

    /** Full classification for a number (rule tag if stronger than stored reports). */
    public Entry lookup(String raw) {
        String key = normalize(raw);
        if (key.length() < 4) return null;
        SQLiteDatabase db = getReadableDatabase();
        Entry e = null;
        Cursor c = db.rawQuery("SELECT number, name, tag, reports FROM numbers WHERE number=?",
                new String[]{key});
        if (c != null && c.moveToFirst()) {
            e = new Entry(c.getString(0), c.getString(1), c.getString(2), c.getInt(3), false);
        }
        if (c != null) c.close();

        boolean blocked = isBlocked(key);
        String ruleTag = matchedRuleTag(db, key);

        String tag;
        int reports = (e == null ? 0 : e.reports);
        if (reports >= 5) tag = e.tag != null ? e.tag : TAG_SPAM;
        else if (reports >= 2) tag = TAG_REPORTED;
        else tag = ruleTag;

        String name = e != null ? e.name : null;
        int score = reports;
        if (ruleTag != null) {
            score += 2;
            if (tag == null) tag = ruleTag;
        }
        return new Entry(key, name, tag, score, blocked);
    }

    /** True if this number should carry a warning banner in UI. */
    public static boolean isWarnTag(String tag) {
        return TAG_SPAM.equals(tag) || TAG_SCAM.equals(tag)
                || TAG_TELEMARKETING.equals(tag) || TAG_REPORTED.equals(tag);
    }

    /** Community report from this device or its owner. */
    public void report(String raw, String tag, String name) {
        String key = normalize(raw);
        if (key.length() < 4) return;
        SQLiteDatabase db = getWritableDatabase();
        db.execSQL("INSERT INTO numbers(number, name, tag, reports, firstTs, lastTs) " +
                "VALUES(?,?,?, 1, strftime('%s','now')*1000, strftime('%s','now')*1000) " +
                "ON CONFLICT(number) DO UPDATE SET " +
                "reports = reports + 1, " +
                "lastTs = strftime('%s','now')*1000, " +
                "tag = COALESCE(?, tag), " +
                "name = COALESCE(name, ?)",
                new Object[]{key, tag, name, tag, name});
    }

    /** Store a business/known-caller name. */
    public void saveName(String raw, String name, String tag) {
        String key = normalize(raw);
        if (key.length() < 4 || name == null || name.length() == 0) return;
        SQLiteDatabase db = getWritableDatabase();
        db.execSQL("INSERT INTO numbers(number, name, tag, reports, firstTs, lastTs) " +
                "VALUES(?, ?, COALESCE(?, 'Known'), 0, strftime('%s','now')*1000, strftime('%s','now')*1000) " +
                "ON CONFLICT(number) DO UPDATE SET name=?, tag=COALESCE(?, tag)",
                new Object[]{key, name, tag, name, tag});
    }

    /* ---------------- blocklist ---------------- */

    public boolean isBlocked(String raw) {
        String key = normalize(raw);
        SQLiteDatabase db = getReadableDatabase();
        Cursor c = db.rawQuery("SELECT 1 FROM blocked WHERE number=?", new String[]{key});
        boolean hit = c != null && c.moveToFirst();
        if (c != null) c.close();
        return hit;
    }

    public void block(String raw, String name) {
        String key = normalize(raw);
        if (key.length() < 4) return;
        SQLiteDatabase db = getWritableDatabase();
        ContentValues cv = new ContentValues();
        cv.put("number", key);
        cv.put("name", name);
        cv.put("ts", System.currentTimeMillis());
        db.insertWithOnConflict("blocked", null, cv, SQLiteDatabase.CONFLICT_IGNORE);
        if (name != null) saveName(key, name, TAG_SPAM);
    }

    public void unblock(String raw) {
        String key = normalize(raw);
        SQLiteDatabase db = getWritableDatabase();
        db.delete("blocked", "number=?", new String[]{key});
    }

    public Cursor listBlocked() {
        return getReadableDatabase().rawQuery(
                "SELECT number, name FROM blocked ORDER BY ts DESC", null);
    }

    /* ---------------- screening log ---------------- */

    public void logScreen(String raw, String decision, String detail) {
        SQLiteDatabase db = getWritableDatabase();
        ContentValues cv = new ContentValues();
        cv.put("ts", System.currentTimeMillis());
        cv.put("number", normalize(raw));
        cv.put("decision", decision);
        cv.put("detail", detail);
        db.insert("screenlog", null, cv);
    }

    public Cursor recentScreenLog(int limit) {
        return getReadableDatabase().rawQuery(
                "SELECT ts, number, decision, detail FROM screenlog ORDER BY ts DESC LIMIT "
                        + Math.max(1, limit), null);
    }

    /* ---------------- community top list ---------------- */

    public Cursor topSpammers(int limit) {
        return getReadableDatabase().rawQuery(
                "SELECT number, name, tag, reports FROM numbers " +
                "WHERE reports > 0 ORDER BY reports DESC, lastTs DESC LIMIT "
                        + Math.max(1, limit), null);
    }

    /* ---------------- contacts ---------------- */

    /** Resolve display name from the device contacts (READ_CONTACTS). */
    public String contactName(String raw) {
        String key = normalize(raw);
        if (key.length() < 5) return null;
        try {
            Cursor c = ctx.getContentResolver().query(
                    android.provider.ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                    new String[]{ android.provider.ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME },
                    null, null, null);
            String hit = null;
            if (c != null) {
                int col = c.getColumnIndex(android.provider.ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME);
                String nm;
                String ph;
                int phCol = c.getColumnIndex(android.provider.ContactsContract.CommonDataKinds.Phone.NUMBER);
                while (c.moveToNext()) {
                    nm = c.getString(col);
                    ph = phCol >= 0 ? c.getString(phCol) : null;
                    if (ph != null && key.equals(normalize(ph))) { hit = nm; break; }
                }
                c.close();
            }
            return hit;
        } catch (Exception e) {
            return null;
        }
    }

    /* ---------------- settings ---------------- */

    public boolean getBool(String key, boolean def) {
        Cursor c = getReadableDatabase().rawQuery(
                "SELECT value FROM settings WHERE key=?", new String[]{key});
        boolean v = def;
        if (c != null && c.moveToFirst()) {
            v = "1".equals(c.getString(0));
        }
        if (c != null) c.close();
        return v;
    }

    public void setBool(String key, boolean value) {
        SQLiteDatabase db = getWritableDatabase();
        ContentValues cv = new ContentValues();
        cv.put("key", key);
        cv.put("value", value ? "1" : "0");
        db.insertWithOnConflict("settings", null, cv, SQLiteDatabase.CONFLICT_REPLACE);
    }

    public int getInt(String key, int def) {
        Cursor c = getReadableDatabase().rawQuery(
                "SELECT value FROM settings WHERE key=?", new String[]{key});
        int v = def;
        if (c != null && c.moveToFirst()) {
            try { v = Integer.parseInt(c.getString(0)); } catch (Exception ignored) {}
        }
        if (c != null) c.close();
        return v;
    }

    public void setInt(String key, int value) {
        SQLiteDatabase db = getWritableDatabase();
        ContentValues cv = new ContentValues();
        cv.put("key", key);
        cv.put("value", String.valueOf(value));
        db.insertWithOnConflict("settings", null, cv, SQLiteDatabase.CONFLICT_REPLACE);
    }

    public boolean seedIfNeeded() {
        SQLiteDatabase db = getWritableDatabase();
        Cursor c = db.rawQuery("SELECT COUNT(*) FROM settings WHERE key='seeded'", null);
        boolean seeded = c != null && c.moveToFirst() && c.getInt(0) > 0 && "1".equals(c.getString(0));
        if (c != null) c.close();
        if (!seeded) {
            loadRules(db);
            ContentValues cv = new ContentValues();
            cv.put("key", "seeded");
            cv.put("value", "1");
            db.insertWithOnConflict("settings", null, cv, SQLiteDatabase.CONFLICT_REPLACE);
            return true;
        }
        return false;
    }
}
