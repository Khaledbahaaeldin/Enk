package app.enk.design

import android.graphics.BlurMaskFilter
import android.graphics.Paint
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.ClipOp
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathOperation
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.asAndroidPath
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp

data class ShadowLayer(
    val dx: Dp,
    val dy: Dp,
    val blur: Dp,
    val spread: Dp,
    val color: Color,
    val inset: Boolean = false
)

object EnkShadows {
    val paper = listOf(
        ShadowLayer(0.dp, (-1).dp, 0.dp, 0.dp, Color(0x1FFFFFFF), false),
        ShadowLayer(0.dp, 8.dp, 20.dp, 0.dp, Color(0x73000000), false)
    )

    val rail = listOf(
        ShadowLayer(0.dp, 7.dp, 10.dp, 0.dp, Color(0x73000000), false),
        ShadowLayer(0.dp, 1.dp, 0.dp, 0.dp, Color(0x66000000), false)
    )

    val cap = listOf(
        ShadowLayer(0.dp, 1.dp, 0.dp, 0.dp, Color(0x59FFFFFF), true),
        ShadowLayer(0.dp, 12.dp, 16.dp, 0.dp, Color(0x8C000000), false),
        ShadowLayer(0.dp, 2.dp, 3.dp, 0.dp, Color(0x99000000), false)
    )

    val knob = listOf(
        ShadowLayer(0.dp, 1.dp, 0.dp, 0.dp, Color(0x4DFFFFFF), true),
        ShadowLayer(0.dp, 6.dp, 8.dp, 0.dp, Color(0x8C000000), false),
        ShadowLayer(0.dp, 1.dp, 2.dp, 0.dp, Color(0xFF000000), false)
    )

    val knobLifted = listOf(
        ShadowLayer(0.dp, 1.dp, 0.dp, 0.dp, Color(0x4DFFFFFF), true),
        ShadowLayer(0.dp, 12.dp, 14.dp, 0.dp, Color(0x99000000), false),
        ShadowLayer(0.dp, 2.dp, 3.dp, 0.dp, Color(0xFF000000), false)
    )

    val panel = listOf(
        ShadowLayer(0.dp, 1.dp, 0.dp, 0.dp, Color(0x38FFFFFF), true),
        ShadowLayer(0.dp, (-1).dp, 0.dp, 0.dp, Color(0x80000000), true),
        ShadowLayer(0.dp, 0.dp, 0.dp, 1.dp, EnkColors.panelRing, false),
        ShadowLayer(0.dp, 14.dp, 24.dp, 0.dp, Color(0x99000000), false)
    )

    val panelWell = listOf(
        ShadowLayer(0.dp, 2.dp, 6.dp, 0.dp, Color(0x80000000), true)
    )

    val slot = listOf(
        ShadowLayer(0.dp, 2.dp, 3.dp, 0.dp, Color(0xFF000000), true),
        ShadowLayer(0.dp, 1.dp, 0.dp, 0.dp, Color(0x26FFFFFF), false)
    )

    val track = listOf(
        ShadowLayer(0.dp, 2.dp, 3.dp, 0.dp, Color(0xFF000000), true),
        ShadowLayer(0.dp, 1.dp, 0.dp, 0.dp, Color(0x24FFFFFF), false)
    )
}

