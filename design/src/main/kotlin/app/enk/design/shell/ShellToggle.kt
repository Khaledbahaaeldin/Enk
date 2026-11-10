package app.enk.design.shell

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.toggleableState
import androidx.compose.ui.state.ToggleableState
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import app.enk.design.motion.ShellMotion
import app.enk.design.theme.LocalShellColors
import app.enk.design.theme.LocalShellMaterial
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
 * Shell toggle per DS 12.8:
 * 52 x 32 dp track, radius 16 dp. Bead 26 x 26 dp.
 * Bead stretches (scaleX 1.12) while pressed.
 * Springs with toggleSpring (response 0.28, damping 1.0).
 * Touch height is at least 48 dp.
 */
@Composable
fun ShellToggle(
    checked: Boolean,
    onChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = LocalShellColors.current
    val material = LocalShellMaterial.current
    val currentOnChange by rememberUpdatedState(onChange)

    var isPressed by remember { mutableStateOf(false) }

    // Spring toggle
    val targetOffset = if (checked) ShellToggleMath.onOffsetDp else ShellToggleMath.offOffsetDp
    val animatedOffsetDp by animateFloatAsState(
        targetValue = targetOffset,
        animationSpec = spring(
            dampingRatio = ShellMotion.toggle.damping,
            stiffness = ShellMotion.toggle.stiffness
        ),
        label = "toggle_bead_offset"
    )

    val targetScaleX = if (isPressed) ShellToggleMath.pressedScaleX else 1.0f
    val animatedScaleX by animateFloatAsState(
        targetValue = targetScaleX,
        animationSpec = spring(dampingRatio = 1.0f, stiffness = 600f),
        label = "toggle_bead_scale"
    )

    val trackColor by animateColorAsState(
        targetValue = if (checked) colors.ui else material.track,
        label = "toggle_track_color"
    )

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
            .pointerInput(checked) {
                awaitEachGesture {
                    val down = awaitFirstDown(requireUnconsumed = false)
                    isPressed = true
                    var moved = false
                    while (true) {
                        val event = awaitPointerEvent(PointerEventPass.Main)
                        val change = event.changes.firstOrNull { it.id == down.id } ?: break
                        if (!change.pressed) {
                            isPressed = false
                            if (!moved && !change.isConsumed) {
                                currentOnChange(!checked)
                            }
                            break
                        }
                    }
                }
            },
        contentAlignment = Alignment.Center
    ) {
        // Track: 52 x 32 dp
        val trackShape = RoundedCornerShape(16.dp)
        Box(
            modifier = Modifier
                .width(ShellToggleMath.trackWidthDp.dp)
                .height(ShellToggleMath.trackHeightDp.dp)
                .testTag("shell_toggle_track")
                .clip(trackShape)
                .background(trackColor)
        ) {
            // Bead: 26 x 26 dp
            // The layer that stretches the bead is the parent of the tagged visual, so the visual's
            // bounds in the root include the stretch.
            Box(
                modifier = Modifier
                    .offset { IntOffset((animatedOffsetDp * density).roundToInt(), (ShellToggleMath.paddingDp * density).roundToInt()) }
                    .size(ShellToggleMath.beadSizeDp.dp)
                    .graphicsLayer {
                        scaleX = animatedScaleX
                        scaleY = 1.0f
                    }
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .testTag("shell_toggle_bead")
                        .clip(CircleShape)
                        .background(Color.White)
                )
            }
        }
    }
}
