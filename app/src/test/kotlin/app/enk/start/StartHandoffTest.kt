package app.enk.start

import androidx.compose.material3.Text
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], qualifiers = "w400dp-h820dp")
class StartHandoffTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private val referenceTargets = StartTargets(
        railY = 508f,
        screenWidth = 400f,
        screenHeight = 820f,
        carriageX = 276f,
        caretRect = Rect(270.3f, 479.5f, 282.01f, 481.5f),
        paperHeight = 760f,
        lightSplash = false
    )

    @Test
    fun touchSkipsWithin150ms() {
        var finished = false
        composeTestRule.mainClock.autoAdvance = false

        composeTestRule.setContent {
            StartOverlay(
                targets = referenceTargets,
                reduceMotion = false,
                onFinished = { finished = true },
                modifier = Modifier.testTag("start_overlay")
            )
        }

        // Initially not finished
        assertFalse(finished)

        // Tap the overlay
        composeTestRule.onNodeWithTag("start_overlay").performClick()

        // Advance 50 ms - still accelerating
        composeTestRule.mainClock.advanceTimeBy(50)
        assertFalse("Must not finish immediately upon touch", finished)

        // Within 150 ms (at 150 ms) must complete
        composeTestRule.mainClock.advanceTimeBy(100)
        assertTrue("Tapping the overlay must accelerate to completion within 150 ms", finished)
    }

    @Test
    fun coldStartEndsOnTheWriter() {
        composeTestRule.mainClock.autoAdvance = false

        composeTestRule.setContent {
            SplashHost(
                targets = referenceTargets,
                shouldPlay = true,
                reduceMotion = false
            ) {
                Text(
                    text = "Writer Screen",
                    modifier = Modifier.testTag("writer_screen")
                )
            }
        }

        // Overlay is displayed at start
        composeTestRule.onNodeWithTag("start_overlay").assertExists()

        // Advance past the 900 ms animation duration (and 750 ms handoff)
        composeTestRule.mainClock.advanceTimeBy(950)
        composeTestRule.waitForIdle()

        // Overlay removed and writer is visible
        composeTestRule.onNodeWithTag("start_overlay").assertDoesNotExist()
        composeTestRule.onNodeWithTag("writer_screen").assertIsDisplayed()
    }

    @Test
    fun widgetLaunchSkipsAnimation() {
        val shouldPlay = StartGate.shouldPlay(
            coldStart = true,
            launchedFromWidgetOrIntent = true,
            restoredState = false,
            loadMs = 400L
        )
        assertFalse("Widget launch must not play start animation", shouldPlay)

        composeTestRule.setContent {
            SplashHost(
                targets = referenceTargets,
                shouldPlay = shouldPlay,
                reduceMotion = false
            ) {
                Text(
                    text = "Writer Screen",
                    modifier = Modifier.testTag("writer_screen")
                )
            }
        }

        // Overlay is never displayed; writer is shown immediately
        composeTestRule.onNodeWithTag("start_overlay").assertDoesNotExist()
        composeTestRule.onNodeWithTag("writer_screen").assertIsDisplayed()
    }

    @Test
    fun reducedMotionFadesOver250ms() {
        var finished = false
        composeTestRule.mainClock.autoAdvance = false

        composeTestRule.setContent {
            StartOverlay(
                targets = referenceTargets,
                reduceMotion = true,
                onFinished = { finished = true },
                modifier = Modifier.testTag("start_overlay")
            )
        }

        // Before 250 ms, not finished
        composeTestRule.mainClock.advanceTimeBy(150)
        assertFalse("Reduced motion must not finish before 250 ms", finished)

        // At 250 ms, complete
        composeTestRule.mainClock.advanceTimeBy(100)
        assertTrue("Reduced motion must finish over 250 ms", finished)
    }
}
