package com.craftcaller.app.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.FactCheck
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.RecordVoiceOver
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SettingsSuggest
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.craftcaller.app.audio.AlertStyle
import com.craftcaller.app.data.remote.FirebaseRepository
import com.craftcaller.app.ui.theme.AmberWarn
import com.craftcaller.app.ui.theme.Blue
import com.craftcaller.app.ui.theme.TextMuted
import com.craftcaller.app.ui.theme.ThemeState
import com.craftcaller.app.util.Prefs

/**
 * The complete settings index. Every destination opens a fully-working,
 * persisted surface — no decorative entries. Live subtexts mirror the actual
 * stored values, so what the user reads is the app's real state.
 */
@Composable
fun SettingsHubScreen(
    onClose: () -> Unit,
    onOpenProfile: () -> Unit,
    onOpenAudio: () -> Unit,
    onOpenVoice: () -> Unit,
    onOpenCallerId: () -> Unit,
    onOpenLists: () -> Unit,
    onOpenPermissions: () -> Unit,
    onOpenData: () -> Unit,
    onOpenTroubleshoot: () -> Unit,
    onOpenSearch: () -> Unit
) {
    val context = LocalContext.current
    val prefs = remember { Prefs(context) }
    var dark by remember { mutableStateOf(ThemeState.isDark) }

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
                modifier = Modifier.semantics { contentDescription = "Close settings" }
            ) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null)
            }
            Column {
                Text("Settings", fontWeight = FontWeight.ExtraBold, fontSize = 17.sp)
                Text("CraftCaller · everything here works & persists", fontSize = 11.sp, color = TextMuted)
            }
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp)
        ) {
            HubRow(
                title = "Personal profile & voice",
                subtitle = listOf(
                    prefs.userName.ifBlank { "no name yet" },
                    prefs.honorific.label.lowercase(),
                    prefs.announceLang.label
                ).joinToString(" · "),
                icon = Icons.Filled.RecordVoiceOver,
                onClick = onOpenProfile
            )
            HubRow(
                title = "Voice engine",
                subtitle = (if (prefs.ttsEngineName.isBlank())
                    "System default"
                else "Custom: ${shortEngineName(prefs.ttsEngineName)}") + " · " +
                    (if (prefs.announcerVoiceName.isBlank()) "system voice" else "voice set") +
                    " · ${"%.2f".format(java.util.Locale.US, prefs.announceRate)}× speed",
                icon = Icons.Filled.GraphicEq,
                onClick = onOpenVoice
            )
            HubRow(
                title = "Audio & alerts",
                subtitle = AlertStyle.fromKey(prefs.alertStyle).label + " · " +
                    "${(prefs.alertVolume * 100).toInt()}% volume · " +
                    if (prefs.vibrateOnAnnounce) "vibration on" else "no vibration",
                icon = Icons.Filled.MusicNote,
                onClick = onOpenAudio
            )
            HubRow(
                title = "Caller-ID & spam blocking",
                subtitle = listOf(
                    if (prefs.announceCallerEnabled) "announcements on" else "announcements off",
                    if (prefs.communityReadEnabled) "community lookups on" else "community lookups off",
                    if (prefs.autoBlockSpammers) "auto-block 10+ reports" else "auto-block off"
                ).joinToString(" · "),
                icon = Icons.Filled.Shield,
                onClick = onOpenCallerId
            )
            HubRow(
                title = "Block & allow lists",
                subtitle = "Manage blocked and allowed numbers",
                icon = Icons.Filled.List,
                onClick = onOpenLists
            )
            HubRow(
                title = "Permissions & role",
                subtitle = "Contacts · phone · SMS · mic · notifications · screening role",
                icon = Icons.Filled.SettingsSuggest,
                onClick = onOpenPermissions
            )
            HubRow(
                title = "Data & privacy",
                subtitle = if (FirebaseRepository.placeholderConfig)
                    "Offline build — community directory not configured" +
                        " · cache & data controls inside"
                else "Cache, call log & full data controls",
                icon = Icons.Filled.CloudOff,
                tint = if (FirebaseRepository.placeholderConfig) AmberWarn else Blue,
                onClick = onOpenData
            )
            HubRow(
                title = "Troubleshooting",
                subtitle = "Live diagnostics: role, audio, TTS, last screening, crashes",
                icon = Icons.Filled.FactCheck,
                onClick = onOpenTroubleshoot
            )

            Spacer(Modifier.height(12.dp))
            HubRow(
                title = "Search any number",
                subtitle = "Identify unsaved numbers with the full caller-ID ladder",
                icon = Icons.Filled.Search,
                onClick = onOpenSearch
            )

            Spacer(Modifier.height(14.dp))
            // Appearance — the same persisted pref the startup theme reads.
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(MaterialTheme.colorScheme.surface)
                    .padding(16.dp)
            ) {
                Icon(Icons.Filled.DarkMode, null, tint = Blue, modifier = Modifier.size(22.dp))
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text("Dark mode", fontWeight = FontWeight.Bold)
                    Text("Amazon-night theme across the app", fontSize = 12.sp, color = TextMuted)
                }
                Switch(
                    checked = dark,
                    onCheckedChange = {
                        dark = it
                        ThemeState.isDark = it
                        prefs.darkTheme = it
                    },
                    modifier = Modifier.semantics { contentDescription = "Dark mode" }
                )
            }

            Spacer(Modifier.height(18.dp))
            Text(
                "CraftCaller · Kotlin, Compose, Room, Firebase (Apache-2.0).\n" +
                    "Phone parsing: Google libphonenumber (Apache-2.0).\n" +
                    "Bundled alert sounds are original recordings. Community data comes only " +
                    "from CraftCaller users' explicit submissions — never private third-party caller databases.",
                fontSize = 11.sp,
                color = TextMuted
            )
            Spacer(Modifier.height(30.dp))
        }
    }
}

@Composable
private fun HubRow(
    title: String,
    subtitle: String,
    icon: ImageVector,
    onClick: () -> Unit,
    tint: Color = Blue
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surface)
            .clickable(onClickLabel = title) { onClick() }
            .padding(16.dp)
    ) {
        Icon(icon, null, tint = tint, modifier = Modifier.size(22.dp))
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(title, fontWeight = FontWeight.Bold)
            Text(subtitle, fontSize = 12.sp, color = TextMuted, maxLines = 2)
        }
        Icon(Icons.Filled.ChevronRight, null, tint = TextMuted)
    }
}

private fun shortEngineName(pkg: String): String =
    pkg.substringAfterLast('.').replaceFirstChar { it.uppercase() }
