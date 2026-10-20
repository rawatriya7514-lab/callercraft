package com.craftcaller.app.ui.screens

import com.craftcaller.app.ui.CraftPalette
import android.content.Intent
import android.net.Uri
import android.provider.ContactsContract
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.craftcaller.app.data.CallerInfo

import com.craftcaller.app.data.Repo
import com.craftcaller.app.ui.common.Avatar
import com.craftcaller.app.ui.common.Badge
import com.craftcaller.app.util.CallerHub
import com.craftcaller.app.util.PhoneUtils
import kotlinx.coroutines.launch
import androidx.compose.runtime.rememberCoroutineScope

/**
 * The Truecaller-style "number sheet": identity, spam score, location and the
 * action row (report spam / block / add contact / copy). Used by the search
 * results, the after-call sheet and the ring-time call card.
 */
@Composable
fun NumberDetailSheet(info: CallerInfo, mode: String = "search", onClose: (() -> Unit)? = null) {
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()
    var live by remember(info.number) { mutableStateOf(info) }

    fun bump(call: suspend () -> Unit) {
        scope.launch {
            runCatching { call() }
            CallerHub.forceRefresh()
        }
    }

    LaunchedEffect(info.number) {
        if (info.name.isEmpty()) {
            val fresh = runCatching { Repo.lookup(info.number) }.getOrNull()
            if (fresh != null && fresh.name.isNotEmpty()) live = fresh
        }
    }

    Column(
        Modifier
            .fillMaxWidth()
            .background(
                MaterialTheme.colorScheme.surface.copy(alpha = 0.98f),
                RoundedCornerShape(18.dp)
            )
            .padding(16.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Avatar(live.displayName, live.spam, 52)
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    live.displayName,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 20.sp,
                    color = if (live.spam) CraftPalette.Spam else MaterialTheme.colorScheme.onSurface
                )
                Text(
                    PhoneUtils.emask(live.number.ifEmpty { "unknown" }),
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.65f)
                )
            }
            if (onClose != null) {
                Text(
                    "✕", fontSize = 18.sp,
                    modifier = Modifier
                        .clickable { onClose() }
                        .padding(6.dp)
                )
            }
        }

        Spacer(Modifier.height(10.dp))
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            when {
                live.spam -> Badge("SPAM", red = true)
                live.isContact -> Badge("CONTACT")
                else -> Badge("UNKNOWN")
            }
            if (live.location.isNotEmpty()) Badge(live.location)
            if (live.spamReports > 0) Badge("${live.spamReports} reports", red = live.spam)
            if (live.blocked) Badge("BLOCKED", red = true)
        }

        Spacer(Modifier.height(10.dp))
        val reasonLine = when {
            live.spam -> "🚩 ${live.spamReason}"
            live.spamReports in 1 until com.craftcaller.app.data.SpamThreshold.COUNT ->
                "⚠️ ${live.spamReports} users flagged this number"
            live.name.isNotEmpty() -> "✓ Known: ${live.location.ifEmpty { "verified context" }}"
            else -> "ℹ️ Unknown number — not in your contacts or the community DB yet"
        }
        Text(
            reasonLine,
            fontSize = 13.sp,
            color = if (live.spam) CraftPalette.Spam else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.72f)
        )

        Spacer(Modifier.height(14.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(
                onClick = {
                    bump {
                        Repo.reportSpam(live.number, "Likely fraud")
                        live = Repo.lookup(live.number)
                    }
                },
                modifier = Modifier.weight(1f)
            ) { Text("⚠ Report spam") }
            Button(
                onClick = {
                    bump {
                        val b = live.blocked
                        if (b) Repo.unblock(live.number) else Repo.block(live.number)
                        live = Repo.lookup(live.number)
                    }
                },
                colors = if (live.blocked)
                    androidx.compose.material3.ButtonDefaults.buttonColors()
                else
                    androidx.compose.material3.ButtonDefaults.buttonColors(
                        containerColor = CraftPalette.Spam
                    ),
                modifier = Modifier.weight(1f)
            ) { Text(if (live.blocked) "✓ Unblock" else "🚫 Block") }
        }
        Spacer(Modifier.height(8.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(
                onClick = {
                    try {
                        ctx.startActivity(Intent(Intent.ACTION_DIAL, Uri.parse("tel:${live.number}")))
                    } catch (_: Throwable) {
                    }
                },
                modifier = Modifier.weight(1f)
            ) { Text("📞 Call") }
            OutlinedButton(
                onClick = {
                    try {
                        ctx.startActivity(Intent(Intent.ACTION_SENDTO, Uri.parse("smsto:${live.number}")))
                    } catch (_: Throwable) {
                    }
                },
                modifier = Modifier.weight(1f)
            ) { Text("✉️ SMS") }
            OutlinedButton(
                onClick = {
                    try {
                        Intent(Intent.ACTION_INSERT)
                            .setData(ContactsContract.Contacts.CONTENT_URI)
                            .putExtra(ContactsContract.Intents.Insert.PHONE, live.number)
                            .let { ctx.startActivity(it) }
                    } catch (_: Throwable) {
                    }
                },
                modifier = Modifier.weight(1f)
            ) { Text("＋Contact") }
        }
    }
}
