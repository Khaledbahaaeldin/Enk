package app.enk.search

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import app.enk.EnkSnapshots
import app.enk.data.SearchHit
import app.enk.design.LocalMonoFont
import app.enk.design.theme.EnkShellTheme
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
class SearchGoldenTest {

    @get:Rule
    val snapshots = EnkSnapshots(SearchGoldenTest::class.java)

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
        snapshots.snapshot {
            SearchScene(ThemeId.Graphite, ThemeMode.Dark, "rain", sampleHits)
        }
    }

    @Test
    fun searchApricotLightResults() {
        snapshots.snapshot {
            SearchScene(ThemeId.Apricot, ThemeMode.Light, "rain", sampleHits)
        }
    }

    @Test
    fun searchGraphiteDarkEmpty() {
        snapshots.snapshot {
            SearchScene(ThemeId.Graphite, ThemeMode.Dark, "", emptyList())
        }
    }

    @Test
    fun searchGraphiteDarkNoResults() {
        snapshots.snapshot {
            SearchScene(ThemeId.Graphite, ThemeMode.Dark, "thunder", emptyList())
        }
    }
}
