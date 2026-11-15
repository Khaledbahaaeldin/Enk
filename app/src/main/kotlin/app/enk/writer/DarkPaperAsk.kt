package app.enk.writer

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.enk.design.EnkColors
import app.enk.design.glass.GlassButton
import app.enk.design.glass.GlassSpecs
import app.enk.design.glass.glassSurface
import app.enk.design.logo.LogoSpec
import app.enk.design.shell.GlassSource
import app.enk.design.shell.rememberGlassSource

/** The one-time question shown in the writer when the app is dark: start with the lamp on? */
@Composable
fun DarkPaperAsk(
    onLamp: () -> Unit,
    onNormal: () -> Unit,
    modifier: Modifier = Modifier,
    glassSource: GlassSource = rememberGlassSource()
) {
    val shape = RoundedCornerShape(20.dp)
    Box(
        modifier = modifier
            .testTag("dark_paper_ask")
            .glassSurface(spec = GlassSpecs.Toast, shape = shape, source = glassSource)
            .clip(shape)
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        Column {
            Text(
                text = "Your phone is in dark mode. Start with the lamp on?",
                color = LogoSpec.CREAM,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium
            )
            Spacer(modifier = Modifier.height(8.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                GlassButton(
                    onClick = onLamp,
                    modifier = Modifier.testTag("dark_paper_lamp"),
                    glassSource = glassSource,
                    solidColor = EnkColors.accent
                ) {
                    Text(
                        text = "Lamp on",
                        color = Color(0xFF1B1A18),
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                GlassButton(
                    onClick = onNormal,
                    modifier = Modifier.testTag("dark_paper_normal"),
                    glassSource = glassSource
                ) {
                    Text(
                        text = "Normal paper",
                        color = LogoSpec.CREAM,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }
    }
}
