package app.enk.design.shell

import android.os.Build
import androidx.compose.foundation.background
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathOperation
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.asAndroidPath
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import app.enk.design.ShadowLayer
import app.enk.design.enkShadow
import app.enk.design.theme.LocalShellColors
import app.enk.design.theme.LocalShellMaterial
import com.kyant.backdrop.backdrops.emptyBackdrop
import com.kyant.backdrop.backdrops.LayerBackdrop
import com.kyant.backdrop.backdrops.layerBackdrop
import com.kyant.backdrop.backdrops.rememberLayerBackdrop
import com.kyant.backdrop.drawPlainBackdrop
import com.kyant.backdrop.effects.blur
import com.kyant.backdrop.effects.colorControls
import com.kyant.backdrop.effects.lens

class GlassSource(backdrop: LayerBackdrop? = null) {
    var backdrop: LayerBackdrop? = backdrop
        internal set
}

@Composable
fun rememberGlassSource(): GlassSource {
    val backdrop = rememberLayerBackdrop()
    return remember(backdrop) { GlassSource(backdrop) }
}

fun Modifier.glassSource(source: GlassSource): Modifier = composed {
    val backdrop = source.backdrop ?: rememberLayerBackdrop().also { source.backdrop = it }
    this.layerBackdrop(backdrop)
}

@Composable
fun Modifier.glass(
    level: GlassLevel,
    shape: Shape,
    source: GlassSource
): Modifier {
    val context = LocalContext.current
    val colors = LocalShellColors.current
    val material = LocalShellMaterial.current
    val reduceEffects = LocalReduceEffects.current || isReducedMotion(context)
    val highContrastText = isHighContrastTextEnabled(context)
    val backend = chooseGlassBackend(Build.VERSION.SDK_INT, reduceEffects, highContrastText)

    if (backend == GlassBackendKind.Solid) {
        return this.background(colors.g1, shape)
    }

    val shadowLayers = if (level == GlassLevel.Regular) {
        listOf(
            ShadowLayer(0.dp, 1.dp, 2.dp, 0.dp, material.sh1),
            ShadowLayer(0.dp, 14.dp, 30.dp, (-8).dp, material.sh2)
        )
    } else {
        listOf(
            ShadowLayer(0.dp, 2.dp, 6.dp, 0.dp, material.sh1),
            ShadowLayer(0.dp, 30.dp, 60.dp, (-12).dp, material.sh2)
        )
    }

    val blurRadius = if (level == GlassLevel.Regular) 24.dp else 44.dp
    val saturation = if (level == GlassLevel.Regular) 1.5f else 1.6f
    val backdrop = source.backdrop ?: emptyBackdrop()

    return this
        .enkShadow(shadowLayers, shape)
        .drawPlainBackdrop(
            backdrop = backdrop,
            shape = { shape },
            effects = {
                blur(blurRadius.toPx())
                colorControls(saturation = saturation)
                if (backend == GlassBackendKind.Refraction) {
                    lens(
                        refractionHeight = blurRadius.toPx(),
                        refractionAmount = 32f
                    )
                }
            },
            onDrawFront = {
                val outline = shape.createOutline(size, layoutDirection, this)
                val shapePath = Path().apply {
                    when (outline) {
                        is Outline.Rectangle -> addRect(outline.rect)
                        is Outline.Rounded -> addRoundRect(outline.roundRect)
                        is Outline.Generic -> addPath(outline.path)
                    }
                }

                clipPath(shapePath) {
                    // 1. Fill
                    if (level == GlassLevel.Regular) {
                        val fillBrush = Brush.verticalGradient(
                            listOf(material.glassA, material.glassB)
                        )
                        drawRect(brush = fillBrush)
                    } else {
                        drawRect(color = colors.thick)
                    }

                    // 2. Inset top highlight: 0.5 dp glassEdge
                    val highlightPx = 0.5.dp.toPx()
                    val translatedHole = Path().apply {
                        addPath(shapePath)
                        asAndroidPath().offset(0f, highlightPx)
                    }
                    val topHighlightPath = Path().apply {
                        op(shapePath, translatedHole, PathOperation.Difference)
                    }
                    drawPath(topHighlightPath, color = material.glassEdge)

                    // 3. Inset outline: 0.5 dp glassLine
                    val strokeWidthPx = 0.5.dp.toPx()
                    drawPath(
                        path = shapePath,
                        color = material.glassLine,
                        style = Stroke(width = strokeWidthPx * 2f)
                    )
                }
            }
        )
}
