package app.enk.writer

import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import app.enk.design.EnkMotion
import kotlinx.coroutines.Job
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.hypot
import kotlin.math.roundToInt

object MarkerTuning {
    const val LONG_PRESS_MS: Long = EnkMotion.longPressMs
    const val SLOP_DP: Float = EnkMotion.longPressSlopDp
}

fun scrubIndex(xDp: Float, geometry: WriterGeometry, pageLength: Int): Int {
    if (pageLength <= 0) return 0
    val left = geometry.textLeftDp
    val width = geometry.cols * geometry.columnWidthDp
    if (width <= 0f) return 0
    if (xDp.isNaN()) return 0
    val clampedX = xDp.coerceIn(left, left + width)
    val fraction = (clampedX - left) / width
    return (fraction * pageLength).roundToInt().coerceIn(0, pageLength)
}

enum class MarkerState {
    Idle,
    Pressed,
    Dragging,
    Consumed
}

class MarkerGestureMachine(
    val geometry: WriterGeometry,
    val pageLength: () -> Int,
    val onEvent: (WriterEvent) -> Unit,
    val onDragState: (CarriageMotion, xDp: Float) -> Unit,
    val onLongPress: () -> Unit,
    val currentCarriageXDp: () -> Float = { geometry.carriageCenterXDp(0) },
    val tuning: MarkerTuning = MarkerTuning
) {
    var state: MarkerState = MarkerState.Idle
        private set

    private var downX: Float = 0f
    private var downY: Float = 0f
    private var downTimeMs: Long = 0L
    private var grabOffset: Float = 0f
    private var lastCarriageX: Float = 0f
    private var lastEmittedIndex: Int? = null

    fun onDown(xDp: Float, yDp: Float, timeMs: Long): Boolean {
        if (!xDp.isFinite() || !yDp.isFinite()) return false
        if (state != MarkerState.Idle) return false

        val carriageX = currentCarriageXDp()
        val railY = geometry.railYDp
        val carriageCenterY = railY + 1f

        val minX = carriageX - 27f
        val maxX = carriageX + 27f
        val minY = carriageCenterY - 24f
        val maxY = carriageCenterY + 24f

        if (xDp !in minX..maxX || yDp !in minY..maxY) {
            return false
        }

        downX = xDp
        downY = yDp
        downTimeMs = timeMs
        grabOffset = carriageX - xDp
        lastCarriageX = carriageX
        lastEmittedIndex = null
        state = MarkerState.Pressed
        return true
    }

    fun onTime(timeMs: Long) {
        if (state == MarkerState.Pressed && (timeMs - downTimeMs) >= tuning.LONG_PRESS_MS) {
            state = MarkerState.Consumed
            onLongPress()
        }
    }

    fun onMove(xDp: Float, yDp: Float, timeMs: Long) {
        if (!xDp.isFinite() || !yDp.isFinite()) return
        if (state == MarkerState.Idle || state == MarkerState.Consumed) return

        if (state == MarkerState.Pressed) {
            val dist = hypot(xDp - downX, yDp - downY)
            if (dist > tuning.SLOP_DP) {
                state = MarkerState.Dragging
            }
        }

        if (state == MarkerState.Dragging) {
            val effectiveCarriageX = xDp + grabOffset
            lastCarriageX = effectiveCarriageX
            onDragState(CarriageMotion.Dragging, effectiveCarriageX)
            val targetIndex = scrubIndex(effectiveCarriageX, geometry, pageLength())
            if (targetIndex != lastEmittedIndex) {
                lastEmittedIndex = targetIndex
                onEvent(WriterEvent.MoveCaret(targetIndex))
            }
        }
    }

    fun onUp(xDp: Float, yDp: Float, timeMs: Long) {
        if (state == MarkerState.Dragging) {
            onDragState(CarriageMotion.Snap, lastCarriageX)
        }
        state = MarkerState.Idle
        lastEmittedIndex = null
    }

    fun onCancel() {
        if (state == MarkerState.Dragging) {
            onDragState(CarriageMotion.Snap, lastCarriageX)
        }
        state = MarkerState.Idle
        lastEmittedIndex = null
    }
}

fun Modifier.markerGestures(
    geometry: WriterGeometry,
    pageLength: () -> Int,
    onEvent: (WriterEvent) -> Unit,
    onDragState: (CarriageMotion, xDp: Float) -> Unit,
    onLongPress: () -> Unit,
    currentCarriageXDp: () -> Float = { geometry.carriageCenterXDp(0) },
    tuning: MarkerTuning = MarkerTuning
): Modifier = pointerInput(geometry, tuning) {
    coroutineScope {
        awaitEachGesture {
            val machine = MarkerGestureMachine(
                geometry = geometry,
                pageLength = pageLength,
                onEvent = onEvent,
                onDragState = onDragState,
                onLongPress = onLongPress,
                currentCarriageXDp = currentCarriageXDp,
                tuning = tuning
            )

            val down = awaitFirstDown(requireUnconsumed = false)
            val downX = down.position.x.toDp().value
            val downY = down.position.y.toDp().value
            val downTime = down.uptimeMillis

            val accepted = machine.onDown(downX, downY, downTime)
            if (!accepted) {
                return@awaitEachGesture
            }

            down.consume()

            var longPressJob: Job? = this@coroutineScope.launch {
                delay(tuning.LONG_PRESS_MS)
                machine.onTime(downTime + tuning.LONG_PRESS_MS)
            }

            try {
                val pointerId = down.id
                while (true) {
                    val event = awaitPointerEvent()
                    val change = event.changes.firstOrNull { it.id == pointerId }
                    if (change == null || change.isConsumed) {
                        machine.onCancel()
                        break
                    }

                    machine.onTime(change.uptimeMillis)

                    if (!change.pressed) {
                        val upX = change.position.x.toDp().value
                        val upY = change.position.y.toDp().value
                        machine.onUp(upX, upY, change.uptimeMillis)
                        change.consume()
                        break
                    }

                    val currentX = change.position.x.toDp().value
                    val currentY = change.position.y.toDp().value
                    val dist = hypot(currentX - downX, currentY - downY)
                    if (dist > tuning.SLOP_DP) {
                        longPressJob?.cancel()
                        longPressJob = null
                    }

                    machine.onMove(currentX, currentY, change.uptimeMillis)
                    if (machine.state == MarkerState.Dragging) {
                        change.consume()
                    }
                }
            } finally {
                longPressJob?.cancel()
                if (machine.state != MarkerState.Idle) {
                    machine.onCancel()
                }
            }
        }
    }
}
