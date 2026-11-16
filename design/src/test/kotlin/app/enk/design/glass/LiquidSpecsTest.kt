package app.enk.design.glass

import app.enk.design.effects.LiquidTuning
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class LiquidSpecsTest {

    @Test
    fun balancedShrinksLensAndDropsFringe() {
        val island = LiquidSpecs.Island
        val b = LiquidSpecs.balanced(island)
        assertEquals(9.6f, b.lensHeightDp, 0.001f)
        assertEquals(19.2f, b.lensAmountDp, 0.001f)
        assertFalse(b.fringe)
        assertFalse(b.depth)
        assertEquals(RimLight.Plain, b.rim)
    }

    @Test
    fun tunedScalesAndClamps() {
        val base = LiquidSpecs.Button // 2 blur, 12 height, 24 amount, fringe false
        val tuning = LiquidTuning(
            refraction = 2f,
            lensDepth = 1.5f,
            fringe = 0f,
            frostDp = 6f
        )
        val t = LiquidSpecs.tuned(base, tuning)
        assertEquals(48f, t.lensAmountDp, 0.001f)
        assertEquals(18f, t.lensHeightDp, 0.001f)
        assertEquals(8f, t.blurDp, 0.001f)
        assertFalse(t.fringe)

        val fringedBase = LiquidSpecs.Island // fringe = true
        val t2 = LiquidSpecs.tuned(fringedBase, LiquidTuning(fringe = 0f))
        assertFalse(t2.fringe)

        val t3 = LiquidSpecs.tuned(fringedBase, LiquidTuning(fringe = 1.5f))
        assertTrue(t3.fringe)
    }

    @Test
    fun catalogValues() {
        assertEquals(2f, LiquidSpecs.Button.blurDp, 0.001f)
        assertEquals(12f, LiquidSpecs.Button.lensHeightDp, 0.001f)
        assertEquals(24f, LiquidSpecs.Button.lensAmountDp, 0.001f)

        assertEquals(8f, LiquidSpecs.Bar.blurDp, 0.001f)
        assertEquals(12f, LiquidSpecs.Bar.lensHeightDp, 0.001f)
        assertEquals(24f, LiquidSpecs.Bar.lensAmountDp, 0.001f)

        assertEquals(0f, LiquidSpecs.Thumb.blurDp, 0.001f)
        assertEquals(10f, LiquidSpecs.Thumb.lensHeightDp, 0.001f)
        assertEquals(14f, LiquidSpecs.Thumb.lensAmountDp, 0.001f)
        assertTrue(LiquidSpecs.Thumb.fringe)
    }
}
