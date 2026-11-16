package app.enk.design.effects

import androidx.compose.runtime.compositionLocalOf

enum class GlassLook { Liquid, Frost }

fun resolveGlassLook(choice: GlassLook, sdkInt: Int): GlassLook =
    if (sdkInt < 33) GlassLook.Frost else choice

data class LiquidTuning(
    val refraction: Float = 1f,   // 0f..2f, multiplies lens amount
    val lensDepth: Float = 1f,    // 0.5f..2f, multiplies lens height
    val fringe: Float = 1f,       // 0f..2f, 0 turns colour fringe off
    val frostDp: Float = 0f,      // 0f..12f, extra blur added to the recipe's own
    val tint: Float = 0.08f,      // 0f..0.4f, surface fill alpha
    val edgeLight: Float = 1f     // 0f..1.5f, multiplies rim light alpha
) {
    fun coerced(): LiquidTuning = LiquidTuning(
        refraction = refraction.coerceIn(0f, 2f),
        lensDepth = lensDepth.coerceIn(0.5f, 2f),
        fringe = fringe.coerceIn(0f, 2f),
        frostDp = frostDp.coerceIn(0f, 12f),
        tint = tint.coerceIn(0f, 0.4f),
        edgeLight = edgeLight.coerceIn(0f, 1.5f)
    )
}

data class FrostTuning(
    val blur: Float = 1f,         // 0.5f..1.5f, multiplies the spec blur
    val tint: Float = 1f,         // 0.5f..1.5f, multiplies the tint alpha
    val edgeLight: Float = 1f     // 0f..1.5f
) {
    fun coerced(): FrostTuning = FrostTuning(
        blur = blur.coerceIn(0.5f, 1.5f),
        tint = tint.coerceIn(0.5f, 1.5f),
        edgeLight = edgeLight.coerceIn(0f, 1.5f)
    )
}

val LocalGlassLook = compositionLocalOf { GlassLook.Liquid }
val LocalLiquidTuning = compositionLocalOf { LiquidTuning() }
val LocalFrostTuning = compositionLocalOf { FrostTuning() }
