package app.enk.text

import org.junit.Assert.assertEquals
import org.junit.Test

class StruckPageTest {

    @Test
    fun struckCountsButIsNotSearchable() {
        val p = StruckPage("abcde", listOf(1..2))
        assertEquals(5, p.length)
        assertEquals("ade", p.searchable())
        assertEquals("ade", p.exported())
        assertEquals("abcde", p.exported(true))
    }

    @Test
    fun emptyPage() {
        val p = StruckPage("")
        assertEquals(0, p.length)
        assertEquals("", p.searchable())
        assertEquals("", p.exported())
        assertEquals("", p.exported(true))
        assertEquals(p, p.withStruck(0))
    }

    @Test
    fun withStruckOutOfBoundsReturnsSame() {
        val p = StruckPage("abc")
        assertEquals(p, p.withStruck(-1))
        assertEquals(p, p.withStruck(3))
        assertEquals(p, p.withStruck(100))
    }

    @Test
    fun withStruckMergesContiguous() {
        val p = StruckPage("hello")
        val p1 = p.withStruck(1)
        assertEquals(listOf(1..1), p1.struck)
        assertEquals("hllo", p1.searchable())

        val p2 = p1.withStruck(2)
        assertEquals(listOf(1..2), p2.struck)
        assertEquals("hlo", p2.searchable())

        val p3 = p2.withStruck(0)
        assertEquals(listOf(0..2), p3.struck)
        assertEquals("lo", p3.searchable())
    }

    @Test
    fun withStruckMergesBridge() {
        val p = StruckPage("abcdef", listOf(1..2, 4..5))
        val p1 = p.withStruck(3)
        assertEquals(listOf(1..5), p1.struck)
        assertEquals("a", p1.searchable())
    }

    @Test
    fun withStruckAlreadyStruck() {
        val p = StruckPage("abc", listOf(1..1))
        val p1 = p.withStruck(1)
        assertEquals(listOf(1..1), p1.struck)
    }

    @Test
    fun allCharactersStruck() {
        val p = StruckPage("abc", listOf(0..2))
        assertEquals(3, p.length)
        assertEquals("", p.searchable())
        assertEquals("", p.exported(false))
        assertEquals("abc", p.exported(true))
    }

    @Test
    fun withStruckReverseOrderMerges() {
        var p = StruckPage("hello")
        for (i in 4 downTo 0) {
            p = p.withStruck(i)
        }
        assertEquals(listOf(0..4), p.struck)
        assertEquals("", p.searchable())
    }

    @Test
    fun firstAndLastCharacterStruck() {
        val p = StruckPage("hello").withStruck(0).withStruck(4)
        assertEquals(listOf(0..0, 4..4), p.struck)
        assertEquals("ell", p.searchable())
        assertEquals("hello", p.exported(true))
    }

    @Test
    fun disjointMultipleRanges() {
        val p = StruckPage("abcdefghij", listOf(1..2, 5..6, 8..8))
        assertEquals("adehj", p.searchable())
        assertEquals("adehj", p.exported(false))
        assertEquals("abcdefghij", p.exported(true))
    }

    @Test
    fun overlappingRangesInConstructor() {
        val p = StruckPage("abcdef", listOf(1..3, 2..4))
        assertEquals("af", p.searchable())
    }
}
