package com.craftcaller.app.auth

import android.app.Activity
import android.app.Application
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.craftcaller.app.ui.common.rememberVm
import com.craftcaller.app.ui.theme.Blue
import com.craftcaller.app.ui.theme.RedSpam
import com.craftcaller.app.ui.theme.TextMuted

@Composable
fun PhoneLoginScreen(
    onOtpSent: (String) -> Unit,
    onOfflineContinue: () -> Unit
) {
    val context = LocalContext.current
    val activity = context as? Activity
    val vm: AuthViewModel = rememberVm { AuthViewModel(context.applicationContext as Application) }
    var raw by rememberSaveable { mutableStateOf("") }

    val phone = runCatching {
        if (raw.startsWith("+")) raw else "+${raw.filter { it.isDigit() }}"
    }.getOrDefault("")

    LaunchedEffect(vm.verifyId) {
        if (vm.verifyId != null && phone.isNotBlank()) onOtpSent(phone)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(rememberScrollState())
            .padding(28.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .size(72.dp)
                .clip(CircleShape)
                .background(Blue),
            contentAlignment = Alignment.Center
        ) {
            Text("C", color = Color.White, fontSize = 34.sp, fontWeight = FontWeight.ExtraBold)
        }
        Spacer(Modifier.height(16.dp))
        Text("Welcome to CraftCaller", fontSize = 24.sp, fontWeight = FontWeight.ExtraBold)
        Text(
            "Verify your number to join the community caller-ID network",
            fontSize = 13.sp,
            color = TextMuted,
            modifier = Modifier.padding(top = 6.dp, bottom = 26.dp)
        )

        OutlinedTextField(
            value = raw,
            onValueChange = { raw = it },
            label = { Text("Phone number e.g. +91 98765 43210") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(Modifier.height(16.dp))

        Button(
            onClick = { activity?.let { vm.sendOtp(it, phone) } },
            enabled = !vm.busy && raw.isNotBlank(),
            colors = ButtonDefaults.buttonColors(containerColor = Blue),
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp)
        ) {
            if (vm.busy) CircularProgressIndicator(color = Color.White, strokeWidth = 2.dp)
            else Text("Get OTP", fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
        }

        if (!vm.firebaseReady || vm.demoMode) {
            Text(
                "Firebase placeholder detected — OTP sign-in is disabled until you add a real google-services.json (see android/README).",
                fontSize = 12.sp,
                color = TextMuted,
                modifier = Modifier.padding(top = 16.dp)
            )
            TextButton(onClick = onOfflineContinue) {
                Text("Continue as local user (offline mode)", color = Blue)
            }
        }

        vm.error?.let { err ->
            Text(err, color = RedSpam, fontSize = 12.sp, modifier = Modifier.padding(top = 16.dp))
            if (!vm.firebaseReady || vm.demoMode) {
                TextButton(onClick = onOfflineContinue) {
                    Text("Continue as local user (offline mode)", color = Blue)
                }
            }
        }
    }
}
