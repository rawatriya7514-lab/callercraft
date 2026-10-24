package com.craftcaller.app.premium

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.RecordVoiceOver
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.craftcaller.app.ui.theme.AmberWarn
import com.craftcaller.app.ui.theme.Blue
import com.craftcaller.app.ui.theme.BlueDark
import com.craftcaller.app.ui.theme.GreenOk
import com.craftcaller.app.ui.theme.TextMuted
import com.craftcaller.app.ui.theme.ThemeState
import com.craftcaller.app.util.Announcer
import com.craftcaller.app.util.Prefs

@Composable
fun PremiumScreen(
    onOpenProfile: () -> Unit = {},
    onOpenAudio: () -> Unit = {}
) {
    val context = LocalContext.current
    val prefs = remember { Prefs(context) }
    var dark by remember { mutableStateOf(ThemeState.isDark) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        // Hero
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(22.dp))
                .background(Brush.horizontalGradient(listOf(Blue, BlueDark)))
                .padding(22.dp)
        ) {
            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.Star, null, tint = AmberWarn, modifier = Modifier.size(22.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("CraftCaller Premium", color = Color.White, fontWeight = FontWeight.ExtraBold, fontSize = 20.sp)
                }
                Text(
                    "Legal-grade protection for your phone:\nAdvanced spam shield, unlimited searches, ad-free.",
                    color = Color(0xFFCFE0FF),
                    fontSize = 13.sp,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }
        }

        Spacer(Modifier.height(14.dp))

        // Plans
        PlanRow("Monthly", "₹99 / mo", selected = true)
        PlanRow("Quarterly", "₹249 / 3 mo", selected = false)
        PlanRow("Yearly", "₹799 / yr — save 46%", selected = false)

        Spacer(Modifier.height(14.dp))

        Section("What you get")
        Benefit("Verified enterprise caller database")
        Benefit("Ghost call fallback lookup via own API")
        Benefit("Priority community spam reports")
        Benefit("Ad-free experience forever")

        Spacer(Modifier.height(10.dp))
        // Personal profile & voice
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(18.dp))
                .background(MaterialTheme.colorScheme.surface)
                .clickable(onClickLabel = "Open personal profile editor") { onOpenProfile() }
                .padding(18.dp)
        ) {
            Icon(Icons.Filled.RecordVoiceOver, null, tint = Blue, modifier = Modifier.size(22.dp))
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text("Personal profile & voice", fontWeight = FontWeight.Bold)
                val who = Announcer.addressed(prefs.userName.ifBlank { "You" }, prefs.honorific)
                Text(
                    "$who · ${prefs.announceLang.label} · " +
                        if (prefs.announcerVoiceName.isBlank()) "system voice" else "voice: ${prefs.announcerVoiceName}",
                    fontSize = 12.sp,
                    color = TextMuted,
                    maxLines = 2
                )
            }
            Icon(Icons.Filled.ChevronRight, null, tint = TextMuted)
        }

        Spacer(Modifier.height(10.dp))
        // Audio & alerts
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(18.dp))
                .background(MaterialTheme.colorScheme.surface)
                .clickable(onClickLabel = "Open audio and alerts settings") { onOpenAudio() }
                .padding(18.dp)
        ) {
            Icon(Icons.Filled.MusicNote, null, tint = Blue, modifier = Modifier.size(22.dp))
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text("Audio & alerts", fontWeight = FontWeight.Bold)
                val styleLabel = com.craftcaller.app.audio.AlertStyle.fromKey(prefs.alertStyle).label
                Text(
                    "$styleLabel · ${(prefs.alertVolume * 100).toInt()}% · " +
                        if (prefs.vibrateOnAnnounce) "vib on" else "no vibrate",
                    fontSize = 12.sp,
                    color = TextMuted,
                    maxLines = 2
                )
            }
            Icon(Icons.Filled.ChevronRight, null, tint = TextMuted)
        }

        Spacer(Modifier.height(10.dp))
        // Dark mode
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(18.dp))
                .background(MaterialTheme.colorScheme.surface)
                .padding(18.dp)
        ) {
            Column(Modifier.weight(1f)) {
                Text("Dark mode", fontWeight = FontWeight.Bold)
                Text("Amazon-night CraftCaller UI", fontSize = 12.sp, color = TextMuted)
            }
            Switch(
                checked = dark,
                onCheckedChange = {
                    dark = it
                    ThemeState.isDark = it
                    prefs.darkTheme = it
                }
            )
        }

        Spacer(Modifier.height(10.dp))
        Section("About")
        Text(
            "CraftCaller v3.0.0 — Kotlin • Jetpack Compose • MVVM • Room • Firebase • Retrofit.\n" +
                "Drop-in community caller-ID: numbers reported as spam 10+ times turn red and are rejected.",
            fontSize = 12.sp,
            color = TextMuted
        )
        Spacer(Modifier.height(24.dp))
    }
}

@Composable
private fun PlanRow(name: String, price: String, selected: Boolean) {
    val tint = if (selected) Blue else MaterialTheme.colorScheme.surfaceVariant
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 5.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(tint.copy(alpha = 0.14f))
            .padding(18.dp)
    ) {
        Column {
            Text(name, fontWeight = FontWeight.Bold)
            Text(price, fontSize = 13.sp, color = TextMuted)
        }
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(50))
                .background(if (selected) Blue else Color.Transparent)
                .clickable { }
                .padding(horizontal = 18.dp, vertical = 8.dp)
        ) {
            Text(
                if (selected) "Current" else "Select",
                color = if (selected) Color.White else TextMuted,
                fontWeight = FontWeight.SemiBold,
                fontSize = 13.sp
            )
        }
    }
}

@Composable
private fun SectionTitle(text: String) = Text(
    text.uppercase(),
    fontSize = 12.sp,
    fontWeight = FontWeight.Bold,
    color = TextMuted
)

@Composable
private fun Section(text: String) {
    SectionTitle(text)
    Spacer(Modifier.height(6.dp))
}

@Composable
private fun Benefit(text: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.padding(vertical = 4.dp)
    ) {
        Icon(Icons.Filled.Verified, null, tint = GreenOk)
        Spacer(Modifier.width(10.dp))
        Text(text, fontSize = 14.sp)
    }
}
