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
        assertEquals(1f, RailTransitionMath.boxBrightness(0f), 0.001f)
        assertEquals(0.35f, RailTransitionMath.boxBrightness(1f), 0.001f)
        assertEquals(1f, RailTransitionMath.boxAlpha(0f), 0.001f)
        assertEquals(0f, RailTransitionMath.boxAlpha(1f), 0.001f)

        // Beat 2: Write button becomes the rail
        assertEquals(12f, RailTransitionMath.writeLeft(0f, startLeft = 12f), 0.001f)
        assertEquals(0f, RailTransitionMath.writeLeft(1f, startLeft = 12f), 0.001f)
        assertEquals(336f, RailTransitionMath.writeWidth(0f, 336f, 360f), 0.001f)
        assertEquals(360f, RailTransitionMath.writeWidth(1f, 336f, 360f), 0.001f)
        assertEquals(64f, RailTransitionMath.writeHeight(0f, 64f, 16f), 0.001f)
        assertEquals(16f, RailTransitionMath.writeHeight(1f, 64f, 16f), 0.001f)
        assertEquals(32f, RailTransitionMath.writeRadius(0f, 32f, 8f), 0.001f)
        assertEquals(8f, RailTransitionMath.writeRadius(1f, 32f, 8f), 0.001f)
        assertEquals(1f, RailTransitionMath.barAlpha(0f), 0.001f)
        assertEquals(0f, RailTransitionMath.barAlpha(1f), 0.001f)

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
    fun writeButtonBeatEndpoints() {
        val startLeft = 280f
        val startTop = 700f
        val startW = 62f
        val startH = 62f
        val startR = 31f
        val railY = 500f
        val screenW = 360f

        // At p = 0: equals the button bounds
        assertEquals(startLeft, RailTransitionMath.writeLeft(0f, startLeft), 0.001f)
        assertEquals(startW, RailTransitionMath.writeWidth(0f, startW, screenW), 0.001f)
        assertEquals(startH, RailTransitionMath.writeHeight(0f, startH, 16f), 0.001f)
        assertEquals(startTop, RailTransitionMath.writeTop(0f, startTop, railY), 0.001f)
        assertEquals(startR, RailTransitionMath.writeRadius(0f, startR, 8f), 0.001f)

        // At p = 1: equals the rail rectangle (left 0, full width, height 16, top railY - 8, radius 8)
        assertEquals(0f, RailTransitionMath.writeLeft(1f, startLeft), 0.001f)
        assertEquals(screenW, RailTransitionMath.writeWidth(1f, startW, screenW), 0.001f)
        assertEquals(16f, RailTransitionMath.writeHeight(1f, startH, 16f), 0.001f)
        assertEquals(railY - 8f, RailTransitionMath.writeTop(1f, startTop, railY), 0.001f)
        assertEquals(8f, RailTransitionMath.writeRadius(1f, startR, 8f), 0.001f)
    }

    @Test
    fun barSinksAndFades() {
        assertEquals(0f, RailTransitionMath.barTranslationY(0f), 0.001f)
        assertEquals(24f, RailTransitionMath.barTranslationY(1f), 0.001f)
        assertEquals(1f, RailTransitionMath.barAlpha(0f), 0.001f)
        assertEquals(0f, RailTransitionMath.barAlpha(1f), 0.001f)
    }

    @Test
    fun noBlurInRecede() {
        // RailTransitionMath no longer has boxBlurDp, and railRecede does not set renderEffect
        for (p in listOf(0f, 0.25f, 0.5f, 0.75f, 1f)) {
            val scale = RailTransitionMath.boxScale(p)
            val transY = RailTransitionMath.boxTranslationY(p)
            val alpha = RailTransitionMath.boxAlpha(p)
            val brightness = RailTransitionMath.boxBrightness(p)
            assertTrue("Scale in range: $scale", scale in 0.95f..1.0f)
            assertTrue("TransY in range: $transY", transY in -8.0f..0.0f)
            assertTrue("Alpha in range: $alpha", alpha in 0.0f..1.0f)
            assertTrue("Brightness in range: $brightness", brightness in 0.3f..1.0f)
        }
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

    // ---- the host: gates, rest values, flights

    private fun runHost(host: RailTransitionHost, target: Float) {
        rule.setContent {
            LaunchedEffect(Unit) { host.run(target, reduceEffects = false) }
        }
    }

    @Test
    fun typeOnGateIsClosedUntilTheDingThenOpenForGood() {
        val host = RailTransitionHost(initialP = 0f)
        host.begin(RailKind.Forward)
        assertFalse("closed at the start of a forward flight", host.typeOnGate())
        rule.mainClock.autoAdvance = false
        runHost(host, 1f)
        rule.mainClock.advanceTimeBy(16)
        assertTrue(host.state.progress.value < RailTransitionMath.DING_AT)
        var guard = 0
        while (host.state.progress.value < RailTransitionMath.DING_AT && guard++ < 400) {
            assertFalse("closed before the ding at p=${host.state.progress.value}", host.typeOnGate())
            rule.mainClock.advanceTimeBy(16)
        }
        rule.mainClock.advanceTimeBy(32)
        assertTrue("open from the ding on", host.typeOnGate())
        assertEquals(1, host.dingToken)
        rule.mainClock.advanceTimeBy(3000)
        rule.waitForIdle()
        assertTrue(host.typeOnGate())
        assertEquals("one ding, also across the overshoot", 1, host.dingToken)
    }

    @Test
    fun aKeyPressMidFlightDoesNotStopTheFlightAndTheTextIsCompleteAfterIt() {
        val host = RailTransitionHost(initialP = 0f)
        host.begin(RailKind.Forward)
        rule.mainClock.autoAdvance = false
        runHost(host, 1f)
        // A key press never touches the host: the flight runs on to 1 and the gate opens at the ding.
        var guard = 0
        while (host.state.progress.value < 0.7f && guard++ < 400) rule.mainClock.advanceTimeBy(16)
        assertFalse(host.typeOnGate())
        rule.mainClock.advanceTimeBy(3000)
        rule.waitForIdle()
        assertEquals(1f, host.state.progress.value, 0.01f)
        assertTrue(host.typeOnGate())
    }

    @Test
    fun leavingTheWriterNeverClosesTheGateAndNeverDings() {
        val host = RailTransitionHost(initialP = 1f)
        host.begin(RailKind.Reverse)
        assertTrue("the text stays drawn while the paper is pulled back", host.typeOnGate())
        rule.mainClock.autoAdvance = false
        runHost(host, 0f)
        rule.mainClock.advanceTimeBy(3000)
        rule.waitForIdle()
        assertTrue(host.typeOnGate())
        assertEquals(0, host.dingToken)
        assertEquals(0f, host.state.progress.value, 0.01f)
    }

    @Test
    fun outsideAFlightEachScreenSitsAtItsRestValue() {
        val host = RailTransitionHost(initialP = 0.4f)
        assertEquals("the writer at rest is fully there", 1f, host.writerP(), 0f)
        assertEquals("a tab screen at rest is untouched", 0f, host.boxP(), 0f)
        assertEquals(null, host.flightP())
        assertTrue(host.islandInteractive())
    }

    @Test
    fun aFinishedFlightKeepsItsEndValueSoTheExitingScreenDoesNotFlashBack() {
        val host = RailTransitionHost(initialP = 1f)
        host.begin(RailKind.Reverse)
        rule.mainClock.autoAdvance = false
        runHost(host, 0f)
        rule.mainClock.advanceTimeBy(3000)
        rule.waitForIdle()
        // The writer is still composed for a moment after the flight; it must stay at p = 0, not jump to 1.
        assertEquals(0f, host.writerP(), 0.01f)
        assertEquals(0f, host.boxP(), 0.01f)
        // The next route change that is not a Rail pair ends the flight.
        host.begin(RailKind.None)
        assertEquals(1f, host.writerP(), 0f)
    }

    @Test
    fun theIslandIsNotTappableBeforeNinetyFivePercent() {
        val host = RailTransitionHost(initialP = 0f)
        host.begin(RailKind.Forward)
        assertFalse(host.islandInteractive())
        rule.mainClock.autoAdvance = false
        runHost(host, 1f)
        rule.mainClock.advanceTimeBy(3000)
        rule.waitForIdle()
        assertTrue(host.islandInteractive())
    }

    @Test
    fun railPairsAreWriteFromATabAndTheIslandsBoxButton() {
        assertEquals(RailKind.Forward, railKind(Route.Box, Route.Writer))
        assertEquals(RailKind.Forward, railKind(Route.Find, Route.Writer))
        assertEquals(RailKind.Forward, railKind(Route.Setup, Route.Writer))
        assertEquals(RailKind.Reverse, railKind(Route.Writer, Route.Box))
        assertEquals("a card tap opens the writer with the slide", RailKind.None, railKind(Route.Box, Route.Note("a", 0)))
        assertEquals(RailKind.None, railKind(Route.Note("a", 0), Route.Box))
        assertEquals(RailKind.None, railKind(Route.Box, Route.Find))
        assertEquals(RailKind.None, railKind(Route.Writer, Route.Find))
    }
}
