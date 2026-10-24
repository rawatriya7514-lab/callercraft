package com.craftcaller.app.onboarding

import android.Manifest
import android.app.role.RoleManager
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.craftcaller.app.service.CraftCallScreeningService
import com.craftcaller.app.ui.theme.AmberWarn
import com.craftcaller.app.ui.theme.GreenOk
import com.craftcaller.app.ui.theme.TextMuted

/**
 * Live "call permissions" card used in onboarding's final step and in the
 * Settings profile editor. Shows the REAL state of every permission the call
 * announcer needs, and requests each one — always behind an explanation
 * dialog, never silently (system role dialog is the system's; we never assume
 * approval or鹤 read its state ahead of time).
 */
@Composable
fun CallSetupCard() {
    val context = LocalContext.current
    var refresh by remember { mutableIntStateOf(0) }
    var explainFor by remember { mutableStateOf<Capability?>(null) }

    fun granted(permission: String) =
        ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED

    val roleLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { refresh++ }
    val phoneLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { refresh++ }
    val contactsLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { refresh++ }

    fun requestCapability(cap: Capability) {
        when (cap) {
            Capability.ROLE -> {
                if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) return
                val manager = runCatching {
                    context.getSystemService(RoleManager::class.java)
                }.getOrNull() ?: return
                val available = runCatching { manager.isRoleAvailable(RoleManager.ROLE_CALL_SCREENING) }
                    .getOrDefault(false)
                if (!available) return // nothing to request; UI shows Not available
                runCatching {
                    roleLauncher.launch(
                        manager.createRequestRoleIntent(RoleManager.ROLE_CALL_SCREENING)
                    )
                }
            }
            Capability.PHONE ->
                phoneLauncher.launch(Manifest.permission.READ_PHONE_STATE)
            Capability.CONTACTS ->
                contactsLauncher.launch(Manifest.permission.READ_CONTACTS)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(MaterialTheme.colorScheme.surface)
            .padding(16.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Filled.Call, null, tint = com.craftcaller.app.ui.theme.Blue, modifier = Modifier.width(22.dp))
            Spacer(Modifier.width(10.dp))
            Text("Call permissions", fontWeight = FontWeight.Bold, fontSize = 15.sp)
        }

        Spacer(Modifier.height(10.dp))
        CapRow(
            title = "Call screening role",
            subtitle = "Instant ring-time detection via Telecom",
            state = when {
                Build.VERSION.SDK_INT < Build.VERSION_CODES.Q -> CapState.UNAVAILABLE
                CraftCallScreeningService.isRoleHeld(context) -> CapState.GRANTED
                roleAvailable(context) -> CapState.PENDING
                else -> CapState.UNAVAILABLE
            },
            onGrant = { explainFor = Capability.ROLE }
        )
        CapRow(
            title = "Phone state",
            subtitle = "Detect ringing without the role",
            state = if (granted(Manifest.permission.READ_PHONE_STATE)) CapState.GRANTED else CapState.PENDING,
            onGrant = { explainFor = Capability.PHONE }
        )
        CapRow(
            title = "Contact names",
            subtitle = "Announce \u201CRahul Sharma\u201D, not a number",
            state = if (granted(Manifest.permission.READ_CONTACTS)) CapState.GRANTED else CapState.PENDING,
            onGrant = { explainFor = Capability.CONTACTS }
        )
    }

    explainFor?.let { cap ->
        AlertDialog(
            onDismissRequest = { explainFor = null },
            icon = { Icon(Icons.Filled.Info, null, tint = AmberWarn) },
            title = { Text(explainTitle(cap), fontWeight = FontWeight.Bold) },
            text = { Text(explainBody(cap), fontSize = 14.sp) },
            confirmButton = {
                TextButton(
                    onClick = {
                        explainFor = null
                        requestCapability(cap)
                    },
                    modifier = Modifier.semantics { contentDescription = "Continue and grant ${explainTitle(cap)}" }
                ) { Text("Continue", color = Color.White) }
            },
            dismissButton = {
                TextButton(onClick = { explainFor = null }) { Text("Not now", color = TextMuted) }
            }
        )
    }
}

private enum class Capability { ROLE, PHONE, CONTACTS }

private enum class CapState { GRANTED, PENDING, UNAVAILABLE }

private fun roleAvailable(context: android.content.Context): Boolean =
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) false
    else runCatching {
        context.getSystemService(RoleManager::class.java)
            ?.isRoleAvailable(RoleManager.ROLE_CALL_SCREENING) == true
    }.getOrDefault(false)

@Composable
private fun CapRow(title: String, subtitle: String, state: CapState, onGrant: () -> Unit) {
    val (dotText, dotColor) = when (state) {
        CapState.GRANTED -> "✓" to GreenOk
        CapState.PENDING -> "!" to AmberWarn
        CapState.UNAVAILABLE -> "–" to TextMuted
    }
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp)
    ) {
        Text(
            dotText,
            color = Color.White,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
            modifier = Modifier
                .width(26.dp)
                .height(26.dp)
                .clip(RoundedCornerShape(13.dp))
                .background(dotColor)
        )
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(title, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
            Text(subtitle, fontSize = 11.sp, color = TextMuted)
        }
        LabelatedGrant(state, onGrant)
    }
}

@Composable
private fun LabelatedGrant(state: CapState, onGrant: () -> Unit) {
    when (state) {
        CapState.GRANTED -> Text("granted", fontSize = 12.sp, color = GreenOk)
        CapState.PENDING -> TextButton(
            onClick = onGrant,
            modifier = Modifier.semantics { contentDescription = "Grant permission" }
        ) { Text("Grant", color = com.craftcaller.app.ui.theme.Blue) }
        CapState.UNAVAILABLE -> Text("not available", fontSize = 12.sp, color = TextMuted)
    }
}

private fun explainTitle(cap: Capability): String = when (cap) {
    Capability.ROLE -> "Set CraftCaller as Call screening"
    Capability.PHONE -> "Allow phone state access"
    Capability.CONTACTS -> "Allow contact access"
}

private fun explainBody(cap: Capability): String = when (cap) {
    Capability.ROLE ->
        "CraftCaller will screen incoming calls directly through Android's Telecom " +
            "system — this is how your name is announced at the first ring and spam is " +
            "rejected automatically. Only one app on this phone can hold this role; you " +
            "can switch it off or move it back to your dialer anytime. No call audio is accessed."
    Capability.PHONE ->
        "Without the screening role, CraftCaller detects incoming calls through the " +
            "phone state so it can still announce the caller. Used only for caller " +
            "identification; nothing is recorded."
    Capability.CONTACTS ->
        "Read saved contact names so the announcer says \u201CRahul Sharma\u201D instead " +
            "of reading out the number. Names stay on this phone and never leave it; " +
            "you can switch this off in announcements settings."
}
