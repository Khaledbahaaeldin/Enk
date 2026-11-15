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
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.enk.design.EnkMotion
import app.enk.design.EnkType
import app.enk.design.Jitter
import app.enk.design.LocalMonoFont
import app.enk.text.GridLayout
import app.enk.text.StruckPage

data class TextCell(
    val text: String,
    val index: Int,
    val col: Int,
    val codePoint: Int
)

/** Goldens only: draw the glyph at [index] as it looks [ageMs] after its key went down. */
data class FrozenStrike(val index: Int, val ageMs: Float, val weight: Float = 1f, val fast: Boolean = false)

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
    geometry: WriterGeometry,
    textColor: Color,
    reduceMotion: Boolean,
    modifier: Modifier = Modifier,
    strikes: StrikeLog? = null,
    clockMs: () -> Float = { 0f },
    frozen: FrozenStrike? = null,
    flashColor: Color = Color.White
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
        val now = clockMs()
        val densityVal = density
        val baselinePx = layoutFor("x").firstBaseline
        val viewportHeightDp = size.height / densityVal
        val visibleLines = visibleLineRange(
            textTopOffsetDp = currentOffsetDp,
            lineHeightDp = geometry.lineHeightDp,
            viewportHeightDp = viewportHeightDp,
            totalLines = lines.size
        )

        val flashFrame: StrikeFrame?
        val flashIndex: Int?
        if (!reduceMotion) {
            if (frozen != null) {
                val f = strikeFrame(frozen.ageMs, frozen.weight, frozen.fast)
                if (f.flashAlpha > 0f) {
                    flashFrame = f
                    flashIndex = frozen.index
                } else {
                    flashFrame = null
                    flashIndex = null
                }
            } else if (strikes != null) {
                val lk = strikes.latestKey(now)
                if (lk != null) {
                    val f = strikeFrame(now - lk.startMs, lk.weight, lk.fast)
                    if (f.flashAlpha > 0f) {
                        flashFrame = f
                        flashIndex = lk.index
                    } else {
                        flashFrame = null
                        flashIndex = null
                    }
                } else {
                    flashFrame = null
                    flashIndex = null
                }
            } else {
                flashFrame = null
                flashIndex = null
            }
        } else {
            flashFrame = null
            flashIndex = null
        }

        for (lineIdx in visibleLines) {
            val lineSpan = lines[lineIdx]
            val lineText = page.text.substring(lineSpan.start, lineSpan.end)
            val cells = cellsOfLine(lineText, lineSpan.start)
            val lineYDp = currentOffsetDp + lineIdx * geometry.lineHeightDp
            val lineYPx = lineYDp * densityVal

            for (cell in cells) {
                val colXDp = (geometry.textLeftDp - 12f) + cell.col * geometry.columnWidthDp
                val colXPx = colXDp * densityVal
                val cellWidthPx = geometry.columnWidthDp * densityVal
                val cellHeightPx = geometry.lineHeightDp * densityVal
                val pivotXPx = colXPx + cellWidthPx / 2f
                val pivotYPx = lineYPx + cellHeightPx / 2f

                val jitter = Jitter.forIndex(cell.index)

                val frame = if (reduceMotion) {
                    null
                } else when {
                    frozen?.index == cell.index -> strikeFrame(frozen.ageMs, frozen.weight, frozen.fast)
                    else -> strikes?.keyAt(cell.index, now)?.let { strikeFrame(now - it.startMs, it.weight, it.fast) }
                }

                if (frame != null && !frame.glyphVisible) {
                    continue
                }

                if (flashIndex == cell.index && flashFrame != null && flashFrame.flashAlpha > 0f) {
                    val rxPx = flashFrame.flashRadiusDp * densityVal
                    val ryPx = StrikeTiming.FLASH_RY_DP * densityVal
                    val flashCenterYPx = lineYPx + baselinePx
                    drawOval(
                        color = flashColor.copy(alpha = flashFrame.flashAlpha),
                        topLeft = Offset(pivotXPx - rxPx, flashCenterYPx - ryPx),
                        size = Size(rxPx * 2f, ryPx * 2f)
                    )
                }

                val scale = frame?.glyphScale ?: 1f
                val glyphColor = if (frame != null && frame.glyphDarken > 0f) {
                    lerp(textColor, Color.Black, frame.glyphDarken)
                } else {
                    textColor
                }
                val haloShadow = if (frame != null && frame.haloDp > 0f) {
                    Shadow(
                        color = textColor.copy(alpha = 0.5f * jitter.alpha),
                        offset = Offset.Zero,
                        blurRadius = frame.haloDp * densityVal
                    )
                } else {
                    null
                }

                val totalDyPx = jitter.dyDp * densityVal
                val totalAlpha = jitter.alpha

                withTransform({
                    translate(left = 0f, top = totalDyPx)
                    if (jitter.rotDeg != 0f) {
                        rotate(degrees = jitter.rotDeg, pivot = Offset(pivotXPx, pivotYPx))
                    }
                    if (scale != 1f) {
                        scale(scaleX = scale, scaleY = scale, pivot = Offset(pivotXPx, pivotYPx))
                    }
                }) {
                    if (!cell.text.first().isWhitespace()) {
                        drawText(
                            textLayoutResult = layoutFor(cell.text),
                            topLeft = Offset(colXPx, lineYPx),
                            color = glyphColor,
                            alpha = totalAlpha,
                            shadow = haloShadow
                        )
                    }

                    if (struckMask.getOrElse(cell.index) { false }) {
                        val struckEntry = if (!reduceMotion) strikes?.struckAt(cell.index, now) else null
                        val lineFraction = if (struckEntry != null) {
                            val age = now - struckEntry.startMs
                            val impact = if (struckEntry.fast) StrikeTiming.FAST_IMPACT_MS else StrikeTiming.IMPACT_MS
                            ((age - impact) / StrikeTiming.STRUCK_LINE_MS).coerceIn(0f, 1f)
                        } else {
                            1f
                        }

                        if (lineFraction > 0f) {
                            val strikeY = lineYPx + baselinePx - 0.27f * geometry.fontSp * densityVal
                            drawLine(
                                color = textColor.copy(alpha = 0.9f),
                                start = Offset(colXPx, strikeY),
                                end = Offset(colXPx + cellWidthPx * lineFraction, strikeY),
                                strokeWidth = 1.5f * densityVal
                            )
                        }
                    }
                }
            }
        }

        if (!reduceMotion && strikes != null) {
            val erases = strikes.erases(now)
            for (erase in erases) {
                if (erase.line in visibleLines) {
                    val lineYDp = currentOffsetDp + erase.line * geometry.lineHeightDp
                    val lineYPx = lineYDp * densityVal
                    val colXDp = (geometry.textLeftDp - 12f) + erase.col * geometry.columnWidthDp
                    val colXPx = colXDp * densityVal
                    val cellWidthPx = geometry.columnWidthDp * densityVal
                    val cellHeightPx = geometry.lineHeightDp * densityVal

                    val p = ((now - erase.startMs) / StrikeTiming.ERASE_WIPE_MS).coerceIn(0f, 1f)

                    clipRect(
                        left = colXPx + p * cellWidthPx,
                        top = lineYPx,
                        right = colXPx + cellWidthPx,
                        bottom = lineYPx + cellHeightPx
                    ) {
                        drawText(
                            textLayoutResult = layoutFor(erase.text),
                            topLeft = Offset(colXPx, lineYPx),
                            color = textColor
                        )
                    }

                    if (p < 1f) {
                        val barWidthPx = 3.dp.toPx()
                        val barLeftPx = colXPx + p * cellWidthPx - 1.dp.toPx()
                        drawRect(
                            color = flashColor.copy(alpha = 0.55f),
                            topLeft = Offset(barLeftPx, lineYPx),
                            size = Size(barWidthPx, cellHeightPx)
                        )
                    }
                }
            }
        }
    }
}
