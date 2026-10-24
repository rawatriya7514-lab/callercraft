package com.craftcaller.app.settings

import android.app.Application
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.craftcaller.app.onboarding.AnnounceToggleRow
import com.craftcaller.app.onboarding.CallSetupCard
import com.craftcaller.app.onboarding.GenderStep
import com.craftcaller.app.onboarding.HonorificStep
import com.craftcaller.app.onboarding.LanguageStep
import com.craftcaller.app.onboarding.NameStep
import com.craftcaller.app.onboarding.ProfileViewModel
import com.craftcaller.app.onboarding.VoiceStep
import com.craftcaller.app.ui.common.rememberVm
import com.craftcaller.app.ui.theme.Blue
import com.craftcaller.app.ui.theme.TextMuted
import com.craftcaller.app.util.Prefs

/**
 * In-Settings editor for the personal caller-announcer profile
 * (Premium → "Personal profile & voice"). Reuses the same step blocks as the
 * first-launch onboarding; writes through encrypted prefs on Save.
 */
@Composable
fun ProfileEditorScreen(onClose: () -> Unit) {
    val context = LocalContext.current
    val vm: ProfileViewModel = rememberVm { ProfileViewModel(context.applicationContext as Application) }
    var playing by remember { mutableStateOf(false) }

    DisposableEffect(Unit) {
        onDispose { vm.stopPreview() }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // ------------------------------------------------------------- top bar
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 10.dp, vertical = 8.dp)
        ) {
            IconButton(
                onClick = {
                    vm.stopPreview()
                    onClose()
                },
                modifier = Modifier.semantics { contentDescription = "Go back to settings" }
            ) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null)
            }
            Column {
                Text("Personal profile & voice", fontWeight = FontWeight.ExtraBold, fontSize = 17.sp)
                Text(
                    "How CraftCaller greets you on every call",
                    fontSize = 11.sp,
                    color = TextMuted
                )
            }
        }

        // ------------------------------------------------------------- content
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 22.dp)
        ) {
            CallSetupCard()
            Spacer(Modifier.height(24.dp))

            ProfileSection("Identity", vm)

            Spacer(Modifier.height(24.dp))
            SectionCaption("Way of address")
            GenderStep(vm)
            Spacer(Modifier.height(14.dp))
            HonorificStep(vm)

            Spacer(Modifier.height(24.dp))
            SectionCaption("Announcement language")
            LanguageStep(vm)

            Spacer(Modifier.height(24.dp))
            SectionCaption("Announcer voice")
            VoiceStep(vm, playing = playing, onPlayToggle = { playing = !playing })

            Spacer(Modifier.height(24.dp))
            SectionCaption("Announcement routing")
            AnnounceToggleRow(vm)
            Spacer(Modifier.height(10.dp))
            ToggleRow(
                title = "Saved contacts & known names",
                subtitle = "Rahul Sharma ke naam ke saath",
                checked = vm.announceSaved,
                onChange = { vm.announceSaved = it }
            )
            ToggleRow(
                title = "Unknown callers",
                subtitle = "\u201CUnknown number\u201D phrasing",
                checked = vm.announceUnknown,
                onChange = { vm.announceUnknown = it }
            )
            ToggleRow(
                title = "Repeat callers",
                subtitle = "\u201C…teesri call\u201D phrasing for repeat rings",
                checked = vm.announceRepeat,
                onChange = { vm.announceRepeat = it }
            )
            ToggleRow(
                title = "Use saved contact names",
                subtitle = "Off: only app data, numbers stay unread",
                checked = vm.useContacts,
                onChange = { vm.useContacts = it }
            )

            Spacer(Modifier.height(24.dp))
            SectionCaption("Voice delivery")
            SliderRow(
                label = "Speech speed",
                valueText = "${String.format("%.2f×", vm.rate)}",
                value = vm.rate,
                onChange = { vm.rate = it }
            )
            SliderRow(
                label = "Pitch",
                valueText = "${String.format("%.2f×", vm.pitch)}",
                value = vm.pitch,
                onChange = { vm.pitch = it }
            )

            Spacer(Modifier.height(24.dp))
            SectionCaption("Community caller-ID & privacy")
            CommunityPrivacySection()

            Spacer(Modifier.height(24.dp))
            Button(
                onClick = {
                    playing = false
                    vm.stopPreview()
                    val addressed = vm.save()
                    Toast.makeText(
                        context,
                        "Saved — announcements will now say \"$addressed\".",
                        Toast.LENGTH_SHORT
                    ).show()
                    onClose()
                },
                colors = ButtonDefaults.buttonColors(containerColor = Blue),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .semantics { contentDescription = "Save profile changes" }
            ) {
                Text("Save changes", fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
            }

            Spacer(Modifier.height(12.dp))
            Text(
                "Stored encrypted on this device only. Your name, address and voice never leave the phone.",
                fontSize = 11.sp,
                color = TextMuted,
                modifier = Modifier.padding(bottom = 28.dp)
            )
        }
    }
}

