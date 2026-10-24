package com.craftcaller.app.service

import android.app.role.RoleManager
import android.content.Context
import android.os.Build
import android.telecom.Call
import android.telecom.CallScreeningService
import android.util.Log
import com.craftcaller.app.data.local.DatabaseProvider
import com.craftcaller.app.data.local.entity.CallerInfo
import com.craftcaller.app.data.remote.FirebaseRepository
import com.craftcaller.app.util.Craft
import com.craftcaller.app.util.Prefs
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeoutOrNull

/**
 * System call screening (requires the user to place CraftCaller in the
 * "Call screening" role — requested from Setup/Settings with an explanation).
 *
 * Android gives 5 seconds to call respondToCall(); we spend at most ~300 ms on
 * LOCAL database checks only, respond, and do everything else (community
 * enrichment + voice announcement) AFTERwards so ringing is never delayed by
 * an online lookup.
 */
class CraftCallScreeningService : CallScreeningService() {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onScreenCall(callDetails: Call.Details) {
        val raw = callDetails.handle?.schemeSpecificPart.orEmpty()
        val number = Craft.normalizeNumber(raw)
        val prefs = Prefs(applicationContext)

        // Call.Details.direction is API 29+; treat everything as incoming on older builds.
        val outgoing = Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q &&
            callDetails.callDirection == Call.Details.DIRECTION_OUTGOING

        if (outgoing || number.isEmpty()) {
            respondToCall(callDetails, CallResponse.Builder().build())
            runCatching {
                prefs.lastScreeningAt = System.currentTimeMillis()
                prefs.lastScreeningSummary = if (outgoing) "Outgoing call (no screening)" else "Unknown/empty number"
            }
            return
        }

        val responseStart = System.currentTimeMillis()
        // LOCAL-ONLY decision: manual blocklist + cached community verdicts.
        // Network lookups never happen here (they would delay ringing).
        val shouldBlock = runBlocking {
            withTimeoutOrNull(LOCAL_DECISION_BUDGET_MS) { decideLocal(number, prefs) }
        } ?: false
        Log.d(TAG, "local decision in ${System.currentTimeMillis() - responseStart}ms block=$shouldBlock ($number)")

        val response = if (shouldBlock) {
            CallResponse.Builder()
                .setDisallowCall(true)
                .setRejectCall(true)
                .setSkipCallLog(false)
                .setSkipNotification(true)
                .build()
        } else {
            CallResponse.Builder().build()
        }

        // Respond FIRST — the screening contract deadline is satisfied.
        respondToCall(callDetails, response)

        // Record the decision for the backstage Troubleshooting screen (real
        // diagnostics, written AFTER the system contract is met).
        runCatching {
            prefs.lastScreeningAt = System.currentTimeMillis()
            prefs.lastScreeningSummary = if (shouldBlock)
                "Blocked $number (local decision)"
            else if (outgoing || number.isEmpty())
                "No screening needed"
            else
                "Allowed $number to ring"
        }

        // Everything after this is async cleanup + the voice announcement.
        if (!shouldBlock) {
            scope.launch {
                // Speak FIRST from 100% local data (contacts + Room cache) —
                // never let a network wait delay the spoken announcement.
                val cached = AnnouncementEngine.cachedCallerInfo(applicationContext, number)
                val localName = AnnouncementEngine.resolveDisplayNameLocal(
                    applicationContext, number, cached
                )
                AnnouncementEngine.maybeAnnounce(
                    applicationContext, number, localName, cached?.spamReports ?: 0
                )

                // THEN enrich the community cache for FUTURE local decisions
                // (best effort; the system may unbind this service mid-way).
                val fresh = runCatching {
                    withTimeoutOrNull(5000L) { FirebaseRepository.lookup(number) }
                }.getOrNull()
                fresh?.let {
                    runCatching {
                        DatabaseProvider.get(applicationContext).callerDao().upsert(
                            CallerInfo(
                                number = number,
                                name = it.name,
                                location = it.location,
                                spamReports = it.spamReports,
                                updatedAt = System.currentTimeMillis(),
                                source = it.source
                            )
                        )
                    }
                }
            }
        }
    }

    /** Local Room checks only — bounded work inside the respondToCall budget. */
    private suspend fun decideLocal(number: String, prefs: Prefs): Boolean {
        val db = DatabaseProvider.get(applicationContext)

        // 1. Manual blocklist — always block.
        if (db.blockedDao().exists(number)) return true

        // 2. Explicit allow-list wins over community verdicts: never
        //    auto-reject a number the user explicitly allowed.
        if (db.allowedDao().exists(number)) return false

        // 3. Cached community knowledge.
        val cached = db.callerDao().get(number)
        if (cached != null) {
            return prefs.autoBlockSpammers && cached.spamReports >= Craft.SPAM_REPORTS_THRESHOLD
        }
        return false // unknown number: never block on first sight
    }

    companion object {
        private const val TAG = "ScreeningSvc"

        /**
         * Budget for the local block decision. Android allows 5s for
         * respondToCall; we keep a wide margin under it.
         */
        const val LOCAL_DECISION_BUDGET_MS = 300L

        /** True when this app currently holds the system call-screening role. */
        fun isRoleHeld(context: Context): Boolean {
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) return false
            val manager = context.getSystemService(RoleManager::class.java) ?: return false
            return runCatching {
                manager.isRoleHeld(RoleManager.ROLE_CALL_SCREENING)
            }.getOrDefault(false)
        }
    }
}
