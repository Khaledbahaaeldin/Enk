package app.enk.writer

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.height
import androidx.compose.ui.unit.width
import app.enk.design.effects.LocalEffects
import app.enk.design.effects.ResolvedEffects
import app.enk.design.shell.LocalReduceEffects
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class TopIslandTest {

    @get:Rule
    val rule = createComposeRule()

    @Test
    fun restShowsCapsule150x40WithMetaAndLampDot() {
        rule.setContent {
            CompositionLocalProvider(
                LocalEffects provides ResolvedEffects.Light,
                LocalReduceEffects provides true
            ) {
                Box(modifier = Modifier.fillMaxSize()) {
                    TopIsland(
                        metaText = "p.1 · 85 / 3000",
                        lampOn = true,
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
                        initialState = IslandState.Rest
                    )
                }
            }
        }
        rule.waitForIdle()

        // Rest: 150 x 40 dp
        val bounds = rule.onNodeWithTag("top_island").getUnclippedBoundsInRoot()
        assertEquals(150f, bounds.width.value, 1.5f)
        assertEquals(40f, bounds.height.value, 1.5f)

        // Meta text displayed
        rule.onNodeWithText("p.1 · 85 / 3000").assertIsDisplayed()

        // Action controls do not exist in Rest
        rule.onNodeWithTag("keyboard_switch").assertDoesNotExist()
        rule.onNodeWithTag("lamp_button").assertDoesNotExist()
        rule.onNodeWithTag("box_button").assertDoesNotExist()
        rule.onNodeWithTag("lamp_popup").assertDoesNotExist()
    }

    @Test
    fun tapRestOpensToolbar200x46AndLampButton46() {
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
                        initialState = IslandState.Rest
                    )
                }
            }
        }
        rule.waitForIdle()

        rule.onNodeWithTag("top_island").performClick()
        rule.waitForIdle()

        // Toolbar: 200 x 46 dp
        val toolbarBounds = rule.onNodeWithTag("top_island").getUnclippedBoundsInRoot()
        assertEquals(200f, toolbarBounds.width.value, 2.0f)
        assertEquals(46f, toolbarBounds.height.value, 2.0f)

        // Round Lamp button: 46 x 46 dp
        val lampNode = rule.onNodeWithTag("lamp_button")
        lampNode.assertIsDisplayed()
        val lampBounds = lampNode.getUnclippedBoundsInRoot()
        assertEquals(46f, lampBounds.width.value, 2.0f)
        assertEquals(46f, lampBounds.height.value, 2.0f)

        // 12 dp gap between toolbar and lamp button
        val gap = lampBounds.left.value - toolbarBounds.right.value
        assertEquals(12f, gap, 2.0f)

        // Keyboard and Box buttons exist inside toolbar
        rule.onNodeWithTag("keyboard_switch").assertIsDisplayed()
        rule.onNodeWithTag("box_button").assertIsDisplayed()
    }

    @Test
    fun tapLampOpensPanel280x230AlignedRight() {
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
                        initialState = IslandState.Actions
                    )
                }
            }
        }
        rule.waitForIdle()

        val lampNode = rule.onNodeWithTag("lamp_button")
        val lampBoundsBefore = lampNode.getUnclippedBoundsInRoot()

        lampNode.performClick()
        rule.waitForIdle()

        // Panel: 280 x 230 dp
        val popupNode = rule.onNodeWithTag("lamp_popup")
        popupNode.assertIsDisplayed()
        val popupBounds = popupNode.getUnclippedBoundsInRoot()
        assertEquals(280f, popupBounds.width.value, 2.5f)
        assertEquals(230f, popupBounds.height.value, 2.5f)

        // Right edge aligned with Lamp button's right edge
        assertEquals(lampBoundsBefore.right.value, popupBounds.right.value, 2.5f)

        // Top edge 8 dp below the Lamp button
        val verticalGap = popupBounds.top.value - lampBoundsBefore.bottom.value
        assertEquals(8f, verticalGap, 2.0f)
    }

    @Test
    fun tapOutsideClosesBackToRest() {
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
                        initialState = IslandState.Actions
                    )
                }
            }
        }
        rule.waitForIdle()

        rule.onNodeWithTag("lamp_button").assertIsDisplayed()

        // Tap far outside on the screen
        rule.onRoot().performTouchInput { click(Offset(centerX, height - 50f)) }
        rule.waitForIdle()

        // Folds back to Rest
        val bounds = rule.onNodeWithTag("top_island").getUnclippedBoundsInRoot()
        assertEquals(150f, bounds.width.value, 2.0f)
        assertEquals(40f, bounds.height.value, 2.0f)
        rule.onNodeWithTag("lamp_button").assertDoesNotExist()
    }

    @Test
    fun typingClosesBackToRest() {
        var lastKeyMs by mutableLongStateOf(0L)

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
                        lastKeyMs = lastKeyMs,
                        initialState = IslandState.Actions
                    )
                }
            }
        }
        rule.waitForIdle()

        rule.onNodeWithTag("lamp_button").assertIsDisplayed()

        // Typing event
        lastKeyMs = 12345L
        rule.waitForIdle()

        // Folds back to Rest
        val bounds = rule.onNodeWithTag("top_island").getUnclippedBoundsInRoot()
        assertEquals(150f, bounds.width.value, 2.0f)
        assertEquals(40f, bounds.height.value, 2.0f)
        rule.onNodeWithTag("lamp_button").assertDoesNotExist()
    }

    @Test
    fun lampLitGlowsInLampColour() {
        rule.setContent {
            CompositionLocalProvider(
                LocalEffects provides ResolvedEffects.Light,
                LocalReduceEffects provides true
            ) {
                Box(modifier = Modifier.fillMaxSize()) {
                    TopIsland(
                        metaText = "p.1 · 85 / 3000",
                        lampOn = true,
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
                        initialState = IslandState.Actions
                    )
                }
            }
        }
        rule.waitForIdle()

        val lampNode = rule.onNodeWithTag("lamp_button")
        lampNode.assertIsDisplayed()
        assertEquals("On", lampNode.fetchSemanticsNode().config[SemanticsProperties.StateDescription])
    }
}
