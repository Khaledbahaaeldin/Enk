package app.enk.settings

import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import app.enk.design.theme.ThemeId
import app.enk.design.theme.ThemeMode
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.io.File

@RunWith(RobolectricTestRunner::class)
class SettingsStoreTest {

    @get:Rule
    val tempFolder = TemporaryFolder()

    private fun createStore(file: File = File(tempFolder.root, "settings_${System.nanoTime()}.preferences_pb")): SettingsStore {
        val dataStore = PreferenceDataStoreFactory.create(produceFile = { file })
        return SettingsStore(dataStore)
    }

    @Test
    fun defaultsMatchSpec() = runBlocking {
        val store = createStore()
        val s = store.settings.first()

        assertEquals(CorrectionMode.CleanErase, s.correction)
        assertFalse(s.autocorrect)
        assertEquals(SoundSetId.Classic, s.soundSet)
        assertFalse(s.muted)
        assertEquals(0.6f, s.bellVolume, 0.0001f)
        assertTrue(s.haptics)
        assertFalse(s.reduceEffects)
        assertEquals(ThemeId.Graphite, s.theme)
        assertEquals(ThemeMode.System, s.themeMode)
        assertEquals(25, s.warmth)
        assertEquals(80, s.intensity)
        assertEquals(55, s.beam)
        assertFalse(s.hintDismissed)
    }

    @Test
    fun themeAndModeRoundTrip() = runBlocking {
        val store = createStore()
        store.update {
            it.copy(
                theme = ThemeId.Dynamic,
                themeMode = ThemeMode.Dark
            )
        }

        val s = store.settings.first()
        assertEquals(ThemeId.Dynamic, s.theme)
        assertEquals(ThemeMode.Dark, s.themeMode)
    }

    @Test
    fun updatePersists() = runBlocking {
        val file = File(tempFolder.root, "persisted_settings.preferences_pb")
        val scope1 = CoroutineScope(Job() + Dispatchers.IO)
        val dataStore1 = PreferenceDataStoreFactory.create(scope = scope1, produceFile = { file })
        val store1 = SettingsStore(dataStore1)

        store1.update {
            it.copy(
                correction = CorrectionMode.StrikeThrough,
                autocorrect = true,
                soundSet = SoundSetId.Antique,
                muted = true,
                bellVolume = 0.8f,
                haptics = false,
                reduceEffects = true,
                theme = ThemeId.Sky,
                themeMode = ThemeMode.Light,
                warmth = 50,
                intensity = 90,
                beam = 70,
                hintDismissed = true
            )
        }
        scope1.cancel()

        val scope2 = CoroutineScope(Job() + Dispatchers.IO)
        val dataStore2 = PreferenceDataStoreFactory.create(scope = scope2, produceFile = { file })
        val store2 = SettingsStore(dataStore2)
        val s = store2.settings.first()

        assertEquals(CorrectionMode.StrikeThrough, s.correction)
        assertTrue(s.autocorrect)
        assertEquals(SoundSetId.Antique, s.soundSet)
        assertTrue(s.muted)
        assertEquals(0.8f, s.bellVolume, 0.0001f)
        assertFalse(s.haptics)
        assertTrue(s.reduceEffects)
        assertEquals(ThemeId.Sky, s.theme)
        assertEquals(ThemeMode.Light, s.themeMode)
        assertEquals(50, s.warmth)
        assertEquals(90, s.intensity)
        assertEquals(70, s.beam)
        assertTrue(s.hintDismissed)
        scope2.cancel()
    }

    @Test
    fun bellVolumeClampedTo0to1() = runBlocking {
        val storeLow = createStore()
        storeLow.update { it.copy(bellVolume = -0.5f) }
        assertEquals(0f, storeLow.settings.first().bellVolume, 0.0001f)

        val storeHigh = createStore()
        storeHigh.update { it.copy(bellVolume = 1.8f) }
        assertEquals(1f, storeHigh.settings.first().bellVolume, 0.0001f)
    }

    @Test
    fun sliderValuesClampedToRanges() = runBlocking {
        // Below minimums: warmth < 0, intensity < 20, beam < 30
        val storeLow = createStore()
        storeLow.update {
            it.copy(
                warmth = -10,
                intensity = 10,
                beam = 15
            )
        }
        val s1 = storeLow.settings.first()
        assertEquals(0, s1.warmth)
        assertEquals(20, s1.intensity)
        assertEquals(30, s1.beam)

        // Above maximums: all > 100
        val storeHigh = createStore()
        storeHigh.update {
            it.copy(
                warmth = 150,
                intensity = 200,
                beam = 110
            )
        }
        val s2 = storeHigh.settings.first()
        assertEquals(100, s2.warmth)
        assertEquals(100, s2.intensity)
        assertEquals(100, s2.beam)
    }

    @Test
    fun unknownEnumStringsFallbackToDefault() = runBlocking {
        val file = File(tempFolder.root, "corrupted_enums.preferences_pb")
        val dataStore = PreferenceDataStoreFactory.create(produceFile = { file })

        dataStore.edit { prefs ->
            prefs[stringPreferencesKey("correction")] = "NonExistentCorrection"
            prefs[stringPreferencesKey("sound_set")] = "AlienTypewriter"
            prefs[stringPreferencesKey("theme")] = "RainbowTheme"
            prefs[stringPreferencesKey("theme_mode")] = "Midnight"
        }

        val store = SettingsStore(dataStore)
        val s = store.settings.first()

        assertEquals(CorrectionMode.CleanErase, s.correction)
        assertEquals(SoundSetId.Classic, s.soundSet)
        assertEquals(ThemeId.Graphite, s.theme)
        assertEquals(ThemeMode.System, s.themeMode)
    }

    @Test
    fun nanAndInfinityBellVolumeHandledGracefully() = runBlocking {
        val store = createStore()

        store.update { it.copy(bellVolume = Float.NaN) }
        assertEquals(0.6f, store.settings.first().bellVolume, 0.0001f)

        val storePos = createStore()
        storePos.update { it.copy(bellVolume = Float.POSITIVE_INFINITY) }
        val posInf = storePos.settings.first().bellVolume
        assertTrue(posInf in 0f..1f)

        val storeNeg = createStore()
        storeNeg.update { it.copy(bellVolume = Float.NEGATIVE_INFINITY) }
        val negInf = storeNeg.settings.first().bellVolume
        assertTrue(negInf in 0f..1f)
    }

    @Test
    fun corruptedValuesClampedOnRead() = runBlocking {
        val file = File(tempFolder.root, "corrupted_values.preferences_pb")
        val dataStore = PreferenceDataStoreFactory.create(produceFile = { file })

        dataStore.edit { prefs ->
            prefs[intPreferencesKey("warmth")] = -999
            prefs[intPreferencesKey("intensity")] = 5
            prefs[intPreferencesKey("beam")] = 999
            prefs[floatPreferencesKey("bell_volume")] = 4.5f
        }

        val store = SettingsStore(dataStore)
        val s = store.settings.first()

        assertEquals(0, s.warmth)
        assertEquals(20, s.intensity)
        assertEquals(100, s.beam)
        assertEquals(1.0f, s.bellVolume, 0.0001f)
    }
}
