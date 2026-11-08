package app.enk.design.logo

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color

object LogoSpec {
    // 108 x 108 grid, values from the logo spec
    val typedLines: List<Rect> = listOf(
        Rect(36f, 24f, 36f + 28f, 24f + 2.6f),
        Rect(36f, 29.5f, 36f + 22f, 29.5f + 2.6f),
        Rect(36f, 35f, 36f + 18f, 35f + 2.6f)
    )
    val caret: Rect = Rect(55.5f, 35f, 55.5f + 11f, 35f + 2.1f)
    val rail: Rect = Rect(6f, 56f, 6f + 96f, 56f + 8.5f)
    val carriageCentre: Offset = Offset(61f, 60.25f)
    const val CARRIAGE_SCALE = 1.25f
    const val VISIBLE_DIAMETER = 72f
    const val SAFE_DIAMETER = 66f

    /** Writer cream: the symbol's typed lines and the wordmark on dark. */
    val CREAM = Color(0xFFF3EEE3)
}
