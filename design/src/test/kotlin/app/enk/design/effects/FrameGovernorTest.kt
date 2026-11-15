package app.enk.design.effects

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class FrameGovernorTest {

    @Test
    fun frames119DoNothing120thDecides() {
        val governor = FrameGovernor(windowFrames = 120, frameIntervalMs = 16.67f)
        for (i in 0 until 119) {
            val result = governor.onFrame(30f, ResolvedEffects.Full)
            assertNull("Frame $i before window completion must return null", result)
        }
        val result120 = governor.onFrame(30f, ResolvedEffects.Full)
        assertEquals("120th frame above threshold must demote", ResolvedEffects.Balanced, result120)
        assertEquals(ResolvedEffects.Balanced, governor.demoted)
    }

    @Test
    fun p95ExactlyAtAndJustAboveThreshold() {
        val threshold = 1.5f * 16.67f // 25.005f

        // Case 1: p95 is exactly 25.005f -> no demotion
        val govExact = FrameGovernor(windowFrames = 120, frameIntervalMs = 16.67f)
        for (i in 0 until 120) {
            govExact.onFrame(threshold, ResolvedEffects.Full)
        }
        assertNull("p95 exactly at threshold must not demote", govExact.demoted)

        // Case 2: p95 is just above 25.005f -> demotes
        val govAbove = FrameGovernor(windowFrames = 120, frameIntervalMs = 16.67f)
        for (i in 0 until 119) {
            govAbove.onFrame(threshold + 0.01f, ResolvedEffects.Full)
        }
        val demotion = govAbove.onFrame(threshold + 0.01f, ResolvedEffects.Full)
        assertEquals(ResolvedEffects.Balanced, demotion)
        assertEquals(ResolvedEffects.Balanced, govAbove.demoted)
    }

    @Test
    fun nanNegativeAndValuesAbove300Ignored() {
        val governor = FrameGovernor(windowFrames = 10, frameIntervalMs = 16.67f)
        // Feed invalid frames: NaN, negative, 301f
        assertNull(governor.onFrame(Float.NaN, ResolvedEffects.Full))
        assertNull(governor.onFrame(-10f, ResolvedEffects.Full))
        assertNull(governor.onFrame(301f, ResolvedEffects.Full))

        // Window size should still be 0, so 9 good frames won't trigger window decision
        for (i in 0 until 9) {
            assertNull(governor.onFrame(30f, ResolvedEffects.Full))
        }
        // 10th good frame triggers window decision
        val result = governor.onFrame(30f, ResolvedEffects.Full)
        assertEquals(ResolvedEffects.Balanced, result)
    }

    @Test
    fun noDemotionAtLight() {
        val governor = FrameGovernor(windowFrames = 10, frameIntervalMs = 16.67f)
        for (i in 0 until 20) {
            val result = governor.onFrame(50f, ResolvedEffects.Light)
            assertNull("Frames at Light must be ignored and return null", result)
        }
        assertNull(governor.demoted)
    }

    @Test
    fun twoWindowsInARowDemoteTwice() {
        val governor = FrameGovernor(windowFrames = 10, frameIntervalMs = 16.67f)
        // Window 1: demote Full -> Balanced
        for (i in 0 until 9) {
            assertNull(governor.onFrame(30f, ResolvedEffects.Full))
        }
        val firstDemotion = governor.onFrame(30f, ResolvedEffects.Full)
        assertEquals(ResolvedEffects.Balanced, firstDemotion)
        assertEquals(ResolvedEffects.Balanced, governor.demoted)

        // Window 2: demote Balanced -> Light
        for (i in 0 until 9) {
            assertNull(governor.onFrame(30f, ResolvedEffects.Balanced))
        }
        val secondDemotion = governor.onFrame(30f, ResolvedEffects.Balanced)
        assertEquals(ResolvedEffects.Light, secondDemotion)
        assertEquals(ResolvedEffects.Light, governor.demoted)

        // Further frames at Light do not demote further
        for (i in 0 until 10) {
            assertNull(governor.onFrame(30f, ResolvedEffects.Light))
        }
        assertEquals(ResolvedEffects.Light, governor.demoted)
    }

    @Test
    fun neverPromotes() {
        val governor = FrameGovernor(windowFrames = 10, frameIntervalMs = 16.67f)
        // Window 1: fast frames (8ms) -> no promotion or demotion
        for (i in 0 until 10) {
            assertNull(governor.onFrame(8f, ResolvedEffects.Balanced))
        }
        assertNull(governor.demoted)
    }

    @Test
    fun emptyGovernorHasNullDemoted() {
        val governor = FrameGovernor()
        assertNull(governor.demoted)
    }
}
