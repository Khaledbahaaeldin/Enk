package app.enk.design.glass

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

class LiquidThumbState(
    initialProgress: Float = 0f
) {
    var pressProgress: Float by mutableFloatStateOf(initialProgress)
    val surfaceAlpha: Float get() = 1f - pressProgress.coerceIn(0f, 1f)
    val pressedScale: Float get() = 1f + 0.5f * pressProgress.coerceIn(0f, 1f)
}

@Composable
fun rememberLiquidThumbState(): LiquidThumbState = remember { LiquidThumbState() }

object LiquidControlsMath {
    const val NECK_THRESHOLD_DP = 8f
    val TouchHeight: Dp = 48.dp

    fun thumbSurfaceAlpha(pressProgress: Float): Float =
        1f - pressProgress.coerceIn(0f, 1f)

    fun toggleBeadScale(pressProgress: Float): Float =
        1f + 0.5f * pressProgress.coerceIn(0f, 1f)

    fun velocityStretch(velocity: Float): Pair<Float, Float> {
        val v = (velocity / 10f).coerceIn(-0.2f, 0.2f)
        val clampX = (v * 0.75f).coerceIn(-0.2f, 0.2f)
        val clampY = (v * 0.25f).coerceIn(-0.2f, 0.2f)
        val sx = 1f / (1f - clampX)
        val sy = 1f - clampY
        return Pair(sx, sy)
    }

    fun shouldShowNeck(overshootDp: Float, isPressed: Boolean): Boolean =
        isPressed && overshootDp > NECK_THRESHOLD_DP

    fun segmentTrailOffset(velocityPx: Float, maxOffsetPx: Float): Float =
        (velocityPx * 0.03f).coerceIn(-maxOffsetPx, maxOffsetPx)
}
