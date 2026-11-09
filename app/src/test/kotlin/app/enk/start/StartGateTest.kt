package app.enk.start

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class StartGateTest {

    @Test
    fun gateRules() {
        // Plain cold launch that loaded within 1500 ms plays
        assertTrue(StartGate.shouldPlay(coldStart = true, launchedFromWidgetOrIntent = false, restoredState = false, loadMs = 400L))

        // Not a cold start (e.g. warm/hot start) does not play
        assertFalse(StartGate.shouldPlay(coldStart = false, launchedFromWidgetOrIntent = false, restoredState = false, loadMs = 400L))

        // Launched from widget or intent does not play
        assertFalse(StartGate.shouldPlay(coldStart = true, launchedFromWidgetOrIntent = true, restoredState = false, loadMs = 400L))

        // Restored state (e.g. process recreation / orientation) does not play
        assertFalse(StartGate.shouldPlay(coldStart = true, launchedFromWidgetOrIntent = false, restoredState = true, loadMs = 400L))

        // Load time exceeds 1500 ms hard cap does not play
        assertFalse(StartGate.shouldPlay(coldStart = true, launchedFromWidgetOrIntent = false, restoredState = false, loadMs = 1600L))

        // Boundary tests for loadMs
        assertTrue(StartGate.shouldPlay(coldStart = true, launchedFromWidgetOrIntent = false, restoredState = false, loadMs = 1500L))
        assertFalse(StartGate.shouldPlay(coldStart = true, launchedFromWidgetOrIntent = false, restoredState = false, loadMs = 1501L))
    }

    @Test
    fun reducedMotionIsAFade() {
        assertEquals(250, StartGate.reducedMotionDurationMs())
    }
}
