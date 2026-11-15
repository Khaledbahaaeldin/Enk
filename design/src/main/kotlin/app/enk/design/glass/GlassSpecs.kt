package app.enk.design.glass

import androidx.compose.ui.unit.dp
import app.enk.design.ShadowLayer
import app.enk.design.theme.ShellMaterialTokens

enum class GlassTint { Regular, Thick, Lens, None }
enum class GlassShadowKind { Regular, Thick, Small, None }

data class GlassSpec(
    val blurDp: Float,
    val saturation: Float,
    val tint: GlassTint,
    val shadow: GlassShadowKind,
    val lensHeightDp: Float = 0f,
    val lensAmountDp: Float = 0f,
    val fringe: Boolean = false,        // chromaticAberration
    val depth: Boolean = false,         // depthEffect
    val highlightWidthDp: Float = 0.5f
)

object GlassSpecs {
    val NavBody        = GlassSpec(24f, 1.5f, GlassTint.Regular, GlassShadowKind.Regular)
    val NavLens        = GlassSpec(1f, 1.5f, GlassTint.Lens, GlassShadowKind.None, 22f, 46f, fringe = true, depth = true, highlightWidthDp = 1f)
    val SegmentedBody  = GlassSpec(24f, 1.5f, GlassTint.Regular, GlassShadowKind.Small)
    val SegmentedLens  = GlassSpec(1f, 1.5f, GlassTint.Lens, GlassShadowKind.None, 14f, 30f, fringe = true, depth = true, highlightWidthDp = 1f)
    val ToggleTrack    = GlassSpec(14f, 1.5f, GlassTint.Regular, GlassShadowKind.None)
    val ToggleBead     = GlassSpec(1f, 1.5f, GlassTint.Lens, GlassShadowKind.Small, 14f, 28f, fringe = true, depth = true, highlightWidthDp = 1f)
    val SliderTrack    = GlassSpec(14f, 1.5f, GlassTint.Regular, GlassShadowKind.None)
    val SliderThumb    = GlassSpec(1f, 1.5f, GlassTint.Lens, GlassShadowKind.Small, 16f, 34f, fringe = true, depth = true, highlightWidthDp = 1f)
    val Button         = GlassSpec(14f, 1.5f, GlassTint.Regular, GlassShadowKind.Small, 14f, 26f)
    val Field          = GlassSpec(24f, 1.5f, GlassTint.Regular, GlassShadowKind.None, 12f, 20f)
    val TopBarButton   = GlassSpec(14f, 1.5f, GlassTint.Regular, GlassShadowKind.None, 10f, 16f)
    val Floating       = GlassSpec(44f, 1.6f, GlassTint.Thick, GlassShadowKind.Thick, 18f, 34f)   // menu, sheet, alert, pop-up
    val Toast          = GlassSpec(24f, 1.5f, GlassTint.Thick, GlassShadowKind.Small)
    val KeyboardTray   = GlassSpec(44f, 1.6f, GlassTint.Thick, GlassShadowKind.None)

    fun shadowLayers(kind: GlassShadowKind, material: ShellMaterialTokens): List<ShadowLayer> = when (kind) {
        GlassShadowKind.Regular -> listOf(
            ShadowLayer(0.dp, 1.dp, 2.dp, 0.dp, material.sh1),
            ShadowLayer(0.dp, 14.dp, 30.dp, (-8).dp, material.sh2)
        )
        GlassShadowKind.Thick -> listOf(
            ShadowLayer(0.dp, 2.dp, 6.dp, 0.dp, material.sh1),
            ShadowLayer(0.dp, 30.dp, 60.dp, (-12).dp, material.sh2)
        )
        GlassShadowKind.Small -> listOf(
            ShadowLayer(0.dp, 1.dp, 2.dp, 0.dp, material.sh1),
            ShadowLayer(0.dp, 6.dp, 14.dp, (-6).dp, material.sh2)
        )
        GlassShadowKind.None -> emptyList()
    }
}
