package com.craftcaller.app.ui.screens

import com.craftcaller.app.ui.CraftPalette
import android.app.role.RoleManager
import android.content.Context
import android.content.Intent
import android.os.Build
import android.provider.Settings
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

import com.craftcaller.app.data.CraftState
import com.craftcaller.app.data.Prefs
import com.craftcaller.app.data.Repo
import com.craftcaller.app.data.SpamThreshold
import com.craftcaller.app.ui.Tab
import com.craftcaller.app.ui.common.Avatar
import com.craftcaller.app.ui.common.Badge
import com.craftcaller.app.ui.common.EmptyHint
import com.craftcaller.app.ui.common.ScreenScaffold
import com.craftcaller.app.util.CallerHub
import com.craftcaller.app.util.PhoneUtils
import com.craftcaller.app.util.TimeAgo
import kotlinx.coroutines.launch
import androidx.compose.runtime.rememberCoroutineScope

/** Block tab: blocklist, auto-block policy and the system screening role. */
@Composable
fun BlockScreen(state: CraftState) {
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()
    ScreenScaffold(Tab.Block) {
        Column(Modifier.fillMaxSize()) {
            var autoBlock by remember { mutableStateOf(Prefs.autoBlockSpam) }
            Row(
                Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(16.dp))
                    .padding(14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(Modifier.weight(1f)) {
                    Text(
                        "Auto-block top spammers",
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 15.sp
                    )
                    Text(
                        "Silently reject calls the community flagged ${SpamThreshold.COUNT}+ times",
                        fontSize = 12.5.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                }
                Switch(
                    checked = autoBlock,
                    onCheckedChange = {
                        autoBlock = it
                        Prefs.autoBlockSpam = it
                    }
                )
            }
            Spacer(Modifier.height(10.dp))

            OutlinedButton(
                onClick = { askScreeningRole(ctx) },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("Make CraftCaller your default Caller ID & spam app", fontSize = 13.sp)
            }
            Spacer(Modifier.height(10.dp))

            OutlinedButton(
                onClick = { openOverlaySettings(ctx) },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("Enable the floating caller popup (overlay)", fontSize = 13.sp)
            }

            Spacer(Modifier.height(14.dp))
            Text(
                "Blocked numbers (${state.blocklist.size})",
                fontWeight = FontWeight.SemiBold,
                fontSize = 14.sp
            )
            if (state.blocklist.isEmpty()) {
                EmptyHint("Nothing blocked yet")
            } else {
                LazyColumn {
                    items(state.blocklist, key = { it.number }) { b ->
                        Row(
                            Modifier
                                .fillMaxWidth()
                                .padding(vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Avatar(b.number, spam = true, 38)
                            Spacer(Modifier.width(10.dp))
                            Column(Modifier.weight(1f)) {
                                Text(
                                    PhoneUtils.emask(b.number),
                                    fontWeight = FontWeight.Medium, fontSize = 14.sp
                                )
                                Text(
                                    "Blocked " + TimeAgo.fmt(b.ts),
                                    fontSize = 11.5.sp,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.55f)
                                )
                            }
                            OutlinedButton(
                                onClick = {
                                    scope.launch {
                                        Repo.unblock(b.number)
                                        CallerHub.forceRefresh()
                                    }
                                }
                            ) { Text("✕ Unblock", fontSize = 12.sp) }
                        }
                    }
                }
            }

            Spacer(Modifier.height(14.dp))
            Text(
                "Community top spammers",
                fontWeight = FontWeight.SemiBold,
                fontSize = 14.sp
            )
            if (state.topSpam.isEmpty()) {
                EmptyHint("No spam reported yet")
            } else {
                LazyColumn {
                    items(state.topSpam.take(20), key = { it.number }) { s ->
                        Row(
                            Modifier
                                .fillMaxWidth()
                                .padding(vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Avatar(s.number, spam = true, 38)
                            Spacer(Modifier.width(10.dp))
                            Column(Modifier.weight(1f)) {
                                Text(
                                    PhoneUtils.emask(s.number),
                                    fontWeight = FontWeight.Medium, fontSize = 14.sp,
                                    color = CraftPalette.Spam
                                )
                                Text(
                                    s.topReason.ifEmpty { SpamThreshold.REASON } + " · ${s.reports} reports",
                                    fontSize = 11.5.sp,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.55f)
                                )
                            }
                            Badge("SPAM", red = true)
                            Spacer(Modifier.width(8.dp))
                            OutlinedButton(
                                onClick = {
                                    scope.launch {
                                        Repo.block(s.number)
                                        CallerHub.forceRefresh()
                                    }
                                }
                            ) { Text("Block", fontSize = 12.sp) }
                        }
                    }
                }
            }
        }
    }
}

private fun askScreeningRole(ctx: Context) {
    try {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val rm = ctx.getSystemService(Context.ROLE_SERVICE) as RoleManager
            if (rm.isRoleAvailable(RoleManager.ROLE_CALL_SCREENING) &&
                !rm.isRoleHeld(RoleManager.ROLE_CALL_SCREENING)
            ) {
                ctx.startActivity(
                    rm.createRequestRoleIntent(RoleManager.ROLE_CALL_SCREENING)
                )
            }
        }
    } catch (_: Throwable) {
    }
}

private fun openOverlaySettings(ctx: Context) {
    try {
        ctx.startActivity(
            Intent(
                Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                android.net.Uri.parse("package:" + ctx.packageName)
            ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        )
    } catch (_: Throwable) {
    }
}
