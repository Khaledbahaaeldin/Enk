package app.enk.writer

import kotlin.math.max
import kotlin.math.min

object TypeOn {
    const val LEAD_MS = 120f
    const val SWEEP_MS = 112f
    const val RETURN_MS = 85f
    const val MAX_STAGGER_MS = 150f
    const val BUDGET_MS = 900f
    const val MAX_CHARS = 2000
    const val PASTE_MAX_SWEEP_MS = 320f

    /** min(150, budget / max(1, visibleLines)) */
    fun staggerMs(visibleLines: Int, budgetMs: Float = BUDGET_MS): Float {
        val lines = max(1, visibleLines)
        return min(MAX_STAGGER_MS, budgetMs / lines)
    }

    /** LEAD_MS + lineInView * staggerMs */
    fun lineStartMs(lineInView: Int, staggerMs: Float): Float {
        return LEAD_MS + lineInView * staggerMs
    }

    /** When the glyph in column [col] of a line becomes visible, ms from the start of the type-on. */
    fun glyphRevealMs(lineInView: Int, col: Int, cols: Int, staggerMs: Float): Float {
        val lineStart = lineStartMs(lineInView, staggerMs)
        if (cols <= 0) return lineStart
        val fraction = (col.toFloat() / cols.toFloat()).coerceIn(0f, 1f)
        return lineStart + fraction * SWEEP_MS
    }

    /** lineStart(last) + SWEEP_MS + RETURN_MS + 90 (the final glide) */
    fun totalMs(visibleLines: Int, staggerMs: Float): Float {
        if (visibleLines <= 0) return 0f
        val lastLine = visibleLines - 1
        return lineStartMs(lastLine, staggerMs) + SWEEP_MS + RETURN_MS + 90f
    }

    private fun easeOut(x: Float): Float {
        val inv = 1f - x.coerceIn(0f, 1f)
        return 1f - inv * inv * inv
    }

    /**
     * The carriage position in columns (0..cols) at [tMs]; null before LEAD_MS and after totalMs.
     * During [start, start + SWEEP_MS]: clamp(((t - start) / SWEEP_MS) * cols, 0, lineLengths[i])
     * During (start + SWEEP_MS, start + SWEEP_MS + RETURN_MS]: lineLengths[i] * (1 - easeOut(...))
     * Between lines: 0.
     * If lines overlap in time (small stagger), the latest started line wins.
     * Empty lineLengths returns null.
     */
    fun carriageColumn(tMs: Float, lineLengths: List<Int>, cols: Int, staggerMs: Float): Float? {
        if (lineLengths.isEmpty()) return null
        val tot = totalMs(lineLengths.size, staggerMs)
        if (tMs < LEAD_MS || tMs > tot) return null

        var activeLine = -1
        for (i in lineLengths.indices.reversed()) {
            if (tMs >= lineStartMs(i, staggerMs)) {
                activeLine = i
                break
            }
        }
        if (activeLine == -1) return 0f

        val start = lineStartMs(activeLine, staggerMs)
        val lineLen = lineLengths[activeLine].toFloat()
        val maxCol = if (cols > 0) lineLen.coerceAtMost(cols.toFloat()) else 0f

        return when {
            tMs <= start + SWEEP_MS -> {
                val progress = if (SWEEP_MS > 0f) (tMs - start) / SWEEP_MS else 1f
                (progress * cols).coerceIn(0f, maxCol)
            }
            tMs <= start + SWEEP_MS + RETURN_MS -> {
                val returnProgress = if (RETURN_MS > 0f) (tMs - (start + SWEEP_MS)) / RETURN_MS else 1f
                val eased = easeOut(returnProgress)
                maxCol * (1f - eased)
            }
            else -> 0f
        }
    }

    /** True when the reveal should be skipped: more than MAX_CHARS visible characters, or no visible lines. */
    fun skip(visibleChars: Int, visibleLines: Int): Boolean {
        return visibleChars > MAX_CHARS || visibleLines <= 0
    }
}
