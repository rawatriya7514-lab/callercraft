package com.craftcaller.app.data.model

import com.craftcaller.app.util.Craft

/** A CraftCaller community user (Firestore `users` collection). */
data class CraftUser(
    val uid: String = "",
    val name: String = "",
    val email: String = "",
    val phone: String = "",
    val createdAt: Long = 0
)

/** A number entry in the community directory (Firestore `numbers/{number}`). */
data class NumberEntry(
    val number: String = "",
    val name: String? = null,
    val reports: Int = 0,
    val updatedAt: Long = 0
)

/** Unified caller lookup result shown everywhere in the UI. */
data class CallerLookup(
    val number: String,
    val name: String? = null,
    val location: String? = null,
    val spamReports: Int = 0,
    val source: String = ""
) {
    val isSpam: Boolean get() = spamReports >= Craft.SPAM_REPORTS_THRESHOLD
}

/** Retrofit request/response DTOs (own backend). */
data class LookupApiRequest(val number: String)
data class LookupApiResponse(
    val number: String? = null,
    val name: String? = null,
    val type: String? = null,
    val overall: Int = -1,
    val city: String? = null,
    val country: String? = null
) {
    val isSpam: Boolean get() = overall >= 50
}

data class ReportSpamRequest(
    val number: String,
    val by: String,
    val reason: String
)

/** A single device contact for the Contacts tab + upload. */
data class DeviceContact(
    val name: String,
    val number: String
)
