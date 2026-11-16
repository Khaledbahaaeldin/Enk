package app.enk.design.glass

import androidx.compose.ui.graphics.Color
import app.enk.design.effects.LiquidTuning

enum class RimLight { Ambient, Directional, Plain }

data class LiquidSpec(
    val blurDp: Float,
    val lensHeightDp: Float,
    val lensAmountDp: Float,
    val fringe: Boolean,
    val depth: Boolean,
    val rim: RimLight,
    val vibrancy: Boolean = true,
    val shadowDp: Float = 0f,
    val innerShadowDp: Float = 0f,
    /** Alpha of the theme's glass colour laid over the lens at the default Tint (keeps text legible). */
    val fill: Float = 0.4f,
    /** Alpha of the soft drop shadow under the surface. */
    val shadowAlpha: Float = 0.05f
)

object LiquidSpecs {   // values from the Catalog sources
    val Button     = LiquidSpec(2f, 12f, 24f, fringe = false, depth = false, rim = RimLight.Directional, fill = 0.35f)
    val Bar        = LiquidSpec(8f, 12f, 24f, fringe = false, depth = false, rim = RimLight.Directional, fill = 0.40f)
    val BarLens    = LiquidSpec(0f, 10f, 14f, fringe = true,  depth = false, rim = RimLight.Directional, shadowDp = 24f, innerShadowDp = 8f, fill = 0.30f)   // pressed values
    val Thumb      = LiquidSpec(0f, 10f, 14f, fringe = true,  depth = false, rim = RimLight.Ambient, shadowDp = 4f, innerShadowDp = 4f, fill = 0.30f)        // pressed values
    val Island     = LiquidSpec(3f, 16f, 32f, fringe = true,  depth = true,  rim = RimLight.Ambient, shadowDp = 18f, fill = 0.50f, shadowAlpha = 0.10f)
    val Panel      = LiquidSpec(8f, 20f, 40f, fringe = true,  depth = true,  rim = RimLight.Ambient, shadowDp = 24f, fill = 0.55f, shadowAlpha = 0.14f)   // menu, alert, lamp panel, sheet
    val Tray       = LiquidSpec(8f, 0f, 0f,   fringe = false, depth = false, rim = RimLight.Plain, fill = 0.50f)        // keyboard tray: no lens (cost)

    fun balanced(spec: LiquidSpec): LiquidSpec = spec.copy(
        lensHeightDp = spec.lensHeightDp * 0.6f,
        lensAmountDp = spec.lensAmountDp * 0.6f,
        fringe = false,
        depth = false,
        rim = RimLight.Plain
    )

    fun tuned(spec: LiquidSpec, t: LiquidTuning): LiquidSpec = spec.copy(
        blurDp = spec.blurDp + t.frostDp,
        lensHeightDp = spec.lensHeightDp * t.lensDepth,
        lensAmountDp = spec.lensAmountDp * t.refraction,
        fringe = if (t.fringe > 0f) spec.fringe else false
    )
}

/** The surface fill drawn over a liquid lens: the theme glass colour at the spec's fill alpha, scaled by Tint. */
object LiquidSurfaceFill {
    /** The Tint slider's default; at this value a surface gets exactly its spec's fill. */
    const val DEFAULT_TINT = 0.08f
    const val MAX_ALPHA = 0.95f

    fun alpha(spec: LiquidSpec, t: LiquidTuning): Float =
        (spec.fill * (t.tint / DEFAULT_TINT)).coerceIn(0f, MAX_ALPHA)

    fun color(glass: Color, spec: LiquidSpec, t: LiquidTuning): Color =
        glass.copy(alpha = alpha(spec, t))
}
