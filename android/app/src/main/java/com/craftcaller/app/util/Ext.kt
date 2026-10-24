package com.craftcaller.app.util

import com.google.i18n.phonenumbers.PhoneNumberUtil
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/** Shared constants + thin helpers used by UI, services and receivers. */
object Craft {

    /** Community spam threshold: "10+ users report" -> Spam - Likely Fraud. */
    const val SPAM_REPORTS_THRESHOLD = 10

    /** Time after a call during which the after-call screen may appear. */
    const val AFTER_CALL_WINDOW_MS = 60_000L

    /** Country fallback for region-less inputs (device locale first, then IN). */
    private val homeRegion: String
        get() = Locale.getDefault().country?.takeIf { it.length == 2 } ?: "IN"

    private val phoneUtil = runCatching { PhoneNumberUtil.getInstance() }.getOrNull()

    /**
     * Correct normalization (libphonenumber): any local/mobile/with-country
     * format becomes a canonical digit string including country code, e.g.
     *   "098765 43210" / "98765 43210" / "+91 98765 43210" → "919876543210".
     * Unparseable input degrades to digits-only (old behavior) — never invented.
     */
    fun normalizeNumber(raw: String): String {
        val trimmed = raw.trim()
        if (trimmed.isEmpty()) return ""
        val util = phoneUtil ?: return trimmed.filter { it.isDigit() }.takeLast(12)
        return runCatching {
            val parsed = util.parse(trimmed, homeRegion)
            if (util.isValidNumber(parsed)) {
                util.format(parsed, PhoneNumberUtil.PhoneNumberFormat.E164)
                    .removePrefix("+")
            } else if (trimmed.startsWith("+")) {
                util.format(parsed, PhoneNumberUtil.PhoneNumberFormat.E164)
                    .removePrefix("+")
            } else {
                trimmed.filter { it.isDigit() }.takeLast(12)
            }
        }.getOrDefault(trimmed.filter { it.isDigit() }.takeLast(12))
    }

    fun prettyNumber(raw: String): String {
        val digits = normalizeNumber(raw)
        return when {
            digits.length == 10 ->
                digits.substring(0, 5) + " " + digits.substring(5)
            digits.length == 12 && digits.startsWith("91") ->
                "+91 " + digits.substring(2, 7) + " " + digits.substring(7)
            digits.length > 8 -> "+${digits.dropLast(10)} ${digits.takeLast(10)}"
            else -> raw.ifBlank { "Unknown" }
        }
    }

    fun initialOf(name: String?, number: String): String =
        name?.trim()?.take(1)?.uppercase() ?: "#"

    private val timeFmt = SimpleDateFormat("dd MMM, hh:mm a", Locale.getDefault())

    fun formatTime(epoch: Long): String = runCatching { timeFmt.format(Date(epoch)) }.getOrDefault("")

    /** India telecom-circle heuristic (best effort; null for other countries). */
    fun guessOperator(number: String): String? {
        val d = normalizeNumber(number)
        val national = when {
            d.length == 12 && d.startsWith("91") -> d.drop(2)
            d.length == 10 -> d
            else -> return null
        }
        val prefix = national.take(2)
        return when (prefix) {
            "60", "70", "86", "87", "88", "89" -> "Jio • India"
            "701", "98", "99" -> "Airtel • India"
            "80900", "890" -> "Vi • India"
            "941", "944", "945" -> "BSNL • India"
            else -> null
        }
    }
}
