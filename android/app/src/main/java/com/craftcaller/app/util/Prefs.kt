package com.craftcaller.app.util

import android.content.Context
import android.content.SharedPreferences

/** SharedPreferences-backed app settings, shared with services/receivers. */
class Prefs(context: Context) {

    private val appCtx = context.applicationContext

    private val sp: SharedPreferences =
        appCtx.getSharedPreferences("craftcaller_prefs", Context.MODE_PRIVATE)

    var darkTheme: Boolean
        get() = sp.getBoolean(KEY_DARK, false)
        set(value) = sp.edit().putBoolean(KEY_DARK, value).apply()

    var userEmail: String
        get() = sp.getString(KEY_EMAIL, "").orEmpty()
        set(value) = sp.edit().putString(KEY_EMAIL, value).apply()

    var userPhone: String
        get() = sp.getString(KEY_PHONE, "").orEmpty()
        set(value) = sp.edit().putString(KEY_PHONE, value).apply()

    var uid: String
        get() = sp.getString(KEY_UID, "").orEmpty()
        set(value) = sp.edit().putString(KEY_UID, value).apply()

    var onboardingDone: Boolean
        get() = sp.getBoolean(KEY_ONBOARDING, false)
        set(value) = sp.edit().putBoolean(KEY_ONBOARDING, value).apply()

    /** Auto-block numbers reported as spam by >=10 users. */
    var autoBlockSpammers: Boolean
        get() = sp.getBoolean(KEY_AUTO_BLOCK_SPAM, true)
        set(value) = sp.edit().putBoolean(KEY_AUTO_BLOCK_SPAM, value).apply()

    var lastRingingNumber: String
        get() = sp.getString(KEY_LAST_RINGING, "").orEmpty()
        set(value) = sp.edit().putString(KEY_LAST_RINGING, value).apply()

    var lastRingingTime: Long
        get() = sp.getLong(KEY_LAST_RING_TIME, 0L)
        set(value) = sp.edit().putLong(KEY_LAST_RING_TIME, value).apply()

    var callWasAnswered: Boolean
        get() = sp.getBoolean(KEY_ANSWERED, false)
        set(value) = sp.edit().putBoolean(KEY_ANSWERED, value).apply()

    /** Stack trace of the last uncaught crash; shown in-app on the next launch. */
    var lastCrash: String
        get() = sp.getString(KEY_LAST_CRASH, "").orEmpty()
        set(value) = sp.edit().putString(KEY_LAST_CRASH, value).apply()

    // ---------------- Personal profile (securely stored; onboarding/Settings) ---

    /**
     * The user's display name. Read/write is routed through [SecurePrefs]
     * (encrypted at rest). The legacy plain-`craftcaller_prefs` copy is
     * transparently migrated: first non-empty secure value wins; a write
     * always lands in the secure store.
     */
    var userName: String
        get() {
            val secure = SecurePrefs.string(appCtx, KEY_SECURE_NAME)
            if (secure.isNotEmpty()) return secure
            val legacy = sp.getString(KEY_NAME, "").orEmpty()
            if (legacy.isNotEmpty()) SecurePrefs.putString(appCtx, KEY_SECURE_NAME, legacy)
            return legacy
        }
        set(value) {
            SecurePrefs.putString(appCtx, KEY_SECURE_NAME, value)
        }

    /** Gender chosen during onboarding (default honorific source). */
    var gender: Gender
        get() = Gender.fromKey(SecurePrefs.string(appCtx, KEY_SECURE_GENDER))
        set(value) = SecurePrefs.putString(appCtx, KEY_SECURE_GENDER, value.key)

    /** How the user is addressed in announcements ("sir"/"ma'am"/"ji"/none). */
    var honorific: Honorific
        get() = Honorific.fromKey(SecurePrefs.string(appCtx, KEY_SECURE_HONORIFIC, "unset"))
        set(value) = SecurePrefs.putString(appCtx, KEY_SECURE_HONORIFIC, value.key)

    /** Announcement phrasing language (Hindi / Hinglish / English). */
    var announceLang: AnnounceLang
        get() = AnnounceLang.fromKey(SecurePrefs.string(appCtx, KEY_SECURE_ANNOUNCE_LANG))
        set(value) = SecurePrefs.putString(appCtx, KEY_SECURE_ANNOUNCE_LANG, value.key)

    /** Chosen announcer voice (independent from honorific). */
    var announcerVoiceName: String
        get() = SecurePrefs.string(appCtx, KEY_SECURE_VOICE_NAME)
        set(value) = SecurePrefs.putString(appCtx, KEY_SECURE_VOICE_NAME, value)

