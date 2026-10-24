package com.craftcaller.app.ui.common

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import com.craftcaller.app.ui.theme.Blue
import com.craftcaller.app.ui.theme.GreenOk
import com.craftcaller.app.ui.theme.TextMuted

/** Minimal ViewModel factory for constructor-injected screens (MVVM, no DI). */
class SimpleFactory(private val createViewModel: () -> ViewModel) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T = createViewModel() as T
}

@Composable
inline fun <reified VM : ViewModel> rememberVm(crossinline createViewModel: () -> VM): VM {
    val factory = remember { SimpleFactory { createViewModel() } }
    return viewModel(factory = factory)
}

/** Circle avatar with initial (Unknown -> '#'). */
@Composable
fun Avatar(name: String?, size: Dp = 44.dp) {
    val initial = name?.trim()?.take(1)?.uppercase()
        ?.let { c -> if (c.first().isLetter()) c else "#" } ?: "#"
    val tint = avatarColor(initial)
    Box(
        modifier = Modifier
            .size(size)
            .clip(CircleShape)
            .background(tint),
        contentAlignment = Alignment.Center
    ) {
        Text(
            initial,
            color = Color.White,
            fontWeight = FontWeight.Bold,
            fontSize = (size.value * 0.4f).sp
        )
    }
}

fun avatarColor(initial: String): Color = when ((initial.firstOrNull()?.code ?: 35) % 6) {
    0 -> Blue
    1 -> GreenOk
    2 -> Color(0xFF7C4DFF)
    3 -> Color(0xFFF59E0B)
    4 -> Color(0xFFEF476F)
    else -> Color(0xFF06B6D4)
}

/** Spam / status pill used across tabs. */
@Composable
fun TagChip(text: String, color: Color, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(50))
            .background(color.copy(alpha = 0.14f))
    ) {
        Text(
            text,
            color = color,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier
                .align(Alignment.Center)
                .padding(horizontal = 10.dp, vertical = 4.dp)
        )
    }
}

@Composable
fun EmptyState(text: String) {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text("👀", fontSize = 42.sp)
            Text(text, color = TextMuted, fontSize = 14.sp, modifier = Modifier.padding(top = 10.dp))
        }
    }
}

@Composable
fun SectionTitle(text: String, modifier: Modifier = Modifier) {
    Text(
        text.uppercase(),
        color = TextMuted,
        fontSize = 12.sp,
        fontWeight = FontWeight.Bold,
        modifier = modifier
    )
}

@Composable
fun screenBg(): Color = MaterialTheme.colorScheme.background
