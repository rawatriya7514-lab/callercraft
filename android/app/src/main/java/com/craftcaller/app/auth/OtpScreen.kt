package com.craftcaller.app.auth

import android.app.Activity
import android.app.Application
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
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
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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
import kotlinx.coroutines.delay

@Composable
fun OtpScreen(
    phone: String,
    onVerified: () -> Unit,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val activity = context as? Activity
    val vm: AuthViewModel = rememberVm { AuthViewModel(context.applicationContext as Application) }
    var code by rememberSaveable { mutableStateOf("") }
    var seconds by rememberSaveable { mutableIntStateOf(60) }

    LaunchedEffect(Unit) {
        while (seconds > 0) {
            delay(1000)
            seconds--
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(28.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text("Enter OTP", fontSize = 24.sp, fontWeight = FontWeight.ExtraBold)
        Text(
            "6-digit code sent to\n$phone",
            fontSize = 13.sp,
            color = TextMuted,
            modifier = Modifier.padding(top = 6.dp, bottom = 26.dp)
        )

        OutlinedTextField(
            value = code,
            onValueChange = { value -> code = value.filter { it.isDigit() }.take(6) },
            label = { Text("Verification code") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(Modifier.height(16.dp))

        Button(
            onClick = { vm.verify(code) { onVerified() } },
            enabled = !vm.busy && code.length == 6,
            colors = ButtonDefaults.buttonColors(containerColor = Blue),
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp)
        ) {
            if (vm.busy) CircularProgressIndicator(color = Color.White, strokeWidth = 2.dp)
            else Text("Verify & continue", fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
        }

        Spacer(Modifier.height(10.dp))

        when {
            seconds > 0 -> Text(
                "Resend code in ${seconds}s",
                color = TextMuted,
                fontSize = 13.sp
            )
            else -> TextButton(onClick = {
                activity?.let { vm.sendOtp(it, phone) }
                seconds = 60
            }) { Text("Resend code", color = Blue, fontWeight = FontWeight.SemiBold) }
        }

        TextButton(onClick = onBack) { Text("Change number", color = TextMuted) }

        if (!vm.firebaseReady) {
            Text(
                "Firebase placeholder detected — you can create your profile and use CraftCaller offline.",
                fontSize = 12.sp,
                color = TextMuted,
                modifier = Modifier.padding(top = 12.dp)
            )
            TextButton(onClick = onVerified) {
                Text("Continue as local user (offline mode)", color = Blue)
            }
        }

        vm.error?.let { err ->
            Text(err, color = RedSpam, fontSize = 12.sp, modifier = Modifier.padding(top = 8.dp))
        }
    }
}
