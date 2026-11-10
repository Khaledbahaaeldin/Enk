package app.enk.design.shell

import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * The arithmetic behind the shell controls. What they look like and how they behave as composed nodes
 * (sizes, hit areas, springs, callbacks) is measured in `ShellControlsCompositionTest` in `:app`.
 */
class ShellControlsTest {

    @Test
    fun segmentedDragSelectsNearest() {
        val itemCenters = listOf(40f, 120f, 200f)
        assertEquals(0, ShellSegmentedMath.nearestItem(35f, itemCenters))
        assertEquals(1, ShellSegmentedMath.nearestItem(110f, itemCenters))
        assertEquals(1, ShellSegmentedMath.nearestItem(140f, itemCenters))
        assertEquals(2, ShellSegmentedMath.nearestItem(190f, itemCenters))
        assertEquals("beyond the ends", 0, ShellSegmentedMath.nearestItem(-500f, itemCenters))
        assertEquals(2, ShellSegmentedMath.nearestItem(5_000f, itemCenters))
        assertEquals("no items", 0, ShellSegmentedMath.nearestItem(10f, emptyList()))
    }

    @Test
    fun faderFractionClampsAndTreatsNaNAsTheStart() {
        assertEquals(0f, ShellFaderMath.fraction(-3f), 0f)
        assertEquals(1f, ShellFaderMath.fraction(7f), 0f)
        assertEquals(0.537f, ShellFaderMath.fraction(0.537f), 0f)
        assertEquals(0f, ShellFaderMath.fraction(Float.NaN), 0f)
        assertEquals(1f, ShellFaderMath.fraction(Float.POSITIVE_INFINITY), 0f)
    }
}
