package com.craftcaller.app.settings

import android.app.Application
import android.content.Intent
import android.content.pm.PackageManager
import android.speech.tts.TextToSpeech
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
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
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.craftcaller.app.tts.VoiceKit
import com.craftcaller.app.ui.common.EmptyState
import com.craftcaller.app.ui.common.rememberVm
import com.craftcaller.app.ui.theme.Blue
import com.craftcaller.app.ui.theme.GreenOk
import com.craftcaller.app.ui.theme.TextMuted
import com.craftcaller.app.util.Announcer
import com.craftcaller.app.util.AvailableVoice
import com.craftcaller.app.util.Prefs
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/** A TTS engine discovered through the system TTS service intent. */
data class TtsEngineOption(val packageName: String, val label: String)

/** First option list entry: empty package = Android system default engine. */
private fun systemDefaultOption() = TtsEngineOption("", "System default (recommended)")

/**
 * Voice-engine picker + voice picker with a live test kit.
 *
 * Real behaviors only:
 *  - the engine list is DISCOVERED from the system (nothing hard-coded),
 *  - choosing an engine persists `Prefs.ttsEngineName`, which every later
 *    TTS creation point (ring-time AnnouncerVoice, profile previews) uses,
 *  - the kit for the chosen engine lists its Hindi/English voices and speaks
 *    the actual personal greeting; a failing engine falls back to the system
 *    default with a visible status message instead of silent breakage.
 */
class VoiceEngineViewModel(app: Application) : AndroidViewModel(app) {

    private val prefs = Prefs(app)
    private var kit: VoiceKit? = null
    private val kitJobs = mutableListOf<Job>()

    var engines by mutableStateOf(listOf(systemDefaultOption()))
        private set
    var scanning by mutableStateOf(true)
        private set
    var selectedEngine by mutableStateOf(prefs.ttsEngineName)
        private set
    var engineReady by mutableStateOf(false)
        private set
    var engineStatus by mutableStateOf("Starting the selected engine…")
        private set
    var voices by mutableStateOf<List<AvailableVoice>>(emptyList())
        private set
    var voiceName by mutableStateOf(prefs.announcerVoiceName)
        private set
    var previewPlaying by mutableStateOf(false)
        private set

    init {
        scan()
    }

    private fun scan() {
        scanning = true
        val app = getApplication<Application>()
        viewModelScope.launch {
            val found = withContext(Dispatchers.IO) { discoveredEngines(app) }
            engines = listOf(systemDefaultOption()) + found
            scanning = false
            rebuildKit(app)
        }
    }

    /** Select + persist an engine immediately; the kit rebuild tests it live. */
    fun chooseEngine(packageName: String) {
        if (packageName == selectedEngine) return
        selectedEngine = packageName
        prefs.ttsEngineName = packageName
        voices = emptyList()
        engineReady = false
        engineStatus = "Testing engine…"
        rebuildKit(getApplication())
    }

    fun selectVoice(name: String) {
        voiceName = name
        prefs.announcerVoiceName = name
        voices.firstOrNull { it.name == name }?.let {
            prefs.announcerVoiceLang = it.languageTag
        }
    }

    fun togglePreview() {
        val current = kit ?: return
        if (previewPlaying) {
            current.stop()
            previewPlaying = false
        } else {
            previewPlaying = true
            current.speak(
                Announcer.personalGreeting(
                    prefs.userName.ifBlank { "Umesh" }, prefs.honorific, prefs.announceLang
                ),
                voiceName,
                prefs.announcerVoiceLang,
                rate = prefs.announceRate,
                pitch = prefs.announcePitch
            )
        }
    }

    /** Row-level preview: hear THIS voice without changing the selection. */
    fun previewVoice(voice: AvailableVoice) {
        val current = kit ?: return
        previewPlaying = true
        current.speak(
            Announcer.personalGreeting(
                prefs.userName.ifBlank { "Umesh" }, prefs.honorific, prefs.announceLang
            ),
            voice.name,
            voice.languageTag,
            rate = prefs.announceRate,
            pitch = prefs.announcePitch
        )
    }

    fun stopPreview() {
        if (previewPlaying) {
            kit?.stop()
            previewPlaying = false
        }
    }

    override fun onCleared() {
        kitJobs.forEach { it.cancel() }
        kitJobs.clear()
        kit?.shutdown()
        kit = null
    }

    // ------------------------------------------------------------ internals

