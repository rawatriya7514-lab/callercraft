package com.craftcaller.app.settings

import android.Manifest
import android.app.role.RoleManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.ScreenRotation
import androidx.compose.material.icons.filled.Sms
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.craftcaller.app.ui.theme.AmberWarn
import com.craftcaller.app.ui.theme.Blue
import com.craftcaller.app.ui.theme.GreenOk
import com.craftcaller.app.ui.theme.TextMuted
import com.craftcaller.app.util.Prefs
import com.craftcaller.app.util.RequiredPermissions

/**
 * Permissions & Android-role center. All statuses are LIVE checks; every
 * button fires the real system request (no fake success).
 */
@Composable
fun PermissionsScreen(onClose: () -> Unit) {
    val context = LocalContext.current
    val prefs = remember { Prefs(context) }

    var callerIdState by remember {
        mutableStateOf(RequiredPermissions.allGranted(context, RequiredPermissions.CALLER_ID))
    }
    var smsState by remember {
        mutableStateOf(RequiredPermissions.allGranted(context, RequiredPermissions.SMS))
    }
    var micState by remember {
        mutableStateOf(RequiredPermissions.granted(context, Manifest.permission.RECORD_AUDIO))
    }
    var notificationsState by remember {
        mutableStateOf(
            RequiredPermissions.NOTIFICATIONS.isEmpty() ||
                RequiredPermissions.granted(context, Manifest.permission.POST_NOTIFICATIONS)
        )
    }
    var roleHeld by remember { mutableStateOf(isCallScreeningRoleHeld(context)) }

    val callerIdLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { callerIdState = RequiredPermissions.allGranted(context, RequiredPermissions.CALLER_ID) }
    val smsLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { smsState = RequiredPermissions.allGranted(context, RequiredPermissions.SMS) }
    val micLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { micState = it }
    val notificationsLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { notificationsState = it }
    val roleLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { roleHeld = isCallScreeningRoleHeld(context) }

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
                Text("Permissions & role", fontWeight = FontWeight.ExtraBold, fontSize = 17.sp)
                Text("Live system state — requesting always opens the real dialog", fontSize = 11.sp, color = TextMuted)
            }
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp)
        ) {
            // ---- Selected Android role (explicit, per requirement) ----------
            PermRow(
                icon = Icons.Filled.ScreenRotation,
                title = "Selected role: Call-screening app",
                detail = if (roleHeld)
                    "Held — CraftCaller screens calls and can auto-reject"
                else
                    "Not held — announcements still work via PHONE_STATE; system auto-reject needs this role",
                granted = roleHeld,
                actionLabel = if (roleHeld) "" else "Request role",
                onAction = {
                    if (!roleHeld) {
                        roleIntent(context)?.let { roleLauncher.launch(it) }
                    }
                }
            )
            Text(
                if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q)
                    "Android version is below 10 — the system call-screening role does not exist on this device."
                else "The role dialog is Android's — approving or denying is always your decision.",
                fontSize = 11.sp, color = TextMuted, modifier = Modifier.padding(start = 4.dp, top = 2.dp)
            )

            Spacer(Modifier.height(18.dp))
            PermRow(
                icon = Icons.Filled.Call,
                title = "Caller-ID core (phone, call log, contacts, answer)",
                detail = if (callerIdState)
                    "Granted — contact names and call metadata can be resolved on-device"
                else "Needed for identifying who's calling and reading the call log",
                granted = callerIdState,
                actionLabel = if (callerIdState) "" else "Grant",
                onAction = { callerIdLauncher.launch(RequiredPermissions.CALLER_ID) }
            )
            PermRow(
                icon = Icons.Filled.Email,
                title = "SMS (read + receive)",
                detail = if (smsState)
                    "Granted — the Messages tab can show live conversations"
                else "Optional — only the Messages tab needs it",
                granted = smsState,
                actionLabel = if (smsState) "" else "Grant",
                onAction = { smsLauncher.launch(RequiredPermissions.SMS) }
            )
            PermRow(
                icon = Icons.Filled.Mic,
                title = "Microphone",
                detail = if (micState)
                    "Granted — the Block tab can record incoming calls (foreground service)"
                else "Optional — used only by call recording",
                granted = micState,
                actionLabel = if (micState) "" else "Grant",
                onAction = { micLauncher.launch(Manifest.permission.RECORD_AUDIO) }
            )
            if (RequiredPermissions.NOTIFICATIONS.isNotEmpty()) {
                PermRow(
                    icon = Icons.Filled.Notifications,
                    title = "Notifications",
                    detail = if (notificationsState)
                        "Granted — screening and call-status notifiers work"
                    else "Optional — without it Android 13+ silences our notifications",
                    granted = notificationsState,
                    actionLabel = if (notificationsState) "" else "Grant",
                    onAction = { notificationsLauncher.launch(Manifest.permission.POST_NOTIFICATIONS) }
                )
            }

            Spacer(Modifier.height(18.dp))
            // Battery/background: honest device-specific affordance.
            PermRow(
                icon = Icons.Filled.Info,
                title = "Background protection",
                detail = "Device-specific: to keep screening alive, some phones require " +
                    "\" unrestricted battery\" for CraftCaller in system app info",
                granted = false,
                actionLabel = "Open app info",
                tintOnFail = Blue,
                onAction = {
                    runCatching {
                        context.startActivity(
                            Intent(
                                Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                                Uri.fromParts("package", context.packageName, null)
                            )
                        )
                    }
                }
            )

            Spacer(Modifier.height(18.dp))
            Text(
                "Why each permission exists: contacts = names for announcements & " +
                    "caller-ID (never uploaded without explicit switch); phone/call log = " +
                    "identify calls; mic = recording you start yourself; SMS = Messages tab; " +
                    "notifications = system screening notices. CraftCaller never asks for " +
                    "anything unrelated to these features.",
                fontSize = 11.sp,
                color = TextMuted
            )
            Spacer(Modifier.height(30.dp))
        }
    }
}

