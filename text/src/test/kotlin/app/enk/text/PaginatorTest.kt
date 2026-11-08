package app.enk.text

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class PaginatorTest {

    @Test
    fun shortTextOnePage() {
        assertEquals(listOf("hello"), Paginator.split("hello"))
    }

    @Test
    fun breaksAtWord() {
        val t = "a ".repeat(1600)
        val p = Paginator.split(t)
        assertEquals(t, p.joinToString(""))
        assertTrue(p.all { it.length <= 3000 })
        assertTrue(p[0].endsWith(" "))
    }

    @Test
    fun noSpacesBreaksAt3000() {
        assertEquals(listOf(3000, 500), Paginator.split("x".repeat(3500)).map { it.length })
    }

    @Test
    fun hugePasteIsFast() {
        val t = "word ".repeat(40_000) // 200,000 chars
        val start = System.nanoTime()
        Paginator.split(t)
        val elapsedMs = (System.nanoTime() - start) / 1e6
        assertTrue("Expected split under 500ms, took ${elapsedMs}ms", elapsedMs < 500)
    }

    @Test
    fun surrogateNotSplit() {
        val t = "x".repeat(2999) + "😀" + "tail"
        val p = Paginator.split(t)
        assertTrue(p.none { it.isNotEmpty() && Character.isLowSurrogate(it.first()) })
        assertEquals(t, p.joinToString(""))
    }

    @Test
    fun overflowNullBelowLimit() {
        assertNull(Paginator.overflowBreak("x".repeat(2999)))
    }

    @Test
    fun overflowAtWord() {
        // length 3000, last space at 2994, so the new page begins at 2995
        assertEquals(2995, Paginator.overflowBreak("a".repeat(2994) + " wxyzq"))
    }

    @Test
    fun emptyTextReturnsEmptyList() {
        assertEquals(emptyList<String>(), Paginator.split(""))
    }

    @Test
    fun whitespaceOnlyText() {
        val t = " ".repeat(3500)
        val p = Paginator.split(t)
        assertEquals(t, p.joinToString(""))
        assertTrue(p.all { it.length <= 3000 })
    }

    @Test
    fun exactPageSizeReturnsSinglePage() {
        val t = "x".repeat(3000)
        assertEquals(listOf(t), Paginator.split(t))
    }

    @Test
    fun pageSizePlusOneReturnsTwoPages() {
        val t = "x".repeat(3001)
        val p = Paginator.split(t)
        assertEquals(2, p.size)
        assertEquals(listOf(3000, 1), p.map { it.length })
        assertEquals(t, p.joinToString(""))
    }

    @Test
    fun pageSizeOneAndTwoWithSurrogates() {
        // pageSize = 1 with surrogate pair cannot split the pair
        val p1 = Paginator.split("😀", 1)
        assertEquals(listOf("😀"), p1)

        // pageSize = 2 with surrogate pair
        val p2 = Paginator.split("a😀b", 2)
        assertEquals(listOf("a", "😀", "b"), p2)
        assertEquals("a😀b", p2.joinToString(""))
    }

    @Test
    fun overflowWithNoWhitespaceInLast200() {
        assertEquals(3000, Paginator.overflowBreak("x".repeat(3000)))
    }

    @Test
    fun overflowWithSurrogateAtBoundary() {
        val t = "x".repeat(2999) + "😀" + "tail"
        assertEquals(2999, Paginator.overflowBreak(t))
    }
}
