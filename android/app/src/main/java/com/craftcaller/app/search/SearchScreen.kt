package com.craftcaller.app.search

import android.app.Application
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Report
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.craftcaller.app.caller.CallerIdentity
import com.craftcaller.app.caller.CallerLabel
import com.craftcaller.app.caller.CallerResolver
import com.craftcaller.app.caller.CallerLabelBadges
import com.craftcaller.app.caller.labelColor
import com.craftcaller.app.data.local.DatabaseProvider
import com.craftcaller.app.data.local.entity.AllowedNumber
import com.craftcaller.app.data.local.entity.BlockedNumber
import com.craftcaller.app.data.local.entity.CallerInfo
import com.craftcaller.app.data.remote.FirebaseRepository
import com.craftcaller.app.ui.common.Avatar
import com.craftcaller.app.ui.common.TagChip
import com.craftcaller.app.ui.common.rememberVm
import com.craftcaller.app.ui.theme.Blue
import com.craftcaller.app.ui.theme.GreenOk
import com.craftcaller.app.ui.theme.RedSpam
import com.craftcaller.app.ui.theme.TextMuted
import com.craftcaller.app.util.Craft
import com.craftcaller.app.util.Prefs
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class SearchViewModel(app: Application) : AndroidViewModel(app) {
    private val db = DatabaseProvider.get(app)
    private val prefs = Prefs(app)
    private val q = MutableStateFlow("")

    var query: String
        get() = q.value
        set(value) { q.value = value }

    var localResults by mutableStateOf<List<CallerInfo>>(emptyList())
    var identity by mutableStateOf<CallerIdentity?>(null)
    var searching by mutableStateOf(false)
    var notice by mutableStateOf<String?>(null)

    init {
        viewModelScope.launch {
            q.debounce(200).collectLatest { queryText ->
                val trimmed = queryText.trim()
                if (trimmed.length < 2) {
                    withContext(Dispatchers.IO) { localResults = emptyList() }
                    return@collectLatest
                }
                withContext(Dispatchers.IO) {
                    db.callerDao().search(Craft.normalizeNumber(trimmed))
                        .ifEmpty { db.callerDao().search(trimmed) }
                        .also { localResults = it }
                }
            }
        }
    }

    fun block(number: String) {
        viewModelScope.launch(Dispatchers.IO) {
            db.blockedDao().insert(BlockedNumber(number = number, reason = "Search"))
        }
    }

    fun allow(number: String) {
        viewModelScope.launch(Dispatchers.IO) {
            db.allowedDao().insert(AllowedNumber(number = number, label = identity?.display))
        }
    }

    /**
     * Spam report: local cache bump always; community counter only after the
     * per-action consent dialog confirms (callback returns true).
     */
    fun reportConfirmed(number: String, consented: Boolean) {
        viewModelScope.launch(Dispatchers.IO) {
            val known = db.callerDao().get(number)
            db.callerDao().upsert(
                CallerInfo(
                    number = number,
                    name = known?.name,
                    location = known?.location,
                    spamReports = (known?.spamReports ?: 0) + 1,
                    updatedAt = System.currentTimeMillis(),
                    source = known?.source ?: "user-report"
                )
            )
            if (consented) {
                runCatching { FirebaseRepository.reportSpam(number) }
            }
        }
    }

    /** Name correction ("this number is actually…"), consent-gated upload. */
    fun submitCorrection(number: String, correctedName: String, consented: Boolean) {
        viewModelScope.launch(Dispatchers.IO) {
            val known = db.callerDao().get(number)
            db.callerDao().upsert(
                CallerInfo(
                    number = number,
                    name = correctedName.trim(),
                    location = known?.location,
                    spamReports = known?.spamReports ?: 0,
                    updatedAt = System.currentTimeMillis(),
                    source = "local"
                )
            )
            if (consented && !FirebaseRepository.placeholderConfig) {
                runCatching { FirebaseRepository.submitCorrection(number, correctedName) }
            }
        }
    }

    /** Full resolve: local ladder first, consent-gated network enrichment. */
    fun resolveOnline() {
        val effective = Craft.normalizeNumber(query)
        if (effective.length < 8 || searching) return
        searching = true
        notice = null
        viewModelScope.launch(Dispatchers.IO) {
            if (!prefs.communityReadEnabled) {
                val local = CallerResolver.resolveLocal(getApplication(), effective)
                identity = local
                notice = "Community lookups are off — showing saved contacts & cache only."
                searching = false
                return@launch
            }
            identity = CallerResolver.resolveFull(getApplication(), effective)
            searching = false
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchScreen(onClose: () -> Unit) {
    val context = LocalContext.current
    val vm: SearchViewModel = rememberVm { SearchViewModel(context.applicationContext as Application) }
    var confirmReportFor by remember { mutableStateOf<String?>(null) }
    var correctionFor by remember { mutableStateOf<String?>(null) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(16.dp)
    ) {
        OutlinedTextField(
            value = vm.query,
            onValueChange = { vm.query = it },
            placeholder = { Text("Search number or name…", fontSize = 15.sp) },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
            singleLine = true,
            leadingIcon = { Icon(Icons.Filled.Search, null) },
            trailingIcon = {
                IconButton(onClick = onClose) { Icon(Icons.Filled.Close, null) }
            },
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(Modifier.height(12.dp))
        if (Craft.normalizeNumber(vm.query).length >= 8) {
            Button(
                onClick = { vm.resolveOnline() },
                enabled = !vm.searching,
                colors = ButtonDefaults.buttonColors(containerColor = Blue),
                modifier = Modifier.fillMaxWidth()
            ) {
                if (vm.searching) CircularProgressIndicator(color = Color.White, strokeWidth = 2.dp)
                else Text("Identify number", fontWeight = FontWeight.SemiBold)
            }
        }

        vm.notice?.let {
            Spacer(Modifier.height(8.dp))
            Text(it, fontSize = 12.sp, color = TextMuted)
        }

        vm.identity?.let { ident ->
            Spacer(Modifier.height(14.dp))
            IdentityResultCard(
                identity = ident,
                onCall = {
                    context.startActivity(
                        Intent(Intent.ACTION_DIAL, Uri.parse("tel:${ident.number}"))
                    )
                },
                onBlock = { vm.block(ident.number) },
                onAllow = { vm.allow(ident.number) },
                onReport = { confirmReportFor = ident.number },
                onCorrect = { correctionFor = ident.number }
            )
        }

        if (vm.localResults.isNotEmpty()) {
            Text(
                "History & cached people",
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = TextMuted,
                modifier = Modifier.padding(top = 14.dp, bottom = 4.dp)
            )
            LazyColumn(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                items(vm.localResults, key = { it.number }) { info ->
                    val display = info.name ?: "Unknown"
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                context.startActivity(
                                    Intent(Intent.ACTION_DIAL, Uri.parse("tel:${info.number}"))
                                )
                            }
                            .padding(vertical = 8.dp)
                    ) {
                        Avatar(display)
                        Spacer(Modifier.width(12.dp))
                        Column(Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    display,
                                    fontWeight = FontWeight.SemiBold,
                                    color = if (info.isSpam) RedSpam else MaterialTheme.colorScheme.onSurface
                                )
                                if (info.isSpam || info.name != null) {
                                    Spacer(Modifier.width(8.dp))
                                    TagChip(
                                        when {
                                            info.isSpam -> "Spam — Likely Fraud"
                                            info.source == "local" || info.source == "user-report" -> "My names"
                                            else -> "Community"
                                        },
                                        if (info.isSpam) RedSpam else TextMuted
                                    )
                                }
                            }
                            Text(Craft.prettyNumber(info.number), fontSize = 12.sp, color = TextMuted)
                        }
                        IconButton(onClick = {
                            context.startActivity(
                                Intent(Intent.ACTION_DIAL, Uri.parse("tel:${info.number}"))
                            )
                        }) {
                            Icon(Icons.Filled.Call, null, tint = GreenOk)
                        }
                    }
                }
            }
        }

        Spacer(Modifier.weight(1f))
        Text(
            "Sources: saved contacts (on-device) → local cache → community submissions " +
                "(opt-in) → CraftCaller public directory (optional). No private third-party " +
                "caller data is used. Numbers with no trustworthy match stay \"Unknown\".",
            fontSize = 10.sp,
            color = TextMuted,
            modifier = Modifier.padding(bottom = 6.dp)
        )
    }

    // ------------------------------------------------------------ dialogs

    confirmReportFor?.let { number ->
        AlertDialog(
            onDismissRequest = { confirmReportFor = null },
            title = { Text("Report as spam?", fontWeight = FontWeight.Bold) },
            text = {
                Text(
                    "Adds one spam report for ${Craft.prettyNumber(number)}.\n\n" +
                        "Community part: the report counter is shared with CraftCaller users " +
                        "(number + report count only — no name, no personal data). " +
                        "This is opt-in and can be turned off in Settings.",
                    fontSize = 13.sp
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    vm.reportConfirmed(number, consented = true)
                    confirmReportFor = null
                }) { Text("Report", color = RedSpam) }
            },
            dismissButton = {
                TextButton(onClick = {
                    vm.reportConfirmed(number, consented = false) // local cache only
                    confirmReportFor = null
                }) { Text("Not now", color = TextMuted) }
            }
        )
    }

    correctionFor?.let { number ->
        var text by remember(correctionFor) {
            mutableStateOf(
                vm.identity?.takeIf { it.number == number }?.displayName.orEmpty()
            )
        }
        AlertDialog(
            onDismissRequest = { correctionFor = null },
            title = { Text("Report correction", fontWeight = FontWeight.Bold) },
            text = {
                Column {
                    Text(
                        "What is the correct name for ${Craft.prettyNumber(number)}?",
                        fontSize = 13.sp
                    )
                    OutlinedTextField(
                        value = text,
                        onValueChange = { text = it },
                        singleLine = true,
                        placeholder = { Text("Correct name…") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 10.dp)
                    )
                    Text(
                        "Saved locally. Uploading to the community directory is opt-in " +
                            "(turn it on in Settings) — never done silently.",
                        fontSize = 10.sp,
                        color = TextMuted,
                        modifier = Modifier.padding(top = 10.dp)
                    )
                }
            },
            confirmButton = {
                TextButton(
                    enabled = text.trim().isNotBlank(),
                    onClick = {
                        vm.submitCorrection(number, text, consented = false)
                        correctionFor = null
                    }
                ) { Text("Save locally") }
            },
            dismissButton = {
                TextButton(onClick = { correctionFor = null }) {
                    Text("Cancel", color = TextMuted)
                }
            }
        )
    }
}

