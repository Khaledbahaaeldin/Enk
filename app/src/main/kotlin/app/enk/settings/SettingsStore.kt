package app.enk.settings

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
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
            prefs[Keys.REDUCE_EFFECTS] = updated.reduceEffects
            prefs[Keys.THEME] = updated.theme.name
            prefs[Keys.THEME_MODE] = updated.themeMode.name
            prefs[Keys.WARMTH] = updated.warmth
            prefs[Keys.INTENSITY] = updated.intensity
            prefs[Keys.BEAM] = updated.beam
            prefs[Keys.HINT_DISMISSED] = updated.hintDismissed
        }
    }

    private object Keys {
        val CORRECTION = stringPreferencesKey("correction")
        val AUTOCORRECT = booleanPreferencesKey("autocorrect")
        val SOUND_SET = stringPreferencesKey("sound_set")
        val MUTED = booleanPreferencesKey("muted")
        val BELL_VOLUME = floatPreferencesKey("bell_volume")
        val HAPTICS = booleanPreferencesKey("haptics")
        val REDUCE_EFFECTS = booleanPreferencesKey("reduce_effects")
        val THEME = stringPreferencesKey("theme")
        val THEME_MODE = stringPreferencesKey("theme_mode")
        val WARMTH = intPreferencesKey("warmth")
        val INTENSITY = intPreferencesKey("intensity")
        val BEAM = intPreferencesKey("beam")
        val HINT_DISMISSED = booleanPreferencesKey("hint_dismissed")
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
                beam = sanitizeBeam(s.beam)
            )
        }

        private fun mapFromPreferences(prefs: Preferences): Settings {
            return Settings(
                correction = safeEnumValueOf(prefs[Keys.CORRECTION], CorrectionMode.CleanErase),
                autocorrect = prefs[Keys.AUTOCORRECT] ?: false,
                soundSet = safeEnumValueOf(prefs[Keys.SOUND_SET], SoundSetId.Classic),
                muted = prefs[Keys.MUTED] ?: false,
                bellVolume = sanitizeBellVolume(prefs[Keys.BELL_VOLUME]),
                haptics = prefs[Keys.HAPTICS] ?: true,
                reduceEffects = prefs[Keys.REDUCE_EFFECTS] ?: false,
                theme = safeEnumValueOf(prefs[Keys.THEME], ThemeId.Graphite),
                themeMode = safeEnumValueOf(prefs[Keys.THEME_MODE], ThemeMode.System),
                warmth = sanitizeWarmth(prefs[Keys.WARMTH]),
                intensity = sanitizeIntensity(prefs[Keys.INTENSITY]),
                beam = sanitizeBeam(prefs[Keys.BEAM]),
                hintDismissed = prefs[Keys.HINT_DISMISSED] ?: false
            )
        }
    }
}
