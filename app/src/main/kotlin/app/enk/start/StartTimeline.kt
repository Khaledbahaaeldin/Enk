package app.enk.start

import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import kotlin.math.hypot

data class StartTargets(
    val railY: Float,
    val screenWidth: Float,
    val screenHeight: Float,
    val carriageX: Float,
    val caretRect: Rect,
    val paperHeight: Float,
    val lightSplash: Boolean
)

data class StartFrame(
    val apertureRadius: Float,
    val rail: Rect,
    val linesAlpha: Float,
    val chassisMix: Float,
    val chassisCircleRadius: Float,
    val paperOffsetY: Float,
    val carriage: CarriageFrame,
    val caret: Rect,
    val textAlpha: Float,
    val topBarAlpha: Float,
    val overlayAlpha: Float,
    val ownPartsAlpha: Float
)

private fun Float.orZero(): Float = if (isFinite()) this else 0f

private fun StartTargets.finite() = StartTargets(
    railY = railY.orZero(),
    screenWidth = screenWidth.orZero().coerceAtLeast(0f),
    screenHeight = screenHeight.orZero().coerceAtLeast(0f),
    carriageX = carriageX.orZero(),
    caretRect = Rect(caretRect.left.orZero(), caretRect.top.orZero(), caretRect.right.orZero(), caretRect.bottom.orZero()),
    paperHeight = paperHeight.orZero().coerceAtLeast(0f),
    lightSplash = lightSplash
)

data class CarriageFrame(val centre: Offset, val scale: Float)

object StartTimeline {
    const val DURATION_MS = 900

    private val easeInOutCubic = CubicBezierEasing(0.65f, 0f, 0.35f, 1f)

    private fun progress(tMs: Float, startMs: Float, endMs: Float): Float {
        if (tMs <= startMs) return 0f
        if (tMs >= endMs) return 1f
        return (tMs - startMs) / (endMs - startMs)
    }

    private fun easedProgress(tMs: Float, startMs: Float, endMs: Float): Float {
        return easeInOutCubic.transform(progress(tMs, startMs, endMs))
    }

    private fun lerp(start: Float, stop: Float, fraction: Float): Float =
        start + (stop - start) * fraction

    /** Total: NaN or infinite times and targets give a finite frame (NaN time is 0, bad targets are 0). */
    fun at(tMs: Float, rawTargets: StartTargets): StartFrame {
        val clampedT = if (tMs.isNaN()) 0f else tMs.coerceIn(0f, DURATION_MS.toFloat())
        val targets = rawTargets.finite()

        // Aperture
        val startAperture = 96f
        val halfDiag = hypot(targets.screenWidth, targets.screenHeight) / 2f
        val endAperture = maxOf(760f, halfDiag + 304f * (targets.screenHeight / 820f))
        val apertureProgress = easedProgress(clampedT, 0f, 350f)
        val apertureRadius = lerp(startAperture, endAperture, apertureProgress)

        // Rail
        val railProgressH = easedProgress(clampedT, 0f, 350f)
        val railLeft = lerp(72f, 0f, railProgressH)
        val railWidth = lerp(256f, targets.screenWidth, railProgressH)

        val railProgressV = easedProgress(clampedT, 250f, 650f)
        val railCenterY = lerp(426.7f, targets.railY, railProgressV)
        val railHeight = lerp(22.7f, 12f, railProgressV)
        val railRect = Rect(
            left = railLeft,
            top = railCenterY - railHeight / 2f,
            right = railLeft + railWidth,
            bottom = railCenterY + railHeight / 2f
        )

        // Lines alpha
        val linesAlpha = lerp(0.34f, 0f, progress(clampedT, 0f, 200f))

        // Chassis mix (dark splash only)
        val chassisMix = if (targets.lightSplash) 0f else easedProgress(clampedT, 60f, 450f)

        // Chassis circle radius (light splash only)
        val chassisCircleRadius = if (!targets.lightSplash || clampedT <= 0f) 0f else apertureRadius

        // Paper translateY
        val paperProgress = easedProgress(clampedT, 150f, 600f)
        val paperOffsetY = lerp(targets.paperHeight, 0f, paperProgress)

        // Carriage
        val carriageProgress = easedProgress(clampedT, 350f, 700f)
        val carriageCentreX = lerp(218.7f, targets.carriageX, carriageProgress)
        val carriageCentreY = lerp(426.7f, targets.railY + 1f, carriageProgress)
        val carriageScale = lerp(1f, 0.54f, carriageProgress)
        val carriage = CarriageFrame(
            centre = Offset(carriageCentreX, carriageCentreY),
            scale = carriageScale
        )

        // Caret
        val caretProgress = easedProgress(clampedT, 300f, 650f)
        val caretRect = Rect(
            left = lerp(204f, targets.caretRect.left, caretProgress),
            top = lerp(359.3f, targets.caretRect.top, caretProgress),
            right = lerp(204f + 29.3f, targets.caretRect.right, caretProgress),
            bottom = lerp(359.3f + 5.6f, targets.caretRect.bottom, caretProgress)
        )

        // Text alpha
        val textAlpha = progress(clampedT, 450f, 700f)

        // Top bar alpha
        val topBarAlpha = progress(clampedT, 600f, 900f)

        // Hand-off swap
        val swapProgress = progress(clampedT, 650f, 750f)
        val ownPartsAlpha = swapProgress
        val overlayAlpha = 1f - swapProgress

        return StartFrame(
            apertureRadius = apertureRadius,
            rail = railRect,
            linesAlpha = linesAlpha,
            chassisMix = chassisMix,
            chassisCircleRadius = chassisCircleRadius,
            paperOffsetY = paperOffsetY,
            carriage = carriage,
            caret = caretRect,
            textAlpha = textAlpha,
            topBarAlpha = topBarAlpha,
            overlayAlpha = overlayAlpha,
            ownPartsAlpha = ownPartsAlpha
        )
    }
}

/** The aperture radius at which the circle (centred on the screen) covers the whole window. */
internal fun StartTargets.coverRadius(): Float =
    kotlin.math.hypot(screenWidth / 2f, screenHeight / 2f)