    /** Locale tag used when the chosen voice name is not installed anymore. */
    var announcerVoiceLang: String
        get() = SecurePrefs.string(appCtx, KEY_SECURE_VOICE_LANG, AnnounceLang.HINDI.tag)
        set(value) = SecurePrefs.putString(appCtx, KEY_SECURE_VOICE_LANG, value)

    /** Whether caller announcements are enabled at ring time (service hook). */
    var announceCallerEnabled: Boolean
        get() = SecurePrefs.bool(appCtx, KEY_SECURE_ANNOUNCE_ENABLED, true)
        set(value) = SecurePrefs.putBool(appCtx, KEY_SECURE_ANNOUNCE_ENABLED, value)

    /** True once the personal-profile onboarding has been completed or skipped. */
    var profileReady: Boolean
        get() = SecurePrefs.bool(appCtx, KEY_SECURE_PROFILE_READY, false)
        set(value) = SecurePrefs.putBool(appCtx, KEY_SECURE_PROFILE_READY, value)

    // ---------------- Announcement routing (which callers get announced) ------

    /** Announce calls from saved contacts / numbers the app already knows. */
    var announceSavedContacts: Boolean
        get() = SecurePrefs.bool(appCtx, KEY_SECURE_ANNOUNCE_SAVED, true)
        set(value) = SecurePrefs.putBool(appCtx, KEY_SECURE_ANNOUNCE_SAVED, value)

    /** Announce calls from numbers with no known name ("unknown number"). */
    var announceUnknown: Boolean
        get() = SecurePrefs.bool(appCtx, KEY_SECURE_ANNOUNCE_UNKNOWN, true)
        set(value) = SecurePrefs.putBool(appCtx, KEY_SECURE_ANNOUNCE_UNKNOWN, value)

    /** Add "2nd/3rd call" phrasing for numbers that rang again. */
    var announceRepeat: Boolean
        get() = SecurePrefs.bool(appCtx, KEY_SECURE_ANNOUNCE_REPEAT, true)
        set(value) = SecurePrefs.putBool(appCtx, KEY_SECURE_ANNOUNCE_REPEAT, value)

    /** Consent switch: read contact names for announcements, else use app data only. */
    var announceUseContacts: Boolean
        get() = SecurePrefs.bool(appCtx, KEY_SECURE_ANNOUNCE_CONTACTS, true)
        set(value) = SecurePrefs.putBool(appCtx, KEY_SECURE_ANNOUNCE_CONTACTS, value)

    /** TTS speed multiplier (0.5–2.0). */
    var announceRate: Float
        get() = SecurePrefs.float(appCtx, KEY_SECURE_ANNOUNCE_RATE, 1.0f).coerceIn(0.5f, 2.0f)
        set(value) = SecurePrefs.putFloat(appCtx, KEY_SECURE_ANNOUNCE_RATE, value.coerceIn(0.5f, 2.0f))

    /** TTS pitch multiplier (0.5–2.0). */
    var announcePitch: Float
        get() = SecurePrefs.float(appCtx, KEY_SECURE_ANNOUNCE_PITCH, 1.1f).coerceIn(0.5f, 2.0f)
        set(value) = SecurePrefs.putFloat(appCtx, KEY_SECURE_ANNOUNCE_PITCH, value.coerceIn(0.5f, 2.0f))

    // ---------------- Audio modes (music/ringtones module) -------------------

    /** Alert style key (see AlertStyle); "voice_only" plays no music. */
    var alertStyle: String
        get() = SecurePrefs.string(appCtx, KEY_SECURE_ALERT_STYLE, "chime")
        set(value) = SecurePrefs.putString(appCtx, KEY_SECURE_ALERT_STYLE, value)

    /** Announcement loudness, 0..1 multiplier applied to alert + speech. */
    var alertVolume: Float
        get() = SecurePrefs.float(appCtx, KEY_SECURE_ALERT_VOLUME, 0.9f).coerceIn(0f, 1f)
        set(value) = SecurePrefs.putFloat(appCtx, KEY_SECURE_ALERT_VOLUME, value.coerceIn(0f, 1f))

    /** Vibrate when an announcement plays (silence-vibrate modes included). */
    var vibrateOnAnnounce: Boolean
        get() = SecurePrefs.bool(appCtx, KEY_SECURE_VIBRATE_ANNOUNCE, true)
        set(value) = SecurePrefs.putBool(appCtx, KEY_SECURE_VIBRATE_ANNOUNCE, value)

    /** Vibration style: "short" (single pulse) or "double" (2 pulses). */
    var vibrationPattern: String
        get() = SecurePrefs.string(appCtx, KEY_SECURE_VIBRATION_PATTERN, "double")
        set(value) = SecurePrefs.putString(appCtx, KEY_SECURE_VIBRATION_PATTERN, value)