    private fun rebuildKit(app: Application) {
        kitJobs.forEach { it.cancel() }
        kitJobs.clear()
        kit?.shutdown()
        val holder = VoiceKit(app, selectedEngine.ifBlank { null })
        kit = holder
        kitJobs += viewModelScope.launch {
            holder.ready.collect { ok ->
                engineReady = ok
                if (ok) {
                    engineStatus = "Engine ready — ${holder.voices.value.size} Hindi/English voices"
                } else if (holder.initError.value == null) {
                    engineStatus = "Starting the selected engine…"
                }
            }
        }
        kitJobs += viewModelScope.launch {
            holder.initError.collect { err ->
                if (!err.isNullOrEmpty()) {
                    engineReady = false
                    engineStatus = err
                }
            }
        }
        kitJobs += viewModelScope.launch {
            holder.voices.collect { list ->
                voices = list
                if (engineReady) {
                    engineStatus = "Engine ready — ${list.size} Hindi/English voices"
                }
                if (engineReady && voiceName.isNotBlank() && list.none { it.name == voiceName }) {
                    // Chosen voice does not exist in this engine: fall back to
                    // the locale default and surface it, never silently break.
                    voiceName = ""
                    prefs.announcerVoiceName = ""
                    engineStatus = "Saved voice is not in this engine — using its language default"
                }
            }
        }
    }
}

/** System query: every package advertising a TTS service becomes an option. */
private fun discoveredEngines(app: Application): List<TtsEngineOption> = runCatching {
    val pm = app.packageManager
    val intent = Intent(TextToSpeech.Engine.INTENT_ACTION_TTS_SERVICE)
        .addCategory(Intent.CATEGORY_DEFAULT)
    pm.queryIntentActivities(intent, 0).mapNotNull { resolve ->
        val pkg = resolve?.serviceInfo?.packageName ?: return@mapNotNull null
        val label = runCatching {
            pm.getApplicationLabel(pm.getApplicationInfo(pkg, 0)).toString()
        }.getOrDefault(pkg)
        TtsEngineOption(pkg, label)
    }.distinctBy { it.packageName }.sortedBy { it.label }
}.getOrDefault(emptyList())

// ============================================================ UI ============

@Composable
fun VoiceEngineScreen(onClose: () -> Unit) {
    val context = LocalContext.current
    val vm: VoiceEngineViewModel = rememberVm {
        VoiceEngineViewModel(context.applicationContext as Application)
    }

    DisposableEffect(Unit) {
        onDispose { vm.stopPreview() }
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
                onClick = {
                    vm.stopPreview()
                    onClose()
                },
                modifier = Modifier.semantics { contentDescription = "Back to settings" }
            ) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null)
            }
            Column {
                Text("Voice engine", fontWeight = FontWeight.ExtraBold, fontSize = 17.sp)
                Text("Which synthesizer speaks your announcements", fontSize = 11.sp, color = TextMuted)
            }
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(bottom = 28.dp)
        ) {
            // ---- live engine status card (real states) --------------------
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                        .clip(RoundedCornerShape(18.dp))
                        .background(
                            if (engineFailed(vm)) TextMuted.copy(alpha = 0.08f)
                            else if (vm.engineReady) GreenOk.copy(alpha = 0.10f)
                            else Blue.copy(alpha = 0.08f)
                        )
                        .padding(16.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (vm.engineReady) {
                            Icon(Icons.Filled.CheckCircle, null, tint = GreenOk)
                        } else if (vm.engineStatus.startsWith("Selected engine failed") ||
                            vm.engineStatus.startsWith("Saved voice is not")
                        ) {
                            Text("!", color = Color(0xFFF59E0B), fontWeight = FontWeight.ExtraBold)
                        } else {
                            CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                        }
                        Spacer(Modifier.width(10.dp))
                        Column(Modifier.weight(1f)) {
                            Text(
                                "Now testing: " + engineDisplayName(vm),
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 14.sp
                            )
                            Text(vm.engineStatus, fontSize = 12.sp, color = TextMuted, maxLines = 2)
                        }
                    }
                    if (vm.engineStatus.startsWith("Saved voice is not")) {
                        Text(
                            "Pick a voice below (or the language default is used).",
                            fontSize = 11.sp,
                            color = TextMuted,
                            modifier = Modifier.padding(top = 6.dp)
                        )
                    }
                }
            }

            // ---- engine list ----------------------------------------------
            item {
                SectionLabel(
                    if (vm.scanning) "Installed engines · searching…" else "Installed engines"
                )
            }
            if (vm.scanning) {
                item {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 12.dp)
                    ) {
                        CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                        Spacer(Modifier.width(10.dp))
                        Text("Searching for TTS engines on this device…", fontSize = 13.sp, color = TextMuted)
                    }
                }
            } else {
                items(vm.engines, key = { it.packageName.ifBlank { "system" } }) { engine ->
                    EngineRow(
                        label = engine.label,
                        selected = vm.selectedEngine == engine.packageName,
                        onClick = { vm.chooseEngine(engine.packageName) }
                    )
                }
            }

            // ---- voices of the tested engine ------------------------------
            item { SectionLabel("Voices in this engine") }
            when {
                vm.engineReady && vm.voices.isEmpty() -> item {
                    Box(Modifier.padding(horizontal = 16.dp)) {
                        EmptyState(
                            "No Hindi/English voices installed in this engine — " +
                                "previews will use its language default (machine voice)."
                        )
                    }
                }
                !vm.engineReady && !vm.scanning -> item {
                    Box(Modifier.padding(horizontal = 16.dp)) {
                        EmptyState(
                            "Voice list appears once the engine starts. A failed engine " +
                                "falls back to the system default automatically."
                        )
                    }
                }
                else -> items(vm.voices, key = { it.name }) { voice ->
                    VoiceRow(
                        voice = voice,
                        selected = vm.voiceName == voice.name,
                        onSelect = { vm.selectVoice(voice.name) },
                        onPreview = {
                            if (vm.previewPlaying) vm.togglePreview() else vm.previewVoice(voice)
                        },
                        previewActive = vm.previewPlaying
                    )
                }
            }

            // ---- preview + honesty note ------------------------------------
            item {
                Spacer(Modifier.padding(top = 12.dp))
                Row(modifier = Modifier.padding(horizontal = 16.dp)) {
                    Button(
                        onClick = { vm.togglePreview() },
                        enabled = vm.engineReady,
                        colors = ButtonDefaults.buttonColors(containerColor = Blue),
                        modifier = Modifier
                            .fillMaxWidth()
                            .semantics { contentDescription = "Preview announcement with this engine" },
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Icon(
                            if (vm.previewPlaying) Icons.Filled.Stop else Icons.Filled.PlayArrow,
                            contentDescription = null, tint = Color.White
                        )
                        Spacer(Modifier.width(6.dp))
                        Text(
                            if (vm.previewPlaying) "Stop preview" else "Preview announcement",
                            color = Color.White, fontWeight = FontWeight.SemiBold
                        )
                    }
                }
                Text(
                    "This engine becomes the one used for real call announcements " +
                        "(and profile previews). Failed engines automatically fall back to " +
                        "the system default — settings persist across restarts.",
                    fontSize = 11.sp,
                    color = TextMuted,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                )
            }
        }
    }
}

