package com.craftcaller.app.ui.screens

import com.craftcaller.app.ui.CraftPalette
import androidx.compose.foundation.background
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
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

import com.craftcaller.app.data.CraftState
import com.craftcaller.app.data.Repo
import com.craftcaller.app.data.SmsEntry
import com.craftcaller.app.data.readSystemSms
import com.craftcaller.app.ui.Tab
import com.craftcaller.app.ui.common.Avatar
import com.craftcaller.app.ui.common.Badge
import com.craftcaller.app.ui.common.EmptyHint
import com.craftcaller.app.ui.common.ScreenScaffold
import com.craftcaller.app.util.CallerHub
import com.craftcaller.app.util.PhoneUtils
import com.craftcaller.app.util.TimeAgo
import com.craftcaller.app.util.Cctx
import kotlinx.coroutines.launch
import androidx.compose.runtime.rememberCoroutineScope

/** Smart SMS tab: inbox + OTP detection + spam/phishing flags. */
@Composable
fun MessagesScreen(state: CraftState) {
    ScreenScaffold(Tab.Messages, extra = {
        val scope = rememberCoroutineScope()
        OutlinedButton(
            onClick = {
                scope.launch {
                    readSystemSms(Cctx.app).forEach { Db_putSms(it) }
                    CallerHub.forceRefresh()
                }
            },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp)
        ) {
            Text("↺ Load SMS from the phone inbox", fontSize = 14.sp)
        }
    }) {
        Column(Modifier.fillMaxSize()) {
            val sms = state.sms
            if (sms.isEmpty()) {
                EmptyHint("Inbox empty. Grant SMS permission during setup to read OTPs & spam texts.")
            } else {
                LazyColumn {
                    items(sms, key = { it.id.toString() + it.ts }) { s ->
                        SmsRow(s)
                    }
                }
            }
        }
    }
}

private fun Db_putSms(e: SmsEntry) = com.craftcaller.app.data.Db.putSms(e)

@Composable
private fun SmsRow(s: SmsEntry) {
    Box(
        Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp)
            .background(
                MaterialTheme.colorScheme.surface,
                RoundedCornerShape(14.dp)
            )
            .padding(12.dp)
    ) {
        Column {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Avatar(s.name.takeIf { it.isNotEmpty() } ?: s.number, s.spam, 34)
                Spacer(Modifier.width(8.dp))
                Text(
                    s.name.takeIf { it.isNotEmpty() } ?: PhoneUtils.emask(s.number),
                    fontWeight = FontWeight.SemiBold, fontSize = 14.sp
                )
                Spacer(Modifier.width(6.dp))
                if (s.otp) Badge("OTP")
                if (s.spam) Badge("SPAM", red = true)
                Spacer(Modifier.width(4.dp))
                Text(
                    TimeAgo.fmt(s.ts),
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                )
            }
            Spacer(Modifier.padding(3.dp))
            Text(
                s.body,
                fontSize = 13.sp,
                color = when {
                    s.spam -> CraftPalette.Spam
                    else -> MaterialTheme.colorScheme.onSurface.copy(alpha = 0.85f)
                },
                maxLines = 3,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}