    /**
     * Headphone/Bluetooth preference: false = suppress audible alerts while any
     * wired/BT audio output is active; true = announce on the active route.
     */
    var announceWithHeadphones: Boolean
        get() = SecurePrefs.bool(appCtx, KEY_SECURE_ANNOUNCE_HEADPHONES, true)
        set(value) = SecurePrefs.putBool(appCtx, KEY_SECURE_ANNOUNCE_HEADPHONES, value)

    /** Display name of the imported custom alert ("" when none imported). */
    var customAlertName: String
        get() = SecurePrefs.string(appCtx, KEY_SECURE_CUSTOM_ALERT_NAME)
        set(value) = SecurePrefs.putString(appCtx, KEY_SECURE_CUSTOM_ALERT_NAME, value)

    // ---------------- Voice engine & speech template -------------------------

    /**
     * Package name of the selected text-to-speech ENGINE ("" = Android system
     * default engine). Applied by every TTS creation point in this app:
     * ring-time [AnnouncerVoice] and screen-side [VoiceKit] previews.
     */
    var ttsEngineName: String
        get() = SecurePrefs.string(appCtx, KEY_SECURE_TTS_ENGINE)
        set(value) = SecurePrefs.putString(appCtx, KEY_SECURE_TTS_ENGINE, value)

    /**
     * Custom speech template ("" = built-in per-language phrasing).
     * Supported tokens: {you}=addressed user, {caller}=caller name/number
     * (falls back to an "unknown number" phrase when there is no name),
     * {repeat}=ordinal ("doosri"/"2nd") or "" on a first ring. Everything
     * else is literal text. Applied at ring time by [Announcer.greeting].
     */
    var announceTemplate: String
        get() = SecurePrefs.string(appCtx, KEY_SECURE_ANNOUNCE_TEMPLATE)
        set(value) = SecurePrefs.putString(appCtx, KEY_SECURE_ANNOUNCE_TEMPLATE, value)

    // ---------------- Device diagnostics (troubleshooting) -------------------

    /** Epoch ms of the last completed call-screening decision (0 = never). */
    var lastScreeningAt: Long
        get() = sp.getLong(KEY_LAST_SCREENING_AT, 0L)
        set(value) = sp.edit().putLong(KEY_LAST_SCREENING_AT, value).apply()

    /** Compact human summary of the last screening decision. */
    var lastScreeningSummary: String
        get() = sp.getString(KEY_LAST_SCREENING_SUMMARY, "").orEmpty()
        set(value) = sp.edit().putString(KEY_LAST_SCREENING_SUMMARY, value).apply()

    // ---------------- Community caller-ID & privacy --------------------------

    /**
     * Opt-in READ access to the community directory (lookup numbers on
     * demand). Read-only: nothing personal leaves the phone for a lookup.
     * Default on, off-switchable.
     */
    var communityReadEnabled: Boolean
        get() = SecurePrefs.bool(appCtx, KEY_SECURE_COMMUNITY_READ, true)
        set(value) = SecurePrefs.putBool(appCtx, KEY_SECURE_COMMUNITY_READ, value)

    /**
     * Explicit informed consent for CONTRIBUTING data (phonebook names / your
     * saved names) to the community directory. Default OFF — the phonebook is
     * never uploaded without this switch being turned on by the user inside
     * an explainer.
     */
    var communityUploadConsent: Boolean
        get() = SecurePrefs.bool(appCtx, KEY_SECURE_COMMUNITY_UPLOAD, false)
        set(value) = SecurePrefs.putBool(appCtx, KEY_SECURE_COMMUNITY_UPLOAD, value)

    /** Ring count while the latest ring is still inside REPEAT_WINDOW_MS. */
    fun repeatCount(number: String): Int {
        val now = System.currentTimeMillis()
        val entry = repeatMap()[number] ?: return 0
        return if (now - entry.second > REPEAT_WINDOW_MS) 0 else entry.first
    }

    /**
     * Registers a fresh ring and returns the resulting ring count.
     *
     * - Count is stored per number as `count@lastRingTs` (encrypted prefs).
     * - Rings arriving within [SAME_RING_GRACE_MS] of each other count as ONE
     *   ring event (PHONE_STATE receiver + CallScreeningService both observe
     *   the same incoming call).
     * - The chain resets after REPEAT_WINDOW_MS without a ring.
     */
    fun bumpRepeat(number: String): Int {
        val now = System.currentTimeMillis()
        val map = repeatMap().toMutableMap()
        val prev = map[number]
        val count = when {
            prev == null -> 1
            now - prev.second < SAME_RING_GRACE_MS -> return prev.first  // same ring event
            now - prev.second > REPEAT_WINDOW_MS -> 1                    // stale window: reset
            else -> prev.first + 1
        }
        map[number] = count to now
        // Keep the 50 most recent numbers so the stored blob stays bounded.
        if (map.size > 50) {
            val keep = map.entries
                .sortedByDescending { it.value.second }
                .take(50)
                .toList()
            map.clear()
            keep.forEach { map[it.key] = it.value }
        }
        val stored = map.entries.joinToString(";") { "${it.key}=${it.value.first}@${it.value.second}" }
        SecurePrefs.putString(appCtx, KEY_SECURE_REPEATS, stored)
        return count
    }

