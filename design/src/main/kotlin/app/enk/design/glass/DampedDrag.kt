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
import androidx.compose.animation.core.spring
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

/**
 * Port of Catalog's DampedDragAnimation.
 * Springs:
 * - value: spring(1f, 1000f)
 * - velocity: spring(0.5f, 300f)
 * - press: spring(1f, 1000f)
 * - scaleX: spring(0.6f, 250f)
 * - scaleY: spring(0.7f, 250f)
 */
class DampedDrag(
    initialValue: Float = 0f
) {
    val valueAnim = Animatable(initialValue)
    val velocityAnim = Animatable(0f)
    val pressAnim = Animatable(0f)
    val scaleXAnim = Animatable(1f)
    val scaleYAnim = Animatable(1f)

    val value: Float get() = valueAnim.value
    val velocity: Float get() = velocityAnim.value
    val press: Float get() = pressAnim.value
    val scaleX: Float get() = scaleXAnim.value
    val scaleY: Float get() = scaleYAnim.value

    companion object {
        val valueSpring = spring<Float>(dampingRatio = 1f, stiffness = 1000f)
        val velocitySpring = spring<Float>(dampingRatio = 0.5f, stiffness = 300f)
        val pressSpring = spring<Float>(dampingRatio = 1f, stiffness = 1000f)
        val scaleXSpring = spring<Float>(dampingRatio = 0.6f, stiffness = 250f)
        val scaleYSpring = spring<Float>(dampingRatio = 0.7f, stiffness = 250f)
    }

    fun animateToValue(scope: CoroutineScope, target: Float) {
        scope.launch { valueAnim.animateTo(target, valueSpring) }
    }

    fun animateVelocity(scope: CoroutineScope, target: Float) {
        scope.launch { velocityAnim.animateTo(target, velocitySpring) }
    }

    fun animatePress(scope: CoroutineScope, target: Float) {
        scope.launch { pressAnim.animateTo(target, pressSpring) }
    }

    fun animateScaleX(scope: CoroutineScope, target: Float) {
        scope.launch { scaleXAnim.animateTo(target, scaleXSpring) }
    }

    fun animateScaleY(scope: CoroutineScope, target: Float) {
        scope.launch { scaleYAnim.animateTo(target, scaleYSpring) }
    }
}
