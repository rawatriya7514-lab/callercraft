package com.craftcaller.app.splash

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.craftcaller.app.data.remote.FirebaseRepository
import com.craftcaller.app.ui.theme.Blue
import com.craftcaller.app.ui.theme.BlueDark
import com.craftcaller.app.util.Prefs
import kotlinx.coroutines.delay

@Composable
fun SplashScreen(
    onGoToAuth: () -> Unit,
    onGoToHome: () -> Unit,
    onGoToOnboarding: () -> Unit = onGoToAuth
) {
    val context = LocalContext.current
    val prefs = remember { Prefs(context) }

    LaunchedEffect(Unit) {
        delay(650)
        val signedIn = runCatching { FirebaseRepository.currentUserUid() }.getOrNull() != null
        when {
            signedIn || prefs.onboardingDone -> onGoToHome()
            !prefs.profileReady -> onGoToOnboarding()
            else -> onGoToAuth()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(Blue, BlueDark))),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Box(
                modifier = Modifier
                    .size(96.dp)
                    .clip(CircleShape)
                    .background(Color.White),
                contentAlignment = Alignment.Center
            ) {
                Text("C", color = Blue, fontSize = 44.sp, fontWeight = FontWeight.ExtraBold)
            }
            Spacer(Modifier.height(16.dp))
            Text("CraftCaller", color = Color.White, fontSize = 30.sp, fontWeight = FontWeight.ExtraBold)
            Text("Know who is calling", color = Color(0xFFCFE0FF), fontSize = 14.sp, modifier = Modifier.height(30.dp))
            Spacer(Modifier.height(10.dp))
            CircularProgressIndicator(color = Color.White, strokeWidth = 3.dp)
        }
    }
}
