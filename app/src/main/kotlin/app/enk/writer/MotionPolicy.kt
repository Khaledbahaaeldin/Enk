package app.enk.writer

import app.enk.settings.reduceMotion

object MotionPolicy {
    fun shouldReduceMotion(settingsReduceEffects: Boolean, animatorDurationScale: Float): Boolean {
        return reduceMotion(settingsReduceEffects, animatorDurationScale)
    }
}
