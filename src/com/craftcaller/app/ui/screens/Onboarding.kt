package com.craftcaller.app.ui.screens

import com.craftcaller.app.ui.CraftPalette
import android.Manifest
import android.content.Context
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.craftcaller.app.data.Auth

import com.craftcaller.app.data.Db
import com.craftcaller.app.data.Prefs
import com.craftcaller.app.data.Repo
import com.craftcaller.app.service.CallerIDService
import com.craftcaller.app.util.CallerHub
import com.craftcaller.app.util.Perms
import com.craftcaller.app.util.PhoneUtils
import kotlinx.coroutines.launch
import androidx.compose.runtime.rememberCoroutineScope

/**
 * Onboarding: the Truecaller-style login (phone → OTP → profile) with the
 * permission gates and the contact-sync consent baked in.
 */
@Composable
fun Onboarding(onDone: () -> Unit) {
    var step by remember { mutableStateOf(0) }
    when (step) {
        0 -> WelcomeStep { step = 1 }
        1 -> PermissionStep { step = 2 }
        2 -> AccountStep { step = 3 }
        else -> SyncStep(onDone)
    }
}

@Composable
private fun WelcomeStep(onNext: () -> Unit) {
    Column(
        Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            Modifier
                .fillMaxWidth()
                .height(150.dp)
                .background(
                    Brush.linearGradient(listOf(CraftPalette.Blue, CraftPalette.BlueDark)),
                    RoundedCornerShape(28.dp)
                ),
            contentAlignment = Alignment.Center
        ) {
            Text("C", color = androidx.compose.ui.graphics.Color.White, fontSize = 64.sp, fontWeight = FontWeight.Bold)
        }
        Spacer(Modifier.height(20.dp))
        Text("CraftCaller", style = MaterialTheme.typography.headlineSmall, color = CraftPalette.ink(Prefs.darkMode))
        Text(
            "Truecaller-style caller ID, spam defence & smart SMS — built from CraftTown with love.",
            fontSize = 14.sp,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
        )
        Spacer(Modifier.height(30.dp))
        PrimaryCta("Get started", onNext)
    }
}

@Composable
private fun PermissionStep(onNext: () -> Unit) {
    val ctx = LocalContext.current
    var phoneGranted by remember {
        mutableStateOf(Perms.allGranted(ctx, listOf(
            Manifest.permission.READ_PHONE_STATE,
            Manifest.permission.READ_CALL_LOG,
            Manifest.permission.READ_CONTACTS
        )))
    }
    var smsGranted by remember {
        mutableStateOf(Perms.allGranted(ctx, listOf(Manifest.permission.READ_SMS, Manifest.permission.RECEIVE_SMS)))
    }
    var notifGranted by remember {
        mutableStateOf(if (android.os.Build.VERSION.SDK_INT >= 33) {
            Perms.granted(ctx, Manifest.permission.POST_NOTIFICATIONS)
        } else true)
    }
    val launcher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { _ ->
        phoneGranted = Perms.allGranted(ctx, listOf(
            Manifest.permission.READ_PHONE_STATE,
            Manifest.permission.READ_CALL_LOG,
            Manifest.permission.READ_CONTACTS
        ))
        smsGranted = Perms.allGranted(ctx, listOf(
            Manifest.permission.READ_SMS, Manifest.permission.RECEIVE_SMS
        ))
        notifGranted = if (android.os.Build.VERSION.SDK_INT >= 33) {
            Perms.granted(ctx, Manifest.permission.POST_NOTIFICATIONS)
        } else true
    }

    fun requestAll(perms: List<String>) = launcher.launch(perms.toTypedArray())

    Column(
        Modifier
            .fillMaxSize()
            .padding(22.dp)
            .verticalScroll(rememberScrollState())
    ) {
        Text("Permissions", style = MaterialTheme.typography.titleLarge)
        Text(
            "CraftCaller inspects the phone state locally — no call audio and no contact payloads ever leave your device.",
            fontSize = 13.sp,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
        )
        Spacer(Modifier.height(16.dp))
        PermissionRow("📞", "Phone & call log", "Retail caller ID, popup previews + spam blocking", phoneGranted) {
            requestAll(listOf(
                Manifest.permission.READ_PHONE_STATE,
                Manifest.permission.READ_CALL_LOG,
                Manifest.permission.READ_CONTACTS
            ))
        }
        PermissionRow("✉️", "SMS (smart OTP & spam texts)", "Read SMS inbox to flag scam / OTP texts locally", smsGranted) {
            requestAll(listOf(Manifest.permission.READ_SMS, Manifest.permission.RECEIVE_SMS))
        }
        PermissionRow("🔔", "Notifications", "Caller popup + alerts on Android 13+", notifGranted) {
            if (android.os.Build.VERSION.SDK_INT >= 33) {
                requestAll(listOf(Manifest.permission.POST_NOTIFICATIONS))
            } else notifGranted = true
        }
        PermissionRow("🎤", "Microphone (call recording)", "Optional, rocks for call recorder", Perms.granted(ctx, Manifest.permission.RECORD_AUDIO)) {
            requestAll(listOf(Manifest.permission.RECORD_AUDIO))
        }
        Spacer(Modifier.height(24.dp))
        PrimaryCta("Continue", onNext, enabled = phoneGranted)
        if (!phoneGranted) {
            Text(
                "Phone & call-log permission is required for caller ID to work.",
                fontSize = 11.5.sp,
                color = CraftPalette.Spam
            )
        }
    }
}

