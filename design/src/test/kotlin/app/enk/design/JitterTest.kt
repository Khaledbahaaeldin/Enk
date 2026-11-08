package app.enk.design

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class JitterTest {

    @Test
    fun jitterStable() {
        assertEquals(Jitter.forIndex(7), Jitter.forIndex(7))
    }

    @Test
    fun jitterDeterminismLargeIndices() {
        listOf(0, 1, 10_000, 50_000, 100_000, 200_000).forEach { idx ->
            assertEquals("Index $idx must be deterministic", Jitter.forIndex(idx), Jitter.forIndex(idx))
        }
    }

    @Test
    fun jitterRanges() {
        for (i in 0 until 5000) {
            val j = Jitter.forIndex(i)
            assertTrue("dyDp $i out of range: ${j.dyDp}", j.dyDp in -0.55f..0.55f)
            assertTrue("rotDeg $i out of range: ${j.rotDeg}", j.rotDeg in -1.1f..1.1f)
            assertTrue("alpha $i out of range: ${j.alpha}", j.alpha in 0.84f..1.0f)
        }
    }

    @Test
    fun jitterRangesLargeIndices() {
        val checkIndices = listOf(0, 10_000, 200_000) + (199_900..200_000)
        for (i in checkIndices) {
            val j = Jitter.forIndex(i)
            assertTrue("dyDp $i out of range: ${j.dyDp}", j.dyDp in -0.55f..0.55f)
            assertTrue("rotDeg $i out of range: ${j.rotDeg}", j.rotDeg in -1.1f..1.1f)
            assertTrue("alpha $i out of range: ${j.alpha}", j.alpha in 0.84f..1.0f)
        }
    }

    @Test
    fun hashRangeAndDeterminism() {
        for (n in listOf(0, 1, 100, 10_000, 200_000)) {
            val h = Jitter.hash(n)
            assertTrue("hash($n) = $h out of [0, 1)", h >= 0.0 && h < 1.0)
            assertEquals("hash($n) must be deterministic", h, Jitter.hash(n), 0.0)
        }
    }
}
