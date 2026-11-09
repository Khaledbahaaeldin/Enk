package app.enk.writer

object MotionPolicy {
    fun shouldReduceMotion(settingsReduceEffects: Boolean, animatorDurationScale: Float): Boolean {
        return settingsReduceEffects || animatorDurationScale == 0f
    }
}
