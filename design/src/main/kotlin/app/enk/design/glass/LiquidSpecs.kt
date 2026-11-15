package app.enk.design.glass

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
    val innerShadowDp: Float = 0f
)

object LiquidSpecs {   // values from the Catalog sources
    val Button     = LiquidSpec(2f, 12f, 24f, fringe = false, depth = false, rim = RimLight.Directional)
    val Bar        = LiquidSpec(8f, 12f, 24f, fringe = false, depth = false, rim = RimLight.Directional)
    val BarLens    = LiquidSpec(0f, 10f, 14f, fringe = true,  depth = false, rim = RimLight.Directional, shadowDp = 24f, innerShadowDp = 8f)   // pressed values
    val Thumb      = LiquidSpec(0f, 10f, 14f, fringe = true,  depth = false, rim = RimLight.Ambient, shadowDp = 4f, innerShadowDp = 4f)        // pressed values
    val Island     = LiquidSpec(3f, 16f, 32f, fringe = true,  depth = true,  rim = RimLight.Ambient, shadowDp = 18f)
    val Panel      = LiquidSpec(6f, 20f, 40f, fringe = true,  depth = true,  rim = RimLight.Ambient, shadowDp = 24f)   // menu, alert, lamp panel, sheet
    val Tray       = LiquidSpec(8f, 0f, 0f,   fringe = false, depth = false, rim = RimLight.Plain)                     // keyboard tray: no lens (cost)

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
