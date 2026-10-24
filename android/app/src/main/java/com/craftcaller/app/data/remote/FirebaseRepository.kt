package com.craftcaller.app.data.remote

import android.util.Log
import com.craftcaller.app.data.model.CallerLookup
import com.craftcaller.app.data.model.CraftUser
import com.craftcaller.app.data.model.DeviceContact
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withTimeoutOrNull

/**
 * Firebase backend:
 *  - `users`       : registered users (uid + name + email + phone)
 *  - `numbers/{key}`: community caller-ID entries (name + spam report counter)
 *  - Realtime DB `numbers/{key}`: mirror for instant overlay lookups
 *
 * Everything is defensive: with the placeholder google-services.json the SDK
 * initialises fine but network calls fail — every call is wrapped + timed out
 * and simply returns null, so the app degrades to local lookups offline.
 */
object FirebaseRepository {

    private const val TAG = "FirebaseRepo"

    private var auth: FirebaseAuth? = null
    private var firestore: FirebaseFirestore? = null
    private var rtdb: FirebaseDatabase? = null
    private var initialized = false

    init {
        runCatching {
            auth = FirebaseAuth.getInstance()
            firestore = FirebaseFirestore.getInstance()
            rtdb = FirebaseDatabase.getInstance()
            initialized = true
        }.onFailure { Log.w(TAG, "Firebase init (expected with placeholder config): ${it.message}") }
    }

    val configured: Boolean get() = initialized

    /**
     * True when the bundled google-services.json is the placeholder (demo value
     * like "AIzaSyA-PLACEHOLDER-…" / project "craftcaller-demo"). The SDK boots
     * fine with it, but every network call will be rejected ("API key not valid").
     */
    val placeholderConfig: Boolean
        get() {
            if (!initialized) return true
            val app = runCatching { FirebaseApp.getInstance() }.getOrNull() ?: return true
            val apiKey = runCatching { app.options.apiKey }.getOrDefault("")
            val projectId = runCatching { app.options.projectId }.getOrNull()
            return apiKey.contains("PLACEHOLDER") || projectId == "craftcaller-demo"
        }
    fun currentUserUid(): String? = runCatching { auth?.currentUser?.uid }.getOrNull()

    /** Stable document key for a number: digits only ("+91 98765 43210" -> "919876543210"). */
    private fun keyOf(number: String): String = number.filter { it.isDigit() }

    // ---- Auth / registration --------------------------------------------

    suspend fun registerUser(user: CraftUser): Boolean = try {
        withTimeoutOrNull(8000) {
            firestore?.collection("users")
                ?.document(user.uid)
                ?.set(user)
                ?.await()
        }
        registerOwnNumber(user.phone, user.name)
        true
    } catch (e: Exception) {
        Log.w(TAG, "registerUser failed: ${e.message}")
        false
    }

    /** Publish "my number -> my name" into the community directory. */
    suspend fun registerOwnNumber(number: String, name: String): Boolean = try {
        val key = keyOf(number)
        if (key.isEmpty()) false else {
            val doc = mapOf(
                "number" to key,
                "name" to name,
                "reports" to 0,
                "ownerUid" to (currentUserUid() ?: ""),
                "updatedAt" to System.currentTimeMillis()
            )
            withTimeoutOrNull(8000) { firestore?.collection("numbers")?.document(key)?.set(doc)?.await() }
            withTimeoutOrNull(8000) { rtdb?.reference?.child("numbers")?.child(key)?.setValue(doc)?.await() }
            true
        }
    } catch (e: Exception) {
        Log.w(TAG, "registerOwnNumber failed: ${e.message}")
        false
    }

    // ---- Lookup -----------------------------------------------------------

    /** Fast path: Realtime DB mirror -> Firestore number doc -> Firestore users query. */
    suspend fun lookup(number: String): CallerLookup? {
        val key = keyOf(number)
        if (key.isEmpty()) return null

        if (initialized) {
            val rtdbResult = withTimeoutOrNull(2500) {
                rtdb?.reference?.child("numbers")?.child(key)?.get()?.await()
            }
            val snapshot = rtdbResult
            if (snapshot?.exists() == true) {
                return CallerLookup(
                    number = number,
                    name = snapshot.child("name").getValue(String::class.java),
                    spamReports = snapshot.child("reports").getValue(Int::class.java) ?: 0,
                    source = "community"
                )
            }

            val fsResult = withTimeoutOrNull(2500) {
                firestore?.collection("numbers")?.document(key)?.get()?.await()
            }
            if (fsResult != null && fsResult.exists()) {
                return CallerLookup(
                    number = number,
                    name = fsResult.getString("name"),
                    spamReports = (fsResult.getLong("reports") ?: 0L).toInt(),
                    source = "community"
                )
            }

            val userResult = withTimeoutOrNull(2500) {
                firestore?.collection("users")
                    ?.whereEqualTo("phoneDigits", key)
                    ?.limit(1L)
                    ?.get()
                    ?.await()
            }
            userResult?.firstOrNull()?.let { doc ->
                return CallerLookup(
                    number = number,
                    name = doc.getString("name"),
                    spamReports = 0,
                    source = "users"
                )
            }
        }

        // Retrofit fallback (own API)
        return runCatching {
            val res = withTimeoutOrNull(4000) { RetrofitClient.api.lookup(key) } ?: return@runCatching null
            CallerLookup(
                number = number,
                name = res.name,
                location = listOfNotNull(res.city, res.country).takeIf { it.isNotEmpty() }?.joinToString(", "),
                spamReports = if (res.isSpam) 50 else 0,
                source = "api"
            )
        }.getOrNull()
    }

