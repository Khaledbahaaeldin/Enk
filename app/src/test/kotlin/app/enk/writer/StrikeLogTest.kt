package app.enk.writer

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class StrikeLogTest {

    @Test
    fun livenessBoundaryAt130ms() {
        val log = StrikeLog()
        log.recordKey(index = 5, weight = 1f, nowMs = 100f)

        assertNotNull(log.keyAt(5, nowMs = 100f))
        assertNotNull(log.keyAt(5, nowMs = 229.9f))
        assertNull(log.keyAt(5, nowMs = 230f))
    }

    @Test
    fun fastIntervalAt95msBoundary() {
        val log = StrikeLog()
        val first = log.recordKey(index = 0, weight = 1f, nowMs = 0f)
        assertFalse(first.fast)

        // 94 ms later: fast (< 95 ms)
        val second = log.recordKey(index = 1, weight = 1f, nowMs = 94f)
        assertTrue(second.fast)

        // 95 ms after second: normal (>= 95 ms)
        val third = log.recordKey(index = 2, weight = 1f, nowMs = 94f + 95f)
        assertFalse(third.fast)
    }

    @Test
    fun maxAnimatingCapacityDropsOldest() {
        val log = StrikeLog()
        // Record 8 live keys
        for (i in 0 until 8) {
            log.recordKey(index = i, weight = 1f, nowMs = 10f * i)
        }
        for (i in 0 until 8) {
            assertNotNull(log.keyAt(i, nowMs = 80f))
        }

        // Recording a 9th live entry drops the oldest (index 0)
        log.recordKey(index = 8, weight = 1f, nowMs = 85f)
        assertNull("Index 0 should have been dropped", log.keyAt(0, nowMs = 85f))
        assertNotNull(log.keyAt(8, nowMs = 85f))
    }

    @Test
    fun missingIndexReturnsNull() {
        val log = StrikeLog()
        log.recordKey(index = 1, weight = 1f, nowMs = 10f)
        assertNull(log.keyAt(99, nowMs = 15f))
    }

    @Test
    fun eraseLivenessBoundaryAt90ms() {
        val log = StrikeLog()
        log.recordErase(text = "x", line = 1, col = 2, nowMs = 50f)

        assertEquals(1, log.erases(nowMs = 50f).size)
        assertEquals(1, log.erases(nowMs = 139.9f).size)
        assertEquals(0, log.erases(nowMs = 140.0f).size)
    }

    @Test
    fun clearWipesAllEntries() {
        val log = StrikeLog()
        log.recordKey(index = 1, weight = 1f, nowMs = 10f)
        log.recordErase(text = "a", line = 0, col = 0, nowMs = 10f)
        assertTrue(log.hasActive(nowMs = 10f))

        log.clear()
        assertFalse(log.hasActive(nowMs = 10f))
        assertNull(log.keyAt(1, nowMs = 10f))
        assertTrue(log.erases(nowMs = 10f).isEmpty())
    }

    @Test
    fun nanAndNegativeTimesAreIgnored() {
        val log = StrikeLog()
        log.recordKey(index = 1, weight = 1f, nowMs = Float.NaN)
        log.recordKey(index = 2, weight = 1f, nowMs = -10f)
        assertFalse(log.hasActive(nowMs = 0f))
        assertNull(log.keyAt(1, nowMs = 0f))
        assertNull(log.keyAt(2, nowMs = 0f))

        log.recordErase("z", 0, 0, Float.NaN)
        log.recordErase("z", 0, 0, -5f)
        assertTrue(log.erases(0f).isEmpty())
    }

    @Test
    fun latestKeyWithKeyThenStruck() {
        val log = StrikeLog()
        log.recordKey(index = 3, weight = 1.4f, nowMs = 10f)
        val lk1 = log.latestKey(nowMs = 15f)
        assertNotNull(lk1)
        assertEquals(3, lk1!!.index)
        assertEquals(1.4f, lk1.weight, 0.001f)

        log.recordStruck(index = 4, weight = 1.0f, nowMs = 20f)
        val lk2 = log.latestKey(nowMs = 25f)
        assertNotNull(lk2)
        assertEquals(4, lk2!!.index)
        assertEquals(1.0f, lk2.weight, 0.001f)
    }

    @Test
    fun thousandRecordsNeverExceedsCapacity() {
        val log = StrikeLog()
        for (i in 0 until 1000) {
            log.recordKey(index = i, weight = 1f, nowMs = 1000f + i * 0.1f)
        }
        var liveCount = 0
        for (i in 0 until 1000) {
            if (log.keyAt(i, nowMs = 1100f) != null) {
                liveCount++
            }
        }
        assertTrue("Live count $liveCount must be <= 8", liveCount <= 8)
    }
}
