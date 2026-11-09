package app.enk.writer

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class MotionPolicyTest {

    @Test
    fun standardSettingsDoesNotReduceMotion() {
        assertFalse(MotionPolicy.shouldReduceMotion(settingsReduceEffects = false, animatorDurationScale = 1.0f))
    }

    @Test
    fun settingsReduceEffectsReducesMotion() {
        assertTrue(MotionPolicy.shouldReduceMotion(settingsReduceEffects = true, animatorDurationScale = 1.0f))
        assertTrue(MotionPolicy.shouldReduceMotion(settingsReduceEffects = true, animatorDurationScale = 0.5f))
    }

    @Test
    fun zeroDurationScaleReducesMotion() {
        assertTrue(MotionPolicy.shouldReduceMotion(settingsReduceEffects = false, animatorDurationScale = 0f))
        assertTrue(MotionPolicy.shouldReduceMotion(settingsReduceEffects = true, animatorDurationScale = 0f))
    }
}
