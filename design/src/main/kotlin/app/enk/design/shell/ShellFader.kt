package app.enk.design.shell

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import app.enk.design.theme.LocalShellColors
import app.enk.design.theme.LocalShellMaterial
import kotlin.math.roundToInt

object ShellFaderMath {
    const val touchHeightDp = 48f
    const val trackHeightDp = 6f
    const val knobSizeDp = 24f

    fun fraction(value: Float): Float = value.coerceIn(0f, 1f)
}

/**
 * Continuous shell fader per DS 12.8 / 12.10:
 * Any continuous float in 0f..1f (not 20 detents).
 * Knob scales 1.12 while held.
 * Touch height is at least 48 dp.
 */
@Composable
fun ShellFader(
    value: Float,
    onChange: (Float) -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = LocalShellColors.current
    val material = LocalShellMaterial.current
    val density = LocalDensity.current
    val currentOnChange by rememberUpdatedState(onChange)

    var isHeld by remember { mutableStateOf(false) }
    var trackWidthPx by remember { mutableIntStateOf(0) }

    val knobScale by animateFloatAsState(
        targetValue = if (isHeld) 1.12f else 1.0f,
        animationSpec = tween(120),
        label = "fader_knob_scale"
    )

    val clampedValue = ShellFaderMath.fraction(value)

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(ShellFaderMath.touchHeightDp.dp)
            .onSizeChanged { trackWidthPx = it.width }
            .pointerInput(trackWidthPx) {
                awaitEachGesture {
                    val down = awaitFirstDown(requireUnconsumed = false)
                    val knobPx = with(density) { ShellFaderMath.knobSizeDp.dp.toPx() }
                    val travel = maxOf(1f, trackWidthPx - knobPx)
                    isHeld = true

                    val newFraction = ((down.position.x - knobPx / 2f) / travel).coerceIn(0f, 1f)
                    currentOnChange(newFraction)

                    while (true) {
                        val event = awaitPointerEvent(PointerEventPass.Main)
                        val change = event.changes.firstOrNull { it.id == down.id } ?: break
                        if (!change.pressed) {
                            isHeld = false
                            break
                        }
                        val dragFraction = ((change.position.x - knobPx / 2f) / travel).coerceIn(0f, 1f)
                        currentOnChange(dragFraction)
                    }
                }
            },
        contentAlignment = Alignment.CenterStart
    ) {
        val knobPx = with(density) { ShellFaderMath.knobSizeDp.dp.toPx() }
        val travelPx = maxOf(0f, trackWidthPx - knobPx)
        val knobOffsetPx = travelPx * clampedValue

        // Background track (6 dp tall)
        val trackShape = RoundedCornerShape(3.dp)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(ShellFaderMath.trackHeightDp.dp)
                .clip(trackShape)
                .background(material.track)
        ) {
            // Filled track in accent
            val fillWidthDp = with(density) { (knobOffsetPx + knobPx / 2f).toDp() }
            Box(
                modifier = Modifier
                    .height(ShellFaderMath.trackHeightDp.dp)
                    .clip(trackShape)
                    .background(colors.ui)
            )
        }

        // Knob (24 x 24 dp, scales 1.12 while held)
        Box(
            modifier = Modifier
                .offset { IntOffset(knobOffsetPx.roundToInt(), 0) }
                .size(ShellFaderMath.knobSizeDp.dp)
                .graphicsLayer {
                    scaleX = knobScale
                    scaleY = knobScale
                }
                .clip(CircleShape)
                .background(Color.White)
        )
    }
}
