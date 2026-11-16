package app.enk.settings

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import app.enk.design.effects.EffectsLevel
import app.enk.design.effects.FrostTuning
import app.enk.design.effects.GlassLook
import app.enk.design.effects.LiquidTuning
import app.enk.design.theme.ThemeId
import app.enk.design.theme.ThemeMode
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import java.io.IOException
import kotlinx.coroutines.flow.map

class SettingsStore(private val dataStore: DataStore<Preferences>) {

    // An unreadable or corrupted file (IOException, CorruptionException) falls back to the defaults.
    val settings: Flow<Settings> = dataStore.data
        .catch { if (it is IOException) emit(emptyPreferences()) else throw it }
        .map { prefs -> mapFromPreferences(prefs) }

    suspend fun update(f: (Settings) -> Settings) {
        dataStore.edit { prefs ->
            val current = mapFromPreferences(prefs)
            val updated = sanitize(f(current))

            prefs[Keys.CORRECTION] = updated.correction.name
            prefs[Keys.AUTOCORRECT] = updated.autocorrect
            prefs[Keys.SOUND_SET] = updated.soundSet.name
            prefs[Keys.MUTED] = updated.muted
            prefs[Keys.BELL_VOLUME] = updated.bellVolume
            prefs[Keys.HAPTICS] = updated.haptics
            prefs[Keys.EFFECTS] = updated.effects.name
            prefs[Keys.GLASS_LOOK] = updated.glassLook.name
            prefs[Keys.LIQUID_REFRACTION] = updated.liquidTuning.refraction
            prefs[Keys.LIQUID_LENS_DEPTH] = updated.liquidTuning.lensDepth
            prefs[Keys.LIQUID_FRINGE] = updated.liquidTuning.fringe
            prefs[Keys.LIQUID_FROST_DP] = updated.liquidTuning.frostDp
            prefs[Keys.LIQUID_TINT] = updated.liquidTuning.tint
            prefs[Keys.LIQUID_EDGE_LIGHT] = updated.liquidTuning.edgeLight
            prefs[Keys.FROST_BLUR] = updated.frostTuning.blur
            prefs[Keys.FROST_TINT] = updated.frostTuning.tint
            prefs[Keys.FROST_EDGE_LIGHT] = updated.frostTuning.edgeLight
            prefs[Keys.THEME] = updated.theme.name
            prefs[Keys.THEME_MODE] = updated.themeMode.name
            prefs[Keys.WARMTH] = updated.warmth
            prefs[Keys.INTENSITY] = updated.intensity
            prefs[Keys.BEAM] = updated.beam
            prefs[Keys.HINT_DISMISSED] = updated.hintDismissed
            prefs[Keys.DARK_PAPER] = updated.darkPaper.name
            prefs[Keys.KEYBOARD_MODE] = updated.keyboard.name
        }
    }

    private object Keys {
        val CORRECTION = stringPreferencesKey("correction")
        val AUTOCORRECT = booleanPreferencesKey("autocorrect")
        val SOUND_SET = stringPreferencesKey("sound_set")
        val MUTED = booleanPreferencesKey("muted")
        val BELL_VOLUME = floatPreferencesKey("bell_volume")
        val HAPTICS = booleanPreferencesKey("haptics")
        val EFFECTS = stringPreferencesKey("effects")
        val GLASS_LOOK = stringPreferencesKey("glass_look")
        val LIQUID_REFRACTION = floatPreferencesKey("liquid_refraction")
        val LIQUID_LENS_DEPTH = floatPreferencesKey("liquid_lens_depth")
        val LIQUID_FRINGE = floatPreferencesKey("liquid_fringe")
        val LIQUID_FROST_DP = floatPreferencesKey("liquid_frost_dp")
        val LIQUID_TINT = floatPreferencesKey("liquid_tint")
        val LIQUID_EDGE_LIGHT = floatPreferencesKey("liquid_edge_light")
        val FROST_BLUR = floatPreferencesKey("frost_blur")
        val FROST_TINT = floatPreferencesKey("frost_tint")
        val FROST_EDGE_LIGHT = floatPreferencesKey("frost_edge_light")
        val REDUCE_EFFECTS = booleanPreferencesKey("reduce_effects")
        val THEME = stringPreferencesKey("theme")
        val THEME_MODE = stringPreferencesKey("theme_mode")
        val WARMTH = intPreferencesKey("warmth")
        val INTENSITY = intPreferencesKey("intensity")
        val BEAM = intPreferencesKey("beam")
        val HINT_DISMISSED = booleanPreferencesKey("hint_dismissed")
        val DARK_PAPER = stringPreferencesKey("dark_paper")
        val KEYBOARD_MODE = stringPreferencesKey("keyboard_mode")
    }

