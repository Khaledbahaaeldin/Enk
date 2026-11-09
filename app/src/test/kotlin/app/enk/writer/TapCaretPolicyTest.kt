package app.enk.writer

import org.junit.Assert.assertEquals
import org.junit.Test

class TapCaretPolicyTest {

    private val geometry = WriterGeometry(
        cols = 28,
        fontSp = 14f,
        columnWidthDp = 8.4f,
        lineHeightDp = 23.8f,
        textLeftDp = 36f,
        textWidthDp = 235.2f,
        railYDp = 500f,
        paperTopDp = 64f
    )

    @Test
    fun tapOnEmptyLine() {
        val lines = listOf(GridLine(0, 0, ""))
        val index = TapCaretPolicy.indexAtTap(
            xDp = 36f,
            yDp = 64f, // paperTopDp + feedOffsetDp (feedOffsetDp = 0)
            geometry = geometry,
            lines = lines,
            feedOffsetDp = 0f,
            paperTopDp = 64f
        )
        assertEquals(0, index)
    }

    @Test
    fun tapAboveFirstLineClampsToFirstLine() {
        val lines = listOf(
            GridLine(0, 5, "Hello"),
            GridLine(6, 11, "World")
        )
        // yDp well above line 0
        val index = TapCaretPolicy.indexAtTap(
            xDp = 36f + 2 * 8.4f,
            yDp = 0f,
            geometry = geometry,
            lines = lines,
            feedOffsetDp = 100f,
            paperTopDp = 64f
        )
        assertEquals(2, index) // col 2 on line 0 -> index 0 + 2 = 2
    }

    @Test
    fun tapBelowLastLineClampsToLastLine() {
        val lines = listOf(
            GridLine(0, 5, "Hello"),
            GridLine(6, 11, "World")
        )
        // yDp well below line 1
        val index = TapCaretPolicy.indexAtTap(
            xDp = 36f + 3 * 8.4f,
            yDp = 1000f,
            geometry = geometry,
            lines = lines,
            feedOffsetDp = 0f,
            paperTopDp = 64f
        )
        assertEquals(9, index) // line 1 start (6) + col 3 = 9
    }

    @Test
    fun tapLeftOfTextClampsToZero() {
        val lines = listOf(GridLine(0, 5, "Hello"))
        val index = TapCaretPolicy.indexAtTap(
            xDp = 10f, // left of textLeftDp (36f)
            yDp = 64f,
            geometry = geometry,
            lines = lines,
            feedOffsetDp = 0f,
            paperTopDp = 64f
        )
        assertEquals(0, index)
    }

    @Test
    fun tapPastEndOfShortLineClampsToEnd() {
        val lines = listOf(GridLine(0, 5, "Hello"))
        val index = TapCaretPolicy.indexAtTap(
            xDp = 300f, // far past end of "Hello"
            yDp = 64f,
            geometry = geometry,
            lines = lines,
            feedOffsetDp = 0f,
            paperTopDp = 64f
        )
        assertEquals(5, index)
    }

    @Test
    fun tapOnEmojiDoesNotSplitSurrogatePair() {
        val emoji = "\uD83D\uDE00" // smiley emoji: high surrogate + low surrogate
        val text = "a" + emoji + "b"
        val lines = listOf(GridLine(0, text.length, text))

        // Index 0: 'a'
        // Index 1: high surrogate (\uD83D)
        // Index 2: low surrogate (\uDE00)
        // Tapping exactly at col 2 (where low surrogate is):
        val index = TapCaretPolicy.indexAtTap(
            xDp = 36f + 2 * 8.4f,
            yDp = 64f,
            geometry = geometry,
            lines = lines,
            feedOffsetDp = 0f,
            paperTopDp = 64f
        )
        // Must clamp backwards to start of surrogate pair (col 1), never splitting surrogate pair
        assertEquals(1, index)
    }

    @Test
    fun emptyLinesReturnsZero() {
        val index = TapCaretPolicy.indexAtTap(
            xDp = 50f,
            yDp = 100f,
            geometry = geometry,
            lines = emptyList(),
            feedOffsetDp = 0f,
            paperTopDp = 64f
        )
        assertEquals(0, index)
    }
}
