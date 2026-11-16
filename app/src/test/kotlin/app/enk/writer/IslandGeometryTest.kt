package app.enk.writer

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class IslandGeometryTest {

    @Test
    fun closedIsTheRestCapsule() {
        val r = IslandGeometry.toolbar(0f)
        assertEquals(150f, r.width, 0.001f)
        assertEquals(40f, r.height, 0.001f)
        assertEquals(0f, r.centreDx, 0.001f)
        // The Lamp button starts inside the capsule's right end.
        val lamp = IslandGeometry.lampButton(0f)
        assertTrue(lamp.centreDx + lamp.width / 2f <= 75f + 0.001f)
    }

    @Test
    fun openIsToolbarPlusRoundLampWithA12DpGap() {
        val bar = IslandGeometry.toolbar(1f)
        val lamp = IslandGeometry.lampButton(1f)
        assertEquals(200f, bar.width, 0.001f)
        assertEquals(46f, bar.height, 0.001f)
        assertEquals(46f, lamp.width, 0.001f)
        val barRight = bar.centreDx + bar.width / 2f
        val lampLeft = lamp.centreDx - lamp.width / 2f
        assertEquals(12f, lampLeft - barRight, 0.001f)
        // The pair (258 dp) is centred.
        assertEquals(-129f, bar.centreDx - bar.width / 2f, 0.001f)
        assertEquals(129f, lamp.centreDx + lamp.width / 2f, 0.001f)
    }

    @Test
    fun lampPanelGrowsOutOfTheButtonKeepingItsRightEdgeFixed() {
        val button = IslandGeometry.lampButton(1f)
        val start = IslandGeometry.panel(0f, 0f)
        // A small pill tucked into the button's lower edge, as wide as the button.
        assertEquals(button.width, start.width, 0.001f)
        assertEquals(10f, start.height, 0.001f)
        assertEquals(button.centreDx, start.centreDx, 0.001f)
        assertEquals(button.height - 8f, start.top, 0.001f)
        assertEquals(5f, start.radius!!, 0.001f)
        var rightEdge: Float? = null
        for (i in 0..20) {
            val p = i / 20f
            val r = IslandGeometry.panel(p, (p - 0.1f).coerceAtLeast(0f))
            val right = r.centreDx + r.width / 2f
            if (rightEdge == null) rightEdge = right
            assertEquals("right edge never moves", rightEdge!!, right, 0.001f)
        }
        // While the island folds the button slides left, and the panel's right edge goes with it.
        val folding = IslandGeometry.panel(0.5f, 0.5f, openP = 0.5f)
        val foldingButton = IslandGeometry.lampButton(0.5f)
        assertEquals(foldingButton.centreDx + foldingButton.width / 2f, folding.centreDx + folding.width / 2f, 0.001f)
        val end = IslandGeometry.panel(1f, 1f)
        assertEquals(280f, end.width, 0.001f)
        assertEquals(230f, end.height, 0.001f)
        assertEquals(54f, end.top, 0.001f)
        assertEquals(129f, end.centreDx + end.width / 2f, 0.001f)
        assertEquals(28f, end.radius!!, 0.001f)
    }

    @Test
    fun widthLeadsHeightAndHeightMaySettlePastItsEnd() {
        val early = IslandGeometry.panel(0.5f, 0.3f)
        val sameProgress = IslandGeometry.panel(0.5f, 0.5f)
        assertTrue(early.width == sameProgress.width && early.height < sameProgress.height)
        val over = IslandGeometry.panel(1f, 1.02f)
        assertTrue("bottom edge settles past the end", over.top + over.height > 284f)
        assertEquals("top edge does not overshoot", 54f, over.top, 0.001f)
    }

    @Test
    fun rowsFadeInOneAfterAnotherAndAllShowAtFullOpen() {
        for (row in 0..3) assertEquals(1f, PanelReveal.rowAlpha(1f, row), 0.0001f)
        assertEquals(0f, PanelReveal.rowAlpha(0.5f, 0), 0.0001f)
        assertTrue(PanelReveal.rowAlpha(0.7f, 0) > PanelReveal.rowAlpha(0.7f, 3))
    }

    @Test
    fun meltReachShrinksAwayBeforeTheMotionEnds() {
        assertEquals(1f, IslandGeometry.meltBlend(0f), 0.0001f)
        assertEquals(1f, IslandGeometry.meltBlend(0.8f), 0.0001f)
        assertEquals(0f, IslandGeometry.meltBlend(1f), 0.0001f)
        assertTrue(IslandGeometry.meltBlend(0.9f) in 0f..1f)
    }

    @Test
    fun contentCrossFadeNeverShowsBothFully() {
        for (i in 0..20) {
            val p = i / 20f
            assertTrue(IslandGeometry.restContentAlpha(p) + IslandGeometry.openContentAlpha(p) <= 1f + 0.0001f)
        }
    }
}
