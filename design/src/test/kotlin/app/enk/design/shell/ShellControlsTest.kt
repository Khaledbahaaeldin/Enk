package app.enk.design.shell

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ShellControlsTest {

    @Test
    fun toggleSpringsToOn() {
        // Bead travel distance: track 52 dp, bead 26 dp, 3 dp padding on both sides.
        // Off position = 3 dp; On position = 23 dp. Travel = 20 dp.
        val travel = ShellToggleMath.onOffsetDp - ShellToggleMath.offOffsetDp
        assertEquals(20f, travel, 0.001f)
    }

    @Test
    fun toggleStretchesWhilePressed() {
        assertEquals(1.12f, ShellToggleMath.pressedScaleX, 0.001f)
    }

    @Test
    fun faderIsContinuous() {
        // Continuous float values, not quantized to 20 detents (unlike the lamp's 0.05 steps).
        val input = 0.537f
        val output = ShellFaderMath.fraction(input)
        assertEquals(0.537f, output, 0.0001f)

        val input2 = 0.12345f
        assertEquals(0.12345f, ShellFaderMath.fraction(input2), 0.0001f)
    }

    @Test
    fun segmentedDragSelectsNearest() {
        // For 3 items: bounds [0..80], [80..160], [160..240]. Centers: 40, 120, 200.
        val itemCenters = listOf(40f, 120f, 200f)
        assertEquals(0, ShellSegmentedMath.nearestItem(35f, itemCenters))
        assertEquals(1, ShellSegmentedMath.nearestItem(110f, itemCenters))
        assertEquals(1, ShellSegmentedMath.nearestItem(140f, itemCenters))
        assertEquals(2, ShellSegmentedMath.nearestItem(190f, itemCenters))
    }

    @Test
    fun controlsMeet48dpTouchHeight() {
        assertTrue(ShellToggleMath.touchHeightDp >= 48f)
        assertTrue(ShellSegmentedMath.touchHeightDp >= 48f)
        assertTrue(ShellFaderMath.touchHeightDp >= 48f)
    }
}
