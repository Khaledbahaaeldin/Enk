package app.enk.design.glass

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationSpec
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
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
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.progressBarRangeInfo
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.setProgress
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import app.enk.design.motion.ShellMotion
import app.enk.design.shell.GlassSource
import app.enk.design.shell.LocalReduceEffects
import app.enk.design.shell.rememberGlassSource
import app.enk.design.theme.LocalShellMaterial
import com.kyant.backdrop.backdrops.layerBackdrop
import com.kyant.backdrop.backdrops.rememberLayerBackdrop
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.roundToInt

@Composable
fun GlassSlider(
    value: Int,
    min: Int,
    max: Int,
    onValueChange: (Int) -> Unit,
    onDetent: (Int) -> Unit,
    accent: Color,
    modifier: Modifier = Modifier,
    label: String,
    glassSource: GlassSource = rememberGlassSource(),
    onTouchChange: ((Boolean) -> Unit)? = null
) {
    val material = LocalShellMaterial.current
    val density = LocalDensity.current
    val reduceEffects = LocalReduceEffects.current
    val currentOnValueChange by rememberUpdatedState(onValueChange)
    val currentOnDetent by rememberUpdatedState(onDetent)
    val currentOnTouchChange by rememberUpdatedState(onTouchChange)
    val scope = rememberCoroutineScope()

    var trackWidthPx by remember { mutableIntStateOf(0) }
    val thumbWidthRestDp = 40.dp
    val thumbHeightRestDp = 24.dp
    val thumbWidthPressedDp = 56.dp
    val thumbHeightPressedDp = 32.dp

    val trackHeightDp = 6.dp
    val touchHeightDp = 48.dp

    val thumbShape = RoundedCornerShape(16.dp)
    val trackShape = RoundedCornerShape(trackHeightDp / 2)

    val thumbInteraction = remember { GlassInteraction() }
    val thumbPressState = rememberLiquidPressState()
    val dampedDrag = remember { DampedDrag() }

    val rawFraction = FaderMath.fractionFor(value, min, max)
    val fractionAnim = remember { Animatable(rawFraction) }

    var isDragging by remember { mutableStateOf(false) }
    var overshootDp by remember { mutableFloatStateOf(0f) }

    LaunchedEffect(rawFraction) {
        if (!isDragging) {
            fractionAnim.snapTo(rawFraction)
        }
    }

    var lastDetentIdx by remember { mutableIntStateOf((FaderMath.detent(rawFraction) * 20f).roundToInt()) }

    fun updateFraction(newFrac: Float) {
        val clamped = newFrac.coerceIn(0f, 1f)
        val detent = FaderMath.detent(clamped)
        val detentIdx = (detent * 20f).roundToInt()
        if (detentIdx != lastDetentIdx) {
            lastDetentIdx = detentIdx
            currentOnDetent(detentIdx * 5)
        }
        val newVal = FaderMath.valueFor(clamped, min, max)
        if (newVal != value) {
            currentOnValueChange(newVal)
        }
    }

    val trackBackdrop = rememberLayerBackdrop()
    val neckGroupState = rememberLiquidGroupState()

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(touchHeightDp)
            .semantics {
                contentDescription = label
                stateDescription = "$value"
                progressBarRangeInfo = ProgressBarRangeInfo(value.toFloat(), min.toFloat()..max.toFloat())
                setProgress { target ->
                    val frac = FaderMath.fractionFor(target.roundToInt(), min, max)
                    updateFraction(frac)
                    scope.launch { fractionAnim.snapTo(frac) }
                    true
                }
            }
            .onSizeChanged { trackWidthPx = it.width }
            .pointerInput(trackWidthPx, min, max, reduceEffects) {
                awaitEachGesture {
                    val down = awaitFirstDown(requireUnconsumed = false)
                    val thumbPx = with(density) { thumbWidthPressedDp.toPx() }
                    val travel = maxOf(1f, trackWidthPx - thumbPx)

                    val currentThumbLeft = fractionAnim.value * travel
                    val touchOffsetInsideThumb = down.position.x - currentThumbLeft
                    val hitThumb = touchOffsetInsideThumb in 0f..thumbPx

                    isDragging = true
                    currentOnTouchChange?.invoke(true)

                    val targetPress = if (reduceEffects) 1f else 1f
                    dampedDrag.animatePress(scope, targetPress)
                    thumbPressState.pressProgress = targetPress

                    val grabOffset = if (hitThumb) touchOffsetInsideThumb else thumbPx / 2f

                    var lastX = down.position.x
                    var lastTime = System.currentTimeMillis()

                    if (!hitThumb) {
                        val targetFrac = ((down.position.x - thumbPx / 2f) / travel).coerceIn(0f, 1f)
                        val spec: AnimationSpec<Float> = if (reduceEffects) tween(100) else ShellMotion.default.toSpec()
                        scope.launch {
                            fractionAnim.animateTo(targetFrac, spec)
                        }
                        updateFraction(targetFrac)
                    }

                    while (true) {
                        val event = awaitPointerEvent(PointerEventPass.Main)
                        val change = event.changes.firstOrNull { it.id == down.id } ?: break

                        val now = System.currentTimeMillis()
                        val dtSec = maxOf(0.001f, (now - lastTime) / 1000f)
                        val vx = (change.position.x - lastX) / dtSec / density.density
                        lastX = change.position.x
                        lastTime = now

                        dampedDrag.animateVelocity(scope, vx)

                        val (stretchX, stretchY) = if (reduceEffects) Pair(1f, 1f) else LiquidControlsMath.velocityStretch(vx)
                        thumbInteraction.stretchX = stretchX
                        thumbInteraction.stretchY = stretchY

                        val moveThumbLeft = change.position.x - grabOffset
                        val rawFrac = moveThumbLeft / travel
                        if (rawFrac < 0f) {
                            overshootDp = with(density) { (-moveThumbLeft).toDp().value }
                        } else if (rawFrac > 1f) {
                            overshootDp = with(density) { (moveThumbLeft - travel).toDp().value }
                        } else {
                            overshootDp = 0f
                        }

                        if (!change.pressed) {
                            isDragging = false
                            overshootDp = 0f
                            currentOnTouchChange?.invoke(false)
                            thumbInteraction.stretchX = 1f
                            thumbInteraction.stretchY = 1f
                            dampedDrag.animatePress(scope, 0f)
                            thumbPressState.pressProgress = 0f
                            break
                        }

                        val dragFrac = rawFrac.coerceIn(0f, 1f)
                        scope.launch { fractionAnim.snapTo(dragFrac) }
                        updateFraction(dragFrac)
                    }
                }
            }
    ) {
        val thumbPx = with(density) { thumbWidthPressedDp.toPx() }
        val travelPx = maxOf(0f, trackWidthPx - thumbPx)
        val currentFrac = fractionAnim.value
        val thumbLeftPx = currentFrac * travelPx
        val thumbLeftDp = with(density) { thumbLeftPx.toDp() }

        // Track (6 dp tall capsule)
        Box(
            modifier = Modifier
                .align(Alignment.CenterStart)
                .fillMaxWidth()
                .height(trackHeightDp)
                .layerBackdrop(trackBackdrop)
                .clip(trackShape)
                .background(material.track)
        ) {
            val filledWidthDp = thumbLeftDp + thumbWidthPressedDp / 2
            Box(
                modifier = Modifier
                    .align(Alignment.CenterStart)
                    .width(filledWidthDp)
                    .height(trackHeightDp)
                    .background(accent)
            )
        }

        val pressP = dampedDrag.press
        val currentWidthDp = androidx.compose.ui.unit.lerp(thumbWidthRestDp, thumbWidthPressedDp, pressP)
        val currentHeightDp = androidx.compose.ui.unit.lerp(thumbHeightRestDp, thumbHeightPressedDp, pressP)

        val showNeck = LiquidControlsMath.shouldShowNeck(overshootDp, isDragging)
        val neckShapes: () -> List<LiquidShape> = {
            val thumbCenterX = thumbLeftPx + thumbPx / 2f
            val thumbCenterY = with(density) { 24.dp.toPx() }
            val thumbShapeObj = LiquidShape(
                cx = thumbCenterX,
                cy = thumbCenterY,
                halfW = with(density) { (currentWidthDp / 2f).toPx() },
                halfH = with(density) { (currentHeightDp / 2f).toPx() },
                radius = with(density) { 12.dp.toPx() }
            )
            if (showNeck) {
                val pinnedCapsule = LiquidShape(
                    cx = if (fractionAnim.value <= 0f) with(density) { 3.dp.toPx() } else trackWidthPx - with(density) { 3.dp.toPx() },
                    cy = thumbCenterY,
                    halfW = with(density) { 3.dp.toPx() },
                    halfH = with(density) { 3.dp.toPx() },
                    radius = with(density) { 3.dp.toPx() }
                )
                listOf(thumbShapeObj, pinnedCapsule)
            } else {
                listOf(thumbShapeObj)
            }
        }

        LiquidGroup(
            state = neckGroupState,
            shapes = neckShapes,
            spec = LiquidSpecs.Thumb,
            source = glassSource,
            pressed = { isDragging }
        )

        // Thumb
        Box(
            modifier = Modifier
                .align(Alignment.CenterStart)
                .offset { IntOffset(thumbLeftPx.roundToInt(), 0) }
                .size(currentWidthDp, currentHeightDp)
                .graphicsLayer {
                    alpha = if (neckGroupState.merging) 0f else 1f
                }
                .glassSurface(
                    spec = GlassSpecs.SliderThumb,
                    liquid = LiquidSpecs.Thumb,
                    shape = thumbShape,
                    source = glassSource,
                    interaction = thumbInteraction,
                    press = thumbPressState,
                    extraSource = GlassSource(trackBackdrop)
                )
        )
    }
}
