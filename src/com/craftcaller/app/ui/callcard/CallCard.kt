package com.craftcaller.app.ui.callcard

import com.craftcaller.app.ui.CraftPalette
import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.craftcaller.app.CallCardActivity
import com.craftcaller.app.data.CallerInfo

import com.craftcaller.app.data.Repo
import com.craftcaller.app.ui.common.Avatar
import com.craftcaller.app.ui.common.Badge
import com.craftcaller.app.ui.screens.NumberDetailSheet
import com.craftcaller.app.util.CallerHub
import com.craftcaller.app.util.PhoneUtils

/**
 * CraftCaller's ring screen / after-call sheet, rendered inside
 * [com.craftcaller.app.CallCardActivity] (showWhenLocked + turnScreenOn).
 */
@Composable
fun CallCard(number: String, mode: String, hintSpam: Boolean, onClose: () -> Unit) {
    val ringing = mode != CallCardActivity.MODE_AFTER
    val ctx = LocalContext.current
    var info by remember { mutableStateOf(CallerHub.get(number) ?: CallerInfo(number = number)) }
    var refreshed by remember { mutableStateOf(false) }

    LaunchedEffect(number) {
        CallerHub.get(number)?.let {
            if (it.name.isNotEmpty() || it.spam || it.location.isNotEmpty()) info = it
        }
        val resolved = runCatching { Repo.lookup(number) }.getOrNull()
        if (resolved != null) {
            info = resolved
            CallerHub.put(number, resolved)
        }
        refreshed = true
    }

    val spam = info.spam
    val accent = if (spam) CraftPalette.Spam else CraftPalette.Blue
    val deep = if (spam) Color(0xFF6B0F0F) else Color(0xFF0A3D75)

    Column(
        Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(listOf(deep.copy(alpha = 0.94f), Color(0xFF0B0E13)))
            )
            .padding(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(Modifier.height(24.dp))
        Badge(
            when {
                ringing && spam -> "SPAM CALL"
                ringing -> "INCOMING CALL"
                else -> "CALL ENDED"
            },
            red = spam
        )
        Spacer(Modifier.height(26.dp))
        Avatar(info.displayName, spam, 96)
        Spacer(Modifier.height(16.dp))
        Text(
            info.displayName,
            color = if (spam) Color(0xFFFFD1CE) else Color.White,
            fontSize = 30.sp,
            fontWeight = FontWeight.SemiBold,
            textAlign = TextAlign.Center
        )
        Text(
            PhoneUtils.emask(info.number.ifEmpty { "unknown" }),
            color = Color(0xB3FFFFFF),
            fontSize = 16.sp
        )
        if (info.location.isNotEmpty()) {
            Spacer(Modifier.height(4.dp))
            Text("📍 ${info.location}", color = Color(0x99FFFFFF), fontSize = 13.sp)
        }
        if (spam) {
            Spacer(Modifier.height(10.dp))
            Text(
                "🚩 ${info.spamReason}",
                color = Color(0xFFFFB4AB),
                textAlign = TextAlign.Center,
                fontSize = 14.sp
            )
        }

        Spacer(Modifier.weight(1f))
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            OutlinedButton(
                onClick = { onClose() },
                modifier = Modifier.weight(1f)
            ) { Text("Dismiss", color = Color.White) }
            Button(
                onClick = { onClose() },
                colors = ButtonDefaults.buttonColors(containerColor = accent),
                modifier = Modifier.weight(1f)
            ) { Text(if (ringing) "Answer on phone" else "Open dashboard", color = Color.White) }
        }

        Spacer(Modifier.height(10.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            OutlinedButton(
                onClick = {
                    try {
                        val tm = ctx.getSystemService(Context.TELECOM_SERVICE) as android.telecom.TelecomManager
                        tm.endCall()
                    } catch (_: Throwable) {
                    }
                    onClose()
                },
                modifier = Modifier.weight(1f)
            ) { Text(if (spam && ringing) "🚫 Block & end" else "End call", color = Color.White) }
            OutlinedButton(
                onClick = { onClose() },
                modifier = Modifier.weight(1f)
            ) { Text("Close card", color = Color.White) }
        }

        Spacer(Modifier.height(14.dp))
        if (refreshed) {
            NumberDetailSheet(
                info = info,
                mode = if (ringing) "ring" else "after",
                onClose = onClose
            )
        }
        Spacer(Modifier.height(8.dp))
    }
}
