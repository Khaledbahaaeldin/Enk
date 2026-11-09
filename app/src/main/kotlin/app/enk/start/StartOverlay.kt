package app.enk.start

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import app.enk.design.EnkColors
import app.enk.design.EnkMaterials
import app.enk.design.logo.LogoSpec

private val BODY = Rect(-15f, -10f, 15f, 11f)
private val TOP_COLLAR = Rect(-15f, -10f, 15f, -7f)
private val BOTTOM_COLLAR = Rect(-15f, 8f, 15f, 11f)
private val SLUG = Rect(-3.5f, -14.6f, 3.5f, -9.6f)
private val SLUG_SLEEVE = Rect(-5.2f, -16.4f, 5.2f, -9.4f)
private val STRIP = Rect(-4.3f, -4.7f, 4.3f, -2.8f)
private val SLEEVE_INK = Color(0xFF141312)

private val SPLASH_DARK = Color(0xFF161514)
private val SPLASH_LIGHT = Color(0xFFECE8DF)
private val TYPED_LINE_INK = Color(0xFF2B2824)

/**
 * The layer under the writer: the splash background and, as it opens, the writer's chassis. The
 * writer's own chassis is held back while the animation runs, so the rising paper lands on this.
 */
@Composable
internal fun StartBackdrop(frame: State<StartFrame>, targets: StartTargets, modifier: Modifier = Modifier) {
    Canvas(modifier = modifier.fillMaxSize().testTag("start_backdrop")) {
        inDp { widthDp, heightDp -> drawBackdrop(frame.value, targets, Size(widthDp, heightDp), alpha = 1f) }
    }
}

/**
 * The layer above the writer: the logo's typed lines, rail, caret and carriage, clipped by the
 * aperture, handing over to the writer's own parts. Any touch skips the animation.
 */
@Composable
internal fun StartOverlay(
    frame: State<StartFrame>,
    targets: StartTargets,
    onSkip: () -> Unit,
    modifier: Modifier = Modifier
) {
    Canvas(
        modifier = modifier
            .fillMaxSize()
            .testTag("start_overlay")
            .pointerInput(Unit) {
                awaitEachGesture {
                    awaitFirstDown().consume()
                    onSkip()
                }
            }
    ) {
        inDp { widthDp, heightDp ->
            val f = frame.value
            drawSymbol(f, targets, Offset(widthDp / 2f, heightDp / 2f), partsAlpha = f.overlayAlpha, clipToAperture = true)
        }
    }
}

/**
 * Reduced motion: the splash is shown still and cross-fades away over the finished writer, with
 * no movement at all.
 */
@Composable
internal fun StartSplashFade(
    targets: StartTargets,
    alpha: () -> Float,
    onSkip: () -> Unit,
    modifier: Modifier = Modifier
) {
    val still = StartTimeline.at(0f, targets)
    Canvas(
        modifier = modifier
            .fillMaxSize()
            .testTag("start_overlay")
            .pointerInput(Unit) {
                awaitEachGesture {
                    awaitFirstDown().consume()
                    onSkip()
                }
            }
    ) {
        inDp { widthDp, heightDp ->
            val a = alpha().coerceIn(0f, 1f)
            if (a <= 0.001f) return@inDp
            drawBackdrop(still, targets, Size(widthDp, heightDp), alpha = a)
            drawSymbol(still, targets, Offset(widthDp / 2f, heightDp / 2f), partsAlpha = a, clipToAperture = true)
        }
    }
}

/** Draws in dp: the canvas is in pixels, the timeline in dp. */
private inline fun DrawScope.inDp(block: DrawScope.(widthDp: Float, heightDp: Float) -> Unit) {
    val density = this.density
    withTransform({ scale(density, density, Offset.Zero) }) {
        block(size.width / density, size.height / density)
    }
}

private fun DrawScope.drawBackdrop(frame: StartFrame, targets: StartTargets, window: Size, alpha: Float) {
    val centre = Offset(window.width / 2f, window.height / 2f)
    if (targets.lightSplash) {
        drawRect(color = SPLASH_LIGHT, alpha = alpha, size = window)
        if (frame.chassisCircleRadius > 0f) {
            drawCircle(brush = EnkMaterials.chassisDay, radius = frame.chassisCircleRadius, center = centre, alpha = alpha)
        }
    } else {
        drawRect(color = SPLASH_DARK, alpha = alpha, size = window)
        if (frame.chassisMix > 0f) {
            drawRect(brush = EnkMaterials.chassisDay, alpha = frame.chassisMix * alpha, size = window)
        }
    }
}

