package app.enk.start

/**
 * Decides how long the system splash stays up: until the note and settings have loaded, but never
 * past [capMs]. Pure, so the cap is testable with a fake clock. It also records how long loading
 * took, which [StartGate] needs.
 */
class SplashHold(private val startMs: Long, private val capMs: Long = CAP_MS) {
    var loadMs: Long? = null
        private set

    /** True while the splash should stay on screen. */
    fun shouldHold(loaded: Boolean, nowMs: Long): Boolean {
        val elapsed = nowMs - startMs
        if (loadMs == null && (loaded || elapsed >= capMs)) loadMs = elapsed
        return !loaded && elapsed < capMs
    }

    companion object {
        const val CAP_MS = 1500L
    }
}
