package com.craftcaller.app.service

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.database.Cursor
import android.net.Uri
import android.provider.ContactsContract
import android.util.Log
import androidx.core.content.ContextCompat
import com.craftcaller.app.data.local.DatabaseProvider
import com.craftcaller.app.data.local.entity.CallerInfo
import com.craftcaller.app.tts.AnnouncerVoice
import com.craftcaller.app.util.AnnounceKind
import com.craftcaller.app.util.AnnounceLang
import com.craftcaller.app.util.Announcer
import com.craftcaller.app.util.Craft
import com.craftcaller.app.util.Prefs

/**
 * Turns an incoming call into a spoken personal announcement.
 *
 * Used by BOTH detection paths:
 *   - CraftCallScreeningService (system screening role granted — primary), and
 *   - CallReceiver PHONE_STATE (fallback while no screening role is held).
 *
 * Guarantees:
 *   - reads contact names ONLY when READ_CONTACTS is granted AND the user's
 *     announcement-consent switch is on; otherwise uses app data (Room cache),
 *   - never performs network I/O here (ringing must not be delayed);
 *   - every failure path stays silent (log only) — a call never breaks
 *     because of the announcer.
 */
object AnnouncementEngine {

    private const val TAG = "AnnouncementEngine"

    /** Test hook for headless/Robolectric environments without a ContactsProvider. */
    @Volatile
    var contactLookupOverride: ((Context, String) -> String?)? = null

    /**
     * Contact-name lookup honoring consent + permission. Returns null when the
     * user has not enabled contact usage, permission is missing, or no match.
     */
    fun lookupContactName(context: Context, number: String): String? {
        val prefs = Prefs(context)
        if (!prefs.announceUseContacts) return null
        val granted = ContextCompat.checkSelfPermission(
            context, Manifest.permission.READ_CONTACTS
        ) == PackageManager.PERMISSION_GRANTED
        if (!granted) return null

        contactLookupOverride?.let { return it(context, number) }

        return runCatching {
            val uri = Uri.withAppendedPath(
                ContactsContract.PhoneLookup.CONTENT_FILTER_URI,
                Uri.encode(number)
            )
            context.contentResolver.query(
                uri,
                arrayOf(ContactsContract.PhoneLookup.DISPLAY_NAME),
                null, null, null
            )?.use { cursor: Cursor ->
                if (cursor.moveToFirst()) cursor.getString(0) else null
            }
        }.getOrNull().also {
            if (it != null) Log.d(TAG, "contact name resolved for $number")
        }
    }

    /**
     * Decides whether/how to announce AFTER the caller's name is (locally)
     * known. Bumps the repeat counter, picks the announcement kind per user
     * switches, and speaks via [AnnouncerVoice].
     *
     * @param displayName saved contact or app-database name (null when unknown)
     * @param spamReports community spam report count for the number
     */
    fun maybeAnnounce(
        context: Context,
        number: String,
        displayName: String?,
        spamReports: Int = 0
    ) {
        runCatching {
            val prefs = Prefs(context)
            if (!prefs.announceCallerEnabled) {
                Log.d(TAG, "announcements disabled by user")
                return
            }
            if (spamReports >= Craft.SPAM_REPORTS_THRESHOLD) {
                Log.d(TAG, "skip announce: flagged spam ($spamReports reports)")
                return
            }

            val ringCount = prefs.bumpRepeat(number)
            val known = !displayName.isNullOrBlank()
            val kind = when {
                ringCount >= 2 && prefs.announceRepeat -> AnnounceKind.REPEAT
                known && prefs.announceSavedContacts -> AnnounceKind.SAVED
                !known && prefs.announceUnknown -> AnnounceKind.UNKNOWN
                else -> {
                    Log.d(TAG, "skip announce: kind not enabled (known=$known, count=$ringCount)")
                    return
                }
            }

            // How the device should refer to the caller in speech.
            val callerLabel = when {
                known -> displayName!!.trim()
                kind == AnnounceKind.REPEAT -> Craft.prettyNumber(number)
                else -> "" // UNKNOWN template does not need the number
            }

            // How the user is addressed falls back gracefully when no name exists.
            val rawName = prefs.userName.trim()
            val addressed = if (rawName.isEmpty()) {
                if (prefs.announceLang == AnnounceLang.ENGLISH) "You" else "Aap"
            } else {
                Announcer.addressed(rawName, prefs.honorific)
            }
            val text = Announcer.greeting(
                addressed, callerLabel, prefs.announceLang, kind, ringCount,
                customTemplate = prefs.announceTemplate
            )
            Log.d(TAG, "announce kind=$kind count=$ringCount for $number")
            // AudioModes module: alert sound (per user style) then speech,
            // with silent/DND/headphone guards + resource cleanup inside.
            com.craftcaller.app.audio.AnnouncerAudio.handleAnnouncement(context, text)
        }.onFailure { e ->
            Log.w(TAG, "announcement failed: $e")
        }
    }

    /** Convenience: full local resolution (Room cache + contacts) without network. */
    fun resolveDisplayNameLocal(context: Context, number: String, cached: CallerInfo?): String? =
        lookupContactName(context, number) ?: cached?.name

    /** Local Room cache read (no network). */
    suspend fun cachedCallerInfo(context: Context, number: String): CallerInfo? =
        runCatching {
            DatabaseProvider.get(context).callerDao().get(number)
        }.getOrNull()

    /** Silences any in-flight announcement (call answered / stopped). */
    fun shutdownVoice() = com.craftcaller.app.tts.AnnouncerVoice.stop()
}
