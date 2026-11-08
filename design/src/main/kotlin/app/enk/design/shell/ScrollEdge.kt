package app.enk.design.shell

import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import app.enk.design.theme.LocalShellColors
import com.kyant.backdrop.backdrops.emptyBackdrop
import com.kyant.backdrop.drawPlainBackdrop
import com.kyant.backdrop.effects.blur
import com.kyant.backdrop.effects.colorControls

fun collapseProgress(scrollY: Float): Float = ((scrollY - 30f) / 36f).coerceIn(0f, 1f)

enum class EdgeKind { CompactBar, StripVeil }

/**
 * DS 12.4 compact bar and strip veil: a blurred, tinted copy of what scrolls beneath the bar
 * (sampled from [source]), faded out along its bottom edge. [progress] is [collapseProgress].
 * Apply it to a backdrop-only layer behind the title, not to the title itself: the bottom fade also
 * masks anything drawn inside the modified element. With reduced effects it is a solid `g1` surface with no blur and no gradient.
 */
fun Modifier.scrollEdge(kind: EdgeKind, progress: Float, source: GlassSource): Modifier = composed {
    val p = progress.coerceIn(0f, 1f)
    if (p <= 0f) return@composed Modifier

    val colors = LocalShellColors.current
    val backend = rememberGlassBackend()

    if (backend == GlassBackendKind.Solid) {
        return@composed Modifier.drawBehind {
            drawRect(color = colors.g1.copy(alpha = colors.g1.alpha * p))
        }
    }

    val compact = kind == EdgeKind.CompactBar
    val blurRadius = if (compact) 22.dp else 18.dp
    val fadeStart = if (compact) 0.7f else 0.6f
    val backdrop = source.backdrop ?: emptyBackdrop()

    Modifier
        .graphicsLayer {
            alpha = p
            compositingStrategy = CompositingStrategy.Offscreen
        }
        .drawWithContent {
            drawContent()
            // Bottom edge fade mask: opaque down to fadeStart, transparent at the bottom.
            drawRect(
                brush = Brush.verticalGradient(
                    fadeStart to colors.bg.copy(alpha = 1f),
                    1.0f to colors.bg.copy(alpha = 0f)
                ),
                blendMode = BlendMode.DstIn
            )
        }
        .drawPlainBackdrop(
            backdrop = backdrop,
            shape = { RectangleShape },
            effects = {
                blur(blurRadius.toPx())
                if (compact) colorControls(saturation = 1.4f)
            },
            onDrawSurface = {
                val tint = if (compact) {
                    Brush.verticalGradient(
                        0.0f to colors.bg.copy(alpha = 0.84f),
                        0.7f to colors.bg.copy(alpha = 0.50f),
                        1.0f to colors.bg.copy(alpha = 0.0f)
                    )
                } else {
                    Brush.verticalGradient(
                        0.0f to colors.bg.copy(alpha = 0.72f),
                        1.0f to colors.bg.copy(alpha = 0.0f)
                    )
                }
                drawRect(brush = tint)
            }
        )
}
