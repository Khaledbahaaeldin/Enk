package app.enk.start

import androidx.compose.ui.geometry.Rect
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.abs

class StartTimelineTest {

    // Reference screen 400 x 820 dp from spec and plan
    private val T = StartTargets(
        railY = 508f,
        screenWidth = 400f,
        screenHeight = 820f,
        carriageX = 276f,
        caretRect = Rect(270.3f, 479.5f, 282.01f, 481.5f),
        paperHeight = 760f,
        lightSplash = false
    )

    @Test
    fun atZeroMatchesTheSplash() {
        val f = StartTimeline.at(0f, T)
        assertEquals(96f, f.apertureRadius, 0.01f)
        assertEquals(72f, f.rail.left, 0.5f)
        assertEquals(415.3f, f.rail.top, 0.5f)
        assertEquals(328f, f.rail.right, 0.5f)
        assertEquals(438f, f.rail.bottom, 0.5f)
        assertEquals(426.7f, f.rail.center.y, 0.5f)
        assertEquals(22.7f, f.rail.height, 0.5f)

        assertEquals(1f, f.carriage.scale, 0.001f)
        assertEquals(218.7f, f.carriage.centre.x, 0.2f)
        assertEquals(426.7f, f.carriage.centre.y, 0.2f)

        assertEquals(0.34f, f.linesAlpha, 0.001f)
        assertEquals(1f, f.overlayAlpha, 0.001f)
        assertEquals(0f, f.ownPartsAlpha, 0.001f)
    }

    @Test
    fun apertureOpensByThreeFifty() {
        assertTrue(StartTimeline.at(350f, T).apertureRadius >= 760f)
    }

    @Test
    fun railIsFullWidthAfterAperture() {
        val r = StartTimeline.at(350f, T).rail
        assertEquals(0f, r.left, 0.5f)
        assertEquals(400f, r.width, 0.5f)
    }

    @Test
    fun railSettlesAtWritingHeight() {
        val r = StartTimeline.at(650f, T).rail
        assertEquals(508f, r.center.y, 0.5f)
        assertEquals(12f, r.height, 0.1f)
    }

    @Test
    fun linesGoByTwoHundred() {
        assertEquals(0f, StartTimeline.at(200f, T).linesAlpha, 0.001f)
    }

    @Test
    fun paperRisesAndLands() {
        assertEquals(760f, StartTimeline.at(150f, T).paperOffsetY, 0.5f)
        assertEquals(0f, StartTimeline.at(600f, T).paperOffsetY, 0.5f)
    }

    @Test
    fun carriageLandsAtWriterSize() {
        val c = StartTimeline.at(700f, T).carriage
        assertEquals(276f, c.centre.x, 0.5f)
        assertEquals(509f, c.centre.y, 0.5f) // railY + 1
        assertEquals(0.54f, c.scale, 0.005f)
    }

    @Test
    fun caretMorphsToWriterCaret() {
        val c = StartTimeline.at(650f, T).caret
        assertEquals(T.caretRect.left, c.left, 0.3f)
        assertEquals(T.caretRect.top, c.top, 0.3f)
        assertEquals(T.caretRect.right, c.right, 0.3f)
        assertEquals(T.caretRect.bottom, c.bottom, 0.3f)
    }

    @Test
    fun textAndTopBarTimings() {
        assertEquals(0f, StartTimeline.at(450f, T).textAlpha, 0.001f)
        assertEquals(1f, StartTimeline.at(700f, T).textAlpha, 0.001f)
        assertEquals(0f, StartTimeline.at(600f, T).topBarAlpha, 0.001f)
        assertEquals(1f, StartTimeline.at(900f, T).topBarAlpha, 0.001f)
    }

    @Test
    fun handoffSwap() {
        assertEquals(0f, StartTimeline.at(650f, T).ownPartsAlpha, 0.001f)
        assertEquals(1f, StartTimeline.at(750f, T).ownPartsAlpha, 0.001f)
        assertEquals(0f, StartTimeline.at(750f, T).overlayAlpha, 0.001f)
    }

