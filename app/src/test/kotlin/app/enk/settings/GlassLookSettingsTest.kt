package app.enk.settings

import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.hasScrollAction
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performScrollToKey
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import app.enk.design.effects.FrostTuning
import app.enk.design.effects.GlassLook
import app.enk.design.effects.LiquidTuning
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File

@RunWith(RobolectricTestRunner::class)
class GlassLookSettingsTest {

    @get:Rule
    val tempFolder = TemporaryFolder()

    @get:Rule
    val composeTestRule = createComposeRule()

    private fun createStore(): SettingsStore {
        val file = File(tempFolder.root, "settings_${System.nanoTime()}.preferences_pb")
        val dataStore = PreferenceDataStoreFactory.create(produceFile = { file })
        return SettingsStore(dataStore)
    }

    @Test
    fun batch14SettingsLoadAsLiquidWithDefaults() = runBlocking {
        val store = createStore()
        val s = store.settings.first()
        assertEquals(GlassLook.Liquid, s.glassLook)
        assertEquals(LiquidTuning(), s.liquidTuning)
        assertEquals(FrostTuning(), s.frostTuning)
    }

    @Test
    fun outOfRangeStoredValuesAreClamped() = runBlocking {
        val file = File(tempFolder.root, "clamped_${System.nanoTime()}.preferences_pb")
        val rawDataStore = PreferenceDataStoreFactory.create(produceFile = { file })
        rawDataStore.edit { prefs ->
            prefs[floatPreferencesKey("liquid_refraction")] = 9f
            prefs[floatPreferencesKey("liquid_lens_depth")] = 0.1f
            prefs[floatPreferencesKey("frost_blur")] = 5f
        }
        val store = SettingsStore(rawDataStore)
        val s = store.settings.first()
        assertEquals(2f, s.liquidTuning.refraction, 0.0001f)
        assertEquals(0.5f, s.liquidTuning.lensDepth, 0.0001f)
        assertEquals(1.5f, s.frostTuning.blur, 0.0001f)
    }

    @Test
    fun roundTripsEveryField() = runBlocking {
        val store = createStore()
        val customLiquid = LiquidTuning(
            refraction = 1.4f,
            lensDepth = 1.2f,
            fringe = 0.5f,
            frostDp = 4f,
            tint = 0.25f,
            edgeLight = 1.1f
        )
        val customFrost = FrostTuning(
            blur = 1.3f,
            tint = 0.8f,
            edgeLight = 0.9f
        )
        store.update {
            it.copy(
                glassLook = GlassLook.Frost,
                liquidTuning = customLiquid,
                frostTuning = customFrost
            )
        }
        val s = store.settings.first()
        assertEquals(GlassLook.Frost, s.glassLook)
        assertEquals(customLiquid, s.liquidTuning)
        assertEquals(customFrost, s.frostTuning)
    }

    @Test
    @Config(sdk = [32])
    fun liquidChoiceDisabledBelowApi33() {
        composeTestRule.setContent {
            SettingsContent(
                settings = Settings(glassLook = GlassLook.Liquid),
                onSettingsChange = {}
            )
        }
        composeTestRule.onNode(hasScrollAction()).performScrollToKey("group_display")
        composeTestRule.onNodeWithText("Liquid").assertIsNotEnabled()
        composeTestRule.onNodeWithText("Liquid glass needs Android 13 or later.").assertExists()
    }
}
