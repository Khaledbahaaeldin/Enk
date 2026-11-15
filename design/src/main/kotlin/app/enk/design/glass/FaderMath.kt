package app.enk.design.glass

import kotlin.math.roundToInt

object FaderMath {
    fun detent(fraction: Float): Float {
        if (fraction.isNaN()) return 0f
        val clamped = fraction.coerceIn(0f, 1f)
        return (clamped * 20f).roundToInt() / 20f
    }

    fun valueFor(fraction: Float, min: Int, max: Int): Int {
        return (min + detent(fraction) * (max - min)).roundToInt()
    }

    fun displayPercent(value: Int, min: Int, max: Int): Int {
        if (max == min) return 0
        return ((value.coerceIn(min, max) - min).toFloat() / (max - min) * 100f).roundToInt()
    }

    fun fractionFor(value: Int, min: Int, max: Int): Float {
        if (max == min) return 0f
        return (value.coerceIn(min, max) - min).toFloat() / (max - min)
    }

    fun knobLeftDp(fraction: Float, trackWidthDp: Float): Float {
        return detent(fraction) * (trackWidthDp - 24f)
    }
}
