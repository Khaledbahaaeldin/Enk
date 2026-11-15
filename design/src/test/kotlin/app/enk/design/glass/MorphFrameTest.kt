package app.enk.design.glass

import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class MorphFrameTest {

    private val collapsed = DpSize(100.dp, 40.dp)
    private val expanded = DpSize(300.dp, 200.dp)
    private val collapsedRadius = 20f
    private val expandedRadius = 28f

    @Test
    fun exactNumbersAtZero() {
        val target = DpOffset((-50).dp, 100.dp)
        val frame5 = morphFrame(
            p = 0f,
            collapsed = collapsed,
            expanded = expanded,
            collapsedRadiusDp = collapsedRadius,
            expandedRadiusDp = expandedRadius,
            target = target,
            itemCount = 5
        )

        assertEquals(100f, frame5.widthDp, 0.001f)
        assertEquals(40f, frame5.heightDp, 0.001f)
        assertEquals(20f, frame5.radiusDp, 0.001f)
        assertEquals(0f, frame5.offsetXDp, 0.001f)
        assertEquals(0f, frame5.offsetYDp, 0.001f)
        assertEquals(1f, frame5.labelAlpha, 0.001f)
        assertEquals(listOf(0f, 0f, 0f, 0f, 0f), frame5.itemAlpha)
        assertEquals(listOf(8f, 8f, 8f, 8f, 8f), frame5.itemDyDp)

        val frame0 = morphFrame(0f, collapsed, expanded, collapsedRadius, expandedRadius, target, 0)
        assertTrue(frame0.itemAlpha.isEmpty())
        assertTrue(frame0.itemDyDp.isEmpty())

        val frame1 = morphFrame(0f, collapsed, expanded, collapsedRadius, expandedRadius, target, 1)
        assertEquals(listOf(0f), frame1.itemAlpha)
        assertEquals(listOf(8f), frame1.itemDyDp)
    }

    @Test
    fun exactNumbersAtHalf() {
        val target = DpOffset((-50).dp, 100.dp)
        val frame = morphFrame(
            p = 0.5f,
            collapsed = collapsed,
            expanded = expanded,
            collapsedRadiusDp = collapsedRadius,
            expandedRadiusDp = expandedRadius,
            target = target,
            itemCount = 5
        )

        assertEquals(200f, frame.widthDp, 0.001f)
        assertEquals(120f, frame.heightDp, 0.001f)
        assertEquals(24f, frame.radiusDp, 0.001f)
        assertEquals(-25f, frame.offsetXDp, 0.001f)
        assertEquals(50f, frame.offsetYDp, 0.001f)
        assertEquals(0f, frame.labelAlpha, 0.001f)
        // item 0: (0.5 - 0.35) * 4 = 0.6
        // item 1: (0.5 - 0.35 - 0.08) * 4 = 0.28
        // item 2..4: clamped to 0
        assertEquals(0.6f, frame.itemAlpha[0], 0.001f)
        assertEquals(0.28f, frame.itemAlpha[1], 0.001f)
        assertEquals(0f, frame.itemAlpha[2], 0.001f)
        assertEquals(0f, frame.itemAlpha[3], 0.001f)
        assertEquals(0f, frame.itemAlpha[4], 0.001f)
        // dy: (1 - (0.5 - 0.25) * 2) * 8 = (1 - 0.5) * 8 = 4f
        assertEquals(listOf(4f, 4f, 4f, 4f, 4f), frame.itemDyDp)
    }

    @Test
    fun exactNumbersAtOne() {
        val target = DpOffset((-50).dp, 100.dp)
        val frame = morphFrame(
            p = 1f,
            collapsed = collapsed,
            expanded = expanded,
            collapsedRadiusDp = collapsedRadius,
            expandedRadiusDp = expandedRadius,
            target = target,
            itemCount = 5
        )

        assertEquals(300f, frame.widthDp, 0.001f)
        assertEquals(200f, frame.heightDp, 0.001f)
        assertEquals(28f, frame.radiusDp, 0.001f)
        assertEquals(-50f, frame.offsetXDp, 0.001f)
        assertEquals(100f, frame.offsetYDp, 0.001f)
        assertEquals(0f, frame.labelAlpha, 0.001f)
        assertEquals(listOf(1f, 1f, 1f, 1f, 1f), frame.itemAlpha)
        assertEquals(listOf(0f, 0f, 0f, 0f, 0f), frame.itemDyDp)
    }

    @Test
    fun intermediateAndEdgeProgressValues() {
        val target = DpOffset(60.dp, (-40).dp)
        val zeroTarget = DpOffset.Zero

        // p = 0.25
        val frame025 = morphFrame(0.25f, collapsed, expanded, collapsedRadius, expandedRadius, target, 5)
        assertEquals(0f, frame025.labelAlpha, 0.001f)
        assertEquals(8f, frame025.itemDyDp[0], 0.001f)

        // p = 0.35
        val frame035 = morphFrame(0.35f, collapsed, expanded, collapsedRadius, expandedRadius, target, 5)
        assertEquals(0f, frame035.itemAlpha[0], 0.001f)
        // dy at 0.35: (1 - 0.1 * 2) * 8 = 0.8 * 8 = 6.4f
        assertEquals(6.4f, frame035.itemDyDp[0], 0.001f)

        // p = 1.1 (overshoot)
        val frame11 = morphFrame(1.1f, collapsed, expanded, collapsedRadius, expandedRadius, target, 5)
        assertEquals(320f, frame11.widthDp, 0.001f)
        assertEquals(216f, frame11.heightDp, 0.001f)
        assertEquals(28f, frame11.radiusDp, 0.001f) // clamped radius
        assertEquals(66f, frame11.offsetXDp, 0.001f)
        assertEquals(-44f, frame11.offsetYDp, 0.001f)
        assertEquals(0f, frame11.labelAlpha, 0.001f)
        assertEquals(listOf(1f, 1f, 1f, 1f, 1f), frame11.itemAlpha)
        assertEquals(listOf(0f, 0f, 0f, 0f, 0f), frame11.itemDyDp)

        // p = -0.1 (undershoot)
        val frameNeg = morphFrame(-0.1f, collapsed, expanded, collapsedRadius, expandedRadius, target, 5)
        assertEquals(80f, frameNeg.widthDp, 0.001f)
        assertEquals(24f, frameNeg.heightDp, 0.001f)
        assertEquals(20f, frameNeg.radiusDp, 0.001f)
        assertEquals(-6f, frameNeg.offsetXDp, 0.001f)
        assertEquals(4f, frameNeg.offsetYDp, 0.001f)
        assertEquals(1f, frameNeg.labelAlpha, 0.001f)
        assertEquals(listOf(0f, 0f, 0f, 0f, 0f), frameNeg.itemAlpha)
        assertEquals(listOf(8f, 8f, 8f, 8f, 8f), frameNeg.itemDyDp)

        // p = NaN
        val frameNaN = morphFrame(Float.NaN, collapsed, expanded, collapsedRadius, expandedRadius, zeroTarget, 5)
        assertEquals(100f, frameNaN.widthDp, 0.001f)
        assertEquals(40f, frameNaN.heightDp, 0.001f)
        assertEquals(20f, frameNaN.radiusDp, 0.001f)
        assertEquals(0f, frameNaN.offsetXDp, 0.001f)
        assertEquals(0f, frameNaN.offsetYDp, 0.001f)
        assertEquals(1f, frameNaN.labelAlpha, 0.001f)

        // zero target
        val frameZeroTarget = morphFrame(1f, collapsed, expanded, collapsedRadius, expandedRadius, zeroTarget, 1)
        assertEquals(0f, frameZeroTarget.offsetXDp, 0.001f)
        assertEquals(0f, frameZeroTarget.offsetYDp, 0.001f)
    }
}
