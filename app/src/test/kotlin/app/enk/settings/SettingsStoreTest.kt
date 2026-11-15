package app.enk.settings

import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import app.enk.design.effects.EffectsLevel
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
        assertEquals(EffectsLevel.Auto, s.effects)
        assertEquals(ThemeId.Graphite, s.theme)
        assertEquals(ThemeMode.System, s.themeMode)
        assertEquals(25, s.warmth)
        assertEquals(80, s.intensity)
        assertEquals(55, s.beam)
        assertFalse(s.hintDismissed)
        assertEquals(KeyboardMode.Enk, s.keyboard)
    }

    @Test
    fun keyboardDefaultsToEnkRoundTripsAndFallsBack() = runBlocking {
        val file = File(tempFolder.root, "keyboard.preferences_pb")
        val store = createStore(file)
        assertEquals(KeyboardMode.Enk, store.settings.first().keyboard)

        store.update { it.copy(keyboard = KeyboardMode.System) }
        assertEquals(KeyboardMode.System, store.settings.first().keyboard)

        val fileCorrupt = File(tempFolder.root, "corrupted_keyboard.preferences_pb")
        val dataStore = PreferenceDataStoreFactory.create(produceFile = { fileCorrupt })
        dataStore.edit { prefs ->
            prefs[stringPreferencesKey("keyboard_mode")] = "UnknownKeyboard"
        }
        val storeCorrupt = SettingsStore(dataStore)
        assertEquals(KeyboardMode.Enk, storeCorrupt.settings.first().keyboard)
    }

    @Test
    fun darkPaperStartsOpenAndRoundTrips() = runBlocking {
        val file = File(tempFolder.root, "dark_paper.preferences_pb")
        val store = createStore(file)
        assertEquals(DarkPaper.Ask, store.settings.first().darkPaper)

        store.update { it.copy(darkPaper = DarkPaper.Lamp) }
        assertEquals(DarkPaper.Lamp, store.settings.first().darkPaper)
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
                effects = EffectsLevel.Balanced,
                theme = ThemeId.Sky,
                themeMode = ThemeMode.Light,
                warmth = 50,
                intensity = 90,
                beam = 70,
                hintDismissed = true,
                keyboard = KeyboardMode.System
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
        assertEquals(EffectsLevel.Balanced, s.effects)
        assertEquals(ThemeId.Sky, s.theme)
        assertEquals(ThemeMode.Light, s.themeMode)
        assertEquals(50, s.warmth)
        assertEquals(90, s.intensity)
        assertEquals(70, s.beam)
        assertTrue(s.hintDismissed)
        assertEquals(KeyboardMode.System, s.keyboard)
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

    @Test
    fun exactReadTimeClampingWithParentSpecifiedValues() = runBlocking {
        val file = File(tempFolder.root, "parent_specified_corrupted.preferences_pb")
        val dataStore = PreferenceDataStoreFactory.create(produceFile = { file })

        dataStore.edit { prefs ->
            prefs[floatPreferencesKey("bell_volume")] = -5f
            prefs[intPreferencesKey("warmth")] = 200
            prefs[intPreferencesKey("intensity")] = 5
            prefs[intPreferencesKey("beam")] = 10
        }

        val store = SettingsStore(dataStore)
        val s = store.settings.first()

        assertEquals(0f, s.bellVolume, 0.0001f)
        assertEquals(100, s.warmth)
        assertEquals(20, s.intensity)
        assertEquals(30, s.beam)
    }

    @Test
    fun readTimeCorruptedFloatNaNAndInfinityHandled() = runBlocking {
        val fileNaN = File(tempFolder.root, "nan_float.preferences_pb")
        val dataStoreNaN = PreferenceDataStoreFactory.create(produceFile = { fileNaN })
        dataStoreNaN.edit { prefs ->
            prefs[floatPreferencesKey("bell_volume")] = Float.NaN
        }
        val storeNaN = SettingsStore(dataStoreNaN)
        assertEquals(0.6f, storeNaN.settings.first().bellVolume, 0.0001f)

        val fileInf = File(tempFolder.root, "inf_float.preferences_pb")
        val dataStoreInf = PreferenceDataStoreFactory.create(produceFile = { fileInf })
        dataStoreInf.edit { prefs ->
            prefs[floatPreferencesKey("bell_volume")] = Float.POSITIVE_INFINITY
        }
        val storeInf = SettingsStore(dataStoreInf)
        assertEquals(0.6f, storeInf.settings.first().bellVolume, 0.0001f)
    }

    @Test
    fun emptyAndUnknownEnumStringsOnReadFallbackToDefaults() = runBlocking {
        val fileInvalid = File(tempFolder.root, "invalid_enums_fallback.preferences_pb")
        val dataStoreInvalid = PreferenceDataStoreFactory.create(produceFile = { fileInvalid })

        dataStoreInvalid.edit { prefs ->
            prefs[stringPreferencesKey("correction")] = "GarbageMode"
            prefs[stringPreferencesKey("sound_set")] = "DoesNotExist"
            prefs[stringPreferencesKey("theme")] = "UnknownTheme"
            prefs[stringPreferencesKey("theme_mode")] = "CrazyMode"
        }

        val store1 = SettingsStore(dataStoreInvalid)
        val s1 = store1.settings.first()

        assertEquals(CorrectionMode.CleanErase, s1.correction)
        assertEquals(SoundSetId.Classic, s1.soundSet)
        assertEquals(ThemeId.Graphite, s1.theme)
        assertEquals(ThemeMode.System, s1.themeMode)

        // Empty strings also safely fallback to defaults on separate file
        val fileEmpty = File(tempFolder.root, "empty_enums_fallback.preferences_pb")
        val dataStoreEmpty = PreferenceDataStoreFactory.create(produceFile = { fileEmpty })

        dataStoreEmpty.edit { prefs ->
            prefs[stringPreferencesKey("correction")] = ""
            prefs[stringPreferencesKey("sound_set")] = ""
            prefs[stringPreferencesKey("theme")] = ""
            prefs[stringPreferencesKey("theme_mode")] = ""
        }
        val store2 = SettingsStore(dataStoreEmpty)
        val s2 = store2.settings.first()
        assertEquals(CorrectionMode.CleanErase, s2.correction)
        assertEquals(SoundSetId.Classic, s2.soundSet)
        assertEquals(ThemeId.Graphite, s2.theme)
        assertEquals(ThemeMode.System, s2.themeMode)
    }

    @Test
    fun booleanPreferencesRoundTripIndependently() = runBlocking {
        val storeToggleTrue = createStore()
        storeToggleTrue.update {
            it.copy(
                autocorrect = true,
                muted = true,
                haptics = false,
                hintDismissed = true
            )
        }
        val sTrue = storeToggleTrue.settings.first()
        assertTrue(sTrue.autocorrect)
        assertTrue(sTrue.muted)
        assertFalse(sTrue.haptics)
        assertTrue(sTrue.hintDismissed)

        val storeToggleFalse = createStore()
        storeToggleFalse.update {
            it.copy(
                autocorrect = false,
                muted = false,
                haptics = true,
                hintDismissed = false
            )
        }
        val sFalse = storeToggleFalse.settings.first()
        assertFalse(sFalse.autocorrect)
        assertFalse(sFalse.muted)
        assertTrue(sFalse.haptics)
        assertFalse(sFalse.hintDismissed)
    }

    @Test
    fun effectsDefaultAutoRoundTripsAndMigratesOldKeys() = runBlocking {
        // default Auto
        val storeDefault = createStore()
        assertEquals(EffectsLevel.Auto, storeDefault.settings.first().effects)

        // round trip each value
        for (level in EffectsLevel.entries) {
            val store = createStore()
            store.update { it.copy(effects = level) }
            assertEquals(level, store.settings.first().effects)
        }

        // old true -> Light
        val fileOldTrue = File(tempFolder.root, "old_true.preferences_pb")
        val dsOldTrue = PreferenceDataStoreFactory.create(produceFile = { fileOldTrue })
        dsOldTrue.edit { prefs ->
            prefs[androidx.datastore.preferences.core.booleanPreferencesKey("reduce_effects")] = true
        }
        val storeOldTrue = SettingsStore(dsOldTrue)
        assertEquals(EffectsLevel.Light, storeOldTrue.settings.first().effects)

        // old false -> Auto
        val fileOldFalse = File(tempFolder.root, "old_false.preferences_pb")
        val dsOldFalse = PreferenceDataStoreFactory.create(produceFile = { fileOldFalse })
        dsOldFalse.edit { prefs ->
            prefs[androidx.datastore.preferences.core.booleanPreferencesKey("reduce_effects")] = false
        }
        val storeOldFalse = SettingsStore(dsOldFalse)
        assertEquals(EffectsLevel.Auto, storeOldFalse.settings.first().effects)

        // unknown string -> Auto
        val fileUnknown = File(tempFolder.root, "unknown.preferences_pb")
        val dsUnknown = PreferenceDataStoreFactory.create(produceFile = { fileUnknown })
        dsUnknown.edit { prefs ->
            prefs[stringPreferencesKey("effects")] = "InvalidLevel"
        }
        val storeUnknown = SettingsStore(dsUnknown)
        assertEquals(EffectsLevel.Auto, storeUnknown.settings.first().effects)

        // both keys present -> effects wins
        val fileBoth = File(tempFolder.root, "both.preferences_pb")
        val dsBoth = PreferenceDataStoreFactory.create(produceFile = { fileBoth })
        dsBoth.edit { prefs ->
            prefs[androidx.datastore.preferences.core.booleanPreferencesKey("reduce_effects")] = true
            prefs[stringPreferencesKey("effects")] = "Balanced"
        }
        val storeBoth = SettingsStore(dsBoth)
        assertEquals(EffectsLevel.Balanced, storeBoth.settings.first().effects)
    }

    @Test
    fun allEnumValuesRoundTrip() = runBlocking {
        // Test non-default combination 1
        val store1 = createStore()
        store1.update {
            it.copy(
                correction = CorrectionMode.StrikeThrough,
                soundSet = SoundSetId.Antique,
                theme = ThemeId.Slate,
                themeMode = ThemeMode.Dark
            )
        }
        val s1 = store1.settings.first()
        assertEquals(CorrectionMode.StrikeThrough, s1.correction)
        assertEquals(SoundSetId.Antique, s1.soundSet)
        assertEquals(ThemeId.Slate, s1.theme)
        assertEquals(ThemeMode.Dark, s1.themeMode)

        // Test non-default combination 2
        val store2 = createStore()
        store2.update {
            it.copy(
                correction = CorrectionMode.CleanErase,
                soundSet = SoundSetId.Soft,
                theme = ThemeId.Sky,
                themeMode = ThemeMode.Light
            )
        }
        val s2 = store2.settings.first()
        assertEquals(CorrectionMode.CleanErase, s2.correction)
        assertEquals(SoundSetId.Soft, s2.soundSet)
        assertEquals(ThemeId.Sky, s2.theme)
        assertEquals(ThemeMode.Light, s2.themeMode)

        // Test non-default combination 3
        val store3 = createStore()
        store3.update {
            it.copy(
                soundSet = SoundSetId.Keyboard,
                theme = ThemeId.Dynamic,
                themeMode = ThemeMode.System
            )
        }
        val s3 = store3.settings.first()
        assertEquals(SoundSetId.Keyboard, s3.soundSet)
        assertEquals(ThemeId.Dynamic, s3.theme)
        assertEquals(ThemeMode.System, s3.themeMode)
    }

    @Test
    fun writeTimeClampingStrictChecks() = runBlocking {
        val storeMin = createStore()
        storeMin.update {
            it.copy(
                bellVolume = -100f,
                warmth = -50,
                intensity = -10,
                beam = -5
            )
        }
        val sMin = storeMin.settings.first()
        assertEquals(0f, sMin.bellVolume, 0.0001f)
        assertEquals(0, sMin.warmth)
        assertEquals(20, sMin.intensity)
        assertEquals(30, sMin.beam)

        val storeMax = createStore()
        storeMax.update {
            it.copy(
                bellVolume = 100f,
                warmth = 250,
                intensity = 500,
                beam = 1000
            )
        }
        val sMax = storeMax.settings.first()
        assertEquals(1f, sMax.bellVolume, 0.0001f)
        assertEquals(100, sMax.warmth)
        assertEquals(100, sMax.intensity)
        assertEquals(100, sMax.beam)

        val storeNaN = createStore()
        storeNaN.update { it.copy(bellVolume = Float.NaN) }
        assertEquals(0.6f, storeNaN.settings.first().bellVolume, 0.0001f)

        val storePosInf = createStore()
        storePosInf.update { it.copy(bellVolume = Float.POSITIVE_INFINITY) }
        assertEquals(0.6f, storePosInf.settings.first().bellVolume, 0.0001f)

        val storeNegInf = createStore()
        storeNegInf.update { it.copy(bellVolume = Float.NEGATIVE_INFINITY) }
        assertEquals(0.6f, storeNegInf.settings.first().bellVolume, 0.0001f)
    }

    @Test
    fun themesComeFromDesignThemePackage() {
        assertEquals("app.enk.design.theme.ThemeId", ThemeId::class.java.name)
        assertEquals("app.enk.design.theme.ThemeMode", ThemeMode::class.java.name)
    }

    @Test
    fun corruptedFileFallsBackToDefaults() = runBlocking {
        val file = File(tempFolder.root, "corrupt_${System.nanoTime()}.preferences_pb")
        file.writeBytes(byteArrayOf(0x7F, 0x00, 0x13, 0x37, 0x42, 0x99.toByte(), 0xFF.toByte(), 0x01))
        val store = createStore(file)
        assertEquals(Settings(), store.settings.first())
    }
}
