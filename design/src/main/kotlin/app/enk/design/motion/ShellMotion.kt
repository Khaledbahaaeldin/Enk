package app.enk.design.motion

import androidx.compose.animation.core.SpringSpec
import androidx.compose.animation.core.spring

data class ShellSpring(val response: Float, val damping: Float) {
    val stiffness: Float
        get() = (2f * Math.PI.toFloat() / response).let { it * it }

    fun <T> toSpec(): SpringSpec<T> = spring(dampingRatio = damping, stiffness = stiffness)
}

object ShellMotion {
    val default = ShellSpring(0.35f, 1f)
    val flick = ShellSpring(0.35f, 0.8f)
    val toggle = ShellSpring(0.28f, 1f)
    val sheet = ShellSpring(0.38f, 0.82f)

    const val LONG_PRESS_MS = 420L
    const val DRAG_SLOP_DP = 8f
    const val FLICK_THRESHOLD = 350f
}
