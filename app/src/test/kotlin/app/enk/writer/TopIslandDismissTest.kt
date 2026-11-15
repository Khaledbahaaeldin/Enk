package app.enk.writer

import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.click
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import app.enk.design.effects.LocalEffects
import app.enk.design.effects.ResolvedEffects
import app.enk.design.shell.LocalReduceEffects
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.GraphicsMode

/** What closes the island besides its timer, and when it does not react at all. */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class TopIslandDismissTest {

    @get:Rule
    val rule = createAndroidComposeRule<ComponentActivity>()

    private fun show(interactive: Boolean = true) {
        rule.setContent {
            CompositionLocalProvider(
                LocalEffects provides ResolvedEffects.Light,
                LocalReduceEffects provides true
            ) {
                Box(modifier = Modifier.fillMaxSize()) {
                    TopIsland(
                        metaText = "p.1 · 85 / 3000",
                        lampOn = false,
                        lampColor = Color(0xffffd15c),
                        onToggleKeyboard = {},
                        onOpenBox = {},
                        onToggleLamp = {},
                        warmth = 25,
                        onWarmthChange = {},
                        intensity = 80,
                        onIntensityChange = {},
                        beam = 55,
                        onBeamChange = {},
                        onDetent = {},
                        lastKeyMs = 0L,
                        interactive = interactive
                    )
                }
            }
        }
        rule.waitForIdle()
    }

    @Test
    fun aTapFarFromTheIslandFoldsIt() {
        show()
        rule.onNodeWithTag("top_island").performClick()
        rule.waitForIdle()
        rule.onNodeWithTag("keyboard_switch").assertExists()

        // Well below the 320 x 110 island, on the paper: the whole screen is the outside.
        rule.onRoot().performTouchInput { click(Offset(centerX, height - 40f)) }
        rule.mainClock.advanceTimeBy(600)
        rule.waitForIdle()
        rule.onNodeWithTag("keyboard_switch").assertDoesNotExist()
    }

    @Test
    fun theBackButtonFoldsIt() {
        show()
        rule.onNodeWithTag("top_island").performClick()
        rule.waitForIdle()
        rule.onNodeWithTag("keyboard_switch").assertExists()

        rule.runOnUiThread { rule.activity.onBackPressedDispatcher.onBackPressed() }
        rule.mainClock.advanceTimeBy(600)
        rule.waitForIdle()
        rule.onNodeWithTag("keyboard_switch").assertDoesNotExist()
    }

    @Test
    fun anIslandThatIsNotInteractiveDoesNotOpen() {
        show(interactive = false)
        rule.onNodeWithTag("top_island").performClick()
        rule.waitForIdle()
        rule.onNodeWithTag("keyboard_switch").assertDoesNotExist()
    }
}
