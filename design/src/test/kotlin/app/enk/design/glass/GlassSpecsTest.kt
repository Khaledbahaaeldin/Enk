package app.enk.design.glass

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class GlassSpecsTest {

    private val allSpecs = listOf(
        GlassSpecs.NavBody,
        GlassSpecs.NavLens,
        GlassSpecs.SegmentedBody,
        GlassSpecs.SegmentedLens,
        GlassSpecs.ToggleTrack,
        GlassSpecs.ToggleBead,
        GlassSpecs.SliderTrack,
        GlassSpecs.SliderThumb,
        GlassSpecs.Button,
        GlassSpecs.Field,
        GlassSpecs.TopBarButton,
        GlassSpecs.Floating,
        GlassSpecs.Toast,
        GlassSpecs.KeyboardTray
    )

    @Test
    fun blurAndSaturationPositive() {
        for (spec in allSpecs) {
            assertTrue("Blur must be positive: ${spec.blurDp}", spec.blurDp > 0f)
            assertTrue("Saturation must be positive: ${spec.saturation}", spec.saturation > 0f)
        }
    }

    @Test
    fun lensHeightNeverExceedsLensAmount() {
        for (spec in allSpecs) {
            assertTrue(
                "lensHeightDp (${spec.lensHeightDp}) must be <= lensAmountDp (${spec.lensAmountDp})",
                spec.lensHeightDp <= spec.lensAmountDp
            )
        }
    }

    @Test
    fun exactValuesOfEverySpec() {
        assertEquals(GlassSpec(20f, 1.5f, GlassTint.Regular, GlassShadowKind.Regular), GlassSpecs.NavBody)
        assertEquals(GlassSpec(1f, 1.5f, GlassTint.Lens, GlassShadowKind.None, 22f, 46f, fringe = true, depth = true, highlightWidthDp = 1f), GlassSpecs.NavLens)
        assertEquals(GlassSpec(20f, 1.5f, GlassTint.Regular, GlassShadowKind.Small), GlassSpecs.SegmentedBody)
        assertEquals(GlassSpec(1f, 1.5f, GlassTint.Lens, GlassShadowKind.None, 14f, 30f, fringe = true, depth = true, highlightWidthDp = 1f), GlassSpecs.SegmentedLens)
        assertEquals(GlassSpec(14f, 1.5f, GlassTint.Regular, GlassShadowKind.None), GlassSpecs.ToggleTrack)
        assertEquals(GlassSpec(1f, 1.5f, GlassTint.Lens, GlassShadowKind.Small, 14f, 28f, fringe = true, depth = true, highlightWidthDp = 1f), GlassSpecs.ToggleBead)
        assertEquals(GlassSpec(14f, 1.5f, GlassTint.Regular, GlassShadowKind.None), GlassSpecs.SliderTrack)
        assertEquals(GlassSpec(1f, 1.5f, GlassTint.Lens, GlassShadowKind.Small, 16f, 34f, fringe = true, depth = true, highlightWidthDp = 1f), GlassSpecs.SliderThumb)
        assertEquals(GlassSpec(14f, 1.5f, GlassTint.Regular, GlassShadowKind.Small, 14f, 26f), GlassSpecs.Button)
        assertEquals(GlassSpec(20f, 1.5f, GlassTint.Regular, GlassShadowKind.None, 12f, 20f), GlassSpecs.Field)
        assertEquals(GlassSpec(14f, 1.5f, GlassTint.Regular, GlassShadowKind.None, 10f, 16f), GlassSpecs.TopBarButton)
        assertEquals(GlassSpec(32f, 1.6f, GlassTint.Thick, GlassShadowKind.Thick, 18f, 34f), GlassSpecs.Floating)
        assertEquals(GlassSpec(24f, 1.5f, GlassTint.Thick, GlassShadowKind.Small), GlassSpecs.Toast)
        assertEquals(GlassSpec(32f, 1.6f, GlassTint.Thick, GlassShadowKind.None), GlassSpecs.KeyboardTray)
    }
}
