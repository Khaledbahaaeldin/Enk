package app.enk.design.glass

import org.junit.Assert.assertEquals
import org.junit.Test

class GlassLensMathTest {

    @Test
    fun coerceLensBounds() {
        val spec = GlassSpec(1f, 1.5f, GlassTint.Lens, GlassShadowKind.None, 22f, 46f)

        // Height above corner radius
        val (h1, a1) = GlassLensMath.coerceLens(spec, widthDp = 100f, heightDp = 100f, minCornerDp = 10f)
        assertEquals(10f, h1, 0.001f)
        assertEquals(46f, a1, 0.001f)

        // Amount above smaller side
        val (h2, a2) = GlassLensMath.coerceLens(spec, widthDp = 30f, heightDp = 100f, minCornerDp = 30f)
        assertEquals(22f, h2, 0.001f)
        assertEquals(30f, a2, 0.001f)

        // Zero / Negative / NaN
        assertEquals(Pair(0f, 0f), GlassLensMath.coerceLens(spec, 0f, 100f, 10f))
        assertEquals(Pair(0f, 0f), GlassLensMath.coerceLens(spec, 100f, -5f, 10f))
        assertEquals(Pair(0f, 0f), GlassLensMath.coerceLens(spec, 100f, 100f, -1f))
        assertEquals(Pair(0f, 0f), GlassLensMath.coerceLens(spec, Float.NaN, 100f, 10f))
        assertEquals(Pair(0f, 0f), GlassLensMath.coerceLens(spec, 100f, Float.NaN, 10f))
        assertEquals(Pair(0f, 0f), GlassLensMath.coerceLens(spec, 100f, 100f, Float.NaN))

        // Circle shape
        val (hc, ac) = GlassLensMath.coerceLens(spec, widthDp = 40f, heightDp = 40f, minCornerDp = 20f)
        assertEquals(20f, hc, 0.001f)
        assertEquals(40f, ac, 0.001f)
    }

    @Test
    fun stretchCalculations() {
        // At 0 velocity
        val (s0x, s0y) = GlassLensMath.stretch(0f)
        assertEquals(1f, s0x, 0.001f)
        assertEquals(1f, s0y, 0.001f)

        // At 3000 velocity (k = 3000/6000 = 0.5 coerced to 0.14)
        val (s3x, s3y) = GlassLensMath.stretch(3000f)
        assertEquals(1.14f, s3x, 0.001f)
        assertEquals(0.93f, s3y, 0.001f)

        // At 20,000 velocity
        val (s20x, s20y) = GlassLensMath.stretch(20000f)
        assertEquals(1.14f, s20x, 0.001f)
        assertEquals(0.93f, s20y, 0.001f)

        // At -20,000 velocity
        val (sn20x, sn20y) = GlassLensMath.stretch(-20000f)
        assertEquals(1.14f, sn20x, 0.001f)
        assertEquals(0.93f, sn20y, 0.001f)

        // NaN and Infinity
        assertEquals(Pair(1f, 1f), GlassLensMath.stretch(Float.NaN))
        assertEquals(Pair(1f, 1f), GlassLensMath.stretch(Float.POSITIVE_INFINITY))
        assertEquals(Pair(1f, 1f), GlassLensMath.stretch(Float.NEGATIVE_INFINITY))
    }

    @Test
    fun specularAngleDegDirections() {
        // East (dx > 0, dy = 0)
        assertEquals(0f, GlassLensMath.specularAngleDeg(0f, 0f, 10f, 0f), 0.001f)

        // South (dx = 0, dy > 0)
        assertEquals(90f, GlassLensMath.specularAngleDeg(0f, 0f, 0f, 10f), 0.001f)

        // West (dx < 0, dy = 0)
        assertEquals(180f, GlassLensMath.specularAngleDeg(0f, 0f, -10f, 0f), 0.001f)

        // North (dx = 0, dy < 0)
        assertEquals(270f, GlassLensMath.specularAngleDeg(0f, 0f, 0f, -10f), 0.001f)

        // Equal points -> 45f
        assertEquals(45f, GlassLensMath.specularAngleDeg(10f, 10f, 10f, 10f), 0.001f)

        // NaN -> 45f
        assertEquals(45f, GlassLensMath.specularAngleDeg(Float.NaN, 0f, 10f, 0f), 0.001f)
        assertEquals(45f, GlassLensMath.specularAngleDeg(0f, Float.NaN, 10f, 0f), 0.001f)
        assertEquals(45f, GlassLensMath.specularAngleDeg(0f, 0f, Float.NaN, 0f), 0.001f)
        assertEquals(45f, GlassLensMath.specularAngleDeg(0f, 0f, 10f, Float.NaN), 0.001f)
    }

    @Test
    fun pressTargetState() {
        assertEquals(1.08f, GlassLensMath.pressTarget(true, GlassLensMath.NAV_PRESS), 0.001f)
        assertEquals(1f, GlassLensMath.pressTarget(false, GlassLensMath.NAV_PRESS), 0.001f)
        assertEquals(1.16f, GlassLensMath.pressTarget(true, GlassLensMath.BEAD_PRESS), 0.001f)
        assertEquals(1f, GlassLensMath.pressTarget(false, GlassLensMath.BEAD_PRESS), 0.001f)
    }
}
