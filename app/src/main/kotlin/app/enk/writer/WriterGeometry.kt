package app.enk.writer

import app.enk.design.ControlPanelMetrics
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
        /** Paper sheet insets inside the chassis; PaperSheet and tap mapping share them. */
        const val PAPER_TOP_DP = 64f
        const val PAPER_SIDE_DP = 12f

        /** Panel footprint (see ControlPanelMetrics) + carriage below the rail (25) + 8 gap. */
        const val PANEL_CLEARANCE_DP = ControlPanelMetrics.FOOTPRINT_DP + 33f

        /** Floor so a short window (keyboard up, landscape) still leaves room for text. */
        const val MIN_RAIL_Y_DP = PAPER_TOP_DP + 60f

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
                .coerceAtMost(safeHeight - PANEL_CLEARANCE_DP)
                .coerceAtLeast(MIN_RAIL_Y_DP)
            val paperTopDp = PAPER_TOP_DP
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
