package com.craftcaller.app.caller

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.craftcaller.app.ui.common.TagChip
import com.craftcaller.app.ui.theme.AmberWarn
import com.craftcaller.app.ui.theme.Blue
import com.craftcaller.app.ui.theme.GreenOk
import com.craftcaller.app.ui.theme.RedSpam
import com.craftcaller.app.ui.theme.TextMuted

/** Source + confidence badges for a resolved caller identity. */
fun labelColor(label: CallerLabel): Color = when (label) {
    CallerLabel.SAVED -> GreenOk
    CallerLabel.VERIFIED_BUSINESS -> Blue
    CallerLabel.COMMUNITY -> AmberWarn
    CallerLabel.SUSPECTED_SPAM -> RedSpam
    CallerLabel.UNKNOWN -> TextMuted
}

@Composable
fun CallerLabelBadges(identity: CallerIdentity, showConfidence: Boolean = true) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        TagChip(identity.label.badge, labelColor(identity.label))
        if (identity.allowed) {
            Spacer(Modifier.width(6.dp))
            TagChip("Allowed", GreenOk)
        }
        if (showConfidence && identity.confidence in 1..100) {
            Spacer(Modifier.width(8.dp))
            Text(
                "confidence ${identity.confidence}% · ${identity.sourceLine}",
                fontSize = 10.sp,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.55f),
                maxLines = 1
            )
        }
    }
}
