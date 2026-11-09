package app.enk.start

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
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
import kotlinx.coroutines.launch

private val BODY = Rect(-15f, -10f, 15f, 11f)
private val TOP_COLLAR = Rect(-15f, -10f, 15f, -7f)
private val BOTTOM_COLLAR = Rect(-15f, 8f, 15f, 11f)
private val SLUG = Rect(-3.5f, -14.6f, 3.5f, -9.6f)
private val SLUG_SLEEVE = Rect(-5.2f, -16.4f, 5.2f, -9.4f)
private val STRIP = Rect(-4.3f, -4.7f, 4.3f, -2.8f)
private val SLEEVE_INK = Color(0xFF141312)

@Composable
fun StartOverlay(
    targets: StartTargets,
    modifier: Modifier = Modifier,
    reduceMotion: Boolean = false,
    timeMs: Float? = null,
    onFinished: () -> Unit = {}
) {
    val coroutineScope = rememberCoroutineScope()
    val anim = remember { Animatable(0f) }
    var isSkipping by remember { mutableStateOf(false) }
    val density = LocalDensity.current.density

    fun skip() {
        if (isSkipping || timeMs != null) return
        isSkipping = true
        coroutineScope.launch {
            val remaining = 1f - anim.value
            if (remaining > 0f) {
                anim.animateTo(
                    targetValue = 1f,
                    animationSpec = tween(
                        durationMillis = 120,
                        easing = LinearEasing
                    )
                )
            }
            onFinished()
        }
    }

    BackHandler(enabled = timeMs == null) {
        skip()
    }

    LaunchedEffect(reduceMotion, timeMs) {
        if (timeMs == null) {
            val duration = if (reduceMotion) {
                StartGate.reducedMotionDurationMs()
            } else {
                StartTimeline.DURATION_MS
            }
            anim.animateTo(
                targetValue = 1f,
                animationSpec = tween(durationMillis = duration, easing = LinearEasing)
            )
            onFinished()
        }
    }

    Canvas(
        modifier = Modifier
            .fillMaxSize()
            .testTag("start_overlay")
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) {
                skip()
            }
            .then(modifier)
    ) {
        val currentTMs = timeMs ?: (anim.value * StartTimeline.DURATION_MS)
        val frame = StartTimeline.at(currentTMs, targets)

        val alpha = if (reduceMotion && timeMs == null) {
            (1f - anim.value).coerceIn(0f, 1f)
        } else {
            1f
        }

        if (alpha <= 0.001f) return@Canvas

        val widthDp = size.width / density
        val heightDp = size.height / density
        val centerDp = Offset(widthDp / 2f, heightDp / 2f)

        withTransform({
            scale(density, density, Offset.Zero)
        }) {
            // 1. Splash background & chassis
            if (targets.lightSplash) {
                drawRect(color = Color(0xFFECE8DF), alpha = alpha, size = Size(widthDp, heightDp))
                if (frame.chassisCircleRadius > 0f) {
                    drawCircle(
                        brush = EnkMaterials.chassisDay,
                        radius = frame.chassisCircleRadius,
                        center = centerDp,
                        alpha = alpha
                    )
                }
            } else {
                drawRect(color = Color(0xFF161514), alpha = alpha, size = Size(widthDp, heightDp))
                if (frame.chassisMix > 0f) {
                    drawRect(
                        brush = EnkMaterials.chassisDay,
                        alpha = frame.chassisMix * alpha,
                        size = Size(widthDp, heightDp)
                    )
                }
            }

            // 2. Aperture clip
            val aperturePath = Path().apply {
                addOval(Rect(center = centerDp, radius = frame.apertureRadius))
            }

            val overlayPartsAlpha = if (reduceMotion && timeMs == null) {
                alpha
            } else if (timeMs != null) {
                1f
            } else {
                frame.overlayAlpha * alpha
            }

            if (overlayPartsAlpha > 0.001f) {
                clipPath(aperturePath) {
                    // Typed lines
                    if (frame.linesAlpha > 0.001f) {
                        val lineColor = if (targets.lightSplash) {
                            Color(0xFF2B2824).copy(alpha = frame.linesAlpha * overlayPartsAlpha)
                        } else {
                            LogoSpec.CREAM.copy(alpha = frame.linesAlpha * overlayPartsAlpha)
                        }
                        val u = 288f / 108f
                        val sx = centerDp.x - 144f
                        val sy = centerDp.y - 144f
                        translate(sx, sy) {
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

                    // Rail
                    drawRail(frame.rail, overlayPartsAlpha)

                    // Caret
                    drawCaret(frame.caret, overlayPartsAlpha)

                    // Carriage
                    drawCarriage(frame.carriage.centre, frame.carriage.scale, overlayPartsAlpha)
                }
            }
        }
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
