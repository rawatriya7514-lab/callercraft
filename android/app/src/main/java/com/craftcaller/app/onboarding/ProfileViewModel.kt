package com.craftcaller.app.onboarding

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.craftcaller.app.tts.VoiceKit
import com.craftcaller.app.util.Announcer
import com.craftcaller.app.util.AnnounceLang
import com.craftcaller.app.util.AvailableVoice
import com.craftcaller.app.util.Gender
import com.craftcaller.app.util.Honorific
import com.craftcaller.app.util.Prefs
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch

/**
 * Shared state for the personal profile (announcer identity):
 *   name -> gender -> honorific (Sir/Ma'am/Ji/None) -> announcement language
 *   -> announcer voice (independent selection, previewable).
 *
 * Used by BOTH the first-launch onboarding wizard and the in-Settings editor;
 * saves go to encrypted prefs via [Prefs].
 */
class ProfileViewModel(app: Application) : AndroidViewModel(app) {

    private val prefs = Prefs(app)
    val voiceKit = VoiceKit(app, prefs.ttsEngineName.ifBlank { null })

    var name by mutableStateOf(prefs.userName)
    var gender by mutableStateOf(prefs.gender)
    var honorific by mutableStateOf(
        if (prefs.profileReady) prefs.honorific else Announcer.defaultHonorific(prefs.gender)
    )

    var lang by mutableStateOf(prefs.announceLang)
    var voices by mutableStateOf<List<AvailableVoice>>(emptyList())
    var voiceName by mutableStateOf(prefs.announcerVoiceName)
    var voiceLang by mutableStateOf(prefs.announcerVoiceLang)
    var announceEnabled by mutableStateOf(prefs.announceCallerEnabled)

    // Announcement routing (which callers get announced) — runtime-call config.
    var announceSaved by mutableStateOf(prefs.announceSavedContacts)
    var announceUnknown by mutableStateOf(prefs.announceUnknown)
    var announceRepeat by mutableStateOf(prefs.announceRepeat)
    var useContacts by mutableStateOf(prefs.announceUseContacts)

    // Voice delivery tuning (0.5–2.0 multipliers).
    var rate by mutableStateOf(prefs.announceRate)
    var pitch by mutableStateOf(prefs.announcePitch)

    /** True once ANY customization committed through save(); gates validation. */
    var savedOnce by mutableStateOf(false)
        private set

    /** User touched the honorific manually -> stop following gender default. */
    private var honorificTouched =
        prefs.honorific != Announcer.defaultHonorific(prefs.gender)

    val engineReady: Boolean get() = voiceKit.ready.value
    val isSpeaking: StateFlow<Boolean> get() = voiceKit.speaking

    init {
        voiceKit.voices
            .onEach { voices = it }
            .launchIn(viewModelScope)

        // Fix a voice-selection that no longer exists after an engine change.
        viewModelScope.launch {
            voiceKit.voices.collect { list ->
                if (voiceName.isNotEmpty() && list.none { it.name == voiceName }) {
                    voiceName = "" // fall back to locale-based default voice
                } else if (voiceName.isEmpty() && voiceLang.isEmpty()) {
                    voiceLang = lang.tag
                }
            }
        }
    }

    // ---------------------------------------------------------------- actions

    fun chooseGender(value: Gender) {
        gender = value
        // Follow the gender default only while the user has not hand-picked one.
        if (!honorificTouched) honorific = Announcer.defaultHonorific(value)
    }

    fun chooseHonorific(value: Honorific) {
        honorific = value
        honorificTouched = true
    }

    fun setLanguage(value: AnnounceLang) {
        lang = value
        voiceLang = value.tag
        // Auto-pick the best installed voice for the new language (first match).
        if (voiceName.isBlank() ||
            voices.none { it.name == voiceName && it.languageTag.sameLang(value.tag) }
        ) {
            voiceName = voices.firstOrNull { it.languageTag.sameLang(value.tag) }?.name.orEmpty()
        }
    }

    fun selectVoice(name: String) {
        voiceName = name
        voices.firstOrNull { it.name == name }?.let { voiceLang = it.languageTag }
    }

    /** Speak the current personal greeting with the currently selected voice. */
    fun previewPersonal() {
        preview(Announcer.personalGreeting(name.trim(), honorific, lang))
    }

    /** Row-level preview: speak the sample with THIS voice, without changing selection. */
    fun previewVoice(voice: AvailableVoice) {
        voiceKit.stop()
        voiceKit.speak(
            Announcer.personalGreeting(name.trim().ifBlank { "Umesh" }, honorific, lang),
            voice.name,
            voice.languageTag,
            rate,
            pitch
        )
    }

    /** Speak arbitrary text (used by step-level samples). */
    fun preview(text: String) {
        voiceKit.stop()
        voiceKit.speak(
            text,
            voiceName.ifBlank { null },
            voiceLang.ifBlank { lang.tag },
            rate,
            pitch
        )
    }

    fun stopPreview() = voiceKit.stop()

    /** Persist the profile; returns the resolved addressed name for UI feedback. */
    fun save(): String {
        val trimmed = name.trim()
        prefs.userName = trimmed
        prefs.gender = gender
        prefs.honorific = honorific
        prefs.announceLang = lang
        prefs.announcerVoiceName = voiceName
        prefs.announcerVoiceLang = voiceLang.ifBlank { lang.tag }
        prefs.announceCallerEnabled = announceEnabled
        prefs.announceSavedContacts = announceSaved
        prefs.announceUnknown = announceUnknown
        prefs.announceRepeat = announceRepeat
        prefs.announceUseContacts = useContacts
        prefs.announceRate = rate
        prefs.announcePitch = pitch
        prefs.profileReady = true
        savedOnce = true
        return Announcer.addressed(trimmed, honorific)
    }

    override fun onCleared() {
        voiceKit.shutdown()
        super.onCleared()
    }
}

/** Language-prefix helper: "hi-IN" matches voice packages tagged "hi-IN"/"hi". */
private fun String.sameLang(tag: String): Boolean =
    lowercase().startsWith(tag.substringBefore('-').lowercase())
