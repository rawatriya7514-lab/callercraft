package com.craftcaller.app.tts

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.speech.tts.Voice
import android.util.Log
import com.craftcaller.app.util.AvailableVoice
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import java.util.Locale

private const val TAG = "VoiceKit"

/**
 * Finds and returns the most natural, human-like voice matching [locale].
 * Prioritizes neural, network, and high-quality voices over legacy robotic synthesizers.
 * Never calls engine.setVoice(null) which throws NullPointerException in Android TTS service.
 */
internal fun selectBestVoice(
    engine: TextToSpeech,
    locale: Locale,
    preferredVoiceName: String? = null
): Voice? {
    val allVoices = runCatching { engine.voices.orEmpty() }.getOrDefault(emptySet())
    if (allVoices.isEmpty()) return null

    // 1. If preferred voice exists and is installed, use it
    if (!preferredVoiceName.isNullOrBlank()) {
        val found = allVoices.firstOrNull { it.name.equals(preferredVoiceName, ignoreCase = true) }
        if (found != null) return found
    }

    // 2. Filter voices for the target language (e.g. Hindi "hi" or English "en")
    val langVoices = allVoices.filter { voice ->
        val vl = voice.locale
        vl.language.equals(locale.language, ignoreCase = true) &&
            !voice.features.contains(TextToSpeech.Engine.KEY_FEATURE_NOT_INSTALLED)
    }
    if (langVoices.isEmpty()) return null

    // Prefer exact country match if available (e.g. hi-IN or en-IN)
    val countryMatched = if (locale.country.isNotBlank()) {
        langVoices.filter { it.locale.country.equals(locale.country, ignoreCase = true) }.ifEmpty { langVoices }
    } else {
        langVoices
    }

    // 3. Score voices to pick the most natural, non-robotic voice
    return countryMatched.maxWithOrNull(
        compareBy<Voice> { voice ->
            var score = voice.quality * 10
            val name = voice.name.lowercase()
            // Neural / network voices sound realistic and human
            if (name.contains("network") || name.contains("neural") || name.contains("wavenet")) score += 100
            if (name.contains("premium") || name.contains("enhanced")) score += 50
            if (voice.quality >= Voice.QUALITY_HIGH) score += 40
            // Low quality local synthesizers sound robotic; penalize them when better voices exist
            if (name.contains("local") && voice.quality <= Voice.QUALITY_LOW) score -= 50
            score
        }
    )
}

internal fun isGoogleTtsInstalled(context: Context): Boolean = runCatching {
    val pm = context.packageManager
    pm.getPackageInfo("com.google.android.tts", 0) != null
}.getOrDefault(false)

