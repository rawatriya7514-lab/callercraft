package com.craftcaller.app.tts

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.speech.tts.TextToSpeech
import android.util.Log
import com.craftcaller.app.util.Prefs
import java.util.Locale
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Device text-to-speech announcer used at ring time (opposite of the
 * screen-scoped [VoiceKit], which is for onboarding/Settings previews).
 *
 * Design:
 *  - lazily initialises ONE TextToSpeech engine for the process, from any
 *    thread (CallScreeningService / broadcast receivers),
 *  - applies the saved announcer profile (voice / language / rate / pitch)
 *    for every utterance straight from encrypted prefs,
 *  - never blocks the caller thread (init is async; the latest utterance is
 *    queued while the engine warms up),
 *  - fails gracefully: a missing engine / failed init only logs — call flow
 *    keeps working (graceful TTS fallback),
 *  - releases the engine after IDLE_RELEASE_MS so no TTS connection is bound
 *    forever.
 */
object AnnouncerVoice : TextToSpeech.OnInitListener {

    private const val TAG = "AnnouncerVoice"
    private const val IDLE_RELEASE_MS = 2 * 60 * 1000L
    private const val DEDUPE_WINDOW_MS = 12_000L

    private val main = Handler(Looper.getMainLooper())
    private val initialising = AtomicBoolean(false)
    private val ready = AtomicBoolean(false)

    @Volatile
    private var tts: TextToSpeech? = null

    @Volatile
    private var appCtx: Context? = null

    @Volatile
    private var pendingText: String? = null

    @Volatile
    private var lastSpokenKey = ""

    @Volatile
    private var lastSpokenAt = 0L

    @Volatile
    private var currentVolume = 1f

    private var idleRunner: Runnable? = null

    /**
     * Speaks [text] with the saved announcer profile at relative loudness
     * [volume] (0..1). Safe to call from any thread. Repeats of the same text
     * within [DEDUPE_WINDOW_MS] are skipped (guards against PHONE_STATE +
     * CallScreening announcing the same call).
     */
    fun announce(context: Context, text: String, volume: Float = 1f) {
        val key = text.trim().lowercase()
        val now = System.currentTimeMillis()
        if (key.isBlank()) {
            Log.d(TAG, "empty announcement text — skipping")
            return
        }
        if (key == lastSpokenKey && (now - lastSpokenAt) < DEDUPE_WINDOW_MS) {
            Log.d(TAG, "skip duplicate announcement (${DEDUPE_WINDOW_MS / 1000}s window)")
            return
        }
        if (volume in 0.0f..0.01f) {
            Log.d(TAG, "announcement volume 0 — skipping speech")
            return
        }
        lastSpokenKey = key
        lastSpokenAt = now
        currentVolume = volume.coerceIn(0f, 1f)
        appCtx = context.applicationContext ?: context

        val engine = tts
        if (engine != null && ready.get()) {
            cancelIdleRelease()
            speakWith(engine, text)
            return
        }

        if (!initialising.compareAndSet(false, true)) {
            pendingText = text // engine warming up; speak the latest on ready
            return
        }
        pendingText = text
        runCatching {
            // Honor the user-selected TTS engine (Settings → Voice engine & voices);
            // defaults to Google Speech Services for natural human WaveNet voices if available.
            val configuredEngine = Prefs(appCtx!!).ttsEngineName.trim()
            val enginePackage = if (configuredEngine.isNotBlank()) {
                configuredEngine
            } else if (isGoogleTtsInstalled(appCtx!!)) {
                "com.google.android.tts"
            } else {
                ""
            }
            Log.d(TAG, "creating TextToSpeech engine (engine=${enginePackage.ifBlank { "system-default" }})")
            val created = enginePackage.takeIf { it.isNotBlank() }?.let { pkg ->
                runCatching { TextToSpeech(appCtx!!, this, pkg) }.getOrNull()
            } ?: TextToSpeech(appCtx!!, this)
            tts = created
        }.onFailure { e ->
            Log.w(TAG, "TextToSpeech creation failed: $e")
            initialising.set(false)
            pendingText = null
        }
    }

    /** Stops the current utterance (e.g. call got answered). */
    fun stop() {
        runCatching {
            tts?.stop()
            pendingText = null
        }
    }

    /** Immediate release; also invoked by the idle timer. */
    fun release() {
        cancelIdleRelease()
        runCatching { tts?.stop() }
        runCatching { tts?.shutdown() }
        tts = null
        pendingText = null
        ready.set(false)
        initialising.set(false)
    }

    // ------------------------------------------------------------ internals

    override fun onInit(status: Int) {
        main.post {
            if (status != TextToSpeech.SUCCESS || tts == null) {
                Log.w(TAG, "TTS init failed status=$status — falling back silently")
                release()
                return@post
            }
            ready.set(true)
            val engine = tts!!
            Log.d(
                TAG,
                "TTS engine ready; voices=${runCatching { engine.voices?.size }.getOrDefault(-1)}"
            )
            val text = pendingText
            pendingText = null
            if (!text.isNullOrEmpty()) speakWith(engine, text)
            scheduleIdleRelease()
        }
    }

    private fun speakWith(engine: TextToSpeech, text: String) {
        val context = appCtx
        if (context == null) {
            Log.w(TAG, "no context for prefs; skipping announcement")
            return
        }
        main.post {
            runCatching {
                val prefs = Prefs(context)
                engine.stop()

                // Voice: saved installed voice -> most natural neural voice -> engine default.
                val savedVoiceName = prefs.announcerVoiceName
                val targetLocale = Locale.forLanguageTag(prefs.announcerVoiceLang.ifBlank { "hi-IN" })
                val bestVoice = selectBestVoice(engine, targetLocale, savedVoiceName.takeIf { it.isNotBlank() })
                if (bestVoice != null) {
                    engine.voice = bestVoice
                } else {
                    // NEVER set engine.voice = null (throws NPE on Android)
                    engine.language = targetLocale
                }

                engine.setSpeechRate(prefs.announceRate)
                engine.setPitch(prefs.announcePitch)
                val params = android.os.Bundle().apply {
                    putFloat(android.speech.tts.TextToSpeech.Engine.KEY_PARAM_VOLUME, currentVolume)
                }
                engine.speak(text, TextToSpeech.QUEUE_FLUSH, params, "call_${System.nanoTime()}")
                Log.d(
                    TAG,
                    "announcing: \"$text\" (rate=${prefs.announceRate}, pitch=${prefs.announcePitch}, vol=$currentVolume, voice=${savedVoiceName.ifBlank { "system" }})"
                )
                scheduleIdleRelease()
            }.onFailure { e ->
                Log.w(TAG, "speak failed: $e")
            }
        }
    }

    private fun scheduleIdleRelease() {
        cancelIdleRelease()
        val runner = Runnable {
            Log.d(TAG, "idle release — shutting down TTS engine")
            release()
        }
        idleRunner = runner
        main.postDelayed(runner, IDLE_RELEASE_MS)
    }

    private fun cancelIdleRelease() {
        idleRunner?.let { main.removeCallbacks(it) }
        idleRunner = null
    }
}
