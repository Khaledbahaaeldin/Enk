package app.enk.writer

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class TypeSlugTest {

    @Test
    fun slugOffsetKeyframes() {
        assertEquals(9f, slugOffsetDp(0f), 0.001f)
        assertEquals(-5f, slugOffsetDp(0.45f), 0.001f)
        assertEquals(9f, slugOffsetDp(1f), 0.001f)
    }

    @Test
    fun slugOffsetBounds1000Points() {
        for (i in 0..1000) {
            val t = i / 1000f
            val offset = slugOffsetDp(t)
            assertTrue("Slug offset at t=$t must be >= -5f, but was $offset", offset >= -5f - 0.001f)
            assertTrue("Slug offset at t=$t must be <= +9f, but was $offset", offset <= 9f + 0.001f)
        }
    }

    @Test
    fun slugRestartLogicDoesNotSnapBack() {
        val inFlightOffset = 0f // mid-strike
        val restartOffset = slugOffsetDp(t = 0f, startOffset = inFlightOffset)
        assertEquals("Restarting strike must begin at in-flight offset 0f, not +9f", inFlightOffset, restartOffset, 0.001f)

        val peakOffset = slugOffsetDp(t = 0.45f, startOffset = inFlightOffset)
        assertEquals("Strike peak must reach -5f", -5f, peakOffset, 0.001f)
    }

    @Test
    fun railGeometry() {
        val railYDp = 508f
        val geom = RailGeometry.compute(railYDp)
        assertEquals(12f, geom.rodHeightDp, 0.001f)
        assertEquals(502f, geom.rodTopDp, 0.001f) // railY - 6
        assertEquals(24f, geom.bracketHeightDp, 0.001f)
        assertEquals(12f, geom.bracketWidthDp, 0.001f)
        assertEquals(496f, geom.bracketTopDp, 0.001f) // railY - 12
        assertEquals(3f, geom.bracketCornerRadiusDp, 0.001f)
    }

    @Test
    fun interactiveArea54x48() {
        assertEquals(54f, CarriageGeometry.interactiveWidthDp, 0.001f)
        assertEquals(48f, CarriageGeometry.interactiveHeightDp, 0.001f)
        assertEquals(54f, CarriageGeometry.bodyWidthDp, 0.001f)
        assertEquals(36f, CarriageGeometry.bodyHeightDp, 0.001f)
        assertEquals(18f, CarriageGeometry.slotWidthDp, 0.001f)
        assertEquals(11f, CarriageGeometry.slotHeightDp, 0.001f)
        assertEquals(14f, CarriageGeometry.slugWidthDp, 0.001f)
        assertEquals(12f, CarriageGeometry.slugHeightDp, 0.001f)
    }

    @Test
    fun slugCharFiltering() {
        assertEquals('A', filterSlugChar('A'))
        assertEquals('z', filterSlugChar('z'))
        assertEquals('1', filterSlugChar('1'))
        assertEquals('?', filterSlugChar('?'))

        assertNull(filterSlugChar(' '))
        assertNull(filterSlugChar('\n'))
        assertNull(filterSlugChar('\r'))
        assertNull(filterSlugChar('\t'))
        assertNull(filterSlugChar('\b'))
        assertNull(filterSlugChar(null))
        assertNull(filterSlugChar('\u0000'))
        assertNull(filterSlugChar('\u001F'))
    }

    @Test
    fun slugOffsetNegativeAndExcessClamping() {
        assertEquals("t < 0 should clamp to t=0", 9f, slugOffsetDp(-0.5f), 0.001f)
        assertEquals("t > 1 should clamp to t=1", 9f, slugOffsetDp(2.5f), 0.001f)
    }

    @Test
    fun slugOffsetPeakAlwaysMinus5RegardlessOfStartOffset() {
        val testStarts = listOf(-5f, -2f, 0f, 4.5f, 9f)
        for (start in testStarts) {
            val peak = slugOffsetDp(0.45f, startOffset = start)
            assertEquals("Peak must always reach -5f regardless of start offset $start", -5f, peak, 0.001f)
            val finish = slugOffsetDp(1.0f, startOffset = start)
            assertEquals("Finish must always return to +9f regardless of start offset $start", 9f, finish, 0.001f)
        }
    }

    @Test
    fun slugCharFilteringEdgeCases() {
        // Retain printable symbols and non-ASCII letters
        assertEquals('!', filterSlugChar('!'))
        assertEquals('#', filterSlugChar('#'))
        assertEquals('é', filterSlugChar('é'))
        assertEquals('ñ', filterSlugChar('ñ'))

        // Filter out control and whitespace variations
        assertNull(filterSlugChar('\u000C')) // Form feed
        assertNull(filterSlugChar('\u001B')) // Escape
        assertNull(filterSlugChar('\u007F')) // DEL
        assertNull(filterSlugChar('\u00A0')) // Non-breaking space
    }
}

