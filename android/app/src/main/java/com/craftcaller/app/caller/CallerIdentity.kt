package com.craftcaller.app.caller

import android.content.Context
import com.craftcaller.app.data.local.DatabaseProvider
import com.craftcaller.app.data.local.entity.CallerInfo
import com.craftcaller.app.data.model.CallerLookup
import com.craftcaller.app.data.remote.FirebaseRepository
import com.craftcaller.app.service.AnnouncementEngine
import com.craftcaller.app.util.Craft
import com.craftcaller.app.util.Prefs

/**
 * Truecaller-style identity ladder for unsaved numbers. TRUST ORDER (highest
 * first) — and honest labels, nothing invented:
 *
 *   1. SAVED             on-device contacts (name from the user's phonebook)
 *   2. VERIFIED_BUSINESS the project-owned public business directory (Retrofit
 *                        API; only used when a real config exists, never with
 *                        the placeholder key)
 *   3. COMMUNITY         user-submitted names (own annotations + opt-in
 *                        community DB), read access off-switchable
 *   4. SUSPECTED_SPAM    community report counter >= threshold
 *   5. UNKNOWN           no trustworthy match — stays "Unknown"; the app
 *                        NEVER fabricates a name
 *
 * Also carries source + confidence so the UI can show where identity came
 * from instead of pretending to know.
 */
enum class CallerLabel(val badge: String, val description: String) {
    SAVED("Saved contact", "From this phone's contacts"),
    VERIFIED_BUSINESS("Verified business", "CraftCaller public directory"),
    COMMUNITY("Community name", "Submitted by CraftCaller users"),
    SUSPECTED_SPAM("Spam — likely fraud", "Community spam reports"),
    UNKNOWN("Unknown", "No trustworthy source matched")
}

data class CallerIdentity(
    val number: String,
    val label: CallerLabel,
    val displayName: String?,
    val location: String?,
    val spamReports: Int,
    /** 0..100 heuristic trust meter for UI display. 0 = truly unknown. */
    val confidence: Int,
    /** Human-readable source chain, e.g. "contacts". */
    val sourceLine: String,
    /** True when present in the explicit allow-list (never auto-blocked). */
    val allowed: Boolean = false
) {
    val isUnknown: Boolean get() = label == CallerLabel.UNKNOWN
    val display: String get() = displayName ?: "Unknown"
}

object CallerResolver {

    private const val UNKNOWN_TAG = "CallerResolver"

    /** Fully-local resolution (contacts + Room cache + allow-list); no network. */
    suspend fun resolveLocal(context: Context, rawNumber: String): CallerIdentity {
        val number = Craft.normalizeNumber(rawNumber)
        val db = DatabaseProvider.get(context)
        val allowed = runCatching { db.allowedDao().exists(number) }.getOrDefault(false)

        if (number.length < 7) {
            return CallerIdentity(
                number = rawNumber, label = CallerLabel.UNKNOWN, displayName = null,
                location = null, spamReports = 0, confidence = 0,
                sourceLine = "unparseable number", allowed = allowed
            )
        }

        // 1. On-device contacts — always first, no consent needed (local only).
        val contactName = AnnouncementEngine.lookupContactName(context, number)
        if (!contactName.isNullOrBlank()) {
            return CallerIdentity(
                number = number, label = CallerLabel.SAVED, displayName = contactName,
                location = null, spamReports = 0, confidence = 100,
                sourceLine = "contacts", allowed = allowed
            )
        }

        // 2/3/4. Room cache entries (user annotations + community/API cache).
        val cached = runCatching { db.callerDao().get(number) }.getOrNull()
        if (cached != null) {
            if (cached.spamReports >= Craft.SPAM_REPORTS_THRESHOLD) {
                return spamIdentity(number, cached.spamReports, cached, allowed)
            }
            if (!cached.name.isNullOrBlank()) {
                return CallerIdentity(
                    number = number, label = CallerLabel.COMMUNITY, displayName = cached.name,
                    location = cached.location, spamReports = cached.spamReports,
                    confidence = if (cached.source == "local" || cached.source == "user-report") 75 else 60,
                    sourceLine = "cache (${cached.source})", allowed = allowed
                )
            }
        }

        return CallerIdentity(
            number = number, label = CallerLabel.UNKNOWN, displayName = null,
            location = null, spamReports = cached?.spamReports ?: 0, confidence = 0,
            sourceLine = "no local match", allowed = allowed
        )
    }

    /**
     * Local first, then (only with read consent + non-placeholder config)
     * community/directory enrichment. Data is cached for future offline use.
     */
    suspend fun resolveFull(context: Context, rawNumber: String): CallerIdentity {
        val prefs = Prefs(context)
        var identity = resolveLocal(context, rawNumber)

        val canQueryNetwork =
            prefs.communityReadEnabled && !FirebaseRepository.placeholderConfig
        if (!canQueryNetwork || identity.label == CallerLabel.SAVED) {
            if (identity.isUnknown && !canQueryNetwork) {
                return identity.copy(
                    sourceLine = if (prefs.communityReadEnabled) identity.sourceLine
                    else "community lookups off"
                )
            }
            return identity
        }

        val lookup: CallerLookup? = runCatching {
            withTimeoutOrNullCols(8000) { FirebaseRepository.lookup(identity.number) }
        }.getOrNull()

        if (lookup != null) {
            val db = DatabaseProvider.get(context)
            runCatching {
                db.callerDao().upsert(
                    CallerInfo(
                        number = identity.number,
                        name = lookup.name,
                        location = lookup.location,
                        spamReports = lookup.spamReports,
                        updatedAt = System.currentTimeMillis(),
                        source = lookup.source
                    )
                )
            }

            identity = when {
                lookup.spamReports >= Craft.SPAM_REPORTS_THRESHOLD ->
                    spamIdentity(identity.number, lookup.spamReports, null, identity.allowed)
                        .copy(
                            displayName = lookup.name ?: identity.displayName,
                            location = lookup.location ?: identity.location
                        )
                identity.isUnknown && !lookup.name.isNullOrBlank() ->
                    CallerIdentity(
                        number = identity.number,
                        label = if (lookup.source == "api") CallerLabel.VERIFIED_BUSINESS
                        else CallerLabel.COMMUNITY,
                        displayName = lookup.name,
                        location = lookup.location,
                        spamReports = lookup.spamReports,
                        confidence = if (lookup.source == "api") 85 else 65,
                        sourceLine = "live ${lookup.source}",
                        allowed = identity.allowed
                    )
                else -> identity // local name was stronger; keep it
            }
        }

        return identity
    }

    private fun spamIdentity(
        number: String,
        reports: Int,
        cached: CallerInfo?,
        allowed: Boolean
    ) = CallerIdentity(
        number = number,
        label = CallerLabel.SUSPECTED_SPAM,
        displayName = cached?.name,
        location = cached?.location,
        spamReports = reports,
        confidence = minOf(95, 60 + reports),
        sourceLine = "community reports ($reports)",
        allowed = allowed
    )

    private suspend fun <T> withTimeoutOrNullCols(millis: Long, block: suspend () -> T): T? =
        kotlinx.coroutines.withTimeoutOrNull(millis) { block() }
}