private fun engineFailed(vm: VoiceEngineViewModel): Boolean =
    vm.engineStatus.startsWith("Selected engine failed")

private fun engineDisplayName(vm: VoiceEngineViewModel): String {
    val option = vm.engines.firstOrNull { it.packageName == vm.selectedEngine }
    return option?.label ?: if (vm.selectedEngine.isBlank()) "System default" else vm.selectedEngine
}

@Composable
private fun SectionLabel(text: String) {
    Text(
        text.uppercase(),
        fontSize = 11.sp,
        fontWeight = FontWeight.Bold,
        color = Blue,
        letterSpacing = 1.sp,
        modifier = Modifier.padding(horizontal = 20.dp, vertical = 10.dp)
    )
}

@Composable
private fun EngineRow(label: String, selected: Boolean, onClick: () -> Unit) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(MaterialTheme.colorScheme.surface)
            .clickable(onClickLabel = label) { onClick() }
            .padding(horizontal = 14.dp, vertical = 12.dp)
            .then(if (selected) Modifier.border(2.dp, Blue, RoundedCornerShape(14.dp)) else Modifier)
    ) {
        Column(Modifier.weight(1f)) {
            Text(label, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
            Text("TTS engine", fontSize = 11.sp, color = TextMuted)
        }
        if (selected) {
            Icon(Icons.Filled.CheckCircle, contentDescription = "Selected", tint = GreenOk)
        }
    }
}

@Composable
private fun VoiceRow(
    voice: AvailableVoice,
    selected: Boolean,
    onSelect: () -> Unit,
    onPreview: () -> Unit,
    previewActive: Boolean
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 2.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(
                if (selected) Blue.copy(alpha = 0.10f) else MaterialTheme.colorScheme.surface
            )
            .clickable(onClickLabel = voice.label) { onSelect() }
            .padding(horizontal = 14.dp, vertical = 10.dp)
    ) {
        Column(Modifier.weight(1f)) {
            Text(voice.label, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
            Text(voice.languageTag, fontSize = 11.sp, color = TextMuted)
        }
        IconButton(
            onClick = onPreview,
            modifier = Modifier.semantics {
                contentDescription =
                    if (previewActive) "Stop voice preview" else "Preview ${voice.label}"
            }
        ) {
            Icon(
                if (previewActive) Icons.Filled.Stop else Icons.Filled.PlayArrow,
                contentDescription = null,
                tint = Blue
            )
        }
        if (selected) {
            Icon(Icons.Filled.CheckCircle, contentDescription = "Voice selected", tint = GreenOk)
        }
    }
}
