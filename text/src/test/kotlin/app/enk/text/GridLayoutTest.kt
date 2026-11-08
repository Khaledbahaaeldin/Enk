package app.enk.text

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

class GridLayoutTest {
    // The plan specifies length 85 with LineSpan(77, 85) and caretOf(85) == CaretPos(4, 8).
    // A trailing space brings the 84-char sample to length 85.
    private val SAMPLE = "dear future me,\nthe rain is soft on the roof today.\nI left the window open a little. "
    private val sampleLines by lazy { GridLayout.wrap(SAMPLE, 28) }
    private fun caretOf(index: Int) = GridLayout.caret(sampleLines, index, 28)

    @Test
    fun wrapSample() = assertEquals(
        listOf(LineSpan(0, 15), LineSpan(16, 45), LineSpan(45, 51), LineSpan(52, 77), LineSpan(77, 85)),
        GridLayout.wrap(SAMPLE, 28)
    )

    @Test
    fun wrapSampleWithoutTrailingSpace() {
        val s = "dear future me,\nthe rain is soft on the roof today.\nI left the window open a little."
        assertEquals(
            listOf(LineSpan(0, 15), LineSpan(16, 45), LineSpan(45, 51), LineSpan(52, 77), LineSpan(77, 84)),
            GridLayout.wrap(s, 28)
        )
    }

    @Test
    fun caretAtEnd() = assertEquals(CaretPos(4, 8), caretOf(85))

    @Test
    fun caretAtWrap() = assertEquals(CaretPos(2, 0), caretOf(45))

    @Test
    fun caretBeforeNewline() = assertEquals(CaretPos(0, 15), caretOf(15))

    @Test
    fun caretAfterNewline() = assertEquals(CaretPos(1, 0), caretOf(16))

    @Test
    fun emptyParagraph() = assertEquals(
        listOf(LineSpan(0, 1), LineSpan(2, 2), LineSpan(3, 4)),
        GridLayout.wrap("a\n\nb", 28)
    )

    @Test
    fun longWordBreaks() = assertEquals(
        listOf(LineSpan(0, 28), LineSpan(28, 40)),
        GridLayout.wrap("x".repeat(40), 28)
    )

    @Test
    fun neverSplitsSurrogate() {
        val t = "x".repeat(27) + "😀" + "y" // emoji at index 27..28
        GridLayout.wrap(t, 28).forEach {
            assertFalse(
                Character.isLowSurrogate(t[it.start]) && it.start > 0 && Character.isHighSurrogate(t[it.start - 1])
            )
        }
    }

    @Test
    fun emptyText() = assertEquals(listOf(LineSpan(0, 0)), GridLayout.wrap("", 28))

    @Test(expected = IllegalArgumentException::class)
    fun nonPositiveColsThrows() {
        GridLayout.wrap("hello", 0)
    }

    @Test(expected = IllegalArgumentException::class)
    fun negativeColsThrows() {
        GridLayout.wrap("hello", -5)
    }

    @Test
    fun singleCharacter() {
        val lines = GridLayout.wrap("a", 28)
        assertEquals(listOf(LineSpan(0, 1)), lines)
        assertEquals(CaretPos(0, 0), GridLayout.caret(lines, 0, 28))
        assertEquals(CaretPos(0, 1), GridLayout.caret(lines, 1, 28))
    }

    @Test
    fun newlinesEdgeCases() {
        // Single newline
        val singleNl = GridLayout.wrap("\n", 28)
        assertEquals(listOf(LineSpan(0, 0), LineSpan(1, 1)), singleNl)
        assertEquals(CaretPos(0, 0), GridLayout.caret(singleNl, 0, 28))
        assertEquals(CaretPos(1, 0), GridLayout.caret(singleNl, 1, 28))

        // Multiple consecutive newlines
        val multiNl = GridLayout.wrap("\n\n\n", 28)
        assertEquals(
            listOf(LineSpan(0, 0), LineSpan(1, 1), LineSpan(2, 2), LineSpan(3, 3)),
            multiNl
        )

        // Leading newline
        val leadingNl = GridLayout.wrap("\nabc", 28)
        assertEquals(listOf(LineSpan(0, 0), LineSpan(1, 4)), leadingNl)

        // Trailing newline
        val trailingNl = GridLayout.wrap("abc\n", 28)
        assertEquals(listOf(LineSpan(0, 3), LineSpan(4, 4)), trailingNl)
    }

