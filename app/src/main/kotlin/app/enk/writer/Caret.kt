package app.enk.writer

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import app.enk.design.EnkColors
import app.enk.design.EnkMotion
import app.enk.text.CaretPos

@Composable
fun Caret(
    geometry: WriterGeometry,
    pos: CaretPos,
    typing: Boolean,
    reduceMotion: Boolean,
    modifier: Modifier = Modifier
) {
    val alpha = if (typing || reduceMotion) {
        1f
    } else {
        val transition = rememberInfiniteTransition(label = "caretBlink")
        val phase by transition.animateFloat(
            initialValue = 0f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(
                animation = tween(durationMillis = EnkMotion.caretBlinkMs, easing = LinearEasing),
                repeatMode = RepeatMode.Restart
            ),
            label = "caretPhase"
        )
        if (phase < 0.5f) 1f else 0.15f
    }

    // The caret lives inside the paper, so its offsets are relative to the paper's top-left.
    val caretTopDp = CaretGeometry.topDp(geometry) - geometry.paperTopDp
    val caretLeftDp = CaretGeometry.leftDp(geometry, pos.col) - WriterGeometry.PAPER_SIDE_DP

    Box(
        modifier = modifier
            .offset(x = caretLeftDp.dp, y = caretTopDp.dp)
            .size(width = geometry.columnWidthDp.dp, height = CaretGeometry.HEIGHT_DP.dp)
            .testTag("writer_caret")
            .alpha(alpha)
            .background(EnkColors.accent)
    )
}
