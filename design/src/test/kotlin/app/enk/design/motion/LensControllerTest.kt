package app.enk.design.motion

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class LensControllerTest {

    private val items = listOf(
        ItemBounds(0f, 80f),
        ItemBounds(80f, 80f),
        ItemBounds(160f, 80f)
    )

    @Test
    fun releaseAtRestPicksNearest() {
        val controller = LensController(items)
        controller.down(40f, 0f, 80f)
        controller.move(110f, 100L)
        controller.move(110f, 120L)
        val release = controller.up(110f, 120L)

        assertEquals(1, release.targetIndex)
        assertEquals(0f, release.velocity, 0.01f)
        assertEquals(ShellMotion.default, release.spring)
    }

    @Test
    fun flickProjectsToLast() {
        val controller = LensController(items)
        controller.down(40f, 0f, 80f)
        controller.move(102f, 100L)
        val release = controller.up(110f, 110L) // dt = 10ms, dx = 8dp -> 800 dp/s

        assertEquals(2, release.targetIndex)
        assertEquals(800f, release.velocity, 0.1f)
        assertEquals(ShellMotion.flick, release.spring)
    }

    @Test
    fun slowReleaseUsesDefaultSpring() {
        val controller1 = LensController(items)
        controller1.down(40f, 0f, 80f)
        controller1.move(100f, 0L)
        val releaseSlow = controller1.up(103f, 10L) // v = 300 dp/s <= 350
        assertEquals(300f, releaseSlow.velocity, 0.1f)
        assertEquals(ShellMotion.default, releaseSlow.spring)

        val controller2 = LensController(items)
        controller2.down(40f, 0f, 80f)
        controller2.move(100f, 0L)
        val releaseFast = controller2.up(104f, 10L) // v = 400 dp/s > 350
        assertEquals(400f, releaseFast.velocity, 0.1f)
        assertEquals(ShellMotion.flick, releaseFast.spring)
    }

    @Test
    fun slopNotCrossed() {
        val controller = LensController(items)
        controller.down(40f, 0f, 80f)

        val frameBeforeSlop = controller.move(47f, 10L) // 7 dp < 8 dp
        assertFalse(frameBeforeSlop.dragging)
        assertEquals(0f, frameBeforeSlop.left, 0.001f)

        val frameAfterSlop = controller.move(49f, 20L) // 9 dp >= 8 dp
        assertTrue(frameAfterSlop.dragging)
    }

    @Test
    fun grabOffLensCentresUnderFinger() {
        val controller = LensController(items)
        // Lens is at [0, 80]. Finger goes down at 150 (outside lens).
        controller.down(150f, 0f, 80f)

        val frame = controller.move(170f, 20L) // moved 20 dp, dragging started
        assertTrue(frame.dragging)
        // Grab offset should be half lens width = 40.
        // Lens left = 170 - 40 = 130.
        // Center of lens = 130 + 40 = 170 (exactly under finger).
        assertEquals(130f, frame.left, 0.001f)
        assertEquals(80f, frame.width, 0.001f)
    }

    @Test
    fun dragBeyondEndIsRubberBanded() {
        val controller = LensController(items)
        // Last item ends at 240. Max left for width 80 is 160.
        controller.down(40f, 0f, 80f)
        // Move finger to 300 -> unconstrained left = 300 - 40 = 260 (100 dp past 160).
        val frame = controller.move(300f, 20L)
        assertTrue(frame.dragging)
        val deltaPastEnd = frame.left - 160f
        assertTrue("Delta past end $deltaPastEnd should be < 100 dp", deltaPastEnd < 100f)
        assertTrue("Delta past end $deltaPastEnd should be > 0 dp", deltaPastEnd > 0f)
        assertEquals(160f + Physics.rubber(100f, 80f), frame.left, 0.01f)
    }

    @Test
    fun tapIndexFindsItem() {
        val controller = LensController(items)
        assertEquals(1, controller.tapIndex(100f))
        assertNull(controller.tapIndex(300f))
    }

    @Test
    fun tapIndexEdges() {
        val controller = LensController(items)
        assertEquals(0, controller.tapIndex(0f))
        assertEquals(0, controller.tapIndex(79.99f))
        assertEquals(1, controller.tapIndex(80f))
        assertEquals(1, controller.tapIndex(159.99f))
        assertEquals(2, controller.tapIndex(160f))
        assertEquals(2, controller.tapIndex(240f))
        assertNull(controller.tapIndex(240.01f))
        assertNull(controller.tapIndex(-0.01f))
    }

    @Test
    fun slopExactBoundary() {
        val controllerRight = LensController(items)
        controllerRight.down(40f, 0f, 80f)
        assertFalse(controllerRight.move(47.999f, 10L).dragging)
        assertTrue(controllerRight.move(48.0f, 20L).dragging)

        val controllerLeft = LensController(items)
        controllerLeft.down(40f, 0f, 80f)
        assertFalse(controllerLeft.move(32.001f, 10L).dragging)
        assertTrue(controllerLeft.move(32.0f, 20L).dragging)
    }

    @Test
    fun highAndNegativeFlickVelocities() {
        val controllerFwd = LensController(items)
        controllerFwd.down(40f, 0f, 80f)
        controllerFwd.move(100f, 0L)
        val releaseFwd = controllerFwd.up(200f, 10L) // 10,000 dp/s
        assertEquals(2, releaseFwd.targetIndex)
        assertEquals(ShellMotion.flick, releaseFwd.spring)

        val controllerBack = LensController(items)
        controllerBack.down(200f, 160f, 80f)
        controllerBack.move(100f, 0L)
        val releaseBack = controllerBack.up(90f, 10L) // -1000 dp/s
        assertEquals(0, releaseBack.targetIndex)
        assertEquals(ShellMotion.flick, releaseBack.spring)
        assertEquals(-1000f, releaseBack.velocity, 0.1f)
    }

    @Test
    fun emptyItemsList() {
        val controller = LensController(emptyList())
        controller.down(10f, 0f, 40f)
        val frame = controller.move(30f, 10L)
        assertTrue(frame.dragging)
        assertEquals(0, frame.highlighted)
        val release = controller.up(30f, 10L)
        assertEquals(0, release.targetIndex)
        assertNull(controller.tapIndex(10f))
    }

    @Test
    fun singleItemList() {
        val controller = LensController(listOf(ItemBounds(0f, 100f)))
        controller.down(50f, 0f, 100f)
        assertEquals(0, controller.tapIndex(0f))
        assertEquals(0, controller.tapIndex(50f))
        assertEquals(0, controller.tapIndex(100f))
        assertNull(controller.tapIndex(100.01f))
        val release = controller.up(50f, 10L)
        assertEquals(0, release.targetIndex)
    }

    @Test
    fun zeroAndNegativeTimeDeltasInVelocity() {
        val controllerSameTime = LensController(items)
        controllerSameTime.down(40f, 0f, 80f)
        controllerSameTime.move(50f, 100L)
        val releaseSameTime = controllerSameTime.up(60f, 100L) // dt = 0
        assertEquals(0f, releaseSameTime.velocity, 0.001f)
        assertEquals(ShellMotion.default, releaseSameTime.spring)

        val controllerBackwards = LensController(items)
        controllerBackwards.down(40f, 0f, 80f)
        controllerBackwards.move(50f, 200L)
        val releaseBackwards = controllerBackwards.up(60f, 100L) // dt < 0
        assertEquals(0f, releaseBackwards.velocity, 0.001f)
        assertEquals(ShellMotion.default, releaseBackwards.spring)

        val controllerInstant = LensController(items)
        controllerInstant.down(40f, 0f, 80f)
        val releaseInstant = controllerInstant.up(40f, 100L) // only 1 sample
        assertEquals(0f, releaseInstant.velocity, 0.001f)
        assertEquals(ShellMotion.default, releaseInstant.spring)
    }
}
