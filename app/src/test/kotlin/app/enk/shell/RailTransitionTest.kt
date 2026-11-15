package app.enk.shell

import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.test.junit4.createComposeRule
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class RailTransitionTest {

    @get:Rule
    val rule = createComposeRule()

    @Test
    fun mathEndpointsAndMidpoints() {
        // seg
        assertEquals(0f, RailTransitionMath.seg(0f, 0.2f, 0.8f), 0.0001f)
        assertEquals(0f, RailTransitionMath.seg(0.2f, 0.2f, 0.8f), 0.0001f)
        assertEquals(0.5f, RailTransitionMath.seg(0.5f, 0.2f, 0.8f), 0.0001f)
        assertEquals(1f, RailTransitionMath.seg(0.8f, 0.2f, 0.8f), 0.0001f)
        assertEquals(1f, RailTransitionMath.seg(1f, 0.2f, 0.8f), 0.0001f)

        // ez: t * t * (3 - 2 * t)
        assertEquals(0f, RailTransitionMath.ez(0f), 0.0001f)
        assertEquals(0.5f, RailTransitionMath.ez(0.5f), 0.0001f)
        assertEquals(1f, RailTransitionMath.ez(1f), 0.0001f)

        // out: 1 - (1 - t)^3
        assertEquals(0f, RailTransitionMath.out(0f), 0.0001f)
        assertEquals(1f - 0.125f, RailTransitionMath.out(0.5f), 0.0001f)
        assertEquals(1f, RailTransitionMath.out(1f), 0.0001f)

        // back: 1 + 2.9 * (t - 1)^3 + 1.9 * (t - 1)^2
        assertEquals(1f + 2.9f * (-1f) + 1.9f * 1f, RailTransitionMath.back(0f), 0.0001f)
        assertEquals(1f, RailTransitionMath.back(1f), 0.0001f)
    }

    @Test
    fun beatValuesAtEndpointsP0AndP1() {
        // Beat 1: Box recedes
        assertEquals(1f, RailTransitionMath.boxScale(0f), 0.001f)
        assertEquals(0.96f, RailTransitionMath.boxScale(1f), 0.001f)
        assertEquals(0f, RailTransitionMath.boxTranslationY(0f), 0.001f)
        assertEquals(-8f, RailTransitionMath.boxTranslationY(1f), 0.001f)
        assertEquals(0f, RailTransitionMath.boxBlurDp(0f), 0.001f)
        assertEquals(8f, RailTransitionMath.boxBlurDp(1f), 0.001f)
        assertEquals(1f, RailTransitionMath.boxBrightness(0f), 0.001f)
        assertEquals(0.35f, RailTransitionMath.boxBrightness(1f), 0.001f)
        assertEquals(1f, RailTransitionMath.boxAlpha(0f), 0.001f)
        assertEquals(0f, RailTransitionMath.boxAlpha(1f), 0.001f)

        // Beat 2: Nav bar becomes the rail
        assertEquals(12f, RailTransitionMath.navLeft(0f, startLeft = 12f), 0.001f)
        assertEquals(0f, RailTransitionMath.navLeft(1f, startLeft = 12f), 0.001f)
        assertEquals(336f, RailTransitionMath.navWidth(0f, 336f, 360f), 0.001f)
        assertEquals(360f, RailTransitionMath.navWidth(1f, 336f, 360f), 0.001f)
        assertEquals(64f, RailTransitionMath.navHeight(0f, 64f, 16f), 0.001f)
        assertEquals(16f, RailTransitionMath.navHeight(1f, 64f, 16f), 0.001f)
        assertEquals(32f, RailTransitionMath.navRadius(0f, 32f, 8f), 0.001f)
        assertEquals(8f, RailTransitionMath.navRadius(1f, 32f, 8f), 0.001f)
        assertEquals(1f, RailTransitionMath.navContentAlpha(0f), 0.001f)
        assertEquals(0f, RailTransitionMath.navContentAlpha(1f), 0.001f)

        // Beat 3: Glass to steel
        assertEquals(0f, RailTransitionMath.steelRailAlpha(0f), 0.001f)
        assertEquals(1f, RailTransitionMath.steelRailAlpha(1f), 0.001f)

        // Beat 4: Paper is fed & keyboard rises
        assertEquals(0f, RailTransitionMath.paperClipU(0f), 0.001f)
        assertEquals(1f, RailTransitionMath.paperClipU(1f), 0.001f)
        assertEquals(240f, RailTransitionMath.keyboardTranslationY(0f), 0.001f)
        assertEquals(0f, RailTransitionMath.keyboardTranslationY(1f), 0.001f)

        // Beat 5: Carriage lands
        assertEquals(-80f * (1f - RailTransitionMath.back(0f)), RailTransitionMath.carriageTranslationY(0f), 0.001f)
        assertEquals(0f, RailTransitionMath.carriageTranslationY(1f), 0.001f)
        assertEquals(0f, RailTransitionMath.carriageAlpha(0f), 0.001f)
        assertEquals(1f, RailTransitionMath.carriageAlpha(1f), 0.001f)

        // Beat 6: Island and caret
        assertEquals(0.55f + 0.45f * RailTransitionMath.back(0f), RailTransitionMath.islandScale(0f), 0.001f)
        assertEquals(1f, RailTransitionMath.islandScale(1f), 0.001f)
        assertEquals(0f, RailTransitionMath.islandAlpha(0f), 0.001f)
        assertEquals(1f, RailTransitionMath.islandAlpha(1f), 0.001f)
        assertEquals(0f, RailTransitionMath.caretAlpha(0f), 0.001f)
        assertEquals(1f, RailTransitionMath.caretAlpha(1f), 0.001f)
    }

    @Test
    fun dingFiresOnceGoingForwardAndNeverInReverse() {
        val state = RailTransitionState(initialP = 0f)
        var dings = 0

        rule.setContent {
            LaunchedEffect(Unit) {
                state.animateTo(1f, reduceEffects = true) {
                    dings++
                }
                state.animateTo(0f, reduceEffects = true) {
                    dings++
                }
            }
        }
        rule.waitForIdle()

        assertEquals("Ding fires exactly once going forward, never in reverse", 1, dings)
        assertFalse("Landed is false after reverse to 0", state.landed)
    }

    @Test
    fun interruptingAtMidpointKeepsProgressContinuous() {
        val state = RailTransitionState(initialP = 0f)

        rule.setContent {
            LaunchedEffect(Unit) {
                state.progress.snapTo(0.5f)
                state.animateTo(0f, reduceEffects = true)
            }
        }
        rule.waitForIdle()

        assertEquals(0f, state.progress.value, 0.001f)
        assertFalse(state.landed)
    }
}
