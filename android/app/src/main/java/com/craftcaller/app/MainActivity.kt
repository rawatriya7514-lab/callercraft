package com.craftcaller.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.enableEdgeToEdge
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.craftcaller.app.ui.navigation.CraftNavGraph
import com.craftcaller.app.ui.theme.Blue
import com.craftcaller.app.ui.theme.BlueSoft
import com.craftcaller.app.ui.theme.CraftCallerTheme
import com.craftcaller.app.ui.theme.TextMuted
import com.craftcaller.app.util.Prefs
import com.craftcaller.app.util.RequiredPermissions

/** Single-Activity Compose host. Permissions are requested before navigation. */
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val prefs = Prefs(this)
            var crashText by remember { mutableStateOf(prefs.lastCrash.takeIf { it.isNotBlank() }) }
            if (crashText != null) {
                CrashReportScreen(crashText ?: "", onDismiss = {
                    prefs.lastCrash = ""
                    crashText = null
                })
            } else {
                CraftCallerTheme {
                    PermissionGate { CraftNavGraph() }
                }
            }
        }
    }
}

@Composable
private fun PermissionGate(content: @Composable () -> Unit) {
    val context = LocalContext.current
    var coreGranted by remember {
        mutableStateOf(RequiredPermissions.allGranted(context, RequiredPermissions.CALLER_ID))
    }
    val launcher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) {
        coreGranted = RequiredPermissions.allGranted(context, RequiredPermissions.CALLER_ID)
    }

    if (coreGranted) {
        content()
        return
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.padding(28.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(88.dp)
                    .clip(CircleShape)
                    .background(Blue),
                contentAlignment = Alignment.Center
            ) {
                Text("C", color = Color.White, fontSize = 40.sp, fontWeight = FontWeight.ExtraBold)
            }
            Spacer(Modifier.height(18.dp))
            Text("CraftCaller", fontSize = 30.sp, fontWeight = FontWeight.ExtraBold)
            Text(
                "Truecaller-grade caller ID, spam shield & smart SMS",
                fontSize = 14.sp,
                color = TextMuted,
                modifier = Modifier.padding(top = 6.dp)
            )
            Spacer(Modifier.height(26.dp))

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(18.dp))
                    .background(BlueSoft)
                    .padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Bullet("Identify callers even if they are not in contacts")
                Bullet("Red overlay + auto-block for reported spam numbers")
                Bullet("Smart SMS inbox: OTP one-tap copy & spam detection")
                Bullet("One-tap community spam reporting")
            }
            Spacer(Modifier.height(26.dp))

            Button(
                onClick = {
                    val all = RequiredPermissions.CALLER_ID +
                        RequiredPermissions.NOTIFICATIONS +
                        RequiredPermissions.SMS +
                        RequiredPermissions.MICROPHONE
                    launcher.launch(all)
                },
                colors = ButtonDefaults.buttonColors(containerColor = Blue),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
            ) { Text("Get started", fontSize = 16.sp, fontWeight = FontWeight.SemiBold) }

            TextButton(
                onClick = {
                    val core = RequiredPermissions.CALLER_ID + RequiredPermissions.NOTIFICATIONS
                    launcher.launch(core)
                }
            ) {
                Text("Continue with caller ID only", color = TextMuted, fontSize = 13.sp)
            }
        }
    }
}

@Composable
private fun Bullet(text: String) {
    Text("•  $text", fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurface)
}

/**
 * Shows the last uncaught crash saved by CrashReporter — works even when the
 * app crashes so hard nothing else renders. Screenshot it (or hit Copy).
 */
@Composable
private fun CrashReportScreen(crash: String, onDismiss: () -> Unit) {
    val clipboard = LocalClipboardManager.current
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF10151F))
            .padding(16.dp)
    ) {
        Text("⚠️ Last crash", color = Color(0xFFFF6E6E), fontSize = 20.sp, fontWeight = FontWeight.ExtraBold)
        Text(
            "Iska screenshot bhej do (ya Copy karke paste) — isse exact reason pata chalega.",
            color = Color(0xFF8CA1BF),
            fontSize = 13.sp,
            modifier = Modifier.padding(top = 4.dp)
        )
        Row(Modifier.padding(top = 8.dp)) {
            TextButton(onClick = { clipboard.setText(AnnotatedString(crash)) }) {
                Text("Copy", color = Color(0xFF6CC0FF))
            }
            TextButton(onClick = onDismiss) {
                Text("Clear & start app", color = Color(0xFF6CC0FF))
            }
        }
        SelectionContainer {
            Column(
                modifier = Modifier
                    .verticalScroll(rememberScrollState())
                    .padding(top = 8.dp)
            ) {
                Text(
                    crash,
                    color = Color(0xFFEFF4FF),
                    fontSize = 11.sp,
                    fontFamily = FontFamily.Monospace
                )
            }
        }
    }
}
