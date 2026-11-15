package app.enk.design.motion

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.unit.dp
import app.enk.design.shell.LocalReduceEffects
import app.enk.design.theme.EnkShellTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class PressFeedbackTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun touchDownScalesNodeAndClickableStillFiresOnce() {
        var clickCount = 0

        composeTestRule.setContent {
            CompositionLocalProvider(LocalReduceEffects provides false) {
                EnkShellTheme {
                    Box(
                        modifier = Modifier
                            .size(100.dp)
                            .testTag("press_box")
                            .pressFeedback(depth = PressDepth.Button)
                            .clickable { clickCount++ }
                    )
                }
            }
        }

        // Tap once
        composeTestRule.onNodeWithTag("press_box").performClick()
        assertEquals("Clickable must fire exactly once", 1, clickCount)

        // Perform press down, hold, release
        composeTestRule.onNodeWithTag("press_box").performTouchInput {
            down(center)
        }
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithTag("press_box").performTouchInput {
            up()
        }
        composeTestRule.waitForIdle()
        assertEquals("Total clicks should be 2 after full touch down and up", 2, clickCount)
    }

    @Test
    fun reduceEffectsDoesNothing() {
        var clickCount = 0

        composeTestRule.setContent {
            CompositionLocalProvider(LocalReduceEffects provides true) {
                EnkShellTheme {
                    Box(
                        modifier = Modifier
                            .size(100.dp)
                            .testTag("press_box_reduced")
                            .pressFeedback(depth = PressDepth.Button)
                            .clickable { clickCount++ }
                    )
                }
            }
        }

        composeTestRule.onNodeWithTag("press_box_reduced").performClick()
        assertEquals("Clickable must fire once with reduced effects", 1, clickCount)
    }
}
