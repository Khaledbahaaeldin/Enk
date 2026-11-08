package app.enk.writer

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.sp
import androidx.compose.ui.text.font.FontFamily
import app.enk.design.LocalMonoFont
import app.enk.design.EnkMotion
import app.enk.design.EnkType
import app.enk.design.Jitter
import app.enk.text.GridLayout
import app.enk.text.StruckPage

data class TextCell(
    val text: String,
    val index: Int,
    val col: Int,
    val codePoint: Int
)

/**
 * Calculates the range of line indices visible in the paper viewport.
 * Lines outside this range are culled to ensure 3,000-character pages do not draw 3,000 cells per frame.
 */
fun visibleLineRange(
    textTopOffsetDp: Float,
    lineHeightDp: Float,
    viewportHeightDp: Float,
    totalLines: Int
): IntRange {
    if (totalLines <= 0) return IntRange.EMPTY
    val first = kotlin.math.floor(-textTopOffsetDp / lineHeightDp).toInt().coerceIn(0, totalLines - 1)
    val last = kotlin.math.ceil((viewportHeightDp - textTopOffsetDp) / lineHeightDp).toInt().coerceIn(0, totalLines - 1)
    return if (first <= last) first..last else IntRange.EMPTY
}

/**
 * Walks code points in [lineText] with line start index [lineStart].
 * Column is defined as UTF-16 index minus line start, as GridLayout.caret defines it (DS 7.3).
 * Surrogate pairs are never split; an emoji occupies 1 cell at its first column,
 * leaving the next column empty. Combining characters are walked individually.
 */
fun cellsOfLine(lineText: String, lineStart: Int): List<TextCell> {
    if (lineText.isEmpty()) return emptyList()
    val cells = ArrayList<TextCell>()
    var offset = 0
    while (offset < lineText.length) {
        val cp = lineText.codePointAt(offset)
        val charCount = Character.charCount(cp)
        val text = lineText.substring(offset, offset + charCount)
        val index = lineStart + offset
        val col = offset
        cells.add(TextCell(text = text, index = index, col = col, codePoint = cp))
        offset += charCount
    }
    return cells
}

@Composable
fun PaperText(
    page: StruckPage,
    caret: Int,
    lastTypedIndex: Int?,
    geometry: WriterGeometry,
    textColor: Color,
    reduceMotion: Boolean,
    modifier: Modifier = Modifier,
    /** Freezes the stamp of the newest character at this progress (0..1) for goldens; null runs the animation. */
    stampFrame: Float? = null
) {
    val textMeasurer = rememberTextMeasurer()
    val lines = remember(page.text, geometry.cols) {
        GridLayout.wrap(page.text, geometry.cols)
    }
    val caretPos = remember(lines, caret, geometry.cols) {
        GridLayout.caret(lines, caret, geometry.cols)
    }

    val targetOffsetDp = geometry.textTopOffsetDp(caretPos.line)
    val feedAnim = remember { Animatable(targetOffsetDp) }

    LaunchedEffect(targetOffsetDp, reduceMotion) {
        val duration = if (reduceMotion) 100 else EnkMotion.feedMs
        val easing = if (reduceMotion) LinearEasing else EnkMotion.feed
        feedAnim.animateTo(
            targetValue = targetOffsetDp,
            animationSpec = tween(durationMillis = duration, easing = easing)
        )
    }

    val stampProgress = remember { Animatable(1f) }
    LaunchedEffect(lastTypedIndex) {
        if (lastTypedIndex != null) {
            stampProgress.snapTo(0f)
            val duration = if (reduceMotion) 100 else EnkMotion.stampMs
            val easing = if (reduceMotion) LinearEasing else EnkMotion.stamp
            stampProgress.animateTo(
                targetValue = 1f,
                animationSpec = tween(durationMillis = duration, easing = easing)
            )
        }
    }

    val currentOffsetDp = feedAnim.value
    val struckMask = remember(page.text, page.struck) {
        BooleanArray(page.text.length).also { mask ->
            for (r in page.struck) for (i in r) if (i in mask.indices) mask[i] = true
        }
    }
    val monoFont = LocalMonoFont.current
    val charLayoutCache = remember(textColor, geometry.fontSp, monoFont) {
        mutableMapOf<String, TextLayoutResult>()
    }

    fun layoutFor(text: String): TextLayoutResult = charLayoutCache.getOrPut(text) {
        textMeasurer.measure(
            text = text,
            style = EnkType.paper.copy(
                fontFamily = monoFont,
                fontSize = geometry.fontSp.sp,
                color = textColor
            )
        )
    }

    Canvas(modifier = modifier) {
        val densityVal = density
        val baselinePx = layoutFor("x").firstBaseline
        val viewportHeightDp = size.height / densityVal
        val visibleLines = visibleLineRange(
            textTopOffsetDp = currentOffsetDp,
            lineHeightDp = geometry.lineHeightDp,
            viewportHeightDp = viewportHeightDp,
            totalLines = lines.size
        )

        for (lineIdx in visibleLines) {
            val lineSpan = lines[lineIdx]
            val lineText = page.text.substring(lineSpan.start, lineSpan.end)
            val cells = cellsOfLine(lineText, lineSpan.start)
            val lineYDp = currentOffsetDp + lineIdx * geometry.lineHeightDp
            val lineYPx = lineYDp * densityVal

            for (cell in cells) {
                val colXDp = (geometry.textLeftDp - 12f) + cell.col * geometry.columnWidthDp
                val colXPx = colXDp * densityVal

                val jitter = Jitter.forIndex(cell.index)

                val isNewest = cell.index == lastTypedIndex
                val p = if (isNewest) (stampFrame ?: stampProgress.value) else 1f

                val stampScale = if (isNewest && !reduceMotion) 1.35f - 0.35f * p else 1f
                val stampDyDp = if (isNewest && !reduceMotion) -2f * (1f - p) else 0f
                val stampAlpha = if (isNewest) 0.2f + 0.8f * p else 1f

                val totalDyPx = (jitter.dyDp + stampDyDp) * densityVal
                val totalAlpha = jitter.alpha * stampAlpha

                val cellWidthPx = geometry.columnWidthDp * densityVal
                val cellHeightPx = geometry.lineHeightDp * densityVal
                val pivotXPx = colXPx + cellWidthPx / 2f
                val pivotYPx = lineYPx + cellHeightPx / 2f

                withTransform({
                    translate(left = 0f, top = totalDyPx)
                    if (jitter.rotDeg != 0f) {
                        rotate(degrees = jitter.rotDeg, pivot = Offset(pivotXPx, pivotYPx))
                    }
                    if (stampScale != 1f) {
                        scale(scaleX = stampScale, scaleY = stampScale, pivot = Offset(pivotXPx, pivotYPx))
                    }
                }) {
                    if (!cell.text.first().isWhitespace()) {
                        drawText(
                            textLayoutResult = layoutFor(cell.text),
                            topLeft = Offset(colXPx, lineYPx),
                            alpha = totalAlpha
                        )
                    }

                    // Struck line: 1.5 dp line drawn across struck character cell
                    if (struckMask.getOrElse(cell.index) { false }) {
                        // Through the middle of the lowercase letters: a quarter em above the baseline.
                        val strikeY = lineYPx + baselinePx - 0.27f * geometry.fontSp * densityVal
                        drawLine(
                            color = textColor.copy(alpha = 0.9f),
                            start = Offset(colXPx, strikeY),
                            end = Offset(colXPx + cellWidthPx, strikeY),
                            strokeWidth = 1.5f * densityVal
                        )
                    }
                }
            }
        }
    }
}

