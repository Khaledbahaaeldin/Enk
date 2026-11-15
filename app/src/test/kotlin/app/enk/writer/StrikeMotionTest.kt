package app.enk.writer

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class StrikeMotionTest {

    @Test
    fun strikeWeightValues() {
        assertEquals(1.0f, strikeWeight("a".codePointAt(0)), 0.001f)
        assertEquals(1.4f, strikeWeight("A".codePointAt(0)), 0.001f)
        assertEquals(1.4f, strikeWeight("É".codePointAt(0)), 0.001f)
        assertEquals(0.5f, strikeWeight(".".codePointAt(0)), 0.001f)
        assertEquals(0.5f, strikeWeight("-".codePointAt(0)), 0.001f)
        assertEquals(1.4f, strikeWeight("@".codePointAt(0)), 0.001f)
        assertEquals(1.0f, strikeWeight("7".codePointAt(0)), 0.001f)

        // Surrogate pair emoji: e.g. 🚂 U+1F682
        val trainEmoji = "\uD83D\uDE82".codePointAt(0)
        assertEquals(1.0f, strikeWeight(trainEmoji), 0.001f)

        // Combining accent: e.g. U+0301 COMBINING ACUTE ACCENT
        val combiningAccent = "\u0301".codePointAt(0)
        assertEquals(1.0f, strikeWeight(combiningAccent), 0.001f)
    }

    @Test
    fun strikeFrameExactValuesAtWeight1Normal() {
        // t = 0
        val f0 = strikeFrame(0f, 1.0f, fast = false)
        assertFalse(f0.glyphVisible)
        assertEquals(0f, f0.slugRiseDp, 0.001f)
        assertEquals(1f, f0.glyphScale, 0.001f)
        assertEquals(0f, f0.glyphDarken, 0.001f)
        assertEquals(0f, f0.haloDp, 0.001f)
        assertEquals(0f, f0.flashAlpha, 0.001f)
        assertEquals(StrikeTiming.FLASH_RADIUS_START_DP, f0.flashRadiusDp, 0.001f)

        // t = 13 (halfway to impact: (13/26)^3 * 17 = 0.125 * 17 = 2.125)
        val f13 = strikeFrame(13f, 1.0f, fast = false)
        assertFalse(f13.glyphVisible)
        assertEquals(2.125f, f13.slugRiseDp, 0.001f)
        assertEquals(1f, f13.glyphScale, 0.001f)
        assertEquals(0f, f13.glyphDarken, 0.001f)
        assertEquals(0f, f13.haloDp, 0.001f)
        assertEquals(0f, f13.flashAlpha, 0.001f)

        // t = 26 (impact instant)
        val f26 = strikeFrame(26f, 1.0f, fast = false)
        assertTrue(f26.glyphVisible)
        assertEquals(17.0f, f26.slugRiseDp, 0.001f)
        assertEquals(1.10f, f26.glyphScale, 0.001f)
        assertEquals(0.22f, f26.glyphDarken, 0.001f)
        assertEquals(2.2f, f26.haloDp, 0.001f)
        assertEquals(0.35f, f26.flashAlpha, 0.001f)
        assertEquals(9.0f, f26.flashRadiusDp, 0.001f)

        // t = 61 (ia = 35)
        val f61 = strikeFrame(61f, 1.0f, fast = false)
        assertTrue(f61.glyphVisible)
        assertEquals(2.778f, f61.slugRiseDp, 0.01f)
        assertEquals(1.0125f, f61.glyphScale, 0.001f)
        assertEquals(0.22f, f61.glyphDarken, 0.001f)
        assertEquals(0.489f, f61.haloDp, 0.002f)
        assertEquals(0.175f, f61.flashAlpha, 0.001f)
        assertEquals(12.0f, f61.flashRadiusDp, 0.001f)

        // t = 96 (ia = 70)
        val f96 = strikeFrame(96f, 1.0f, fast = false)
        assertTrue(f96.glyphVisible)
        assertEquals(0.064f, f96.slugRiseDp, 0.01f)
        assertEquals(1.0f, f96.glyphScale, 0.001f)
        assertEquals(0.0f, f96.glyphDarken, 0.001f)
        assertEquals(0.0f, f96.haloDp, 0.001f)
        assertEquals(0.0f, f96.flashAlpha, 0.001f)
        assertEquals(15.0f, f96.flashRadiusDp, 0.001f)

        // t = 110 (impact + RETURN_MS = 26 + 84 = 110)
        val f110 = strikeFrame(110f, 1.0f, fast = false)
        assertTrue(f110.glyphVisible)
        assertEquals(0.0f, f110.slugRiseDp, 0.001f)
        assertEquals(1.0f, f110.glyphScale, 0.001f)
        assertEquals(0.0f, f110.glyphDarken, 0.001f)
        assertEquals(0.0f, f110.haloDp, 0.001f)
        assertEquals(0.0f, f110.flashAlpha, 0.001f)
        assertEquals(15.0f, f110.flashRadiusDp, 0.001f)
    }

    @Test
    fun boundaryConditionsAndProperties() {
        val testTimes = listOf(-1f, 0f, 13f, 17.9f, 18f, 25.9f, 26f, 45f, 96f, 110f, 130f, 1000f, Float.NaN, Float.POSITIVE_INFINITY)
        val weights = listOf(0.5f, 1.0f, 1.4f, 0f, Float.NaN)

        for (w in weights) {
            for (fast in listOf(false, true)) {
                val impact = if (fast) StrikeTiming.FAST_IMPACT_MS else StrikeTiming.IMPACT_MS
                var prevRise = -1f

                for (t in testTimes) {
                    val frame = strikeFrame(t, w, fast)

                    if (t.isNaN() || t.isInfinite()) {
                        assertEquals(StrikeFrame.Rest, frame)
                        continue
                    }

                    if (t < 0f) {
                        assertFalse(frame.glyphVisible)
                        assertEquals(0f, frame.slugRiseDp, 0.001f)
                        continue
                    }

                    // Visibility boundary
                    if (t < impact) {
                        assertFalse("At t=$t with impact=$impact glyph must be invisible", frame.glyphVisible)
                    } else {
                        assertTrue("At t=$t with impact=$impact glyph must be visible", frame.glyphVisible)
                    }

                    // Slug rise bounds
                    assertTrue("Rise ${frame.slugRiseDp} must be >= 0 at t=$t", frame.slugRiseDp >= 0f)
                    assertTrue("Rise ${frame.slugRiseDp} must be <= 17 at t=$t", frame.slugRiseDp <= 17.0001f)

                    // Monotonic up to impact
                    if (t in 0f..impact) {
                        if (prevRise >= 0f) {
                            assertTrue("Rise must be non-decreasing before impact (prev=$prevRise, cur=${frame.slugRiseDp})", frame.slugRiseDp >= prevRise - 0.001f)
                        }
                        prevRise = frame.slugRiseDp
                    }

                    // After impact properties
                    if (t >= impact) {
                        val ia = t - impact
                        if (ia >= StrikeTiming.IMPRESSION_MS) {
                            assertEquals(1.0f, frame.glyphScale, 0.001f)
                        }
                        if (ia >= StrikeTiming.DARKEN_MS) {
                            assertEquals(0.0f, frame.glyphDarken, 0.001f)
                        } else {
                            assertEquals(StrikeTiming.DARKEN_AMOUNT, frame.glyphDarken, 0.001f)
                        }
                        if (ia >= StrikeTiming.FLASH_MS) {
                            assertEquals(0.0f, frame.flashAlpha, 0.001f)
                            assertEquals(StrikeTiming.FLASH_RADIUS_END_DP, frame.flashRadiusDp, 0.001f)
                        }
                    }
                }
            }
        }
    }

    @Test
    fun weightAndFastEffects() {
        val normalLight = strikeFrame(26f, 0.5f, fast = false)
        val normalHeavy = strikeFrame(26f, 1.4f, fast = false)
        assertTrue(normalHeavy.glyphScale > normalLight.glyphScale)
        assertTrue(normalHeavy.haloDp > normalLight.haloDp)
        assertEquals(normalLight.slugRiseDp, normalHeavy.slugRiseDp, 0.001f)
        assertEquals(normalLight.flashAlpha, normalHeavy.flashAlpha, 0.001f)

        val fastHeavy = strikeFrame(18f, 1.4f, fast = true)
        assertTrue(fastHeavy.glyphVisible)
        assertEquals(StrikeTiming.HALO_BASE_DP * 1.4f * 0.3f, fastHeavy.haloDp, 0.001f)
    }
}
