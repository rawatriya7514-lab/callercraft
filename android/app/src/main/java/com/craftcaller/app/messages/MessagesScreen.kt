package com.craftcaller.app.messages

import android.app.Application
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.craftcaller.app.data.local.DatabaseProvider
import com.craftcaller.app.data.local.entity.SmsMessage
import com.craftcaller.app.repository.SmsRepository
import com.craftcaller.app.ui.common.Avatar
import com.craftcaller.app.ui.common.TagChip
import com.craftcaller.app.ui.common.rememberVm
import com.craftcaller.app.ui.theme.GreenOk
import com.craftcaller.app.ui.theme.RedSpam
import com.craftcaller.app.ui.theme.TextMuted
import com.craftcaller.app.util.Craft
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MessagesViewModel(app: Application) : AndroidViewModel(app) {
    private val db = DatabaseProvider.get(app)

    var loading by mutableStateOf(true)
    var messages by mutableStateOf<List<SmsMessage>>(emptyList())
        private set
    var query by mutableStateOf("")

    init {
        viewModelScope.launch {
            val repo = SmsRepository(app, db)
            withContext(Dispatchers.IO) {
                val fresh = repo.syncInbox()
                // Annotate persisted cached names where we know the sender.
                fresh.take(40).distinctBy { it.address }.forEach { sms ->
                    val known = db.callerDao().get(sms.address)
                    if (known?.name != null) db.smsDao().annotate(sms.address, known.name)
                }
            }
            db.smsDao().observeAll().collect { list ->
                messages = list
                loading = false
            }
        }
    }
}

@Composable
fun MessagesScreen() {
    val context = LocalContext.current
    val clipboard = LocalClipboardManager.current
    val vm: MessagesViewModel = rememberVm { MessagesViewModel(context.applicationContext as Application) }

    var query by remember { mutableStateOf("") }
    val filtered = remember(vm.messages, query) {
        val q = query.trim().lowercase()
        if (q.isEmpty()) vm.messages else vm.messages.filter {
            it.body.lowercase().contains(q) || it.address.contains(q) ||
                (it.senderName?.lowercase()?.contains(q) == true)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        OutlinedTextField(
            value = query,
            onValueChange = { query = it },
            placeholder = { Text("Search messages…", fontSize = 14.sp) },
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
                "${filtered.size} messages • ${filtered.count { it.isSpam }} spam",
                fontSize = 12.sp,
                color = TextMuted
            )
        }

        if (vm.loading) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(2.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                items(filtered, key = { it.id }) { sms ->
                    MessageRow(sms) { code ->
                        clipboard.setText(AnnotatedString(code))
                        Toast.makeText(
                            context,
                            "OTP $code copied",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
            }
        }
    }
}

@Composable
private fun MessageRow(sms: SmsMessage, onCopyOtp: (String) -> Unit) {
    Surface(
        color = if (sms.isSpam) RedSpam.copy(alpha = 0.08f) else MaterialTheme.colorScheme.surface,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 3.dp),
        shape = RoundedCornerShape(14.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(12.dp)
        ) {
            Avatar(sms.senderName ?: "S", size = 40.dp)
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        sms.senderName ?: sms.address,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 14.sp
                    )
                    if (sms.isSpam) {
                        Spacer(Modifier.width(8.dp))
                        TagChip("Spam SMS", RedSpam)
                    }
                }
                Text(
                    sms.body,
                    fontSize = 12.sp,
                    color = TextMuted,
                    maxLines = 2,
                    lineHeight = 17.sp
                )
                Text(Craft.formatTime(sms.timestamp), fontSize = 10.sp, color = TextMuted)
            }
            sms.otpCode?.let { code ->
                Button(
                    onClick = { onCopyOtp(code) },
                    colors = ButtonDefaults.buttonColors(containerColor = GreenOk)
                ) {
                    Text(
                        "Copy $code",
                        color = Color.White,
                        fontSize = 12.sp,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }
        }
    }
}
