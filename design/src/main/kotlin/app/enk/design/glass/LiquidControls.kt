package app.enk.design.glass

import androidx.compose.runtime.Composable
import kotlin.math.abs
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

    /** Drag speed (dp/s) that counts as 1 in the Catalog stretch; the stretch tops out near 1000 dp/s. */
    const val STRETCH_VELOCITY_DP_S = 4000f

    /** Catalog velocity stretch: width grows and height shrinks with drag speed (either direction). */
    fun velocityStretch(velocityDpS: Float): Pair<Float, Float> {
        val v = abs(velocityDpS) / STRETCH_VELOCITY_DP_S
        val clampX = (v * 0.75f).coerceIn(0f, 0.2f)
        val clampY = (v * 0.25f).coerceIn(0f, 0.2f)
        val sx = 1f / (1f - clampX)
        val sy = 1f - clampY
        return Pair(sx, sy)
    }

    fun shouldShowNeck(overshootDp: Float, isPressed: Boolean): Boolean =
        isPressed && overshootDp > NECK_THRESHOLD_DP

    fun segmentTrailOffset(velocityPx: Float, maxOffsetPx: Float): Float =
        (velocityPx * 0.03f).coerceIn(-maxOffsetPx, maxOffsetPx)
}
