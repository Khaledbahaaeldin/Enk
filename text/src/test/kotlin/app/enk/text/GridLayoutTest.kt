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
}
