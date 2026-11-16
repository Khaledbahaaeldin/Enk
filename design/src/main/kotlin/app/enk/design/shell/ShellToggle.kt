package app.enk.design.shell

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.toggleableState
import androidx.compose.ui.state.ToggleableState
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import app.enk.design.glass.DampedDrag
import app.enk.design.glass.GlassInteraction
import app.enk.design.glass.GlassSpecs
import app.enk.design.glass.LiquidControlsMath
import app.enk.design.glass.LiquidGroup
import app.enk.design.glass.LiquidShape
import app.enk.design.glass.LiquidSpecs
import app.enk.design.glass.glassSurface
import app.enk.design.glass.rememberLiquidGroupState
import app.enk.design.glass.rememberLiquidPressState
import app.enk.design.motion.ShellMotion
import app.enk.design.theme.LocalShellColors
import app.enk.design.theme.LocalShellMaterial
import com.kyant.backdrop.backdrops.LayerBackdrop
import com.kyant.backdrop.backdrops.layerBackdrop
import com.kyant.backdrop.backdrops.rememberLayerBackdrop
import kotlin.math.roundToInt

object ShellToggleMath {
    const val trackWidthDp = 52f
    const val trackHeightDp = 32f
    const val beadWidthDp = 26f
    const val beadHeightDp = 26f
    const val paddingDp = 3f

    const val offOffsetDp = paddingDp // 3f
    const val onOffsetDp = trackWidthDp - beadWidthDp - paddingDp // 23f
    const val pressedScale = 1.5f
    const val touchHeightDp = 48f
}

