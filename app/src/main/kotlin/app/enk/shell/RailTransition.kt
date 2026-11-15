package app.enk.shell

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationSpec
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import app.enk.design.effects.LocalEffects
import app.enk.design.shell.LocalReduceEffects
import app.enk.design.effects.ResolvedEffects
import app.enk.design.motion.ShellMotion
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

object RailTransitionMath {
    fun seg(p: Float, a: Float, b: Float): Float =
        if (b == a) (if (p >= a) 1f else 0f) else ((p - a) / (b - a)).coerceIn(0f, 1f)

    fun ez(t: Float): Float = t * t * (3f - 2f * t)

    fun out(t: Float): Float = 1f - (1f - t) * (1f - t) * (1f - t)

    fun back(t: Float): Float = 1f + 2.9f * (t - 1f) * (t - 1f) * (t - 1f) + 1.9f * (t - 1f) * (t - 1f)

    fun boxScale(p: Float): Float = 1f - 0.04f * ez(seg(p, 0.05f, 0.70f))
    fun boxTranslationY(p: Float): Float = -8f * ez(seg(p, 0.05f, 0.70f))
    fun boxBlurDp(p: Float): Float = 8f * ez(seg(p, 0.05f, 0.70f))
    fun boxBrightness(p: Float): Float = 1f - 0.65f * ez(seg(p, 0.05f, 0.70f))
    fun boxAlpha(p: Float): Float = 1f - seg(p, 0.72f, 0.92f)

    fun navLeft(p: Float, startLeft: Float = 12f): Float = startLeft * (1f - ez(seg(p, 0.05f, 0.60f)))
    fun navWidth(p: Float, startW: Float = 336f, endW: Float = 360f): Float =
        startW + (endW - startW) * ez(seg(p, 0.05f, 0.60f))
    fun navHeight(p: Float, startH: Float = 64f, endH: Float = 16f): Float =
        startH + (endH - startH) * ez(seg(p, 0.05f, 0.60f))
    fun navTop(p: Float, startTop: Float, railY: Float): Float =
        startTop + ((railY - 8f) - startTop) * ez(seg(p, 0.05f, 0.60f))
    fun navRadius(p: Float, startR: Float = 32f, endR: Float = 8f): Float =
        startR + (endR - startR) * ez(seg(p, 0.05f, 0.60f))
    fun navContentAlpha(p: Float): Float = 1f - seg(p, 0f, 0.16f)

    fun steelRailAlpha(p: Float): Float = seg(p, 0.50f, 0.62f)

    fun paperClipU(p: Float): Float = out(seg(p, 0.40f, 0.92f))
    fun keyboardTranslationY(p: Float): Float = 240f * (1f - out(seg(p, 0.35f, 0.88f)))

    fun carriageTranslationY(p: Float): Float = -80f * (1f - back(seg(p, 0.72f, 0.96f)))
    fun carriageAlpha(p: Float): Float = seg(p, 0.72f, 0.80f)

    fun islandScale(p: Float): Float = 0.55f + 0.45f * back(seg(p, 0.80f, 1f))
    fun islandAlpha(p: Float): Float = seg(p, 0.80f, 1f)
    fun caretAlpha(p: Float): Float = seg(p, 0.94f, 1f)
}

@Stable
class RailTransitionState(
    initialP: Float = 1f
) {
    val progress = Animatable(initialP)
    var landed by mutableStateOf(initialP >= 0.90f)
        internal set
    private var dingFired = initialP >= 0.90f

    suspend fun animateTo(
        target: Float,
        reduceEffects: Boolean = false,
        onDing: () -> Unit = {}
    ) {
        val forward = target > progress.value
        if (!forward) {
            dingFired = false
            landed = false
        }
        val spec: AnimationSpec<Float> = if (reduceEffects) {
            tween(150)
        } else {
            ShellMotion.railPage.toSpec()
        }

        progress.animateTo(target, spec) {
            if (forward && !dingFired && value >= 0.90f) {
                dingFired = true
                landed = true
                onDing()
            }
        }
        if (target >= 0.90f) {
            landed = true
        } else if (target <= 0.05f) {
            landed = false
            dingFired = false
        }
    }
}
