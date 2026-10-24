package com.craftcaller.app.audio

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioDeviceInfo
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.media.MediaPlayer
import android.net.Uri
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.util.Log
import com.craftcaller.app.tts.AnnouncerVoice
import com.craftcaller.app.util.Prefs

/**
 * Sequential caller-alert player: [optional alert sound] -> spoken
 * announcement -> release everything. Plays from existing call events
 * (CallScreeningService / PHONE_STATE) — no extra services or receivers, and
 * nothing runs between calls (battery-neutral).
 *
 * System-state contract (checked at play time, never assumed):
 *  - Ringer mode != NORMAL or ring volume == 0 -> no audible alert, no
 *    speech. Vibration still honors the user's own switch.
 *  - Do-Not-Disturb (any interruption filter other than ALL/UNKNOWN) ->
 *    fully muted + no speech (correct DND behavior).
 *  - Headphones/Bluetooth audio output active -> audible only if the user
 *    kept the "announce even with headphones" switch on (never hijack the
 *    user's own music).
 *  - Transient audio focus requested for the alert; abandoned when playback
 *    completes. Refused focus = speech-only fallback.
 *  - Any failure falls through to speech-only announcements: a call is never
 *    affected by audio-module problems.
 */
object AnnouncerAudio {

    private const val TAG = "AnnouncerAudio"

    @Volatile
    private var activePlayer: MediaPlayer? = null

    @Volatile
    private var audioManager: AudioManager? = null

    @Volatile
    private var activeFocus: AudioFocusRequest? = null

    /**
     * Full sequence: vibrate (per user switch), play the configured alert
     * sound, then the spoken line [speakText] at the configured volume.
     * [preview] = true is the manual Settings preview: it deliberately
     * bypasses the silent/DND/headphone guards (the user pressed the button)
     * but still honors focus + resource cleanup.
     */
    fun handleAnnouncement(context: Context, speakText: String, preview: Boolean = false) {
        runCatching {
            val prefs = Prefs(context)
            val guard = audibleAllowed(context, prefs, preview)
            if (prefs.vibrateOnAnnounce) vibrate(context, prefs)
            if (!guard.audible) {
                Log.d(TAG, "audible suppressed (${guard.reason})")
                if (!preview) return
            }
            val style = AlertStyle.effective(context)
            val volume = prefs.alertVolume.coerceIn(0f, 1f)
            val playAlertMusic = guard.audible && style != AlertStyle.VOICE_ONLY

            if (playAlertMusic) {
                playAlert(context, style, speakText, volume)
            } else {
                speak(context, speakText, volume)
            }
        }.onFailure { e ->
            Log.w(TAG, "announcement failed; falling back to speech: $e")
            AnnouncerVoice.announce(context, speakText)
        }
    }

    /** Stops any alert + speech immediately (call answered / flow torn down). */
    fun stopAll() {
        runCatching { activePlayer?.stop() }
        releasePlayer()
        AnnouncerVoice.stop()
    }

    /**
     * Settings-only A/B preview of ONE alert style (speech suppressed) so the
     * user can compare alert sounds without launching whole announcements.
     * Bypasses the silent-guard (explicit button press) but honors cleanup.
     */
    fun previewAlert(context: Context, style: AlertStyle) {
        runCatching {
            val volume = Prefs(context).alertVolume.coerceIn(0f, 1f)
            playAlert(context, style, speakText = "", volume = volume, thenSpeak = false)
        }
    }

    // ----------------------------------------------------------- guards

    data class Guard(val audible: Boolean, val reason: String)

    fun audibleAllowed(context: Context, prefs: Prefs, preview: Boolean): Guard {
        if (preview) return Guard(true, "preview")

        val am = context.getSystemService(AudioManager::class.java)
            ?: return Guard(false, "no audio manager")

        if (am.ringerMode != AudioManager.RINGER_MODE_NORMAL) {
            return Guard(false, "ringer=${am.ringerMode}")
        }
        if (am.getStreamVolume(AudioManager.STREAM_RING) == 0) {
            return Guard(false, "ring volume 0")
        }

        val filter = runCatching {
            context.getSystemService(android.app.NotificationManager::class.java)
                .currentInterruptionFilter
        }.getOrDefault(android.app.NotificationManager.INTERRUPTION_FILTER_ALL)
        if (filter != android.app.NotificationManager.INTERRUPTION_FILTER_ALL &&
            filter != android.app.NotificationManager.INTERRUPTION_FILTER_UNKNOWN
        ) {
            return Guard(false, "DND filter=$filter")
        }

        if (!prefs.announceWithHeadphones && hasAudioAccessory(context, am)) {
            return Guard(false, "headphones connected")
        }
        return Guard(true, "ok")
    }

