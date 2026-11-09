package app.enk.shell

import androidx.activity.ComponentActivity
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.core.view.WindowCompat
import app.enk.design.theme.EnkShellTheme
import app.enk.design.theme.LocalShellColors
import app.enk.design.theme.ThemeId
import app.enk.design.theme.ThemeMode
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], qualifiers = "w400dp-h820dp")
class SystemBarIconsTest {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun surfaceBrightnessDecidesTheIconColour() {
        assertTrue(isDarkSurface(Color(0xFF161514)))
        assertTrue(isDarkSurface(Color.Black))
        assertFalse(isDarkSurface(Color(0xFFECE8DF)))
        assertFalse(isDarkSurface(Color.White))
    }

    private fun lightIcons(): Boolean {
        val window = composeTestRule.activity.window
        return WindowCompat.getInsetsController(window, window.decorView).isAppearanceLightStatusBars
    }

    @Test
    fun aLightShellGetsDarkIconsAndADarkOneGetsLightIcons() {
        var theme by mutableStateOf(ThemeId.Graphite)
        var mode by mutableStateOf(ThemeMode.Light)
        composeTestRule.setContent {
            EnkShellTheme(theme = theme, mode = mode) {
                SystemBarIcons(onDarkSurface = isDarkSurface(LocalShellColors.current.bg))
            }
        }
        for (id in ThemeId.values().filter { it != ThemeId.Dynamic }) {
            for ((m, expectLightIcons) in listOf(ThemeMode.Light to true, ThemeMode.Dark to false)) {
                theme = id
                mode = m
                composeTestRule.waitForIdle()
                // The shell theme fades its colours, so wait for the icons to settle on the right value.
                composeTestRule.waitUntil(5_000) { lightIcons() == expectLightIcons }
                assertEquals("$id $m", expectLightIcons, lightIcons())
            }
        }
    }

    @Test
    fun theWritersDarkChassisAlwaysGetsLightIconsAndTheChoiceFollowsTheScreen() {
        var onWriter by mutableStateOf(false)
        composeTestRule.setContent {
            EnkShellTheme(theme = ThemeId.Graphite, mode = ThemeMode.Light) {
                SystemBarIcons(onDarkSurface = if (onWriter) true else isDarkSurface(LocalShellColors.current.bg))
            }
        }
        composeTestRule.waitForIdle()
        assertTrue("a light shell screen: dark icons", lightIcons())

        onWriter = true
        composeTestRule.waitForIdle()
        assertFalse("the writer: light icons even in a light app", lightIcons())

        onWriter = false
        composeTestRule.waitForIdle()
        assertTrue("back on a shell screen: dark icons again", lightIcons())
    }
}
