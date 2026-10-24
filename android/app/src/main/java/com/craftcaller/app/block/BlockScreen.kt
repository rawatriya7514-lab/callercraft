package com.craftcaller.app.block

import android.app.Activity
import android.app.Application
import android.content.Context
import android.content.Intent
import android.os.Build
import android.app.role.RoleManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import com.craftcaller.app.data.local.DatabaseProvider
import com.craftcaller.app.data.local.entity.BlockedNumber
import com.craftcaller.app.service.RecordingService
import com.craftcaller.app.ui.common.Avatar
import com.craftcaller.app.ui.common.EmptyState
import com.craftcaller.app.ui.common.SectionTitle
import com.craftcaller.app.ui.common.rememberVm
import com.craftcaller.app.ui.theme.Blue
import com.craftcaller.app.ui.theme.GreenOk
import com.craftcaller.app.ui.theme.RedSpam
import com.craftcaller.app.ui.theme.TextMuted
import com.craftcaller.app.util.Craft
import com.craftcaller.app.util.Prefs
import com.craftcaller.app.util.RequiredPermissions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class BlockViewModel(app: Application) : AndroidViewModel(app) {
    private val db = DatabaseProvider.get(app)

    var blocked by mutableStateOf<List<BlockedNumber>>(emptyList())
        private set
    var count by mutableStateOf(0)
        private set

    init {
        viewModelScope.launch {
            db.blockedDao().observeAll().collect { blocked = it }
        }
        viewModelScope.launch {
            db.blockedDao().observeCount().collectLatest { count = it }
        }
    }

    fun add(number: String, name: String?) {
        viewModelScope.launch(Dispatchers.IO) {
            db.blockedDao().insert(BlockedNumber(number = number, savedName = name, reason = "Manual"))
        }
    }

    fun remove(number: String) {
        viewModelScope.launch(Dispatchers.IO) { db.blockedDao().delete(number) }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BlockScreen() {
    val context = LocalContext.current
    val vm: BlockViewModel = rememberVm { BlockViewModel(context.applicationContext as Application) }
    val prefs = remember { Prefs(context) }
    var autoBlockSpammers by remember { mutableStateOf(prefs.autoBlockSpammers) }
    var manualNumber by remember { mutableStateOf("") }
    var recording by remember { mutableStateOf(RecordingService.isRecording) }

    val micPermission = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            context.startForegroundService(
                Intent(context, RecordingService::class.java)
                    .setAction(RecordingService.ACTION_START)
            )
        }
    }

    // Poll recording state for the toggle UI.
    LaunchedEffect(Unit) {
        while (true) {
            recording = RecordingService.isRecording
            delay(600)
        }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(vertical = 6.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {

        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .clip(RoundedCornerShape(18.dp))
                    .background(Blue.copy(alpha = 0.08f))
                    .padding(16.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.Shield, null, tint = Blue)
                    Spacer(Modifier.width(10.dp))
                    Column {
                        Text("Spam Shield", fontWeight = FontWeight.Bold)
                        Text(
                            "${vm.count} numbers in your personal blocklist",
                            fontSize = 12.sp,
                            color = TextMuted
                        )
                    }
                }
            }
        }

        item {
            Column(Modifier.padding(horizontal = 16.dp)) {
                SectionTitle("Protection toggles", Modifier.padding(vertical = 6.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(Modifier.weight(1f)) {
                        Text("Auto-block top spammers", fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
                        Text(
                            "Reject calls reported as spam by 10+ users",
                            fontSize = 12.sp,
                            color = TextMuted
                        )
                    }
                    Switch(
                        checked = autoBlockSpammers,
                        onCheckedChange = {
                            autoBlockSpammers = it
                            prefs.autoBlockSpammers = it
                        }
                    )
                }
            }
        }

        item {
            Column(Modifier.padding(horizontal = 16.dp)) {
                SectionTitle("Call screening (system-level)", Modifier.padding(vertical = 6.dp))
                TextButton(
                    onClick = { screeningRoleRequest(context)?.let(context::startActivity) },
                    colors = ButtonDefaults.textButtonColors(contentColor = Blue)
                ) {
                    Text("Make CraftCaller your call-screening app", fontWeight = FontWeight.SemiBold)
                }
                Text(
                    "Required for automatic rejection of blocked numbers even before the phone rings.",
                    fontSize = 12.sp,
                    color = TextMuted
                )
            }
        }

        item {
            Column(Modifier.padding(horizontal = 16.dp)) {
                SectionTitle("Call recording", Modifier.padding(vertical = 6.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Button(
                        onClick = {
                            if (recording) {
                                context.startForegroundService(
                                    Intent(context, RecordingService::class.java)
                                        .setAction(RecordingService.ACTION_STOP)
                                )
                            } else {
                                if (RequiredPermissions.granted(context, android.Manifest.permission.RECORD_AUDIO)) {
                                    context.startForegroundService(
                                        Intent(context, RecordingService::class.java)
                                            .setAction(RecordingService.ACTION_START)
                                    )
                                } else {
                                    micPermission.launch(android.Manifest.permission.RECORD_AUDIO)
                                }
                            }
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (recording) RedSpam else GreenOk
                        )
                    ) {
                        if (recording) CircularProgressIndicator(color = Color.White, strokeWidth = 2.dp)
                        else Text(if (recording) "Stop recording" else "Start recording", color = Color.White)
                    }
                }
                Text(
                    "Recordings land in Android/data/com.craftcaller.app/files/craftcaller_recordings.",
                    fontSize = 12.sp,
                    color = TextMuted,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }
        }

        item {
            Column(Modifier.padding(horizontal = 16.dp)) {
                SectionTitle("Add number to blocklist", Modifier.padding(vertical = 6.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    OutlinedTextField(
                        value = manualNumber,
                        onValueChange = { manualNumber = it },
                        label = { Text("Phone number") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                    Spacer(Modifier.width(10.dp))
                    Button(
                        onClick = {
                            val normalized = Craft.normalizeNumber(manualNumber)
                            if (normalized.length >= 8) {
                                vm.add(normalized, null)
                                manualNumber = ""
                            }
                        }
                    ) {
                        Icon(Icons.Filled.Add, null, tint = Color.White)
                    }
                }
            }
        }

        item { SectionTitle("Blocked numbers", Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) }

        if (vm.blocked.isEmpty()) {
            item { Box(Modifier.height(220.dp)) { EmptyState("No blocked numbers yet") } }
        }

        items(vm.blocked, key = { it.number }) { entry ->
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp)
            ) {
                Avatar(entry.savedName ?: "B", size = 36.dp)
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text(
                        entry.savedName ?: Craft.prettyNumber(entry.number),
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(Craft.prettyNumber(entry.number), fontSize = 12.sp, color = TextMuted)
                }
                IconButton(onClick = { vm.remove(entry.number) }) {
                    Icon(Icons.Filled.Close, contentDescription = "Unblock", tint = TextMuted)
                }
            }
        }

        item { Spacer(Modifier.height(24.dp)) }
    }
}

private fun screeningRoleRequest(context: Context): Intent? {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) return null
    val roleManager = context.getSystemService(RoleManager::class.java) ?: return null
    if (roleManager.isRoleAvailable(RoleManager.ROLE_CALL_SCREENING) &&
        !roleManager.isRoleHeld(RoleManager.ROLE_CALL_SCREENING)
    ) {
        return roleManager.createRequestRoleIntent(RoleManager.ROLE_CALL_SCREENING)
    }
    return null
}
