package app.enk.design.glass

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class LiquidFieldMathTest {

    @Test
    fun roundRectDistance() {
        val capsule = LiquidShape(
            cx = 50f,
            cy = 20f,
            halfW = 50f,
            halfH = 20f,
            radius = 20f
        )
        // Centre of a 100 x 40 capsule
        val centerDist = LiquidFieldMath.sdRoundRect(50f, 20f, capsule)
        assertEquals(-20f, centerDist, 0.001f)

        // Point 5 px outside its right end (x = 100 + 5 = 105, y = 20)
        val outsideDist = LiquidFieldMath.sdRoundRect(105f, 20f, capsule)
        assertEquals(5f, outsideDist, 0.001f)
    }

    @Test
    fun smoothMinIsMinFarApart() {
        val result = LiquidFieldMath.smoothMin(0f, 50f, 24f)
        assertEquals(0f, result, 0.001f)
    }

    @Test
    fun smoothMinBulgesWhenClose() {
        val result = LiquidFieldMath.smoothMin(2f, 3f, 24f)
        assertTrue("smoothMin should bulge below the min of 2f and 3f", result < 2f)
    }

    @Test
    fun fieldOfOneShapeIsItsDistance() {
        val shape = LiquidShape(100f, 100f, 40f, 30f, 15f)
        val x = 120f
        val y = 110f
        val expected = LiquidFieldMath.sdRoundRect(x, y, shape)
        val actual = LiquidFieldMath.field(x, y, listOf(shape), 24f)
        assertEquals(expected, actual, 0.001f)
    }

    @Test
    fun gapOfTouchingShapesIsZero() {
        // Two 100x40 capsules: A from x=0 to 100, B touching A from x=100 to 200
        val a = LiquidShape(cx = 50f, cy = 20f, halfW = 50f, halfH = 20f, radius = 20f)
        val b = LiquidShape(cx = 150f, cy = 20f, halfW = 50f, halfH = 20f, radius = 20f)
        assertEquals(0f, LiquidFieldMath.gap(a, b), 0.001f)

        // Overlapping capsules
        val c = LiquidShape(cx = 140f, cy = 20f, halfW = 50f, halfH = 20f, radius = 20f)
        assertEquals(0f, LiquidFieldMath.gap(a, c), 0.001f)
    }

    @Test
    fun shouldMergeAt23NotAt25() {
        val a = LiquidShape(cx = 50f, cy = 20f, halfW = 50f, halfH = 20f, radius = 20f) // right edge at x=100

        // 23 px gap: left edge at x=123 -> cx = 123 + 50 = 173
        val b23 = LiquidShape(cx = 173f, cy = 20f, halfW = 50f, halfH = 20f, radius = 20f)
        assertTrue("Gap of 23 px should merge with mergeDistance 24", LiquidFieldMath.shouldMerge(listOf(a, b23), 24f))

        // 25 px gap: left edge at x=125 -> cx = 125 + 50 = 175
        val b25 = LiquidShape(cx = 175f, cy = 20f, halfW = 50f, halfH = 20f, radius = 20f)
        assertFalse("Gap of 25 px should not merge with mergeDistance 24", LiquidFieldMath.shouldMerge(listOf(a, b25), 24f))
    }

    @Test
    fun shaderStringHasNoTodo() {
        assertFalse("LiquidFieldShader must have no TODO", LiquidFieldShader.contains("TODO"))
        assertTrue("Must declare uniform shader content", LiquidFieldShader.contains("uniform shader content;"))
        assertTrue("Must declare uniform shapes", LiquidFieldShader.contains("uniform float4 shapes[6];"))
        assertTrue("Must declare uniform radii", LiquidFieldShader.contains("uniform float radii[6];"))
        assertTrue("Must declare uniform count", LiquidFieldShader.contains("uniform int count;"))
        assertTrue("Must declare uniform k", LiquidFieldShader.contains("uniform float k;"))
        assertTrue("Must declare uniform refractionHeight", LiquidFieldShader.contains("uniform float refractionHeight;"))
        assertTrue("Must declare uniform refractionAmount", LiquidFieldShader.contains("uniform float refractionAmount;"))
        assertTrue("Must declare uniform depthEffect", LiquidFieldShader.contains("uniform float depthEffect;"))
        assertTrue("Must declare uniform chromaticAberration", LiquidFieldShader.contains("uniform float chromaticAberration;"))
        assertTrue("Must declare uniform tint", LiquidFieldShader.contains("uniform float4 tint;"))
        assertTrue("Must declare uniform edgeLight", LiquidFieldShader.contains("uniform float edgeLight;"))
        assertTrue("Must declare uniform lightAngle", LiquidFieldShader.contains("uniform float lightAngle;"))
    }
}
