package app.enk.writer

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class Spring1DTest {

    @Test
    fun thumpSpringImpulseBehavior() {
        val spring = Spring1D(StrikeTiming.THUMP_RESPONSE_S, StrikeTiming.THUMP_DAMPING)
        spring.impulse(StrikeTiming.THUMP_IMPULSE_DPS)

        var peakX = 0f
        var crossedZero = false
        var timeS = 0f
        val dt = 0.001f

        var wasPositive = false
        var reachedRestTimeS = -1f

        while (timeS < 1.0f) {
            spring.step(dt)
            timeS += dt

            if (spring.x > peakX) {
                peakX = spring.x
            }

            if (spring.x > 0.05f) {
                wasPositive = true
            }
            if (wasPositive && spring.x < 0f) {
                crossedZero = true
            }

            if (spring.atRest && reachedRestTimeS < 0f && timeS > 0.1f) {
                reachedRestTimeS = timeS
            }
        }

        // Peak is mathematically damped from undamped 55/omega_n (~0.79) to ~0.511
        assertEquals(0.511f, peakX, 0.01f)
        assertTrue("Peak $peakX must be in 0.5..1.0", peakX in 0.5f..1.0f)
        assertTrue("Peak $peakX must be <= 1.0", peakX <= 1.0f)

        // Crosses zero (rings at least once)
        assertTrue("Underdamped thump spring must cross zero", crossedZero)

        // At rest within 600 ms
        assertTrue("Spring must reach rest within 0.6s (was $reachedRestTimeS)", reachedRestTimeS in 0f..0.6f)
        assertTrue(spring.atRest)
    }

    @Test
    fun robustnessAgainstExtremeDt() {
        val spring = Spring1D(0.1f, 0.5f)
        spring.impulse(10f)

        // dt 0, negative, NaN, 10s
        spring.step(0f)
        assertFalse(spring.x.isNaN())
        assertFalse(spring.v.isNaN())

        spring.step(-1f)
        assertFalse(spring.x.isNaN())
        assertFalse(spring.v.isNaN())

        spring.step(Float.NaN)
        assertFalse(spring.x.isNaN())
        assertFalse(spring.v.isNaN())

        spring.step(Float.POSITIVE_INFINITY)
        assertFalse(spring.x.isNaN())
        assertFalse(spring.v.isNaN())

        spring.step(10f)
        assertFalse(spring.x.isNaN())
        assertFalse(spring.v.isNaN())
        assertFalse(spring.x.isInfinite())
        assertFalse(spring.v.isInfinite())
    }

    @Test
    fun resetClearsState() {
        val spring = Spring1D(0.1f, 0.5f)
        spring.impulse(10f)
        spring.step(0.016f)
        assertTrue(spring.x != 0f || spring.v != 0f)

        spring.reset()
        assertEquals(0f, spring.x, 0.0001f)
        assertEquals(0f, spring.v, 0.0001f)
        assertTrue(spring.atRest)
    }

    @Test
    fun criticallyDampedNeverCrossesZero() {
        val spring = Spring1D(StrikeTiming.THUMP_RESPONSE_S, 1.0f)
        spring.impulse(50f)

        var timeS = 0f
        val dt = 0.001f
        var sawPositive = false
        var crossedZero = false

        while (timeS < 1.0f) {
            spring.step(dt)
            timeS += dt
            if (spring.x > 0.01f) {
                sawPositive = true
            }
            if (sawPositive && spring.x < 0f) {
                crossedZero = true
            }
        }

        assertTrue(sawPositive)
        assertFalse("Critically damped spring must not cross zero", crossedZero)
    }
}
