package app.enk.writer

import org.junit.Assert.assertEquals
import org.junit.Test

class TopBarFormatterTest {

    @Test
    fun page1Empty() {
        assertEquals("p.1 · 0 / 3000", TopBarFormatter.formatMeta(pageIndex = 0, chars = 0))
    }

    @Test
    fun page12At3000() {
        assertEquals("p.12 · 3000 / 3000", TopBarFormatter.formatMeta(pageIndex = 11, chars = 3000))
    }

    @Test
    fun emojiPage() {
        val emojiText = "🖋️ typewriter ✨"
        assertEquals("p.3 · ${emojiText.length} / 3000", TopBarFormatter.formatMeta(pageIndex = 2, chars = emojiText.length))
    }
}
