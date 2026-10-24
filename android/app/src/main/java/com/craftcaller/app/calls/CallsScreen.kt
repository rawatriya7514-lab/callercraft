package com.craftcaller.app.calls

import android.app.Application
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.CallMade
import androidx.compose.material.icons.filled.CallMissed
import androidx.compose.material.icons.filled.CallReceived
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Report
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.craftcaller.app.data.local.DatabaseProvider
import com.craftcaller.app.data.local.entity.BlockedNumber
import com.craftcaller.app.data.local.entity.CallLogEntry
import com.craftcaller.app.data.local.entity.CallerInfo
import com.craftcaller.app.data.remote.FirebaseRepository
import com.craftcaller.app.repository.CallLogRepository
import com.craftcaller.app.ui.common.Avatar
import com.craftcaller.app.ui.common.TagChip
import com.craftcaller.app.ui.common.rememberVm
import com.craftcaller.app.ui.theme.RedSpam
import com.craftcaller.app.ui.theme.TextMuted
import com.craftcaller.app.util.Craft
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import android.provider.CallLog

class CallsViewModel(app: Application) : AndroidViewModel(app) {
    private val db = DatabaseProvider.get(app)

    var loading by mutableStateOf(true)
    var entries by mutableStateOf<List<CallLogEntry>>(emptyList())
        private set
    var query by mutableStateOf("")

    init {
        viewModelScope.launch {
            withContext(Dispatchers.IO) { CallLogRepository(app, db).syncRecent() }
            db.callLogDao().observeAll().collect { list ->
                entries = list
                loading = false
            }
        }
    }

    fun refresh() {
        viewModelScope.launch {
            withContext(Dispatchers.IO) { CallLogRepository(getApplication(), db).syncRecent() }
        }
    }

    fun block(number: String) {
        viewModelScope.launch(Dispatchers.IO) {
            db.blockedDao().insert(BlockedNumber(number = number, reason = "Calls tab"))
            db.callLogDao().annotate(number, null, true)
        }
    }

    fun reportSpam(number: String) {
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
            db.callLogDao().annotate(number, known?.name, ((known?.spamReports ?: 0) + 1) >= 10)
            runCatching { FirebaseRepository.reportSpam(number) }
        }
    }
}

private fun typeIcon(type: Int): Pair<ImageVector, Color> = when (type) {
    CallLog.Calls.INCOMING_TYPE -> Icons.Filled.CallReceived to Color(0xFF10B981)
    CallLog.Calls.OUTGOING_TYPE -> Icons.Filled.CallMade to Color(0xFF0A6CFF)
    CallLog.Calls.MISSED_TYPE, CallLog.Calls.REJECTED_TYPE, CallLog.Calls.BLOCKED_TYPE ->
        Icons.Filled.CallMissed to RedSpam
    else -> Icons.Filled.Call to TextMuted
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CallsScreen() {
    val context = LocalContext.current
    val vm: CallsViewModel = rememberVm { CallsViewModel(context.applicationContext as Application) }
    var selected by remember { mutableStateOf<CallLogEntry?>(null) }

    val filtered = remember(vm.entries, vm.query) {
        val q = vm.query.trim()
        if (q.isEmpty()) vm.entries else vm.entries.filter {
            it.number.contains(q) || (it.resolvedName?.contains(q, ignoreCase = true) == true)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        OutlinedTextField(
            value = vm.query,
            onValueChange = { vm.query = it },
            placeholder = { Text("Search call history…", fontSize = 14.sp) },
            singleLine = true,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp)
        )
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 16.dp)
        ) {
            Text(
                "${filtered.size} calls",
                fontSize = 12.sp,
                color = TextMuted,
                modifier = Modifier.weight(1f)
            )
            IconButton(onClick = { vm.refresh() }) {
                Icon(Icons.Filled.Refresh, contentDescription = "Refresh call history")
            }
        }
        Spacer(Modifier.height(4.dp))

        if (vm.loading) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(2.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                items(filtered, key = { it.id }) { entry ->
                    val (icon, tint) = typeIcon(entry.callType)
                    val name = entry.resolvedName ?: "Unknown"
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { selected = entry }
                            .padding(horizontal = 16.dp, vertical = 10.dp)
                    ) {
                        Avatar(if (entry.resolvedName != null) name else null)
                        Spacer(Modifier.width(12.dp))
                        Column(Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    name,
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 15.sp,
                                    color = if (entry.isSpam) RedSpam else MaterialTheme.colorScheme.onSurface
                                )
                                if (entry.isSpam) {
                                    Spacer(Modifier.width(8.dp))
                                    TagChip("Spam — Likely Fraud", RedSpam)
                                }
                            }
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(icon, null, tint = tint, modifier = Modifier.size(14.dp))
                                Text(
                                    "  ${Craft.prettyNumber(entry.number)} • ${Craft.formatTime(entry.timestamp)}",
                                    fontSize = 12.sp,
                                    color = TextMuted
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    selected?.let { entry ->
        ModalBottomSheet(onDismissRequest = { selected = null }) {
            CallDetail(
                entry = entry,
                onBlock = { vm.block(entry.number); selected = null },
                onReport = { vm.reportSpam(entry.number); selected = null },
                onClose = { selected = null }
            )
        }
    }
}

@Composable
private fun CallDetail(
    entry: CallLogEntry,
    onBlock: () -> Unit,
    onReport: () -> Unit,
    onClose: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 8.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Avatar(if (entry.resolvedName != null) entry.resolvedName else null, size = 56.dp)
            Spacer(Modifier.width(14.dp))
            Column {
                Text(
                    entry.resolvedName ?: "Unknown",
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 20.sp
                )
                Text(Craft.prettyNumber(entry.number), color = TextMuted, fontSize = 14.sp)
                Text(
                    Craft.formatTime(entry.timestamp),
                    color = TextMuted,
                    fontSize = 12.sp,
                    modifier = Modifier.padding(top = 2.dp)
                )
            }
        }
        if (entry.isSpam) {
            Box(
                modifier = Modifier
                    .padding(top = 12.dp)
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(RedSpam.copy(alpha = 0.12f))
                    .padding(12.dp)
            ) {
                Text(
                    "🚨 Spam — Likely Fraud (reported by users)",
                    color = RedSpam,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 13.sp
                )
            }
        }
        Spacer(Modifier.height(18.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Button(
                onClick = onBlock,
                colors = ButtonDefaults.buttonColors(containerColor = RedSpam),
                modifier = Modifier.weight(1f)
            ) {
                Icon(Icons.Filled.Block, null, tint = Color.White)
                Spacer(Modifier.width(6.dp))
                Text("Block", color = Color.White)
            }
            Button(
                onClick = onReport,
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFF59E0B)),
                modifier = Modifier.weight(1f)
            ) {
                Icon(Icons.Filled.Report, null, tint = Color.White)
                Spacer(Modifier.width(6.dp))
                Text("Report spam", color = Color.White)
            }
        }
        TextButton(
            onClick = onClose,
            modifier = Modifier.align(Alignment.CenterHorizontally)
        ) { Text("Close", color = TextMuted) }
        Spacer(Modifier.height(12.dp))
    }
}
