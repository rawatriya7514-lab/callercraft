package com.craftcaller.app.ui.screens

import com.craftcaller.app.ui.CraftPalette
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.craftcaller.app.data.CallEntry
import com.craftcaller.app.data.CallerInfo

import com.craftcaller.app.data.CraftState
import com.craftcaller.app.data.Repo
import com.craftcaller.app.ui.Tab
import com.craftcaller.app.ui.common.Avatar
import com.craftcaller.app.ui.common.Badge
import com.craftcaller.app.ui.common.ScreenScaffold
import com.craftcaller.app.ui.common.EmptyHint
import com.craftcaller.app.util.PhoneUtils
import com.craftcaller.app.util.TimeAgo

/** Calls tab: search bar + full call history with caller ID + spam marks. */
@Composable
fun CallsScreen(state: CraftState) {
    ScreenScaffold(Tab.Calls) {
        Column(Modifier.fillMaxSize()) {
            SearchAndHistory(state)
        }
    }
}

@Composable
private fun SearchAndHistory(state: CraftState) {
    var query by remember { mutableStateOf("") }
    val searching = query.isNotBlank()

    Column(Modifier.fillMaxSize()) {
        OutlinedTextField(
            value = query,
            onValueChange = { query = it },
            singleLine = true,
            shape = RoundedCornerShape(14.dp),
            placeholder = { Text("Search any number…", fontSize = 15.sp) },
            textStyle = MaterialTheme.typography.bodyLarge,
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = CraftPalette.Blue,
                cursorColor = CraftPalette.Blue
            ),
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(Modifier.size(10.dp))

        if (searching) {
            SearchResults(query, state)
        } else {
            LazyColumn(
                Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                items(state.calls, key = { it.ts.toString() + "#" + it.number }) { call ->
                    CallRow(call)
                }
                if (state.calls.isEmpty()) {
                    item { EmptyHint("No calls yet — incoming and outgoing calls will appear here with caller ID") }
                }
            }
        }
    }
}

@Composable
private fun SearchResults(query: String, state: CraftState) {
    var info by remember(query) { mutableStateOf<CallerInfo?>(null) }
    LaunchedEffect(query) {
        info = if (PhoneUtils.isValidNumber(query)) Repo.lookup(query) else null
    }
    val resolved = info
    if (resolved == null || !PhoneUtils.isValidNumber(query)) {
        EmptyHint("Enter a valid phone number to identify it")
        return
    }
    Row(
        Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(16.dp))
            .clickable { } // detail is presented below
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Avatar(resolved.displayName, resolved.spam)
        Spacer(Modifier.width(10.dp))
        Column(Modifier.weight(1f)) {
            Text(
                resolved.displayName,
                fontWeight = FontWeight.SemiBold,
                fontSize = 16.sp,
                color = if (resolved.spam) CraftPalette.Spam else MaterialTheme.colorScheme.onBackground
            )
            Text(
                PhoneUtils.emask(resolved.number),
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
            )
        }
        if (resolved.spam) Badge("SPAM", red = true)
        else Badge(resolved.source.uppercase())
    }
    Spacer(Modifier.size(8.dp))
    NumberDetailSheet(resolved)
}

@Composable
private fun CallRow(call: CallEntry) {
    val isMissed = call.type == 3
    val isOutgoing = call.type == 2
    Row(
        Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.background)
            .padding(vertical = 8.dp, horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        val title = when {
            call.name.isNotEmpty() -> call.name
            call.number.isNotEmpty() -> call.number
            else -> "Unknown"
        }
        Avatar(title, call.spam)
        Spacer(Modifier.size(10.dp))
        Column(Modifier.weight(1f)) {
            Text(
                title,
                fontWeight = FontWeight.SemiBold,
                fontSize = 15.5.sp,
                color = when {
                    call.spam -> CraftPalette.Spam
                    else -> MaterialTheme.colorScheme.onBackground
                },
                maxLines = 1, overflow = TextOverflow.Ellipsis
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                val dText = when {
                    isOutgoing -> "outgoing"
                    isMissed -> "missed"
                    else -> "incoming"
                }
                Text(
                    dText + "  ·  " + TimeAgo.fmt(call.ts),
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.55f)
                )
                if (call.spam) {
                    Spacer(Modifier.width(6.dp))
                    Badge("SPAM", red = true)
                }
            }
        }
        val ctx = LocalContext.current
        Text(
            "📞",
            fontSize = 18.sp,
            modifier = Modifier
                .clickable {
                    try {
                        ctx.startActivity(Intent(Intent.ACTION_DIAL, Uri.parse("tel:${call.number}")))
                    } catch (_: Throwable) { }
                }
                .padding(6.dp)
        )
    }
}
