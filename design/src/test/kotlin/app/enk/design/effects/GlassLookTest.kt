package app.enk.design.effects

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class GlassLookTest {

    @Test
    fun resolvesToFrostBelowApi33() {
        assertEquals(GlassLook.Frost, resolveGlassLook(GlassLook.Liquid, 32))
        assertEquals(GlassLook.Liquid, resolveGlassLook(GlassLook.Liquid, 33))
        assertEquals(GlassLook.Frost, resolveGlassLook(GlassLook.Frost, 36))
    }

    @Test
    fun coercedClampsEveryField() {
        val belowLiquid = LiquidTuning(
            refraction = -1f,
            lensDepth = 0.1f,
            fringe = -0.5f,
            frostDp = -2f,
            tint = -0.1f,
            edgeLight = -0.5f
        ).coerced()
        assertEquals(0f, belowLiquid.refraction, 0.0001f)
        assertEquals(0.5f, belowLiquid.lensDepth, 0.0001f)
        assertEquals(0f, belowLiquid.fringe, 0.0001f)
        assertEquals(0f, belowLiquid.frostDp, 0.0001f)
        assertEquals(0f, belowLiquid.tint, 0.0001f)
        assertEquals(0f, belowLiquid.edgeLight, 0.0001f)

        val aboveLiquid = LiquidTuning(
            refraction = 5f,
            lensDepth = 3f,
            fringe = 4f,
            frostDp = 20f,
            tint = 1f,
            edgeLight = 2.5f
        ).coerced()
        assertEquals(2f, aboveLiquid.refraction, 0.0001f)
        assertEquals(2f, aboveLiquid.lensDepth, 0.0001f)
        assertEquals(2f, aboveLiquid.fringe, 0.0001f)
        assertEquals(12f, aboveLiquid.frostDp, 0.0001f)
        assertEquals(0.4f, aboveLiquid.tint, 0.0001f)
        assertEquals(1.5f, aboveLiquid.edgeLight, 0.0001f)

        val belowFrost = FrostTuning(
            blur = 0.1f,
            tint = 0.2f,
            edgeLight = -1f
        ).coerced()
        assertEquals(0.5f, belowFrost.blur, 0.0001f)
        assertEquals(0.5f, belowFrost.tint, 0.0001f)
        assertEquals(0f, belowFrost.edgeLight, 0.0001f)

        val aboveFrost = FrostTuning(
            blur = 2.5f,
            tint = 3f,
            edgeLight = 2f
        ).coerced()
        assertEquals(1.5f, aboveFrost.blur, 0.0001f)
        assertEquals(1.5f, aboveFrost.tint, 0.0001f)
        assertEquals(1.5f, aboveFrost.edgeLight, 0.0001f)
    }

    @Test
    fun defaultsAreInRange() {
        val defLiquid = LiquidTuning()
        assertEquals(defLiquid, defLiquid.coerced())
        val defFrost = FrostTuning()
        assertEquals(defFrost, defFrost.coerced())
    }
}
