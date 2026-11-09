package app.enk.writer

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.ui.graphics.Color
import app.enk.design.EnkColors
import app.enk.design.EnkMotion
import app.enk.design.LampSettings
import app.enk.design.Rgb
import app.enk.settings.Settings

fun paperColor(nightProgress: Float): Color {
    val p = nightProgress.coerceIn(0f, 1f)
    val start = EnkColors.paperDay
    val stop = EnkColors.paperNight
    return Color(
        red = start.red + (stop.red - start.red) * p,
        green = start.green + (stop.green - start.green) * p,
        blue = start.blue + (stop.blue - start.blue) * p,
        alpha = start.alpha + (stop.alpha - start.alpha) * p
    )
}

fun textColor(nightProgress: Float, lamp: Rgb): Color {
    val p = nightProgress.coerceIn(0f, 1f)
    val start = EnkColors.ink
    val stop = Color(red = lamp.r, green = lamp.g, blue = lamp.b)
    return Color(
        red = start.red + (stop.red - start.red) * p,
        green = start.green + (stop.green - start.green) * p,
        blue = start.blue + (stop.blue - start.blue) * p,
        alpha = start.alpha + (stop.alpha - start.alpha) * p
    )
}

fun grainOpacity(nightProgress: Float): Float {
    val p = nightProgress.coerceIn(0f, 1f)
    return 0.35f + (0.10f - 0.35f) * p
}

fun lampSettingsFromSettings(settings: Settings): LampSettings =
    LampSettings(
        warmth = settings.warmth,
        intensity = settings.intensity,
        beam = settings.beam
    )

@Composable
fun rememberNightProgress(lampOn: Boolean, reduceEffects: Boolean): State<Float> {
    val duration = if (reduceEffects) 100 else EnkMotion.themeFadeMs
    return animateFloatAsState(
        targetValue = if (lampOn) 1f else 0f,
        animationSpec = tween(durationMillis = duration, easing = LinearEasing),
        label = "nightProgress"
    )
}
