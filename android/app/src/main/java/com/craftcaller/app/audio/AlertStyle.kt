package com.craftcaller.app.audio

import android.content.Context
import com.craftcaller.app.R
import com.craftcaller.app.util.Prefs
import java.io.File

/**
 * Selectable alert sounds for the caller announcer.
 *
 * NOTE (honest-mode policy): alerts play BEFORE the spoken announcement —
 * sequential playback. Simultaneous background music + ringing is NOT
 * promised (device-dependent and unreliable); the continuous fallback chain
 * is: CUSTOM (if imported & decodable) -> selected bundled style -> speech
 * only, so a call is never broken by a missing sound.
 *
 * All bundled clips are original in-repo synthesized audio; see
 * android/app/LICENSES-AUDIO.md.
 */
enum class AlertStyle(
    val key: String,
    val label: String,
    val subtitle: String,
    val resId: Int?
) {
    VOICE_ONLY("voice_only", "Speech only", "Just the announcer's voice", null),
    CHIME("chime", "Chime", "Bell-style arpeggio", R.raw.alert_chime),
    FLUTE("melody", "Instrument melody", "Gentle pentatonic line", R.raw.alert_flute),
    JOY("joy", "Bright motif", "Rising instrumental", R.raw.alert_joy),
    CUSTOM("custom", "Imported audio", "Your own file", null);

    companion object {
        private const val CUSTOM_FILE_NAME = "imported_alert_audio"

        fun fromKey(key: String): AlertStyle =
            entries.firstOrNull { it.key == key } ?: CHIME

        /** Persisted file for user-imported audio (app-private storage). */
        fun customFile(context: Context): File = File(context.filesDir, CUSTOM_FILE_NAME)

        /**
         * Effective style for playback with a real fallback chain: an imported
         * style that lost its file falls back to CHIME (never breaks calls).
         */
        fun effective(context: Context): AlertStyle {
            val style = fromKey(Prefs(context).alertStyle)
            if (style == CUSTOM && !customFile(context).exists()) return CHIME
            return style
        }
    }
}
