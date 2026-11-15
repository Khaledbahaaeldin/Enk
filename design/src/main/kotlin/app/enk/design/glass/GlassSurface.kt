package app.enk.design.glass

import androidx.compose.foundation.background
import androidx.compose.foundation.shape.CornerBasedShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import app.enk.design.effects.LocalEffects
import app.enk.design.effects.ResolvedEffects
import app.enk.design.enkShadow
import app.enk.design.shell.GlassBackendKind
import app.enk.design.shell.GlassLevel
import app.enk.design.shell.GlassSource
import app.enk.design.shell.LocalReduceEffects
import app.enk.design.shell.rememberGlassBackend
import app.enk.design.theme.LocalShellColors
import app.enk.design.theme.LocalShellMaterial
import com.kyant.backdrop.backdrops.emptyBackdrop
import com.kyant.backdrop.drawBackdrop
import com.kyant.backdrop.effects.blur
import com.kyant.backdrop.effects.colorControls
import com.kyant.backdrop.effects.lens
import com.kyant.backdrop.highlight.Highlight
import com.kyant.backdrop.highlight.HighlightStyle
import com.kyant.backdrop.shadow.InnerShadow

class GlassInteraction {
    var pressScale: Float by mutableFloatStateOf(1f)
    var stretchX: Float by mutableFloatStateOf(1f)
    var stretchY: Float by mutableFloatStateOf(1f)
    var specularAngleDeg: Float? by mutableStateOf(null)
}

@Composable
fun Modifier.glassSurface(
    spec: GlassSpec,
    shape: CornerBasedShape,
    source: GlassSource,
    interaction: GlassInteraction? = null
): Modifier {
    val colors = LocalShellColors.current
    val material = LocalShellMaterial.current
    val backend = rememberGlassBackend()
    val level = LocalEffects.current
    val reduceEffects = LocalReduceEffects.current

    if (backend == GlassBackendKind.Solid) {
        val solidModifier = if (spec.tint == GlassTint.Lens) {
            Modifier.background(material.lensA, shape)
        } else {
            Modifier.background(colors.g1, shape)
        }
        val hairlineModifier = Modifier.drawWithContent {
            drawContent()
            val outline = shape.createOutline(size, layoutDirection, this)
            val path = outline.toPath()
            clipPath(path) {
                drawPath(
                    path = path,
                    color = material.glassLine,
                    style = Stroke(width = 0.5.dp.toPx() * 2f)
                )
            }
        }
        val scaleModifier = Modifier.graphicsLayer {
            val ps = if (reduceEffects) 1f else (interaction?.pressScale ?: 1f)
            val sx = if (reduceEffects) 1f else (interaction?.stretchX ?: 1f)
            val sy = if (reduceEffects) 1f else (interaction?.stretchY ?: 1f)
            scaleX = ps * sx
            scaleY = ps * sy
        }
        return this
            .then(scaleModifier)
            .then(solidModifier)
            .then(hairlineModifier)
    }

    val shadowLayers = GlassSpecs.shadowLayers(spec.shadow, material)
    val backdrop = source.backdrop ?: emptyBackdrop()

    return this
        .enkShadow(shadowLayers, shape)
        .drawBackdrop(
            backdrop = backdrop,
            shape = { shape },
            effects = {
                colorControls(saturation = spec.saturation)
                blur(spec.blurDp.dp.toPx())
                if (backend == GlassBackendKind.Refraction && spec.lensHeightDp > 0f) {
                    val widthDp = size.width.toDp().value
                    val heightDp = size.height.toDp().value
                    val topStartPx = shape.topStart.toPx(size, this)
                    val topEndPx = shape.topEnd.toPx(size, this)
                    val bottomEndPx = shape.bottomEnd.toPx(size, this)
                    val bottomStartPx = shape.bottomStart.toPx(size, this)
                    val minCornerPx = minOf(topStartPx, topEndPx, bottomEndPx, bottomStartPx)
                    val minCornerDp = minCornerPx.toDp().value
                    val (hDp, aDp) = GlassLensMath.coerceLens(spec, widthDp, heightDp, minCornerDp)
                    if (hDp > 0f && aDp > 0f) {
                        lens(
                            refractionHeight = hDp.dp.toPx(),
                            refractionAmount = aDp.dp.toPx(),
                            depthEffect = spec.depth,
                            chromaticAberration = spec.fringe
                        )
                    }
                }
            },
            highlight = {
                if (backend == GlassBackendKind.Refraction) {
                    Highlight(
                        width = spec.highlightWidthDp.dp,
                        style = HighlightStyle.Default(
                            color = material.glassEdge,
                            angle = interaction?.specularAngleDeg ?: 45f
                        )
                    )
                } else {
                    Highlight.Plain.copy(width = spec.highlightWidthDp.dp)
                }
            },
            shadow = null,
            innerShadow = {
                if (backend == GlassBackendKind.Refraction && spec.tint == GlassTint.Lens) {
                    InnerShadow(
                        radius = 6.dp,
                        offset = DpOffset(0.dp, 2.dp),
                        color = material.glassEdge,
                        alpha = 0.35f
                    )
                } else {
                    null
                }
            },
            layerBlock = {
                val i = interaction
                val ps = i?.pressScale ?: 1f
                val sx = i?.stretchX ?: 1f
                val sy = i?.stretchY ?: 1f
                scaleX = ps * sx
                scaleY = ps * sy
            },
            onDrawSurface = {
                if (spec.tint != GlassTint.None) {
                    val outline = shape.createOutline(size, layoutDirection, this)
                    val shapePath = outline.toPath()
                    clipPath(shapePath) {
                        when (spec.tint) {
                            GlassTint.Regular -> drawRect(brush = Brush.verticalGradient(listOf(material.glassA, material.glassB)))
                            GlassTint.Thick -> drawRect(color = colors.thick)
                            GlassTint.Lens -> drawRect(brush = Brush.verticalGradient(listOf(material.lensA, material.lensB)))
                            GlassTint.None -> {}
                        }
                    }
                }
            },
            onDrawFront = {
                val outline = shape.createOutline(size, layoutDirection, this)
                val shapePath = outline.toPath()
                clipPath(shapePath) {
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

internal fun Outline.toPath(): Path = Path().apply {
    when (val outline = this@toPath) {
        is Outline.Rectangle -> addRect(outline.rect)
        is Outline.Rounded -> addRoundRect(outline.roundRect)
        is Outline.Generic -> addPath(outline.path)
    }
}
