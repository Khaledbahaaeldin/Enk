package app.enk.writer

import app.enk.design.Jitter
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PaperTextTest {

    // --- 1. visibleLineRange tests ---

    @Test
    fun visibleLineRangeFirstLine() {
        val geometry = WriterGeometry.compute(widthDp = 400f, heightDp = 820f, fontScale = 1.0f)
        val textTopOffsetDp = geometry.textTopOffsetDp(0)
        val viewportHeightDp = 744f // paper height: 820 - 76
        val totalLines = 20

        val range = visibleLineRange(
            textTopOffsetDp = textTopOffsetDp,
            lineHeightDp = geometry.lineHeightDp,
            viewportHeightDp = viewportHeightDp,
            totalLines = totalLines
        )

        assertEquals(0, range.first)
        assertTrue("Range must contain line 0", range.contains(0))
        assertTrue("Range upper bound must not exceed totalLines", range.last < totalLines)
    }

    @Test
    fun visibleLineRangeLastLine() {
        val geometry = WriterGeometry.compute(widthDp = 400f, heightDp = 820f, fontScale = 1.0f)
        val totalLines = 30
        val lastLine = totalLines - 1
        val textTopOffsetDp = geometry.textTopOffsetDp(lastLine)
        val viewportHeightDp = 744f

        val range = visibleLineRange(
            textTopOffsetDp = textTopOffsetDp,
            lineHeightDp = geometry.lineHeightDp,
            viewportHeightDp = viewportHeightDp,
            totalLines = totalLines
        )

        assertTrue("Range must contain lastLine", range.contains(lastLine))
        assertEquals(lastLine, range.last)
        assertTrue("Earlier lines (e.g. line 0) must be scrolled off", range.first > 0)
    }

    @Test
    fun visibleLineRangeEmptyText() {
        val geometry = WriterGeometry.compute(widthDp = 400f, heightDp = 820f, fontScale = 1.0f)
        val rangeZero = visibleLineRange(
            textTopOffsetDp = geometry.textTopOffsetDp(0),
            lineHeightDp = geometry.lineHeightDp,
            viewportHeightDp = 744f,
            totalLines = 0
        )
        assertTrue("Zero lines must return empty range", rangeZero.isEmpty())
    }

    @Test
    fun visibleLineRangeWindowLargerThanText() {
        val geometry = WriterGeometry.compute(widthDp = 400f, heightDp = 820f, fontScale = 1.0f)
        val totalLines = 3
        val range = visibleLineRange(
            textTopOffsetDp = geometry.textTopOffsetDp(0),
            lineHeightDp = geometry.lineHeightDp,
            viewportHeightDp = 1500f, // huge viewport
            totalLines = totalLines
        )
        assertEquals(0, range.first)
        assertEquals(2, range.last)
        assertEquals(3, range.count())
    }

    @Test
    fun visibleLineRangeExtremeOffsetsAndBoundaries() {
        val geometry = WriterGeometry.compute(widthDp = 400f, heightDp = 820f, fontScale = 1.0f)
        val totalLines = 10

        // Scrolled way far down (all lines above viewport)
        val rangeScrolledPastEnd = visibleLineRange(
            textTopOffsetDp = -5000f,
            lineHeightDp = geometry.lineHeightDp,
            viewportHeightDp = 500f,
            totalLines = totalLines
        )
        // Coerced to totalLines - 1
        assertEquals(9, rangeScrolledPastEnd.first)
        assertEquals(9, rangeScrolledPastEnd.last)

        // Single line total
        val rangeSingleLine = visibleLineRange(
            textTopOffsetDp = geometry.textTopOffsetDp(0),
            lineHeightDp = geometry.lineHeightDp,
            viewportHeightDp = 744f,
            totalLines = 1
        )
        assertEquals(0, rangeSingleLine.first)
        assertEquals(0, rangeSingleLine.last)
    }

    // --- 2. cellsOfLine tests ---

    @Test
    fun cellsOfLineEmptyLineReturnsEmptyList() {
        val cells = cellsOfLine("", lineStart = 0)
        assertTrue("Empty line must produce 0 cells", cells.isEmpty())
    }

    @Test
    fun cellsOfLineAsciiTextWalksIndicesAndColumns() {
        val cells = cellsOfLine("Hello", lineStart = 0)
        assertEquals(5, cells.size)
        assertEquals("H", cells[0].text)
        assertEquals(0, cells[0].index)
        assertEquals(0, cells[0].col)

        assertEquals("e", cells[1].text)
        assertEquals(1, cells[1].index)
        assertEquals(1, cells[1].col)

        assertEquals("o", cells[4].text)
        assertEquals(4, cells[4].index)
        assertEquals(4, cells[4].col)
    }

    @Test
    fun cellsOfLineWithNonZeroLineStartCalculatesColumnRelativeToStart() {
        val cells = cellsOfLine("World", lineStart = 15)
        assertEquals(5, cells.size)
        assertEquals("W", cells[0].text)
        assertEquals(15, cells[0].index)
        assertEquals(0, cells[0].col)

        assertEquals("d", cells[4].text)
        assertEquals(19, cells[4].index)
        assertEquals(4, cells[4].col)
    }

    @Test
    fun cellsOfLineNeverSplitsSurrogatePairAndLeavesNextColEmpty() {
        // "A😀B": 'A' (idx 0), '😀' (idx 1..2, surrogate pair), 'B' (idx 3)
        val text = "A\uD83D\uDE00B"
        val cells = cellsOfLine(text, lineStart = 0)

        assertEquals(3, cells.size)

        // Cell 0: 'A'
        assertEquals("A", cells[0].text)
        assertEquals(0, cells[0].index)
        assertEquals(0, cells[0].col)

        // Cell 1: '😀' (Unicode 0x1F600)
        assertEquals("\uD83D\uDE00", cells[1].text)
        assertEquals(0x1F600, cells[1].codePoint)
        assertEquals(1, cells[1].index)
        assertEquals(1, cells[1].col)

        // Cell 2: 'B'
        // Index in string is 3 (after 2-char surrogate pair)
        // Col is index - lineStart = 3 - 0 = 3
        // Column 2 stays empty!
        assertEquals("B", cells[2].text)
        assertEquals(3, cells[2].index)
        assertEquals(3, cells[2].col)
    }

    @Test
    fun cellsOfLineCombiningCharactersWalkedIndividually() {
        // 'e' (U+0065) + combining acute accent (U+0301)
        val text = "e\u0301"
        val cells = cellsOfLine(text, lineStart = 0)

        assertEquals(2, cells.size)
        assertEquals("e", cells[0].text)
        assertEquals(0, cells[0].index)
        assertEquals(0, cells[0].col)

        assertEquals("\u0301", cells[1].text)
        assertEquals(1, cells[1].index)
        assertEquals(1, cells[1].col)
    }

    @Test
    fun cellsOfLineMultipleConsecutiveEmojisColumnsAdvanceByTwo() {
        // "😀🎉🚀" -> 3 emojis, each 2 UTF-16 code units
        val text = "😀🎉🚀"
        val cells = cellsOfLine(text, lineStart = 10)

        assertEquals(3, cells.size)

        assertEquals("😀", cells[0].text)
        assertEquals(10, cells[0].index)
        assertEquals(0, cells[0].col)

        assertEquals("🎉", cells[1].text)
        assertEquals(12, cells[1].index)
        assertEquals(2, cells[1].col)

        assertEquals("🚀", cells[2].text)
        assertEquals(14, cells[2].index)
        assertEquals(4, cells[2].col)
    }

    @Test
    fun jitterDeterministicByPageTextIndex() {
        val text = "Enk Typewriter"
        val lineStartLine0 = 0
        val cellsLine0 = cellsOfLine(text, lineStart = lineStartLine0)

        for (i in text.indices) {
            val cell0 = cellsLine0[i]
            val jitter0 = Jitter.forIndex(cell0.index)
            val jitterPageIdx = Jitter.forIndex(i)
            assertEquals("Jitter must be keyed by character page text index", jitterPageIdx, jitter0)
        }
    }

    // --- 3. feedOffsetLogic tests ---

    @Test
    fun feedOffsetInterpolationComputesLinearOffset() {
        val start = 100f
        val target = 200f
        assertEquals(100f, interpolateFeedOffset(start, target, 0f), 0.001f)
        assertEquals(150f, interpolateFeedOffset(start, target, 0.5f), 0.001f)
        assertEquals(200f, interpolateFeedOffset(start, target, 1f), 0.001f)
    }

    @Test
    fun interruptFeedRestartsFromCurrentOffsetNotOldTarget() {
        val oldStart = 387.81f
        val oldTarget = 321.43f
        val progress = 0.5f
        val currentOffset = interpolateFeedOffset(oldStart, oldTarget, progress) // 354.62f
        val newTarget = 255.05f

        val newFeed = interruptFeed(
            currentOffsetDp = currentOffset,
            newTargetOffsetDp = newTarget,
            reduceMotion = false
        )

        assertEquals("Feed must start from current offset", currentOffset, newFeed.startOffsetDp, 0.001f)
        assertEquals("Feed target must be new target", newTarget, newFeed.targetOffsetDp, 0.001f)
        assertEquals("Duration without reduceMotion must be 200ms", 200, newFeed.durationMs)
    }

    @Test
    fun interruptFeedUses100msWithReduceMotion() {
        val newFeed = interruptFeed(
            currentOffsetDp = 100f,
            newTargetOffsetDp = 200f,
            reduceMotion = true
        )
        assertEquals("Duration with reduceMotion must be 100ms", 100, newFeed.durationMs)
    }

    @Test
    fun feedOffsetNegativeAndZeroDistance() {
        // Negative scroll (e.g. scrolling towards top)
        val start = -200f
        val target = -400f
        assertEquals(-200f, interpolateFeedOffset(start, target, 0f), 0.001f)
        assertEquals(-300f, interpolateFeedOffset(start, target, 0.5f), 0.001f)
        assertEquals(-400f, interpolateFeedOffset(start, target, 1f), 0.001f)

        // Zero distance (no movement)
        val stationary = interruptFeed(
            currentOffsetDp = 150f,
            newTargetOffsetDp = 150f,
            reduceMotion = false
        )
        assertEquals(150f, stationary.startOffsetDp, 0.001f)
        assertEquals(150f, stationary.targetOffsetDp, 0.001f)
    }
}
