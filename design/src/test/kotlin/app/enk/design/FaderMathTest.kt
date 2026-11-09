package app.enk.design

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.roundToInt

class FaderMathTest {

    @Test
    fun detentSnapsTo20Steps() {
        assertEquals(0.50f, FaderMath.detent(0.51f), 0.0001f)
        assertEquals(0.55f, FaderMath.detent(0.53f), 0.0001f)
        assertEquals(0.00f, FaderMath.detent(0.01f), 0.0001f)
        assertEquals(0.05f, FaderMath.detent(0.04f), 0.0001f)
        assertEquals(1.00f, FaderMath.detent(0.99f), 0.0001f)
    }

    @Test
    fun valueForIntensityRange() {
        // fraction 0.5f on range 20..100 gives 60
        assertEquals(60, FaderMath.valueFor(0.5f, min = 20, max = 100))
        assertEquals(20, FaderMath.valueFor(0f, min = 20, max = 100))
        assertEquals(100, FaderMath.valueFor(1f, min = 20, max = 100))
    }

    @Test
    fun displayPercentDefaults() {
        // intensity 80 on 20..100 gives 75
        assertEquals(75, FaderMath.displayPercent(value = 80, min = 20, max = 100))
        // beam 55 on 30..100 gives 36
        assertEquals(36, FaderMath.displayPercent(value = 55, min = 30, max = 100))
        // warmth 25 on 0..100 gives 25
        assertEquals(25, FaderMath.displayPercent(value = 25, min = 0, max = 100))
    }

    @Test
    fun knobLeftAtEnds() {
        // fraction 1f with trackWidth 200f gives 176f (200 - 24)
        assertEquals(176f, FaderMath.knobLeftDp(fraction = 1f, trackWidthDp = 200f), 0.001f)
        // fraction 0f with trackWidth 200f gives 0f
        assertEquals(0f, FaderMath.knobLeftDp(fraction = 0f, trackWidthDp = 200f), 0.001f)
        // fraction 0.5f gives 88f
        assertEquals(88f, FaderMath.knobLeftDp(fraction = 0.5f, trackWidthDp = 200f), 0.001f)
    }

    @Test
    fun defaultsSurviveRoundTrip() {
        // warmth 25 (0..100)
        val warmthVal = 25
        val warmthFrac = (warmthVal - 0) / (100f - 0f)
        val warmthDetent = FaderMath.detent(warmthFrac)
        val warmthRoundTrip = FaderMath.valueFor(warmthDetent, min = 0, max = 100)
        assertEquals(warmthVal, warmthRoundTrip)

        // intensity 80 (20..100)
        val intensityVal = 80
        val intensityFrac = (intensityVal - 20) / (100f - 20f)
        val intensityDetent = FaderMath.detent(intensityFrac)
        val intensityRoundTrip = FaderMath.valueFor(intensityDetent, min = 20, max = 100)
        assertEquals(intensityVal, intensityRoundTrip)

        // beam 55 (30..100)
        val beamVal = 55
        val beamFrac = (beamVal - 30) / (100f - 30f)
        val beamDetent = FaderMath.detent(beamFrac)
        val beamRoundTrip = FaderMath.valueFor(beamDetent, min = 30, max = 100)
        assertEquals(beamVal, beamRoundTrip)
    }

    @Test
    fun everyDetentMapsToDistinctInteger() {
        val detents = (0..20).map { it / 20f }
        assertEquals(21, detents.size)

        // Range 0..100
        val warmthValues: List<Int> = detents.map { d: Float -> FaderMath.valueFor(d, 0, 100) }
        assertEquals(21, warmthValues.distinct().size)

        // Range 20..100
        val intensityValues: List<Int> = detents.map { d: Float -> FaderMath.valueFor(d, 20, 100) }
        assertEquals(21, intensityValues.distinct().size)

        // Range 30..100
        val beamValues: List<Int> = detents.map { d: Float -> FaderMath.valueFor(d, 30, 100) }
        assertEquals(21, beamValues.distinct().size)
    }

    @Test
    fun fractionClampingBelowAboveNaN() {
        // fraction < 0 clamps to 0
        assertEquals(0f, FaderMath.detent(-0.25f), 0.001f)
        assertEquals(0f, FaderMath.detent(-10f), 0.001f)

        // fraction > 1 clamps to 1
        assertEquals(1f, FaderMath.detent(1.25f), 0.001f)
        assertEquals(1f, FaderMath.detent(10f), 0.001f)

        // NaN clamps to 0
        assertEquals(0f, FaderMath.detent(Float.NaN), 0.001f)
    }

    @Test
    fun onDetentFiresOnlyWhenValueChanges() {
        val firedPositions = mutableListOf<Int>()
        var lastDetentIndex = (FaderMath.detent(0.50f) * 20f).roundToInt()

        fun simulateMove(fraction: Float) {
            val d = FaderMath.detent(fraction)
            val index = (d * 20f).roundToInt()
            if (index != lastDetentIndex) {
                lastDetentIndex = index
                firedPositions.add(index * 5)
            }
        }

        // Slight movements inside detent 10
        simulateMove(0.51f)
        simulateMove(0.52f)
        simulateMove(0.49f)
        assertTrue("Movements staying within the same detent must fire zero times", firedPositions.isEmpty())

        // Move to detent 11 (0.53f snaps to 0.55f -> index 11 -> position 55)
        simulateMove(0.53f)
        assertEquals(listOf(55), firedPositions)

        // Move to detent 12 (0.60f -> index 12 -> position 60)
        simulateMove(0.60f)
        assertEquals(listOf(55, 60), firedPositions)

        // Move back to detent 11 (0.55f -> index 11 -> position 55)
        simulateMove(0.55f)
        assertEquals(listOf(55, 60, 55), firedPositions)
    }
}
