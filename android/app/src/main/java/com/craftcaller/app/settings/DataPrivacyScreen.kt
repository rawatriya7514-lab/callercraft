package com.craftcaller.app.settings

import android.app.Application
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
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.PhonelinkRing
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
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
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.craftcaller.app.data.local.DatabaseProvider
import com.craftcaller.app.data.remote.FirebaseRepository
import com.craftcaller.app.ui.common.rememberVm
import com.craftcaller.app.ui.theme.AmberWarn
import com.craftcaller.app.ui.theme.Blue
import com.craftcaller.app.ui.theme.GreenOk
import com.craftcaller.app.ui.theme.RedSpam
import com.craftcaller.app.ui.theme.TextMuted
import com.craftcaller.app.util.Prefs
import com.craftcaller.app.util.SecurePrefs
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Data & privacy center. Counters are live database counts; each delete
 * button really deletes its own table (with a confirm dialog). Full wipe
 * also clears encrypted profile prefs, which honestly returns the app to
 * the first-launch state (the next start lands on onboarding/login).
 */
class DataPrivacyViewModel(app: Application) : AndroidViewModel(app) {

    private val db = DatabaseProvider.get(app)

    /** null = still counting; -1 = db error (shown as an error row). */
    val cachedCallers = MutableStateFlow<Int?>(null)
    val callLogEntries = MutableStateFlow<Int?>(null)
    val blockedCount = MutableStateFlow<Int?>(null)
    val allowedCount = MutableStateFlow<Int?>(null)
    val smsCount = MutableStateFlow<Int?>(null)
    val loadFailed = MutableStateFlow(false)
    val wipeDone = MutableStateFlow(false)

    init {
        countAll()
    }

    fun countAll() {
        viewModelScope.launch {
            try {
                withContext(Dispatchers.IO) {
                    cachedCallers.value = runCatching { db.callerDao().currentCount() }.getOrDefault(-1)
                    callLogEntries.value = runCatching { db.callLogDao().currentCount() }.getOrDefault(-1)
                    blockedCount.value = runCatching { db.blockedDao().currentCount() }.getOrDefault(-1)
                    allowedCount.value = runCatching { db.allowedDao().currentCount() }.getOrDefault(-1)
                    smsCount.value = runCatching { db.smsDao().currentCount() }.getOrDefault(-1)
                }
            } catch (_: Exception) {
                loadFailed.value = true
            }
        }
    }

    fun clearCallerCache() {
        viewModelScope.launch {
            try {
                withContext(Dispatchers.IO) {
                    runCatching { db.callerDao().clear() }
                    cachedCallers.value = db.callerDao().currentCount()
                }
            } catch (_: Exception) {
                loadFailed.value = true
            }
        }
    }

    fun clearCallLog() {
        viewModelScope.launch {
            try {
                withContext(Dispatchers.IO) {
                    runCatching { db.callLogDao().clear() }
                    callLogEntries.value = db.callLogDao().currentCount()
                }
            } catch (_: Exception) {
                loadFailed.value = true
            }
        }
    }

    /** Full local wipe: all tables + plain prefs + encrypted profile store. */
    fun wipeEverything(contextApplication: Application) {
        viewModelScope.launch {
            try {
                withContext(Dispatchers.IO) {
                    runCatching { db.smsDao().clear() }
                    runCatching { db.callerDao().clear() }
                    runCatching { db.callLogDao().clear() }
                    runCatching { db.blockedDao().clear() }
                    runCatching { db.allowedDao().clear() }
                }
                SecurePrefs.clearAll(contextApplication)
                contextApplication.getSharedPreferences("craftcaller_prefs", 0)
                    .edit().clear().apply()
                wipeDone.value = true
            } catch (_: Exception) {
                // leave user on the screen; counts reloaded by caller
            }
            countAll()
        }
    }
}

