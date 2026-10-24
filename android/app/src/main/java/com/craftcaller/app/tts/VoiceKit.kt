package com.craftcaller.app.tts

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.speech.tts.TextToSpeech
import android.speech.tts.Voice
import com.craftcaller.app.util.AvailableVoice
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import java.util.Locale

/**
 * Thin TextToSpeech wrapper used by onboarding/Settings for voice listing and
 * the announcement preview ("Umesh sir, aapko Rahul Sharma ka call aa raha
 * hai." spoken with the actually-selected announcer voice).
 *
 * Lifecycle: create per screen (ViewModel), call [shutdown] in onCleared().
 * Full call-time announcements hook into this object later (service side).
 */
class VoiceKit(
    context: Context,
    /** Selected TTS engine package (null/blank = Android system default). */
    private val enginePackage: String? = null
) : TextToSpeech.OnInitListener {

    private val appCtx = context.applicationContext
    private val main = Handler(Looper.getMainLooper())

    private var tts: TextToSpeech? = null

    /** Compose-observed readiness (main thread writes only). */
    private val _ready = MutableStateFlow(false)
    val ready: StateFlow<Boolean> = _ready

    private val _voices = MutableStateFlow<List<AvailableVoice>>(emptyList())
    val voices: StateFlow<List<AvailableVoice>> = _voices

    private val _initError = MutableStateFlow<String?>(null)
    val initError: StateFlow<String?> = _initError

    private var fellBack = false

    init {
        runCatching {
            if (enginePackage.isNullOrBlank()) {
                tts = TextToSpeech(appCtx, this)
            } else {
                tts = TextToSpeech(appCtx, this, enginePackage)
            }
        }.onFailure { e ->
            _initError.value = e.message ?: "TTS engine could not be created"
        }
    }

    override fun onInit(status: Int) {
        main.post {
            if (status != TextToSpeech.SUCCESS && enginePackage != null && !fellBack) {
                // Selected engine failed to initialise — fall back to default ONCE.
                fellBack = true
                _initError.value = "Selected engine failed to start — using system default"
                runCatching { tts?.shutdown() }
                tts = TextToSpeech(appCtx, this)
            } else {
                _ready.value = status == TextToSpeech.SUCCESS
                if (status == TextToSpeech.SUCCESS) refreshVoices()
            }
        }
    }

    /** Re-query installed voices (e.g., after the Google TTS engine updates). */
    fun refreshVoices() {
        val engine = tts ?: return
        val list = runCatching {
            engine.voices.orEmpty()
                .filter { it.name.isNotBlank() && (it.locale.language == "hi" || it.locale.language == "en") }
                .sortedWith(compareBy({ it.locale.language != "hi" }, { it.name }))
                .map { it.toAvailableVoice() }
        }.getOrDefault(emptyList())
        main.post { _voices.value = list }
    }

    private fun Voice.toAvailableVoice(): AvailableVoice =
        AvailableVoice(
            name = name,
            label = prettyVoiceName(name),
            languageTag = runCatching { locale.toLanguageTag() }.getOrDefault(locale.toString())
        )

    /** Speak [text]; prefer the installed voice [voiceName], else fall back to
     *  a locale matching [languageTag]. Interrupts any previous preview. */
    fun speak(
        text: String,
        voiceName: String?,
        languageTag: String,
        rate: Float = 1.02f,
        pitch: Float = 1.0f
    ) {
        val engine = tts ?: return
        runCatching {
            engine.stop()
            val installed = engine.voices?.firstOrNull { it.name == voiceName }
            if (installed != null) {
                engine.voice = installed
            } else {
                engine.voice = null
                engine.language = parseLocale(languageTag)
            }
            engine.setSpeechRate(rate)
            engine.setPitch(pitch)
            engine.speak(text, TextToSpeech.QUEUE_FLUSH, null, "cc_preview_${System.nanoTime()}")
        }
    }

    fun stop() {
        runCatching { tts?.stop() }
    }

    /** Release the engine; must be called when the host ViewModel is cleared. */
    fun shutdown() {
        runCatching { tts?.stop() }
        runCatching { tts?.shutdown() }
        tts = null
        main.post {
            _ready.value = false
            _voices.value = emptyList()
        }
    }

    private fun parseLocale(tag: String): Locale =
        runCatching { Locale.forLanguageTag(tag) }.getOrDefault(Locale.US)

    private fun prettyVoiceName(name: String): String {
        // "hi-in-x-gfm-local" -> "hi-IN · x-gfm · local"
        val parts = name.lowercase().split('-').filter { it.isNotBlank() }
        if (parts.size < 3) return name
        val lang = parts[0]
        val region = parts.getOrNull(1)?.uppercase() ?: ""
        val rest = parts.drop(2).joinToString(" · ")
        return listOf(if (region.length == 2) "$lang-$region" else lang, rest)
            .filter { it.isNotBlank() }
            .joinToString(" · ")
    }
}
