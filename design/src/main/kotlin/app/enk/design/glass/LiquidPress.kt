/*
 * Copyright 2025 Kyant
 * Modified for Enk
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package app.enk.design.glass

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.VectorConverter
import androidx.compose.animation.core.spring
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import app.enk.design.effects.LocalEffects
import app.enk.design.effects.ResolvedEffects
import app.enk.design.shell.LocalReduceEffects
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.tanh

class LiquidPressState {
    var pressProgress: Float by mutableFloatStateOf(0f)
    var offset: Offset by mutableStateOf(Offset.Zero)
    var position: Offset by mutableStateOf(Offset.Zero)
}

@Composable
fun rememberLiquidPressState(): LiquidPressState {
    return remember { LiquidPressState() }
}

object LiquidPressMath {
    fun swellScale(heightPx: Float, density: Float, progress: Float): Float {
        if (heightPx <= 0f) return 1f
        val maxScale = 1f + (4f * density) / heightPx
        return 1f + progress * (maxScale - 1f)
    }

    fun lean(offsetPx: Float, maxOffsetPx: Float): Float {
        if (maxOffsetPx <= 0f) return 0f
        return maxOffsetPx * tanh((0.05f * offsetPx) / maxOffsetPx)
    }

    fun stretch(offset: Offset, size: Size, density: Float): Pair<Float, Float> {
        val length = offset.getDistance()
        if (length == 0f || size.height <= 0f) return Pair(0f, 0f)
        val maxAdd = (4f * density) / size.height
        val nx = abs(offset.x) / length
        val ny = abs(offset.y) / length
        val factor = tanh(length / (minOf(size.width, size.height).coerceAtLeast(1f)))
        return Pair(maxAdd * nx * factor, maxAdd * ny * factor)
    }

    fun resolveScale(isLight: Boolean, heightPx: Float, density: Float, progress: Float): Float =
        if (isLight) 1f else swellScale(heightPx, density, progress)

    fun resolveTranslation(isLight: Boolean, offsetPx: Float, maxOffsetPx: Float): Float =
        if (isLight) 0f else lean(offsetPx, maxOffsetPx)

    fun resolveStretch(isLight: Boolean, offset: Offset, size: Size, density: Float): Pair<Float, Float> =
        if (isLight) Pair(0f, 0f) else stretch(offset, size, density)
}

@Composable
fun Modifier.liquidPress(state: LiquidPressState, enabled: Boolean = true): Modifier {
    if (!enabled) return this
    val density = LocalDensity.current.density
    val effects = LocalEffects.current
    val reduceEffects = LocalReduceEffects.current
    val isLight = effects == ResolvedEffects.Light || reduceEffects

    val progressAnim = remember { Animatable(0f) }
    val posAnim = remember { Animatable(Offset.Zero, Offset.VectorConverter) }
    val scope = rememberCoroutineScope()

    val pressSpec = remember(isLight) {
        if (isLight) androidx.compose.animation.core.tween<Float>(100) else spring<Float>(dampingRatio = 0.5f, stiffness = 300f)
    }
    val posSpec = remember { spring<Offset>(dampingRatio = 0.5f, stiffness = 300f) }

    return this
        .pointerInput(enabled) {
            awaitEachGesture {
                val down = awaitFirstDown(requireUnconsumed = false)
                state.position = down.position
                state.offset = Offset.Zero
                val startPos = down.position

                scope.launch {
                    posAnim.snapTo(down.position)
                }
                scope.launch {
                    progressAnim.animateTo(1f, pressSpec) {
                        state.pressProgress = value
                    }
                }

                while (true) {
                    val event = awaitPointerEvent(PointerEventPass.Main)
                    val change = event.changes.firstOrNull { it.id == down.id } ?: break
                    if (!change.pressed) {
                        scope.launch {
                            progressAnim.animateTo(0f, pressSpec) {
                                state.pressProgress = value
                            }
                        }
                        scope.launch {
                            posAnim.animateTo(startPos, posSpec) {
                                state.position = value
                                state.offset = value - startPos
                            }
                        }
                        break
                    }
                    val currentPos = change.position
                    state.position = currentPos
                    state.offset = currentPos - startPos
                }
            }
        }
        .drawWithContent {
            drawContent()
            val p = state.pressProgress
            if (p > 0.001f) {
                drawRect(
                    color = Color.White.copy(alpha = 0.08f * p),
                    blendMode = BlendMode.Plus
                )
                val minDim = minOf(size.width, size.height)
                val radialRadius = (1.5f * minDim).coerceAtLeast(1f)
                val touchCenter = if (state.position == Offset.Zero) center else state.position
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            Color.White.copy(alpha = 0.15f * p),
                            Color.Transparent
                        ),
                        center = touchCenter,
                        radius = radialRadius
                    ),
                    center = touchCenter,
                    radius = radialRadius,
                    blendMode = BlendMode.Plus
                )
            }
        }
}