@Composable
private fun IdentityResultCard(
    identity: CallerIdentity,
    onCall: () -> Unit,
    onBlock: () -> Unit,
    onAllow: () -> Unit,
    onReport: () -> Unit,
    onCorrect: () -> Unit
) {
    val tint = labelColor(identity.label)
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(
                if (identity.label == CallerLabel.SUSPECTED_SPAM) RedSpam.copy(alpha = 0.1f)
                else MaterialTheme.colorScheme.surface
            )
            .padding(18.dp)
    ) {
        Column {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Avatar(identity.display, size = 56.dp)
                Spacer(Modifier.width(14.dp))
                Column(Modifier.weight(1f)) {
                    Text(
                        identity.display,
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 20.sp,
                        color = if (identity.label == CallerLabel.SUSPECTED_SPAM) RedSpam
                        else MaterialTheme.colorScheme.onSurface
                    )
                    Text(Craft.prettyNumber(identity.number), color = TextMuted, fontSize = 13.sp)
                    (identity.location ?: Craft.guessOperator(identity.number))?.let {
                        Text(it, color = TextMuted, fontSize = 12.sp)
                    }
                    Spacer(Modifier.height(6.dp))
                    CallerLabelBadges(identity)
                    if (identity.spamReports > 0) {
                        Text(
                            "${identity.spamReports} community report(s)",
                            color = TextMuted,
                            fontSize = 10.sp,
                            modifier = Modifier.padding(top = 4.dp)
                        )
                    }
                }
                IconButton(onClick = onCall) {
                    Icon(Icons.Filled.Call, null, tint = GreenOk)
                }
            }
            Spacer(Modifier.height(14.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(
                    onClick = onCall,
                    colors = ButtonDefaults.buttonColors(containerColor = GreenOk),
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Filled.Call, null, tint = Color.White)
                    Spacer(Modifier.width(6.dp))
                    Text("Call", color = Color.White)
                }
                Button(
                    onClick = onBlock,
                    colors = ButtonDefaults.buttonColors(containerColor = RedSpam),
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Filled.Block, null, tint = Color.White)
                    Spacer(Modifier.width(6.dp))
                    Text("Block", color = Color.White)
                }
                IconButton(onClick = onAllow) {
                    Icon(Icons.Filled.CheckCircle, contentDescription = "Allow this number", tint = GreenOk)
                }
                IconButton(onClick = onCorrect) {
                    Icon(Icons.Filled.Edit, contentDescription = "Report name correction", tint = Blue)
                }
                IconButton(onClick = onReport) {
                    Icon(Icons.Filled.Report, contentDescription = "Report spam", tint = Color(0xFFF59E0B))
                }
            }
            if (identity.label == CallerLabel.UNKNOWN) {
                Text(
                    "No trustworthy source matched — shown as Unknown (we never guess names).",
                    fontSize = 10.sp,
                    color = TextMuted,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }
        }
    }
}
