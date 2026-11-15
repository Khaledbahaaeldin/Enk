package app.enk.shell

import androidx.activity.ComponentActivity
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.lifecycle.ViewModelProvider
import app.enk.MainViewModel
import app.enk.design.shell.LocalReduceEffects
import app.enk.testContainer
import kotlinx.coroutines.Dispatchers
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/** The Rail transition in the running shell: Write from a tab, and the island's Box button back. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class RailNavigationTest {

    @get:Rule
    val rule = createAndroidComposeRule<ComponentActivity>()

    private fun start() {
        val activity = rule.activity
        val container = testContainer(activity, activity.cacheDir)
        val vm = ViewModelProvider(activity, MainViewModel.factory(activity, container, Dispatchers.Unconfined))[MainViewModel::class.java]
        rule.setContent {
            CompositionLocalProvider(LocalReduceEffects provides false) {
                EnkNavHost(container = container, mainViewModel = vm)
            }
        }
        rule.waitUntil(5_000) { rule.onAllNodesWithTag("writer_screen").fetchSemanticsNodes().isNotEmpty() }
        rule.waitForIdle()
    }

    @Test
    fun theIslandsBoxButtonRunsItBackAndWriteRunsItForward() {
        start()
        rule.onNodeWithTag("top_island").assertExists()
        rule.onNodeWithTag("glass_nav_bar").assertDoesNotExist()

        // Island, then Box: the Box with its nav bar, the writer gone.
        rule.onNodeWithTag("top_island").performClick()
        rule.waitForIdle()
        rule.onNodeWithTag("box_button").performClick()
        rule.mainClock.autoAdvance = false
        rule.mainClock.advanceTimeBy(100)
        // The writer is kept while the paper is pulled back.
        rule.onNodeWithTag("writer_screen").assertExists()
        rule.onNodeWithTag("box_screen").assertExists()
        rule.mainClock.advanceTimeBy(3_000)
        rule.mainClock.autoAdvance = true
        rule.waitForIdle()
        rule.onNodeWithTag("box_screen").assertExists()
        rule.onNodeWithTag("glass_nav_bar").assertExists()
        rule.onNodeWithTag("writer_screen").assertDoesNotExist()

        // Write: the writer with its island, the nav bar gone.
        rule.onNodeWithContentDescription("Write").performClick()
        rule.mainClock.autoAdvance = false
        rule.mainClock.advanceTimeBy(100)
        rule.onNodeWithTag("box_screen").assertExists()
        rule.onNodeWithTag("writer_screen").assertExists()
        rule.mainClock.advanceTimeBy(3_000)
        rule.mainClock.autoAdvance = true
        rule.waitForIdle()
        rule.onNodeWithTag("writer_screen").assertExists()
        rule.onNodeWithTag("top_island").assertExists()
        rule.onNodeWithTag("box_screen").assertDoesNotExist()
        rule.onNodeWithTag("glass_nav_bar").assertDoesNotExist()
    }
}
