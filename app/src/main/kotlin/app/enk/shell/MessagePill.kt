package app.enk.shell

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.enk.design.theme.LocalShellColors

/** A short one-shot message above the navigation bar (a failed move, an import result). */
@Composable
fun MessagePill(text: String, modifier: Modifier = Modifier, tag: String = "message_pill") {
    val colors = LocalShellColors.current
    val shape = RoundedCornerShape(20.dp)
    Box(
        modifier = modifier
            .testTag(tag)
            .clip(shape)
            .background(colors.thick)
            .border(0.5.dp, colors.hair, shape)
            .padding(horizontal = 18.dp, vertical = 12.dp)
    ) {
        Text(text = text, color = colors.t1, fontSize = 14.sp, fontWeight = FontWeight.Medium)
    }
}