    @Test
    fun everyValueIsMonotonicNoJumps() {
        // Sample every 4 ms from 0 to 900 ms; verify smooth progression with no jumps > 6% of component range
        val samples = (0..900 step 4).map { t -> StartTimeline.at(t.toFloat(), T) }

        // Component ranges
        val apertureRange = maxOf(samples.last().apertureRadius - samples.first().apertureRadius, 1f)
        val railLeftRange = abs(samples.first().rail.left - samples.last().rail.left).coerceAtLeast(1f)
        val railWidthRange = abs(samples.last().rail.width - samples.first().rail.width).coerceAtLeast(1f)
        val railCenterYRange = abs(samples.last().rail.center.y - samples.first().rail.center.y).coerceAtLeast(1f)
        val railHeightRange = abs(samples.first().rail.height - samples.last().rail.height).coerceAtLeast(1f)
        val linesAlphaRange = 0.34f
        val paperOffsetRange = 760f
        val carriageXRange = abs(samples.last().carriage.centre.x - samples.first().carriage.centre.x).coerceAtLeast(1f)
        val carriageYRange = abs(samples.last().carriage.centre.y - samples.first().carriage.centre.y).coerceAtLeast(1f)
        val carriageScaleRange = abs(samples.first().carriage.scale - samples.last().carriage.scale).coerceAtLeast(0.1f)
        val caretLeftRange = abs(samples.last().caret.left - samples.first().caret.left).coerceAtLeast(1f)
        val caretTopRange = abs(samples.last().caret.top - samples.first().caret.top).coerceAtLeast(1f)
        val caretRightRange = abs(samples.last().caret.right - samples.first().caret.right).coerceAtLeast(1f)
        val caretBottomRange = abs(samples.last().caret.bottom - samples.first().caret.bottom).coerceAtLeast(1f)
        val textAlphaRange = 1f
        val topBarAlphaRange = 1f
        val ownPartsAlphaRange = 1f
        val overlayAlphaRange = 1f
        val chassisMixRange = 1f

        val maxJumpFraction = 0.06f

        for (i in 0 until samples.size - 1) {
            val curr = samples[i]
            val next = samples[i + 1]

            // Aperture: non-decreasing
            assertTrue("Aperture non-decreasing at step $i", next.apertureRadius >= curr.apertureRadius - 0.001f)
            assertTrue("Aperture jump <= 6% at step $i", (next.apertureRadius - curr.apertureRadius) <= maxJumpFraction * apertureRange + 0.001f)

            // Rail left: non-increasing
            assertTrue("Rail left non-increasing at step $i", next.rail.left <= curr.rail.left + 0.001f)
            assertTrue("Rail left jump <= 6% at step $i", abs(next.rail.left - curr.rail.left) <= maxJumpFraction * railLeftRange + 0.001f)

            // Rail width: non-decreasing
            assertTrue("Rail width non-decreasing at step $i", next.rail.width >= curr.rail.width - 0.001f)
            assertTrue("Rail width jump <= 6% at step $i", (next.rail.width - curr.rail.width) <= maxJumpFraction * railWidthRange + 0.001f)

            // Rail center Y: non-decreasing
            assertTrue("Rail center Y non-decreasing at step $i", next.rail.center.y >= curr.rail.center.y - 0.001f)
            assertTrue("Rail center Y jump <= 6% at step $i", (next.rail.center.y - curr.rail.center.y) <= maxJumpFraction * railCenterYRange + 0.001f)

            // Rail height: non-increasing
            assertTrue("Rail height non-increasing at step $i", next.rail.height <= curr.rail.height + 0.001f)
            assertTrue("Rail height jump <= 6% at step $i", abs(next.rail.height - curr.rail.height) <= maxJumpFraction * railHeightRange + 0.001f)

            // Lines alpha: non-increasing
            assertTrue("Lines alpha non-increasing at step $i", next.linesAlpha <= curr.linesAlpha + 0.001f)
            assertTrue("Lines alpha jump <= 6% at step $i", abs(next.linesAlpha - curr.linesAlpha) <= maxJumpFraction * linesAlphaRange + 0.001f)

            // Paper offset Y: non-increasing
            assertTrue("Paper offset Y non-increasing at step $i", next.paperOffsetY <= curr.paperOffsetY + 0.001f)
            assertTrue("Paper offset Y jump <= 6% at step $i", abs(next.paperOffsetY - curr.paperOffsetY) <= maxJumpFraction * paperOffsetRange + 0.001f)

            // Carriage X: non-decreasing
            assertTrue("Carriage X non-decreasing at step $i", next.carriage.centre.x >= curr.carriage.centre.x - 0.001f)
            assertTrue("Carriage X jump <= 6% at step $i", (next.carriage.centre.x - curr.carriage.centre.x) <= maxJumpFraction * carriageXRange + 0.001f)

            // Carriage Y: non-decreasing
            assertTrue("Carriage Y non-decreasing at step $i", next.carriage.centre.y >= curr.carriage.centre.y - 0.001f)
            assertTrue("Carriage Y jump <= 6% at step $i", (next.carriage.centre.y - curr.carriage.centre.y) <= maxJumpFraction * carriageYRange + 0.001f)

            // Carriage scale: non-increasing
            assertTrue("Carriage scale non-increasing at step $i", next.carriage.scale <= curr.carriage.scale + 0.001f)
            assertTrue("Carriage scale jump <= 6% at step $i", abs(next.carriage.scale - curr.carriage.scale) <= maxJumpFraction * carriageScaleRange + 0.001f)

            // Caret left, top, right, bottom: non-decreasing
            assertTrue("Caret left non-decreasing at step $i", next.caret.left >= curr.caret.left - 0.001f)
            assertTrue("Caret left jump <= 6% at step $i", (next.caret.left - curr.caret.left) <= maxJumpFraction * caretLeftRange + 0.001f)

            assertTrue("Caret top non-decreasing at step $i", next.caret.top >= curr.caret.top - 0.001f)
            assertTrue("Caret top jump <= 6% at step $i", (next.caret.top - curr.caret.top) <= maxJumpFraction * caretTopRange + 0.001f)

            assertTrue("Caret right non-decreasing at step $i", next.caret.right >= curr.caret.right - 0.001f)
            assertTrue("Caret right jump <= 6% at step $i", (next.caret.right - curr.caret.right) <= maxJumpFraction * caretRightRange + 0.001f)

            assertTrue("Caret bottom non-decreasing at step $i", next.caret.bottom >= curr.caret.bottom - 0.001f)
            assertTrue("Caret bottom jump <= 6% at step $i", (next.caret.bottom - curr.caret.bottom) <= maxJumpFraction * caretBottomRange + 0.001f)

            // Text alpha: non-decreasing
            assertTrue("Text alpha non-decreasing at step $i", next.textAlpha >= curr.textAlpha - 0.001f)
            assertTrue("Text alpha jump <= 6% at step $i", (next.textAlpha - curr.textAlpha) <= maxJumpFraction * textAlphaRange + 0.001f)

            // Top bar alpha: non-decreasing
            assertTrue("Top bar alpha non-decreasing at step $i", next.topBarAlpha >= curr.topBarAlpha - 0.001f)
            assertTrue("Top bar alpha jump <= 6% at step $i", (next.topBarAlpha - curr.topBarAlpha) <= maxJumpFraction * topBarAlphaRange + 0.001f)

            // Own parts alpha: non-decreasing
            assertTrue("Own parts alpha non-decreasing at step $i", next.ownPartsAlpha >= curr.ownPartsAlpha - 0.001f)
            assertTrue("Own parts alpha jump <= 6% at step $i", (next.ownPartsAlpha - curr.ownPartsAlpha) <= maxJumpFraction * ownPartsAlphaRange + 0.001f)

            // Overlay alpha: non-increasing
            assertTrue("Overlay alpha non-increasing at step $i", next.overlayAlpha <= curr.overlayAlpha + 0.001f)
            assertTrue("Overlay alpha jump <= 6% at step $i", abs(next.overlayAlpha - curr.overlayAlpha) <= maxJumpFraction * overlayAlphaRange + 0.001f)

            // Chassis mix: non-decreasing
            assertTrue("Chassis mix non-decreasing at step $i", next.chassisMix >= curr.chassisMix - 0.001f)
            assertTrue("Chassis mix jump <= 6% at step $i", (next.chassisMix - curr.chassisMix) <= maxJumpFraction * chassisMixRange + 0.001f)
        }
    }

