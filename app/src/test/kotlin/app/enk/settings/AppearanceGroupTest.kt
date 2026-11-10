package app.enk.settings

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import app.enk.design.theme.EnkShellTheme
import app.enk.design.theme.ThemeId
import app.enk.design.theme.ThemeMode
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], qualifiers = "w400dp-h820dp")
class AppearanceGroupTest {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun tilesAreInDsOrder() {
        // DS 12.8: 3 x 2 grid row by row: Graphite, Apricot, Sky / Slate, Mono, Dynamic
        val expected = listOf(
            ThemeId.Graphite,
            ThemeId.Apricot,
            ThemeId.Sky,
            ThemeId.Slate,
            ThemeId.Mono,
            ThemeId.Dynamic
        )
        assertEquals(expected, THEME_TILE_ORDER)
    }

    @Test
    fun modeSegmentOrderIsLightDarkSystem() {
        val expected = listOf(
            ThemeMode.Light,
            ThemeMode.Dark,
            ThemeMode.System
        )
        assertEquals(expected, MODE_ORDER)
    }

    @Test
    fun groupsAreInDsOrder() {
        val expected = listOf("Appearance", "Writing", "Sound", "Display", "Data", "About")
        assertEquals(expected, SETUP_GROUP_ORDER)
    }

    @Test
    fun selectingTileCallsSetTheme() {
        var selectedTheme: ThemeId = ThemeId.Graphite

        composeTestRule.setContent {
            EnkShellTheme(theme = ThemeId.Graphite, mode = ThemeMode.Dark) {
                AppearanceGroup(
                    selectedTheme = selectedTheme,
                    onThemeSelect = { selectedTheme = it },
                    selectedMode = ThemeMode.System,
                    onModeSelect = {}
                )
            }
        }

        composeTestRule.onNodeWithTag("theme_tile_apricot").performClick()
        assertEquals(ThemeId.Apricot, selectedTheme)
    }

    @Test
    fun selectedTileHasIndexMark() {
        composeTestRule.setContent {
            EnkShellTheme(theme = ThemeId.Graphite, mode = ThemeMode.Dark) {
                AppearanceGroup(
                    selectedTheme = ThemeId.Graphite,
                    onThemeSelect = {},
                    selectedMode = ThemeMode.System,
                    onModeSelect = {}
                )
            }
        }

        // Graphite is selected, its index mark is present and displayed
        composeTestRule.onNodeWithTag("theme_tile_graphite_index_mark", useUnmergedTree = true).assertIsDisplayed()
    }
}
