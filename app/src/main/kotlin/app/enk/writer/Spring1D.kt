package app.enk.writer

import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.sqrt

class Spring1D(responseS: Float, damping: Float) {
    var x: Float = 0f
    var v: Float = 0f

    private val k: Float = ((2f * PI.toFloat()) / responseS).let { it * it }
    private val c: Float = 2f * damping * sqrt(k)

    fun impulse(dv: Float) {
        v += dv
    }

    fun step(dtS: Float) {
        if (dtS.isNaN() || dtS.isInfinite()) return
        val clampedDt = dtS.coerceIn(0f, 0.05f)
        if (clampedDt <= 0f) return

        val subStep = 0.004f
        var remaining = clampedDt
        while (remaining > 0f) {
            val dt = remaining.coerceAtMost(subStep)
            // semi-implicit Euler
            val a = -k * x - c * v
            v += a * dt
            x += v * dt
            remaining -= dt
        }
    }

    val atRest: Boolean
        get() = abs(x) < 0.01f && abs(v) < 0.5f

    fun reset() {
        x = 0f
        v = 0f
    }
}
