package com.craftcaller.app.ui

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.craftcaller.app.data.Prefs

/** Truecaller-inspired CraftCaller blue-on-white (with full dark theme). */
object CraftPalette {
    val Blue = Color(0xFF0087FF)
    val BlueDark = Color(0xFF0069E0)
    val BlueSoft = Color(0xFFE6F3FF)
    val Spam = Color(0xFFE53935)
    val SpamSoft = Color(0xFFFFEBEE)
    val Ok = Color(0xFF2E7D32)
    val Unknown = Color(0xFF8A94A6)
    val BgLight = Color(0xFFFFFFFF)
    val BgDark = Color(0xFF0E1116)
    val SurfaceLight = Color(0xFFF4F7FB)
    val SurfaceDark = Color(0xFF171B22)
    val InkLight = Color(0xFF111418)
    val InkDark = Color(0xFFEDF1F7)
    val MutedLight = Color(0xFF67707E)
    val MutedDark = Color(0xFF9AA3B0)

    fun ink(dark: Boolean) = if (dark) InkDark else InkLight
    fun muted(dark: Boolean) = if (dark) MutedDark else MutedLight
    fun bg(dark: Boolean) = if (dark) BgDark else BgLight
    fun surface(dark: Boolean) = if (dark) SurfaceDark else SurfaceLight
}

fun craftTypography() = Typography(
    headlineSmall = TextStyle(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.SemiBold, fontSize = 26.sp),
    titleLarge = TextStyle(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.SemiBold, fontSize = 21.sp),
    titleMedium = TextStyle(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.SemiBold, fontSize = 17.sp),
    bodyLarge = TextStyle(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.Normal, fontSize = 16.sp),
    bodyMedium = TextStyle(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.Normal, fontSize = 14.sp),
    labelSmall = TextStyle(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.SemiBold, fontSize = 11.sp)
)

@Composable
fun CraftCallerTheme(content: @Composable () -> Unit) {
    val dark = Prefs.darkMode || isSystemInDarkTheme()
    val scheme = if (dark) darkColorScheme(
        primary = CraftPalette.Blue,
        secondary = CraftPalette.BlueDark,
        error = CraftPalette.Spam,
        background = CraftPalette.BgDark,
        surface = CraftPalette.SurfaceDark,
        onBackground = CraftPalette.InkDark,
        onSurface = CraftPalette.InkDark
    ) else lightColorScheme(
        primary = CraftPalette.Blue,
        secondary = CraftPalette.BlueDark,
        error = CraftPalette.Spam,
        background = CraftPalette.BgLight,
        surface = CraftPalette.SurfaceLight,
        onBackground = CraftPalette.InkLight,
        onSurface = CraftPalette.InkLight
    )
    MaterialTheme(
        colorScheme = scheme,
        typography = craftTypography(),
        shapes = MaterialTheme.shapes.copy(
            large = RoundedCornerShape(24.dp),
            extraLarge = RoundedCornerShape(28.dp)
        ),
        content = content
    )
}
