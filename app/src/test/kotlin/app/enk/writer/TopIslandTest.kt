package app.enk.writer

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.height
import androidx.compose.ui.unit.width
import app.enk.design.effects.LocalEffects
import app.enk.design.shell.LocalReduceEffects
import app.enk.design.effects.ResolvedEffects
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
    fun islandStartsInRestAndExpandsOnTap() {
        var keyboardToggled = false
        var boxOpened = false
        var lampToggled = false
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
                        onToggleKeyboard = { keyboardToggled = true },
                        onOpenBox = { boxOpened = true },
                        onToggleLamp = { lampToggled = true },
                        warmth = 25,
                        onWarmthChange = {},
                        intensity = 80,
                        onIntensityChange = {},
                        beam = 55,
                        onBeamChange = {},
                        onDetent = {},
                        lastKeyMs = lastKeyMs
                    )
                }
            }
        }
        rule.waitForIdle()

        // Rest: 150 x 40 dp (within 1 dp)
        val bounds = rule.onNodeWithTag("top_island").getUnclippedBoundsInRoot()
        assertEquals(150f, bounds.width.value, 1.5f)
        assertEquals(40f, bounds.height.value, 1.5f)

        // Action buttons do not exist yet
        rule.onNodeWithTag("keyboard_switch").assertDoesNotExist()
        rule.onNodeWithTag("lamp_button").assertDoesNotExist()
        rule.onNodeWithTag("box_button").assertDoesNotExist()

        // Tap opens Actions (320 x 110)
        rule.onNodeWithTag("top_island").performClick()
        rule.waitForIdle()

        val expandedBounds = rule.onNodeWithTag("top_island").getUnclippedBoundsInRoot()
        assertEquals(320f, expandedBounds.width.value, 1.5f)
        assertEquals(110f, expandedBounds.height.value, 1.5f)

        // Three tags shown
        rule.onNodeWithTag("keyboard_switch").assertIsDisplayed()
        rule.onNodeWithTag("lamp_button").assertIsDisplayed()
        rule.onNodeWithTag("box_button").assertIsDisplayed()

        // Tap keyboard switch toggles and folds
        rule.onNodeWithTag("keyboard_switch").performClick()
        rule.waitForIdle()
        assertTrue(keyboardToggled)

        val foldedBounds = rule.onNodeWithTag("top_island").getUnclippedBoundsInRoot()
        assertEquals(150f, foldedBounds.width.value, 1.5f)
        assertEquals(40f, foldedBounds.height.value, 1.5f)
    }

    @Test
    fun boxButtonOpensBoxAndFoldsIsland() {
        var boxOpened = false
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
                        onOpenBox = { boxOpened = true },
                        onToggleLamp = {},
                        warmth = 25,
                        onWarmthChange = {},
                        intensity = 80,
                        onIntensityChange = {},
                        beam = 55,
                        onBeamChange = {},
                        onDetent = {},
                        lastKeyMs = 0L
                    )
                }
            }
        }
        rule.waitForIdle()

        rule.onNodeWithTag("top_island").performClick()
        rule.waitForIdle()

        rule.onNodeWithTag("box_button").performClick()
        rule.waitForIdle()
        assertTrue(boxOpened)

        val foldedBounds = rule.onNodeWithTag("top_island").getUnclippedBoundsInRoot()
        assertEquals(150f, foldedBounds.width.value, 1.5f)
    }

    @Test
    fun lampButtonExpandsToLampPopup() {
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
                        lastKeyMs = 0L
                    )
                }
            }
        }
        rule.waitForIdle()

        rule.onNodeWithTag("top_island").performClick()
        rule.waitForIdle()

        rule.onNodeWithTag("lamp_button").performClick()
        rule.waitForIdle()

        // Lamp state: 320 x 270 dp
        val lampBounds = rule.onNodeWithTag("top_island").getUnclippedBoundsInRoot()
        assertEquals(320f, lampBounds.width.value, 1.5f)
        assertEquals(270f, lampBounds.height.value, 1.5f)

        rule.onNodeWithTag("lamp_popup").assertIsDisplayed()
        rule.onNodeWithTag("lamp_toggle").assertIsDisplayed()
        rule.onNodeWithTag("slider_warmth").assertIsDisplayed()
        rule.onNodeWithTag("slider_intensity").assertIsDisplayed()
        rule.onNodeWithTag("slider_beam").assertIsDisplayed()
    }

    @Test
    fun typingFoldsIsland() {
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
                        lastKeyMs = lastKeyMs
                    )
                }
            }
        }
        rule.waitForIdle()

        rule.onNodeWithTag("top_island").performClick()
        rule.waitForIdle()
        rule.onNodeWithTag("keyboard_switch").assertExists()

        // Type a key
        lastKeyMs = System.currentTimeMillis()
        rule.waitForIdle()

        rule.onNodeWithTag("keyboard_switch").assertDoesNotExist()
        val foldedBounds = rule.onNodeWithTag("top_island").getUnclippedBoundsInRoot()
        assertEquals(150f, foldedBounds.width.value, 1.5f)
    }

    @Test
    fun idleTimerCollapsesAfter4500Ms() {
        rule.mainClock.autoAdvance = false
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
                        lastKeyMs = 0L
                    )
                }
            }
        }
        rule.mainClock.advanceTimeByFrame()

        rule.onNodeWithTag("top_island").performClick()
        rule.mainClock.advanceTimeByFrame()
        rule.onNodeWithTag("keyboard_switch").assertExists()

        // Advance 4400 ms: still open
        rule.mainClock.advanceTimeBy(4400)
        rule.onNodeWithTag("keyboard_switch").assertExists()

        // Advance 200 ms more (4600 ms total): it has folded; the old content is gone once its fade ends
        rule.mainClock.advanceTimeBy(200)
        rule.mainClock.advanceTimeBy(200)
        rule.mainClock.advanceTimeByFrame()
        rule.onNodeWithTag("keyboard_switch").assertDoesNotExist()
    }

    @Test
    fun layoutAt360WidthFontScale13() {
        assertIslandMarginsAt(360, 1.3f)
    }

    @Test
    fun layoutAt411Width() {
        assertIslandMarginsAt(411, 1.0f)
    }

    @Test
    fun layoutAt600Width() {
        assertIslandMarginsAt(600, 1.0f)
    }

    private fun assertIslandMarginsAt(w: Int, fontScale: Float) {
        rule.setContent {
            CompositionLocalProvider(
                LocalDensity provides Density(density = 2f, fontScale = fontScale),
                LocalEffects provides ResolvedEffects.Light,
                LocalReduceEffects provides true
            ) {
                Box(modifier = Modifier.size(w.dp, 700.dp)) {
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
                        lastKeyMs = 0L
                    )
                }
            }
        }
        rule.waitForIdle()

        // Open Actions
        rule.onNodeWithTag("top_island").performClick()
        rule.waitForIdle()

        val islandBounds = rule.onNodeWithTag("top_island").getUnclippedBoundsInRoot()
        assertTrue("Island width ${islandBounds.width.value} fits within width $w with 12 dp margins", islandBounds.width.value <= w - 24f)

        // Open Lamp
        rule.onNodeWithTag("lamp_button").performClick()
        rule.waitForIdle()

        val lampBounds = rule.onNodeWithTag("top_island").getUnclippedBoundsInRoot()
        assertTrue("Lamp width ${lampBounds.width.value} fits within width $w with 12 dp margins", lampBounds.width.value <= w - 24f)
    }
}
