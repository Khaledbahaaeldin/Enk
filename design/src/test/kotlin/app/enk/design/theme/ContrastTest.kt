package app.enk.design.theme

import androidx.compose.ui.graphics.Color
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ContrastTest {

    @Test
    fun contrastRatioKnown() {
        assertEquals(21.0, Contrast.ratio(Color.White, Color.Black), 0.01)
        assertEquals(1.0, Contrast.ratio(Color.White, Color.White), 0.01)
        assertEquals(1.0, Contrast.ratio(Color.Black, Color.Black), 0.01)
    }

    @Test
    fun ensureRaisesContrast() {
        val fixed = Contrast.ensure(Color(0xFF777777), Color(0xFF111111), 4.5, towardLight = true)
        assertTrue(Contrast.ratio(fixed, Color(0xFF111111)) >= 4.5)
    }

    @Test
    fun ensureRaisesContrastDarker() {
        val fixed = Contrast.ensure(Color(0xFF888888), Color(0xFFFFFFFF), 4.5, towardLight = false)
        assertTrue(Contrast.ratio(fixed, Color(0xFFFFFFFF)) >= 4.5)
    }

    @Test
    fun ensureAlreadyPassingDoesNotChange() {
        val sameLight = Contrast.ensure(Color.White, Color.Black, 4.5, towardLight = true)
        assertEquals(Color.White, sameLight)

        val sameDark = Contrast.ensure(Color.Black, Color.White, 4.5, towardLight = false)
        assertEquals(Color.Black, sameDark)
    }

    @Test
    fun ensureWhenFgIdenticalToBgTowardLightRaisesLightness() {
        val fixed = Contrast.ensure(Color.Black, Color.Black, 4.5, towardLight = true)
        assertTrue(Contrast.ratio(fixed, Color.Black) >= 4.5)
        assertTrue(fixed.red > 0f || fixed.green > 0f || fixed.blue > 0f)
    }

    @Test
    fun ensureWhenFgIdenticalToBgTowardDarkLowersLightness() {
        val fixed = Contrast.ensure(Color.White, Color.White, 4.5, towardLight = false)
        assertTrue(Contrast.ratio(fixed, Color.White) >= 4.5)
        assertTrue(fixed.red < 1f || fixed.green < 1f || fixed.blue < 1f)
    }

    @Test
    fun ensureWhenLightnessHitsUpperBoundaryTerminatesWithoutHanging() {
        val result = Contrast.ensure(Color.White, Color.White, 4.5, towardLight = true)
        assertEquals(Color.White, result)
    }

    @Test
    fun ensureWhenLightnessHitsLowerBoundaryTerminatesWithoutHanging() {
        val result = Contrast.ensure(Color.Black, Color.Black, 4.5, towardLight = false)
        assertEquals(Color.Black, result)
    }

    @Test
    fun ensureImpossibleRatioTerminatesAtBoundary() {
        val result = Contrast.ensure(Color(0xFF888888), Color.Black, 30.0, towardLight = true)
        // Ratio max is 21.0, so 30.0 is impossible. Lightness should clamp at 1.0f (White).
        assertEquals(Color.White, result)
    }

    @Test
    fun ensurePreservesAlphaChannel() {
        val fg = Color(red = 0.5f, green = 0.5f, blue = 0.5f, alpha = 0.75f)
        val fixed = Contrast.ensure(fg, Color.Black, 4.5, towardLight = true)
        assertEquals(0.75f, fixed.alpha, 0.001f)
        assertTrue(Contrast.ratio(fixed, Color.Black) >= 4.5)
    }
}
