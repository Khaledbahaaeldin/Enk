package app.enk.design.motion

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PhysicsTest {

    @Test
    fun projectOneThousand() {
        assertEquals(499.0f, Physics.project(1000f), 0.01f)
    }

    @Test
    fun projectNegative() {
        assertEquals(-499.0f, Physics.project(-1000f), 0.01f)
    }

    @Test
    fun rubberValue() {
        assertEquals(48.35f, Physics.rubber(100f, 400f), 0.01f)
    }

    @Test
    fun rubberZeroAndMonotonic() {
        assertEquals(0f, Physics.rubber(0f, 400f), 0.0001f)
        assertTrue(Physics.rubber(200f, 400f) > Physics.rubber(100f, 400f))
        assertTrue(Physics.rubber(10_000f, 400f) < 400f)
    }

    @Test
    fun stiffnessDefault() {
        assertEquals(322.3f, ShellMotion.default.stiffness, 0.5f)
    }

    @Test
    fun sheetDismissRule() {
        assertFalse(sheetShouldDismiss(100f, 0f))
        assertTrue(sheetShouldDismiss(100f, 200f))
        assertTrue(sheetShouldDismiss(0f, 400f))
        assertFalse(sheetShouldDismiss(0f, 100f))
    }

    @Test
    fun stretchValues() {
        assertEquals(1f to 1f, stretch(0f))
        val s = stretch(6000f)
        assertEquals(1.14f, s.first, 0.001f)
        assertEquals(0.93f, s.second, 0.001f)
    }

    @Test
    fun stretchNegativeAndClamped() {
        val neg = stretch(-6000f)
        assertEquals(1.14f, neg.first, 0.001f)
        assertEquals(0.93f, neg.second, 0.001f)

        val clampedPos = stretch(12000f)
        assertEquals(1.14f, clampedPos.first, 0.001f)
        assertEquals(0.93f, clampedPos.second, 0.001f)

        val clampedNeg = stretch(-12000f)
        assertEquals(1.14f, clampedNeg.first, 0.001f)
        assertEquals(0.93f, clampedNeg.second, 0.001f)
    }

    @Test
    fun rubberNegativeAndExtreme() {
        assertEquals(-48.35f, Physics.rubber(-100f, 400f), 0.01f)
        assertTrue(Physics.rubber(-10_000f, 400f) > -400f)
        assertTrue(Physics.rubber(1_000_000f, 400f) < 400f)
        assertTrue(Physics.rubber(-1_000_000f, 400f) > -400f)
    }

    @Test
    fun projectZeroAndExtremes() {
        assertEquals(0f, Physics.project(0f), 0.0001f)
    }

    @Test
    fun sheetDismissNegativeVelocity() {
        assertFalse(sheetShouldDismiss(200f, -500f))
    }

    @Test
    fun stiffnessNamedSprings() {
        assertEquals(503.55f, ShellMotion.toggle.stiffness, 1f)
        assertEquals(273.4f, ShellMotion.sheet.stiffness, 1f)
    }
}