    private fun repeatMap(): Map<String, Pair<Int, Long>> {
        val raw = SecurePrefs.string(appCtx, KEY_SECURE_REPEATS)
        if (raw.isEmpty()) return emptyMap()
        return raw.split(";").mapNotNull {
            val eq = it.indexOf('=')
            val at = it.lastIndexOf('@')
            if (eq <= 0 || at < eq) return@mapNotNull null
            val count = it.substring(eq + 1, at).toIntOrNull() ?: return@mapNotNull null
            val ts = it.substring(at + 1).toLongOrNull() ?: return@mapNotNull null
            it.substring(0, eq) to (count to ts)
        }.toMap()
    }

    companion object {
        private const val KEY_DARK = "dark_theme"
        private const val KEY_NAME = "user_name"
        private const val KEY_EMAIL = "user_email"
        private const val KEY_PHONE = "user_phone"
        private const val KEY_UID = "uid"
        private const val KEY_ONBOARDING = "onboarding_done"
        private const val KEY_AUTO_BLOCK_SPAM = "auto_block_spammers"
        private const val KEY_LAST_RINGING = "last_ringing_number"
        private const val KEY_LAST_RING_TIME = "last_ringing_time"
        private const val KEY_ANSWERED = "call_answered"
        private const val KEY_LAST_CRASH = "last_crash"

        // Secure-store keys (EncryptedSharedPreferences, see SecurePrefs).
        private const val KEY_SECURE_NAME = "profile_name"
        private const val KEY_SECURE_GENDER = "profile_gender"
        private const val KEY_SECURE_HONORIFIC = "profile_honorific"
        private const val KEY_SECURE_ANNOUNCE_LANG = "profile_announce_lang"
        private const val KEY_SECURE_ANNOUNCE_ENABLED = "profile_announce_enabled"
        private const val KEY_SECURE_VOICE_NAME = "announcer_voice_name"
        private const val KEY_SECURE_VOICE_LANG = "announcer_voice_lang"
        private const val KEY_SECURE_PROFILE_READY = "profile_setup_ready"

        private const val KEY_SECURE_ANNOUNCE_SAVED = "announce_saved_contacts"
        private const val KEY_SECURE_ANNOUNCE_UNKNOWN = "announce_unknown_callers"
        private const val KEY_SECURE_ANNOUNCE_REPEAT = "announce_repeat_callers"
        private const val KEY_SECURE_ANNOUNCE_CONTACTS = "announce_use_contacts"
        private const val KEY_SECURE_ANNOUNCE_RATE = "announce_rate"
        private const val KEY_SECURE_ANNOUNCE_PITCH = "announce_pitch"
        private const val KEY_SECURE_REPEATS = "repeat_ring_counts"

        /** Rings within this window count towards "repeat caller". */
        val REPEAT_WINDOW_MS: Long = 24 * 60 * 60 * 1000L

        /** Max gap between two tray events of the SAME ring (dedupe window). */
        const val SAME_RING_GRACE_MS: Long = 10_000L

        // Audio-mode secure keys.
        private const val KEY_SECURE_ALERT_STYLE = "alert_style"
        private const val KEY_SECURE_ALERT_VOLUME = "alert_volume"
        private const val KEY_SECURE_VIBRATE_ANNOUNCE = "vibrate_on_announce"
        private const val KEY_SECURE_VIBRATION_PATTERN = "vibration_pattern"
        private const val KEY_SECURE_ANNOUNCE_HEADPHONES = "announce_with_headphones"
        private const val KEY_SECURE_CUSTOM_ALERT_NAME = "custom_alert_name"
        private const val KEY_SECURE_COMMUNITY_READ = "community_read_enabled"
        private const val KEY_SECURE_COMMUNITY_UPLOAD = "community_upload_consent"
        private const val KEY_SECURE_TTS_ENGINE = "tts_engine_package"
        private const val KEY_SECURE_ANNOUNCE_TEMPLATE = "announce_template"
        private const val KEY_LAST_SCREENING_AT = "last_screening_at"
        private const val KEY_LAST_SCREENING_SUMMARY = "last_screening_summary"
    }
}
