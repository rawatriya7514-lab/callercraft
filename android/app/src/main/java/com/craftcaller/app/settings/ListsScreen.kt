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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.craftcaller.app.data.local.DatabaseProvider
import com.craftcaller.app.data.local.entity.AllowedNumber
import com.craftcaller.app.data.local.entity.BlockedNumber
import com.craftcaller.app.ui.common.Avatar
import com.craftcaller.app.ui.common.EmptyState
import com.craftcaller.app.ui.common.rememberVm
import com.craftcaller.app.ui.theme.Blue
import com.craftcaller.app.ui.theme.GreenOk
import com.craftcaller.app.ui.theme.RedSpam
import com.craftcaller.app.ui.theme.TextMuted
import com.craftcaller.app.util.Craft
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class ListsViewModel(app: Application) : AndroidViewModel(app) {

    private val db = DatabaseProvider.get(app)

    var blocked by mutableStateOf<List<BlockedNumber>>(emptyList())
        private set
    var allowed by mutableStateOf<List<AllowedNumber>>(emptyList())
        private set
    var loadFailed by mutableStateOf(false)
        private set

    init {
        // Each list observes independently; a database hiccup surfaces as an
        // honest error state instead of pretending the lists are empty.
        viewModelScope.launch {
            try {
                db.blockedDao().observeAll().collect { blocked = it }
            } catch (_: Exception) {
                loadFailed = true
            }
        }
        viewModelScope.launch {
            try {
                db.allowedDao().observeAll().collect { allowed = it }
            } catch (_: Exception) {
                loadFailed = true
            }
        }
    }

    fun addBlocked(rawNumber: String) {
        val number = Craft.normalizeNumber(rawNumber)
        if (number.length < 8) return
        viewModelScope.launch(Dispatchers.IO) {
            db.blockedDao().insert(BlockedNumber(number = number, reason = "Manual"))
        }
    }

    fun addAllowed(rawNumber: String, label: String?) {
        val number = Craft.normalizeNumber(rawNumber)
        if (number.length < 8) return
        viewModelScope.launch(Dispatchers.IO) {
            db.allowedDao().insert(AllowedNumber(number = number, label = label))
        }
    }

    fun removeBlocked(number: String) {
        viewModelScope.launch(Dispatchers.IO) { db.blockedDao().delete(number) }
    }

    fun removeAllowed(number: String) {
        viewModelScope.launch(Dispatchers.IO) { db.allowedDao().delete(number) }
    }
}

@Composable
fun ListsScreen(onClose: () -> Unit) {
    val context = LocalContext.current
    val vm: ListsViewModel = rememberVm { ListsViewModel(context.applicationContext as Application) }
    var manualNumber by mutableStateOf("")
    var manualLabel by mutableStateOf("")

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
                Text("Block & allow lists", fontWeight = FontWeight.ExtraBold, fontSize = 17.sp)
                Text("Manifest numbers — screening honours both instantly", fontSize = 11.sp, color = TextMuted)
            }
        }

        if (vm.loadFailed) {
            Box(Modifier.padding(16.dp)) {
                EmptyState("Could not read the lists database right now.")
            }
        } else {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(bottom = 28.dp)
        ) {
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(MaterialTheme.colorScheme.surface)
                        .padding(14.dp)
                ) {
                    Text("Add a number", fontWeight = FontWeight.Bold)
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        OutlinedTextField(
                            value = manualNumber,
                            onValueChange = { manualNumber = it },
                            label = { Text("Phone number") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                        Spacer(Modifier.width(8.dp))
                        OutlinedTextField(
                            value = manualLabel,
                            onValueChange = { manualLabel = it },
                            label = { Text("Label (allow only)") },
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                    }
                    Row(modifier = Modifier.padding(top = 8.dp)) {
                        Button(
                            onClick = {
                                vm.addBlocked(manualNumber)
                                manualNumber = ""
                                manualLabel = ""
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = RedSpam),
                            enabled = Craft.normalizeNumber(manualNumber).length >= 8,
                            shape = RoundedCornerShape(14.dp)
                        ) {
                            Text("Block number", color = Color.White)
                        }
                        Spacer(Modifier.width(10.dp))
                        Button(
                            onClick = {
                                vm.addAllowed(manualNumber, manualLabel.ifBlank { null })
                                manualNumber = ""
                                manualLabel = ""
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = GreenOk),
                            enabled = Craft.normalizeNumber(manualNumber).length >= 8,
                            shape = RoundedCornerShape(14.dp)
                        ) {
                            Text("Allow number", color = Color.White)
                        }
                    }
                    Text(
                        "\"Allow\" wins over community spam reports: an allowed number is " +
                            "never auto-rejected, even when it has 10+ reports.",
                        fontSize = 11.sp,
                        color = TextMuted,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }
            }

            item { ListTitle("${vm.blocked.size} blocked — auto-rejected", RedSpam) }
            if (vm.blocked.isEmpty()) {
                item { EmptyInline("Nothing blocked yet") }
            } else items(vm.blocked, key = { "b${it.number}" }) { entry ->
                ListRow(
                    name = entry.savedName ?: Craft.prettyNumber(entry.number),
                    number = Craft.prettyNumber(entry.number),
                    chip = "Blocked",
                    chipTint = RedSpam,
                    onRemove = { vm.removeBlocked(entry.number) },
                    removeDescription = "Unblock ${Craft.prettyNumber(entry.number)}"
                )
            }

            item { ListTitle("${vm.allowed.size} allowed — never auto-rejected", GreenOk) }
            if (vm.allowed.isEmpty()) {
                item { EmptyInline("Nothing allowed yet") }
            } else items(vm.allowed, key = { "a${it.number}" }) { entry ->
                ListRow(
                    name = entry.label ?: Craft.prettyNumber(entry.number),
                    number = Craft.prettyNumber(entry.number),
                    chip = "Allowed",
                    chipTint = GreenOk,
                    onRemove = { vm.removeAllowed(entry.number) },
                    removeDescription = "Stop allowing ${Craft.prettyNumber(entry.number)}"
                )
            }
        }
        }
    }
}

@Composable
private fun ListTitle(text: String, tint: Color) {
    Text(
        text,
        fontSize = 13.sp,
        fontWeight = FontWeight.Bold,
        color = tint,
        modifier = Modifier.padding(horizontal = 20.dp, vertical = 10.dp)
    )
}

@Composable
private fun EmptyInline(text: String) {
    Box(Modifier.padding(horizontal = 16.dp)) { EmptyState(text) }
}

@Composable
private fun ListRow(
    name: String,
    number: String,
    chip: String,
    chipTint: Color,
    onRemove: () -> Unit,
    removeDescription: String
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 2.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(MaterialTheme.colorScheme.surface)
            .padding(horizontal = 14.dp, vertical = 10.dp)
    ) {
        Avatar(name, size = 36.dp)
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(name, fontWeight = FontWeight.SemiBold)
            Text(number, fontSize = 12.sp, color = TextMuted)
        }
        Text(chip, fontSize = 11.sp, color = chipTint, fontWeight = FontWeight.Bold)
        IconButton(
            onClick = onRemove,
            modifier = Modifier.semantics { contentDescription = removeDescription }
        ) {
            Icon(Icons.Filled.Close, contentDescription = null, tint = TextMuted)
        }
    }
}
