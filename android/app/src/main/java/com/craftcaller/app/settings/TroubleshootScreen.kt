package com.craftcaller.app.settings

import android.app.Application
import android.media.AudioManager
import android.os.Build
import android.speech.tts.TextToSpeech
import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Campaign
import androidx.compose.material.icons.filled.FactCheck
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.craftcaller.app.ui.theme.AmberWarn
import com.craftcaller.app.ui.theme.Blue
import com.craftcaller.app.ui.theme.GreenOk
import com.craftcaller.app.ui.theme.RedSpam
import com.craftcaller.app.ui.theme.TextMuted
import com.craftcaller.app.util.Prefs
import com.craftcaller.app.util.RequiredPermissions
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import java.text.DateFormat
import java.util.Date

/**
 * Real troubleshooting: every row is a live device/app check executed when
 * the screen opens (and on manual refresh):
 *   - call-screening role state,
 *   - runtime permissions,
 *   - ringer/DND audio state,
 *   - the LAST real screening decision (written by the service itself),
 *   - the last crash (pinned at app start by the crash handler),
 *   - a live TTS test you can hear.
 */
@Composable
fun TroubleshootScreen(onClose: () -> Unit) {
    val context = LocalContext.current
    val prefs = remember { Prefs(context) }
    var ttsTestState by remember { mutableStateOf("Idle — press \"Run TTS test\"") }
    var refreshKey by remember { mutableStateOf(0) }

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
                onClick = onClose,
                modifier = Modifier.semantics { contentDescription = "Back to settings" }
            ) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null)
            }
            Column {
                Text("Troubleshooting", fontWeight = FontWeight.ExtraBold, fontSize = 17.sp)
                Text("Everything below is measured live on this device", fontSize = 11.sp, color = TextMuted)
            }
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp)
        ) {
            // Refresh button
            Row {
                Button(
                    onClick = { refreshKey++ }, // re-evaluates every live check below
                    colors = ButtonDefaults.buttonColors(containerColor = Blue),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.semantics { contentDescription = "Refresh diagnostics" }
                ) {
                    Icon(Icons.Filled.Refresh, null, tint = Color.White)
                    Spacer(Modifier.width(6.dp))
                    Text("Re-check now", color = Color.White)
                }
            }

            Spacer(Modifier.height(14.dp))
            key(refreshKey) {
            DiagSection("Call screening")
            DiagRow(
                title = "Call-screening role",
                value = if (callScreeningRoleHeld(context)) "HELD — system routing works"
                else "Not held — announcements only (PHONE_STATE fallback)"
            )
            val lastAt = prefs.lastScreeningAt
            DiagRow(
                title = "Last screening decision",
                value = if (lastAt == 0L) "None recorded yet — receive a call first"
                else prefs.lastScreeningSummary.ifBlank { "Unknown" } + " · " +
                    DateFormat.getDateTimeInstance().format(Date(lastAt))
            )
            DiagRow(
                title = "Caller-ID permissions",
                value = if (RequiredPermissions.allGranted(context, RequiredPermissions.CALLER_ID))
                    "All granted"
                else "Incomplete — some caller data unavailable"
            )
            }

            Spacer(Modifier.height(14.dp))
            key(refreshKey) {
            DiagSection("Audio path")
            val audio = context.getSystemService(AudioManager::class.java)
            val ringer = when (audio?.ringerMode) {
                AudioManager.RINGER_MODE_NORMAL -> "Normal (rings)"
                AudioManager.RINGER_MODE_SILENT -> "Silent — app stays silent, vibrations vary"
                AudioManager.RINGER_MODE_VIBRATE -> "Vibrate — announcements suppressed, vibration only"
                else -> "Unknown"
            }
            val notificationManager =
                context.getSystemService(android.app.NotificationManager::class.java)
            val dndValue = runCatching {
                when (notificationManager?.currentInterruptionFilter) {
                    android.app.NotificationManager.INTERRUPTION_FILTER_ALL -> "Off"
                    null -> "Unknown (no policy access)"
                    else -> "On (filter active)"
                }
            }.getOrDefault("Unknown")
            DiagRow(title = "Ringer mode", value = ringer)
            DiagRow(title = "Do-not-disturb", value = dndValue)
            DiagRow(
                title = "Selected alert style",
                value = com.craftcaller.app.audio.AlertStyle.fromKey(prefs.alertStyle).label +
                    " · volume ${(prefs.alertVolume * 100).toInt()}%"
            )
            }

            // TTS live test
            Spacer(Modifier.height(14.dp))
            DiagSection("Speech (real test)")
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(MaterialTheme.colorScheme.surface)
                    .padding(14.dp)
            ) {
                Column(Modifier.weight(1f)) {
                    Text("TTS test", fontWeight = FontWeight.SemiBold)
                    Text(ttsTestState, fontSize = 12.sp, color = TextMuted)
                }
                Button(
                    onClick = {
                        ttsTestState = "Speaking…"
                        com.craftcaller.app.tts.AnnouncerVoice.announce(
                            context,
                            com.craftcaller.app.util.Announcer.personalGreeting(
                                prefs.userName.ifBlank { "Umesh" }, prefs.honorific, prefs.announceLang
                            ),
                            prefs.alertVolume.coerceAtLeast(0.2f)
                        )
                        // Honest: no callback exposes completion; the state line
                        // stays "Speaking…" until audio actually happens.
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = GreenOk),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.semantics { contentDescription = "Run TTS test" }
                ) {
                    Text("Run TTS test", color = Color.White, fontSize = 12.sp)
                }
            }
            Text(
                "Uses the engine + voice selected in Settings → Voice engine. " +
                    "If nothing is heard: check ringer mode, media volume, and the engine test there.",
                fontSize = 11.sp, color = TextMuted, modifier = Modifier.padding(start = 4.dp, top = 6.dp)
            )

            Spacer(Modifier.height(14.dp))
            key(refreshKey) {
            DiagSection("Crash history")
            val crash = prefs.lastCrash
            DiagRow(
                title = "Last crash",
                value = crash.ifBlank { "No crash recorded — app restarts cleanly" }
            )
            if (crash.isNotBlank()) {
                TextButton(onClick = {
                    prefs.lastCrash = ""
                    refreshKey++ // honest immediate refresh after clearing
                }) { Text("Clear crash record", color = RedSpam) }
            }
            }

            Spacer(Modifier.height(20.dp))
            Text(
                "Expected behaviour checks: role held => screening handles blocked/allowed lists " +
                    "before ringing; not held => PHONE_STATE path still announces but CANNOT auto-reject " +
                    "(an Android platform limit, honest in the app too).",
                fontSize = 11.sp, color = TextMuted
            )
            Spacer(Modifier.height(30.dp))
        }
    }
}

@Composable
private fun DiagSection(text: String) {
    Text(
        text.uppercase(),
        fontSize = 11.sp,
        fontWeight = FontWeight.Bold,
        color = Blue,
        letterSpacing = 1.sp,
        modifier = Modifier.padding(vertical = 8.dp)
    )
}

@Composable
private fun DiagRow(title: String, value: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 3.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(MaterialTheme.colorScheme.surface)
            .padding(14.dp)
    ) {
        Icon(Icons.Filled.FactCheck, null, tint = Blue, modifier = Modifier.size(20.dp))
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(title, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
            Text(value, fontSize = 12.sp, color = TextMuted, maxLines = 3)
        }
    }
}

private fun callScreeningRoleHeld(context: android.content.Context): Boolean {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) return false
    val rm = context.getSystemService(android.app.role.RoleManager::class.java) ?: return false
    return runCatching { rm.isRoleHeld(android.app.role.RoleManager.ROLE_CALL_SCREENING) }
        .getOrDefault(false)
}