    @Test
    fun worksOnATallerScreen() {
        // Test 412 x 915 and 360 x 640 screens, check end frames match targets
        val tallTargets = StartTargets(
            railY = 567f, // round(915 * 0.62)
            screenWidth = 412f,
            screenHeight = 915f,
            carriageX = 280f,
            caretRect = Rect(275f, 538f, 287f, 540f),
            paperHeight = 850f,
            lightSplash = false
        )
        val endTall = StartTimeline.at(900f, tallTargets)
        assertEquals(0f, endTall.rail.left, 0.5f)
        assertEquals(412f, endTall.rail.width, 0.5f)
        assertEquals(567f, endTall.rail.center.y, 0.5f)
        assertEquals(12f, endTall.rail.height, 0.1f)
        assertEquals(280f, endTall.carriage.centre.x, 0.5f)
        assertEquals(568f, endTall.carriage.centre.y, 0.5f) // railY + 1
        assertEquals(0.54f, endTall.carriage.scale, 0.005f)
        assertEquals(tallTargets.caretRect.left, endTall.caret.left, 0.3f)
        assertEquals(tallTargets.caretRect.top, endTall.caret.top, 0.3f)
        assertEquals(tallTargets.caretRect.right, endTall.caret.right, 0.3f)
        assertEquals(tallTargets.caretRect.bottom, endTall.caret.bottom, 0.3f)
        assertEquals(0f, endTall.paperOffsetY, 0.5f)
        assertEquals(0f, endTall.linesAlpha, 0.001f)
        assertEquals(1f, endTall.textAlpha, 0.001f)
        assertEquals(1f, endTall.topBarAlpha, 0.001f)
        assertEquals(0f, endTall.overlayAlpha, 0.001f)
        assertEquals(1f, endTall.ownPartsAlpha, 0.001f)

        val shortTargets = StartTargets(
            railY = 397f, // round(640 * 0.62)
            screenWidth = 360f,
            screenHeight = 640f,
            carriageX = 240f,
            caretRect = Rect(235f, 370f, 247f, 372f),
            paperHeight = 580f,
            lightSplash = false
        )
        val endShort = StartTimeline.at(900f, shortTargets)
        assertEquals(0f, endShort.rail.left, 0.5f)
        assertEquals(360f, endShort.rail.width, 0.5f)
        assertEquals(397f, endShort.rail.center.y, 0.5f)
        assertEquals(12f, endShort.rail.height, 0.1f)
        assertEquals(240f, endShort.carriage.centre.x, 0.5f)
        assertEquals(398f, endShort.carriage.centre.y, 0.5f) // railY + 1
        assertEquals(0.54f, endShort.carriage.scale, 0.005f)
        assertEquals(shortTargets.caretRect.left, endShort.caret.left, 0.3f)
        assertEquals(shortTargets.caretRect.top, endShort.caret.top, 0.3f)
        assertEquals(shortTargets.caretRect.right, endShort.caret.right, 0.3f)
        assertEquals(shortTargets.caretRect.bottom, endShort.caret.bottom, 0.3f)
        assertEquals(0f, endShort.paperOffsetY, 0.5f)
        assertEquals(0f, endShort.linesAlpha, 0.001f)
        assertEquals(1f, endShort.textAlpha, 0.001f)
        assertEquals(1f, endShort.topBarAlpha, 0.001f)
        assertEquals(0f, endShort.overlayAlpha, 0.001f)
        assertEquals(1f, endShort.ownPartsAlpha, 0.001f)
    }

    @Test
    fun lightSplashGrowsAChassisCircle() {
        val L = T.copy(lightSplash = true)
        assertEquals(0f, StartTimeline.at(0f, L).chassisCircleRadius, 0.01f)
        assertEquals(StartTimeline.at(200f, L).apertureRadius, StartTimeline.at(200f, L).chassisCircleRadius, 0.5f)
        assertEquals(0f, StartTimeline.at(200f, L).chassisMix, 0.001f)
    }

    @Test
    fun darkSplashHasNoChassisCircle() {
        assertEquals(0f, StartTimeline.at(200f, T).chassisCircleRadius, 0.01f)
        assertTrue(StartTimeline.at(300f, T).chassisMix > 0f)
    }
}
