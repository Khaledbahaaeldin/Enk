package app.enk.design.effects

import org.junit.Assert.assertEquals
import org.junit.Test

class EffectsResolveTest {

    private fun env(
        sdkInt: Int = 34,
        systemReducedMotion: Boolean = false,
        highContrastText: Boolean = false,
        lowRamDevice: Boolean = false,
        batterySaver: Boolean = false,
        slowFrames: ResolvedEffects? = null
    ) = EffectsEnvironment(
        sdkInt = sdkInt,
        systemReducedMotion = systemReducedMotion,
        highContrastText = highContrastText,
        lowRamDevice = lowRamDevice,
        batterySaver = batterySaver,
        slowFrames = slowFrames
    )

    @Test
    fun highContrastAndReducedMotionBeatEverything() {
        for (choice in EffectsLevel.entries) {
            assertEquals(
                "Reduced motion must force Light for $choice",
                ResolvedEffects.Light,
                resolveEffects(choice, env(systemReducedMotion = true))
            )
            assertEquals(
                "High contrast must force Light for $choice",
                ResolvedEffects.Light,
                resolveEffects(choice, env(highContrastText = true))
            )
            assertEquals(
                "Both flags must force Light for $choice",
                ResolvedEffects.Light,
                resolveEffects(choice, env(systemReducedMotion = true, highContrastText = true))
            )
        }
    }

    @Test
    fun explicitChoicesResolvedDirectly() {
        assertEquals(ResolvedEffects.Full, resolveEffects(EffectsLevel.Full, env()))
        assertEquals(ResolvedEffects.Balanced, resolveEffects(EffectsLevel.Balanced, env()))
        assertEquals(ResolvedEffects.Light, resolveEffects(EffectsLevel.Light, env()))
    }

    @Test
    fun explicitChoicesIgnoreSlowFrames() {
        assertEquals(ResolvedEffects.Full, resolveEffects(EffectsLevel.Full, env(slowFrames = ResolvedEffects.Light)))
        assertEquals(ResolvedEffects.Balanced, resolveEffects(EffectsLevel.Balanced, env(slowFrames = ResolvedEffects.Light)))
    }

    @Test
    fun autoAcrossSdkVersions() {
        assertEquals(ResolvedEffects.Balanced, resolveEffects(EffectsLevel.Auto, env(sdkInt = 31)))
        assertEquals(ResolvedEffects.Balanced, resolveEffects(EffectsLevel.Auto, env(sdkInt = 32)))
        assertEquals(ResolvedEffects.Full, resolveEffects(EffectsLevel.Auto, env(sdkInt = 33)))
        assertEquals(ResolvedEffects.Full, resolveEffects(EffectsLevel.Auto, env(sdkInt = 37)))
    }

    @Test
    fun autoLowRamDeviceForcesLight() {
        assertEquals(ResolvedEffects.Light, resolveEffects(EffectsLevel.Auto, env(sdkInt = 37, lowRamDevice = true)))
    }

    @Test
    fun autoBatterySaverForcesBalanced() {
        assertEquals(ResolvedEffects.Balanced, resolveEffects(EffectsLevel.Auto, env(sdkInt = 37, batterySaver = true)))
    }

    @Test
    fun autoSlowFramesDemotions() {
        // Base is Full (sdk 34, no low ram, no battery saver)
        assertEquals(ResolvedEffects.Full, resolveEffects(EffectsLevel.Auto, env(sdkInt = 34, slowFrames = null)))
        assertEquals(ResolvedEffects.Balanced, resolveEffects(EffectsLevel.Auto, env(sdkInt = 34, slowFrames = ResolvedEffects.Balanced)))
        assertEquals(ResolvedEffects.Light, resolveEffects(EffectsLevel.Auto, env(sdkInt = 34, slowFrames = ResolvedEffects.Light)))

        // Base is Balanced (sdk 31)
        assertEquals(ResolvedEffects.Balanced, resolveEffects(EffectsLevel.Auto, env(sdkInt = 31, slowFrames = null)))
        // slowFrames = Full is ignored because base Balanced is weaker (ordinal 1 > 0)
        assertEquals(ResolvedEffects.Balanced, resolveEffects(EffectsLevel.Auto, env(sdkInt = 31, slowFrames = ResolvedEffects.Full)))
        // slowFrames = Light demotes Balanced to Light
        assertEquals(ResolvedEffects.Light, resolveEffects(EffectsLevel.Auto, env(sdkInt = 31, slowFrames = ResolvedEffects.Light)))
    }
}