private fun DrawScope.drawSymbol(
    frame: StartFrame,
    targets: StartTargets,
    centre: Offset,
    partsAlpha: Float,
    clipToAperture: Boolean
) {
    if (partsAlpha <= 0.001f) return
    val draw: DrawScope.() -> Unit = {
        if (frame.linesAlpha > 0.001f) {
            val lineColor = (if (targets.lightSplash) TYPED_LINE_INK else LogoSpec.CREAM)
                .copy(alpha = frame.linesAlpha * partsAlpha)
            val u = 288f / 108f
            translate(centre.x - 144f, centre.y - 144f) {
                LogoSpec.typedLines.forEach { line ->
                    drawRoundRect(
                        color = lineColor,
                        topLeft = Offset(line.left * u, line.top * u),
                        size = Size(line.width * u, line.height * u),
                        cornerRadius = CornerRadius(1.3f * u)
                    )
                }
            }
        }
        drawRail(frame.rail, partsAlpha)
        drawCaret(frame.caret, partsAlpha)
        drawCarriage(frame.carriage.centre, frame.carriage.scale, partsAlpha)
    }
    if (clipToAperture) {
        val aperture = Path().apply { addOval(Rect(center = centre, radius = frame.apertureRadius)) }
        clipPath(aperture) { draw() }
    } else {
        draw()
    }
}

private fun DrawScope.drawRail(rail: Rect, alpha: Float = 1f) {
    drawRect(
        color = Color.Black.copy(alpha = 0.42f * alpha),
        topLeft = Offset(rail.left, rail.top + 1.5f),
        size = Size(rail.width, 4f)
    )
    fillRect(rail, EnkMaterials.railSteel, alpha = alpha)
}

private fun DrawScope.drawCaret(caret: Rect, alpha: Float = 1f) {
    val corner = (caret.height * 0.4f).coerceIn(0.5f, 2.0f)
    drawRoundRect(
        color = EnkColors.accent.copy(alpha = alpha),
        topLeft = caret.topLeft,
        size = caret.size,
        cornerRadius = CornerRadius(corner)
    )
}

private fun DrawScope.drawCarriage(centre: Offset, scale: Float, alpha: Float = 1f) {
    val totalScale = (288f / 108f) * LogoSpec.CARRIAGE_SCALE * scale
    withTransform({
        translate(centre.x, centre.y)
        scale(totalScale, totalScale, Offset.Zero)
    }) {
        drawOval(
            color = Color.Black.copy(alpha = 0.55f * alpha),
            topLeft = Offset(-14.5f, 12f),
            size = Size(29f, 6f)
        )
        drawRoundRect(
            color = SLEEVE_INK.copy(alpha = alpha),
            topLeft = SLUG_SLEEVE.topLeft,
            size = Size(SLUG_SLEEVE.width, 7f),
            cornerRadius = CornerRadius(1.7f)
        )
        fillRect(SLUG, EnkMaterials.slugSteel, corner = 1.0f, alpha = alpha)

        val bodyPath = Path().apply { addRoundRect(RoundRect(BODY, CornerRadius(4.5f))) }
        clipPath(bodyPath) {
            fillRect(BODY, EnkMaterials.graphiteCap, alpha = alpha)
            fillRect(TOP_COLLAR, EnkMaterials.collarTop, alpha = alpha)
            fillRect(BOTTOM_COLLAR, EnkMaterials.collarBottom, alpha = alpha)
        }

        drawLine(
            color = Color.White.copy(alpha = 0.17f * alpha),
            start = Offset(-8.4f, 4.1f),
            end = Offset(8.4f, 4.1f),
            strokeWidth = 2.5f,
            pathEffect = PathEffect.dashPathEffect(floatArrayOf(0.9f, 1.7f), 0f)
        )
        drawRoundRect(
            color = EnkColors.accent.copy(alpha = alpha),
            topLeft = STRIP.topLeft,
            size = STRIP.size,
            cornerRadius = CornerRadius(0.95f)
        )
    }
}

private fun DrawScope.fillRect(rect: Rect, brush: Brush, corner: Float = 0f, alpha: Float = 1f) {
    val canvasSize = drawContext.size
    drawContext.size = rect.size
    translate(rect.left, rect.top) {
        drawRoundRect(
            brush = brush,
            size = rect.size,
            cornerRadius = CornerRadius(corner),
            alpha = alpha
        )
    }
    drawContext.size = canvasSize
}