    // ---- Spam reporting -----------------------------------------------------

    /** One-tap spam report. Firestore counter + RTDB mirror. >=10 -> spam. */
    suspend fun reportSpam(number: String): Boolean {        val key = keyOf(number)
        if (key.isEmpty()) return false
        return try {
            withTimeoutOrNull(8000) {
                firestore?.collection("numbers")?.document(key)?.set(
                    mapOf(
                        "number" to key,
                        "reports" to FieldValue.increment(1),
                        "lastReportAt" to System.currentTimeMillis()
                    ),
                    com.google.firebase.firestore.SetOptions.merge()
                )?.await()
            }
            withTimeoutOrNull(8000) {
                val reportsRef = rtdb?.reference?.child("numbers")?.child(key)?.child("reports")
                val current = runCatching { reportsRef?.get()?.await() }.getOrNull()
                val next = (current?.getValue(Int::class.java) ?: 0) + 1
                runCatching { reportsRef?.setValue(next)?.await() }
            }
            true
        } catch (e: Exception) {
            Log.w(TAG, "reportSpam failed: ${e.message}")
            false
        }
    }

    // ---- Contact sync (community contributions) -------------------------------

    /**
     * User-driven name correction ("this number is actually X"). Writes the
     * corrected name + increments a transparency counter. Data submitted is
     * ONLY the number + corrected name — no personal data. Callers MUST gate
     * this behind the per-action community consent dialog.
     */
    suspend fun submitCorrection(number: String, correctedName: String): Boolean {
        val key = keyOf(number)
        val name = correctedName.trim()
        if (key.isEmpty() || name.isEmpty()) return false
        return try {
            withTimeoutOrNull(8000) {
                firestore?.collection("numbers")?.document(key)?.set(
                    mapOf(
                        "number" to key,
                        "name" to name,
                        "reports" to FieldValue.increment(0),
                        "corrections" to FieldValue.increment(1),
                        "ownerUid" to (currentUserUid() ?: ""),
                        "updatedAt" to System.currentTimeMillis()
                    ),
                    com.google.firebase.firestore.SetOptions.merge()
                )?.await()
            }
            withTimeoutOrNull(8000) {
                rtdb?.reference?.child("numbers")?.child(key)?.let { ref ->
                    val current = runCatching { ref.get()?.await() }.getOrNull()
                    val reports = current?.child("reports")?.getValue(Int::class.java) ?: 0
                    ref.setValue(mapOf("name" to name, "reports" to reports))?.await()
                }
            }
            true
        } catch (e: Exception) {
            Log.w(TAG, "submitCorrection failed: ${e.message}")
            false
        }
    }

    // ---- Contact sync (community contributions) -------------------------------

    /**
     * Uploads the user's saved phonebook names to the community directory.
     * GATED: callers must only invoke this after the user turned on the
     * explicit community-upload consent switch (see ContactSyncWorker +
     * Settings explainer); no automatic path is allowed to reach it.
     */
    suspend fun uploadCommunityContacts(contacts: List<DeviceContact>): Boolean {
        if (!initialized) return false
        return try {
            withTimeoutOrNull(20000) {
                val fs = firestore ?: return@withTimeoutOrNull false
                fs.runBatch { batch ->
                    contacts.forEach { contact ->
                        val key = keyOf(contact.number)
                        if (key.isNotEmpty()) {
                            batch.set(
                                fs.collection("numbers").document(key),
                                mapOf(
                                    "number" to key,
                                    "name" to contact.name,
                                    "reports" to 0,
                                    "ownerUid" to (currentUserUid() ?: ""),
                                    "updatedAt" to System.currentTimeMillis()
                                )
                            )
                        }
                    }
                }.await()
            }
            true
        } catch (e: Exception) {
            Log.w(TAG, "uploadCommunityContacts failed: ${e.message}")
            false
        }
    }
}
