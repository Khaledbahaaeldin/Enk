package app.enk.design.shell

import app.enk.design.effects.ResolvedEffects
import org.junit.Assert.assertEquals
import org.junit.Test

class GlassBackendSelectionTest {

    @Test
    fun highContrastForcesSolidAcrossAllCombinations() {
        for (level in ResolvedEffects.entries) {
            for (sdk in listOf(31, 32, 33, 34, 37)) {
                assertEquals(
                    "High contrast must force Solid for level=$level, sdk=$sdk",
                    GlassBackendKind.Solid,
                    resolveGlassBackend(level, sdk, highContrastText = true)
                )
            }
        }
    }

    @Test
    fun lightLevelForcesSolidAcrossAllSdks() {
        for (sdk in listOf(31, 32, 33, 34, 37)) {
            assertEquals(
                "Light level must be Solid for sdk=$sdk",
                GlassBackendKind.Solid,
                resolveGlassBackend(ResolvedEffects.Light, sdk, highContrastText = false)
            )
        }
    }

    @Test
    fun balancedLevelUsesBlurAcrossAllSdks() {
        for (sdk in listOf(31, 32, 33, 34, 37)) {
            assertEquals(
                "Balanced level must be Blur for sdk=$sdk",
                GlassBackendKind.Blur,
                resolveGlassBackend(ResolvedEffects.Balanced, sdk, highContrastText = false)
            )
        }
    }

    @Test
    fun fullLevelUsesBlurBelowSdk33AndRefractionOnSdk33Plus() {
        assertEquals(
            GlassBackendKind.Blur,
            resolveGlassBackend(ResolvedEffects.Full, sdkInt = 31, highContrastText = false)
        )
        assertEquals(
            GlassBackendKind.Blur,
            resolveGlassBackend(ResolvedEffects.Full, sdkInt = 32, highContrastText = false)
        )
        assertEquals(
            GlassBackendKind.Refraction,
            resolveGlassBackend(ResolvedEffects.Full, sdkInt = 33, highContrastText = false)
        )
        assertEquals(
            GlassBackendKind.Refraction,
            resolveGlassBackend(ResolvedEffects.Full, sdkInt = 34, highContrastText = false)
        )
        assertEquals(
            GlassBackendKind.Refraction,
            resolveGlassBackend(ResolvedEffects.Full, sdkInt = 37, highContrastText = false)
        )
    }
}
