package app.enk.design.shell

import org.junit.Assert.assertEquals
import org.junit.Test

class ScrollProgressTest {

    @Test
    fun collapseProgressValues() {
        assertEquals(0f, collapseProgress(0f), 0.0001f)
        assertEquals(0f, collapseProgress(30f), 0.0001f)
        assertEquals(0.5f, collapseProgress(48f), 0.001f)
        assertEquals(1f, collapseProgress(66f), 0.0001f)
        assertEquals(1f, collapseProgress(500f), 0.0001f)
    }

    @Test
    fun collapseProgressNegativeValues() {
        assertEquals(0f, collapseProgress(-100f), 0.0001f)
        assertEquals(0f, collapseProgress(-1f), 0.0001f)
        assertEquals(0f, collapseProgress(-0.001f), 0.0001f)
    }

    @Test
    fun collapseProgressExactBoundaries() {
        assertEquals(0f, collapseProgress(30f), 0.0001f)
        assertEquals(0f, collapseProgress(29.999f), 0.0001f)
        org.junit.Assert.assertTrue(collapseProgress(30.001f) > 0f)
        org.junit.Assert.assertTrue(collapseProgress(65.999f) < 1f)
        assertEquals(1f, collapseProgress(66f), 0.0001f)
        assertEquals(1f, collapseProgress(66.001f), 0.0001f)
    }

    @Test
    fun collapseProgressExtremeValues() {
        assertEquals(1f, collapseProgress(Float.MAX_VALUE), 0.0001f)
        assertEquals(0f, collapseProgress(Float.NEGATIVE_INFINITY), 0.0001f)
        assertEquals(1f, collapseProgress(Float.POSITIVE_INFINITY), 0.0001f)
    }
}
