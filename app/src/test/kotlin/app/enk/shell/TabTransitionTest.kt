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
class TabTransitionTest {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun switchingTabsShowsBothScreensDuringTransitionAndOnlyNewOneAfter() {
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

        // Navigate to Find
        mainVm.navigate(Route.Find)

        // Advance 100 ms: transition is mid-flight, both screens exist
        composeTestRule.mainClock.advanceTimeBy(100)
        composeTestRule.onNodeWithTag("box_screen").assertExists()
        composeTestRule.onNodeWithTag("search_screen").assertExists()

        // Advance to 800 ms: transition complete, only search_screen exists
        composeTestRule.mainClock.advanceTimeBy(700)
        composeTestRule.onNodeWithTag("box_screen").assertDoesNotExist()
        composeTestRule.onNodeWithTag("search_screen").assertExists()
    }

    @Test
    fun secondTabTapMidTransitionLandsOnSecondTarget() {
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

        composeTestRule.mainClock.autoAdvance = false

        // Navigate to Find
        mainVm.navigate(Route.Find)
        composeTestRule.mainClock.advanceTimeBy(100)

        // Mid-transition, retarget to Setup
        mainVm.navigate(Route.Setup)
        composeTestRule.mainClock.advanceTimeBy(800)

        composeTestRule.onNodeWithTag("box_screen").assertDoesNotExist()
        composeTestRule.onNodeWithTag("search_screen").assertDoesNotExist()
        composeTestRule.onNodeWithTag("settings_screen").assertExists()
        assertEquals(Route.Setup, mainVm.currentRoute.value)
    }

    @Test
    fun reduceEffectsEndsTransitionInstantly() {
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
        composeTestRule.onNodeWithTag("box_screen").assertExists()

        composeTestRule.mainClock.autoAdvance = false

        mainVm.navigate(Route.Find)
        composeTestRule.mainClock.advanceTimeBy(100)
        composeTestRule.onNodeWithTag("box_screen").assertDoesNotExist()
        composeTestRule.onNodeWithTag("search_screen").assertExists()
    }
}
