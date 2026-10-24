package com.craftcaller.app.onboarding

import android.app.Application
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Man
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.RecordVoiceOver
import androidx.compose.material.icons.filled.Woman
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.craftcaller.app.ui.common.Avatar
import com.craftcaller.app.ui.common.rememberVm
import com.craftcaller.app.ui.theme.AmberWarn
import com.craftcaller.app.ui.theme.Blue
import com.craftcaller.app.ui.theme.BlueDark
import com.craftcaller.app.ui.theme.BlueSoft
import com.craftcaller.app.ui.theme.TextMuted
import com.craftcaller.app.util.Announcer
import com.craftcaller.app.util.AnnounceLang
import com.craftcaller.app.util.Gender
import com.craftcaller.app.util.Honorific
import com.craftcaller.app.util.AvailableVoice

/** Wizard steps of first-launch personal-profile onboarding. */
private enum class Step { WELCOME, NAME, GENDER, HONORIFIC, LANGUAGE, VOICE, DONE }

@Composable
fun OnboardingScreen(onDone: () -> Unit) {
    val context = LocalContext.current
    val vm: ProfileViewModel = rememberVm { ProfileViewModel(context.applicationContext as Application) }
    var step by rememberSaveable { mutableStateOf(Step.WELCOME) }
    var playing by remember { mutableStateOf(false) }

    // Never leak a playing preview across steps.
    DisposableEffect(step) {
        onDispose { if (playing) { vm.stopPreview(); playing = false } }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        StepHeader(step)

        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 22.dp)
        ) {
            when (step) {
                Step.WELCOME -> WelcomeStep()
                Step.NAME -> NameStep(vm)
                Step.GENDER -> GenderStep(vm)
                Step.HONORIFIC -> HonorificStep(vm)
                Step.LANGUAGE -> LanguageStep(vm)
                Step.VOICE -> VoiceStep(vm, playing = playing, onPlayToggle = { playing = !playing })
                Step.DONE -> DoneStep(vm, playing = playing, onPlayToggle = { playing = !playing })
            }
            Spacer(Modifier.height(18.dp))
        }

        StepFooter(
            step = step,
            canContinue = step != Step.NAME || vm.name.isNotBlank(),
            onBack = { step = prevStep(step) },
            onContinue = {
                playing = false
                if (step == Step.DONE) {
                    vm.save()
                    onDone()
                } else {
                    step = nextStep(step)
                }
            },
            continueLabel = if (step == Step.DONE) "Save & continue" else "Continue"
        )
    }
}

private fun nextStep(step: Step) = when (step) {
    Step.WELCOME -> Step.NAME
    Step.NAME -> Step.GENDER
    Step.GENDER -> Step.HONORIFIC
    Step.HONORIFIC -> Step.LANGUAGE
    Step.LANGUAGE -> Step.VOICE
    Step.VOICE -> Step.DONE
    Step.DONE -> Step.DONE
}

private fun prevStep(step: Step) = when (step) {
    Step.NAME -> Step.WELCOME
    Step.GENDER -> Step.NAME
    Step.HONORIFIC -> Step.GENDER
    Step.LANGUAGE -> Step.HONORIFIC
    Step.VOICE -> Step.LANGUAGE
    else -> Step.WELCOME
}

// ------------------------------------------------------------------ chrome

@Composable
private fun StepHeader(step: Step) {
    val index = step.ordinal
    val total = Step.entries.size
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 22.dp, vertical = 14.dp)
            .semantics { contentDescription = "Onboarding step ${index + 1} of $total" }
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                "CraftCaller",
                fontWeight = FontWeight.ExtraBold,
                fontSize = 15.sp,
                color = Blue
            )
            Spacer(Modifier.weight(1f))
            if (step != Step.WELCOME) {
                Text(
                    "${index + 1} / ${total - 1}",
                    fontSize = 12.sp,
                    color = TextMuted,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
        Spacer(Modifier.height(10.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(5.dp)) {
            repeat(total) { i ->
                val active = i <= index
                val width by animateDpAsState(if (i == index) 26.dp else 8.dp, label = "dot")
                Box(
                    modifier = Modifier
                        .height(6.dp)
                        .width(width)
                        .clip(RoundedCornerShape(3.dp))
                        .background(
                            animateColorAsState(
                                if (active) Blue else MaterialTheme.colorScheme.outline,
                                label = "dotColor"
                            ).value
                        )
                )
            }
        }
    }
}

@Composable
private fun StepFooter(
    step: Step,
    canContinue: Boolean,
    onBack: () -> Unit,
    onContinue: () -> Unit,
    continueLabel: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(horizontal = 22.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (step != Step.WELCOME) {
            TextButton(
                onClick = onBack,
                modifier = Modifier.semantics { contentDescription = "Go back to previous step" }
            ) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null, tint = TextMuted)
                Spacer(Modifier.width(4.dp))
                Text("Back", color = TextMuted)
            }
        }
        Spacer(Modifier.weight(1f))
        Button(
            onClick = onContinue,
            enabled = canContinue,
            colors = ButtonDefaults.buttonColors(containerColor = Blue),
            modifier = Modifier
                .height(52.dp)
                .semantics { contentDescription = continueLabel }
        ) {
            Text(continueLabel, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
        }
    }
}

