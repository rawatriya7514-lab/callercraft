package com.craftcaller.app.settings

import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.RecordVoiceOver
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.craftcaller.app.audio.AlertStyle
import com.craftcaller.app.audio.AnnouncerAudio
import com.craftcaller.app.util.Announcer
import com.craftcaller.app.util.Prefs
import com.craftcaller.app.ui.theme.AmberWarn
import com.craftcaller.app.ui.theme.Blue
import com.craftcaller.app.ui.theme.BlueDark
import com.craftcaller.app.ui.theme.BlueSoft
import com.craftcaller.app.ui.theme.TextMuted

/**
 * Audio & alerts module (Prompt 4): alert styles (original bundled tones /
 * imported audio / speech-only), loudness, vibration, headphones/Bluetooth
 * behavior and honest system-behavior notes. Writes straight through to
 * (encrypted) prefs — no always-on component is involved.
 */
@Composable
fun AudioSettingsScreen(onClose: () -> Unit) {
    val context = LocalContext.current
    val prefs = Prefs(context)

    var style by remember { mutableStateOf(AlertStyle.fromKey(prefs.alertStyle)) }
    var volume by remember { mutableStateOf(prefs.alertVolume) }
    var vibrateOn by remember { mutableStateOf(prefs.vibrateOnAnnounce) }
    var vibPattern by remember { mutableStateOf(prefs.vibrationPattern) }
    var withHeadphones by remember { mutableStateOf(prefs.announceWithHeadphones) }
    var customName by remember { mutableStateOf(prefs.customAlertName) }
    var importedExists by remember { mutableStateOf(AlertStyle.customFile(context).exists()) }
    var playing by remember { mutableStateOf(false) }

    DisposableEffect(Unit) {
        onDispose { AnnouncerAudio.stopAll() }
    }

    fun persistStyle(s: AlertStyle) {
        style = s
        prefs.alertStyle = s.key
    }

    val importLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        runCatching {
            val target = AlertStyle.customFile(context)
            target.outputStream().use { out ->
                context.contentResolver.openInputStream(uri)?.use { it.copyTo(out) }
                    ?: throw IllegalStateException("cannot open selected audio")
            }
            if (target.length() > MAX_IMPORT_BYTES) error("file too large")
            prefs.customAlertName = uri.lastPathSegment?.substringAfterLast('/') ?: "imported audio"
            customName = prefs.customAlertName
            importedExists = true
            persistStyle(AlertStyle.CUSTOM)
            AnnouncerAudio.previewAlert(context, AlertStyle.CUSTOM)
            Toast.makeText(context, "Imported — previewing your audio", Toast.LENGTH_SHORT).show()
        }.onFailure { e ->
            AlertStyle.customFile(context).delete()
            importedExists = AlertStyle.customFile(context).exists()
            Toast.makeText(context, "Import failed (${e.message}); style unchanged", Toast.LENGTH_LONG).show()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 10.dp, vertical = 8.dp)
        ) {
            IconButton(
                onClick = { AnnouncerAudio.stopAll(); onClose() },
                modifier = Modifier.semantics { contentDescription = "Go back to settings" }
            ) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null) }
            Column {
                Text("Audio & alerts", fontWeight = FontWeight.ExtraBold, fontSize = 17.sp)
                Text("What rings with CraftCaller — and how", fontSize = 11.sp, color = TextMuted)
            }
        }

        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 22.dp)
        ) {
            AudioNoteCard(
                "Plays short alert style first, then your announcer speaks. Playing a full " +
                    "song behind the ring is not promised on all devices — this sequential " +
                    "mode is the reliable one, and a missing sound falls back to speech only."
            )

            Spacer(Modifier.height(22.dp))
            SectionCaptionAlert("Alert style")
            Spacer(Modifier.height(8.dp))

            AlertStyle.entries.forEach { s ->
                val selected = style == s
                val unavailable = s == AlertStyle.CUSTOM && !importedExists
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(if (selected) BlueSoft else MaterialTheme.colorScheme.surface)
                        .clickable(enabled = !unavailable) { persistStyle(s) }
                        .padding(horizontal = 12.dp, vertical = 10.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            if (selected) Icons.Filled.CheckCircle else Icons.Filled.MusicNote,
                            contentDescription = null,
                            tint = if (selected) Blue else TextMuted,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(Modifier.width(10.dp))
                        Column(Modifier.weight(1f)) {
                            Text(
                                s.label + if (unavailable) " (no file imported)" else "",
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 14.sp,
                                color = if (unavailable) TextMuted else MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                if (s == AlertStyle.CUSTOM && customName.isNotBlank()) customName else s.subtitle,
                                fontSize = 11.sp,
                                color = TextMuted,
                                maxLines = 1
                            )
                        }
                        if (!unavailable) {
                            IconButton(
                                onClick = { AnnouncerAudio.previewAlert(context, s) },
                                modifier = Modifier.semantics { contentDescription = "Preview ${s.label}" }
                            ) {
                                Icon(Icons.Filled.PlayArrow, null, tint = Blue)
                            }
                        }
                    }
                }
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                TextButton(onClick = { importLauncher.launch(arrayOf("audio/*")) }) {
                    Icon(Icons.Filled.GraphicEq, null, tint = Blue, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Import your audio", color = Blue)
                }
                Spacer(Modifier.weight(1f))
                if (importedExists) {
                    TextButton(onClick = {
                        AlertStyle.customFile(context).delete()
                        importedExists = false
                        customName = ""
                        prefs.customAlertName = ""
                        if (style == AlertStyle.CUSTOM) persistStyle(AlertStyle.CHIME)
                        Toast.makeText(context, "Imported audio removed", Toast.LENGTH_SHORT).show()
                    }) { Text("Remove", color = TextMuted, fontSize = 13.sp) }
                }
            }

            Spacer(Modifier.height(20.dp))
            SectionCaptionAlert("Loudness & feedback")
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(MaterialTheme.colorScheme.surface)
                    .padding(horizontal = 14.dp, vertical = 10.dp)
            ) {
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("Announcement volume", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                        Spacer(Modifier.weight(1f))
                        Text("${(volume * 100).toInt()}%", fontSize = 12.sp, color = TextMuted)
                    }
                    Slider(
                        value = volume,
                        onValueChange = {
                            volume = it
                            prefs.alertVolume = it
                        },
                        modifier = Modifier.semantics { contentDescription = "Announcement volume" }
                    )
                    Text(
                        "Relative gain applied to the alert tone and the spoken announcement.",
                        fontSize = 11.sp,
                        color = TextMuted
                    )
                }
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 10.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(MaterialTheme.colorScheme.surface)
                    .padding(14.dp)
            ) {
                Column(Modifier.weight(1f)) {
                    Text("Vibrate on announcements", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                    Text(
                        if (vibPattern == "short") "One short pulse" else "Double pulse",
                        fontSize = 11.sp,
                        color = TextMuted
                    )
                }
                if (vibrateOn) {
                    TextButton(onClick = {
                        vibPattern = if (vibPattern == "short") "double" else "short"
                        prefs.vibrationPattern = vibPattern
                        prefs.vibrateOnAnnounce = true
                        AnnouncerAudio.handleAnnouncement(context, "", preview = true)
                    }) {
                        Text(
                            if (vibPattern == "short") "Double" else "Short",
                            color = Blue,
                            fontSize = 13.sp
                        )
                    }
                }
                Switch(
                    checked = vibrateOn,
                    onCheckedChange = {
                        vibrateOn = it
                        prefs.vibrateOnAnnounce = it
                    },
                    modifier = Modifier.semantics { contentDescription = "Vibrate on announcements" }
                )
            }

            Spacer(Modifier.height(20.dp))
            SectionCaptionAlert("Headphones & Bluetooth")
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(MaterialTheme.colorScheme.surface)
                    .padding(14.dp)
            ) {
                Column(Modifier.weight(1f)) {
                    Text(
                        if (AnnouncerAudio.hasAudioAccessory(context)) "Headphones/Bluetooth connected"
                        else "No headphones or Bluetooth audio connected",
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 14.sp
                    )
                    Text(
                        if (withHeadphones) "Announcements always play (active route)"
                        else "Announcements pause while your headphones are in use",
                        fontSize = 11.sp,
                        color = TextMuted
                    )
                }
                Switch(
                    checked = withHeadphones,
                    onCheckedChange = {
                        withHeadphones = it
                        prefs.announceWithHeadphones = it
                    },
                    modifier = Modifier.semantics { contentDescription = "Announce even when headphones are connected" }
                )
            }

            Spacer(Modifier.height(20.dp))
            SectionCaptionAlert("Hear the full announcement")
            Spacer(Modifier.height(6.dp))
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(18.dp))
                    .background(Brush.horizontalGradient(listOf(Blue, BlueDark)))
                    .padding(14.dp)
            ) {
                Icon(
                    if (playing) Icons.Filled.Pause else Icons.Filled.RecordVoiceOver,
                    null,
                    tint = Color.White
                )
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text("Play full sequence", color = Color.White, fontWeight = FontWeight.Bold)
                    Text(
                        "Alert → announcement, exactly like an incoming call",
                        color = Color(0xFFCFE0FF),
                        fontSize = 11.sp
                    )
                }
                IconButton(
                    onClick = {
                        if (playing) {
                            AnnouncerAudio.stopAll()
                            playing = false
                        } else {
                            val sample = Announcer.personalGreeting(
                                prefs.userName.trim().ifBlank { "Umesh" },
                                prefs.honorific,
                                prefs.announceLang
                            )
                            AnnouncerAudio.handleAnnouncement(context, sample, preview = true)
                            playing = true
                        }
                    },
                    modifier = Modifier
                        .size(46.dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.18f))
                        .semantics { contentDescription = if (playing) "Stop preview" else "Play full announcement preview" }
                ) { Icon(Icons.Filled.PlayArrow, null, tint = Color.White) }
            }

            Spacer(Modifier.height(20.dp))
            AudioNoteCard(
                "System behavior: CraftCaller respects silent mode, ring volume 0 and " +
                    "Do-Not-Disturb — alerts and speech are simply suppressed then. " +
                    "Your default ringtone and dialer are never changed without explicit consent; " +
                    "the alert plays alongside your existing ringtone. No audio component runs " +
                    "in the background between calls."
            )
            Spacer(Modifier.height(10.dp))
            AudioNoteCard(
                "Bundled tones are original audio generated for CraftCaller (CC0, see " +
                    "LICENSES-AUDIO.md). Imported audio stays in CraftCaller's private storage " +
                    "and is never shared."
            )
            Spacer(Modifier.height(26.dp))
        }
    }
}

private const val MAX_IMPORT_BYTES = 8L * 1024 * 1024 // 8 MB sanity limit

@Composable
private fun SectionCaptionAlert(label: String) {
    Text(
        label.uppercase(),
        fontSize = 11.sp,
        fontWeight = FontWeight.Bold,
        color = Blue,
        letterSpacing = 1.sp
    )
}

@Composable
private fun AudioNoteCard(text: String) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(AmberWarn.copy(alpha = 0.10f))
            .padding(14.dp)
    ) {
        Text(text, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurface)
    }
}
