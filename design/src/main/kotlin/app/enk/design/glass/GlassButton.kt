package app.enk.design.glass

import androidx.compose.animation.core.Animatable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
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
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import app.enk.design.motion.ShellMotion
import app.enk.design.shell.GlassSource
import app.enk.design.shell.LocalReduceEffects
import app.enk.design.shell.rememberGlassSource
import kotlinx.coroutines.launch

@Composable
fun GlassButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    glassSource: GlassSource = rememberGlassSource(),
    content: @Composable RowScope.() -> Unit
) {
    val currentOnClick by rememberUpdatedState(onClick)
    val reduceEffects = LocalReduceEffects.current
    val scope = rememberCoroutineScope()
    val interaction = remember { GlassInteraction() }
    val scaleAnim = remember { Animatable(1f) }

    val shape = RoundedCornerShape(23.dp)

    Box(
        modifier = modifier
            .defaultMinSize(minWidth = 48.dp, minHeight = 48.dp)
            .semantics {
                role = Role.Button
                onClick {
                    if (enabled) {
                        currentOnClick()
                        true
                    } else false
                }
            }
            .pointerInput(enabled, reduceEffects) {
                if (!enabled) return@pointerInput
                awaitEachGesture {
                    val down = awaitFirstDown(requireUnconsumed = false)
                    val targetScale = if (reduceEffects) 1f else GlassLensMath.BUTTON_PRESS
                    scope.launch {
                        scaleAnim.animateTo(targetScale, ShellMotion.press.toSpec())
                    }
                    interaction.pressScale = targetScale

                    var cancelled = false
                    while (true) {
                        val event = awaitPointerEvent(PointerEventPass.Main)
                        val change = event.changes.firstOrNull { it.id == down.id } ?: break
                        if (!change.pressed) {
                            if (!cancelled && !change.isConsumed) {
                                currentOnClick()
                            }
                            break
                        }
                    }

                    scope.launch {
                        scaleAnim.animateTo(1f, ShellMotion.release.toSpec())
                    }
                    interaction.pressScale = 1f
                }
            },
        contentAlignment = Alignment.Center
    ) {
        interaction.pressScale = scaleAnim.value

        Box(
            modifier = Modifier
                .height(46.dp)
                .glassSurface(
                    spec = GlassSpecs.Button,
                    shape = shape,
                    source = glassSource,
                    interaction = interaction
                )
                .padding(horizontal = 18.dp),
            contentAlignment = Alignment.Center
        ) {
            Row(
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically,
                content = content
            )
        }
    }
}
