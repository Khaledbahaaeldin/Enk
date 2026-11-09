package app.enk.start

object StartGate {
    fun shouldPlay(
        coldStart: Boolean,
        launchedFromWidgetOrIntent: Boolean,
        restoredState: Boolean,
        loadMs: Long
    ): Boolean = coldStart && !launchedFromWidgetOrIntent && !restoredState && loadMs <= 1500L

    fun reducedMotionDurationMs(): Int = 250
}
