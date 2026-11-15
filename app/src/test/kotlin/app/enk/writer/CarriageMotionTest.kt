package app.enk.writer

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.Snapshot
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class CarriageMotionTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun carriageTickAnimatesToTarget() {
        var centerXDp by mutableFloatStateOf(100f)
        var mode by mutableStateOf(CarriageMotion.Rest)

        composeTestRule.setContent {
            Carriage(
                railYDp = 300f,
                centerXDp = centerXDp,
                mode = mode,
                slugChar = 'A',
                night = false,
                reduceEffects = false
            )
        }

        mode = CarriageMotion.Tick
        centerXDp = 120f
        composeTestRule.waitForIdle()

        val bounds = composeTestRule.onNodeWithTag("carriage").getUnclippedBoundsInRoot()
        val currentCenter = (bounds.left.value + bounds.right.value) / 2f
        assertEquals(120f, currentCenter, 1.0f)
    }

    @Test
    fun carriageReturnAnimatesToTarget() {
        var centerXDp by mutableFloatStateOf(200f)
        var mode by mutableStateOf(CarriageMotion.Rest)

        composeTestRule.setContent {
            Carriage(
                railYDp = 300f,
                centerXDp = centerXDp,
                mode = mode,
                slugChar = null,
                night = false,
                reduceEffects = false
            )
        }

        mode = CarriageMotion.Return
        centerXDp = 50f
        Snapshot.sendApplyNotifications()
        composeTestRule.waitForIdle()

        val bounds = composeTestRule.onNodeWithTag("carriage").getUnclippedBoundsInRoot()
        val currentCenter = (bounds.left.value + bounds.right.value) / 2f
        assertEquals(50f, currentCenter, 1.0f)
    }

    @Test
    fun carriageReduceEffectsSnapsDirectly() {
        var centerXDp by mutableFloatStateOf(100f)
        var mode by mutableStateOf(CarriageMotion.Rest)

        composeTestRule.setContent {
            Carriage(
                railYDp = 300f,
                centerXDp = centerXDp,
                mode = mode,
                slugChar = 'A',
                night = false,
                reduceEffects = true
            )
        }

        mode = CarriageMotion.Tick
        centerXDp = 150f
        composeTestRule.waitForIdle()

        val bounds = composeTestRule.onNodeWithTag("carriage").getUnclippedBoundsInRoot()
        val currentCenter = (bounds.left.value + bounds.right.value) / 2f
        assertEquals(150f, currentCenter, 0.5f)
    }

    private fun centerX(): Float {
        val b = composeTestRule.onNodeWithTag("carriage").getUnclippedBoundsInRoot()
        return (b.left.value + b.right.value) / 2f
    }

    /** Runs the clock [frames] frames (16 ms each) and returns the carriage centre after each one. */
    private fun sample(frames: Int): List<Float> = (1..frames).map {
        composeTestRule.mainClock.advanceTimeByFrame()
        centerX()
    }

    @Test
    fun tickMovesMostOfAColumnInThirtyMsAndOvershootsOnTheWay() {
        var centerXDp by mutableFloatStateOf(100f)
        var mode by mutableStateOf(CarriageMotion.Rest)
        composeTestRule.mainClock.autoAdvance = false
        composeTestRule.setContent {
            Carriage(railYDp = 300f, centerXDp = centerXDp, mode = mode, slugChar = null, night = false, reduceEffects = false)
        }
        repeat(3) { composeTestRule.mainClock.advanceTimeByFrame() }
        val column = 13f
        val x0 = centerX() // the box centre is not exactly the requested x; measure from where it really is
        mode = CarriageMotion.Tick
        centerXDp = 100f + column
        Snapshot.sendApplyNotifications()
        // The effect starts a frame or two after the state change: count from the first frame that moves.
        var guard = 0
        while (kotlin.math.abs(centerX() - x0) < 0.01f && guard++ < 10) composeTestRule.mainClock.advanceTimeByFrame()
        val samples = sample(8) // 16 .. 128 ms after the first moving frame
        assertTrue("moved more than 60% of the column 32 ms after it started: ${samples[1]}", samples[1] - x0 > 0.6f * column)
        val overshoot = (samples.max() - (x0 + column)) / column
        assertTrue("peak overshoot between 5% and 25% of the column: $overshoot", overshoot in 0.05f..0.25f)
        assertEquals(x0 + column, samples.last(), 0.5f)
    }

    @Test
    fun returnOverAWideDistanceOvershootsLittleAndSettlesWithinSevenHundredMs() {
        var centerXDp by mutableFloatStateOf(200f)
        var mode by mutableStateOf(CarriageMotion.Rest)
        composeTestRule.mainClock.autoAdvance = false
        composeTestRule.setContent {
            Carriage(railYDp = 300f, centerXDp = centerXDp, mode = mode, slugChar = null, night = false, reduceEffects = false)
        }
        composeTestRule.mainClock.advanceTimeByFrame()
        mode = CarriageMotion.Return
        centerXDp = 50f
        Snapshot.sendApplyNotifications()
        composeTestRule.mainClock.advanceTimeByFrame()
        val samples = sample(44) // about 700 ms
        val overshootDp = 50f - samples.min()
        assertTrue("overshoot under 6 dp: $overshootDp", overshootDp in 0f..6f)
        assertEquals(50f, samples.last(), 0.5f)
        assertTrue("still travelling at 48 ms: ${samples[2]}", samples[2] > 60f)
    }

    @Test
    fun restSnapsWithoutAnimating() {
        var centerXDp by mutableFloatStateOf(100f)
        composeTestRule.mainClock.autoAdvance = false
        composeTestRule.setContent {
            Carriage(railYDp = 300f, centerXDp = centerXDp, mode = CarriageMotion.Rest, slugChar = null, night = false, reduceEffects = false)
        }
        repeat(3) { composeTestRule.mainClock.advanceTimeByFrame() }
        centerXDp = 180f
        Snapshot.sendApplyNotifications()
        repeat(3) { composeTestRule.mainClock.advanceTimeByFrame() }
        assertEquals("Rest moves in one jump, within a few frames", 180f, centerX(), 0.5f)
    }

    @Test
    fun theCarriageGlidesFromWhereTheSweepEndedInsteadOfJumping() {
        var sweepX by mutableStateOf<Float?>(40f)
        var mode by mutableStateOf(CarriageMotion.Rest)
        composeTestRule.mainClock.autoAdvance = false
        composeTestRule.setContent {
            Carriage(
                railYDp = 300f, centerXDp = 200f, mode = mode, slugChar = null, night = false, reduceEffects = false,
                carriageSweepXDp = { sweepX }
            )
        }
        composeTestRule.mainClock.advanceTimeByFrame()
        assertEquals(40f, centerX(), 0.5f)
        sweepX = null
        mode = CarriageMotion.Return
        Snapshot.sendApplyNotifications()
        composeTestRule.mainClock.advanceTimeByFrame()
        composeTestRule.mainClock.advanceTimeByFrame()
        val early = centerX()
        assertTrue("a glide starts near the sweep's end ($early), it does not jump to 200", early < 150f)
        composeTestRule.mainClock.advanceTimeBy(900)
        assertEquals(200f, centerX(), 0.5f)
    }
}
