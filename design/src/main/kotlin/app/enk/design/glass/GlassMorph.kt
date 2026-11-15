package app.enk.design.glass

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
import androidx.compose.ui.platform.testTag
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
    val anim = remember { androidx.compose.animation.core.Animatable(if (expanded) 1f else 0f) }
    val effects = LocalEffects.current
    val reduceEffects = LocalReduceEffects.current
    val colors = LocalShellColors.current

    LaunchedEffect(expanded, effects, reduceEffects) {
        val targetVal = if (expanded) 1f else 0f
        val spec: AnimationSpec<Float> = if (effects == ResolvedEffects.Light || reduceEffects) {
            tween(100, easing = LinearEasing)
        } else {
            ShellMotion.sheet.toSpec()
        }
        anim.animateTo(targetVal, animationSpec = spec)
    }

    val p = anim.value

    if (p > 0.001f || expanded) {
        Box(modifier = modifier.fillMaxSize()) {
            if (p > 0.02f) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .testTag("glass_morph_scrim_tap")
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                            onClick = onDismiss
                        )
                )
            }

            val frame = morphFrame(
                p = p,
                collapsed = DpSize(anchor.width, anchor.height),
                expanded = expandedSize,
                collapsedRadiusDp = collapsedRadius.value,
                expandedRadiusDp = expandedRadius.value,
                target = target,
                itemCount = itemCount
            )

            val tintFactor = (collapsedAlpha + (1f - collapsedAlpha) * (p * 4f).coerceIn(0f, 1f))
            val tintColor = colors.thick.copy(alpha = colors.thick.alpha * tintFactor)
            val shape = RoundedCornerShape(frame.radiusDp.dp)

            Box(
                modifier = Modifier
                    .offset(x = anchor.left + frame.offsetXDp.dp, y = anchor.top + frame.offsetYDp.dp)
                    .size(width = frame.widthDp.dp, height = frame.heightDp.dp)
                    .testTag("glass_morph_surface")
                    .glassSurface(spec = GlassSpecs.Floating, shape = shape, source = source, solidColor = tintColor)
                    .clip(shape)
            ) {
                if (frame.labelAlpha > 0.001f) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .graphicsLayer { alpha = frame.labelAlpha },
                        contentAlignment = Alignment.Center,
                        content = collapsedContent
                    )
                }

                if (p > 0.25f) {
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        content = {
                            expandedContent(
                                { i -> frame.itemAlpha.getOrElse(i) { 0f } },
                                { i -> frame.itemDyDp.getOrElse(i) { 0f }.dp }
                            )
                        }
                    )
                }
            }
        }
    }
}
