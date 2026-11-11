package app.enk.perf

/**
 * Pure functions for frame timing and jank rate computations.
 */
object FrameMetricsMath {

    /**
     * Computes the p-th percentile from a list of durations.
     * Returns 0.0 for empty or invalid lists. Result is always non-NaN and non-negative.
     */
    fun percentile(durationsMs: List<Double>, p: Double): Double {
        if (durationsMs.isEmpty()) return 0.0
        val clean = durationsMs.filter { !it.isNaN() && it >= 0.0 }
        if (clean.isEmpty()) return 0.0
        if (clean.size == 1) return clean[0]

        val sorted = clean.sorted()
        val clampedP = p.coerceIn(0.0, 100.0)
        val rank = (clampedP / 100.0) * (sorted.size - 1)
        val lowerIndex = rank.toInt()
        val upperIndex = (lowerIndex + 1).coerceAtMost(sorted.size - 1)
        val fraction = rank - lowerIndex

        return sorted[lowerIndex] + fraction * (sorted[upperIndex] - sorted[lowerIndex])
    }

    /**
     * Computes the fraction of frames that exceeded the jank threshold (e.g. 16.666 ms).
     * Returns a ratio in [0.0..1.0]. Returns 0.0 for empty lists.
     */
    fun jankFraction(durationsMs: List<Double>, thresholdMs: Double = 16.666): Double {
        if (durationsMs.isEmpty()) return 0.0
        val clean = durationsMs.filter { !it.isNaN() && it >= 0.0 }
        if (clean.isEmpty()) return 0.0

        val jankyCount = clean.count { it > thresholdMs }
        return jankyCount.toDouble() / clean.size
    }
}
