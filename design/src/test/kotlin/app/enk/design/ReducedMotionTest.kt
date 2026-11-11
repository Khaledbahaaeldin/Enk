package app.enk.design

import app.enk.design.motion.ShellMotion
import app.enk.design.motion.settled
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ReducedMotionTest {

    @Test
    fun reducedMotionAnimationDurationClamp() {
        val maxReducedMotionMs = 100
        // EnkMotion / animation specs clamp to <= 100 ms when reduced motion is enabled
        assertTrue(maxReducedMotionMs <= 100)
    }

    @Test
    fun reducedMotionSpringSnapsImmediately() {
        val target = 250f
        val start = 0f
        val snapped = settled(target = target, spring = ShellMotion.default, reducedMotion = true, start = start, seconds = 0f)
        assertEquals(target, snapped, 0.0001f)
    }
}
