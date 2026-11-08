package app.enk.design.logo

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.drawscope.withTransform
import app.enk.design.EnkColors
import app.enk.design.EnkMaterials

data class SymbolParts(
    val lines: Boolean = true,
    val caret: Boolean = true,
    val rail: Boolean = true,
    val carriage: Boolean = true
) {
    companion object {
        val All = SymbolParts()
    }
}

private const val GRID = 108f

/** Carriage body, in carriage-local units (centred on [LogoSpec.carriageCentre], before scaling). */
private val BODY = Rect(-15f, -10f, 15f, 11f)
private val TOP_COLLAR = Rect(-15f, -10f, 15f, -7f)
private val BOTTOM_COLLAR = Rect(-15f, 8f, 15f, 11f)
private val SLUG = Rect(-3.5f, -14.6f, 3.5f, -9.6f)
private val SLUG_SLEEVE = Rect(-5.2f, -16.4f, 5.2f, -9.4f)
private val STRIP = Rect(-4.3f, -4.7f, 4.3f, -2.8f)

private val SLEEVE_INK = Color(0xFF141312)

/**
 * The Enk symbol on the 108 x 108 grid, drawn with the writer's own materials. The vector drawables
 * (launcher icon, splash) come from the same geometry, see tools/GenerateLogoAssets.md.
 */
@Composable
fun EnkSymbol(modifier: Modifier = Modifier, parts: SymbolParts = SymbolParts.All) {
    Canvas(modifier = modifier.aspectRatio(1f)) {
        val scale = size.width / GRID
        withTransform({ scale(scale, scale, Offset.Zero) }) {
            if (parts.lines) drawTypedLines()
            if (parts.caret) drawCaret()
            if (parts.rail) drawRail()
            if (parts.carriage) drawCarriage()
        }
    }
}

private fun DrawScope.drawTypedLines() {
    val lineColor = LogoSpec.CREAM.copy(alpha = 0.34f)
    LogoSpec.typedLines.forEach { line ->
        drawRoundRect(
            color = lineColor,
            topLeft = line.topLeft,
            size = line.size,
            cornerRadius = CornerRadius(1.3f)
        )
    }
}

private fun DrawScope.drawCaret() {
    drawRoundRect(
        color = EnkColors.accent,
        topLeft = LogoSpec.caret.topLeft,
        size = LogoSpec.caret.size,
        cornerRadius = CornerRadius(1.0f)
    )
}

private fun DrawScope.drawRail() {
    val rail = LogoSpec.rail
    drawRect(
        color = Color.Black.copy(alpha = 0.42f),
        topLeft = Offset(rail.left, rail.top + 1.5f),
        size = Size(rail.width, 4f)
    )
    fillRect(rail, EnkMaterials.railSteel)
}

private fun DrawScope.drawCarriage() {
    withTransform({
        translate(LogoSpec.carriageCentre.x, LogoSpec.carriageCentre.y)
        scale(LogoSpec.CARRIAGE_SCALE, LogoSpec.CARRIAGE_SCALE, Offset.Zero)
    }) {
        drawOval(
            color = Color.Black.copy(alpha = 0.55f),
            topLeft = Offset(-14.5f, 12f),
            size = Size(29f, 6f)
        )
        drawRoundRect(
            color = SLEEVE_INK,
            topLeft = SLUG_SLEEVE.topLeft,
            size = Size(SLUG_SLEEVE.width, 7f),
            cornerRadius = CornerRadius(1.7f)
        )
        fillRect(SLUG, EnkMaterials.slugSteel, corner = 1.0f)

        val bodyPath = Path().apply { addRoundRect(RoundRect(BODY, CornerRadius(4.5f))) }
        clipPath(bodyPath) {
            fillRect(BODY, EnkMaterials.graphiteCap)
            fillRect(TOP_COLLAR, EnkMaterials.collarTop)
            fillRect(BOTTOM_COLLAR, EnkMaterials.collarBottom)
        }

        drawLine(
            color = Color.White.copy(alpha = 0.17f),
            start = Offset(-8.4f, 4.1f),
            end = Offset(8.4f, 4.1f),
            strokeWidth = 2.5f,
            pathEffect = PathEffect.dashPathEffect(floatArrayOf(0.9f, 1.7f), 0f)
        )
        drawRoundRect(
            color = EnkColors.accent,
            topLeft = STRIP.topLeft,
            size = STRIP.size,
            cornerRadius = CornerRadius(0.95f)
        )
    }
}

/**
 * Fills [rect] with a brush whose gradient spans the rect itself. A relative brush such as
 * [EnkMaterials.railSteel] otherwise spans the whole canvas, so the draw size is narrowed to the rect.
 */
private fun DrawScope.fillRect(rect: Rect, brush: Brush, corner: Float = 0f) {
    val canvasSize = drawContext.size
    drawContext.size = rect.size
    translate(rect.left, rect.top) {
        drawRoundRect(brush = brush, size = rect.size, cornerRadius = CornerRadius(corner))
    }
    drawContext.size = canvasSize
}
