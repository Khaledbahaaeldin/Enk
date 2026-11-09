package app.enk.shell

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.SemanticsPropertyKey
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.enk.design.shell.EdgeKind
import app.enk.design.shell.GlassSource
import app.enk.design.shell.scrollEdge
import app.enk.design.theme.LocalShellColors

val CompactTitleAlpha = SemanticsPropertyKey<Float>("CompactTitleAlpha")

@Composable
fun CompactBar(
    title: String,
    progress: Float,
    glassSource: GlassSource,
    modifier: Modifier = Modifier
) {
    val colors = LocalShellColors.current
    val p = progress.coerceIn(0f, 1f)

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(58.dp)
    ) {
        Box(
            modifier = Modifier
                .matchParentSize()
                .scrollEdge(EdgeKind.CompactBar, progress = p, source = glassSource)
        )
        Text(
            text = title,
            fontSize = 17.sp,
            fontWeight = FontWeight.SemiBold,
            color = colors.t1,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 26.dp)
                .graphicsLayer { alpha = p }
                .clearAndSetSemantics {
                    testTag = "compact_bar_title"
                    this[CompactTitleAlpha] = p
                }
        )
    }
}
