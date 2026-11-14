package app.enk.keyboard

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class KeyboardTimingTest {

    @Test
    fun backspaceRepeatDelayMsValues() {
        assertEquals(90L, KeyboardTiming.backspaceRepeatDelayMs(1))
        assertEquals(90L, KeyboardTiming.backspaceRepeatDelayMs(10))
        assertEquals(45L, KeyboardTiming.backspaceRepeatDelayMs(11))
        assertEquals(45L, KeyboardTiming.backspaceRepeatDelayMs(1000))

        // 0 and negative behave like 1 (90 ms)
        assertEquals(90L, KeyboardTiming.backspaceRepeatDelayMs(0))
        assertEquals(90L, KeyboardTiming.backspaceRepeatDelayMs(-1))
        assertEquals(90L, KeyboardTiming.backspaceRepeatDelayMs(-42))
    }

    @Test
    fun cursorColumnsTruncation() {
        assertEquals(0, cursorColumns(0f))
        assertEquals(0, cursorColumns(13.9f))
        assertEquals(1, cursorColumns(14f))
        assertEquals(-1, cursorColumns(-14f))
        assertEquals(-1, cursorColumns(-27.9f))
        assertEquals(0, cursorColumns(Float.NaN))
        assertEquals(0, cursorColumns(Float.POSITIVE_INFINITY))
        assertEquals(0, cursorColumns(Float.NEGATIVE_INFINITY))
    }

    @Test
    fun popupIndexAtPositions() {
        val popupLeft = 100f
        val count = 5
        // slot width is 44 dp
        // slot 0: [100, 144)
        // slot 2: [188, 232)
        // slot 4: [276, 320)

        // Left edge of slot 0
        assertEquals(0, popupIndexAt(100f, popupLeft, count))
        // Middle of slot 2
        assertEquals(2, popupIndexAt(210f, popupLeft, count))
        // Inside right edge of slot 4
        assertEquals(4, popupIndexAt(319.9f, popupLeft, count))

        // Just outside on the left
        assertNull(popupIndexAt(99.9f, popupLeft, count))
        // Just outside on the right
        assertNull(popupIndexAt(320f, popupLeft, count))
        assertNull(popupIndexAt(350f, popupLeft, count))

        // Count 0
        assertNull(popupIndexAt(100f, popupLeft, 0))

        // Count 1: slot 0 is [100, 144)
        assertEquals(0, popupIndexAt(100f, popupLeft, 1))
        assertEquals(0, popupIndexAt(143.9f, popupLeft, 1))
        assertNull(popupIndexAt(144f, popupLeft, 1))
        assertNull(popupIndexAt(99f, popupLeft, 1))
    }

    @Test
    fun keyboardMetricsForWindow() {
        val full = KeyboardMetrics.forWindow(1000f)
        assertEquals(46f, full.keyHeightDp, 0.001f)
        assertEquals(22f, full.letterSp, 0.001f)

        val fullAt640 = KeyboardMetrics.forWindow(640f)
        assertEquals(46f, fullAt640.keyHeightDp, 0.001f)

        val medAt639 = KeyboardMetrics.forWindow(639.9f)
        assertEquals(40f, medAt639.keyHeightDp, 0.001f)
        assertEquals(20f, medAt639.letterSp, 0.001f)

        val medAt480 = KeyboardMetrics.forWindow(480f)
        assertEquals(40f, medAt480.keyHeightDp, 0.001f)

        val compAt479 = KeyboardMetrics.forWindow(479.9f)
        assertEquals(32f, compAt479.keyHeightDp, 0.001f)
        assertEquals(17f, compAt479.letterSp, 0.001f)

        val compAt100 = KeyboardMetrics.forWindow(100f)
        assertEquals(32f, compAt100.keyHeightDp, 0.001f)

        // 0, -5, NaN fallback to first row (FULL)
        assertEquals(46f, KeyboardMetrics.forWindow(0f).keyHeightDp, 0.001f)
        assertEquals(46f, KeyboardMetrics.forWindow(-5f).keyHeightDp, 0.001f)
        assertEquals(46f, KeyboardMetrics.forWindow(Float.NaN).keyHeightDp, 0.001f)
    }

    @Test
    fun totalHeightDpCalculations() {
        val full = KeyboardMetrics.forWindow(820f)
        // 4 rows: 10 + 4 * 46 + 3 * 10 + 8 = 232.0f
        assertEquals(232.0f, full.totalHeightDp(4), 0.001f)

        // 1 row: 10 + 1 * 46 + 0 * 10 + 8 = 64.0f
        assertEquals(64.0f, full.totalHeightDp(1), 0.001f)

        // 0 rows: 10 + 8 = 18.0f
        assertEquals(18.0f, full.totalHeightDp(0), 0.001f)

        // -1 rows: 10 + 8 = 18.0f
        assertEquals(18.0f, full.totalHeightDp(-1), 0.001f)
    }
}
