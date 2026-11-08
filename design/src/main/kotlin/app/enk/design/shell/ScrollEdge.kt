package app.enk.design.shell

import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.RenderEffect
import android.graphics.Shader
import android.os.Build
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.asComposeRenderEffect
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import app.enk.design.theme.LocalShellColors

fun collapseProgress(scrollY: Float): Float = ((scrollY - 30f) / 36f).coerceIn(0f, 1f)

enum class EdgeKind { CompactBar, StripVeil }

fun Modifier.scrollEdge(kind: EdgeKind, progress: Float): Modifier = composed {
    val p = progress.coerceIn(0f, 1f)
    if (p <= 0f) return@composed Modifier

    val context = LocalContext.current
    val colors = LocalShellColors.current
    val reduceEffects = LocalReduceEffects.current || isReducedMotion(context) || isHighContrastTextEnabled(context)

    val blurRadius = if (kind == EdgeKind.CompactBar) 22.dp else 18.dp

    this.then(
        if (reduceEffects) {
            Modifier.drawBehind {
                drawRect(color = colors.g1.copy(alpha = colors.g1.alpha * p))
            }
        } else {
            Modifier
                .graphicsLayer {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                        runCatching {
                            val blurPx = blurRadius.toPx()
                            val blur = RenderEffect.createBlurEffect(blurPx, blurPx, Shader.TileMode.CLAMP)
                            val finalEffect = if (kind == EdgeKind.CompactBar) {
                                val cm = ColorMatrix().apply { setSaturation(1.4f) }
                                val sat = RenderEffect.createColorFilterEffect(ColorMatrixColorFilter(cm))
                                RenderEffect.createChainEffect(sat, blur)
                            } else {
                                blur
                            }
                            renderEffect = finalEffect.asComposeRenderEffect()
                        }
                    }
                }
                .drawBehind {
                    when (kind) {
                        EdgeKind.CompactBar -> {
                            val brush = Brush.verticalGradient(
                                0.0f to colors.bg.copy(alpha = 0.84f * p),
                                0.7f to colors.bg.copy(alpha = 0.50f * p),
                                1.0f to colors.bg.copy(alpha = 0.0f)
                            )
                            drawRect(brush = brush)
                        }
                        EdgeKind.StripVeil -> {
                            val brush = Brush.verticalGradient(
                                0.0f to colors.bg.copy(alpha = 0.72f * p),
                                0.6f to colors.bg.copy(alpha = 0.72f * p),
                                1.0f to colors.bg.copy(alpha = 0.0f)
                            )
                            drawRect(brush = brush)
                        }
                    }
                }
        }
    )
}
