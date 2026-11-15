package app.enk.writer

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class TypeOnTest {

    @Test
    fun staggerMsComputations() {
        assertEquals(150f, TypeOn.staggerMs(0), 0.001f)
        assertEquals(150f, TypeOn.staggerMs(-5), 0.001f)
        assertEquals(150f, TypeOn.staggerMs(1), 0.001f)
        assertEquals(150f, TypeOn.staggerMs(6), 0.001f)
        assertEquals(75f, TypeOn.staggerMs(12), 0.001f)
        assertEquals(900f / 14f, TypeOn.staggerMs(14), 0.001f)
        assertEquals(9f, TypeOn.staggerMs(100), 0.001f)
    }

    @Test
    fun lineStartMsComputations() {
        val stagger = 100f
        assertEquals(120f, TypeOn.lineStartMs(0, stagger), 0.001f)
        assertEquals(220f, TypeOn.lineStartMs(1, stagger), 0.001f)
        assertEquals(320f, TypeOn.lineStartMs(2, stagger), 0.001f)
    }

    @Test
    fun glyphRevealMsComputations() {
        val stagger = 100f
        val cols = 28
        val lineStart = TypeOn.lineStartMs(0, stagger) // 120f
        assertEquals(120f, TypeOn.glyphRevealMs(0, 0, cols, stagger), 0.001f)
        assertEquals(120f + (13f / 28f) * 112f, TypeOn.glyphRevealMs(0, 13, cols, stagger), 0.001f)
        assertEquals(120f + (27f / 28f) * 112f, TypeOn.glyphRevealMs(0, 27, cols, stagger), 0.001f)

        // Edge cases: cols = 0, negative col
        val zeroCols = TypeOn.glyphRevealMs(0, 5, 0, stagger)
        assertTrue("zero cols must be finite", zeroCols.isFinite())
        assertEquals(lineStart, zeroCols, 0.001f)

        val negCol = TypeOn.glyphRevealMs(0, -5, cols, stagger)
        assertTrue("negative col must be finite", negCol.isFinite())
        assertEquals(lineStart, negCol, 0.001f)
    }

    @Test
    fun totalMsComputations() {
        val stagger = 100f
        assertEquals(0f, TypeOn.totalMs(0, stagger), 0.001f)
        // 1 line: lineStart(0) = 120 + 112 + 85 + 90 = 407
        assertEquals(120f + 112f + 85f + 90f, TypeOn.totalMs(1, stagger), 0.001f)
        // 3 lines: lineStart(2) = 120 + 200 = 320 + 112 + 85 + 90 = 607
        assertEquals(320f + 112f + 85f + 90f, TypeOn.totalMs(3, stagger), 0.001f)
    }

    @Test
    fun carriageColumnEvaluations() {
        val cols = 28
        val stagger = 150f
        val lineLengths = listOf(20)

        // Empty list -> null
        assertNull(TypeOn.carriageColumn(150f, emptyList(), cols, stagger))

        // Before LEAD_MS
        assertNull(TypeOn.carriageColumn(0f, lineLengths, cols, stagger))
        assertNull(TypeOn.carriageColumn(119.9f, lineLengths, cols, stagger))

        // At LEAD_MS = 120f -> 0
        val colAt120 = TypeOn.carriageColumn(120f, lineLengths, cols, stagger)
        assertNotNull(colAt120)
        assertEquals(0f, colAt120!!, 0.001f)

        // Mid-sweep at t = 176f (120 + 56 = half of 112ms) -> 28 * 0.5 = 14, clamped to 20
        val colAt176 = TypeOn.carriageColumn(176f, lineLengths, cols, stagger)
        assertNotNull(colAt176)
        assertEquals(14f, colAt176!!, 0.001f)

        // End of sweep at t = 232f (120 + 112) -> clamped to lineLengths[0] = 20
        val colAt232 = TypeOn.carriageColumn(232f, lineLengths, cols, stagger)
        assertNotNull(colAt232)
        assertEquals(20f, colAt232!!, 0.001f)

        // Returning at t = 232.1f -> just starting easeOut return from 20 toward 0
        val colAt232_1 = TypeOn.carriageColumn(232.1f, lineLengths, cols, stagger)
        assertNotNull(colAt232_1)
        assertTrue(colAt232_1!! <= 20f && colAt232_1!! > 19f)

        // Beyond return (t = 120 + 112 + 85 = 317)
        val colAt300 = TypeOn.carriageColumn(300f, lineLengths, cols, stagger)
        assertNotNull(colAt300)
        assertTrue(colAt300!! < 10f)

        val colAt317_1 = TypeOn.carriageColumn(317.1f, lineLengths, cols, stagger)
        assertNotNull(colAt317_1)
        assertEquals(0f, colAt317_1!!, 0.001f)

        // After totalMs -> null
        val tot = TypeOn.totalMs(1, stagger)
        assertNull(TypeOn.carriageColumn(tot + 1f, lineLengths, cols, stagger))

        // Multiple lines with overlap: stagger = 50ms, lineStart(0) = 120, lineStart(1) = 170
        val multiLineLengths = listOf(20, 25, 22)
        val multiStagger = 50f
        // At t = 175f, line 1 has started at 170f, so line 1 wins over line 0
        val multiCol = TypeOn.carriageColumn(175f, multiLineLengths, cols, multiStagger)
        assertNotNull(multiCol)
        // t - start = 5ms / 112 * 28 = 1.25 col
        assertEquals(5f / 112f * 28f, multiCol!!, 0.01f)

        // Line longer than cols (35 > 28): clamped to cols (28)
        val longLine = listOf(35)
        val endSweep = TypeOn.carriageColumn(232f, longLine, cols, stagger)
        assertNotNull(endSweep)
        assertEquals(28f, endSweep!!, 0.001f)
    }

    @Test
    fun skipEvaluations() {
        assertTrue("No visible lines must skip", TypeOn.skip(visibleChars = 10, visibleLines = 0))
        assertFalse("1 char in 1 line must not skip", TypeOn.skip(visibleChars = 1, visibleLines = 1))
        assertFalse("2000 chars in 10 lines must not skip", TypeOn.skip(visibleChars = 2000, visibleLines = 10))
        assertTrue("2001 chars must skip", TypeOn.skip(visibleChars = 2001, visibleLines = 10))
    }
}
