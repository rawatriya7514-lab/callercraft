package com.craftcaller.app.ui.screens

import com.craftcaller.app.ui.CraftPalette
import androidx.compose.foundation.background
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
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
import com.craftcaller.app.ui.Tab
import com.craftcaller.app.ui.common.Avatar
import com.craftcaller.app.ui.common.Badge
import com.craftcaller.app.ui.common.EmptyHint
import com.craftcaller.app.ui.common.ScreenScaffold
import com.craftcaller.app.util.CallerHub
import com.craftcaller.app.util.PhoneUtils
import kotlinx.coroutines.launch
import androidx.compose.runtime.rememberCoroutineScope

/** Contacts tab with CraftCaller badges (friend / spam / unknown). */
@Composable
fun ContactsScreen(state: CraftState) {
    ScreenScaffold(Tab.Contacts, extra = {
        val scope = rememberCoroutineScope()
        OutlinedButton(
            onClick = {
                scope.launch {
                    Repo.syncContacts()
                    CallerHub.forceRefresh()
                }
            },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp)
        ) {
            Text("↺ Sync contacts with CraftCaller", fontSize = 14.sp)
        }
    }) {
        val contacts = state.contacts
        Column(Modifier.fillMaxSize()) {
            if (contacts.isEmpty()) {
                EmptyHint("No contacts synced yet — tap sync above")
            } else {
                LazyColumn(Modifier.fillMaxSize()) {
                    items(contacts, key = { it.number }) { c ->
                        Row(
                            Modifier
                                .fillMaxWidth()
                                .background(MaterialTheme.colorScheme.background)
                                .padding(vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Avatar(c.name, c.spam)
                            Spacer(Modifier.width(10.dp))
                            Column(Modifier.weight(1f)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        c.name.ifEmpty { PhoneUtils.emask(c.number) },
                                        fontWeight = FontWeight.SemiBold,
                                        fontSize = 15.sp,
                                        maxLines = 1, overflow = TextOverflow.Ellipsis
                                    )
                                    if (c.badge.isNotEmpty()) {
                                        Spacer(Modifier.width(6.dp))
                                        Badge(c.badge, red = c.spam)
                                    }
                                }
                                Text(
                                    PhoneUtils.emask(c.number),
                                    fontSize = 12.5.sp,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.55f)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