    companion object {
        private inline fun <reified T : Enum<T>> safeEnumValueOf(name: String?, default: T): T {
            if (name == null) return default
            return enumValues<T>().firstOrNull { it.name == name } ?: default
        }

        private fun sanitizeBellVolume(raw: Float?): Float {
            if (raw == null) return 0.6f
            if (raw.isNaN() || raw.isInfinite()) return 0.6f
            return raw.coerceIn(0f, 1f)
        }

        private fun sanitizeWarmth(raw: Int?): Int = (raw ?: 25).coerceIn(0, 100)
        private fun sanitizeIntensity(raw: Int?): Int = (raw ?: 80).coerceIn(20, 100)
        private fun sanitizeBeam(raw: Int?): Int = (raw ?: 55).coerceIn(30, 100)

        private fun sanitize(s: Settings): Settings {
            return s.copy(
                bellVolume = sanitizeBellVolume(s.bellVolume),
                warmth = sanitizeWarmth(s.warmth),
                intensity = sanitizeIntensity(s.intensity),
                beam = sanitizeBeam(s.beam),
                liquidTuning = s.liquidTuning.coerced(),
                frostTuning = s.frostTuning.coerced()
            )
        }

        private fun mapFromPreferences(prefs: Preferences): Settings {
            val resolvedEffects = if (prefs.contains(Keys.EFFECTS)) {
                val raw = prefs[Keys.EFFECTS]
                enumValues<EffectsLevel>().firstOrNull { it.name == raw } ?: EffectsLevel.Auto
            } else if (prefs[Keys.REDUCE_EFFECTS] == true) {
                EffectsLevel.Light
            } else {
                EffectsLevel.Auto
            }

            val glassLook = safeEnumValueOf(prefs[Keys.GLASS_LOOK], GlassLook.Liquid)
            val liquidTuning = LiquidTuning(
                refraction = prefs[Keys.LIQUID_REFRACTION] ?: 1f,
                lensDepth = prefs[Keys.LIQUID_LENS_DEPTH] ?: 1f,
                fringe = prefs[Keys.LIQUID_FRINGE] ?: 1f,
                frostDp = prefs[Keys.LIQUID_FROST_DP] ?: 0f,
                tint = prefs[Keys.LIQUID_TINT] ?: 0.08f,
                edgeLight = prefs[Keys.LIQUID_EDGE_LIGHT] ?: 1f
            ).coerced()
            val frostTuning = FrostTuning(
                blur = prefs[Keys.FROST_BLUR] ?: 1f,
                tint = prefs[Keys.FROST_TINT] ?: 1f,
                edgeLight = prefs[Keys.FROST_EDGE_LIGHT] ?: 1f
            ).coerced()

            return Settings(
                correction = safeEnumValueOf(prefs[Keys.CORRECTION], CorrectionMode.CleanErase),
                autocorrect = prefs[Keys.AUTOCORRECT] ?: false,
                soundSet = safeEnumValueOf(prefs[Keys.SOUND_SET], SoundSetId.Classic),
                muted = prefs[Keys.MUTED] ?: false,
                bellVolume = sanitizeBellVolume(prefs[Keys.BELL_VOLUME]),
                haptics = prefs[Keys.HAPTICS] ?: true,
                effects = resolvedEffects,
                glassLook = glassLook,
                liquidTuning = liquidTuning,
                frostTuning = frostTuning,
                theme = safeEnumValueOf(prefs[Keys.THEME], ThemeId.Graphite),
                themeMode = safeEnumValueOf(prefs[Keys.THEME_MODE], ThemeMode.System),
                warmth = sanitizeWarmth(prefs[Keys.WARMTH]),
                intensity = sanitizeIntensity(prefs[Keys.INTENSITY]),
                beam = sanitizeBeam(prefs[Keys.BEAM]),
                hintDismissed = prefs[Keys.HINT_DISMISSED] ?: false,
                darkPaper = safeEnumValueOf(prefs[Keys.DARK_PAPER], DarkPaper.Ask),
                keyboard = safeEnumValueOf(prefs[Keys.KEYBOARD_MODE], KeyboardMode.Enk)
            )
        }
    }
}