@Composable
private fun PermRow(
    icon: ImageVector,
    title: String,
    detail: String,
    granted: Boolean,
    actionLabel: String,
    onAction: () -> Unit,
    tintOnFail: Color = AmberWarn
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surface)
            .padding(14.dp)
    ) {
        Icon(
            if (granted) Icons.Filled.Verified else icon,
            null,
            tint = if (granted) GreenOk else tintOnFail,
            modifier = Modifier.size(22.dp)
        )
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(title, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
            Text(detail, fontSize = 11.sp, color = TextMuted, maxLines = 3)
        }
        if (actionLabel.isNotEmpty()) {
            Spacer(Modifier.width(8.dp))
            Button(
                onClick = onAction,
                colors = ButtonDefaults.buttonColors(containerColor = Blue),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.semantics { contentDescription = actionLabel }
            ) {
                Text(actionLabel, fontSize = 12.sp, color = Color.White)
            }
        }
    }
}

private fun isCallScreeningRoleHeld(context: Context): Boolean {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) return false
    val roleManager = context.getSystemService(RoleManager::class.java) ?: return false
    return runCatching {
        roleManager.isRoleHeld(RoleManager.ROLE_CALL_SCREENING)
    }.getOrDefault(false)
}

private fun roleIntent(context: Context): Intent? {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) return null
    val roleManager = context.getSystemService(RoleManager::class.java) ?: return null
    if (roleManager.isRoleAvailable(RoleManager.ROLE_CALL_SCREENING) &&
        !roleManager.isRoleHeld(RoleManager.ROLE_CALL_SCREENING)
    ) {
        return roleManager.createRequestRoleIntent(RoleManager.ROLE_CALL_SCREENING)
    }
    return null
}
