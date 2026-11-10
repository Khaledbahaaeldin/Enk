package app.enk.shell

import android.view.View
import androidx.activity.ComponentActivity
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.core.graphics.Insets
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import app.enk.box.BoxScreen
import app.enk.box.BoxUiState
import app.enk.box.TrashScreen
import app.enk.data.DividerEntity
import app.enk.data.NoteSummary
import app.enk.data.SearchHit
import app.enk.data.TrashedSummary
import app.enk.design.theme.EnkShellTheme
import app.enk.design.theme.ThemeId
import app.enk.design.theme.ThemeMode
import app.enk.search.SearchContent
import app.enk.settings.SettingsContent
import app.enk.settings.Settings
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * The Box, Find, Trash and Setup laid out between real system bars and with the keyboard up. They
 * passed every earlier test while, on a device, the large titles overlapped the status bar, the search
 * field and the first group, and the sheet sat under the navigation bar: none of those tests had insets.
 * Robolectric's density is 1, so px and dp are the same here.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], qualifiers = "w400dp-h820dp")
class ShellScreensLayoutTest {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<ComponentActivity>()

    private val statusBarPx = 49
    private val navBarPx = 24
    private val keyboardPx = 300

    private var view: View? = null

    private val notes = listOf(
        NoteSummary("n1", "Rain on the glass\nSecond line", 1, 1_700_000_000_000L),
        NoteSummary("n2", "Thunder over the hill", 2, 1_700_001_000_000L)
    )
    private val dividers = listOf(DividerEntity(1L, "Journal", 0))

    private fun applyInsets(keyboard: Boolean = false) {
        composeTestRule.runOnUiThread {
            val builder = WindowInsetsCompat.Builder()
                .setInsets(WindowInsetsCompat.Type.statusBars(), Insets.of(0, statusBarPx, 0, 0))
                .setInsets(WindowInsetsCompat.Type.navigationBars(), Insets.of(0, 0, 0, navBarPx))
            if (keyboard) {
                builder.setInsets(WindowInsetsCompat.Type.ime(), Insets.of(0, 0, 0, keyboardPx))
                builder.setVisible(WindowInsetsCompat.Type.ime(), true)
            }
            ViewCompat.dispatchApplyWindowInsets(view!!, builder.build())
        }
        composeTestRule.waitForIdle()
    }

    private fun show(content: @androidx.compose.runtime.Composable () -> Unit) {
        composeTestRule.setContent {
            view = LocalView.current
            EnkShellTheme(theme = ThemeId.Graphite, mode = ThemeMode.Light) { content() }
        }
        applyInsets()
    }

    private fun top(tag: String) = composeTestRule.onNodeWithTag(tag).fetchSemanticsNode().boundsInRoot.top
    private fun bottom(tag: String) = composeTestRule.onNodeWithTag(tag).fetchSemanticsNode().boundsInRoot.bottom

    @Test
    fun boxTitleClearsTheStatusBarAndTheContentClearsTheTitle() {
        show { BoxScreen(state = BoxUiState(dividers = dividers, notes = notes), entranceProgress = 1f) }

        assertTrue("title top ${top("large_title")} must be below the status bar", top("large_title") >= statusBarPx)
        assertTrue(
            "the divider strip (${top("divider_strip")}) must start below the title (${bottom("large_title")})",
            top("divider_strip") >= bottom("large_title")
        )
    }

    @Test
    fun findTitleClearsTheStatusBarAndThePinnedFieldClearsTheTitle() {
        show {
            SearchContent(
                query = "rain",
                results = listOf(SearchHit("n1", 0, "Rain on the glass")),
                onQueryChange = {},
                onHitClick = { _, _ -> }
            )
        }

        assertTrue(top("large_title") >= statusBarPx)
        assertTrue(
            "the search field (${top("search_field")}) must start below the title (${bottom("large_title")})",
            top("search_field") >= bottom("large_title")
        )
    }

    @Test
    fun setupTitleClearsTheStatusBarAndTheFirstGroupClearsTheTitle() {
        show { SettingsContent(settings = Settings(), onSettingsChange = {}) }

        assertTrue(top("large_title") >= statusBarPx)
        assertTrue(
            "the Appearance group (${top("group_appearance")}) must start below the title (${bottom("large_title")})",
            top("group_appearance") >= bottom("large_title")
        )
    }

    @Test
    fun trashTitleClearsTheStatusBar() {
        show {
            TrashScreen(
                trashed = listOf(TrashedSummary("t1", "Old note", 1L, 5)),
                onRestore = {}, onDeleteAllForever = {}, onNavigate = {}, onNavigateBack = {}
            )
        }
        assertTrue(top("large_title") >= statusBarPx)
    }

    @Test
    fun theNewDividerSheetIsNotUnderTheNavigationBar() {
        show { BoxScreen(state = BoxUiState(dividers = dividers, notes = notes), entranceProgress = 1f) }
        composeTestRule.onNodeWithText("+").performClick()
        composeTestRule.waitForIdle()

        val windowBottom = 820f
        assertTrue(
            "Add (bottom ${bottom("new_divider_add")}) must be above the navigation bar (top ${windowBottom - navBarPx})",
            bottom("new_divider_add") <= windowBottom - navBarPx
        )
        assertTrue(bottom("new_divider_cancel") <= windowBottom - navBarPx)
        // The shell's own navigation bar is covered by the scrim and the sheet, not drawn over them.
        composeTestRule.onAllNodesWithTag("new_divider_sheet").assertCountEquals(1)
    }

    @Test
    fun theNewDividerSheetRisesAboveTheKeyboard() {
        show { BoxScreen(state = BoxUiState(dividers = dividers, notes = notes), entranceProgress = 1f) }
        composeTestRule.onNodeWithText("+").performClick()
        composeTestRule.waitForIdle()
        applyInsets(keyboard = true)

        val windowBottom = 820f
        assertTrue(
            "Add (bottom ${bottom("new_divider_add")}) must be above the keyboard (top ${windowBottom - keyboardPx})",
            bottom("new_divider_add") <= windowBottom - keyboardPx
        )
        assertTrue(top("divider_name_field") >= 0f)
    }

    @Test
    fun theTrashRowOpensTheTrashAndShowsTheCount() {
        var opened = 0
        var state by mutableStateOf(BoxUiState(dividers = dividers, notes = notes, trashCount = 0))
        show { BoxScreen(state = state, entranceProgress = 1f, onOpenTrash = { opened++ }) }

        composeTestRule.onNodeWithText("Empty").assertExists()
        state = state.copy(trashCount = 3)
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("3 notes", useUnmergedTree = true).assertExists()

        composeTestRule.onNodeWithTag("trash_row").performClick()
        assertEquals(1, opened)
    }
}
