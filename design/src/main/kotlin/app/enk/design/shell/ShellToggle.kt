package app.enk.design.shell

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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
import app.enk.design.glass.GlassInteraction
import app.enk.design.glass.GlassLensMath
import app.enk.design.glass.GlassSpecs
import app.enk.design.glass.glassSurface
import app.enk.design.motion.ShellMotion
import app.enk.design.theme.LocalShellColors
import app.enk.design.theme.LocalShellMaterial
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

object ShellToggleMath {
    const val trackWidthDp = 52f
    const val trackHeightDp = 32f
    const val beadSizeDp = 26f
    const val paddingDp = 3f

    const val offOffsetDp = paddingDp // 3f
    const val onOffsetDp = trackWidthDp - beadSizeDp - paddingDp // 23f
    const val pressedScaleX = 1.12f
    const val touchHeightDp = 48f
}

/**
 * Shell toggle on the glass kit:
 * Track with ToggleTrack, orange colors.ui (alpha 0.55 under glass, opaque at Light).
 * Bead with ToggleBead (BEAD_PRESS = 1.16f, pressedScaleX = 1.12f).
 * Springs with toggle (response 0.28, damping 1.0).
 * Touch height is at least 48 dp.
 */
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
    val pressScaleAnim = remember { Animatable(1f) }
    val stretchXAnim = remember { Animatable(1f) }

    // Spring toggle offset
    val targetOffset = if (checked) ShellToggleMath.onOffsetDp else ShellToggleMath.offOffsetDp
    val animatedOffsetDp by animateFloatAsState(
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

    val trackColor by animateColorAsState(
        targetValue = if (checked) activeTrackColor else inactiveTrackColor,
        animationSpec = spring(
            dampingRatio = ShellMotion.toggle.damping,
            stiffness = ShellMotion.toggle.stiffness
        ),
        label = "toggle_track_color"
    )

    val trackShape = RoundedCornerShape(16.dp)

    // Outer touch area: 52 x 48 dp (touch height >= 48 dp)
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
                    val targetPress = if (reduceEffects) 1f else GlassLensMath.BEAD_PRESS
                    val targetStretch = if (reduceEffects) 1f else ShellToggleMath.pressedScaleX
                    scope.launch {
                        pressScaleAnim.animateTo(targetPress, ShellMotion.press.toSpec())
                    }
                    scope.launch {
                        stretchXAnim.animateTo(targetStretch, ShellMotion.press.toSpec())
                    }

                    while (true) {
                        val event = awaitPointerEvent(PointerEventPass.Main)
                        val change = event.changes.firstOrNull { it.id == down.id } ?: break
                        if (!change.pressed) {
                            scope.launch {
                                pressScaleAnim.animateTo(1f, ShellMotion.release.toSpec())
                            }
                            scope.launch {
                                stretchXAnim.animateTo(1f, ShellMotion.release.toSpec())
                            }
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
        beadInteraction.pressScale = pressScaleAnim.value
        beadInteraction.stretchX = stretchXAnim.value

        // Track: 52 x 32 dp with ToggleTrack
        Box(
            modifier = Modifier
                .width(ShellToggleMath.trackWidthDp.dp)
                .height(ShellToggleMath.trackHeightDp.dp)
                .testTag("shell_toggle_track")
                .glassSurface(
                    spec = GlassSpecs.ToggleTrack,
                    shape = trackShape,
                    source = glassSource,
                    solidColor = trackColor
                )
        ) {
            // Bead: 26 x 26 dp with ToggleBead
            Box(
                modifier = Modifier
                    .offset {
                        IntOffset(
                            (animatedOffsetDp * density.density).roundToInt(),
                            (ShellToggleMath.paddingDp * density.density).roundToInt()
                        )
                    }
                    .size(ShellToggleMath.beadSizeDp.dp)
                    .testTag("shell_toggle_bead")
                    .glassSurface(
                        spec = GlassSpecs.ToggleBead,
                        shape = CircleShape,
                        source = glassSource,
                        interaction = beadInteraction
                    )
            )
        }
    }
}
