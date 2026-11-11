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

        /** Windows shorter than this (landscape, keyboard up) use the compact layout. */
        const val COMPACT_HEIGHT_DP = 360f

        /** Compact layout: top bar and paper top, so the paper keeps its height. */
        const val COMPACT_TOP_BAR_DP = 40f
        const val COMPACT_PAPER_TOP_DP = 44f

        /** Smallest font the compact layout shrinks to before the carriage may be pushed off the window. */
        const val COMPACT_MIN_FONT_SP = 8f

        /** Space between the caret line and the rail (see textTopOffsetDp). */
        const val RAIL_TEXT_GAP_DP = 23f

        /** Carriage body below the rail, plus a margin. */
        const val CARRIAGE_BELOW_RAIL_DP = 30f

        fun isCompact(heightDp: Float): Boolean = !heightDp.isNaN() && heightDp in 1f..COMPACT_HEIGHT_DP

        fun compute(widthDp: Float, heightDp: Float, fontScale: Float): WriterGeometry {
            val safeFontScale = if (fontScale.isNaN() || fontScale <= 0f) 1.0f else fontScale.coerceIn(0.85f, 1.3f)
            val cols = (28f / safeFontScale).roundToInt().coerceIn(18, 28)

            val safeWidth = if (widthDp.isNaN() || widthDp.isInfinite() || widthDp <= 0f) 72f else widthDp
            // Ensure available width yields at least 1 sp font even on tiny width or bad inputs
            val availableWidth = (safeWidth - 72f).coerceAtLeast(cols * 0.6f * 1f)
            val uncappedFs = availableWidth / (cols * 0.6f)
            val safeHeight = if (heightDp.isNaN() || heightDp <= 0f) 0f else heightDp
            val compact = isCompact(safeHeight)
            // Compact: shrink the font until the caret line fits above a rail that clears the window.
            val paperTopDp = if (compact) COMPACT_PAPER_TOP_DP else PAPER_TOP_DP
            val fitFontSp = (safeHeight - paperTopDp - RAIL_TEXT_GAP_DP - CARRIAGE_BELOW_RAIL_DP) / 1.7f
            val maxFontSp = if (compact) fitFontSp.coerceIn(COMPACT_MIN_FONT_SP, 22f) else 22f
            val fontSp = uncappedFs.coerceIn(1f, maxFontSp)
            val columnWidthDp = 0.6f * fontSp
            val textWidthDp = cols * columnWidthDp
            val textLeftDp = if (uncappedFs >= maxFontSp) (safeWidth - textWidthDp) / 2f else 36f

            val lineHeightDp = 1.7f * fontSp
            val railYDp = if (compact) {
                // Short window: the lamp panel overlays the paper when it is open, so it does not
                // push the rail up. The caret line always shows; the carriage stays above the
                // bottom of the window whenever the window is tall enough for both.
                val caretLineFits = paperTopDp + RAIL_TEXT_GAP_DP + lineHeightDp
                val carriageFits = safeHeight - CARRIAGE_BELOW_RAIL_DP
                round(safeHeight * 0.62f).coerceAtMost(carriageFits).coerceAtLeast(caretLineFits)
            } else {
                round(safeHeight * 0.62f)
                    .coerceAtMost(safeHeight - PANEL_CLEARANCE_DP)
                    .coerceAtLeast(MIN_RAIL_Y_DP)
            }

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
