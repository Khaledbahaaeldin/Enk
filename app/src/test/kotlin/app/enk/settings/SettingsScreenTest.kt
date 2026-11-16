package app.enk.settings

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.hasScrollAction
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToKey
import app.enk.design.effects.EffectsLevel
import app.enk.design.effects.LocalEffects
import app.enk.design.effects.ResolvedEffects
import app.enk.design.shell.LocalReduceEffects
import app.enk.design.theme.EnkShellTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class SettingsScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun effectsControlChangesSettingAndFooterMatchesSpec() {
        var currentSettings by mutableStateOf(Settings())

        composeTestRule.setContent {
            CompositionLocalProvider(
                LocalEffects provides ResolvedEffects.Light,
                LocalReduceEffects provides true
            ) {
                EnkShellTheme(theme = currentSettings.theme, mode = currentSettings.themeMode) {
                    SettingsContent(
                        settings = currentSettings,
                        onSettingsChange = { block ->
                            currentSettings = block(currentSettings)
                        }
                    )
                }
            }
        }

        // Scroll to the display group
        composeTestRule.onNode(hasScrollAction()).performScrollToKey("group_display")

        // Verify footer text matches specification exactly
        val expectedFooter = "Liquid bends light like a drop of water; Frost blurs it. Effects sets how much the phone may spend: Light is solid and still."
        composeTestRule.onNodeWithText(expectedFooter).assertIsDisplayed()

        // Verify initial selection is Auto
        assertEquals(EffectsLevel.Auto, currentSettings.effects)

        // Select "Balanced"
        composeTestRule.onNodeWithText("Balanced").performClick()
        assertEquals(EffectsLevel.Balanced, currentSettings.effects)

        // Select "Light"
        composeTestRule.onNodeWithText("Light").performClick()
        assertEquals(EffectsLevel.Light, currentSettings.effects)

        // Select "Full"
        composeTestRule.onNodeWithText("Full").performClick()
        assertEquals(EffectsLevel.Full, currentSettings.effects)
    }
}
