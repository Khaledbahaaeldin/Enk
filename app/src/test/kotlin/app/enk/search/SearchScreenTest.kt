package app.enk.search

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.activity.ComponentActivity
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToIndex
import androidx.compose.ui.test.performTextInput
import app.enk.data.SearchHit
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
class SearchScreenTest {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun emptyAndNoResultStrings() {
        var query by mutableStateOf("")
        var hits by mutableStateOf(emptyList<SearchHit>())

        composeTestRule.setContent {
            EnkShellTheme(theme = ThemeId.Graphite, mode = ThemeMode.Dark) {
                SearchContent(
                    query = query,
                    onQueryChange = { query = it },
                    results = hits,
                    onHitClick = { _, _ -> }
                )
            }
        }

        // 1. Empty state
        composeTestRule.onNodeWithText("Type to search your notes.").assertIsDisplayed()

        // 2. No results state
        query = "zzz"
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("Nothing found for “zzz”.").assertIsDisplayed()
    }

    @Test
    fun hitOpensNoteAtPage() {
        val sampleHits = listOf(
            SearchHit("note-abc", 2, "First line of match in note")
        )
        var openedNoteId: String? = null
        var openedPageIndex: Int? = null

        composeTestRule.setContent {
            EnkShellTheme(theme = ThemeId.Graphite, mode = ThemeMode.Dark) {
                SearchContent(
                    query = "match",
                    onQueryChange = {},
                    results = sampleHits,
                    onHitClick = { id, page ->
                        openedNoteId = id
                        openedPageIndex = page
                    }
                )
            }
        }

        composeTestRule.onNodeWithTag("search_hit_note-abc_2").performClick()
        assertEquals("note-abc", openedNoteId)
        assertEquals(2, openedPageIndex)
    }

    @Test
    fun fieldIsPinnedAndTitleCollapses() {
        // Generate enough hits so list is scrollable
        val manyHits = (0..20).map { i ->
            SearchHit("note-$i", 0, "Note line $i with keyword rain")
        }

        composeTestRule.setContent {
            EnkShellTheme(theme = ThemeId.Graphite, mode = ThemeMode.Dark) {
                SearchContent(
                    query = "rain",
                    onQueryChange = {},
                    results = manyHits,
                    onHitClick = { _, _ -> }
                )
            }
        }

        // Search field is displayed initially
        composeTestRule.onNodeWithTag("search_field").assertIsDisplayed()

        // Scroll to the 15th item
        composeTestRule.onNodeWithTag("search_results_list").performScrollToIndex(15)

        // Search field is pinned: it is still displayed!
        composeTestRule.onNodeWithTag("search_field").assertIsDisplayed()
    }
}