// ------------------------------------------------------------------ steps

@Composable
private fun WelcomeStep() {
    Column(modifier = Modifier.fillMaxWidth()) {
        Spacer(Modifier.height(18.dp))
        Box(
            modifier = Modifier
                .size(96.dp)
                .clip(CircleShape)
                .background(Brush.verticalGradient(listOf(Blue, BlueDark))),
            contentAlignment = Alignment.Center
        ) {
            Text("C", color = Color.White, fontSize = 40.sp, fontWeight = FontWeight.ExtraBold)
        }
        Spacer(Modifier.height(20.dp))
        Text("Your personal caller announcer", fontSize = 26.sp, fontWeight = FontWeight.ExtraBold)
        Text(
            "Know who is calling — and be greeted the way you like.",
            fontSize = 14.sp,
            color = TextMuted,
            modifier = Modifier.padding(top = 6.dp, bottom = 22.dp)
        )
        IntroCard(
            title = "Hear your name, not a number",
            body = "\u201CUmesh sir, aapko Rahul Sharma ka call aa raha hai.\u201D — announced in your language, in a voice you choose."
        )
        IntroCard(
            title = "Spam shield",
            body = "Community reports flag likely fraud numbers before you pick up."
        )
        IntroCard(
            title = "Private by design",
            body = "Your profile and settings are stored encrypted on this device only."
        )
    }
}

@Composable
private fun IntroCard(title: String, body: String) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(BlueSoft)
            .padding(16.dp)
    ) {
        Text(title, fontWeight = FontWeight.Bold, fontSize = 15.sp)
        Text(body, fontSize = 13.sp, color = TextMuted, modifier = Modifier.padding(top = 4.dp))
        Spacer(Modifier.height(10.dp))
    }
}

@Composable
internal fun NameStep(vm: ProfileViewModel) {
    Column(modifier = Modifier.fillMaxWidth()) {
        StepTitle(
            "What should we call you?",
            "Only your first name is used for announcements."
        )
        OutlinedTextField(
            value = vm.name,
            onValueChange = { vm.name = it },
            label = { Text("Your name") },
            singleLine = true,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 14.dp)
        )
        if (vm.name.isNotBlank()) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 18.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(MaterialTheme.colorScheme.surface)
                    .padding(14.dp)
            ) {
                Avatar(vm.name, size = 40.dp)
                Spacer(Modifier.width(12.dp))
                Column {
                    Text(
                        Announcer.addressed(vm.name, vm.honorific),
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    )
                    Text("You will hear this at ring time.", fontSize = 12.sp, color = TextMuted)
                }
            }
        }
    }
}

