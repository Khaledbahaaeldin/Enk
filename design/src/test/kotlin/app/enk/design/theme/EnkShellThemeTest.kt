package app.enk.design.theme

import android.content.res.Configuration
import androidx.compose.runtime.AbstractApplier
import androidx.compose.runtime.Composition
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Recomposer
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import kotlinx.coroutines.Dispatchers
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
class EnkShellThemeTest {

    private fun resolveColorsInSystemMode(uiModeNight: Int): ShellColors? {
        val config = Configuration().apply {
            uiMode = uiModeNight
        }
        var currentColors: ShellColors? = null
        val recomposer = Recomposer(Dispatchers.Unconfined)
        val composition = Composition(
            object : AbstractApplier<Unit>(Unit) {
                override fun insertBottomUp(index: Int, instance: Unit) {}
                override fun insertTopDown(index: Int, instance: Unit) {}
                override fun move(from: Int, to: Int, count: Int) {}
                override fun onClear() {}
                override fun remove(index: Int, count: Int) {}
            },
            recomposer
        )
        composition.setContent {
            CompositionLocalProvider(
                LocalConfiguration provides config,
                LocalContext provides RuntimeEnvironment.getApplication()
            ) {
                EnkShellTheme(theme = ThemeId.Graphite, mode = ThemeMode.System) {
                    currentColors = LocalShellColors.current
                }
            }
        }
        return currentColors
    }

    @Test
    @Config(qualifiers = "night")
    fun systemModeFollowsDarkTheme() {
        val colors = resolveColorsInSystemMode(Configuration.UI_MODE_NIGHT_YES)
        val expected = ShellPalettes.colors(ThemeId.Graphite, true)
        assertNotNull("Expected colors to be captured in composition", colors)
        assertEquals(expected.bg, colors?.bg)
        assertEquals(expected.ui, colors?.ui)
    }

    @Test
    @Config(qualifiers = "notnight")
    fun systemModeFollowsLightTheme() {
        val colors = resolveColorsInSystemMode(Configuration.UI_MODE_NIGHT_NO)
        val expected = ShellPalettes.colors(ThemeId.Graphite, false)
        assertNotNull("Expected colors to be captured in composition", colors)
        assertEquals(expected.bg, colors?.bg)
        assertEquals(expected.ui, colors?.ui)
    }
}