/**
 * TextToSpeech wrapper used by onboarding and Settings for voice listing,
 * natural voice previews, and voice selection.
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

    private val _speaking = MutableStateFlow(false)
    val speaking: StateFlow<Boolean> = _speaking

    private var pendingSpeakAction: Runnable? = null
    private var fellBack = false

    init {
        runCatching {
            val resolvedEngine = if (!enginePackage.isNullOrBlank()) {
                enginePackage
            } else if (isGoogleTtsInstalled(appCtx)) {
                "com.google.android.tts"
            } else {
                null
            }

            if (resolvedEngine.isNullOrBlank()) {
                tts = TextToSpeech(appCtx, this)
            } else {
                tts = TextToSpeech(appCtx, this, resolvedEngine)
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
                val isSuccess = status == TextToSpeech.SUCCESS
                _ready.value = isSuccess
                if (isSuccess) {
                    setupProgressListener()
                    refreshVoices()
                    // Run any queued preview action that was requested while engine was initializing
                    pendingSpeakAction?.let { action ->
                        pendingSpeakAction = null
                        action.run()
                    }
                }
            }
        }
    }

    private fun setupProgressListener() {
        tts?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
            override fun onStart(utteranceId: String?) {
                main.post { _speaking.value = true }
            }
            override fun onDone(utteranceId: String?) {
                main.post { _speaking.value = false }
            }
            override fun onError(utteranceId: String?) {
                main.post { _speaking.value = false }
            }
        })
    }

    /** Re-query installed voices (e.g., after the Google TTS engine updates). */
    fun refreshVoices() {
        val engine = tts ?: return
        val list = runCatching {
            engine.voices.orEmpty()
                .filter { it.name.isNotBlank() && (it.locale.language == "hi" || it.locale.language == "en") }
                .sortedWith(
                    compareBy<Voice>(
                        { it.locale.language != "hi" }, // Hindi first, then English
                        { -scoreVoice(it) },           // Natural voices on top
                        { it.name }
                    )
                )
                .map { it.toAvailableVoice() }
        }.getOrDefault(emptyList())
        main.post { _voices.value = list }
    }

    private fun scoreVoice(voice: Voice): Int {
        var score = voice.quality * 10
        val name = voice.name.lowercase()
        if (name.contains("network") || name.contains("neural") || name.contains("wavenet")) score += 100
        if (name.contains("premium") || name.contains("enhanced")) score += 50
        if (voice.quality >= Voice.QUALITY_HIGH) score += 40
        if (name.contains("local") && voice.quality <= Voice.QUALITY_LOW) score -= 50
        return score
    }

    private fun Voice.toAvailableVoice(): AvailableVoice =
        AvailableVoice(
            name = name,
            label = prettyVoiceName(name, quality),
            languageTag = runCatching { locale.toLanguageTag() }.getOrDefault(locale.toString())
        )

    /** Speak [text]; prefer the installed voice [voiceName], else fall back to
     *  a locale matching [languageTag]. Interrupts any previous preview. */
    fun speak(
        text: String,
        voiceName: String?,
        languageTag: String,
        rate: Float = 1.0f,
        pitch: Float = 1.0f
    ) {
        if (!_ready.value || tts == null) {
            // Buffer the request so when onInit completes it speaks immediately
            pendingSpeakAction = Runnable {
                speak(text, voiceName, languageTag, rate, pitch)
            }
            return
        }
        val engine = tts ?: return
        runCatching {
            engine.stop()
            val targetLocale = parseLocale(languageTag)
            val bestVoice = selectBestVoice(engine, targetLocale, voiceName)
            if (bestVoice != null) {
                engine.voice = bestVoice
            } else {
                // NEVER set engine.voice = null (throws NPE on Android)
                engine.language = targetLocale
            }
            engine.setSpeechRate(rate.coerceIn(0.7f, 1.5f))
            engine.setPitch(pitch.coerceIn(0.7f, 1.5f))
            _speaking.value = true
            engine.speak(text, TextToSpeech.QUEUE_FLUSH, null, "cc_preview_${System.nanoTime()}")
        }.onFailure { e ->
            Log.w(TAG, "speak failed: $e")
            _speaking.value = false
        }
    }

    fun stop() {
        pendingSpeakAction = null
        runCatching { tts?.stop() }
        main.post { _speaking.value = false }
    }

    /** Release the engine; must be called when the host ViewModel is cleared. */
    fun shutdown() {
        pendingSpeakAction = null
        runCatching { tts?.stop() }
        runCatching { tts?.shutdown() }
        tts = null
        main.post {
            _ready.value = false
            _speaking.value = false
            _voices.value = emptyList()
        }
    }

    private fun parseLocale(tag: String): Locale =
        runCatching { Locale.forLanguageTag(tag) }.getOrDefault(Locale("hi", "IN"))

    private fun prettyVoiceName(name: String, quality: Int): String {
        val lower = name.lowercase()
        val typeBadge = when {
            lower.contains("network") || lower.contains("neural") || lower.contains("wavenet") -> "Natural (HD)"
            quality >= Voice.QUALITY_HIGH -> "Enhanced"
            lower.contains("local") -> "Standard"
            else -> ""
        }
        val parts = lower.split('-').filter { it.isNotBlank() }
        if (parts.size < 3) return if (typeBadge.isNotEmpty()) "$name · $typeBadge" else name
        val lang = parts[0]
        val region = parts.getOrNull(1)?.uppercase() ?: ""
        val id = parts.drop(2).filter { it != "network" && it != "local" }.joinToString(" · ")
        val base = if (region.length == 2) "$lang-$region" else lang
        val items = mutableListOf<String>()
        items.add(base)
        if (id.isNotBlank()) items.add(id)
        if (typeBadge.isNotBlank()) items.add(typeBadge)
        return items.joinToString(" · ")
    }
}
