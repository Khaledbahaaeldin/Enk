package app.enk.design

import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.Easing
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing

object EnkMotion {
    val feed: Easing = CubicBezierEasing(0.25f, 0.8f, 0.3f, 1.0f)
    const val feedMs = 200

    val carriageGlide: Easing = CubicBezierEasing(0.2f, 0.9f, 0.3f, 1.15f)
    const val carriageGlideMs = 110

    val carriageSnap: Easing = CubicBezierEasing(0.3f, 1.7f, 0.5f, 1.0f)
    const val carriageSnapMs = 400

    val slugStrike: Easing = CubicBezierEasing(0.2f, 0.9f, 0.3f, 1.0f)
    const val slugStrikeMs = 110

    val stamp: Easing = FastOutSlowInEasing
    const val stampMs = 160

    const val caretBlinkMs = 1000

    val themeFade: Easing = LinearEasing
    const val themeFadeMs = 600

    val springPanel: Easing = CubicBezierEasing(0.3f, 1.35f, 0.5f, 1.0f)
    const val springPanelMs = 560

    val springRow: Easing = CubicBezierEasing(0.3f, 1.4f, 0.5f, 1.0f)
    const val springRowMs = 550

    val rowAlpha: Easing = LinearEasing
    const val rowAlphaMs = 400

    val panelAlpha: Easing = LinearEasing
    const val panelAlphaMs = 350

    val rowStaggerMs = listOf(120, 200, 280)

    val springKnob: Easing = CubicBezierEasing(0.3f, 1.7f, 0.5f, 1.0f)
    const val springKnobMs = 300

    const val knobLiftMs = 200

    const val longPressMs = 480L
    const val longPressSlopDp = 6f
}
