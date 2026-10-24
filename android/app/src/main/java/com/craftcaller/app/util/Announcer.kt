package com.craftcaller.app.util

/**
 * Personal-profile model for the caller announcer.
 *
 * IMPORTANT (design note): the user's OWN honorific ("address") is completely
 * separated from the ANNOUNCER voice:
 *   - honorific / gender     -> how the app addresses the user inside the
 *                               announcement line ("Umesh sir", "Umesh ma'am")
 *   - announcement language  -> the phrasing template
 *   - announcer voice        -> which installed text-to-speech voice speaks it
 *                               (fully independent selection; user previews it)
 */

enum class Gender(val key: String, val label: String) {
    MALE("male", "Male"),
    FEMALE("female", "Female");

    companion object {
        fun fromKey(key: String): Gender = entries.firstOrNull { it.key == key } ?: MALE
    }
}

/**
 * How the user wants to be addressed in announcements.
 * SIR/MAAM follow the chosen gender by default but remain independently
 * user-selectable; JI is gender-neutral; NONE drops the honorific entirely.
 */
enum class Honorific(val key: String, val suffix: String, val label: String, val hint: String) {
    SIR("sir", " sir", "Sir", "Umesh sir"),
    MAAM("maam", " ma'am", "Ma'am", "Umesh ma'am"),
    JI("ji", " ji", "Ji", "Umesh ji"),
    NONE("none", "", "None", "Umesh");

    companion object {
        fun fromKey(key: String): Honorific = entries.firstOrNull { it.key == key } ?: NONE
    }
}

/** Phrasing template of the announcement. */
enum class AnnounceLang(val key: String, val label: String, val tag: String, val tagline: String) {
    HINDI("hindi", "Hindi", "hi-IN", "Umesh sir, aapko Rahul Sharma ka call aa raha hai."),
    HINGLISH("hinglish", "Hinglish", "hi-IN", "Umesh sir, aapke liye Rahul Sharma ka call aaya hai."),
    ENGLISH("english", "English", "en-IN", "Umesh sir, you have a call from Rahul Sharma.");

    companion object {
        fun fromKey(key: String): AnnounceLang = entries.firstOrNull { it.key == key } ?: HINDI
    }
}

/** Which announcement phrasing to use, driven by caller data. */
enum class AnnounceKind { SAVED, UNKNOWN, REPEAT }

/** A text-to-speech voice installed on this device (previewable + selectable). */
data class AvailableVoice(
    val name: String,
    val label: String,
    val languageTag: String
)

object Announcer {

    const val SAMPLE_CALLER_NAME = "Rahul Sharma"

    /** Default honorific derived from gender (user can always override). */
    fun defaultHonorific(gender: Gender): Honorific =
        if (gender == Gender.FEMALE) Honorific.MAAM else Honorific.SIR

    /** "Umesh" -> "Umesh sir" / "Umesh ma'am" / "Umesh ji" / "Umesh". */
    fun addressed(rawName: String, honorific: Honorific): String {
        val first = rawName.trim().split(" ").firstOrNull { it.isNotBlank() } ?: rawName
        return if (first.isEmpty()) first else first + honorific.suffix
    }

    /**
     * The full spoken announcement, e.g.
     *   "Umesh sir, aapko Rahul Sharma ka call aa raha hai."
     */
    fun greeting(addressedName: String, callerName: String, lang: AnnounceLang): String =
        greeting(addressedName, callerName, lang, AnnounceKind.SAVED, 1)

