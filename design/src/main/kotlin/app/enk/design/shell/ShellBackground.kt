package app.enk.design.shell

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.ImageShader
import androidx.compose.ui.graphics.ShaderBrush
import androidx.compose.ui.graphics.TileMode
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import app.enk.design.Grain
import app.enk.design.theme.LocalShellColors
import app.enk.design.theme.LocalShellMaterial

@Composable
fun ShellBackground(
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit
) {
    val colors = LocalShellColors.current
    val material = LocalShellMaterial.current
    val context = LocalContext.current
    val reduceEffects = LocalReduceEffects.current || isReducedMotion(context) || isHighContrastTextEnabled(context)

    val grainTile = remember { Grain.tile() }
    val grainBrush = remember(grainTile) {
        ShaderBrush(ImageShader(grainTile, TileMode.Repeated, TileMode.Repeated))
    }

    Box(
        modifier = modifier.drawBehind {
            // 1. bg fill
            drawRect(colors.bg)

            // 2. Gradient 1: ellipse 420 x 380 dp centered at 88% x, 2% y, auraTop fading at 72%
            val rx1 = 210.dp.toPx()
            val ry1 = 190.dp.toPx()
            val c1 = Offset(size.width * 0.88f, size.height * 0.02f)
            if (rx1 > 0f && ry1 > 0f) {
                withTransform({
                    scale(scaleX = 1f, scaleY = ry1 / rx1, pivot = c1)
                }) {
                    drawRect(
                        brush = Brush.radialGradient(
                            0.0f to colors.auraTop,
                            0.72f to colors.auraTop.copy(alpha = 0f),
                            1.0f to colors.auraTop.copy(alpha = 0f),
                            center = c1,
                            radius = rx1
                        )
                    )
                }
            }

            // 3. Gradient 2: ellipse 360 x 420 dp at -8% x, 46% y, auraSide
            val rx2 = 180.dp.toPx()
            val ry2 = 210.dp.toPx()
            val c2 = Offset(size.width * -0.08f, size.height * 0.46f)
            if (rx2 > 0f && ry2 > 0f) {
                withTransform({
                    scale(scaleX = rx2 / ry2, scaleY = 1f, pivot = c2)
                }) {
                    drawRect(
                        brush = Brush.radialGradient(
                            0.0f to colors.auraSide,
                            1.0f to colors.auraSide.copy(alpha = 0f),
                            center = c2,
                            radius = ry2
                        )
                    )
                }
            }

            // 4. Gradient 3: ellipse 440 x 340 dp at 70% x, 104% y, auraBottom
            val rx3 = 220.dp.toPx()
            val ry3 = 170.dp.toPx()
            val c3 = Offset(size.width * 0.70f, size.height * 1.04f)
            if (rx3 > 0f && ry3 > 0f) {
                withTransform({
                    scale(scaleX = 1f, scaleY = ry3 / rx3, pivot = c3)
                }) {
                    drawRect(
                        brush = Brush.radialGradient(
                            0.0f to colors.auraBottom,
                            1.0f to colors.auraBottom.copy(alpha = 0f),
                            center = c3,
                            radius = rx3
                        )
                    )
                }
            }

            // 5. Grain tile overlay at auraGrain (turns off when reduceEffects is true)
            val grainOpacity = if (reduceEffects) 0f else material.auraGrain
            if (grainOpacity > 0f) {
                drawRect(
                    brush = grainBrush,
                    alpha = grainOpacity,
                    blendMode = BlendMode.Overlay
                )
            }
        },
        content = content
    )
}
