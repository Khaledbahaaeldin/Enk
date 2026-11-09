package app.enk.writer

import kotlin.math.floor
import kotlin.math.roundToInt

data class GridLine(
    val start: Int,
    val end: Int,
    val text: String = ""
)

object TapCaretPolicy {
    fun indexAtTap(
        xDp: Float,
        yDp: Float,
        geometry: WriterGeometry,
        lines: List<GridLine>,
        feedOffsetDp: Float,
        paperTopDp: Float = WriterGeometry.PAPER_TOP_DP
    ): Int {
        if (lines.isEmpty()) return 0

        val yRelPaper = yDp - paperTopDp
        val yRelText = yRelPaper - feedOffsetDp
        val rawLine = floor(yRelText / geometry.lineHeightDp).toInt()
        val lineIdx = rawLine.coerceIn(0, lines.size - 1)

        val targetLine = lines[lineIdx]
        val lineLen = targetLine.end - targetLine.start
        if (lineLen <= 0) return targetLine.start

        val rawCol = ((xDp - geometry.textLeftDp) / geometry.columnWidthDp).roundToInt()
        var col = rawCol.coerceIn(0, lineLen)

        if (targetLine.text.isNotEmpty() && col in 1 until targetLine.text.length) {
            if (Character.isHighSurrogate(targetLine.text[col - 1]) &&
                Character.isLowSurrogate(targetLine.text[col])
            ) {
                col -= 1
            }
        }

        return targetLine.start + col
    }
}