    /**
     * Kind-aware templates:
     *   SAVED   -> "Umesh sir, aapko Rahul Sharma ka call aa raha hai."
     *   UNKNOWN -> "Umesh sir, ek unknown number se call aa raha hai."
     *   REPEAT  -> "Umesh sir, Rahul Sharma ki teesri call aa raha hai."
     *
     * [customTemplate] (user-authored, Prefs.announceTemplate) overrides the
     * built-in phrasing entirely when non-blank. Tokens:
     *   {you}    addressed user                                   ("Umesh sir")
     *   {caller} caller name/number; on first-ring unknown calls this
     *            slot falls back to an "unknown number" phrase
     *   {repeat} ordinal word ("doosri" / "2nd") on repeat rings, else ""
     * Everything outside tokens is literal.
     */
    fun greeting(
        addressedName: String,
        callerName: String,
        lang: AnnounceLang,
        kind: AnnounceKind,
        ringCount: Int,
        customTemplate: String = ""
    ): String {
        val template = customTemplate.trim()
        if (template.isNotEmpty()) {
            val caller = callerName.trim().ifBlank { unknownCallerPhrase(lang) }
            val repeat = when {
                ringCount < 2 -> ""
                lang == AnnounceLang.ENGLISH -> "(${ordinalEnglish(ringCount)})"
                else -> "(${ordinalHindi(ringCount)})"
            }
            return buildString {
                var matcher = template
                // Simple, allocation-cheap token expansion (case-insensitive).
                for (token in listOf("{you}", "{caller}", "{repeat}")) {
                    val value = when (token) {
                        "{you}" -> addressedName
                        "{caller}" -> caller
                        else -> repeat
                    }
                    val regex = Regex(Regex.escape(token), RegexOption.IGNORE_CASE)
                    matcher = matcher.replace(regex, value)
                }
                append(matcher)
                if (!matcher.endsWith('.') && !matcher.endsWith('!') && !matcher.endsWith('।')) {
                    append('.')
                }
            }
        }
        return when (kind) {
        AnnounceKind.SAVED -> when (lang) {
            AnnounceLang.HINDI -> "$addressedName, aapko $callerName ka call aa raha hai."
            AnnounceLang.HINGLISH -> "$addressedName, aapke liye $callerName ka call aaya hai."
            AnnounceLang.ENGLISH -> "$addressedName, you have a call from $callerName."
        }
        AnnounceKind.UNKNOWN -> when (lang) {
            AnnounceLang.HINDI -> "$addressedName, ek unknown number se call aa raha hai."
            AnnounceLang.HINGLISH -> "$addressedName, ek unknown number se call aa raha hai — dekh lo."
            AnnounceLang.ENGLISH -> "$addressedName, you have a call from an unknown number."
        }
        AnnounceKind.REPEAT -> when (lang) {
            AnnounceLang.HINDI -> "$addressedName, $callerName ki ${ordinalHindi(ringCount)} call aa raha hai."
            AnnounceLang.HINGLISH -> "$addressedName, $callerName ne ${ordinalHindi(ringCount)} baar call kiya hai."
            AnnounceLang.ENGLISH -> "$addressedName, $callerName is calling for the ${ordinalEnglish(ringCount)} time."
        }
        }
    }

    /** Phrase spoken for {caller} when the ring has no trusted name (first-ring unknown). */
    private fun unknownCallerPhrase(lang: AnnounceLang): String = when (lang) {
        AnnounceLang.ENGLISH -> "an unknown number"
        else -> "ek unknown number"
    }

    /** Hindi ordinal word for repeat announcements ("doosri", "teesri", ...). */
    fun ordinalHindi(count: Int): String = when {
        count <= 1 -> "pehli"
        count == 2 -> "doosri"
        count == 3 -> "teesri"
        count == 4 -> "chauthi"
        count == 5 -> "paanchvi"
        else -> "$count baar" // "baaki: N baar" phrasing reads naturally
    }

    /** English ordinal suffix ("2nd", "3rd", "4th", ...). */
    fun ordinalEnglish(count: Int): String {
        val suffix = when {
            count % 100 in 11..13 -> "th"
            count % 10 == 1 -> "st"
            count % 10 == 2 -> "nd"
            count % 10 == 3 -> "rd"
            else -> "th"
        }
        return "$count$suffix"
    }

    /** Same line, but for the user's own profile (name = user's first name). */
    fun personalGreeting(
        rawName: String,
        honorific: Honorific,
        lang: AnnounceLang,
        callerName: String = SAMPLE_CALLER_NAME
    ): String {
        val safeName = rawName.trim().ifBlank { "Umesh" }
        return greeting(addressed(safeName, honorific), callerName, lang)
    }
}
