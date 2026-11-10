package app.enk.box

import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import app.cash.paparazzi.DeviceConfig
import app.cash.paparazzi.Paparazzi
import app.enk.data.DividerEntity
import app.enk.data.NoteSummary
import app.enk.data.TrashedSummary
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
import java.io.File

class BoxThemeGoldenTest {

    @get:Rule
    val paparazzi = Paparazzi(
        deviceConfig = DeviceConfig.NEXUS_5.copy(
            screenWidth = 1200,
            screenHeight = 2460
        ),
        maxPercentDifference = 2.0
    )

    private val courier: FontFamily
        get() = FontFamily(Font(File("../design/src/main/res/font/courier_prime_regular.ttf")))

    private val sampleDividers = listOf(
        DividerEntity(id = 1L, name = "Journal", position = 0),
        DividerEntity(id = 2L, name = "Drafts", position = 1),
        DividerEntity(id = 3L, name = "Archive", position = 2)
    )

    private val sampleNotes = listOf(
        NoteSummary(
            id = "note-1",
            firstLines = "Morning thoughts\nA quiet studio before dawn.",
            pageCount = 1,
            modifiedAt = 1700000000000L
        ),
        NoteSummary(
            id = "note-2",
            firstLines = "Chapter IV: The Carriage\nThe steel rail held firm under the keystrokes.",
            pageCount = 4,
            modifiedAt = 1699900000000L
        )
    )

    private val sampleTrashed = listOf(
        TrashedSummary(
            id = "trash-1",
            title = "First draft of the preface",
            trashedAt = 1699800000000L,
            daysRemaining = 26
        ),
        TrashedSummary(
            id = "trash-2",
            title = "Old scratchpad ideas",
            trashedAt = 1699000000000L,
            daysRemaining = 1
        )
    )

    @Composable
    private fun BoxScene(theme: ThemeId, mode: ThemeMode) {
        CompositionLocalProvider(LocalMonoFont provides courier) {
            EnkShellTheme(theme = theme, mode = mode) {
                BoxContent(
                    state = BoxUiState(
                        dividers = sampleDividers,
                        selectedDividerId = 1L,
                        notes = sampleNotes,
                        trashCount = 2
                    )
                )
            }
        }
    }

    @Composable
    private fun DynamicBoxScene(isDark: Boolean, seedColor: Color) {
        val colorScheme = if (isDark) {
            darkColorScheme(primary = seedColor, surface = Color(0xFF141210))
        } else {
            lightColorScheme(primary = seedColor, surface = Color(0xFFFAF7F2))
        }
        val colors = DynamicShellColors.from(colorScheme, isDark)
        val material = ShellPalettes.material(isDark)

        CompositionLocalProvider(
            LocalMonoFont provides courier,
            LocalShellColors provides colors,
            LocalShellMaterial provides material
        ) {
            BoxContent(
                state = BoxUiState(
                    dividers = sampleDividers,
                    selectedDividerId = 1L,
                    notes = sampleNotes,
                    trashCount = 2
                )
            )
        }
    }

    @Composable
    private fun TrashScene(theme: ThemeId, mode: ThemeMode) {
        CompositionLocalProvider(LocalMonoFont provides courier) {
            EnkShellTheme(theme = theme, mode = mode) {
                TrashContent(
                    trashedNotes = sampleTrashed
                )
            }
        }
    }

    // 10 static theme/mode combinations
    @Test
    fun boxGraphiteDark() {
        paparazzi.snapshot { BoxScene(ThemeId.Graphite, ThemeMode.Dark) }
    }

    @Test
    fun boxGraphiteLight() {
        paparazzi.snapshot { BoxScene(ThemeId.Graphite, ThemeMode.Light) }
    }

    @Test
    fun boxApricotDark() {
        paparazzi.snapshot { BoxScene(ThemeId.Apricot, ThemeMode.Dark) }
    }

    @Test
    fun boxApricotLight() {
        paparazzi.snapshot { BoxScene(ThemeId.Apricot, ThemeMode.Light) }
    }

    @Test
    fun boxSkyDark() {
        paparazzi.snapshot { BoxScene(ThemeId.Sky, ThemeMode.Dark) }
    }

    @Test
    fun boxSkyLight() {
        paparazzi.snapshot { BoxScene(ThemeId.Sky, ThemeMode.Light) }
    }

    @Test
    fun boxSlateDark() {
        paparazzi.snapshot { BoxScene(ThemeId.Slate, ThemeMode.Dark) }
    }

    @Test
    fun boxSlateLight() {
        paparazzi.snapshot { BoxScene(ThemeId.Slate, ThemeMode.Light) }
    }

    @Test
    fun boxMonoDark() {
        paparazzi.snapshot { BoxScene(ThemeId.Mono, ThemeMode.Dark) }
    }

    @Test
    fun boxMonoLight() {
        paparazzi.snapshot { BoxScene(ThemeId.Mono, ThemeMode.Light) }
    }

    // Dynamic with 2 seed schemes
    @Test
    fun boxDynamicLightSeed1() {
        paparazzi.snapshot {
            // Seed 1: Blue scheme
            DynamicBoxScene(isDark = false, seedColor = Color(0xFF1976D2))
        }
    }

    @Test
    fun boxDynamicDarkSeed2() {
        paparazzi.snapshot {
            // Seed 2: Amber scheme
            DynamicBoxScene(isDark = true, seedColor = Color(0xFFFFB74D))
        }
    }

    // TrashContent golden
    @Test
    fun trashGraphiteDark() {
        paparazzi.snapshot { TrashScene(ThemeId.Graphite, ThemeMode.Dark) }
    }

    @Test
    fun trashApricotLight() {
        paparazzi.snapshot { TrashScene(ThemeId.Apricot, ThemeMode.Light) }
    }
}
