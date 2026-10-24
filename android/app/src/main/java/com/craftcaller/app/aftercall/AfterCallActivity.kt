package com.craftcaller.app.aftercall

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
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
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.craftcaller.app.data.local.DatabaseProvider
import com.craftcaller.app.data.local.entity.BlockedNumber
import com.craftcaller.app.data.local.entity.CallerInfo
import com.craftcaller.app.data.remote.FirebaseRepository
import com.craftcaller.app.ui.common.Avatar
import com.craftcaller.app.ui.theme.CraftCallerTheme
import com.craftcaller.app.ui.theme.GreenOk
import com.craftcaller.app.ui.theme.RedSpam
import com.craftcaller.app.ui.theme.TextMuted
import com.craftcaller.app.util.Craft
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Shown right after an incoming call ends (via the "Who was that call from?"
 * notification): add a name, report spam, or block the number.
 */
class AfterCallActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val number = Craft.normalizeNumber(intent.getStringExtra(EXTRA_NUMBER).orEmpty())
        if (number.isEmpty()) {
            finish()
            return
        }
        enableEdgeToEdge()
        setContent {
            CraftCallerTheme {
                AfterCallContent(number = number, onClose = { finish() })
            }
        }
    }

    companion object {
        const val EXTRA_NUMBER = "number"
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AfterCallContent(number: String, onClose: () -> Unit) {
    val context = LocalContext.current
    val scope = androidx.compose.runtime.rememberCoroutineScope()
    val db = remember { DatabaseProvider.get(context) }

    var info by remember { mutableStateOf<CallerInfo?>(null) }
    var name by remember { mutableStateOf("") }
    var message by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(number) {
        val loaded = withContext(Dispatchers.IO) { db.callerDao().get(number) }
        info = loaded
        name = loaded?.name.orEmpty()
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = { Text("After call", fontWeight = FontWeight.ExtraBold, color = com.craftcaller.app.ui.theme.Blue) },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(22.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Avatar(info?.name, size = 56.dp)
                Spacer(Modifier.width(14.dp))
                Column(Modifier.weight(1f)) {
                    Text(
                        info?.name ?: "Unknown",
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 22.sp,
                        color = if (info?.isSpam == true) RedSpam else MaterialTheme.colorScheme.onSurface
                    )
                    Text(Craft.prettyNumber(number), color = TextMuted)
                    if (info?.isSpam == true) {
                        Text(
                            "Spam — Likely Fraud (${info?.spamReports} reports)",
                            color = RedSpam,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }

            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("Add or correct a name") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Button(
                    onClick = {
                        val trimmed = name.trim()
                        if (trimmed.isEmpty()) return@Button
                        scope.launch(Dispatchers.IO) {
                            db.callerDao().setName(number, trimmed, System.currentTimeMillis())
                            runCatching { FirebaseRepository.registerOwnNumber(number, trimmed) }
                            withContext(Dispatchers.Main) { message = "Name saved — thanks!" }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = com.craftcaller.app.ui.theme.Blue),
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Save name", color = Color.White)
                }

                Button(
                    onClick = {
                        scope.launch(Dispatchers.IO) {
                            val known = db.callerDao().get(number)
                            db.callerDao().upsert(
                                CallerInfo(
                                    number = number,
                                    name = name.ifBlank { known?.name },
                                    location = known?.location,
                                    spamReports = (known?.spamReports ?: 0) + 1,
                                    updatedAt = System.currentTimeMillis(),
                                    source = known?.source ?: "user-report"
                                )
                            )
                            runCatching { FirebaseRepository.reportSpam(number) }
                            withContext(Dispatchers.Main) { message = "Spam report submitted" }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFF59E0B)),
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Report spam", color = Color.White)
                }
            }

            Button(
                onClick = {
                    scope.launch(Dispatchers.IO) {
                        db.blockedDao().insert(
                            BlockedNumber(
                                number = number,
                                savedName = name.ifBlank { null },
                                reason = "After-call screen"
                            )
                        )
                        withContext(Dispatchers.Main) { message = "Number blocked" }
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = RedSpam),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Block this number", color = Color.White)
            }

            message?.let { msg ->
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(MaterialTheme.shapes.medium)
                        .background(GreenOk.copy(alpha = 0.12f))
                        .padding(12.dp)
                ) {
                    Text(msg, color = GreenOk, fontWeight = FontWeight.SemiBold)
                }
            }

            TextButton(onClick = onClose, modifier = Modifier.align(Alignment.CenterHorizontally)) {
                Text("Done", color = TextMuted)
            }
            Spacer(Modifier.height(20.dp))
        }
    }
}
