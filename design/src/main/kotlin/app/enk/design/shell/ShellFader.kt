package app.enk.design.shell

import androidx.compose.animation.core.Animatable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.progressBarRangeInfo
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.setProgress
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import app.enk.design.glass.GlassInteraction
import app.enk.design.glass.GlassLensMath
import app.enk.design.glass.GlassSpecs
import app.enk.design.glass.glassSurface
import app.enk.design.motion.ShellMotion
import app.enk.design.theme.LocalShellColors
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

object ShellFaderMath {
    const val touchHeightDp = 48f
    const val trackHeightDp = 8f
    const val thumbWidthDp = 56f
    const val thumbHeightDp = 32f
    const val thumbRadiusDp = 16f

    /** Clamped to 0..1; NaN (a bad stored value) is the start of the track. */
    fun fraction(value: Float): Float = if (value.isNaN()) 0f else value.coerceIn(0f, 1f)
}

/**
 * Continuous shell fader on the glass kit:
 * Track with SliderTrack (8 dp high), thumb with SliderThumb (56 x 32 dp, radius 16 dp).
 * THUMB_PRESS = 1.14f, stretches with velocity.
 * Touch height is at least 48 dp.
 */
@Composable
fun ShellFader(
    value: Float,
    onChange: (Float) -> Unit,
    modifier: Modifier = Modifier,
    glassSource: GlassSource = rememberGlassSource()
) {
    val colors = LocalShellColors.current
    val density = LocalDensity.current
    val reduceEffects = LocalReduceEffects.current
    val currentOnChange by rememberUpdatedState(onChange)
    val scope = rememberCoroutineScope()

    var isHeld by remember { mutableStateOf(false) }
    var trackWidthPx by remember { mutableIntStateOf(0) }

    val thumbInteraction = remember { GlassInteraction() }
    val pressScaleAnim = remember { Animatable(1f) }

    val clampedValue = ShellFaderMath.fraction(value)
    val trackShape = RoundedCornerShape(4.dp)
    val thumbShape = RoundedCornerShape(ShellFaderMath.thumbRadiusDp.dp)

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(ShellFaderMath.touchHeightDp.dp)
            .testTag("shell_fader")
            .semantics {
                progressBarRangeInfo = ProgressBarRangeInfo(clampedValue, 0f..1f)
                setProgress { target ->
                    currentOnChange(ShellFaderMath.fraction(target))
                    true
                }
            }
            .onSizeChanged { trackWidthPx = it.width }
            .pointerInput(trackWidthPx, reduceEffects) {
                awaitEachGesture {
                    val down = awaitFirstDown(requireUnconsumed = false)
                    val thumbPx = with(density) { ShellFaderMath.thumbWidthDp.dp.toPx() }
                    val travel = maxOf(1f, trackWidthPx - thumbPx)
                    isHeld = true

                    var lastX = down.position.x
                    var lastTime = System.currentTimeMillis()

                    val targetPress = if (reduceEffects) 1f else GlassLensMath.THUMB_PRESS
                    scope.launch {
                        pressScaleAnim.animateTo(targetPress, ShellMotion.press.toSpec())
                    }

                    val newFraction = ((down.position.x - thumbPx / 2f) / travel).coerceIn(0f, 1f)
                    currentOnChange(newFraction)

                    while (true) {
                        val event = awaitPointerEvent(PointerEventPass.Main)
                        val change = event.changes.firstOrNull { it.id == down.id } ?: break

                        val now = System.currentTimeMillis()
                        val dtSec = maxOf(0.001f, (now - lastTime) / 1000f)
                        val vx = (change.position.x - lastX) / dtSec / density.density
                        lastX = change.position.x
                        lastTime = now

                        val (sx, sy) = if (reduceEffects) Pair(1f, 1f) else GlassLensMath.stretch(vx)
                        thumbInteraction.stretchX = sx
                        thumbInteraction.stretchY = sy

                        if (!change.pressed) {
                            isHeld = false
                            thumbInteraction.stretchX = 1f
                            thumbInteraction.stretchY = 1f
                            scope.launch {
                                pressScaleAnim.animateTo(1f, ShellMotion.release.toSpec())
                            }
                            break
                        }
                        val dragFraction = ((change.position.x - thumbPx / 2f) / travel).coerceIn(0f, 1f)
                        currentOnChange(dragFraction)
                    }
                }
            },
        contentAlignment = Alignment.CenterStart
    ) {
        thumbInteraction.pressScale = pressScaleAnim.value

        val thumbPx = with(density) { ShellFaderMath.thumbWidthDp.dp.toPx() }
        val travelPx = maxOf(0f, trackWidthPx - thumbPx)
        val thumbOffsetPx = travelPx * clampedValue

        // Track with SliderTrack (8 dp tall)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(ShellFaderMath.trackHeightDp.dp)
                .glassSurface(
                    spec = GlassSpecs.SliderTrack,
                    shape = trackShape,
                    source = glassSource
                )
        ) {
            // Filled track in accent
            val fillWidthDp = with(density) { (thumbOffsetPx + thumbPx / 2f).toDp() }
            Box(
                modifier = Modifier
                    .width(fillWidthDp)
                    .height(ShellFaderMath.trackHeightDp.dp)
                    .clip(trackShape)
                    .glassSurface(
                        spec = GlassSpecs.SliderTrack,
                        shape = trackShape,
                        source = glassSource,
                        solidColor = colors.ui.copy(alpha = 0.7f)
                    )
            )
        }

        // Thumb: 56 x 32 dp with SliderThumb
        Box(
            modifier = Modifier
                .offset { IntOffset(thumbOffsetPx.roundToInt(), 0) }
                .size(
                    width = ShellFaderMath.thumbWidthDp.dp,
                    height = ShellFaderMath.thumbHeightDp.dp
                )
                .testTag("shell_fader_knob")
                .glassSurface(
                    spec = GlassSpecs.SliderThumb,
                    shape = thumbShape,
                    source = glassSource,
                    interaction = thumbInteraction
                )
        )
    }
}
