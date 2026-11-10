package app.enk.search

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import app.cash.paparazzi.DeviceConfig
import app.cash.paparazzi.Paparazzi
import app.enk.data.SearchHit
import app.enk.design.LocalMonoFont
import app.enk.design.theme.EnkShellTheme
import app.enk.design.theme.ThemeId
import app.enk.design.theme.ThemeMode
import org.junit.Rule
import org.junit.Test
import java.io.File

class SearchGoldenTest {

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

    private val sampleHits = listOf(
        SearchHit("note-1", 0, "Walking in the rain along the old canal"),
        SearchHit("note-2", 1, "The sound of rain tapping on the glass"),
        SearchHit("note-3", 0, "Heavy rain started right after noon")
    )

    @Composable
    private fun SearchScene(theme: ThemeId, mode: ThemeMode, query: String, hits: List<SearchHit>) {
        CompositionLocalProvider(LocalMonoFont provides courier) {
            EnkShellTheme(theme = theme, mode = mode) {
                SearchContent(
                    query = query,
                    onQueryChange = {},
                    results = hits,
                    onHitClick = { _, _ -> }
                )
            }
        }
    }

    @Test
    fun searchGraphiteDarkResults() {
        paparazzi.snapshot {
            SearchScene(ThemeId.Graphite, ThemeMode.Dark, "rain", sampleHits)
        }
    }

    @Test
    fun searchApricotLightResults() {
        paparazzi.snapshot {
            SearchScene(ThemeId.Apricot, ThemeMode.Light, "rain", sampleHits)
        }
    }

    @Test
    fun searchGraphiteDarkEmpty() {
        paparazzi.snapshot {
            SearchScene(ThemeId.Graphite, ThemeMode.Dark, "", emptyList())
        }
    }

    @Test
    fun searchGraphiteDarkNoResults() {
        paparazzi.snapshot {
            SearchScene(ThemeId.Graphite, ThemeMode.Dark, "thunder", emptyList())
        }
    }
}
