package app.enk.design.motion

import kotlin.math.abs
import kotlin.math.min
import kotlin.math.round
import kotlin.math.sqrt

object Physics {
    fun project(velocity: Float, decel: Float = 0.998f): Float =
        (velocity / 1000f) * decel / (1f - decel)

    fun rubber(overshoot: Float, dimension: Float, c: Float = 0.55f): Float =
        overshoot * dimension * c / (dimension + c * abs(overshoot))
}

fun sheetShouldDismiss(y: Float, velocity: Float): Boolean =
    (y + Physics.project(velocity)) > 160f

fun stretch(velocity: Float): Pair<Float, Float> {
    val k = min(0.14f, abs(velocity) / 6000f)
    return Pair(1f + k, 1f - k / 2f)
}

fun settled(target: Float, spring: ShellSpring, reducedMotion: Boolean, start: Float, seconds: Float): Float {
    if (reducedMotion) return target
    val dt = 0.004f
    val steps = round(seconds / dt).toInt()
    val k = spring.stiffness
    val c = 2f * spring.damping * sqrt(k)
    var x = start
    var v = 0f
    for (i in 0 until steps) {
        val accel = -k * (x - target) - c * v
        v += accel * dt
        x += v * dt
    }
    return x
}

fun maxPositionDuring(spring: ShellSpring, start: Float, target: Float, seconds: Float = 2f): Float {
    val dt = 0.004f
    val steps = round(seconds / dt).toInt()
    val k = spring.stiffness
    val c = 2f * spring.damping * sqrt(k)
    var x = start
    var v = 0f
    var maxPos = maxOf(start, target)
    for (i in 0 until steps) {
        val accel = -k * (x - target) - c * v
        v += accel * dt
        x += v * dt
        if (x > maxPos) {
            maxPos = x
        }
    }
    return maxPos
}
