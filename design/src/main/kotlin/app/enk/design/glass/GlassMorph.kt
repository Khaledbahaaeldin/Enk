package app.enk.design.glass

import androidx.compose.ui.unit.Density
import androidx.compose.ui.geometry.Size
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.shape.CornerSize
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationSpec
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.layout
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.DpRect
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.height
import androidx.compose.ui.unit.width
import app.enk.design.effects.LocalEffects
import app.enk.design.effects.ResolvedEffects
import app.enk.design.motion.ShellMotion
import app.enk.design.shell.GlassSource
import app.enk.design.shell.LocalReduceEffects
import app.enk.design.theme.LocalShellColors
import kotlin.math.roundToInt

data class MorphFrame(
    val widthDp: Float,
    val heightDp: Float,
    val radiusDp: Float,
    val offsetXDp: Float,
    val offsetYDp: Float,   // from the collapsed top-left corner
    val labelAlpha: Float,
    val itemAlpha: List<Float>,
    val itemDyDp: List<Float>
)

fun morphFrame(
    p: Float,
    collapsed: DpSize,
    expanded: DpSize,
    collapsedRadiusDp: Float,
    expandedRadiusDp: Float,
    target: DpOffset,        // where the expanded top-left ends up, relative to the collapsed top-left
    itemCount: Int
): MorphFrame {
    val safeP = if (p.isNaN()) 0f else p
    val cw = collapsed.width.value
    val ew = expanded.width.value
    val ch = collapsed.height.value
    val eh = expanded.height.value

    val width = maxOf(0f, cw + (ew - cw) * safeP)
    val height = maxOf(0f, ch + (eh - ch) * safeP)
    val pClamped = safeP.coerceIn(0f, 1f)
    val radius = collapsedRadiusDp + (expandedRadiusDp - collapsedRadiusDp) * pClamped

    val offsetX = target.x.value * safeP
    val offsetY = target.y.value * safeP

    val labelAlpha = (1f - safeP * 4f).coerceIn(0f, 1f)

    if (itemCount <= 0) {
        return MorphFrame(
            widthDp = width,
            heightDp = height,
            radiusDp = radius,
            offsetXDp = offsetX,
            offsetYDp = offsetY,
            labelAlpha = labelAlpha,
            itemAlpha = emptyList(),
            itemDyDp = emptyList()
        )
    }

    val itemAlpha = List(itemCount) { i ->
        ((safeP - 0.35f - 0.08f * i) * 4f).coerceIn(0f, 1f)
    }
    val dy = (1f - ((safeP - 0.25f) * 2f).coerceIn(0f, 1f)) * 8f
    val itemDyDp = List(itemCount) { dy }

    return MorphFrame(
        widthDp = width,
        heightDp = height,
        radiusDp = radius,
        offsetXDp = offsetX,
        offsetYDp = offsetY,
        labelAlpha = labelAlpha,
        itemAlpha = itemAlpha,
        itemDyDp = itemDyDp
    )
}

