package app.enk.design

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.progressBarRangeInfo
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.setProgress
import androidx.compose.ui.unit.dp
import kotlin.math.roundToInt

object FaderMath {
    fun detent(fraction: Float): Float {
        if (fraction.isNaN()) return 0f
        val clamped = fraction.coerceIn(0f, 1f)
        return (clamped * 20f).roundToInt() / 20f
    }

    fun valueFor(fraction: Float, min: Int, max: Int): Int {
        return (min + detent(fraction) * (max - min)).roundToInt()
    }

    fun displayPercent(value: Int, min: Int, max: Int): Int {
        if (max == min) return 0
        return ((value.coerceIn(min, max) - min).toFloat() / (max - min) * 100f).roundToInt()
    }

    fun fractionFor(value: Int, min: Int, max: Int): Float {
        if (max == min) return 0f
        return (value.coerceIn(min, max) - min).toFloat() / (max - min)
    }

    fun knobLeftDp(fraction: Float, trackWidthDp: Float): Float {
        return detent(fraction) * (trackWidthDp - 24f)
    }
}

@Composable
fun Fader(
    label: String,
    value: Int,
    min: Int,
    max: Int,
    lampColor: Color,
    onValueChange: (Int) -> Unit,
    onDetent: (Int) -> Unit,
    modifier: Modifier = Modifier,
    lifted: Boolean? = null,
    reduceEffects: Boolean = false,
    enabled: Boolean = true
) {
    val density = LocalDensity.current
    var isPressed by remember { mutableStateOf(false) }

    val isLifted = lifted ?: isPressed

    val liftProgress by animateFloatAsState(
        targetValue = if (isLifted) 1f else 0f,
        animationSpec = if (isLifted) {
            tween(durationMillis = if (reduceEffects) 0 else EnkMotion.knobLiftMs)
        } else {
            tween(
                durationMillis = if (reduceEffects) 0 else EnkMotion.springKnobMs,
                easing = EnkMotion.springKnob
            )
        },
        label = "knobLift"
    )

    var currentDetentIndex by remember(value, min, max) {
        val frac = FaderMath.fractionFor(value, min, max)
        mutableIntStateOf((FaderMath.detent(frac) * 20f).roundToInt())
    }
    var currentValue by remember(value) {
        mutableIntStateOf(value)
    }

    val knobShadow = if (isLifted || liftProgress > 0.5f) {
        EnkShadows.knobLifted
    } else {
        EnkShadows.knob
    }

    BoxWithConstraints(
        modifier = Modifier
            .wrapContentHeight()
            .height(48.dp)
            .then(modifier)
            .semantics {
                if (label.isNotEmpty()) {
                    contentDescription = label
                }
                progressBarRangeInfo = ProgressBarRangeInfo(
                    current = value.toFloat(),
                    range = min.toFloat()..max.toFloat(),
                    steps = 20
                )
                setProgress { targetValue ->
                    val newVal = targetValue.roundToInt().coerceIn(min, max)
                    if (newVal != value) {
                        onValueChange(newVal)
                        val frac = FaderMath.fractionFor(newVal, min, max)
                        val detentIdx = (FaderMath.detent(frac) * 20f).roundToInt()
                        onDetent(detentIdx * 5)
                        true
                    } else {
                        false
                    }
                }
            }
            .pointerInput(enabled, min, max) {
                if (!enabled) return@pointerInput
                val knobWidthPx = with(density) { 24.dp.toPx() }
                awaitEachGesture {
                    val down = awaitFirstDown(requireUnconsumed = false)
                    down.consume()
                    isPressed = true

                    fun updatePosition(pointerX: Float) {
                        val travelPx = size.width - knobWidthPx
                        val fraction = if (travelPx > 0f) {
                            ((pointerX - knobWidthPx / 2f) / travelPx).coerceIn(0f, 1f)
                        } else 0f
                        val d = FaderMath.detent(fraction)
                        val index = (d * 20f).roundToInt()
                        if (index != currentDetentIndex) {
                            currentDetentIndex = index
                            onDetent(index * 5)
                        }
                        val newVal = FaderMath.valueFor(d, min, max)
                        if (newVal != currentValue) {
                            currentValue = newVal
                            onValueChange(newVal)
                        }
                    }

                    updatePosition(down.position.x)

                    while (true) {
                        val event = awaitPointerEvent()
                        val drag = event.changes.firstOrNull { it.id == down.id } ?: break
                        if (!drag.pressed) {
                            break
                        }
                        drag.consume()
                        updatePosition(drag.position.x)
                    }
                    isPressed = false
                }
            },
        contentAlignment = Alignment.CenterStart
    ) {
        val trackWidthDp = maxWidth.value
        val fraction = FaderMath.fractionFor(value, min, max)
        val knobLeftDp = FaderMath.knobLeftDp(fraction, trackWidthDp)
        val activeFillWidthDp = (knobLeftDp + 12f).coerceAtLeast(0f)

        // Track: height 8 dp, radius 5 dp, fill EnkColors.trackDark, shadow EnkShadows.track
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(8.dp)
                .align(Alignment.CenterStart)
                .enkShadow(EnkShadows.track, RoundedCornerShape(5.dp))
                .background(EnkColors.trackDark, RoundedCornerShape(5.dp))
                .clip(RoundedCornerShape(5.dp))
        ) {
            // Active fill
            if (activeFillWidthDp > 0f) {
                val fillGlowShadow = if (!reduceEffects) {
                    listOf(ShadowLayer(0.dp, 0.dp, 8.dp, 0.dp, lampColor.copy(alpha = 0.5f), false))
                } else {
                    emptyList()
                }
                Box(
                    modifier = Modifier
                        .width(activeFillWidthDp.dp)
                        .fillMaxHeight()
                        .enkShadow(fillGlowShadow, RoundedCornerShape(5.dp))
                        .background(
                            Brush.horizontalGradient(
                                colors = listOf(
                                    lampColor.copy(alpha = 0.3f),
                                    lampColor.copy(alpha = 1.0f)
                                )
                            )
                        )
                )
            }
        }

        // Knob: 24 x 26 dp, vertically centered in fader
        val knobTranslateYDp = (-2).dp * liftProgress
        val knobScale = 1.0f + 0.07f * liftProgress

        Box(
            modifier = Modifier
                .offset(x = knobLeftDp.dp)
                .size(width = 24.dp, height = 26.dp)
                .align(Alignment.CenterStart)
                .graphicsLayer {
                    translationY = knobTranslateYDp.toPx()
                    scaleX = knobScale
                    scaleY = knobScale
                }
                .enkShadow(knobShadow, RoundedCornerShape(7.dp))
                .background(EnkMaterials.graphiteKnob, RoundedCornerShape(7.dp))
        ) {
            // Top collar: 4 dp
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(4.dp)
                    .background(
                        EnkMaterials.collarTop,
                        RoundedCornerShape(topStart = 7.dp, topEnd = 7.dp)
                    )
            )

            // Bottom collar: 4 dp
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .height(4.dp)
                    .background(
                        EnkMaterials.collarBottom,
                        RoundedCornerShape(bottomStart = 7.dp, bottomEnd = 7.dp)
                    )
            )

            // Index line: 2 x 12 dp vertical, top inset 7 dp, radius 1 dp, fill accent, 1 dp border Black 30%
            Box(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 7.dp)
                    .size(width = 2.dp, height = 12.dp)
                    .background(EnkColors.accent, RoundedCornerShape(1.dp))
                    .border(1.dp, Color.Black.copy(alpha = 0.3f), RoundedCornerShape(1.dp))
            )

            // Grooves: width 14 dp (inset 5 dp sides), height 3 dp, bottom inset 7 dp, vertical lines 1 dp wide white 18% repeating every 3 dp
            Canvas(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 7.dp)
                    .size(width = 14.dp, height = 3.dp)
            ) {
                val lineWidth = 1.dp.toPx()
                val period = 3.dp.toPx()
                var x = 0f
                while (x <= size.width) {
                    drawRect(
                        color = Color.White.copy(alpha = 0.18f),
                        topLeft = Offset(x, 0f),
                        size = Size(lineWidth, size.height)
                    )
                    x += period
                }
            }
        }
    }
}

@Composable
fun Fader(
    label: String,
    value: Int,
    min: Int,
    max: Int,
    lampColor: Rgb,
    onValueChange: (Int) -> Unit,
    onDetent: (Int) -> Unit,
    modifier: Modifier = Modifier,
    lifted: Boolean? = null,
    reduceEffects: Boolean = false,
    enabled: Boolean = true
) = Fader(
    label = label,
    value = value,
    min = min,
    max = max,
    lampColor = Color(lampColor.r, lampColor.g, lampColor.b),
    onValueChange = onValueChange,
    onDetent = onDetent,
    modifier = modifier,
    lifted = lifted,
    reduceEffects = reduceEffects,
    enabled = enabled
)
