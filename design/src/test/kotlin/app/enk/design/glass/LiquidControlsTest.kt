package app.enk.design.glass

import androidx.compose.ui.unit.dp
import app.enk.design.shell.ShellSegmentedMath
import app.enk.design.shell.ShellToggleMath
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class LiquidControlsTest {

    @Test
    fun segmentedLensMeltsOnlyWhileMoving() {
        val segmentWidthPx = 100f
        val controlHeightPx = 36f

        // At rest: single lens shape
        val restMain = LiquidShape(cx = 50f, cy = 18f, halfW = 50f, halfH = 16f, radius = 16f)
        val restShapes = listOf(restMain)
        assertFalse("At rest, single lens shape must not merge", LiquidFieldMath.shouldMerge(restShapes, 24f))

        // Moving: lens + trailing shape offset by velocity * 0.03 (e.g. 15 px)
        val trailOffset = LiquidControlsMath.segmentTrailOffset(500f, segmentWidthPx) // 15 px
        val trailShape = LiquidShape(
            cx = restMain.cx - trailOffset,
            cy = 18f,
            halfW = restMain.halfW * 0.8f,
            halfH = restMain.halfH * 0.8f,
            radius = 12f
        )
        val movingShapes = listOf(restMain, trailShape)
        assertTrue("While moving, trailing shape and lens must merge", LiquidFieldMath.shouldMerge(movingShapes, 24f))
    }

    @Test
    fun neckOnlyPastTheEnd() {
        // No merge at 7 dp past the end
        assertFalse("No neck at 7 dp past the end", LiquidControlsMath.shouldShowNeck(7f, isPressed = true))

        // Merge at 9 dp past the end
        assertTrue("Neck merges at 9 dp past the end", LiquidControlsMath.shouldShowNeck(9f, isPressed = true))

        // None after release even if overshoot was recorded
        assertFalse("No neck after release", LiquidControlsMath.shouldShowNeck(9f, isPressed = false))
    }

    @Test
    fun thumbIsWhiteAtRestAndClearWhenPressed() {
        val thumbState = LiquidThumbState()

        // At rest (p = 0): surface is opaque white (alpha = 1)
        thumbState.pressProgress = 0f
        assertEquals(1f, thumbState.surfaceAlpha, 0.001f)

        // Fully pressed (p = 1): surface is clear lens (alpha = 0)
        thumbState.pressProgress = 1f
        assertEquals(0f, thumbState.surfaceAlpha, 0.001f)
    }

    @Test
    fun toggleBeadScalesWhilePressed() {
        assertEquals(1.5f, ShellToggleMath.pressedScale, 0.001f)
        assertEquals(1.0f, LiquidControlsMath.toggleBeadScale(0f), 0.001f)
        assertEquals(1.5f, LiquidControlsMath.toggleBeadScale(1f), 0.001f)
    }

    @Test
    fun sliderValueAndDetentsUnchanged() {
        assertEquals(0.5f, FaderMath.fractionFor(50, 0, 100), 0.0001f)
        assertEquals(50, FaderMath.valueFor(0.5f, 0, 100))
        assertEquals(0f, FaderMath.detent(0.01f), 0.0001f)
        assertEquals(0.05f, FaderMath.detent(0.04f), 0.0001f)
        assertEquals(1f, FaderMath.detent(0.99f), 0.0001f)
    }

    @Test
    fun everyControlHas48dpTouchHeight() {
        assertEquals(48f, ShellToggleMath.touchHeightDp, 0.001f)
        assertEquals(48f, ShellSegmentedMath.touchHeightDp, 0.001f)
        assertEquals(48.dp, LiquidControlsMath.TouchHeight)
    }
}
