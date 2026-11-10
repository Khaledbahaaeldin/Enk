package app.enk.search

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class MatchTextTest {

    @Test
    fun matchStyledMarksEveryOccurrence() {
        val result = matchStyled("rain on the rain", "rain")
        assertEquals(2, result.spanStyles.size)
        assertEquals(0, result.spanStyles[0].start)
        assertEquals(4, result.spanStyles[0].end)
        assertEquals(12, result.spanStyles[1].start)
        assertEquals(16, result.spanStyles[1].end)
    }

    @Test
    fun matchStyledIsCaseInsensitive() {
        val result = matchStyled("Rain on the RAIN and rAiN", "rain")
        assertEquals(3, result.spanStyles.size)
        assertEquals(0, result.spanStyles[0].start)
        assertEquals(4, result.spanStyles[0].end)
        assertEquals(12, result.spanStyles[1].start)
        assertEquals(16, result.spanStyles[1].end)
        assertEquals(21, result.spanStyles[2].start)
        assertEquals(25, result.spanStyles[2].end)
    }

    @Test
    fun matchStyledUsesNoBackground() {
        val result = matchStyled("rain", "rain")
        assertEquals(1, result.spanStyles.size)
        val span = result.spanStyles[0].item
        assertEquals(Color.Unspecified, span.background)
        assertEquals(FontWeight.Bold, span.fontWeight)
        assertEquals(Color(0xFF14110D), span.color)
        assertEquals(TextDecoration.Underline, span.textDecoration)
        assertTrue(result.spanStyles.all { it.item.background == Color.Unspecified })
    }
}