@Composable
private fun PermissionRow(
    glyph: String,
    title: String,
    subtitle: String,
    granted: Boolean,
    onGrant: () -> Unit
) {
    Row(
        Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(14.dp))
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(glyph, fontSize = 22.sp)
        Spacer(Modifier.width(10.dp))
        Column(Modifier.weight(1f)) {
            Text(title, fontWeight = FontWeight.SemiBold, fontSize = 14.5.sp)
            Text(subtitle, fontSize = 11.5.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
        }
        OutlinedButton(onClick = { if (!granted) onGrant() }, enabled = !granted) {
            Text(if (granted) "✓" else "Grant", fontSize = 12.sp)
        }
    }
}

@Composable
private fun AccountStep(onNext: () -> Unit) {
    var phone by remember { mutableStateOf(Prefs.phoneNumber ?: "") }
    var otpPage by remember { mutableStateOf(false) }
    var otp by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }
    // 6-digit code behavior: request -> (Firebase if configured; else local 123456)
    Column(
        Modifier
            .fillMaxSize()
            .padding(22.dp)
    ) {
        Text("Verify your number", style = MaterialTheme.typography.titleLarge)
        Text(
            "Truecaller-style: your number becomes your CraftCaller identity — plus it powers the community DB.",
            fontSize = 13.sp,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
        )
        Spacer(Modifier.height(18.dp))
        if (!otpPage) {
            OutlinedTextField(
                value = phone,
                onValueChange = { phone = it },
                singleLine = true,
                label = { Text("Phone number with country code") },
                placeholder = { Text("+91 90000 00000") },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            )
            Spacer(Modifier.height(12.dp))
            PrimaryCta("Send verification code", {
                if (!PhoneUtils.isValidNumber(phone)) {
                    error = "Enter a valid number with country code"
                } else {
                    val code = "123456"
                    Auth.savePendingCode(PhoneUtils.normalize(phone), code)
                    otpPage = true
                    error = "Demo verification: use code $code (attach firebase_config.json for live OTP)"
                }
            })
        } else {
            OutlinedTextField(
                value = otp,
                onValueChange = { otp = it.take(6) },
                singleLine = true,
                label = { Text("6-digit code") },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            )
            Spacer(Modifier.height(12.dp))
            PrimaryCta("Verify", {
                val code = Prefs.pendingCode ?: ""
                if (otp == code) {
                    Prefs.phoneNumber = PhoneUtils.normalize(phone)
                    Prefs.who = phone
                    onNext()
                } else {
                    error = "Wrong code"
                }
            })
        }
        if (error != null) {
            Spacer(Modifier.height(10.dp))
            Text(error!!, fontSize = 12.sp, color = CraftPalette.Spam)
        }
    }
}

@Composable
private fun SyncStep(onDone: () -> Unit) {
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()
    var name by remember { mutableStateOf(Prefs.who ?: "") }
    var email by remember { mutableStateOf(Prefs.email ?: "") }
    var syncContacts by remember { mutableStateOf(true) }
    var working by remember { mutableStateOf(false) }

    Column(
        Modifier
            .fillMaxSize()
            .padding(22.dp)
            .verticalScroll(rememberScrollState())
    ) {
        Text("Finish profile", style = MaterialTheme.typography.titleLarge)
        Spacer(Modifier.height(10.dp))
        OutlinedTextField(
            value = name, onValueChange = { name = it },
            singleLine = true, label = { Text("Your name") },
            modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp)
        )
        Spacer(Modifier.height(8.dp))
        OutlinedTextField(
            value = email, onValueChange = { email = it },
            singleLine = true, label = { Text("Email (optional)") },
            modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp)
        )
        Spacer(Modifier.height(16.dp))
        Row(
            Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(14.dp))
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(Modifier.weight(1f)) {
                Text("Upload your contacts", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                Text(
                    "Share name+number pairs with the community DB (Truecaller behaviour). Storage stays local otherwise.",
                    fontSize = 11.5.sp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )
            }
            androidx.compose.material3.Switch(
                checked = syncContacts,
                onCheckedChange = { syncContacts = it }
            )
        }
        Spacer(Modifier.height(20.dp))
        Button(
            onClick = {
                working = true
                Prefs.who = name.ifEmpty { "Friend" }
                Prefs.email = email.ifEmpty { "" }
                Prefs.onboarded = true
                scope.launch {
                    if (syncContacts) runCatching { Repo.syncContacts() }
                    runCatching {
                        val phone = Prefs.phoneNumber
                        if (phone != null) {
                            Repo.addManualNumber(phone, name.ifEmpty { "Me" })
                        }
                    }
                    CallerIDService.start(ctx)
                    CallerHub.forceRefresh()
                    working = false
                    onDone()
                }
            },
            enabled = !working && name.isNotEmpty(),
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp),
            shape = RoundedCornerShape(14.dp),
            colors = androidx.compose.material3.ButtonDefaults.buttonColors(containerColor = CraftPalette.Blue)
        ) { Text(if (working) "Syncing…" else "Start CraftCaller", fontSize = 15.sp) }
        Spacer(Modifier.height(10.dp))
        Text(
            "Your identity is stored in the directory as: \"$name\" — change it any time from Settings.",
            fontSize = 11.sp,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.55f)
        )
    }
}

@Composable
private fun PrimaryCta(label: String, onClick: () -> Unit, enabled: Boolean = true) {
    Button(
        onClick = onClick,
        enabled = enabled,
        modifier = Modifier
            .fillMaxWidth()
            .height(50.dp),
        shape = RoundedCornerShape(14.dp),
        colors = androidx.compose.material3.ButtonDefaults.buttonColors(containerColor = CraftPalette.Blue)
    ) {
        Text(label, fontSize = 15.sp, color = androidx.compose.ui.graphics.Color.White)
    }
}
