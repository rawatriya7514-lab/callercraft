package com.craftcaller.app.ui.screens

import com.craftcaller.app.ui.CraftPalette
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

import com.craftcaller.app.data.Prefs

/** Premium tab: Truecaller-style value wheel rendered locally. */
@Composable
fun PremiumScreens() {
    var activateAttempt by remember { mutableStateOf(false) }
    val dark = Prefs.darkMode

    Column(
        Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text("CraftCaller Premium", style = MaterialTheme.typography.titleLarge)
        Text(
            "Beautiful caller ID is free — power tools make it better.",
            fontSize = 13.5.sp,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.65f)
        )
        Spacer(Modifier.height(14.dp))

        FeatureCard(
            title = "Per month",
            headline = "₹99 · Basic blocking",
            details = listOf("✓ No ads-free UI? Ads removed", "✓ Unlimited phone-number search", "✓ Ring-time popup"),
            highlight = false
        )
        Spacer(Modifier.height(10.dp))
        FeatureCard(
            title = "Best value",
            headline = "₹699 · Yearly + recording",
            details = listOf(
                "✓ Everything in monthly",
                "✓ Call recorder (mic capture)",
                "✓ Auto-block all top spammers",
                "✓ Priority community sync"
            ),
            highlight = true
        )
        Spacer(Modifier.height(10.dp))
        FeatureCard(
            title = "Family",
            headline = "₹1099 · Family of 5",
            details = listOf("✓ Premium for 5 numbers", "✓ Shared family blocklist", "✓ Priority support"),
            highlight = false
        )

        Spacer(Modifier.height(16.dp))
        Button(
            onClick = {
                activateAttempt = true
                Prefs.premium = true
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp),
            shape = RoundedCornerShape(14.dp),
            colors = androidx.compose.material3.ButtonDefaults.buttonColors(
                containerColor = CraftPalette.Blue
            )
        ) {
            Text(if (Prefs.premium) "✓ Premium active on this device" else "Activate locally (device-purchase)", fontSize = 15.sp)
        }
        if (activateAttempt) {
            Spacer(Modifier.height(8.dp))
            Text(
                "Local activation complete — this build demonstrates the premium state " +
                        "with no billing backend attached; hook Google Play Billing for production.",
                fontSize = 11.5.sp,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
            )
        }
        Spacer(Modifier.weight(1f))
    }
}

@Composable
private fun FeatureCard(title: String, headline: String, details: List<String>, highlight: Boolean) {
    val startColor = if (highlight) CraftPalette.Blue else MaterialTheme.colorScheme.surface
    val endColor = if (highlight) CraftPalette.BlueDark else MaterialTheme.colorScheme.surface
    val on = if (highlight) Color.White else MaterialTheme.colorScheme.onSurface
    Column(
        Modifier
            .fillMaxWidth()
            .background(
                Brush.linearGradient(listOf(startColor, endColor)),
                RoundedCornerShape(18.dp)
            )
            .padding(16.dp)
    ) {
        Text(title, fontSize = 11.5.sp, color = on.copy(alpha = 0.8f), fontWeight = FontWeight.Medium)
        Text(headline, fontSize = 18.sp, color = on, fontWeight = FontWeight.SemiBold)
        Spacer(Modifier.height(8.dp))
        details.forEach {
            Text(it, fontSize = 13.sp, color = on.copy(alpha = 0.92f))
            Spacer(Modifier.height(2.dp))
        }
    }
}
