package app.enk.design.effects

import kotlin.math.max

enum class EffectsLevel { Auto, Full, Balanced, Light }

enum class ResolvedEffects { Full, Balanced, Light }

data class EffectsEnvironment(
    val sdkInt: Int,
    val systemReducedMotion: Boolean,
    val highContrastText: Boolean,
    val lowRamDevice: Boolean,
    val batterySaver: Boolean,
    val slowFrames: ResolvedEffects? = null
)

fun resolveEffects(choice: EffectsLevel, env: EffectsEnvironment): ResolvedEffects {
    // 1. env.systemReducedMotion || env.highContrastText -> Light (for every choice)
    if (env.systemReducedMotion || env.highContrastText) {
        return ResolvedEffects.Light
    }

    // 2. base = Full, Balanced, Light for those choices;
    // for Auto: Light if lowRamDevice; else Balanced if sdkInt < 33 || batterySaver; else Full.
    val base = when (choice) {
        EffectsLevel.Full -> ResolvedEffects.Full
        EffectsLevel.Balanced -> ResolvedEffects.Balanced
        EffectsLevel.Light -> ResolvedEffects.Light
        EffectsLevel.Auto -> {
            if (env.lowRamDevice) {
                ResolvedEffects.Light
            } else if (env.sdkInt < 33 || env.batterySaver) {
                ResolvedEffects.Balanced
            } else {
                ResolvedEffects.Full
            }
        }
    }

    // 3. For Auto only: if env.slowFrames != null, the result is the weaker (higher ordinal) of base and env.slowFrames
    if (choice == EffectsLevel.Auto && env.slowFrames != null) {
        return if (env.slowFrames.ordinal > base.ordinal) env.slowFrames else base
    }

    return base
}