@Composable
internal fun GenderStep(vm: ProfileViewModel) {
    Column(modifier = Modifier.fillMaxWidth()) {
        StepTitle(
            "Are you…",
            "Sets a default way of address — you can change it on the next step."
        )
        Spacer(Modifier.height(12.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Gender.entries.forEach { g ->
                val selected = vm.gender == g
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(20.dp))
                        .background(if (selected) BlueSoft else MaterialTheme.colorScheme.surface)
                        .border(
                            width = if (selected) 2.dp else 1.dp,
                            color = if (selected) Blue else MaterialTheme.colorScheme.outline,
                            shape = RoundedCornerShape(20.dp)
                        )
                        .clickable(role = Role.RadioButton, onClickLabel = g.label) { vm.chooseGender(g) }
                        .padding(vertical = 22.dp)
                ) {
                    Icon(
                        if (g == Gender.MALE) Icons.Filled.Man else Icons.Filled.Woman,
                        contentDescription = null,
                        tint = if (selected) Blue else TextMuted,
                        modifier = Modifier.size(30.dp)
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        g.label,
                        fontWeight = FontWeight.Bold,
                        color = if (selected) Blue else MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }
    }
}

@Composable
internal fun HonorificStep(vm: ProfileViewModel) {
    Column(modifier = Modifier.fillMaxWidth()) {
        StepTitle(
            "How should the announcer address you?",
            "Sir / Ma'am / Ji / None — independent of the voice you will pick."
        )
        Spacer(Modifier.height(8.dp))
        Column(modifier = Modifier.selectableGroup()) {
            Honorific.entries.forEach { h ->
                val selected = vm.honorific == h
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(if (selected) BlueSoft else Color.Transparent)
                        .selectable(
                            selected = selected,
                            role = Role.RadioButton,
                            onClick = { vm.chooseHonorific(h) }
                        )
                        .padding(horizontal = 6.dp, vertical = 10.dp)
                ) {
                    RadioButton(selected = selected, onClick = null)
                    Spacer(Modifier.width(10.dp))
                    Text(h.label, fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
                    Spacer(Modifier.weight(1f))
                    Text(
                        Announcer.addressed(vm.name.trim().ifBlank { "Umesh" }, h),
                        fontSize = 13.sp,
                        color = if (selected) Blue else TextMuted,
                        fontFamily = FontFamily.Default
                    )
                }
            }
        }
        SampleCard(
            sample = Announcer.personalGreeting(vm.name.trim(), vm.honorific, vm.lang),
            onPlay = { vm.previewPersonal() }
        )
    }
}

@Composable
internal fun LanguageStep(vm: ProfileViewModel) {
    Column(modifier = Modifier.fillMaxWidth()) {
        StepTitle("Announcement language", "Also pre-selects the announcer voice language.")
        Spacer(Modifier.height(8.dp))
        Column(modifier = Modifier.selectableGroup()) {
            AnnounceLang.entries.forEach { l ->
                val selected = vm.lang == l
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(if (selected) BlueSoft else MaterialTheme.colorScheme.surface)
                        .selectable(
                            selected = selected,
                            role = Role.RadioButton,
                            onClick = { vm.setLanguage(l) }
                        )
                        .padding(16.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(l.label, fontWeight = FontWeight.Bold, fontSize = 16.sp, color = if (selected) Blue else MaterialTheme.colorScheme.onSurface)
                        Spacer(Modifier.weight(1f))
                        Text(l.tag, fontSize = 11.sp, color = TextMuted, fontFamily = FontFamily.Monospace)
                    }
                    Text(
                        l.tagline,
                        fontSize = 13.sp,
                        color = TextMuted,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
                Spacer(Modifier.height(10.dp))
            }
        }
    }
}

@Composable
internal fun VoiceStep(
    vm: ProfileViewModel,
    playing: Boolean,
    onPlayToggle: () -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        StepTitle(
            "Pick the announcer voice",
            "A different voice than your honorific — listen to previews and choose."
        )
        Spacer(Modifier.height(12.dp))

        if (!vm.engineReady) {
            NoteCard(
                "System text-to-speech engine is not ready yet. \u201CSystem default voice\u201D will be used for now — you can change it anytime from Premium → Personal profile."
            )
        } else if (vm.voices.isEmpty()) {
            NoteCard(
                "No Hindi/English voices found on this device. \u201CSystem default voice\u201D will be used — install Google Text-to-Speech for more options."
            )
        } else {
            Column(modifier = Modifier.selectableGroup()) {
                SystemVoiceRow(
                    selected = vm.voiceName.isBlank(),
                    onClick = { vm.selectVoice("") }
                )
                Spacer(Modifier.height(8.dp))
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(300.dp)
                        .clip(RoundedCornerShape(18.dp))
                        .background(MaterialTheme.colorScheme.surface),
                    userScrollEnabled = true
                ) {
                    items(vm.voices, key = { it.name }) { v ->
                        VoiceRow(
                            voice = v,
                            selected = vm.voiceName == v.name,
                            onSelect = { vm.selectVoice(v.name) },
                            onPreview = { vm.previewVoice(v) }
                        )
                    }
                }
            }
        }

        Spacer(Modifier.height(14.dp))
        PlayBar(
            label = Announcer.personalGreeting(vm.name.trim(), vm.honorific, vm.lang),
            playing = playing,
            onToggle = {
                if (playing) vm.stopPreview() else vm.previewPersonal()
                onPlayToggle()
            },
            hint = "Preview"
        )
    }
}

@Composable
private fun SystemVoiceRow(selected: Boolean, onClick: () -> Unit) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(if (selected) BlueSoft else MaterialTheme.colorScheme.surface)
            .clickable(role = Role.RadioButton, onClick = onClick)
            .padding(14.dp)
    ) {
        Icon(
            Icons.Filled.Person,
            contentDescription = null,
            tint = if (selected) Blue else TextMuted,
            modifier = Modifier.size(22.dp)
        )
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text("System default voice", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
            Text("Follows the selected language", fontSize = 11.sp, color = TextMuted)
        }
        RadioButton(selected = selected, onClick = null)
    }
}

