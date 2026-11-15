package app.enk.shell

import androidx.activity.ComponentActivity
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.lifecycle.ViewModelProvider
import app.enk.MainViewModel
import app.enk.design.shell.LocalReduceEffects
import app.enk.testContainer
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class PushedScreenTransitionTest {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun pushingTrashShowsTransitionAndBackMirrorsIt() {
        val activity = composeTestRule.activity
        val container = testContainer(activity, activity.cacheDir)
        val mainVm = ViewModelProvider(activity, MainViewModel.factory(activity, container)).get(MainViewModel::class.java)

        mainVm.navigate(Route.Box)

        composeTestRule.setContent {
            CompositionLocalProvider(LocalReduceEffects provides false) {
                EnkNavHost(
                    container = container,
                    mainViewModel = mainVm
                )
            }
        }
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("box_screen").assertExists()

        composeTestRule.mainClock.autoAdvance = false

        // Push Trash
        mainVm.navigate(Route.Trash)

        // Mid-flight (100 ms)
        composeTestRule.mainClock.advanceTimeBy(100)
        composeTestRule.onNodeWithTag("box_screen").assertExists()
        composeTestRule.onNodeWithTag("trash_screen").assertExists()

        // Settle at 800 ms
        composeTestRule.mainClock.advanceTimeBy(700)
        composeTestRule.onNodeWithTag("box_screen").assertDoesNotExist()
        composeTestRule.onNodeWithTag("trash_screen").assertExists()

        // Pop / back
        mainVm.handleBack()

        // Mid-flight back (100 ms)
        composeTestRule.mainClock.advanceTimeBy(100)
        composeTestRule.onNodeWithTag("box_screen").assertExists()
        composeTestRule.onNodeWithTag("trash_screen").assertExists()

        // Settle back at 800 ms
        composeTestRule.mainClock.advanceTimeBy(700)
        composeTestRule.onNodeWithTag("box_screen").assertExists()
        composeTestRule.onNodeWithTag("trash_screen").assertDoesNotExist()
    }

    @Test
    fun pushingCreditsFromSetupWorksAndBackMirrors() {
        val activity = composeTestRule.activity
        val container = testContainer(activity, activity.cacheDir)
        val mainVm = ViewModelProvider(activity, MainViewModel.factory(activity, container)).get(MainViewModel::class.java)

        mainVm.navigate(Route.Setup)

        composeTestRule.setContent {
            CompositionLocalProvider(LocalReduceEffects provides false) {
                EnkNavHost(
                    container = container,
                    mainViewModel = mainVm
                )
            }
        }
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("settings_screen").assertExists()

        composeTestRule.mainClock.autoAdvance = false

        // Push Credits
        mainVm.navigate(Route.Credits)
        composeTestRule.mainClock.advanceTimeBy(100)
        composeTestRule.onNodeWithTag("settings_screen").assertExists()
        composeTestRule.onNodeWithTag("credits_screen").assertExists()

        composeTestRule.mainClock.advanceTimeBy(700)
        composeTestRule.onNodeWithTag("settings_screen").assertDoesNotExist()
        composeTestRule.onNodeWithTag("credits_screen").assertExists()

        // Back
        mainVm.handleBack()
        composeTestRule.mainClock.advanceTimeBy(800)
        composeTestRule.onNodeWithTag("settings_screen").assertExists()
        composeTestRule.onNodeWithTag("credits_screen").assertDoesNotExist()
    }

    @Test
    fun pushedScreenReduceEffectsEndsInstantly() {
        val activity = composeTestRule.activity
        val container = testContainer(activity, activity.cacheDir)
        val mainVm = ViewModelProvider(activity, MainViewModel.factory(activity, container)).get(MainViewModel::class.java)

        mainVm.navigate(Route.Box)

        composeTestRule.setContent {
            CompositionLocalProvider(LocalReduceEffects provides true) {
                EnkNavHost(
                    container = container,
                    mainViewModel = mainVm
                )
            }
        }
        composeTestRule.waitForIdle()

        composeTestRule.mainClock.autoAdvance = false

        mainVm.navigate(Route.Trash)
        composeTestRule.mainClock.advanceTimeBy(100)
        composeTestRule.onNodeWithTag("box_screen").assertDoesNotExist()
        composeTestRule.onNodeWithTag("trash_screen").assertExists()
    }
}