@Composable
fun GlassMorph(
    expanded: Boolean,
    anchor: DpRect,                    // the collapsed button's bounds in the overlay container's coordinates
    expandedSize: DpSize,
    expandedRadius: Dp,
    target: DpOffset,                  // see morphFrame
    collapsedAlpha: Float,             // 1f draws the collapsed surface; 0f makes it transparent (the real button stays visible beneath)
    source: GlassSource,
    onDismiss: () -> Unit,
    collapsedContent: @Composable BoxScope.() -> Unit,
    expandedContent: @Composable ColumnScope.(itemAlpha: (Int) -> Float, itemDy: (Int) -> Dp) -> Unit,
    itemCount: Int,
    modifier: Modifier = Modifier,
    collapsedRadius: Dp = minOf(anchor.width, anchor.height) / 2
) {
    val anim = remember { Animatable(if (expanded) 1f else 0f) }
    val effects = LocalEffects.current
    val reduceEffects = LocalReduceEffects.current
    val colors = LocalShellColors.current
    val densityPx = LocalDensity.current.density

    LaunchedEffect(expanded, effects, reduceEffects) {
        val targetVal = if (expanded) 1f else 0f
        val spec: AnimationSpec<Float> = if (effects == ResolvedEffects.Light || reduceEffects) {
            tween(100, easing = LinearEasing)
        } else {
            ShellMotion.sheet.toSpec()
        }
        anim.animateTo(targetVal, animationSpec = spec)
    }

    val liquidGroupState = rememberLiquidGroupState()
    val shouldCompose = collapsedAlpha > 0f || expanded || anim.targetValue > 0f || anim.isRunning

    if (shouldCompose) {
        Box(modifier = modifier.fillMaxSize()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .testTag("glass_morph_scrim_tap")
                    .graphicsLayer {
                        alpha = if (anim.value > 0.02f) 1f else 0f
                    }
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = onDismiss
                    )
            )

            LiquidGroup(
                state = liquidGroupState,
                shapes = {
                    // Pixels, in this overlay's space (the melt shader works in pixels).
                    val p = anim.value
                    val frame = morphFrame(
                        p = p,
                        collapsed = DpSize(anchor.width, anchor.height),
                        expanded = expandedSize,
                        collapsedRadiusDp = collapsedRadius.value,
                        expandedRadiusDp = expandedRadius.value,
                        target = target,
                        itemCount = itemCount
                    )
                    val morphLeft = anchor.left.value + frame.offsetXDp
                    val morphTop = anchor.top.value + frame.offsetYDp
                    val panel = LiquidShape(
                        cx = (morphLeft + frame.widthDp / 2f) * densityPx,
                        cy = (morphTop + frame.heightDp / 2f) * densityPx,
                        halfW = frame.widthDp / 2f * densityPx,
                        halfH = frame.heightDp / 2f * densityPx,
                        radius = frame.radiusDp * densityPx
                    )
                    if (!anim.isRunning || collapsedAlpha <= 0f) {
                        listOf(panel)
                    } else {
                        // The bud's root: the opener's glass, shrinking as the panel pulls free.
                        val root = (1f - p).coerceIn(0f, 1f)
                        val opener = LiquidShape(
                            cx = (anchor.left.value + anchor.width.value / 2f) * densityPx,
                            cy = (anchor.top.value + anchor.height.value / 2f) * densityPx,
                            halfW = anchor.width.value / 2f * densityPx * root,
                            halfH = anchor.height.value / 2f * densityPx * root,
                            radius = collapsedRadius.value * densityPx * root
                        )
                        listOf(opener, panel)
                    }
                },
                spec = LiquidSpecs.Panel,
                source = source,
                pressed = { anim.isRunning }
            )

            if (collapsedAlpha > 0f) {
                Box(
                    modifier = Modifier
                        .offset(x = anchor.left, y = anchor.top)
                        .size(width = anchor.width, height = anchor.height)
                        .graphicsLayer {
                            val p = anim.value
                            alpha = if (liquidGroupState.merging) 0f else (1f - p * 3f).coerceIn(0f, 1f)
                        }
                        .glassSurface(
                            spec = GlassSpecs.Floating,
                            liquid = LiquidSpecs.Panel,
                            shape = RoundedCornerShape(collapsedRadius),
                            source = source
                        )
                )
            }

            // The corner radius runs from the opener's to the panel's on the same progress as the
            // size, so the glass, the clip and the melt all share one shape.
            fun liveRadiusPx(): Float {
                val t = anim.value.coerceIn(0f, 1f)
                return (collapsedRadius.value + (expandedRadius.value - collapsedRadius.value) * t) * densityPx
            }
            val shape = RoundedCornerShape(
                object : CornerSize {
                    override fun toPx(shapeSize: Size, density: Density): Float =
                        minOf(liveRadiusPx(), shapeSize.minDimension / 2f)
                }
            )
            // The glass, in its own layer so it can hand over to the melt shader without
            // hiding the menu's labels.
            Box(
                modifier = Modifier
                    .layout { measurable, _ ->
                        val p = anim.value
                        val frame = morphFrame(
                            p = p,
                            collapsed = DpSize(anchor.width, anchor.height),
                            expanded = expandedSize,
                            collapsedRadiusDp = collapsedRadius.value,
                            expandedRadiusDp = expandedRadius.value,
                            target = target,
                            itemCount = itemCount
                        )
                        val wPx = (frame.widthDp * density).roundToInt()
                        val hPx = (frame.heightDp * density).roundToInt()
                        val placeable = measurable.measure(Constraints.fixed(wPx, hPx))
                        val xPx = ((anchor.left.value + frame.offsetXDp) * density).roundToInt()
                        val yPx = ((anchor.top.value + frame.offsetYDp) * density).roundToInt()
                        layout(wPx, hPx) {
                            placeable.place(xPx, yPx)
                        }
                    }
                    .graphicsLayer { alpha = if (liquidGroupState.merging) 0f else 1f }
                    .glassSurface(
                        spec = GlassSpecs.Floating,
                        liquid = LiquidSpecs.Panel,
                        shape = shape,
                        source = source
                    )
            )
            Box(
                modifier = Modifier
                    .layout { measurable, _ ->
                        val p = anim.value
                        val frame = morphFrame(
                            p = p,
                            collapsed = DpSize(anchor.width, anchor.height),
                            expanded = expandedSize,
                            collapsedRadiusDp = collapsedRadius.value,
                            expandedRadiusDp = expandedRadius.value,
                            target = target,
                            itemCount = itemCount
                        )
                        val wPx = (frame.widthDp * density).roundToInt()
                        val hPx = (frame.heightDp * density).roundToInt()
                        val placeable = measurable.measure(Constraints.fixed(wPx, hPx))
                        val xPx = ((anchor.left.value + frame.offsetXDp) * density).roundToInt()
                        val yPx = ((anchor.top.value + frame.offsetYDp) * density).roundToInt()
                        layout(wPx, hPx) {
                            placeable.place(xPx, yPx)
                        }
                    }
                    .testTag("glass_morph_surface")
                    .graphicsLayer {
                        clip = true
                        this.shape = RoundedCornerShape(minOf(liveRadiusPx(), minOf(size.width, size.height) / 2f))
                    }
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .graphicsLayer {
                            val p = anim.value
                            alpha = (1f - p * 4f).coerceIn(0f, 1f)
                        },
                    contentAlignment = Alignment.Center,
                    content = collapsedContent
                )

                Column(
                    modifier = Modifier
                        .wrapContentSize(Alignment.TopStart, unbounded = true)
                        .requiredSize(expandedSize.width, expandedSize.height)
                        .graphicsLayer {
                            val p = anim.value
                            alpha = ((p - 0.2f) * 2f).coerceIn(0f, 1f)
                        },
                    content = {
                        val itemAlphaProvider: (Int) -> Float = { i ->
                            val p = anim.value
                            val safeP = if (p.isNaN()) 0f else p
                            ((safeP - 0.35f - 0.08f * i) * 4f).coerceIn(0f, 1f)
                        }
                        val itemDyProvider: (Int) -> Dp = { i ->
                            val p = anim.value
                            val safeP = if (p.isNaN()) 0f else p
                            val dy = (1f - ((safeP - 0.25f) * 2f).coerceIn(0f, 1f)) * 8f
                            dy.dp
                        }
                        expandedContent(itemAlphaProvider, itemDyProvider)
                    }
                )
            }
        }
    }
}
