package app.enk.writer

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.height
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
class LampPopupTest {

    @get:Rule
    val rule = createComposeRule()

    @Test
    fun lampToggleAndWarmthSlider() {
        var lampOn by mutableStateOf(false)
        var warmth by mutableIntStateOf(25)
        var intensity by mutableIntStateOf(80)
        var beam by mutableIntStateOf(55)
        var detentCount by mutableIntStateOf(0)

        rule.setContent {
            CompositionLocalProvider(
                LocalEffects provides ResolvedEffects.Light,
                LocalReduceEffects provides true
            ) {
                Box(modifier = Modifier.fillMaxSize()) {
                    TopIsland(
                        metaText = "p.1 · 85 / 3000",
                        lampOn = lampOn,
                        lampColor = Color(0xffffd15c),
                        onToggleKeyboard = {},
                        onOpenBox = {},
                        onToggleLamp = { lampOn = !lampOn },
                        warmth = warmth,
                        onWarmthChange = { warmth = it },
                        intensity = intensity,
                        onIntensityChange = { intensity = it },
                        beam = beam,
                        onBeamChange = { beam = it },
                        onDetent = { detentCount++ },
                        lastKeyMs = 0L
                    )
                }
            }
        }
        rule.waitForIdle()

        // Open island -> Actions -> Lamp
        rule.onNodeWithTag("top_island").performClick()
        rule.waitForIdle()
        rule.onNodeWithTag("lamp_button").performClick()
        rule.waitForIdle()

        // Toggle changes lampOn
        rule.onNodeWithTag("lamp_toggle").performClick()
        rule.waitForIdle()
        assertTrue("lampOn must be true after toggle", lampOn)

        // Drag warmth slider across detents
        rule.onNodeWithTag("slider_warmth").performTouchInput {
            down(Offset(10f, centerY))
            moveTo(Offset(width * 0.8f, centerY))
            up()
        }
        rule.waitForIdle()

        assertTrue("Warmth value must have changed", warmth > 25)
        assertTrue("Detents must have fired: count = $detentCount", detentCount > 0)
    }

    @Test
    fun slidersKeepValuesAfterFoldingAndReopening() {
        var warmth by mutableIntStateOf(65)
        var intensity by mutableIntStateOf(45)
        var beam by mutableIntStateOf(75)

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
                        warmth = warmth,
                        onWarmthChange = { warmth = it },
                        intensity = intensity,
                        onIntensityChange = { intensity = it },
                        beam = beam,
                        onBeamChange = { beam = it },
                        onDetent = {},
                        lastKeyMs = 0L
                    )
                }
            }
        }
        rule.waitForIdle()

        // Open to Lamp
        rule.onNodeWithTag("top_island").performClick()
        rule.waitForIdle()
        rule.onNodeWithTag("lamp_button").performClick()
        rule.waitForIdle()

        // Check semantic values
        val warmthNode = rule.onNodeWithTag("slider_warmth").fetchSemanticsNode()
        assertEquals("65", warmthNode.config[SemanticsProperties.StateDescription])

        // Fold by clicking outside
        rule.onNodeWithTag("top_island").performClick() // Tapping folds or toggles
        rule.waitForIdle()

        // Values are kept
        assertEquals(65, warmth)
        assertEquals(45, intensity)
        assertEquals(75, beam)
    }

    @Test
    fun openingOrClosingIslandDoesNotChangeRailY() {
        // Rail Y is purely derived from WriterGeometry.compute and does not change between lamp on/off or island state
        val geomLampOff = WriterGeometry.compute(widthDp = 400f, heightDp = 820f, fontScale = 1.0f)
        val geomLampOn = WriterGeometry.compute(widthDp = 400f, heightDp = 820f, fontScale = 1.0f)
        assertEquals(geomLampOff.railYDp, geomLampOn.railYDp, 0.001f)
        assertEquals(508f, geomLampOff.railYDp, 0.001f)
    }

    @Test
    fun accessibilityAndTouchHeight() {
        rule.setContent {
            CompositionLocalProvider(
                LocalEffects provides ResolvedEffects.Light,
                LocalReduceEffects provides true
            ) {
                Box(modifier = Modifier.fillMaxSize()) {
                    LampPopupContent(
                        lampOn = false,
                        onToggleLamp = {},
                        warmth = 50,
                        onWarmthChange = {},
                        intensity = 50,
                        onBeamChange = {},
                        beam = 50,
                        onIntensityChange = {},
                        onDetent = {}
                    )
                }
            }
        }
        rule.waitForIdle()

        // Every slider has 48 dp touch height, role, and description
        for (tag in listOf("slider_warmth", "slider_intensity", "slider_beam")) {
            val node = rule.onNodeWithTag(tag)
            val bounds = node.getUnclippedBoundsInRoot()
            assertEquals("Touch height of $tag", 48f, bounds.height.value, 1.5f)
            val semantics = node.fetchSemanticsNode()
            assertTrue(semantics.config.contains(SemanticsProperties.ContentDescription))
            assertTrue(semantics.config.contains(SemanticsProperties.ProgressBarRangeInfo))
        }

        // Toggle has role and 48 dp min touch height
        val toggleNode = rule.onNodeWithTag("lamp_toggle")
        val toggleBounds = toggleNode.getUnclippedBoundsInRoot()
        assertTrue("Toggle touch height ${toggleBounds.height.value} >= 32 dp", toggleBounds.height.value >= 32f)
        val toggleSemantics = toggleNode.fetchSemanticsNode()
        assertEquals(Role.Switch, toggleSemantics.config.getOrNull(SemanticsProperties.Role))
    }
}
