package app.enk.perf

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

class FrameMetricsMathTest {

    @Test
    fun emptyListReturnsZeroWithoutNaN() {
        val p95 = FrameMetricsMath.percentile(emptyList(), 95.0)
        assertFalse(p95.isNaN())
        assertEquals(0.0, p95, 0.001)

        val jank = FrameMetricsMath.jankFraction(emptyList(), 16.666)
        assertFalse(jank.isNaN())
        assertEquals(0.0, jank, 0.001)
    }

    @Test
    fun singleFrameReturnsItself() {
        val single = listOf(14.2)
        assertEquals(14.2, FrameMetricsMath.percentile(single, 50.0), 0.001)
        assertEquals(14.2, FrameMetricsMath.percentile(single, 95.0), 0.001)

        assertEquals(0.0, FrameMetricsMath.jankFraction(single, 16.666), 0.001)
        assertEquals(1.0, FrameMetricsMath.jankFraction(listOf(22.0), 16.666), 0.001)
    }

    @Test
    fun handlesTiesCorrectly() {
        val ties = listOf(10.0, 10.0, 10.0, 10.0, 10.0)
        assertEquals(10.0, FrameMetricsMath.percentile(ties, 95.0), 0.001)
        assertEquals(10.0, FrameMetricsMath.percentile(ties, 50.0), 0.001)
        assertEquals(0.0, FrameMetricsMath.jankFraction(ties, 16.666), 0.001)
    }

    @Test
    fun filtersNaNAndNegativeValues() {
        val dirty = listOf(Double.NaN, -5.0, 12.0, 16.0, Double.NaN)
        val p50 = FrameMetricsMath.percentile(dirty, 50.0)
        assertFalse(p50.isNaN())
        assertEquals(14.0, p50, 0.001)

        val jank = FrameMetricsMath.jankFraction(dirty, 15.0)
        assertEquals(0.5, jank, 0.001) // 1 out of 2 valid (16.0 > 15.0)
    }

    @Test
    fun handlesHugeValuesAndComputesPercentileAccurately() {
        val huge = listOf(1_000_000.0, 2_000_000.0, 3_000_000.0, 4_000_000.0)
        val p95 = FrameMetricsMath.percentile(huge, 95.0)
        assertFalse(p95.isNaN())
        assertEquals(3_850_000.0, p95, 0.001)

        val jank = FrameMetricsMath.jankFraction(huge, 16.666)
        assertEquals(1.0, jank, 0.001)
    }

    @Test
    fun orderDoesNotMatterAndPercentilesAreClamped() {
        val shuffled = listOf(30.0, 10.0, 20.0, 40.0)
        assertEquals(10.0, FrameMetricsMath.percentile(shuffled, 0.0), 0.001)
        assertEquals(40.0, FrameMetricsMath.percentile(shuffled, 100.0), 0.001)
        assertEquals(10.0, FrameMetricsMath.percentile(shuffled, -50.0), 0.001)
        assertEquals(40.0, FrameMetricsMath.percentile(shuffled, 250.0), 0.001)
        assertEquals(25.0, FrameMetricsMath.percentile(shuffled, 50.0), 0.001)
    }

    @Test
    fun aFrameExactlyOnTheThresholdIsNotJanky() {
        assertEquals(0.0, FrameMetricsMath.jankFraction(listOf(16.0, 16.0), 16.0), 0.0)
        assertEquals(0.5, FrameMetricsMath.jankFraction(listOf(16.0, 16.01), 16.0), 0.0)
    }

    @Test
    fun computesExpectedPercentileAndJankOnNormalDataset() {
        // 100 frames: 95 frames at 10.0 ms, 5 frames at 25.0 ms
        val frames = (1..95).map { 10.0 } + (1..5).map { 25.0 }
        val p95 = FrameMetricsMath.percentile(frames, 95.0)
        // At rank 94.05 (index 94 is 10.0, index 95 is 25.0): 10.0 + 0.05 * 15.0 = 10.75
        assertEquals(10.75, p95, 0.01)

        val jank = FrameMetricsMath.jankFraction(frames, 16.666)
        assertEquals(0.05, jank, 0.001) // exactly 5%
    }
}
