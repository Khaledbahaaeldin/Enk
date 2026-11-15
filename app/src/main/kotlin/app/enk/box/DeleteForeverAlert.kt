package app.enk.box

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.DpRect
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.enk.design.effects.LocalEffects
import app.enk.design.effects.ResolvedEffects
import app.enk.design.glass.GlassButton
import app.enk.design.glass.GlassMorph
import app.enk.design.shell.GlassSource
import app.enk.design.shell.LocalReduceEffects
import app.enk.design.shell.rememberGlassSource
import app.enk.design.theme.LocalShellColors

@Composable
fun DeleteForeverAlert(
    count: Int,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
    anchor: DpRect? = null,
    target: DpOffset? = null,
    expanded: Boolean = true,
    modifier: Modifier = Modifier,
    isDeleting: Boolean = false,
    glassSource: GlassSource = rememberGlassSource()
) {
    val colors = LocalShellColors.current
    val effects = LocalEffects.current
    val reduceEffects = LocalReduceEffects.current
    val isLight = effects == ResolvedEffects.Light || reduceEffects

    val scrimAnim = remember { Animatable(if (expanded) 0f else 0f) }
    LaunchedEffect(expanded, isLight) {
        val targetAlpha = if (expanded) 1f else 0f
        val spec: androidx.compose.animation.core.AnimationSpec<Float> = if (isLight) {
            tween(100, easing = LinearEasing)
        } else {
            tween(300, easing = LinearEasing)
        }
        scrimAnim.animateTo(targetAlpha, animationSpec = spec)
    }

    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
        val containerWidth = maxWidth.value
        val containerHeight = maxHeight.value

        val defaultAnchor = remember(containerWidth, containerHeight) {
            DpRect(
                left = (containerWidth / 2f - 90f).dp,
                top = (containerHeight - 120f).dp,
                right = (containerWidth / 2f + 90f).dp,
                bottom = (containerHeight - 74f).dp
            )
        }
        val effectiveAnchor = anchor ?: defaultAnchor
        val defaultTarget = remember(containerWidth, containerHeight, effectiveAnchor) {
            DpOffset(
                x = (containerWidth / 2f - 150f - effectiveAnchor.left.value).dp,
                y = (containerHeight / 2f - 100f - effectiveAnchor.top.value).dp
            )
        }
        val effectiveTarget = target ?: defaultTarget

        if (scrimAnim.value > 0.001f || expanded) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .testTag("delete_forever_scrim")
                    .background(colors.thick.copy(alpha = 0.4f * scrimAnim.value))
                    .clickable(
                        enabled = !isDeleting,
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = onDismiss
                    )
            )
        }

        GlassMorph(
            expanded = expanded,
            anchor = effectiveAnchor,
            expandedSize = DpSize(300.dp, 200.dp),
            expandedRadius = 28.dp,
            target = effectiveTarget,
            collapsedAlpha = 0f,
            source = glassSource,
            onDismiss = { if (!isDeleting) onDismiss() },
            collapsedContent = {},
            expandedContent = { itemAlpha, itemDy ->
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(top = 22.dp, start = 20.dp, end = 20.dp, bottom = 18.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier
                            .fillMaxWidth()
                            .graphicsLayer {
                                alpha = itemAlpha(0)
                                translationY = itemDy(0).toPx()
                            }
                    ) {
                        Text(
                            text = "Delete forever",
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Bold,
                            color = colors.t1,
                            textAlign = TextAlign.Center
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        val noteWord = if (count == 1) "note" else "notes"
                        Text(
                            text = "$count $noteWord will be erased. This can’t be undone.",
                            fontSize = 13.sp,
                            lineHeight = 18.sp,
                            color = colors.t2,
                            textAlign = TextAlign.Center
                        )
                    }

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .graphicsLayer {
                                alpha = itemAlpha(1)
                                translationY = itemDy(1).toPx()
                            },
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        GlassButton(
                            onClick = onDismiss,
                            enabled = !isDeleting,
                            modifier = Modifier.weight(1f),
                            glassSource = glassSource
                        ) {
                            Text(
                                text = "Keep",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Medium,
                                color = colors.t1
                            )
                        }

                        GlassButton(
                            onClick = onConfirm,
                            enabled = !isDeleting,
                            modifier = Modifier.weight(1f),
                            glassSource = glassSource
                        ) {
                            Text(
                                text = "Delete",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = if (isDeleting) colors.danger.copy(alpha = 0.5f) else colors.danger
                            )
                        }
                    }
                }
            },
            itemCount = 2
        )
    }
}