@Composable
private fun VoiceRow(
    voice: AvailableVoice,
    selected: Boolean,
    onSelect: () -> Unit,
    onPreview: () -> Unit
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .clickable(role = Role.RadioButton, onClickLabel = voice.label, onClick = onSelect)
            .padding(horizontal = 6.dp, vertical = 8.dp)
            .semantics { role = Role.RadioButton }
    ) {
        IconButton(onClick = onPreview, modifier = Modifier.semantics { contentDescription = "Preview ${voice.label}" }) {
            Icon(Icons.Filled.PlayArrow, contentDescription = null, tint = Blue)
        }
        Box(Modifier.width(4.dp))
        Column(Modifier.weight(1f)) {
            Text(voice.label, fontWeight = FontWeight.SemiBold, fontSize = 14.sp, maxLines = 1)
            Text(voice.languageTag, fontSize = 11.sp, color = TextMuted, fontFamily = FontFamily.Monospace)
        }
        Box(Modifier.width(6.dp))
        RadioButton(selected = selected, onClick = null)
    }
    androidx.compose.material3.HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.4f))
}

@Composable
private fun DoneStep(vm: ProfileViewModel, playing: Boolean, onPlayToggle: () -> Unit) {
    Column(modifier = Modifier.fillMaxWidth()) {
        StepTitle("All set!", "One last check — hear your announcement.")
        Spacer(Modifier.height(6.dp))

        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(22.dp))
                .background(Brush.horizontalGradient(listOf(Blue, BlueDark)))
                .padding(20.dp)
        ) {
            Text(Announcer.addressed(vm.name.trim().ifBlank { "CraftCaller" }, vm.honorific), color = Color.White, fontWeight = FontWeight.ExtraBold, fontSize = 22.sp)
            Spacer(Modifier.weight(1f))
            IconButton(
                onClick = {
                    if (playing) vm.stopPreview() else vm.previewPersonal()
                    onPlayToggle()
                },
                modifier = Modifier
                    .size(52.dp)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.18f))
                    .semantics { contentDescription = if (playing) "Stop preview" else "Play announcement preview" }
            ) {
                Icon(
                    if (playing) Icons.Filled.Pause else Icons.Filled.RecordVoiceOver,
                    contentDescription = null,
                    tint = Color.White
                )
            }
        }

        Spacer(Modifier.height(14.dp))
        PlayBar(
            label = Announcer.personalGreeting(vm.name.trim(), vm.honorific, vm.lang),
            playing = playing,
            onToggle = {
                if (playing) vm.stopPreview() else vm.previewPersonal()
                onPlayToggle()
            },
            hint = "Your announcement"
        )

        Spacer(Modifier.height(14.dp))
        AnnounceToggleRow(vm)

        Spacer(Modifier.height(14.dp))
        Text("Make it real", fontSize = 13.sp, color = Blue, fontWeight = FontWeight.Bold)
        Text(
            "CraftCaller needs system permissions for ring-time announcements. You approve each one with an explanation — nothing is assumed.",
            fontSize = 12.sp,
            color = TextMuted,
            modifier = Modifier.padding(top = 4.dp, bottom = 8.dp)
        )
        CallSetupCard()
    }
}

@Composable
internal fun AnnounceToggleRow(vm: ProfileViewModel) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surface)
            .padding(14.dp)
    ) {
        Column(Modifier.weight(1f)) {
            Text("Announce caller on incoming calls", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
            Text("TTS announcements during ringing — can be changed later.", fontSize = 11.sp, color = TextMuted)
        }
        Switch(
            checked = vm.announceEnabled,
            onCheckedChange = { vm.announceEnabled = it },
            modifier = Modifier.semantics { contentDescription = "Announce caller on incoming calls" }
        )
    }
}

// ------------------------------------------------------------------ helpers

@Composable
private fun StepTitle(title: String, subtitle: String) {
    Column {
        Text(title, fontSize = 22.sp, fontWeight = FontWeight.ExtraBold)
        Text(subtitle, fontSize = 13.sp, color = TextMuted, modifier = Modifier.padding(top = 4.dp))
    }
}

@Composable
private fun SampleCard(sample: String, onPlay: () -> Unit) {
    PlayBar(
        label = sample,
        playing = false,
        onToggle = onPlay,
        hint = "Sample"
    )
}

@Composable
private fun PlayBar(label: String, playing: Boolean, onToggle: () -> Unit, hint: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surface)
            .padding(12.dp)
    ) {
        Text(hint, fontSize = 10.sp, color = TextMuted, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
        Spacer(Modifier.width(8.dp))
        IconButton(
            onClick = onToggle,
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(Blue)
                .semantics { contentDescription = if (playing) "Stop" else "Play sample" }
        ) {
            Icon(
                if (playing) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                contentDescription = null,
                tint = Color.White
            )
        }
        Spacer(Modifier.width(8.dp))
        Text(label, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
    }
}

@Composable
private fun NoteCard(text: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(AmberWarn.copy(alpha = 0.12f))
            .padding(14.dp)
    ) {
        Text(text, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurface)
    }
}