    @Test
    fun wordLengthsRelativeToCols() {
        val cols = 5

        // Exactly cols
        assertEquals(listOf(LineSpan(0, 5)), GridLayout.wrap("abcde", cols))

        // cols + 1 without space
        assertEquals(listOf(LineSpan(0, 5), LineSpan(5, 6)), GridLayout.wrap("abcdef", cols))

        // 2 * cols without space
        assertEquals(listOf(LineSpan(0, 5), LineSpan(5, 10)), GridLayout.wrap("abcdefghij", cols))

        // 2 * cols + 1 without space
        assertEquals(
            listOf(LineSpan(0, 5), LineSpan(5, 10), LineSpan(10, 11)),
            GridLayout.wrap("abcdefghijk", cols)
        )
    }

    @Test
    fun surrogatePairsAtBoundaries() {
        val cols = 5
        // High surrogate at cols - 1, low surrogate at cols
        val strBoundary = "xxxx" + "😀" + "y" // lengths: 4 + 2 + 1 = 7. cols = 5
        val linesBoundary = GridLayout.wrap(strBoundary, cols)
        assertEquals(listOf(LineSpan(0, 4), LineSpan(4, 7)), linesBoundary)

        // Emoji starting right at cols
        val strAtCols = "xxxxx" + "😀" // lengths: 5 + 2 = 7. cols = 5
        val linesAtCols = GridLayout.wrap(strAtCols, cols)
        assertEquals(listOf(LineSpan(0, 5), LineSpan(5, 7)), linesAtCols)

        // Emoji fitting completely at cols - 2 and cols - 1
        val strInside = "xxx" + "😀" + "yy" // lengths: 3 + 2 + 2 = 7. cols = 5
        val linesInside = GridLayout.wrap(strInside, cols)
        assertEquals(listOf(LineSpan(0, 5), LineSpan(5, 7)), linesInside)

        // Multiple emojis wrapping cleanly
        val emojis = "😀😁😂" // 3 emojis * 2 = 6 code units. cols = 2
        assertEquals(
            listOf(LineSpan(0, 2), LineSpan(2, 4), LineSpan(4, 6)),
            GridLayout.wrap(emojis, 2)
        )
    }

    @Test
    fun spacesAndBreakBoundaries() {
        val cols = 5

        // Space exactly at cols
        // "hello world" -> space at index 5
        assertEquals(
            listOf(LineSpan(0, 6), LineSpan(6, 11)),
            GridLayout.wrap("hello world", cols)
        )

        // Space at cols - 1
        // "hell world" -> space at index 4
        assertEquals(
            listOf(LineSpan(0, 5), LineSpan(5, 10)),
            GridLayout.wrap("hell world", cols)
        )

        // Space preceded by word longer than cols
        // "abcdef world" -> "abcde", "f ", "world"
        assertEquals(
            listOf(LineSpan(0, 5), LineSpan(5, 7), LineSpan(7, 12)),
            GridLayout.wrap("abcdef world", cols)
        )
    }

    @Test
    fun caretClampingAndBounds() {
        val lines = listOf(LineSpan(0, 15), LineSpan(16, 30))

        // Negative index clamped to (0, 0)
        assertEquals(CaretPos(0, 0), GridLayout.caret(lines, -1, 10))
        assertEquals(CaretPos(0, 0), GridLayout.caret(lines, -100, 10))

        // Index beyond text length clamps col to cols on the last line
        assertEquals(CaretPos(1, 10), GridLayout.caret(lines, 999, 10))

        // Empty lines list returns (0, 0)
        assertEquals(CaretPos(0, 0), GridLayout.caret(emptyList(), 0, 10))
        assertEquals(CaretPos(0, 0), GridLayout.caret(emptyList(), 5, 10))
        assertEquals(CaretPos(0, 0), GridLayout.caret(emptyList(), -1, 10))
    }

    @Test
    fun oneColumnGridKeepsSurrogatePairsWhole() {
        val t = "\uD83D\uDE00a"
        assertEquals(listOf(LineSpan(0, 2), LineSpan(2, 3)), GridLayout.wrap(t, 1))
    }
}
