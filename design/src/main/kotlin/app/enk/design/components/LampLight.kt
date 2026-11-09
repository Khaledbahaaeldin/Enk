package app.enk.design

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp

data class LampSettings(val warmth: Int, val intensity: Int, val beam: Int)

object LampGeometry {
    fun beamBottomYDp(railYDp: Float): Float = railYDp - 24f
    fun haloCenterYDp(railYDp: Float): Float = railYDp - 26f
    fun textMaskCenterDp(carriageXDp: Float, railYDp: Float, paperTopDp: Float): Pair<Float, Float> =
        Pair(carriageXDp - 12f, railYDp - 116f - paperTopDp)
    fun maskEdgeAlpha(intensitySlider: Int): Float = 0.10f + 0.001f * intensitySlider.coerceIn(0, 100)
}

@Composable
fun LampLight(
    carriageXDp: Float,
    railYDp: Float,
    settings: LampSettings,
    visible: Boolean,
    reduceEffects: Boolean,
    modifier: Modifier = Modifier
) {
    val duration = if (reduceEffects) 100 else EnkMotion.themeFadeMs
    val alpha by animateFloatAsState(
        targetValue = if (visible) 1f else 0f,
        animationSpec = tween(durationMillis = duration, easing = LinearEasing),
        label = "lampAlpha"
    )

    if (alpha <= 0.001f) return

    val intensity = LampMath.intensity(settings.intensity)
    val rgb = LampColor.fromWarmth(settings.warmth)
    val lampColor = Color(rgb.r, rgb.g, rgb.b)

    val beamWidthDp = LampMath.beamWidthDp(settings.beam)
    val beamBottomYDp = LampGeometry.beamBottomYDp(railYDp)
    val beamHeightDp = 360f
    val beamTopYDp = beamBottomYDp - beamHeightDp

    val leftXDp = carriageXDp - beamWidthDp / 2f
    val bottomLeftXDp = leftXDp + 0.44f * beamWidthDp
    val bottomRightXDp = leftXDp + 0.56f * beamWidthDp
    val topLeftXDp = leftXDp
    val topRightXDp = leftXDp + beamWidthDp

    val haloCenterYDp = LampGeometry.haloCenterYDp(railYDp)
    val haloRadiusDp = 80f // 160 dp diameter circle

    val blendMode = if (reduceEffects) BlendMode.SrcOver else BlendMode.Screen
    val blurModifier = if (!reduceEffects) Modifier.blur(5.dp) else Modifier

    Canvas(
        modifier = modifier
            .fillMaxSize()
            .then(blurModifier)
    ) {
        val beamPath = Path().apply {
            moveTo(bottomLeftXDp.dp.toPx(), beamBottomYDp.dp.toPx())
            lineTo(topLeftXDp.dp.toPx(), beamTopYDp.dp.toPx())
            lineTo(topRightXDp.dp.toPx(), beamTopYDp.dp.toPx())
            lineTo(bottomRightXDp.dp.toPx(), beamBottomYDp.dp.toPx())
            close()
        }

        val beamBrush = Brush.verticalGradient(
            colorStops = arrayOf(
                0.0f to lampColor.copy(alpha = 0.55f * intensity * alpha),
                0.92f to lampColor.copy(alpha = 0f),
                1.0f to lampColor.copy(alpha = 0f)
            ),
            startY = beamBottomYDp.dp.toPx(),
            endY = beamTopYDp.dp.toPx()
        )

        drawPath(
            path = beamPath,
            brush = beamBrush,
            blendMode = blendMode
        )

        val haloCenter = Offset(carriageXDp.dp.toPx(), haloCenterYDp.dp.toPx())
        val haloRadius = haloRadiusDp.dp.toPx()
        val haloBrush = Brush.radialGradient(
            colorStops = arrayOf(
                0.0f to lampColor.copy(alpha = intensity * 0.55f * alpha),
                0.65f to lampColor.copy(alpha = 0f),
                1.0f to lampColor.copy(alpha = 0f)
            ),
            center = haloCenter,
            radius = haloRadius
        )

        drawCircle(
            brush = haloBrush,
            radius = haloRadius,
            center = haloCenter,
            blendMode = blendMode
        )
    }
}

fun Modifier.lampTextMask(
    carriageXDp: Float,
    railYDp: Float,
    paperTopDp: Float,
    settings: LampSettings,
    active: Boolean
): Modifier {
    if (!active) return this

    return this
        .graphicsLayer {
            compositingStrategy = CompositingStrategy.Offscreen
        }
        .drawWithContent {
            drawContent()

            val (centerXDp, centerYDp) = LampGeometry.textMaskCenterDp(carriageXDp, railYDp, paperTopDp)
            val centerPx = Offset(centerXDp.dp.toPx(), centerYDp.dp.toPx())
            val beamWidthDp = LampMath.beamWidthDp(settings.beam)
            val rx = (0.8f * beamWidthDp).dp.toPx()
            val ry = 250f.dp.toPx()
            val edgeAlpha = LampGeometry.maskEdgeAlpha(settings.intensity)

            val maskBrush = Brush.radialGradient(
                colorStops = arrayOf(
                    0.0f to Color.Black,
                    1.0f to Color.Black.copy(alpha = edgeAlpha)
                ),
                center = centerPx,
                radius = rx
            )

            val scaleY = if (rx > 0f) ry / rx else 1f
            scale(scaleX = 1f, scaleY = scaleY, pivot = centerPx) {
                drawRect(
                    brush = maskBrush,
                    topLeft = Offset(-size.width * 2f, -size.height * 2f),
                    size = Size(size.width * 5f, size.height * 5f),
                    blendMode = BlendMode.DstIn
                )
            }
        }
}
