package com.craftcaller.app.contacts

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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.Call
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.craftcaller.app.data.local.DatabaseProvider
import com.craftcaller.app.data.local.entity.BlockedNumber
import com.craftcaller.app.data.local.entity.CallerInfo
import com.craftcaller.app.data.model.DeviceContact
import com.craftcaller.app.data.remote.FirebaseRepository
import com.craftcaller.app.repository.ContactsRepository
import com.craftcaller.app.ui.common.Avatar
import com.craftcaller.app.ui.common.TagChip
import com.craftcaller.app.ui.common.rememberVm
import com.craftcaller.app.ui.theme.Blue
import com.craftcaller.app.ui.theme.GreenOk
import com.craftcaller.app.ui.theme.RedSpam
import com.craftcaller.app.ui.theme.TextMuted
import com.craftcaller.app.util.Craft
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class ContactsViewModel(app: Application) : AndroidViewModel(app) {
    private val db = DatabaseProvider.get(app)

    var loading by mutableStateOf(true)
    var contacts by mutableStateOf<List<DeviceContact>>(emptyList())
        private set
    var query by mutableStateOf("")

    init {
        viewModelScope.launch {
            contacts = withContext(Dispatchers.IO) {
                ContactsRepository(app).loadDeviceContacts()
            }
            loading = false
        }
    }

    fun block(number: String) {
        viewModelScope.launch(Dispatchers.IO) {
            db.blockedDao().insert(BlockedNumber(number = number, reason = "Contacts tab"))
        }
    }

    fun reportSpam(number: String) {
        viewModelScope.launch(Dispatchers.IO) {
            val known = db.callerDao().get(number)
            db.callerDao().upsert(
                CallerInfo(
                    number = number,
                    name = known?.name ?: nameOf(number),
                    location = known?.location,
                    spamReports = (known?.spamReports ?: 0) + 1,
                    updatedAt = System.currentTimeMillis(),
                    source = known?.source ?: "user-report"
                )
            )
            runCatching { FirebaseRepository.reportSpam(number) }
        }
    }

    private fun nameOf(number: String): String? =
        contacts.firstOrNull { it.number == number }?.name
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ContactsScreen() {
    val context = LocalContext.current
    val vm: ContactsViewModel = rememberVm { ContactsViewModel(context.applicationContext as Application) }
    var selected by remember { mutableStateOf<DeviceContact?>(null) }

    val callerDao = remember { DatabaseProvider.get(context).callerDao() }
    val knownNames = remember(vm.contacts) {
        mutableStateOf<Map<String, CallerInfo>>(emptyMap())
    }

    // Resolve CraftCaller community names for the visible directory (best-effort cache).
    androidx.compose.runtime.LaunchedEffect(vm.contacts) {
        val map = HashMap<String, CallerInfo>()
        vm.contacts.forEach { contact ->
            callerDao.get(contact.number)?.let { info -> map[contact.number] = info }
        }
        knownNames.value = map
    }

    val filtered = remember(vm.contacts, vm.query) {
        val q = vm.query.trim().lowercase()
        if (q.isEmpty()) vm.contacts else vm.contacts.filter {
            it.name.lowercase().contains(q) || it.number.contains(q)
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
            placeholder = { Text("Search contacts…", fontSize = 14.sp) },
            singleLine = true,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp)
        )
        Text(
            "${filtered.size} contacts",
            fontSize = 12.sp,
            color = TextMuted,
            modifier = Modifier.padding(horizontal = 16.dp)
        )
        Spacer(Modifier.height(4.dp))

        if (vm.loading) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                items(filtered, key = { it.number + it.name }) { contact ->
                    val known = knownNames.value[contact.number]
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { selected = contact }
                            .padding(horizontal = 16.dp, vertical = 10.dp)
                    ) {
                        Avatar(contact.name)
                        Spacer(Modifier.width(12.dp))
                        Column(Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(contact.name, fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
                                if (known != null) {
                                    Spacer(Modifier.width(8.dp))
                                    TagChip("CraftCaller", Blue)
                                }
                            }
                            Text(
                                Craft.prettyNumber(contact.number),
                                fontSize = 12.sp,
                                color = TextMuted
                            )
                        }
                        IconButton(onClick = {
                            context.startActivity(
                                Intent(Intent.ACTION_DIAL, Uri.parse("tel:${contact.number}"))
                            )
                        }) {
                            Icon(
                                Icons.Filled.Call,
                                contentDescription = "Call",
                                tint = GreenOk
                            )
                        }
                    }
                }
            }
        }
    }

    selected?.let { contact ->
        ModalBottomSheet(onDismissRequest = { selected = null }) {
            Column(Modifier.padding(horizontal = 24.dp, vertical = 8.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Avatar(contact.name, size = 56.dp)
                    Spacer(Modifier.width(14.dp))
                    Column {
                        Text(contact.name, fontWeight = FontWeight.ExtraBold, fontSize = 20.sp)
                        Text(Craft.prettyNumber(contact.number), color = TextMuted, fontSize = 14.sp)
                    }
                }
                Spacer(Modifier.height(18.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Button(
                        onClick = {
                            context.startActivity(
                                Intent(Intent.ACTION_CALL, Uri.parse("tel:${contact.number}"))
                            )
                            selected = null
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = GreenOk),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Filled.Call, null, tint = Color.White)
                        Spacer(Modifier.width(6.dp))
                        Text("Call", color = Color.White)
                    }
                    Button(
                        onClick = { vm.block(contact.number); selected = null },
                        colors = ButtonDefaults.buttonColors(containerColor = RedSpam),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Filled.Block, null, tint = Color.White)
                        Spacer(Modifier.width(6.dp))
                        Text("Block", color = Color.White)
                    }
                }
                TextButton(
                    onClick = { vm.reportSpam(contact.number); selected = null },
                    modifier = Modifier.align(Alignment.CenterHorizontally)
                ) { Text("Report as spam", color = Color(0xFFF59E0B)) }
                Spacer(Modifier.height(12.dp))
            }
        }
    }
}
