package app.enk.settings

import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import app.enk.EnkSnapshots
import app.enk.design.LocalMonoFont
import app.enk.design.theme.DynamicShellColors
import app.enk.design.theme.EnkShellTheme
import app.enk.design.theme.LocalShellColors
import app.enk.design.theme.LocalShellMaterial
import app.enk.design.theme.ShellPalettes
import app.enk.design.theme.ThemeId
import app.enk.design.theme.ThemeMode
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.GraphicsMode
import java.io.File

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class SetupGoldenTest {

    @get:Rule
    val snapshots = EnkSnapshots(SetupGoldenTest::class.java)

    private val courier: FontFamily
        get() = FontFamily(Font(File("../design/src/main/res/font/courier_prime_regular.ttf")))

    @Composable
    private fun SetupScene(theme: ThemeId, mode: ThemeMode) {
        CompositionLocalProvider(LocalMonoFont provides courier) {
            EnkShellTheme(theme = theme, mode = mode) {
                SettingsContent(
                    settings = Settings(
                        theme = theme,
                        themeMode = mode
                    ),
                    onSettingsChange = {}
                )
            }
        }
    }

    @Test
    fun setupGraphiteDark() {
        snapshots.snapshot {
            SetupScene(ThemeId.Graphite, ThemeMode.Dark)
        }
    }

    @Test
    fun setupSlateLight() {
        snapshots.snapshot {
            SetupScene(ThemeId.Slate, ThemeMode.Light)
        }
    }

    @Test
    fun setupApricotDark() {
        snapshots.snapshot {
            SetupScene(ThemeId.Apricot, ThemeMode.Dark)
        }
    }

    @Test
    fun setupDynamicDarkSeed() {
        val seedColor = Color(0xFF1976D2)
        val colorScheme = darkColorScheme(primary = seedColor)
        val colors = DynamicShellColors.from(colorScheme, dark = true)
        val material = ShellPalettes.material(dark = true)

        snapshots.snapshot {
            CompositionLocalProvider(
                LocalMonoFont provides courier,
                LocalShellColors provides colors,
                LocalShellMaterial provides material
            ) {
                SettingsContent(
                    settings = Settings(
                        theme = ThemeId.Dynamic,
                        themeMode = ThemeMode.Dark
                    ),
                    onSettingsChange = {}
                )
            }
        }
    }
}