    /** Any wired/Bluetooth/USB audio accessory the user may be listening on. */
    fun hasAudioAccessory(
        context: Context,
        am: AudioManager = context.getSystemService(AudioManager::class.java)!!
    ): Boolean = runCatching {
        am.getDevices(AudioManager.GET_DEVICES_OUTPUTS).any {
            it.type == AudioDeviceInfo.TYPE_WIRED_HEADSET ||
                it.type == AudioDeviceInfo.TYPE_WIRED_HEADPHONES ||
                it.type == AudioDeviceInfo.TYPE_BLUETOOTH_A2DP ||
                it.type == AudioDeviceInfo.TYPE_BLUETOOTH_SCO ||
                it.type == AudioDeviceInfo.TYPE_BLE_HEADSET ||
                it.type == AudioDeviceInfo.TYPE_USB_HEADSET
        }
    }.getOrDefault(false)

    // -------------------------------------------------------- vibration

    private fun vibrate(context: Context, prefs: Prefs) {
        runCatching {
            val vibrator: Vibrator? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                context.getSystemService(VibratorManager::class.java)?.defaultVibrator
            } else {
                context.getSystemService(Vibrator::class.java)
            }
            if (vibrator == null || !vibrator.hasVibrator()) return
            val timings = if (prefs.vibrationPattern == "short") {
                longArrayOf(0, 220)
            } else {
                longArrayOf(0, 90, 90, 90)
            }
            vibrator.vibrate(VibrationEffect.createWaveform(timings, -1))
        }.onFailure { Log.w(TAG, "vibrate failed: $it") }
    }

    // -------------------------------------------------------- playback

    private fun playAlert(
        context: Context,
        style: AlertStyle,
        speakText: String,
        volume: Float,
        thenSpeak: Boolean = true
    ) {
        val am = context.getSystemService(AudioManager::class.java)
        releasePlayer() // strictly one player at a time

        var focus: AudioFocusRequest? = null
        if (am != null) {
            focus = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN_TRANSIENT)
                .setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_NOTIFICATION_RINGTONE)
                        .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                        .build()
                )
                .build()
            val granted = runCatching {
                am.requestAudioFocus(focus)
            }.getOrDefault(AudioManager.AUDIOFOCUS_REQUEST_FAILED)
            if (granted != AudioManager.AUDIOFOCUS_REQUEST_GRANTED) {
                Log.d(TAG, "audio focus refused; falling back to speech-only")
                speak(context, speakText, volume)
                return
            }
            audioManager = am
            activeFocus = focus
        }

        val player = MediaPlayer()
        activePlayer = player
        runCatching {
            player.setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_NOTIFICATION_RINGTONE)
                    .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                    .build()
            )
            if (style == AlertStyle.CUSTOM) {
                player.setDataSource(AlertStyle.customFile(context).absolutePath)
            } else {
                player.setDataSource(
                    context,
                    Uri.parse("android.resource://${context.packageName}/${style.resId ?: 0}")
                )
            }
            player.setVolume(volume, volume)
            player.prepare()
            player.setOnCompletionListener {
                releasePlayer()
                if (thenSpeak && speakText.isNotBlank()) {
                    speak(context, speakText, volume)
                }
            }
            player.setOnErrorListener { _, what, extra ->
                Log.w(TAG, "alert playback error what=$what extra=$extra — speech-only fallback")
                releasePlayer()
                if (thenSpeak && speakText.isNotBlank()) {
                    speak(context, speakText, volume)
                }
                true
            }
            player.start()
        }.onFailure { e ->
            Log.w(TAG, "alert could not start (${style.key}); speech-only fallback: $e")
            releasePlayer()
            if (thenSpeak && speakText.isNotBlank()) {
                speak(context, speakText, volume)
            }
        }
    }

    private fun speak(context: Context, text: String, volume: Float) =
        AnnouncerVoice.announce(context, text, volume)

    private fun releasePlayer() {
        // Abandon focus with the same AudioManager that granted it.
        val focus = activeFocus
        val am = audioManager
        activeFocus = null
        audioManager = null
        if (focus != null && am != null) {
            runCatching { am.abandonAudioFocusRequest(focus) }
        }
        val player = activePlayer
        activePlayer = null
        if (player != null) {
            runCatching { player.setOnCompletionListener(null) }
            runCatching { player.setOnErrorListener(null) }
            runCatching {
                if (player.isPlaying) player.stop()
            }
            runCatching { player.release() }
        }
    }
}