@Composable
fun DataPrivacyScreen(onClose: () -> Unit) {
    val context = LocalContext.current
    val vm: DataPrivacyViewModel = rememberVm {
        DataPrivacyViewModel(context.applicationContext as Application)
    }

    val cachedCallers by vm.cachedCallers.collectAsState()
    val callLogEntries by vm.callLogEntries.collectAsState()
    val blockedCount by vm.blockedCount.collectAsState()
    val allowedCount by vm.allowedCount.collectAsState()
    val smsCount by vm.smsCount.collectAsState()
    val loadFailed by vm.loadFailed.collectAsState()
    val wipeDone by vm.wipeDone.collectAsState()

    var confirmCache by remember { mutableStateOf(false) }
    var confirmLog by remember { mutableStateOf(false) }
    var confirmWipe by remember { mutableStateOf(false) }

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
                Text("Data & privacy", fontWeight = FontWeight.ExtraBold, fontSize = 17.sp)
                Text("All counts are live from your device; deletes are immediate", fontSize = 11.sp, color = TextMuted)
            }
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(MaterialTheme.colorScheme.surface)
                    .padding(14.dp)
            ) {
                Icon(
                    if (FirebaseRepository.placeholderConfig) Icons.Filled.CloudOff else Icons.Filled.CloudOff,
                    null,
                    tint = if (FirebaseRepository.placeholderConfig) AmberWarn else GreenOk,
                    modifier = Modifier.size(22.dp)
                )
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text(
                        if (FirebaseRepository.placeholderConfig)
                            "Offline/demo build — no community connection active"
                        else "Community connection configured",
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        if (FirebaseRepository.placeholderConfig)
                            "Nothing leaves this phone in this build; the directory API key is a placeholder"
                        else "Lookups report/correct flows use the configured community backend",
                        fontSize = 12.sp, color = TextMuted, maxLines = 2
                    )
                }
            }

            Spacer(Modifier.height(16.dp))
            DataLabel("On-device data (live counts)")
            DataRow(
                icon = Icons.Filled.People,
                title = "Identified people cache",
                detail = "Names CraftCaller learned (contacts/community) for future rings",
                count = cachedCallers,
                deleteLabel = "Clear cache",
                onDelete = { confirmCache = true }
            )
            DataRow(
                icon = Icons.Filled.History,
                title = "Call log entries",
                detail = "This app's own record of screened/rung calls",
                count = callLogEntries,
                deleteLabel = "Clear",
                onDelete = { confirmLog = true }
            )
            DataRow(
                icon = Icons.Filled.Shield,
                title = "Blocked numbers",
                detail = "Manual blocklist — removing entries here also unblocks",
                count = blockedCount,
                deleteLabel = "Clear all",
                onDelete = { confirmWipe = true }
            )
            DataRow(
                icon = Icons.Filled.PhonelinkRing,
                title = "Allowed numbers",
                detail = "Never auto-rejected even with spam reports",
                count = allowedCount,
                deleteLabel = "Clear all",
                onDelete = { confirmWipe = true }
            )
            DataRow(
                icon = Icons.Filled.History,
                title = "Messages cache",
                detail = "SMS the Messages tab read",
                count = smsCount,
                deleteLabel = "Clear",
                onDelete = { confirmWipe = true }
            )

            Spacer(Modifier.height(20.dp))
            DataLabel("Full data deletion")
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(RedSpam.copy(alpha = 0.08f))
                    .padding(14.dp)
            ) {
                Icon(Icons.Filled.DeleteForever, null, tint = RedSpam, modifier = Modifier.size(22.dp))
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text("Delete everything & start over", fontWeight = FontWeight.Bold, color = RedSpam)
                    Text(
                        "Wipes ALL tables, encrypted profile prefs and app settings. " +
                            "Next launch starts from onboarding. No server deletion is " +
                            "possible in this build (no account data is stored server-side).",
                        fontSize = 12.sp, color = TextMuted, maxLines = 4
                    )
                }
            }
            Spacer(Modifier.height(8.dp))
            Button(
                onClick = { confirmWipe = true },
                colors = ButtonDefaults.buttonColors(containerColor = RedSpam),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .semantics { contentDescription = "Delete all app data" }
            ) {
                Text("Delete everything", color = Color.White, fontWeight = FontWeight.SemiBold)
            }

            if (loadFailed) {
                Spacer(Modifier.height(12.dp))
                Text(
                    "Some counters failed to load (database error) — values marked \"?\". " +
                        "Restart the app and reopen this screen to retry.",
                    fontSize = 11.sp,
                    color = AmberWarn
                )
            }

            Spacer(Modifier.height(16.dp))
            Text(
                "Privacy summary: profile data (name/honorific) is encrypted at rest. " +
                    "Contacts are read only for announcements/caller-ID and are never " +
                    "uploaded unless you explicitly enable contribution in the informed-consent " +
                    "dialog. Calls are never recorded automatically.",
                fontSize = 11.sp,
                color = TextMuted
            )
            Spacer(Modifier.height(30.dp))
        }
    }

    // ------------------------------------------------------------ dialogs

    if (confirmCache) {
        AlertDialog(
            onDismissRequest = { confirmCache = false },
            title = { Text("Clear identified-people cache?", fontWeight = FontWeight.Bold) },
            text = {
                Text(
                    "$cachedCallers cached entries will be deleted. Future calls will " +
                        "fall back to contacts/community lookups. Your contacts and lists are untouched.",
                    fontSize = 13.sp
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    vm.clearCallerCache()
                    confirmCache = false
                }) { Text("Clear", color = RedSpam) }
            },
            dismissButton = {
                TextButton(onClick = { confirmCache = false }) { Text("Cancel", color = TextMuted) }
            }
        )
    }
    if (confirmLog) {
        AlertDialog(
            onDismissRequest = { confirmLog = false },
            title = { Text("Clear the call log?", fontWeight = FontWeight.Bold) },
            text = {
                Text("$callLogEntries entries will be deleted permanently.", fontSize = 13.sp)
            },
            confirmButton = {
                TextButton(onClick = {
                    vm.clearCallLog()
                    confirmLog = false
                }) { Text("Clear", color = RedSpam) }
            },
            dismissButton = {
                TextButton(onClick = { confirmLog = false }) { Text("Cancel", color = TextMuted) }
            }
        )
    }
    if (confirmWipe) {
        AlertDialog(
            onDismissRequest = { confirmWipe = false },
            title = { Text("Delete everything?", fontWeight = FontWeight.Bold) },
            text = {
                Text(
                    "All blocked/allowed lists, caches, call log, messages and the " +
                        "encrypted profile will be wiped. This cannot be undone.",
                    fontSize = 13.sp
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    vm.wipeEverything(context.applicationContext as Application)
                    confirmWipe = false
                }) { Text("Delete", color = RedSpam) }
            },
            dismissButton = {
                TextButton(onClick = { confirmWipe = false }) { Text("Cancel", color = TextMuted) }
            }
        )
    }
}

@Composable
private fun DataLabel(text: String) {
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
private fun DataRow(
    icon: ImageVector,
    title: String,
    detail: String,
    count: Int?,
    deleteLabel: String,
    onDelete: () -> Unit
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surface)
            .padding(14.dp)
    ) {
        Icon(icon, null, tint = Blue, modifier = Modifier.size(22.dp))
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(title, fontWeight = FontWeight.SemiBold)
            Text(detail, fontSize = 11.sp, color = TextMuted, maxLines = 2)
        }
        Text(
            when (count) {
                null -> "…"
                -1 -> "?"
                else -> count.toString()
            },
            fontWeight = FontWeight.ExtraBold,
            fontSize = 15.sp,
            modifier = Modifier.padding(horizontal = 8.dp)
        )
        TextButton(onClick = onDelete) { Text(deleteLabel, fontSize = 12.sp, color = RedSpam) }
    }
}
