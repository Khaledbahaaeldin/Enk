package app.enk.writer

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.enk.design.EnkColors
import app.enk.design.logo.LogoSpec

/** The one-time question shown in the writer when the app is dark: start with the lamp on? */
@Composable
fun DarkPaperAsk(
    onLamp: () -> Unit,
    onNormal: () -> Unit,
    modifier: Modifier = Modifier
) {
    val shape = RoundedCornerShape(20.dp)
    Column(
        modifier = modifier
            .testTag("dark_paper_ask")
            .clip(shape)
            .background(Color(0xF0262421))
            .border(0.5.dp, Color(0x33F3EEE3), shape)
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        Text(
            text = "Your phone is in dark mode. Start with the lamp on?",
            color = LogoSpec.CREAM,
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium
        )
        Spacer(modifier = Modifier.height(8.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            AskButton(label = "Lamp on", primary = true, tag = "dark_paper_lamp", onClick = onLamp)
            Spacer(modifier = Modifier.width(8.dp))
            AskButton(label = "Normal paper", primary = false, tag = "dark_paper_normal", onClick = onNormal)
        }
    }
}

@Composable
private fun AskButton(label: String, primary: Boolean, tag: String, onClick: () -> Unit) {
    val shape = RoundedCornerShape(14.dp)
    Box(
        modifier = Modifier
            .testTag(tag)
            .defaultMinSize(minHeight = 48.dp)
            .clip(shape)
            .background(if (primary) EnkColors.accent else Color(0x22F3EEE3))
            .semantics { role = Role.Button }
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            color = if (primary) Color(0xFF1B1A18) else LogoSpec.CREAM,
            fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold
        )
    }
}
