package app.enk.design.glass

import app.enk.design.effects.LiquidTuning
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class LiquidMergeRulesTest {

    private fun pill(cx: Float) = LiquidShape(cx, 20f, 50f, 20f, 20f)

    @Test
    fun aSingleShapeNeverMerges() {
        assertFalse(LiquidGroupState.resolve(true, listOf(pill(50f)), 24f, isPressed = true))
    }

    @Test
    fun zeroBlendNeverMergesOnceReleased() {
        // 10 px apart: within a 24 px reach, but the blend has shrunk the reach to nothing.
        val close = listOf(pill(50f), pill(160f))
        assertTrue(LiquidGroupState.resolve(false, close, 24f, isPressed = false))
        assertFalse(LiquidGroupState.resolve(true, close, 0f, isPressed = false))
    }

    @Test
    fun resolveIsIdempotentForEqualInputs() {
        val shapes = listOf(pill(50f), pill(190f)) // 40 px apart
        val first = LiquidGroupState.resolve(true, shapes, 24f, isPressed = true)
        val second = LiquidGroupState.resolve(first, shapes, 24f, isPressed = true)
        assertEquals(first, second)
    }

    @Test
    fun noBridgeIsDrawnAtTheMergeEdge() {
        // A bridge forms only when the gap is under half the smooth-union radius, so handing
        // back to separate surfaces at gap == k shows no neck being cut.
        val k = 24f
        val a = pill(50f)
        val b = pill(100f + k + 50f) // gap == k
        val midX = 100f + k / 2f
        assertTrue(LiquidFieldMath.field(midX, 20f, listOf(a, b), k) > 0f)
    }

    @Test
    fun velocityStretchIsSymmetricAndTopsOut() {
        val (lx, ly) = LiquidControlsMath.velocityStretch(-800f)
        val (rx, ry) = LiquidControlsMath.velocityStretch(800f)
        assertEquals(lx, rx, 0.0001f)
        assertEquals(ly, ry, 0.0001f)
        assertTrue(rx > 1f && ry < 1f)
        val (slowX, _) = LiquidControlsMath.velocityStretch(100f)
        assertTrue("a slow drag barely stretches", slowX < 1.03f)
        val (maxX, _) = LiquidControlsMath.velocityStretch(20000f)
        assertEquals(1f / 0.8f, maxX, 0.0001f)
    }

    @Test
    fun surfaceFillAtDefaultTintIsTheSpecFill() {
        val t = LiquidTuning()
        assertEquals(LiquidSpecs.Island.fill, LiquidSurfaceFill.alpha(LiquidSpecs.Island, t), 0.0001f)
        assertEquals(0f, LiquidSurfaceFill.alpha(LiquidSpecs.Panel, t.copy(tint = 0f)), 0.0001f)
        assertEquals(LiquidSurfaceFill.MAX_ALPHA, LiquidSurfaceFill.alpha(LiquidSpecs.Panel, t.copy(tint = 0.4f)), 0.0001f)
    }
}
