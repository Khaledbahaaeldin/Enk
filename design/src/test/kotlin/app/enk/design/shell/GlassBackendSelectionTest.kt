package app.enk.design.shell

import org.junit.Assert.assertEquals
import org.junit.Test

class GlassBackendSelectionTest {

    @Test
    fun sdk31IsBlur() {
        assertEquals(GlassBackendKind.Blur, chooseGlassBackend(31, false, false))
    }

    @Test
    fun sdk32IsBlur() {
        assertEquals(GlassBackendKind.Blur, chooseGlassBackend(32, false, false))
    }

    @Test
    fun sdk33IsRefraction() {
        assertEquals(GlassBackendKind.Refraction, chooseGlassBackend(33, false, false))
    }

    @Test
    fun reduceEffectsIsSolid() {
        assertEquals(GlassBackendKind.Solid, chooseGlassBackend(35, true, false))
    }

    @Test
    fun highContrastIsSolid() {
        assertEquals(GlassBackendKind.Solid, chooseGlassBackend(35, false, true))
    }

    @Test
    fun boundarySdk32Versus33() {
        assertEquals(GlassBackendKind.Blur, chooseGlassBackend(32, false, false))
        assertEquals(GlassBackendKind.Refraction, chooseGlassBackend(33, false, false))
    }

    @Test
    fun olderSdkDefaultsToBlur() {
        assertEquals(GlassBackendKind.Blur, chooseGlassBackend(30, false, false))
        assertEquals(GlassBackendKind.Blur, chooseGlassBackend(28, false, false))
        assertEquals(GlassBackendKind.Blur, chooseGlassBackend(1, false, false))
    }

    @Test
    fun newerSdkUsesRefraction() {
        assertEquals(GlassBackendKind.Refraction, chooseGlassBackend(34, false, false))
        assertEquals(GlassBackendKind.Refraction, chooseGlassBackend(35, false, false))
    }

    @Test
    fun bothReduceEffectsAndHighContrastAreSolid() {
        assertEquals(GlassBackendKind.Solid, chooseGlassBackend(35, true, true))
        assertEquals(GlassBackendKind.Solid, chooseGlassBackend(31, true, true))
    }

    @Test
    fun solidPreemptsRefractionOnHighSdk() {
        assertEquals(GlassBackendKind.Solid, chooseGlassBackend(33, true, false))
        assertEquals(GlassBackendKind.Solid, chooseGlassBackend(34, false, true))
        assertEquals(GlassBackendKind.Solid, chooseGlassBackend(35, true, false))
    }
}
