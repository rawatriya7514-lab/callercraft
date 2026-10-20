package com.craftcaller.app.ui.common

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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

import com.craftcaller.app.ui.Tab

/** Shared visual atoms used by every CraftCaller screen. */

@Composable
fun Avatar(initial: String, spam: Boolean, size: Int = 46) {
    val bg = if (spam) CraftPalette.Spam else CraftPalette.Blue
    Box(
        Modifier
            .size(size.dp)
            .background(
                Brush.linearGradient(listOf(bg, bg.copy(alpha = 0.82f))),
                shape = CircleShape
            ),
        contentAlignment = Alignment.Center
    ) {
        Text(
            initial.take(1).uppercase().ifEmpty { "?" },
            color = Color.White,
            fontSize = (size * 0.4f).sp,
            fontWeight = FontWeight.SemiBold
        )
    }
}

@Composable
fun Badge(label: String, red: Boolean = false) {
    val bg = if (red) CraftPalette.SpamSoft else CraftPalette.BlueSoft
    val fg = if (red) CraftPalette.Spam else CraftPalette.Blue
    Box(
        Modifier
            .background(bg, RoundedCornerShape(6.dp))
            .padding(horizontal = 6.dp, vertical = 2.dp)
    ) {
        Text(label, color = fg, fontSize = 10.sp, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
fun AppTitle(title: String) {
    Row(
        Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            title,
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onBackground
        )
    }
}

@Composable
fun craftPlaceholderScrollHint() {
    Text(
        "Scroll to see more",
        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
        fontSize = 12.sp
    )
}

@Composable
fun ScreenScaffold(
    tab: Tab,
    extra: @Composable () -> Unit = {},
    content: @Composable () -> Unit
) {
    Column(
        Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
    ) {
        Spacer(Modifier.height(18.dp))
        AppTitle(tab.label)
        Spacer(Modifier.height(8.dp))
        extra()
        Spacer(Modifier.height(6.dp))
        content()
        Spacer(Modifier.height(14.dp))
    }
}

@Composable
fun StatChip(text: String, color: Color) {
    Text(text, color = color, fontSize = 12.sp, fontWeight = FontWeight.Medium)
}

@Composable
fun EmptyHint(text: String) {
    Row(
        Modifier
            .fillMaxWidth()
            .padding(vertical = 30.dp),
        horizontalArrangement = Arrangement.Center
    ) {
        Text(
            text,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.45f),
            fontSize = 14.sp
        )
    }
}
