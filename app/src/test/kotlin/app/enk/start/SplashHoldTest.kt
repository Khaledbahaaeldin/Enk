package app.enk.start

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class SplashHoldTest {

    @Test
    fun holdsWhileLoadingAndReleasesTheMomentItIsLoaded() {
        val hold = SplashHold(startMs = 1000)
        assertTrue(hold.shouldHold(loaded = false, nowMs = 1000))
        assertTrue(hold.shouldHold(loaded = false, nowMs = 1400))
        assertFalse(hold.shouldHold(loaded = true, nowMs = 1450))
        assertEquals(450L, hold.loadMs)
    }

    @Test
    fun neverHoldsPastTheCapEvenIfNothingLoads() {
        val hold = SplashHold(startMs = 0)
        assertTrue(hold.shouldHold(false, 1499))
        assertFalse("at the cap the splash must go", hold.shouldHold(false, 1500))
        assertFalse(hold.shouldHold(false, 60_000))
        assertEquals(1500L, hold.loadMs)
    }

    @Test
    fun theLoadTimeIsTakenOnceAndNeverChanges() {
        val hold = SplashHold(startMs = 0)
        assertNull(hold.loadMs)
        hold.shouldHold(true, 300)
        hold.shouldHold(true, 9000)
        assertEquals(300L, hold.loadMs)
    }

    @Test
    fun theLoadTimeIsNullUntilSomethingDecidesIt() {
        val hold = SplashHold(startMs = 0)
        hold.shouldHold(false, 100)
        assertNull(hold.loadMs)
    }

    @Test
    fun aLoadAtTheCapCountsAsLateSoTheAnimationIsSkipped() {
        val hold = SplashHold(startMs = 0)
        hold.shouldHold(true, 1501)
        assertFalse(StartGate.shouldPlay(true, false, false, hold.loadMs!!))
    }
}
