package com.craftcaller.app.settings

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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.CloudOff
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.craftcaller.app.data.remote.FirebaseRepository
import com.craftcaller.app.ui.theme.AmberWarn
import com.craftcaller.app.ui.theme.Blue
import com.craftcaller.app.ui.theme.GreenOk
import com.craftcaller.app.ui.theme.TextMuted
import com.craftcaller.app.util.Prefs

/**
 * Consolidated caller-ID behaviour settings. Every switch writes the real
 * pref key used by ring-time code paths (screening service, announcement
 * engine, resolver) — nothing here is decorative.
 */
@Composable
fun CallerIdSettingsScreen(
    onClose: () -> Unit,
    onOpenLists: () -> Unit,
    onOpenProfile: () -> Unit
) {
    val context = LocalContext.current
    val prefs = remember { Prefs(context) }

    var announceEnabled by remember { mutableStateOf(prefs.announceCallerEnabled) }
    var announceSaved by remember { mutableStateOf(prefs.announceSavedContacts) }
    var announceUnknown by remember { mutableStateOf(prefs.announceUnknown) }
    var announceRepeat by remember { mutableStateOf(prefs.announceRepeat) }
    var useContacts by remember { mutableStateOf(prefs.announceUseContacts) }
    var autoBlock by remember { mutableStateOf(prefs.autoBlockSpammers) }
    var communityRead by remember { mutableStateOf(prefs.communityReadEnabled) }

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
                Text("Caller-ID & spam blocking", fontWeight = FontWeight.ExtraBold, fontSize = 17.sp)
                Text("Real switches — the ring-time code reads these values", fontSize = 11.sp, color = TextMuted)
            }
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp)
        ) {
            SettingLabel("Announcement rules")
            SwitchRow(
                title = "Announce caller names",
                subtitle = "Spoken at ring time (sequential with alert sound)",
                checked = announceEnabled,
                onChecked = {
                    announceEnabled = it
                    prefs.announceCallerEnabled = it
                }
            )
            SwitchRow(
                title = "Saved contacts & known names",
                subtitle = "Speak the name when CraftCaller knows it",
                checked = announceSaved,
                onChecked = {
                    announceSaved = it
                    prefs.announceSavedContacts = it
                }
            )
            SwitchRow(
                title = "Unknown callers",
                subtitle = "\"You have a call from an unknown number\"",
                checked = announceUnknown,
                onChecked = {
                    announceUnknown = it
                    prefs.announceUnknown = it
                }
            )
            SwitchRow(
                title = "Repeat caller count",
                subtitle = "Adds \"…for the teesri/2nd time\" phrasing",
                checked = announceRepeat,
                onChecked = {
                    announceRepeat = it
                    prefs.announceRepeat = it
                }
            )
            SwitchRow(
                title = "Use saved contact names",
                subtitle = "Off: only app/community data — numbers stay unread",
                checked = useContacts,
                onChecked = {
                    useContacts = it
                    prefs.announceUseContacts = it
                }
            )

            Spacer(Modifier.height(20.dp))
            SettingLabel("Spam blocking")
            SwitchRow(
                title = "Auto-block top spammers",
                subtitle = "Rejects calls the community reported 10+ times " +
                    "(fixed honest threshold — never a guessed score)",
                checked = autoBlock,
                onChecked = {
                    autoBlock = it
                    prefs.autoBlockSpammers = it
                }
            )
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(MaterialTheme.colorScheme.surface)
                    .clickable(onClickLabel = "Block and allow lists") { onOpenLists() }
                    .padding(16.dp)
            ) {
                Column(Modifier.weight(1f)) {
                    Text("Block & allow lists", fontWeight = FontWeight.Bold)
                    Text(
                        "Manual numbers: blocked always reject; allowed never auto-reject",
                        fontSize = 12.sp, color = TextMuted, maxLines = 2
                    )
                }
                Icon(Icons.Filled.ChevronRight, null, tint = TextMuted)
            }

            Spacer(Modifier.height(20.dp))
            SettingLabel("Community caller-ID")
            SwitchRow(
                title = "Community number lookups",
                subtitle = "Read-only queries of user-submitted names; " +
                    "sends only the number you look up",
                checked = communityRead,
                onChecked = {
                    communityRead = it
                    prefs.communityReadEnabled = it
                }
            )
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(MaterialTheme.colorScheme.surface)
                    .padding(16.dp)
            ) {
                val configured = !FirebaseRepository.placeholderConfig
                Icon(
                    if (configured) Icons.Filled.CloudDone else Icons.Filled.CloudOff,
                    null,
                    tint = if (configured) GreenOk else AmberWarn,
                    modifier = Modifier.size(22.dp)
                )
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text("Business directory", fontWeight = FontWeight.Bold)
                    Text(
                        if (configured) "Configured (own public API is queried on demand)"
                        else "Not configured in this build — lookups run with contacts + community only",
                        fontSize = 12.sp, color = TextMuted, maxLines = 2
                    )
                }
            }
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(MaterialTheme.colorScheme.surface)
                    .clickable(onClickLabel = "Phonebook contribution consent") { onOpenProfile() }
                    .padding(16.dp)
            ) {
                Column(Modifier.weight(1f)) {
                    Text(
                        "Phonebook contribution: " +
                            if (prefs.communityUploadConsent) "ON (explicit consent given)"
                            else "OFF — your phonebook never leaves this phone",
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        "Informed-consent switch lives in Personal profile → Community & privacy",
                        fontSize = 12.sp, color = TextMuted, maxLines = 2
                    )
                }
                Icon(Icons.Filled.ChevronRight, null, tint = TextMuted)
            }

            Spacer(Modifier.height(20.dp))
            Text(
                "How identity is resolved (in order): saved contacts → local " +
                    "cache/your own names → opted-in community submissions → business " +
                    "directory. No match stays \"Unknown\" — the app never invents names.",
                fontSize = 11.sp,
                color = TextMuted
            )
            Spacer(Modifier.height(30.dp))
        }
    }
}

@Composable
private fun SettingLabel(text: String) {
    Text(
        text.uppercase(),
        fontSize = 11.sp,
        fontWeight = FontWeight.Bold,
        color = Blue,
        letterSpacing = 1.sp,
        modifier = Modifier.padding(vertical = 10.dp)
    )
}

@Composable
private fun SwitchRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    onChecked: (Boolean) -> Unit
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surface)
            .padding(14.dp)
    ) {
        Column(Modifier.weight(1f)) {
            Text(title, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
            Text(subtitle, fontSize = 11.sp, color = TextMuted, maxLines = 3)
        }
        Spacer(Modifier.width(8.dp))
        Switch(
            checked = checked,
            onCheckedChange = onChecked,
            modifier = Modifier.semantics { contentDescription = title }
        )
    }
}
