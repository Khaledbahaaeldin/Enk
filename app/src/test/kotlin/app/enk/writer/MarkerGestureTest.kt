package app.enk.writer

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.down
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.moveTo
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.up
import androidx.compose.ui.unit.dp
import app.enk.settings.CorrectionMode
import app.enk.text.StruckPage
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import kotlin.math.roundToInt

@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = "w400dp-h820dp")
class MarkerGestureTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private val testGeometry = WriterGeometry.compute(
        widthDp = 400f,
        heightDp = 820f,
        fontScale = 1.0f
    )

    @Test
    fun markerTuningConstants() {
        assertEquals(480L, MarkerTuning.LONG_PRESS_MS)
        assertEquals(6f, MarkerTuning.SLOP_DP, 0.001f)
    }

    @Test
    fun scrubIndexAtEnds() {
        val pageLen = 100
        val textRight = testGeometry.textLeftDp + testGeometry.cols * testGeometry.columnWidthDp

        // Exact left and right edges
        assertEquals(0, scrubIndex(testGeometry.textLeftDp, testGeometry, pageLen))
        assertEquals(pageLen, scrubIndex(textRight, testGeometry, pageLen))

        // Beyond left and right edges clamp safely
        assertEquals(0, scrubIndex(testGeometry.textLeftDp - 50f, testGeometry, pageLen))
        assertEquals(pageLen, scrubIndex(textRight + 50f, testGeometry, pageLen))
    }

    @Test
    fun scrubIndexMidpoint() {
        val pageLen = 100
        val textRight = testGeometry.textLeftDp + testGeometry.cols * testGeometry.columnWidthDp
        val midX = (testGeometry.textLeftDp + textRight) / 2f
        assertEquals(50, scrubIndex(midX, testGeometry, pageLen))

        val oddPageLen = 101
        val expected = (0.5f * oddPageLen).roundToInt()
        assertEquals(expected, scrubIndex(midX, testGeometry, oddPageLen))
    }

    @Test
    fun holdWithoutMovingFiresLongPressAt480ms() {
        val carriageX = testGeometry.carriageCenterXDp(col = 12)
        val carriageY = testGeometry.railYDp + 1f
        var longPressCount = 0

        val machine = MarkerGestureMachine(
            geometry = testGeometry,
            pageLength = { 100 },
            onEvent = { _: WriterEvent -> },
            onDragState = { _: CarriageMotion, _: Float -> },
            onLongPress = { longPressCount++ },
            currentCarriageXDp = { carriageX }
        )

        val accepted = machine.onDown(carriageX, carriageY, timeMs = 0L)
        assertTrue("Touch down within carriage area must be accepted", accepted)

        machine.onTime(timeMs = 479L)
        assertEquals("At 479 ms, onLongPress must NOT have fired yet", 0, longPressCount)

        machine.onTime(timeMs = 480L)
        assertEquals("At 480 ms, onLongPress must fire exactly once", 1, longPressCount)

        machine.onTime(timeMs = 1000L)
        assertEquals("Subsequent time advance must not fire onLongPress again", 1, longPressCount)
    }

    @Test
    fun movingMoreThanSlopCancelsLongPress() {
        val carriageX = testGeometry.carriageCenterXDp(col = 12)
        val carriageY = testGeometry.railYDp + 1f
        var longPressCount = 0
        var lastMotion: CarriageMotion? = null

        val machine = MarkerGestureMachine(
            geometry = testGeometry,
            pageLength = { 100 },
            onEvent = { _: WriterEvent -> },
            onDragState = { motion: CarriageMotion, _: Float -> lastMotion = motion },
            onLongPress = { longPressCount++ },
            currentCarriageXDp = { carriageX }
        )

        // Part A: Move of exactly 6.0 dp has NOT exceeded slop (slop is 6f)
        machine.onDown(carriageX, carriageY, timeMs = 0L)
        machine.onMove(carriageX + 6.0f, carriageY, timeMs = 100L)
        assertNull("Move of exactly 6.0 dp has not exceeded slop; must not enter Dragging", lastMotion)
        machine.onTime(timeMs = 480L)
        assertEquals("Move <= 6.0 dp still fires onLongPress at 480 ms", 1, longPressCount)
        machine.onUp(carriageX + 6.0f, carriageY, timeMs = 500L)

        // Part B: Move of 6.01 dp exceeds slop: cancels long press and enters Dragging
        longPressCount = 0
        lastMotion = null
        machine.onDown(carriageX, carriageY, timeMs = 0L)
        machine.onMove(carriageX + 6.01f, carriageY, timeMs = 100L)
        assertEquals("Move > 6.0 dp enters Dragging motion", CarriageMotion.Dragging, lastMotion)
        machine.onTime(timeMs = 500L)
        assertEquals("Long press must never fire once slop is exceeded", 0, longPressCount)
    }

    @Test
    fun dragEmitsMoveCaretEvents() {
        val carriageX = testGeometry.carriageCenterXDp(col = 12)
        val carriageY = testGeometry.railYDp + 1f
        val emittedEvents = mutableListOf<WriterEvent>()
        val dragStates = mutableListOf<Pair<CarriageMotion, Float>>()

        val machine = MarkerGestureMachine(
            geometry = testGeometry,
            pageLength = { 100 },
            onEvent = { event: WriterEvent -> emittedEvents += event },
            onDragState = { motion: CarriageMotion, x: Float -> dragStates += motion to x },
            onLongPress = {},
            currentCarriageXDp = { carriageX }
        )

        // Touch down slightly off center within interactive area (e.g. carriageX - 4f)
        val touchDownX = carriageX - 4f
        machine.onDown(touchDownX, carriageY, timeMs = 0L)

        // Move 10 dp to the right: fingerX = touchDownX + 10f = carriageX + 6f
        val fingerMove = 10f
        val fingerX = touchDownX + fingerMove
        machine.onMove(fingerX, carriageY, timeMs = 50L)

        assertTrue("Drag states must be emitted", dragStates.isNotEmpty())
        val firstDrag = dragStates.first()
        assertEquals(CarriageMotion.Dragging, firstDrag.first)

        // Carriage does not jump: effective carriage x = fingerX - grabOffset = pre-drag X + fingerMove
        val expectedCarriageX = carriageX + fingerMove
        assertEquals(
            "First drag move leaves carriage at pre-drag position plus finger movement",
            expectedCarriageX,
            firstDrag.second,
            0.001f
        )

        val initialCaretMoves = emittedEvents.filterIsInstance<WriterEvent.MoveCaret>().size
        assertTrue("MoveCaret must be emitted when scrub index changes", initialCaretMoves >= 1)

        // Tiny move that doesn't change scrubIndex should not emit another MoveCaret
        val countBeforeTinyMove = emittedEvents.size
        machine.onMove(fingerX + 0.0001f, carriageY, timeMs = 60L)
        assertEquals(
            "Emits WriterEvent.MoveCaret only when scrubIndex changes",
            countBeforeTinyMove,
            emittedEvents.size
        )
    }

    @Test
    fun releaseEmitsSnap() {
        val carriageX = testGeometry.carriageCenterXDp(col = 12)
        val carriageY = testGeometry.railYDp + 1f
        var lastSnapX: Float? = null

        val machine = MarkerGestureMachine(
            geometry = testGeometry,
            pageLength = { 100 },
            onEvent = { _: WriterEvent -> },
            onDragState = { motion: CarriageMotion, x: Float ->
                if (motion == CarriageMotion.Snap) lastSnapX = x
            },
            onLongPress = {},
            currentCarriageXDp = { carriageX }
        )

        machine.onDown(carriageX, carriageY, timeMs = 0L)
        machine.onMove(carriageX + 20f, carriageY, timeMs = 50L)
        machine.onUp(carriageX + 20f, carriageY, timeMs = 100L)

        assertEquals(carriageX + 20f, lastSnapX!!, 0.001f)
    }

    @Test
    fun tapDoesNothing() {
        val carriageX = testGeometry.carriageCenterXDp(col = 12)
        val carriageY = testGeometry.railYDp + 1f
        var longPressFired = false
        var dragEmitted = false
        var moveCaretEmitted = false

        val machine = MarkerGestureMachine(
            geometry = testGeometry,
            pageLength = { 100 },
            onEvent = { event: WriterEvent -> if (event is WriterEvent.MoveCaret) moveCaretEmitted = true },
            onDragState = { _: CarriageMotion, _: Float -> dragEmitted = true },
            onLongPress = { longPressFired = true },
            currentCarriageXDp = { carriageX }
        )

        // Tap: down at t=0, move 2 dp (<= 6 dp slop), up at t=100 ms
        machine.onDown(carriageX, carriageY, timeMs = 0L)
        machine.onMove(carriageX + 2f, carriageY, timeMs = 50L)
        machine.onUp(carriageX + 2f, carriageY, timeMs = 100L)

        assertFalse("Tap must not fire long press", longPressFired)
        assertFalse("Tap must not emit drag state", dragEmitted)
        assertFalse("Tap must not emit MoveCaret", moveCaretEmitted)
    }

    @Test
    fun longPressConsumedNoDragAfterwards() {
        val carriageX = testGeometry.carriageCenterXDp(col = 12)
        val carriageY = testGeometry.railYDp + 1f
        var longPressCount = 0
        var dragEmitted = false
        var moveCaretEmitted = false

        val machine = MarkerGestureMachine(
            geometry = testGeometry,
            pageLength = { 100 },
            onEvent = { event: WriterEvent -> if (event is WriterEvent.MoveCaret) moveCaretEmitted = true },
            onDragState = { _: CarriageMotion, _: Float -> dragEmitted = true },
            onLongPress = { longPressCount++ },
            currentCarriageXDp = { carriageX }
        )

        machine.onDown(carriageX, carriageY, timeMs = 0L)
        machine.onTime(timeMs = 480L)
        assertEquals(1, longPressCount)

        // Subsequent finger movement after long press consumed
        machine.onMove(carriageX + 30f, carriageY, timeMs = 600L)
        machine.onUp(carriageX + 30f, carriageY, timeMs = 700L)

        assertFalse("No dragging emitted after long press consumed", dragEmitted)
        assertFalse("No MoveCaret emitted after long press consumed", moveCaretEmitted)
    }

    @Test
    fun cancelAfterDragEmitsSnap() {
        val carriageX = testGeometry.carriageCenterXDp(col = 12)
        val carriageY = testGeometry.railYDp + 1f
        var snapEmitted = false
        var lastSnapX: Float? = null

        val machine = MarkerGestureMachine(
            geometry = testGeometry,
            pageLength = { 100 },
            onEvent = { _: WriterEvent -> },
            onDragState = { motion: CarriageMotion, x: Float ->
                if (motion == CarriageMotion.Snap) {
                    snapEmitted = true
                    lastSnapX = x
                }
            },
            onLongPress = {},
            currentCarriageXDp = { carriageX }
        )

        machine.onDown(carriageX, carriageY, timeMs = 0L)
        machine.onMove(carriageX + 25f, carriageY, timeMs = 50L)
        machine.onCancel()

        assertTrue("Pointer cancel while dragging emits CarriageMotion.Snap", snapEmitted)
        assertEquals(carriageX + 25f, lastSnapX!!, 0.001f)
    }

    @Test
    fun cancelBeforeDragEmitsNothing() {
        val carriageX = testGeometry.carriageCenterXDp(col = 12)
        val carriageY = testGeometry.railYDp + 1f
        var callbacksCalled = false

        val machine = MarkerGestureMachine(
            geometry = testGeometry,
            pageLength = { 100 },
            onEvent = { _: WriterEvent -> callbacksCalled = true },
            onDragState = { _: CarriageMotion, _: Float -> callbacksCalled = true },
            onLongPress = { callbacksCalled = true },
            currentCarriageXDp = { carriageX }
        )

        machine.onDown(carriageX, carriageY, timeMs = 0L)
        machine.onMove(carriageX + 2f, carriageY, timeMs = 50L)
        machine.onCancel()

        assertFalse("Cancel before drag emits no callbacks", callbacksCalled)
    }

    @Test
    fun secondFingerDoesNotStartSecondGesture() {
        val carriageX = testGeometry.carriageCenterXDp(col = 12)
        val carriageY = testGeometry.railYDp + 1f

        val machine = MarkerGestureMachine(
            geometry = testGeometry,
            pageLength = { 100 },
            onEvent = { _: WriterEvent -> },
            onDragState = { _: CarriageMotion, _: Float -> },
            onLongPress = {},
            currentCarriageXDp = { carriageX }
        )

        val firstAccepted = machine.onDown(carriageX, carriageY, timeMs = 0L)
        assertTrue("First finger down is accepted", firstAccepted)

        val secondAccepted = machine.onDown(carriageX + 5f, carriageY, timeMs = 10L)
        assertFalse("Second pointer down while tracking is ignored", secondAccepted)
    }

    @Test
    fun touchOutsideCarriageAreaNotConsumed() {
        val carriageX = testGeometry.carriageCenterXDp(col = 12)
        val railY = testGeometry.railYDp

        val machine = MarkerGestureMachine(
            geometry = testGeometry,
            pageLength = { 100 },
            onEvent = { _: WriterEvent -> },
            onDragState = { _: CarriageMotion, _: Float -> },
            onLongPress = {},
            currentCarriageXDp = { carriageX }
        )

        // Interactive area: 54x48 dp centered on carriage body (body top = railY - 17, center = railY + 1)
        // X bounds: [carriageX - 27, carriageX + 27]
        // Y bounds: [railY - 23, railY + 25]

        // Inside bounds
        assertTrue("Inside center", machine.onDown(carriageX, railY + 1f, 0L))
        machine.onCancel()

        assertTrue("Inside left-top corner", machine.onDown(carriageX - 26.9f, railY - 22.9f, 0L))
        machine.onCancel()

        assertTrue("Inside right-bottom corner", machine.onDown(carriageX + 26.9f, railY + 24.9f, 0L))
        machine.onCancel()

        // Outside bounds: return false and not consumed
        assertFalse("Left of carriage area", machine.onDown(carriageX - 27.5f, railY + 1f, 0L))
        assertFalse("Right of carriage area", machine.onDown(carriageX + 27.5f, railY + 1f, 0L))
        assertFalse("Above carriage area", machine.onDown(carriageX, railY - 23.5f, 0L))
        assertFalse("Below carriage area", machine.onDown(carriageX, railY + 25.5f, 0L))
    }

    @Test
    fun dragClampedAtBothEnds() {
        val carriageX = testGeometry.carriageCenterXDp(col = 12)
        val carriageY = testGeometry.railYDp + 1f
        val caretIndices = mutableListOf<Int>()

        val machine = MarkerGestureMachine(
            geometry = testGeometry,
            pageLength = { 100 },
            onEvent = { event: WriterEvent -> if (event is WriterEvent.MoveCaret) caretIndices += event.index },
            onDragState = { _: CarriageMotion, _: Float -> },
            onLongPress = {},
            currentCarriageXDp = { carriageX }
        )

        machine.onDown(carriageX, carriageY, timeMs = 0L)
        // Drag far left
        machine.onMove(-200f, carriageY, timeMs = 50L)
        assertEquals(0, caretIndices.last())

        // Drag far right
        machine.onMove(1000f, carriageY, timeMs = 100L)
        assertEquals(100, caretIndices.last())
    }

    @Test
    fun nanOrInfiniteXDoesNotCrash() {
        val pageLen = 100

        // Pure function handles non-finite numbers safely
        assertEquals(0, scrubIndex(Float.NaN, testGeometry, pageLen))
        assertEquals(0, scrubIndex(Float.NEGATIVE_INFINITY, testGeometry, pageLen))
        assertEquals(pageLen, scrubIndex(Float.POSITIVE_INFINITY, testGeometry, pageLen))

        // State machine rejects non-finite touch coordinates
        val machine = MarkerGestureMachine(
            geometry = testGeometry,
            pageLength = { pageLen },
            onEvent = { _: WriterEvent -> },
            onDragState = { _: CarriageMotion, _: Float -> },
            onLongPress = {},
            currentCarriageXDp = { testGeometry.carriageCenterXDp(col = 12) }
        )
        assertFalse("NaN x onDown rejected", machine.onDown(Float.NaN, testGeometry.railYDp + 1f, 0L))
        assertFalse("Infinite x onDown rejected", machine.onDown(Float.POSITIVE_INFINITY, testGeometry.railYDp + 1f, 0L))
    }

    @Test
    fun surrogatePairSafetyThroughReducer() {
        // "a😀b😀" has 6 UTF-16 code units: 0: 'a', 1-2: 😀, 3: 'b', 4-5: 😀
        val text = "a\uD83D\uDE00b\uD83D\uDE00"
        val page = StruckPage(text = text)
        var state = WriterState(noteId = "n1", pages = listOf(page), pageIndex = 0, caret = 0)

        for (step in 0..100) {
            val x = testGeometry.textLeftDp + (step / 100f) * (testGeometry.cols * testGeometry.columnWidthDp)
            val rawIndex = scrubIndex(x, testGeometry, text.length)
            state = reduce(state, WriterEvent.MoveCaret(rawIndex), CorrectionMode.CleanErase)
            assertTrue(
                "Caret at index ${state.caret} must not split surrogate pair (indices 2 or 5)",
                state.caret != 2 && state.caret != 5
            )
        }
    }

    @Test
    fun holdWithoutMovingFiresLongPressAt480ms_compose() {
        var longPressFired = false
        val carriageX = testGeometry.carriageCenterXDp(col = 12)
        val carriageY = testGeometry.railYDp + 1f

        composeTestRule.setContent {
            Box(
                modifier = Modifier
                    .size(400.dp, 820.dp)
                    .testTag("marker_area")
                    .markerGestures(
                        geometry = testGeometry,
                        pageLength = { 100 },
                        onEvent = { _: WriterEvent -> },
                        onDragState = { _: CarriageMotion, _: Float -> },
                        onLongPress = { longPressFired = true },
                        currentCarriageXDp = { carriageX }
                    )
            )
        }

        composeTestRule.onNodeWithTag("marker_area").performTouchInput {
            down(Offset(carriageX, carriageY))
            advanceEventTime(480L)
            up()
        }

        assertTrue("onLongPress must be fired via Compose touch input", longPressFired)
    }

    @Test
    fun dragEmitsMoveCaretEvents_compose() {
        val emittedEvents = mutableListOf<WriterEvent>()
        val carriageX = testGeometry.carriageCenterXDp(col = 12)
        val carriageY = testGeometry.railYDp + 1f

        composeTestRule.setContent {
            Box(
                modifier = Modifier
                    .size(400.dp, 820.dp)
                    .testTag("marker_area")
                    .markerGestures(
                        geometry = testGeometry,
                        pageLength = { 100 },
                        onEvent = { event: WriterEvent -> emittedEvents += event },
                        onDragState = { _: CarriageMotion, _: Float -> },
                        onLongPress = {},
                        currentCarriageXDp = { carriageX }
                    )
            )
        }

        composeTestRule.onNodeWithTag("marker_area").performTouchInput {
            down(Offset(carriageX, carriageY))
            moveTo(Offset(carriageX + 40f, carriageY))
            up()
        }

        assertTrue(
            "MoveCaret must be emitted via Compose touch input drag",
            emittedEvents.any { it is WriterEvent.MoveCaret }
        )
    }
}
