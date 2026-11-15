package app.enk.settings

import android.content.Context
import app.enk.design.effects.EffectsLevel
import app.enk.design.effects.ResolvedEffects
import app.enk.design.effects.resolveEffects
import app.enk.design.isReducedMotion

fun reduceMotion(effects: ResolvedEffects): Boolean = effects == ResolvedEffects.Light

fun reduceMotion(context: Context, settings: Settings): Boolean {
    val env = readEffectsEnvironment(context)
    return resolveEffects(settings.effects, env) == ResolvedEffects.Light
}

fun reduceMotion(effectsLevel: EffectsLevel, isReducedMotion: Boolean): Boolean {
    return effectsLevel == EffectsLevel.Light || isReducedMotion
}

fun reduceMotion(settingsReduceEffects: Boolean, animatorDurationScale: Float): Boolean {
    return settingsReduceEffects || animatorDurationScale == 0f
}
