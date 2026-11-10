package app.enk.search

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** Awkward queries and texts for [matchStyled]: the match must be where the text says, never beyond it. */
class MatchTextEdgeCasesTest {

    private fun ranges(text: String, query: String) =
        matchStyled(text, query).spanStyles.map { it.start until it.end }

    @Test
    fun regexCharactersAreLiteral() {
        assertEquals(listOf(0 until 3), ranges("a.b axb", "a.b"))
        assertEquals(listOf(0 until 2), ranges("(( and ))", "(("))
        assertEquals(listOf(1 until 2), ranges("a\\b", "\\"))
        assertTrue(ranges("anything", "[").isEmpty())
        assertTrue(ranges("anything", "*").isEmpty())
    }

    @Test
    fun emojiMatchAsWholePairs() {
        val text = "a😀b😀"
        assertEquals(listOf(1 until 3, 4 until 6), ranges(text, "😀"))
    }

    @Test
    fun aLoneHalfOfAPairNeverMatchesInsideOne() {
        val text = "x😀y"
        assertTrue("a high surrogate alone must not split the emoji", ranges(text, "\uD83D").isEmpty())
        assertTrue(ranges(text, "\uDE00").isEmpty())
    }

    @Test
    fun overlappingWordsMergeIntoOneSpan() {
        assertEquals(listOf(0 until 4), ranges("rain", "ain rai"))
    }

    @Test
    fun aCharacterWhoseLowerCaseHasAnotherLengthCannotShiftTheMatch() {
        // U+0130 (capital dotted I) lower-cases to two characters; the match must still start at 9.
        val text = "İstanbul rain"
        assertEquals(listOf(9 until 13), ranges(text, "rain"))
    }

    @Test
    fun emptyAndBlankInputsReturnThePlainText() {
        assertEquals("", matchStyled("", "rain").text)
        assertTrue(matchStyled("rain", "").spanStyles.isEmpty())
        assertTrue(matchStyled("rain", "   ").spanStyles.isEmpty())
        assertEquals("rain", matchStyled("rain", "zzz").text)
    }

    @Test
    fun aTenThousandCharacterTextIsFast() {
        val text = "the rain on the roof and the rain on the glass. ".repeat(210)
        val start = System.nanoTime()
        val result = matchStyled(text, "rain glass")
        val ms = (System.nanoTime() - start) / 1_000_000
        assertTrue("took $ms ms", ms < 2_000)
        assertTrue(result.spanStyles.size >= 420)
    }

    @Test
    fun theMatchInkIsTheDesignSystemOne() {
        assertEquals(androidx.compose.ui.graphics.Color(0xFF14110D), MatchInk)
    }
}
