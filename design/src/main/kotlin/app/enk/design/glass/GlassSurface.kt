package app.enk.design.glass

import androidx.compose.foundation.background
import androidx.compose.foundation.shape.CornerBasedShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import app.enk.design.effects.GlassLook
import app.enk.design.effects.LocalEffects
import app.enk.design.effects.LocalFrostTuning
import app.enk.design.effects.LocalGlassLook
import app.enk.design.effects.LocalLiquidTuning
import app.enk.design.effects.ResolvedEffects
import app.enk.design.enkShadow
import app.enk.design.shell.GlassBackendKind
import app.enk.design.shell.GlassSource
import app.enk.design.shell.LocalReduceEffects
import app.enk.design.shell.rememberGlassBackend
import app.enk.design.theme.LocalShellColors
import app.enk.design.theme.LocalShellMaterial
import com.kyant.backdrop.BackdropEffectScope
import com.kyant.backdrop.backdrops.LayerBackdrop
import com.kyant.backdrop.backdrops.emptyBackdrop
import com.kyant.backdrop.backdrops.rememberCombinedBackdrop
import com.kyant.backdrop.drawBackdrop
import com.kyant.backdrop.effects.blur
import com.kyant.backdrop.effects.colorControls
import com.kyant.backdrop.effects.lens
import com.kyant.backdrop.effects.vibrancy
import com.kyant.backdrop.highlight.Highlight
import com.kyant.backdrop.highlight.HighlightStyle
import com.kyant.backdrop.shadow.InnerShadow
import com.kyant.backdrop.shadow.Shadow
import kotlin.math.atan2

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
    interaction: GlassInteraction? = null,
    solidColor: Color? = null
): Modifier = glassSurface(
    spec = spec,
    liquid = LiquidSpecs.Panel,
    shape = shape,
    source = source,
    interaction = interaction,
    solidColor = solidColor,
    press = null,
    exportedBackdrop = null,
    extraSource = null
)