@Composable
fun ShellToggle(
    checked: Boolean,
    onChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    glassSource: GlassSource = rememberGlassSource()
) {
    val colors = LocalShellColors.current
    val material = LocalShellMaterial.current
    val backend = rememberGlassBackend()
    val reduceEffects = LocalReduceEffects.current
    val density = LocalDensity.current
    val currentOnChange by rememberUpdatedState(onChange)
    val scope = rememberCoroutineScope()

    val beadInteraction = remember { GlassInteraction() }
    val beadPressState = rememberLiquidPressState()
    val dampedDrag = remember { DampedDrag() }

    val targetOffset = if (checked) ShellToggleMath.onOffsetDp else ShellToggleMath.offOffsetDp
    val animatedOffset = animateFloatAsState(
        targetValue = targetOffset,
        animationSpec = spring(
            dampingRatio = ShellMotion.toggle.damping,
            stiffness = ShellMotion.toggle.stiffness
        ),
        label = "toggle_bead_offset"
    )

    val trackAlpha = if (backend == GlassBackendKind.Solid) 1f else 0.55f
    val activeTrackColor = colors.ui.copy(alpha = trackAlpha)
    val inactiveTrackColor = if (backend == GlassBackendKind.Solid) material.field else material.track

    val trackColor = animateColorAsState(
        targetValue = if (checked) activeTrackColor else inactiveTrackColor,
        animationSpec = spring(
            dampingRatio = ShellMotion.toggle.damping,
            stiffness = ShellMotion.toggle.stiffness
        ),
        label = "toggle_track_color"
    )

    val beadShape = RoundedCornerShape(13.dp)

    val trackBackdrop = rememberLayerBackdrop()
    val neckGroupState = rememberLiquidGroupState()

    var isPressed by remember { mutableStateOf(false) }
    var dragOvershootDp by remember { mutableFloatStateOf(0f) }

    Box(
        modifier = modifier
            .width(ShellToggleMath.trackWidthDp.dp)
            .height(ShellToggleMath.touchHeightDp.dp)
            .testTag("shell_toggle")
            .semantics {
                role = Role.Switch
                toggleableState = ToggleableState(checked)
                onClick {
                    currentOnChange(!checked)
                    true
                }
            }
            .pointerInput(checked, reduceEffects) {
                awaitEachGesture {
                    val down = awaitFirstDown(requireUnconsumed = false)
                    isPressed = true
                    dampedDrag.animatePress(scope, 1f)
                    beadPressState.animatePress(scope, 1f, instant = reduceEffects)

                    var lastX = down.position.x
                    var lastTime = System.currentTimeMillis()

                    while (true) {
                        val event = awaitPointerEvent(PointerEventPass.Main)
                        val change = event.changes.firstOrNull { it.id == down.id } ?: break

                        val now = System.currentTimeMillis()
                        val dtSec = maxOf(0.001f, (now - lastTime) / 1000f)
                        val vx = (change.position.x - lastX) / dtSec / density.density
                        lastX = change.position.x
                        lastTime = now

                        dampedDrag.animateVelocity(scope, vx)

                        val (sx, sy) = if (reduceEffects) Pair(1f, 1f) else LiquidControlsMath.velocityStretch(vx)
                        beadInteraction.stretchX = sx
                        beadInteraction.stretchY = sy

                        val moveX = change.position.x - down.position.x
                        if (moveX < -8f) {
                            dragOvershootDp = with(density) { (-moveX).toDp().value }
                        } else if (moveX > 8f) {
                            dragOvershootDp = with(density) { moveX.toDp().value }
                        } else {
                            dragOvershootDp = 0f
                        }

                        if (!change.pressed) {
                            isPressed = false
                            dragOvershootDp = 0f
                            dampedDrag.animatePress(scope, 0f)
                            beadPressState.animatePress(scope, 0f, instant = reduceEffects)
                            beadInteraction.stretchX = 1f
                            beadInteraction.stretchY = 1f
                            if (!change.isConsumed) {
                                currentOnChange(!checked)
                            }
                            break
                        }
                    }
                }
            },
        contentAlignment = Alignment.Center
    ) {
        // Track glass. It records the track backdrop the bead samples, so the bead must not
        // be its child (a child sampling its parent's recorded layer recurses on the render thread).
        ToggleTrackGlass(
            trackColor = trackColor,
            trackBackdrop = trackBackdrop,
            glassSource = glassSource
        )

        // Bead layer: same bounds as the track, drawn above it.
        Box(
            modifier = Modifier
                .width(ShellToggleMath.trackWidthDp.dp)
                .height(ShellToggleMath.trackHeightDp.dp)
        ) {
            val showNeck = LiquidControlsMath.shouldShowNeck(dragOvershootDp, isPressed)
            val neckShapes: () -> List<LiquidShape> = {
                val beadLeftPx = with(density) { animatedOffset.value.dp.toPx() }
                val beadWidthPx = with(density) { ShellToggleMath.beadWidthDp.dp.toPx() }
                val beadHeightPx = with(density) { ShellToggleMath.beadHeightDp.dp.toPx() }
                val beadCenterY = with(density) { (ShellToggleMath.trackHeightDp / 2f).dp.toPx() }
                val beadShapeObj = LiquidShape(
                    cx = beadLeftPx + beadWidthPx / 2f,
                    cy = beadCenterY,
                    halfW = beadWidthPx / 2f,
                    halfH = beadHeightPx / 2f,
                    radius = with(density) { 13.dp.toPx() }
                )
                if (showNeck) {
                    val pinnedCapsule = LiquidShape(
                        cx = if (!checked) with(density) { 3.dp.toPx() } else with(density) { (ShellToggleMath.trackWidthDp - 3f).dp.toPx() },
                        cy = beadCenterY,
                        halfW = with(density) { 3.dp.toPx() },
                        halfH = with(density) { 3.dp.toPx() },
                        radius = with(density) { 3.dp.toPx() }
                    )
                    listOf(beadShapeObj, pinnedCapsule)
                } else {
                    listOf(beadShapeObj)
                }
            }

            LiquidGroup(
                state = neckGroupState,
                shapes = neckShapes,
                spec = LiquidSpecs.Thumb,
                source = glassSource,
                pressed = { isPressed },
                modifier = Modifier.fillMaxSize()
            )

            // Bead: 26 x 26 dp with ToggleBead
            Box(
                modifier = Modifier
                    .offset {
                        IntOffset(
                            (animatedOffset.value * density.density).roundToInt(),
                            (ShellToggleMath.paddingDp * density.density).roundToInt()
                        )
                    }
                    .size(ShellToggleMath.beadWidthDp.dp, ShellToggleMath.beadHeightDp.dp)
                    .graphicsLayer {
                        val scale = if (reduceEffects) 1f else LiquidControlsMath.toggleBeadScale(dampedDrag.press)
                        scaleX = scale * beadInteraction.stretchX
                        scaleY = scale * beadInteraction.stretchY
                        alpha = if (neckGroupState.merging) 0f else 1f
                    }
                    .testTag("shell_toggle_bead")
                    .glassSurface(
                        spec = GlassSpecs.ToggleBead,
                        liquid = LiquidSpecs.Thumb,
                        shape = beadShape,
                        source = glassSource,
                        interaction = beadInteraction,
                        press = beadPressState,
                        extraSource = GlassSource(trackBackdrop)
                    )
            )
        }
    }
}

@Composable
private fun ToggleTrackGlass(
    trackColor: State<Color>,
    trackBackdrop: LayerBackdrop,
    glassSource: GlassSource
) {
    Box(
        modifier = Modifier
            .width(ShellToggleMath.trackWidthDp.dp)
            .height(ShellToggleMath.trackHeightDp.dp)
            .testTag("shell_toggle_track")
            .layerBackdrop(trackBackdrop)
            .glassSurface(
                spec = GlassSpecs.ToggleTrack,
                liquid = LiquidSpecs.Bar,
                shape = RoundedCornerShape(16.dp),
                source = glassSource,
                solidColor = trackColor.value
            )
    )
}
