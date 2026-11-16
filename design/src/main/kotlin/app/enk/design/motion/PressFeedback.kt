package app.enk.design.motion

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.spring
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import app.enk.design.effects.LocalEffects
import app.enk.design.effects.ResolvedEffects
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

    val effects = LocalEffects.current
    val isLight = effects == ResolvedEffects.Light
    val scale = remember { Animatable(1f) }
    val glowProgress = remember { Animatable(0f) }
    val pressPos = remember { mutableStateOf(Offset.Zero) }
    val scope = rememberCoroutineScope()

    return this
        .graphicsLayer {
            scaleX = scale.value
            scaleY = scale.value
        }
        .pointerInput(enabled) {
            awaitEachGesture {
                val down = awaitFirstDown(requireUnconsumed = false, pass = PointerEventPass.Initial)
                pressPos.value = down.position
                scope.launch {
                    scale.animateTo(depth, ShellMotion.press.toSpec())
                }
                if (!isLight) {
                    scope.launch {
                        glowProgress.animateTo(1f, spring(dampingRatio = 0.5f, stiffness = 300f))
                    }
                }
                try {
                    do {
                        val event = awaitPointerEvent(PointerEventPass.Initial)
                    } while (event.changes.any { it.pressed })
                } finally {
                    scope.launch {
                        scale.animateTo(1f, ShellMotion.release.toSpec())
                    }
                    if (!isLight) {
                        scope.launch {
                            glowProgress.animateTo(0f, spring(dampingRatio = 0.5f, stiffness = 300f))
                        }
                    }
                }
            }
        }
        .drawWithContent {
            drawContent()
            if (!isLight) {
                val p = glowProgress.value
                if (p > 0.001f) {
                    drawRect(
                        color = Color.White.copy(alpha = 0.08f * p),
                        blendMode = BlendMode.Plus
                    )
                    val minDim = minOf(size.width, size.height)
                    val radialRadius = (1.5f * minDim).coerceAtLeast(1f)
                    val centerPos = if (pressPos.value == Offset.Zero) center else pressPos.value
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                Color.White.copy(alpha = 0.15f * p),
                                Color.Transparent
                            ),
                            center = centerPos,
                            radius = radialRadius
                        ),
                        center = centerPos,
                        radius = radialRadius,
                        blendMode = BlendMode.Plus
                    )
                }
            }
        }
}
