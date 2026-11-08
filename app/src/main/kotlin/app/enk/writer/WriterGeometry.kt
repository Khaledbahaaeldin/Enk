package app.enk.writer

import kotlin.math.round
import kotlin.math.roundToInt

data class WriterGeometry(
    val cols: Int,
    val fontSp: Float,
    val columnWidthDp: Float,
    val lineHeightDp: Float,
    val textLeftDp: Float,
    val textWidthDp: Float,
    val railYDp: Float,
    val paperTopDp: Float,
) {
    fun textTopOffsetDp(line: Int): Float = railYDp - paperTopDp - 23f - (line + 1) * lineHeightDp

    fun carriageCenterXDp(col: Int): Float = textLeftDp + (col + 0.5f) * columnWidthDp

    fun colFromX(xDp: Float): Float =
        if (columnWidthDp <= 0f) 0f else (xDp - textLeftDp) / columnWidthDp - 0.5f

    companion object {
        fun compute(widthDp: Float, heightDp: Float, fontScale: Float): WriterGeometry {
            val safeFontScale = if (fontScale.isNaN() || fontScale <= 0f) 1.0f else fontScale.coerceIn(0.85f, 1.3f)
            val cols = (28f / safeFontScale).roundToInt().coerceIn(18, 28)

            val safeWidth = if (widthDp.isNaN() || widthDp.isInfinite() || widthDp <= 0f) 72f else widthDp
            // Ensure available width yields at least 1 sp font even on tiny width or bad inputs
            val availableWidth = (safeWidth - 72f).coerceAtLeast(cols * 0.6f * 1f)
            val uncappedFs = availableWidth / (cols * 0.6f)
            val isCapped = uncappedFs >= 22f
            val fontSp = uncappedFs.coerceIn(1f, 22f)
            val columnWidthDp = 0.6f * fontSp
            val textWidthDp = cols * columnWidthDp
            val textLeftDp = if (isCapped) (safeWidth - textWidthDp) / 2f else 36f

            val safeHeight = if (heightDp.isNaN() || heightDp <= 0f) 0f else heightDp
            val railYDp = round(safeHeight * 0.62f)
            val paperTopDp = 64f
            val lineHeightDp = 1.7f * fontSp

            return WriterGeometry(
                cols = cols,
                fontSp = fontSp,
                columnWidthDp = columnWidthDp,
                lineHeightDp = lineHeightDp,
                textLeftDp = textLeftDp,
                textWidthDp = textWidthDp,
                railYDp = railYDp,
                paperTopDp = paperTopDp,
            )
        }
    }
}
