package app.enk.start

import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.Dp
import app.enk.text.GridLayout
import app.enk.writer.CaretGeometry
import app.enk.writer.WriterGeometry

/** Window insets in dp. */
data class InsetsDp(val left: Float, val top: Float, val right: Float, val bottom: Float) {
    companion object {
        val None = InsetsDp(0f, 0f, 0f, 0f)
    }
}

/**
 * Builds the animation's landing targets from the writer's own layout, so the overlay ends exactly
 * where the writer draws its rail, carriage and caret. The writer lays itself out inside the
 * safe-drawing insets; the overlay covers the whole window, so everything is shifted by the insets.
 */
object StartLayout {
    /** The paper starts this far below the bottom edge, so its shadow is out of sight too. */
    private const val PAPER_OFFSCREEN_MARGIN_DP = 24f

    fun targets(
        rawWindowWidthDp: Float,
        rawWindowHeightDp: Float,
        insets: InsetsDp,
        fontScale: Float,
        firstPageText: String,
        lightSplash: Boolean
    ): StartTargets {
        val windowWidthDp = if (rawWindowWidthDp.isFinite()) rawWindowWidthDp.coerceAtLeast(0f) else 0f
        val windowHeightDp = if (rawWindowHeightDp.isFinite()) rawWindowHeightDp.coerceAtLeast(0f) else 0f
        val writerWidth = (windowWidthDp - insets.left - insets.right).coerceAtLeast(0f)
        val writerHeight = (windowHeightDp - insets.top - insets.bottom).coerceAtLeast(0f)
        val geometry = WriterGeometry.compute(writerWidth, writerHeight, fontScale)
        val lines = GridLayout.wrap(firstPageText, geometry.cols)
        val col = GridLayout.caret(lines, firstPageText.length, geometry.cols).col

        val caretLeft = insets.left + CaretGeometry.leftDp(geometry, col)
        val caretTop = insets.top + CaretGeometry.topDp(geometry)
        return StartTargets(
            railY = insets.top + geometry.railYDp,
            screenWidth = windowWidthDp,
            screenHeight = windowHeightDp,
            carriageX = insets.left + geometry.carriageCenterXDp(col),
            caretRect = Rect(
                left = caretLeft,
                top = caretTop,
                right = caretLeft + geometry.columnWidthDp,
                bottom = caretTop + CaretGeometry.HEIGHT_DP
            ),
            paperHeight = windowHeightDp - (insets.top + geometry.paperTopDp) + PAPER_OFFSCREEN_MARGIN_DP,
            lightSplash = lightSplash
        )
    }
}

/**
 * The targets for the window the writer lives in: [maxWidth] x [maxHeight] is the space the writer's
 * parent gives it, and the insets and font scale are read the same way the writer reads them.
 */
@Composable
internal fun rememberStartTargets(
    maxWidth: Dp,
    maxHeight: Dp,
    firstPageText: String,
    lightSplash: Boolean
): StartTargets {
    val density = LocalDensity.current
    val direction = LocalLayoutDirection.current
    val safe = WindowInsets.safeDrawing
    val insets = with(density) {
        InsetsDp(
            left = safe.getLeft(this, direction).toDp().value,
            top = safe.getTop(this).toDp().value,
            right = safe.getRight(this, direction).toDp().value,
            bottom = safe.getBottom(this).toDp().value
        )
    }
    return remember(maxWidth, maxHeight, insets, density.fontScale, firstPageText, lightSplash) {
        StartLayout.targets(maxWidth.value, maxHeight.value, insets, density.fontScale, firstPageText, lightSplash)
    }
}
