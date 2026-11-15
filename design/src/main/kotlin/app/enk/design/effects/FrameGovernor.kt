package app.enk.design.effects

import kotlin.math.ceil

class FrameGovernor(
    private val windowFrames: Int = 120,
    private val frameIntervalMs: Float = 16.67f
) {
    var demoted: ResolvedEffects? = null
        private set

    private val window = ArrayList<Float>(windowFrames)

    /** Feed one frame's total duration in ms; returns the new demotion when this frame caused one, else null. */
    fun onFrame(totalMs: Float, current: ResolvedEffects): ResolvedEffects? {
        if (current == ResolvedEffects.Light) {
            return null
        }
        if (totalMs.isNaN() || totalMs < 0f || totalMs > 300f) {
            return null
        }

        window.add(totalMs)
        if (window.size < windowFrames) {
            return null
        }

        window.sort()
        val index = (ceil(0.95 * window.size).toInt() - 1).coerceIn(0, window.size - 1)
        val p95 = window[index]
        window.clear()

        val threshold = 1.5f * frameIntervalMs
        if (p95 > threshold) {
            val newDemotion = when (current) {
                ResolvedEffects.Full -> ResolvedEffects.Balanced
                ResolvedEffects.Balanced -> ResolvedEffects.Light
                ResolvedEffects.Light -> null
            }
            if (newDemotion != null) {
                demoted = newDemotion
                return newDemotion
            }
        }

        return null
    }
}
