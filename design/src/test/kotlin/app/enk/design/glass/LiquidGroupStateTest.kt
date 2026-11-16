package app.enk.design.glass

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.unit.dp
import app.enk.design.shell.GlassSource
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class LiquidGroupStateTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun mergesWhenClose() {
        var shapes by mutableStateOf(
            listOf(
                LiquidShape(50f, 20f, 50f, 20f, 20f),
                LiquidShape(175f, 20f, 50f, 20f, 20f) // 25 px gap -> apart
            )
        )
        var isPressed by mutableStateOf(false)
        lateinit var state: LiquidGroupState

        composeTestRule.setContent {
            CompositionLocalProvider(LocalDensity provides Density(1f)) {
                state = rememberLiquidGroupState()
                LiquidGroup(
                    state = state,
                    shapes = { shapes },
                    spec = LiquidSpecs.Island,
                    source = GlassSource(null),
                    pressed = { isPressed },
                    modifier = Modifier.size(300.dp),
                    mergeDistance = 24.dp
                )
            }
        }
        composeTestRule.waitForIdle()
        assertFalse("Should be separate when gap is 25px", state.merging)

        // Move closer to 20 px gap
        shapes = listOf(
            LiquidShape(50f, 20f, 50f, 20f, 20f),
            LiquidShape(170f, 20f, 50f, 20f, 20f)
        )
        composeTestRule.waitForIdle()
        assertTrue("Should be merging when gap is 20px", state.merging)
    }

    @Test
    fun separatesWhenApart() {
        var shapes by mutableStateOf(
            listOf(
                LiquidShape(50f, 20f, 50f, 20f, 20f),
                LiquidShape(170f, 20f, 50f, 20f, 20f) // 20 px gap -> merging
            )
        )
        var isPressed by mutableStateOf(false)
        lateinit var state: LiquidGroupState

        composeTestRule.setContent {
            CompositionLocalProvider(LocalDensity provides Density(1f)) {
                state = rememberLiquidGroupState()
                LiquidGroup(
                    state = state,
                    shapes = { shapes },
                    spec = LiquidSpecs.Island,
                    source = GlassSource(null),
                    pressed = { isPressed },
                    modifier = Modifier.size(300.dp),
                    mergeDistance = 24.dp
                )
            }
        }
        composeTestRule.waitForIdle()
        assertTrue("Should start merged", state.merging)

        // Move apart
        shapes = listOf(
            LiquidShape(50f, 20f, 50f, 20f, 20f),
            LiquidShape(200f, 20f, 50f, 20f, 20f)
        )
        composeTestRule.waitForIdle()
        assertFalse("Should separate when gap > mergeDistance and not pressed", state.merging)
    }

    @Test
    fun swapWaitsForRelease() {
        var shapes by mutableStateOf(
            listOf(
                LiquidShape(50f, 20f, 50f, 20f, 20f),
                LiquidShape(170f, 20f, 50f, 20f, 20f) // 20 px gap -> merging
            )
        )
        var isPressed by mutableStateOf(true)
        lateinit var state: LiquidGroupState

        composeTestRule.setContent {
            CompositionLocalProvider(LocalDensity provides Density(1f)) {
                state = rememberLiquidGroupState()
                LiquidGroup(
                    state = state,
                    shapes = { shapes },
                    spec = LiquidSpecs.Island,
                    source = GlassSource(null),
                    pressed = { isPressed },
                    modifier = Modifier.size(300.dp),
                    mergeDistance = 24.dp
                )
            }
        }
        composeTestRule.waitForIdle()
        assertTrue("Should be merged", state.merging)

        // Move apart while still pressed
        shapes = listOf(
            LiquidShape(50f, 20f, 50f, 20f, 20f),
            LiquidShape(250f, 20f, 50f, 20f, 20f)
        )
        composeTestRule.waitForIdle()
        assertTrue("Should stay merged while pressed even if apart", state.merging)

        // Release press
        isPressed = false
        composeTestRule.waitForIdle()
        assertFalse("Should un-merge on release when apart", state.merging)
    }

    @Test
    fun neverBothDrawn() {
        var shapes by mutableStateOf(
            listOf(
                LiquidShape(50f, 20f, 50f, 20f, 20f),
                LiquidShape(175f, 20f, 50f, 20f, 20f)
            )
        )
        var bothDrawnCount = 0
        lateinit var state: LiquidGroupState

        composeTestRule.setContent {
            CompositionLocalProvider(LocalDensity provides Density(1f)) {
                state = rememberLiquidGroupState()
                var probeDrawnInFrame = false
                var groupDrawnInFrame = false

                Box(Modifier.size(300.dp)) {
                    // Probe individual surface
                    Box(
                        Modifier
                            .size(100.dp)
                            .drawWithContent {
                                if (!state.merging) {
                                    probeDrawnInFrame = true
                                    drawContent()
                                }
                            }
                    )
                    // LiquidGroup
                    LiquidGroup(
                        state = state,
                        shapes = { shapes },
                        spec = LiquidSpecs.Island,
                        source = GlassSource(null),
                        pressed = { false },
                        modifier = Modifier
                            .size(300.dp)
                            .drawWithContent {
                                if (state.merging) {
                                    groupDrawnInFrame = true
                                    drawContent()
                                }
                            }
                    )
                }
                if (probeDrawnInFrame && groupDrawnInFrame) {
                    bothDrawnCount++
                }
            }
        }

        // Run over 60 frames moving shapes across the merge threshold
        for (i in 0 until 60) {
            // Gap moves between 10px (merged) and 35px (separated)
            val gap = 10f + (i % 26).toFloat()
            shapes = listOf(
                LiquidShape(50f, 20f, 50f, 20f, 20f),
                LiquidShape(150f + gap, 20f, 50f, 20f, 20f)
            )
            composeTestRule.waitForIdle()
        }

        assertEquals("Probe surface and group must never both be drawn in the same frame", 0, bothDrawnCount)
    }
}