@Composable
fun Modifier.glassSurface(
    spec: GlassSpec,
    liquid: LiquidSpec = LiquidSpecs.Panel,
    shape: CornerBasedShape,
    source: GlassSource,
    interaction: GlassInteraction? = null,
    solidColor: Color? = null,
    press: LiquidPressState? = null,
    exportedBackdrop: LayerBackdrop? = null,
    extraSource: GlassSource? = null
): Modifier {
    val colors = LocalShellColors.current
    val material = LocalShellMaterial.current
    val backend = rememberGlassBackend()
    val level = LocalEffects.current
    val reduceEffects = LocalReduceEffects.current
    val glassLook = LocalGlassLook.current
    val liquidTuning = LocalLiquidTuning.current
    val frostTuning = LocalFrostTuning.current
    val density = LocalDensity.current.density
    val isLight = level == ResolvedEffects.Light || reduceEffects

    if (backend == GlassBackendKind.Solid) {
        val solidModifier = if (solidColor != null) {
            Modifier.background(solidColor, shape)
        } else if (spec.tint == GlassTint.Lens) {
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
            if (press != null && !isLight) {
                val p = press.pressProgress
                val swell = LiquidPressMath.swellScale(size.height, density, p)
                val (sxAdd, syAdd) = LiquidPressMath.stretch(press.offset, size, density)
                val maxDim = maxOf(size.width, size.height)
                scaleX = ps * sx * (swell + sxAdd)
                scaleY = ps * sy * (swell + syAdd)
                translationX = LiquidPressMath.lean(press.offset.x, maxDim)
                translationY = LiquidPressMath.lean(press.offset.y, maxDim)
            } else {
                scaleX = ps * sx
                scaleY = ps * sy
            }
        }
        return this
            .then(scaleModifier)
            .then(solidModifier)
            .then(hairlineModifier)
    }

    val baseBackdrop = source.backdrop ?: emptyBackdrop()
    val backdrop = if (extraSource?.backdrop != null) {
        rememberCombinedBackdrop(baseBackdrop, extraSource.backdrop!!)
    } else {
        baseBackdrop
    }

    val isLiquid = glassLook == GlassLook.Liquid && backend == GlassBackendKind.Refraction
    val isBalanced = level == ResolvedEffects.Balanced

    val layerBlockLambda: androidx.compose.ui.graphics.GraphicsLayerScope.() -> Unit = {
        val i = interaction
        val ps = i?.pressScale ?: 1f
        val sx = i?.stretchX ?: 1f
        val sy = i?.stretchY ?: 1f
        if (press != null && !isLight) {
            val p = press.pressProgress
            val swell = LiquidPressMath.swellScale(size.height, density, p)
            val (sxAdd, syAdd) = LiquidPressMath.stretch(press.offset, size, density)
            val maxDim = maxOf(size.width, size.height)
            scaleX = ps * sx * (swell + sxAdd)
            scaleY = ps * sy * (swell + syAdd)
            translationX = LiquidPressMath.lean(press.offset.x, maxDim)
            translationY = LiquidPressMath.lean(press.offset.y, maxDim)
        } else {
            scaleX = ps * sx
            scaleY = ps * sy
        }
    }

    if (isLiquid) {
        val resolvedLiquid = if (isBalanced) LiquidSpecs.balanced(liquid) else liquid
        val tunedLiquid = LiquidSpecs.tuned(resolvedLiquid, liquidTuning)
        val isPressedSpec = liquid == LiquidSpecs.BarLens || liquid == LiquidSpecs.Thumb
        val p = press?.pressProgress ?: 0f

        val actualBlurDp = if (press != null && isPressedSpec) {
            8f * (1f - p) + tunedLiquid.blurDp * p
        } else {
            tunedLiquid.blurDp
        }

        val actualLensHeightDp = if (press != null && isPressedSpec) {
            tunedLiquid.lensHeightDp * p
        } else {
            tunedLiquid.lensHeightDp
        }

        val actualLensAmountDp = if (press != null && isPressedSpec) {
            tunedLiquid.lensAmountDp * p
        } else {
            tunedLiquid.lensAmountDp
        }

        val effectsLambda: BackdropEffectScope.() -> Unit = {
            if (tunedLiquid.vibrancy) {
                vibrancy()
            }
            if (actualBlurDp > 0f) {
                blur(actualBlurDp.dp.toPx())
            }
            if (actualLensHeightDp > 0f && actualLensAmountDp > 0f) {
                lens(
                    refractionHeight = actualLensHeightDp.dp.toPx(),
                    refractionAmount = actualLensAmountDp.dp.toPx(),
                    depthEffect = tunedLiquid.depth,
                    chromaticAberration = tunedLiquid.fringe
                )
            }
        }

        val highlightLambda: () -> Highlight = {
            val edgeAlpha = liquidTuning.edgeLight.coerceIn(0f, 1.5f)
            when (tunedLiquid.rim) {
                RimLight.Ambient -> {
                    Highlight.Ambient.copy(
                        alpha = (Highlight.Ambient.alpha * edgeAlpha).coerceIn(0f, 1f)
                    )
                }
                RimLight.Directional -> {
                    val angle = interaction?.specularAngleDeg
                        ?: if (press != null && press.offset != Offset.Zero) {
                            Math.toDegrees(atan2(press.offset.y.toDouble(), press.offset.x.toDouble())).toFloat()
                        } else {
                            45f
                        }
                    Highlight(
                        style = HighlightStyle.Default(
                            color = material.glassEdge,
                            angle = angle
                        ),
                        alpha = (1f * edgeAlpha).coerceIn(0f, 1f)
                    )
                }
                RimLight.Plain -> {
                    Highlight.Plain.copy(
                        alpha = (Highlight.Plain.alpha * edgeAlpha).coerceIn(0f, 1f)
                    )
                }
            }
        }

        val shadowLambda: () -> Shadow? = {
            if (tunedLiquid.shadowDp > 0f) {
                if (isPressedSpec) {
                    val prog = press?.pressProgress ?: 0f
                    Shadow(
                        radius = (tunedLiquid.shadowDp * prog).dp,
                        color = Color.Black.copy(alpha = 0.05f * prog)
                    )
                } else {
                    Shadow(
                        radius = tunedLiquid.shadowDp.dp,
                        color = Color.Black.copy(alpha = 0.05f)
                    )
                }
            } else {
                null
            }
        }

        val innerShadowLambda: () -> InnerShadow? = {
            if (tunedLiquid.innerShadowDp > 0f) {
                val prog = press?.pressProgress ?: 1f
                InnerShadow(
                    radius = (tunedLiquid.innerShadowDp * prog).dp,
                    color = material.glassEdge,
                    alpha = (0.35f * prog).coerceIn(0f, 1f)
                )
            } else {
                null
            }
        }

        val onDrawSurfaceLambda: DrawScope.() -> Unit = {
            val outline = shape.createOutline(size, layoutDirection, this)
            val shapePath = outline.toPath()
            clipPath(shapePath) {
                if (solidColor != null) {
                    drawRect(color = solidColor)
                } else if (press != null && isPressedSpec) {
                    val prog = press.pressProgress
                    drawRect(color = Color.White.copy(alpha = 1f - prog))
                } else {
                    drawRect(color = colors.g1.copy(alpha = liquidTuning.tint.coerceIn(0f, 1f)))
                }
            }
        }

        return this.drawBackdrop(
            backdrop = backdrop,
            shape = { shape },
            effects = effectsLambda,
            highlight = highlightLambda,
            shadow = shadowLambda,
            innerShadow = innerShadowLambda,
            layerBlock = layerBlockLambda,
            exportedBackdrop = exportedBackdrop,
            onDrawSurface = onDrawSurfaceLambda,
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

    // Frost look
    val tunedBlurDp = spec.blurDp * frostTuning.blur
    val shadowLayers = GlassSpecs.shadowLayers(spec.shadow, material)

    return this
        .enkShadow(shadowLayers, shape)
        .drawBackdrop(
            backdrop = backdrop,
            shape = { shape },
            effects = {
                colorControls(saturation = spec.saturation)
                blur(tunedBlurDp.dp.toPx())
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
                val edgeAlpha = frostTuning.edgeLight.coerceIn(0f, 1.5f)
                if (backend == GlassBackendKind.Refraction) {
                    Highlight(
                        width = spec.highlightWidthDp.dp,
                        style = HighlightStyle.Default(
                            color = material.glassEdge,
                            angle = interaction?.specularAngleDeg ?: 45f
                        ),
                        alpha = (1f * edgeAlpha).coerceIn(0f, 1f)
                    )
                } else {
                    Highlight.Plain.copy(
                        width = spec.highlightWidthDp.dp,
                        alpha = (Highlight.Plain.alpha * edgeAlpha).coerceIn(0f, 1f)
                    )
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
            layerBlock = layerBlockLambda,
            exportedBackdrop = exportedBackdrop,
            onDrawSurface = {
                val outline = shape.createOutline(size, layoutDirection, this)
                val shapePath = outline.toPath()
                clipPath(shapePath) {
                    if (solidColor != null) {
                        drawRect(color = solidColor)
                    } else if (spec.tint != GlassTint.None) {
                        val tintMult = frostTuning.tint.coerceIn(0.5f, 1.5f)
                        when (spec.tint) {
                            GlassTint.Regular -> drawRect(
                                brush = Brush.verticalGradient(
                                    listOf(
                                        material.glassA.copy(alpha = (material.glassA.alpha * tintMult).coerceIn(0f, 1f)),
                                        material.glassB.copy(alpha = (material.glassB.alpha * tintMult).coerceIn(0f, 1f))
                                    )
                                )
                            )
                            GlassTint.Thick -> drawRect(color = colors.thick.copy(alpha = (colors.thick.alpha * tintMult).coerceIn(0f, 1f)))
                            GlassTint.Lens -> drawRect(
                                brush = Brush.verticalGradient(
                                    listOf(
                                        material.lensA.copy(alpha = (material.lensA.alpha * tintMult).coerceIn(0f, 1f)),
                                        material.lensB.copy(alpha = (material.lensB.alpha * tintMult).coerceIn(0f, 1f))
                                    )
                                )
                            )
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
