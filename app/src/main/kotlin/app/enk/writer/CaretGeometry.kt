package app.enk.writer

/** Where the writer draws its caret, in writer coordinates (origin at the writer's top-left). */
object CaretGeometry {
    const val HEIGHT_DP = 2f

    /** Left edge of the caret for a column. */
    fun leftDp(geometry: WriterGeometry, col: Int): Float = geometry.textLeftDp + col * geometry.columnWidthDp

    /**
     * Top edge of the caret. It does not depend on the line: the text feed always puts the
     * caret's line just above the rail.
     */
    fun topDp(geometry: WriterGeometry): Float =
        geometry.railYDp - 23f - geometry.lineHeightDp + 1.42f * geometry.fontSp
}
