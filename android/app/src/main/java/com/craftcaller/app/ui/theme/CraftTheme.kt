package com.craftcaller.app.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color

// Truecaller-style blue/white palette.
val Blue = Color(0xFF0A6CFF)
val BlueDark = Color(0xFF0854C6)
val BlueSoft = Color(0xFFE8F0FE)
val LightBg = Color(0xFFF7F9FC)
val CardSurface = Color(0xFFFFFFFF)
val TextPrimary = Color(0xFF16233B)
val TextMuted = Color(0xFF6B7A90)
val RedSpam = Color(0xFFE53935)
val GreenOk = Color(0xFF10B981)
val AmberWarn = Color(0xFFF59E0B)
val DarkBg = Color(0xFF0B1220)
val DarkSurface = Color(0xFF131C2E)

/** Runtime toggle (persisted via Prefs in the settings screen). */
object ThemeState {
    var isDark by mutableStateOf(false)
}

@Composable
fun CraftCallerTheme(content: @Composable () -> Unit) {
    val dark = ThemeState.isDark
    val scheme = if (dark) {
        darkColorScheme(
            primary = Blue,
            onPrimary = Color.White,
            secondary = AmberWarn,
            background = DarkBg,
            surface = DarkSurface,
            onBackground = Color(0xFFE6EAF2),
            onSurface = Color(0xFFE6EAF2),
            surfaceVariant = Color(0xFF1B2540),
            onSurfaceVariant = Color(0xFF9AA8C2),
            outline = Color(0xFF2A3550)
        )
    } else {
        lightColorScheme(
            primary = Blue,
            onPrimary = Color.White,
            secondary = AmberWarn,
            background = LightBg,
            surface = CardSurface,
            onBackground = TextPrimary,
            onSurface = TextPrimary,
            surfaceVariant = BlueSoft,
            onSurfaceVariant = TextMuted,
            outline = Color(0xFFDDE4EE)
        )
    }
    MaterialTheme(colorScheme = scheme, content = content)
}
