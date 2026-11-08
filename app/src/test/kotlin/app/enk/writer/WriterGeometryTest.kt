package app.enk.writer

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class WriterGeometryTest {

    @Test
    fun referenceScreen400x820() {
        val g = WriterGeometry.compute(widthDp = 400f, heightDp = 820f, fontScale = 1.0f)

        assertEquals(28, g.cols)
        assertEquals(19.5238f, g.fontSp, 0.001f)
        assertEquals(11.7143f, g.columnWidthDp, 0.001f)
        assertEquals(33.19f, g.lineHeightDp, 0.01f)
        assertEquals(508f, g.railYDp, 0.001f)
        assertEquals(387.81f, g.textTopOffsetDp(0), 0.01f)
        assertEquals(41.857f, g.carriageCenterXDp(0), 0.01f)
        assertEquals(64f, g.paperTopDp, 0.001f)
        assertEquals(36f, g.textLeftDp, 0.001f)
        assertEquals(328f, g.textWidthDp, 0.001f)
    }

    @Test
    fun fontScaleAdjustments() {
        val g13 = WriterGeometry.compute(widthDp = 400f, heightDp = 820f, fontScale = 1.3f)
        assertEquals(22, g13.cols)

        val g085 = WriterGeometry.compute(widthDp = 400f, heightDp = 820f, fontScale = 0.85f)
        assertEquals(28, g085.cols)
    }

    @Test
    fun wideScreenCap() {
        val g = WriterGeometry.compute(widthDp = 1000f, heightDp = 820f, fontScale = 1.0f)

        assertEquals(22f, g.fontSp, 0.001f)
        assertEquals(13.2f, g.columnWidthDp, 0.001f)
        assertEquals(369.6f, g.textWidthDp, 0.001f)
        assertEquals(315.2f, g.textLeftDp, 0.001f)
        assertTrue("Text block must be centred, textLeftDp > 36f", g.textLeftDp > 36f)
    }

    @Test
    fun colFromXInverse() {
        val g = WriterGeometry.compute(widthDp = 400f, heightDp = 820f, fontScale = 1.0f)

        for (c in listOf(0, 5, 15, 27)) {
            val x = g.carriageCenterXDp(c)
            val derivedCol = g.colFromX(x)
            assertEquals("colFromX inverse for col $c", c.toFloat(), derivedCol, 0.0001f)
        }

        // Continuous dragging: test out of bounds does not clamp
        val leftOfText = g.colFromX(g.textLeftDp - 20f)
        assertTrue("x to the left of textLeftDp returns negative col", leftOfText < 0f)

        val rightOfText = g.colFromX(g.textLeftDp + g.textWidthDp + 20f)
        assertTrue("x to the right of textWidthDp returns col > cols", rightOfText > g.cols.toFloat())
    }

    @Test
    fun lineFeedProgression() {
        val g = WriterGeometry.compute(widthDp = 400f, heightDp = 820f, fontScale = 1.0f)

        for (line in listOf(0, 1, 5, 10)) {
            val topCurrent = g.textTopOffsetDp(line)
            val topNext = g.textTopOffsetDp(line + 1)
            assertEquals(
                "Next line offset must decrease by lineHeightDp",
                topCurrent - g.lineHeightDp,
                topNext,
                0.001f
            )
        }
    }

    @Test
    fun edgeCasesAndBadInputs() {
        // Width < 72 (e.g. 50f, 0f, -10f, NaN)
        val badWidths = listOf(50f, 0f, -10f, Float.NaN)
        for (w in badWidths) {
            val g = WriterGeometry.compute(widthDp = w, heightDp = 820f, fontScale = 1.0f)
            assertTrue("cols must be positive", g.cols > 0)
            assertTrue("fontSp must be positive and finite", g.fontSp > 0f && !g.fontSp.isNaN() && !g.fontSp.isInfinite())
            assertTrue("columnWidthDp must be positive and finite", g.columnWidthDp > 0f && !g.columnWidthDp.isNaN())
            assertTrue("lineHeightDp must be positive and finite", g.lineHeightDp > 0f && !g.lineHeightDp.isNaN())
            assertTrue("railYDp must be finite and non-negative", !g.railYDp.isNaN() && g.railYDp >= 0f)
        }

        // Height 0, negative, NaN
        val badHeights = listOf(0f, -100f, Float.NaN)
        for (h in badHeights) {
            val g = WriterGeometry.compute(widthDp = 400f, heightDp = h, fontScale = 1.0f)
            assertTrue("railYDp must be finite and non-negative", !g.railYDp.isNaN() && g.railYDp >= 0f)
        }

        // FontScale 0, negative, NaN, huge
        val badFontScales = listOf(0f, -1f, Float.NaN, 100f)
        for (fs in badFontScales) {
            val g = WriterGeometry.compute(widthDp = 400f, heightDp = 820f, fontScale = fs)
            assertTrue("cols must be in valid range", g.cols in 1..28)
            assertTrue("fontSp must be positive and finite", g.fontSp > 0f && !g.fontSp.isNaN() && !g.fontSp.isInfinite())
        }
    }

    @Test
    fun railYRounding() {
        // railYDp = round(height * 0.62)
        // Height = 820 -> 820 * 0.62 = 508.4 -> round(508.4) = 508.0
        val g1 = WriterGeometry.compute(widthDp = 400f, heightDp = 820f, fontScale = 1.0f)
        assertEquals(508f, g1.railYDp, 0.001f)

        // Height = 822 -> 822 * 0.62 = 509.64 -> round(509.64) = 510.0
        val g2 = WriterGeometry.compute(widthDp = 400f, heightDp = 822f, fontScale = 1.0f)
        assertEquals(510f, g2.railYDp, 0.001f)
    }

    @Test
    fun compactPhoneGeometry() {
        val g = WriterGeometry.compute(widthDp = 360f, heightDp = 640f, fontScale = 1.0f)
        assertEquals(28, g.cols)
        assertEquals(17.1428f, g.fontSp, 0.001f)
        assertEquals(10.2857f, g.columnWidthDp, 0.001f)
        assertEquals(288f, g.textWidthDp, 0.001f)
        assertEquals(36f, g.textLeftDp, 0.001f)
        assertEquals(397f, g.railYDp, 0.001f)
    }

    @Test
    fun foldableTabletGeometry() {
        val g = WriterGeometry.compute(widthDp = 800f, heightDp = 1200f, fontScale = 1.0f)
        assertEquals(28, g.cols)
        assertEquals(22f, g.fontSp, 0.001f) // Capped at 22 sp
        assertEquals(13.2f, g.columnWidthDp, 0.001f)
        assertEquals(369.6f, g.textWidthDp, 0.001f)
        assertEquals(215.2f, g.textLeftDp, 0.001f) // Centered
        assertEquals(744f, g.railYDp, 0.001f)
    }

    @Test
    fun columnBoundariesAndNearestColumn() {
        val g = WriterGeometry.compute(widthDp = 400f, heightDp = 820f, fontScale = 1.0f)

        for (col in 0 until g.cols) {
            val leftEdge = g.textLeftDp + col * g.columnWidthDp
            val center = g.carriageCenterXDp(col)
            val rightEdge = g.textLeftDp + (col + 1) * g.columnWidthDp

            assertEquals(col.toFloat(), g.colFromX(center), 0.001f)
            assertEquals(col - 0.5f, g.colFromX(leftEdge), 0.001f)
            assertEquals(col + 0.5f, g.colFromX(rightEdge), 0.001f)
        }
    }
}
