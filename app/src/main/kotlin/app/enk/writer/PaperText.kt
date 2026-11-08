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

/**
 * Linearly interpolates the feed offset between [start] and [target].
 */
fun interpolateFeedOffset(start: Float, target: Float, progress: Float): Float =
    start + (target - start) * progress

data class FeedAnimationState(
    val startOffsetDp: Float,
    val targetOffsetDp: Float,
    val durationMs: Int
)

/**
 * Restarts a feed animation from the current in-flight offset [currentOffsetDp]
 * towards [newTargetOffsetDp], preventing jumps when rapid typing triggers feeds.
 */
fun interruptFeed(
    currentOffsetDp: Float,
    newTargetOffsetDp: Float,
    reduceMotion: Boolean
): FeedAnimationState = FeedAnimationState(
    startOffsetDp = currentOffsetDp,
    targetOffsetDp = newTargetOffsetDp,
    durationMs = if (reduceMotion) 100 else 200
)

@Composable
fun PaperText(
    page: StruckPage,
    caret: Int,
    lastTypedIndex: Int?,
    geometry: WriterGeometry,
    textColor: Color,
    reduceMotion: Boolean,
    modifier: Modifier = Modifier
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
    val charLayoutCache = remember(textColor, geometry.fontSp) {
        mutableMapOf<String, TextLayoutResult>()
    }

    Canvas(modifier = modifier) {
        val densityVal = density
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
                val p = if (isNewest) stampProgress.value else 1f

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
                        val layoutResult = charLayoutCache.getOrPut(cell.text) {
                            textMeasurer.measure(
                                text = cell.text,
                                style = EnkType.paper.copy(
                                    fontSize = geometry.fontSp.sp,
                                    color = textColor
                                )
                            )
                        }
                        drawText(
                            textLayoutResult = layoutResult,
                            topLeft = Offset(colXPx, lineYPx),
                            alpha = totalAlpha
                        )
                    }

                    // Struck line: 1.5 dp line drawn across struck character cell
                    if (page.struck.any { cell.index in it }) {
                        val strikeY = lineYPx + (geometry.fontSp * 0.72f) * densityVal
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
