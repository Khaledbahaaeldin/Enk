package app.enk.design

import org.junit.Assert.assertEquals
import org.junit.Test

class LampColorTest {

    @Test
    fun warmth0() {
        assertEquals(Rgb(255, 236, 214), LampColor.fromWarmth(0))
    }

    @Test
    fun warmth25() {
        assertEquals(Rgb(255, 215, 178), LampColor.fromWarmth(25))
    }

    @Test
    fun warmth50() {
        // Independently re-derived:
        // w = 0.5 -> g = round(236 - 86 * 0.5) = 193, b = round(214 - 144 * 0.5) = 142
        assertEquals(Rgb(255, 193, 142), LampColor.fromWarmth(50))
    }

    @Test
    fun warmth100() {
        assertEquals(Rgb(255, 150, 70), LampColor.fromWarmth(100))
    }

    @Test
    fun lampMathBoundaries() {
        // Slider min (0)
        assertEquals(0.0f, LampMath.intensity(0), 0.0001f)
        assertEquals(70.0f, LampMath.beamWidthDp(0), 0.001f)
        // Slider max (100)
        assertEquals(1.0f, LampMath.intensity(100), 0.0001f)
        assertEquals(330.0f, LampMath.beamWidthDp(100), 0.001f)
    }

    @Test
    fun beamDefault() {
        assertEquals(213f, LampMath.beamWidthDp(55), 0.001f)
    }

    @Test
    fun intensityDefault() {
        assertEquals(0.8f, LampMath.intensity(80), 0.0001f)
    }
}
