package app.enk.writer

import androidx.compose.runtime.Composable
import app.enk.design.effects.ResolvedEffects

@Composable
fun rememberReduceMotion(effects: ResolvedEffects): Boolean = effects == ResolvedEffects.Light

@Composable
fun rememberReduceMotion(isLight: Boolean): Boolean = isLight