@Composable
private fun ProfileSection(label: String, vm: ProfileViewModel) {
    Column {
        SectionCaption(label)
        Spacer(Modifier.height(10.dp))
        NameStep(vm)
    }
}

@Composable
private fun SectionCaption(label: String) {
    Text(
        label.uppercase(),
        fontSize = 11.sp,
        fontWeight = FontWeight.Bold,
        color = Blue,
        letterSpacing = 1.sp
    )
}

@Composable
private fun ToggleRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    onChange: (Boolean) -> Unit
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
            Text(subtitle, fontSize = 11.sp, color = TextMuted)
        }
        Switch(
            checked = checked,
            onCheckedChange = onChange,
            modifier = Modifier.semantics { contentDescription = title }
        )
    }
}

@Composable
private fun SliderRow(
    label: String,
    valueText: String,
    value: Float,
    onChange: (Float) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surface)
            .padding(horizontal = 14.dp, vertical = 10.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(label, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
            Spacer(Modifier.weight(1f))
            Text(valueText, fontSize = 12.sp, color = TextMuted)
        }
        Slider(
            value = value,
            onValueChange = onChange,
            valueRange = 0.5f..2f,
            modifier = Modifier.semantics { contentDescription = label }
        )
        Text(
            "Preview uses this instantly; calls apply it at ring time.",
            fontSize = 11.sp,
            color = TextMuted
        )
    }
}

/**
 * Community caller-ID privacy controls: read-only lookups (opt-out-able) and
 * the contribute switch which requires an explicit informed-consent
 * confirmation before it can ever be enabled. The phonebook upload worker is
 * hard-gated on this pref in com.craftcaller.app.work.ContactSyncWorker.
 */
@Composable
private fun CommunityPrivacySection() {
    val context = LocalContext.current
    val prefs = remember { Prefs(context) }
    var readEnabled by remember { mutableStateOf(prefs.communityReadEnabled) }
    var uploadConsent by remember { mutableStateOf(prefs.communityUploadConsent) }
    var showUploadExplain by remember { mutableStateOf(false) }

    Column {
        ToggleRow(
            title = "Community number lookups",
            subtitle = "Read-only: sends only the number you look up",
            checked = readEnabled,
            onChange = {
                readEnabled = it
                prefs.communityReadEnabled = it
            }
        )
        Spacer(Modifier.height(10.dp))
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(MaterialTheme.colorScheme.surface)
                .padding(14.dp)
        ) {
            Column(Modifier.weight(1f)) {
                Text("Contribute my saved names", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                Text(
                    if (uploadConsent) "On: your phonebook names are shared with the directory"
                    else "Off: your phonebook never leaves this phone",
                    fontSize = 11.sp,
                    color = TextMuted
                )
            }
            Switch(
                checked = uploadConsent,
                onCheckedChange = { want ->
                    if (want) showUploadExplain = true // informed consent first
                    else {
                        uploadConsent = false
                        prefs.communityUploadConsent = false
                    }
                },
                modifier = Modifier.semantics { contentDescription = "Contribute my saved names" }
            )
        }
        Text(
            "Standing policy: lookups query community data read-only; contributing uploads " +
                "NAME→NUMBER pairs from your contacts. Reports/corrections always show " +
                "this same explicit choice first. Nothing is uploaded silently.",
            fontSize = 10.sp,
            color = TextMuted,
            modifier = Modifier.padding(top = 8.dp, start = 2.dp)
        )
    }

    if (showUploadExplain) {
        AlertDialog(
            onDismissRequest = { showUploadExplain = false },
            title = { Text("Share your saved names?", fontWeight = FontWeight.Bold) },
            text = {
                Text(
                    "If you continue, CraftCaller will upload NAME → NUMBER pairs from saving " +
                        "your contacts to the shared community directory so other CraftCaller " +
                        "users can identify those callers.\n\n" +
                        "What is uploaded: contact names + their numbers.\n" +
                        "What is NOT uploaded: photos, emails, calls, messages.\n" +
                        "You can switch this off anytime; switching off stops future uploads.",
                    fontSize = 13.sp
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    uploadConsent = true
                    prefs.communityUploadConsent = true
                    showUploadExplain = false
                }) { Text("I understand — enable", color = Color.White) }
            },
            dismissButton = {
                TextButton(onClick = { showUploadExplain = false }) {
                    Text("Not now", color = TextMuted)
                }
            }
        )
    }
}
