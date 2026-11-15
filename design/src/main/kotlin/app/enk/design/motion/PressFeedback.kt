package app.enk.design.motion

import androidx.compose.animation.core.Animatable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import app.enk.design.shell.LocalReduceEffects
import kotlinx.coroutines.launch

object PressDepth {
    const val Button = 0.96f
    const val Card = 0.985f
    const val Pill = 0.95f
    const val Row = 0.985f
}

@Composable
fun Modifier.pressFeedback(depth: Float, enabled: Boolean = true): Modifier {
    val reduceEffects = LocalReduceEffects.current
    if (!enabled || reduceEffects) return this

    val scale = remember { Animatable(1f) }
    val scope = rememberCoroutineScope()

    return this
        .graphicsLayer {
            scaleX = scale.value
            scaleY = scale.value
        }
        .pointerInput(enabled) {
            awaitEachGesture {
                awaitFirstDown(requireUnconsumed = false, pass = PointerEventPass.Initial)
                scope.launch {
                    scale.animateTo(depth, ShellMotion.press.toSpec())
                }
                try {
                    do {
                        val event = awaitPointerEvent(PointerEventPass.Initial)
                    } while (event.changes.any { it.pressed })
                } finally {
                    scope.launch {
                        scale.animateTo(1f, ShellMotion.release.toSpec())
                    }
                }
            }
        }
}
