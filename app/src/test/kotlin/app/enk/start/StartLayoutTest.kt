package app.enk.start

import app.enk.text.GridLayout
import app.enk.writer.CaretGeometry
import app.enk.writer.WriterGeometry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class StartLayoutTest {

    private fun targets(
        w: Float = 400f,
        h: Float = 820f,
        insets: InsetsDp = InsetsDp.None,
        fontScale: Float = 1f,
        text: String = "",
        light: Boolean = false
    ) = StartLayout.targets(w, h, insets, fontScale, text, light)

    @Test
    fun withoutInsetsTheTargetsAreTheWritersOwnGeometry() {
        val g = WriterGeometry.compute(400f, 820f, 1f)
        val t = targets(text = "Hello")
        assertEquals(g.railYDp, t.railY, 0.001f)
        assertEquals(g.carriageCenterXDp(5), t.carriageX, 0.001f)
        assertEquals(CaretGeometry.leftDp(g, 5), t.caretRect.left, 0.001f)
        assertEquals(CaretGeometry.topDp(g), t.caretRect.top, 0.001f)
        assertEquals(g.columnWidthDp, t.caretRect.width, 0.001f)
        assertEquals(CaretGeometry.HEIGHT_DP, t.caretRect.height, 0.001f)
    }

    @Test
    fun theWriterLaysOutInsideTheInsetsSoTheTargetsShiftByThem() {
        val insets = InsetsDp(left = 8f, top = 49.5f, right = 12f, bottom = 24f)
        val g = WriterGeometry.compute(400f - 20f, 820f - 73.5f, 1f)
        val t = targets(insets = insets, text = "Hello")
        assertEquals("rail", insets.top + g.railYDp, t.railY, 0.001f)
        assertEquals("carriage", insets.left + g.carriageCenterXDp(5), t.carriageX, 0.001f)
        assertEquals("caret left", insets.left + CaretGeometry.leftDp(g, 5), t.caretRect.left, 0.001f)
        assertEquals("caret top", insets.top + CaretGeometry.topDp(g), t.caretRect.top, 0.001f)
        assertEquals(400f, t.screenWidth, 0f)
        assertEquals(820f, t.screenHeight, 0f)
    }

    @Test
    fun theTopInsetMovesTheRailByTheInsetMinusTheShorterWriter() {
        val flat = targets().railY
        val withBar = targets(insets = InsetsDp(0f, 49.5f, 0f, 0f)).railY
        // 62% of a box that is 49.5 dp shorter, shifted down by 49.5 dp: net 38% of the inset.
        assertEquals(0.38f * 49.5f, withBar - flat, 1f)
    }

    @Test
    fun fontScaleChangesTheColumnCountAndSoTheCaretColumnOfALongNote() {
        val text = "x".repeat(30)
        val gNormal = WriterGeometry.compute(400f, 820f, 1f)
        val gLarge = WriterGeometry.compute(400f, 820f, 1.3f)
        assertTrue("the test needs two different column counts", gNormal.cols != gLarge.cols)
        for ((scale, g) in listOf(1f to gNormal, 1.3f to gLarge)) {
            val col = GridLayout.caret(GridLayout.wrap(text, g.cols), text.length, g.cols).col
            assertEquals("fontScale $scale", CaretGeometry.leftDp(g, col), targets(text = text, fontScale = scale).caretRect.left, 0.001f)
        }
    }

    @Test
    fun theCaretFollowsTheEndOfTheNoteNotTheStartOfTheLine() {
        val g = WriterGeometry.compute(400f, 820f, 1f)
        val oneLine = targets(text = "a".repeat(g.cols))
        val wrapped = targets(text = "a".repeat(g.cols + 3))
        assertTrue("a wrapped note ends further left than a full line", wrapped.caretRect.left < oneLine.caretRect.left)
    }

    @Test
    fun emojiAndNewlinesDoNotBreakTheColumn() {
        val t = targets(text = "ab😀\ncd😀")
        assertTrue(t.caretRect.left.isFinite() && t.carriageX.isFinite())
        assertTrue(targets(text = "\n\n\n").caretRect.left.isFinite())
    }

    @Test
    fun emptyAndFirstLaunchPutTheCarriageAtColumnZero() {
        val g = WriterGeometry.compute(400f, 820f, 1f)
        assertEquals(g.carriageCenterXDp(0), targets(text = "").carriageX, 0.001f)
    }

    @Test
    fun thePaperStartsBelowTheBottomEdgeEvenWithABottomInset() {
        val insets = InsetsDp(0f, 49.5f, 0f, 24f)
        val t = targets(insets = insets)
        val g = WriterGeometry.compute(400f, 820f - 73.5f, 1f)
        val paperTopAtStart = insets.top + g.paperTopDp + t.paperHeight
        assertTrue("paper top $paperTopAtStart must be below the window bottom 820", paperTopAtStart > 820f)
    }

    @Test
    fun tinyOrNonsenseWindowsStillGiveFiniteTargets() {
        val windows = listOf(0f to 0f, 1f to 1f, 40f to 40f, -5f to 100f, Float.NaN to 820f, 400f to Float.NaN)
        for ((w, h) in windows) {
            val t = targets(w = w, h = h, insets = InsetsDp(500f, 500f, 500f, 500f))
            val finite = t.railY.isFinite() && t.carriageX.isFinite() && t.caretRect.left.isFinite() && t.paperHeight.isFinite()
            assertTrue("$w x $h", finite)
        }
    }

    @Test
    fun lightSplashIsPassedThrough() {
        assertTrue(targets(light = true).lightSplash)
        assertFalse(targets(light = false).lightSplash)
    }
}
