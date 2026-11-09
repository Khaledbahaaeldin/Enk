package app.enk.design

import androidx.compose.ui.Modifier
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotSame
import org.junit.Assert.assertSame
import org.junit.Test

class LampGeometryTest {

    @Test
    fun beamBottomYDp() {
        assertEquals(484f, LampGeometry.beamBottomYDp(508f), 0.001f)
        assertEquals(476f, LampGeometry.beamBottomYDp(500f), 0.001f)
    }

    @Test
    fun haloCenterYDp() {
        assertEquals(482f, LampGeometry.haloCenterYDp(508f), 0.001f)
        assertEquals(474f, LampGeometry.haloCenterYDp(500f), 0.001f)
    }

    @Test
    fun maskEdgeAlphaValuesAndClamping() {
        assertEquals(0.18f, LampGeometry.maskEdgeAlpha(80), 0.001f)
        assertEquals(0.12f, LampGeometry.maskEdgeAlpha(20), 0.001f)
        assertEquals(0.10f, LampGeometry.maskEdgeAlpha(0), 0.001f)
        assertEquals(0.20f, LampGeometry.maskEdgeAlpha(100), 0.001f)

        // Clamping below 0 and above 100
        assertEquals(0.10f, LampGeometry.maskEdgeAlpha(-20), 0.001f)
        assertEquals(0.20f, LampGeometry.maskEdgeAlpha(120), 0.001f)
    }

    @Test
    fun textMaskCenterDp() {
        val carriageX = 180f
        val railY = 508f
        val paperTop = 64f
        val (centerX, centerY) = LampGeometry.textMaskCenterDp(carriageX, railY, paperTop)

        assertEquals("mask center x is carriageX - 12f", 168f, centerX, 0.001f)
        // railY - 116f - paperTop = 508 - 116 - 64 = 328f
        assertEquals("mask center y is railY - 116f - paperTop", 328f, centerY, 0.001f)
    }

    @Test
    fun lampTextMaskInactiveIsIdentity() {
        val settings = LampSettings(warmth = 25, intensity = 80, beam = 55)
        val initial = Modifier
        val result = initial.lampTextMask(
            carriageXDp = 180f,
            railYDp = 508f,
            paperTopDp = 64f,
            settings = settings,
            active = false
        )
        assertSame("Inactive lampTextMask must return the exact same Modifier instance", initial, result)
    }

    @Test
    fun lampTextMaskActiveModifiesModifier() {
        val settings = LampSettings(warmth = 25, intensity = 80, beam = 55)
        val initial = Modifier
        val result = initial.lampTextMask(
            carriageXDp = 180f,
            railYDp = 508f,
            paperTopDp = 64f,
            settings = settings,
            active = true
        )
        assertNotSame("Active lampTextMask must modify Modifier", initial, result)
    }

    @Test
    fun beamWidthDp() {
        // default beam 55 gives 213f via LampMath.beamWidthDp(55)
        assertEquals(213f, LampMath.beamWidthDp(55), 0.001f)
        assertEquals(148f, LampMath.beamWidthDp(30), 0.001f)
        assertEquals(330f, LampMath.beamWidthDp(100), 0.001f)
    }

    @Test
    fun lampSettingsModel() {
        val settings = LampSettings(warmth = 25, intensity = 80, beam = 55)
        assertEquals(25, settings.warmth)
        assertEquals(80, settings.intensity)
        assertEquals(55, settings.beam)
    }
}
