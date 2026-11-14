package app.enk.keyboard

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class KeyboardLayoutFitsTest {

    @Test
    fun compactWindowsHeightRatios() {
        val testHeights = listOf(360f, 411f, 480f, 640f, 800f, 915f, 1000f)
        val expectedTotals = mapOf(
            360f to 151f,
            411f to 151f,
            480f to 196f,
            640f to 232f,
            800f to 232f,
            915f to 232f,
            1000f to 232f
        )

        println("Window Height (dp) | Variant | Keyboard Height (dp) | Ratio | Allowed Max")
        println("-------------------|---------|----------------------|-------|------------")

        for (h in testHeights) {
            val metrics = KeyboardMetrics.forWindow(h)
            val variant = when {
                h >= 640f -> "FULL"
                h >= 480f -> "MEDIUM"
                else -> "COMPACT"
            }
            val totalH = metrics.totalHeightDp(4)
            assertEquals("Exact height for window $h", expectedTotals[h]!!, totalH, 0.001f)

            val ratio = totalH / h
            val maxAllowed = if (h >= 640f) 0.45f else 0.60f
            println(String.format("%-18.0f | %-7s | %-20.1f | %-5.1f%% | <= %-2.0f%%", h, variant, totalH, ratio * 100f, maxAllowed * 100f))

            if (h >= 640f) {
                assertTrue("Height $h ratio $ratio should be <= 0.45", ratio <= 0.45f)
            } else {
                assertTrue("Height $h ratio $ratio should be <= 0.60", ratio <= 0.60f)
            }
        }
    }
}