fun Modifier.enkShadow(layers: List<ShadowLayer>, shape: Shape = RectangleShape): Modifier = this.drawWithContent {
    if (size.width <= 0f || size.height <= 0f) {
        drawContent()
        return@drawWithContent
    }

    val density = this
    val layoutDir = layoutDirection
    val basePath = createPathForShape(shape, size, layoutDir, density, 0f)

    // 1. Draw outer drops (inset == false) before content
    for (layer in layers) {
        if (!layer.inset) {
            val spreadPx = layer.spread.toPx()
            val spreadPath = if (spreadPx != 0f) {
                createPathForShape(shape, size, layoutDir, density, spreadPx)
            } else {
                basePath
            }
            val dxPx = layer.dx.toPx()
            val dyPx = layer.dy.toPx()
            val blurPx = layer.blur.toPx()

            clipPath(basePath, clipOp = ClipOp.Difference) {
                drawIntoCanvas { canvas ->
                    val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                        color = layer.color.toArgb()
                        if (blurPx > 0f) {
                            maskFilter = BlurMaskFilter(blurPx, BlurMaskFilter.Blur.NORMAL)
                        }
                    }
                    val nativeCanvas = canvas.nativeCanvas
                    nativeCanvas.save()
                    nativeCanvas.translate(dxPx, dyPx)
                    nativeCanvas.drawPath(spreadPath.asAndroidPath(), paint)
                    nativeCanvas.restore()
                }
            }
        }
    }

    // 2. Draw content
    drawContent()

    // 3. Draw inset shadows (inset == true) after content
    for (layer in layers) {
        if (layer.inset) {
            val spreadPx = layer.spread.toPx()
            val dxPx = layer.dx.toPx()
            val dyPx = layer.dy.toPx()
            val blurPx = layer.blur.toPx()

            val holePath = if (spreadPx != 0f) {
                createPathForShape(shape, size, layoutDir, density, -spreadPx)
            } else {
                createPathForShape(shape, size, layoutDir, density, 0f)
            }

            clipPath(basePath, clipOp = ClipOp.Intersect) {
                if (blurPx <= 0f) {
                    val translatedHole = Path().apply {
                        addPath(holePath)
                        asAndroidPath().offset(dxPx, dyPx)
                    }
                    val diff = Path().apply {
                        op(basePath, translatedHole, PathOperation.Difference)
                    }
                    drawPath(diff, color = layer.color)
                } else {
                    val pad = maxOf(blurPx * 3f, 50f)
                    val bigRect = Path().apply {
                        addRect(Rect(-pad, -pad, size.width + pad, size.height + pad))
                    }
                    val translatedHole = Path().apply {
                        addPath(holePath)
                        asAndroidPath().offset(dxPx, dyPx)
                    }
                    val exterior = Path().apply {
                        op(bigRect, translatedHole, PathOperation.Difference)
                    }
                    drawIntoCanvas { canvas ->
                        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                            color = layer.color.toArgb()
                            maskFilter = BlurMaskFilter(blurPx, BlurMaskFilter.Blur.NORMAL)
                        }
                        canvas.nativeCanvas.drawPath(exterior.asAndroidPath(), paint)
                    }
                }
            }
        }
    }
}

private fun createPathForShape(
    shape: Shape,
    size: Size,
    layoutDirection: LayoutDirection,
    density: Density,
    spreadPx: Float
): Path {
    val outline = shape.createOutline(size, layoutDirection, density)
    return when (outline) {
        is Outline.Rectangle -> {
            val rect = if (spreadPx != 0f) {
                Rect(
                    left = outline.rect.left - spreadPx,
                    top = outline.rect.top - spreadPx,
                    right = outline.rect.right + spreadPx,
                    bottom = outline.rect.bottom + spreadPx
                )
            } else {
                outline.rect
            }
            Path().apply { addRect(rect) }
        }
        is Outline.Rounded -> {
            val rr = if (spreadPx != 0f) {
                RoundRect(
                    left = outline.roundRect.left - spreadPx,
                    top = outline.roundRect.top - spreadPx,
                    right = outline.roundRect.right + spreadPx,
                    bottom = outline.roundRect.bottom + spreadPx,
                    topLeftCornerRadius = CornerRadius(
                        maxOf(0f, outline.roundRect.topLeftCornerRadius.x + spreadPx),
                        maxOf(0f, outline.roundRect.topLeftCornerRadius.y + spreadPx)
                    ),
                    topRightCornerRadius = CornerRadius(
                        maxOf(0f, outline.roundRect.topRightCornerRadius.x + spreadPx),
                        maxOf(0f, outline.roundRect.topRightCornerRadius.y + spreadPx)
                    ),
                    bottomRightCornerRadius = CornerRadius(
                        maxOf(0f, outline.roundRect.bottomRightCornerRadius.x + spreadPx),
                        maxOf(0f, outline.roundRect.bottomRightCornerRadius.y + spreadPx)
                    ),
                    bottomLeftCornerRadius = CornerRadius(
                        maxOf(0f, outline.roundRect.bottomLeftCornerRadius.x + spreadPx),
                        maxOf(0f, outline.roundRect.bottomLeftCornerRadius.y + spreadPx)
                    )
                )
            } else {
                outline.roundRect
            }
            Path().apply { addRoundRect(rr) }
        }
        is Outline.Generic -> {
            Path().apply { addPath(outline.path) }
        }
    }
}
