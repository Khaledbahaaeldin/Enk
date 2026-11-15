package app.enk.keyboard

import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.layout.positionInWindow
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties
import app.enk.design.shell.GlassLevel
import app.enk.design.shell.GlassSource
import app.enk.design.shell.LocalReduceEffects
import app.enk.design.shell.glass
import app.enk.design.theme.LocalShellColors
import app.enk.design.theme.LocalShellMaterial
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.math.abs

@Composable
fun EnkKeyboard(
    layout: KeyboardLayout,
    metrics: KeyboardMetrics,
    onFeedback: (KeyAction) -> Unit,
    onApply: (KeyAction) -> Unit,
    onAlternative: (base: String, alternative: String) -> Unit,
    caretProvider: () -> Int,
    modifier: Modifier = Modifier,
    glassSource: GlassSource? = null,
    initialState: KeyboardState = KeyboardState(),
    clock: () -> Long = System::currentTimeMillis
) {
    var state by remember { mutableStateOf(initialState) }
    val reduceEffects = LocalReduceEffects.current
    val colors = LocalShellColors.current
    val material = LocalShellMaterial.current

    val pageRows = layout.pages[state.page] ?: layout.pages.getValue(PageId.Letters)
    val rows = pageRows.size

    var keyboardWidthPx by remember { mutableFloatStateOf(0f) }
    var keyboardRootOffsetInWindow by remember { mutableStateOf(Offset.Zero) }

    val trayShape = RoundedCornerShape(topStart = 22.dp, topEnd = 22.dp)
    val trayModifier = if (glassSource != null) {
        Modifier.glass(GlassLevel.Thick, trayShape, glassSource)
    } else {
        Modifier.background(colors.thick, trayShape)
    }

    Column(
        modifier = modifier
            .testTag("enk_keyboard")
            .fillMaxWidth()
            .height(metrics.totalHeightDp(rows).dp)
            .onGloballyPositioned { coordinates ->
                keyboardWidthPx = coordinates.size.width.toFloat()
                keyboardRootOffsetInWindow = coordinates.positionInWindow()
            }
            .then(trayModifier)
            .drawBehind {
                val strokeWidth = 0.5.dp.toPx()
                drawLine(
                    color = material.glassLine,
                    start = Offset(0f, strokeWidth / 2f),
                    end = Offset(size.width, strokeWidth / 2f),
                    strokeWidth = strokeWidth
                )
            }
            .padding(
                start = metrics.sidePaddingDp.dp,
                end = metrics.sidePaddingDp.dp,
                top = metrics.topPaddingDp.dp,
                bottom = metrics.bottomPaddingDp.dp
            )
    ) {
        if (reduceEffects) {
            KeyboardPageContent(
                rows = pageRows,
                state = state,
                metrics = metrics,
                onStateChange = { state = it },
                onFeedback = onFeedback,
                onApply = onApply,
                onAlternative = onAlternative,
                caretProvider = caretProvider,
                clock = clock,
                keyboardWidthPx = keyboardWidthPx,
                keyboardRootOffsetInWindow = keyboardRootOffsetInWindow
            )
        } else {
            Crossfade(
                targetState = state.page,
                animationSpec = tween(120),
                label = "KeyboardPageCrossfade"
            ) { targetPage ->
                val currentRows = layout.pages[targetPage] ?: pageRows
                KeyboardPageContent(
                    rows = currentRows,
                    state = state,
                    metrics = metrics,
                    onStateChange = { state = it },
                    onFeedback = onFeedback,
                    onApply = onApply,
                    onAlternative = onAlternative,
                    caretProvider = caretProvider,
                    clock = clock,
                    keyboardWidthPx = keyboardWidthPx,
                    keyboardRootOffsetInWindow = keyboardRootOffsetInWindow
                )
            }
        }
    }
}

@Composable
private fun KeyboardPageContent(
    rows: List<KeyRow>,
    state: KeyboardState,
    metrics: KeyboardMetrics,
    onStateChange: (KeyboardState) -> Unit,
    onFeedback: (KeyAction) -> Unit,
    onApply: (KeyAction) -> Unit,
    onAlternative: (base: String, alternative: String) -> Unit,
    caretProvider: () -> Int,
    clock: () -> Long,
    keyboardWidthPx: Float,
    keyboardRootOffsetInWindow: Offset
) {
    val rowCount = rows.size
    Column(modifier = Modifier.fillMaxWidth()) {
        for ((r, row) in rows.withIndex()) {
            val rowHeight = when {
                rowCount <= 1 -> metrics.keyHeightDp.dp
                r == 0 -> (metrics.keyHeightDp + metrics.gapYDp / 2).dp
                r == rowCount - 1 -> (metrics.keyHeightDp + metrics.gapYDp / 2).dp
                else -> (metrics.keyHeightDp + metrics.gapYDp).dp
            }
            val capPaddingTop = when {
                rowCount <= 1 || r == 0 -> 0.dp
                else -> (metrics.gapYDp / 2).dp
            }
            val capPaddingBottom = when {
                rowCount <= 1 || r == rowCount - 1 -> 0.dp
                else -> (metrics.gapYDp / 2).dp
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(rowHeight)
            ) {
                for (keySpec in row.keys) {
                    if (keySpec is SpacerKey) {
                        Box(
                            modifier = Modifier
                                .weight(keySpec.width)
                                .fillMaxHeight()
                        )
                    } else {
                        KeyView(
                            spec = keySpec,
                            state = state,
                            metrics = metrics,
                            capPaddingTop = capPaddingTop,
                            capPaddingBottom = capPaddingBottom,
                            onStateChange = onStateChange,
                            onFeedback = onFeedback,
                            onApply = onApply,
                            onAlternative = onAlternative,
                            caretProvider = caretProvider,
                            clock = clock,
                            keyboardWidthPx = keyboardWidthPx,
                            keyboardRootOffsetInWindow = keyboardRootOffsetInWindow,
                            modifier = Modifier
                                .weight(keySpec.width)
                                .fillMaxHeight()
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun KeyView(
    spec: KeySpec,
    state: KeyboardState,
    metrics: KeyboardMetrics,
    capPaddingTop: androidx.compose.ui.unit.Dp,
    capPaddingBottom: androidx.compose.ui.unit.Dp,
    onStateChange: (KeyboardState) -> Unit,
    onFeedback: (KeyAction) -> Unit,
    onApply: (KeyAction) -> Unit,
    onAlternative: (base: String, alternative: String) -> Unit,
    caretProvider: () -> Int,
    clock: () -> Long,
    keyboardWidthPx: Float,
    keyboardRootOffsetInWindow: Offset,
    modifier: Modifier = Modifier
) {
    val colors = LocalShellColors.current
    val material = LocalShellMaterial.current
    val reduceEffects = LocalReduceEffects.current
    val scope = rememberCoroutineScope()
    val density = LocalDensity.current
    val currentState by rememberUpdatedState(state)

    var isPressed by remember { mutableStateOf(false) }
    val scaleAnim = remember { Animatable(1f) }

    // Popup/callout states for CharKey
    var showCallout by remember { mutableStateOf(false) }
    // What the key looked like when it was touched: shift is cleared on touch-down, but the callout
    // and the alternatives must keep showing (and later inserting) the case that was typed.
    var stateAtDownShown by remember { mutableStateOf<KeyboardState?>(null) }
    var showPopup by remember { mutableStateOf(false) }
    var popupSelectedIndex by remember { mutableStateOf<Int?>(0) }
    var keyCoordinates by remember { mutableStateOf<LayoutCoordinates?>(null) }
    var keyWidthPx by remember { mutableFloatStateOf(0f) }
    var keyHeightPx by remember { mutableFloatStateOf(0f) }

    val baseFill: Color = when (spec) {
        is CharKey, is SpaceKey -> colors.g1
        is BackspaceKey, is PageKey, is GlobeKey, is EnterKey -> material.field2
        is ShiftKey -> {
            when (state.shift) {
                Shift.Off -> material.field2
                Shift.Once -> colors.g1
                Shift.Caps -> colors.ui
            }
        }
        else -> colors.g1
    }

    val labelColor: Color = when (spec) {
        is ShiftKey -> if (state.shift == Shift.Caps) colors.uiInk else colors.t1
        else -> colors.t1
    }

    val capFill = if (isPressed) {
        lerpColor(baseFill, colors.t1, 0.14f)
    } else {
        baseFill
    }

    val contentDesc = when (spec) {
        is CharKey -> outputFor(spec.label, state)
        is ShiftKey -> when (state.shift) {
            Shift.Off -> "Shift"
            Shift.Once -> "Shift, on"
            Shift.Caps -> "Shift, caps lock"
        }
        is BackspaceKey -> "Delete"
        is SpaceKey -> "Space"
        is EnterKey -> "Return"
        is PageKey -> when (spec.target) {
            PageId.Numbers -> "Numbers"
            PageId.Symbols -> "Symbols"
            PageId.Letters -> "Letters"
        }
        is GlobeKey -> "Switch to system keyboard"
        else -> ""
    }

    val testTag = when (spec) {
        is CharKey -> "key_${spec.label}"
        is ShiftKey -> "key_shift"
        is BackspaceKey -> "key_backspace"
        is SpaceKey -> "key_space"
        is EnterKey -> "key_enter"
        is GlobeKey -> "key_globe"
        is PageKey -> when (spec.target) {
            PageId.Numbers -> "key_page_numbers"
            PageId.Symbols -> "key_page_symbols"
            PageId.Letters -> "key_page_letters"
        }
        else -> ""
    }

    fun animatePressDown() {
        isPressed = true
        if (!reduceEffects) {
            scope.launch {
                scaleAnim.animateTo(0.96f, spring(dampingRatio = 1f, stiffness = 1800f))
            }
        }
    }

    fun animatePressUp() {
        isPressed = false
        if (!reduceEffects) {
            scope.launch {
                scaleAnim.animateTo(1f, spring(dampingRatio = 0.8f, stiffness = 900f))
            }
        }
    }

    Box(
        modifier = modifier
            .testTag(testTag)
            .onGloballyPositioned { coordinates ->
                keyCoordinates = coordinates
                keyWidthPx = coordinates.size.width.toFloat()
                keyHeightPx = coordinates.size.height.toFloat()
            }
            .semantics(mergeDescendants = true) {
                role = Role.Button
                contentDescription = contentDesc
                onClick {
                    when (spec) {
                        is CharKey -> {
                            val out = outputFor(spec.label, state)
                            onFeedback(KeyAction.Character(out))
                            onApply(KeyAction.Character(out))
                            if (state.page == PageId.Letters) {
                                onStateChange(state.afterCharacter())
                            }
                        }
                        is ShiftKey -> onStateChange(state.tapShift(clock()))
                        is BackspaceKey -> {
                            onFeedback(KeyAction.Backspace)
                            onApply(KeyAction.Backspace)
                        }
                        is SpaceKey -> {
                            onFeedback(KeyAction.Space)
                            onApply(KeyAction.Space)
                        }
                        is EnterKey -> {
                            onFeedback(KeyAction.Enter)
                            onApply(KeyAction.Enter)
                        }
                        is PageKey -> onStateChange(state.toPage(spec.target))
                        is GlobeKey -> {
                            onFeedback(KeyAction.SystemKeyboard)
                            onApply(KeyAction.SystemKeyboard)
                        }
                        else -> {}
                    }
                    true
                }
            }
            .pointerInput(spec) {
                awaitEachGesture {
                    val down = awaitPointerEvent().changes.firstOrNull { it.pressed } ?: return@awaitEachGesture
                    val pointerId = down.id
                    down.consume()

                    animatePressDown()

                    when (spec) {
                        is CharKey -> {
                            val stateAtDown = currentState
                            stateAtDownShown = stateAtDown
                            val out = outputFor(spec.label, stateAtDown)
                            onFeedback(KeyAction.Character(out))
                            onApply(KeyAction.Character(out))
                            if (stateAtDown.page == PageId.Letters) {
                                onStateChange(stateAtDown.afterCharacter())
                            }

                            showCallout = true
                            showPopup = false
                            popupSelectedIndex = 0

                            val startPos = down.position
                            var longPressTriggered = false
                            val slop8Px = with(density) { 8.dp.toPx() }

                            val longPressJob = if (spec.popup.isNotEmpty()) {
                                scope.launch {
                                    delay(KeyboardTiming.LONG_PRESS_MS)
                                    longPressTriggered = true
                                    showCallout = false
                                    showPopup = true
                                    popupSelectedIndex = 0
                                }
                            } else null

                            while (true) {
                                val event = awaitPointerEvent()
                                val change = event.changes.firstOrNull { it.id == pointerId } ?: break

                                if (!change.pressed) {
                                    // Pointer released!
                                    change.consume()
                                    longPressJob?.cancel()
                                    if (longPressTriggered && spec.popup.isNotEmpty()) {
                                        val selected = popupSelectedIndex
                                        if (selected != null && selected in spec.popup.indices) {
                                            val alt = spec.popup[selected]
                                            val altResolved = alternativeFor(alt, stateAtDown)
                                            onAlternative(out, altResolved)
                                        }
                                    }
                                    break
                                }

                                val moveDist = (change.position - startPos).getDistance()
                                if (!longPressTriggered) {
                                    if (moveDist > slop8Px) {
                                        longPressJob?.cancel()
                                    }
                                }

                                if ((longPressTriggered || showPopup) && spec.popup.isNotEmpty()) {
                                    val coords = keyCoordinates
                                    if (coords != null && coords.isAttached) {
                                        val keyPosInRoot = coords.positionInRoot()
                                        val fingerXInRoot = keyPosInRoot.x + change.position.x
                                        val keyCenterXInRoot = keyPosInRoot.x + keyWidthPx / 2f
                                        val popupTotalWidthPx = with(density) { (KeyboardTiming.POPUP_SLOT_WIDTH_DP * spec.popup.size + 8f).dp.toPx() }

                                        val nominalLeft = if (keyCenterXInRoot > keyboardWidthPx / 2f) {
                                            keyPosInRoot.x + keyWidthPx - popupTotalWidthPx
                                        } else {
                                            keyPosInRoot.x
                                        }
                                        val minLeft = with(density) { 4.dp.toPx() }
                                        val maxLeft = keyboardWidthPx - popupTotalWidthPx - with(density) { 4.dp.toPx() }
                                        val clampedLeft = nominalLeft.coerceIn(minLeft, maxLeft.coerceAtLeast(minLeft))
                                        val slotsLeftInRoot = clampedLeft + with(density) { 4.dp.toPx() }

                                        val fingerXDp = with(density) { fingerXInRoot.toDp().value }
                                        val slotsLeftDp = with(density) { slotsLeftInRoot.toDp().value }
                                        val idx = popupIndexAt(fingerXDp, slotsLeftDp, spec.popup.size)
                                        popupSelectedIndex = idx
                                    }
                                }
                            }

                            longPressJob?.cancel()
                            showCallout = false
                            showPopup = false
                            stateAtDownShown = null
                            animatePressUp()
                        }

                        is SpaceKey -> {
                            onFeedback(KeyAction.Space)
                            val startX = down.position.x
                            var isCursorDrag = false
                            var slopCrossingX = 0f
                            var originCaret = 0
                            var lastColumns = 0

                            val slopPx = with(density) { KeyboardTiming.CURSOR_DRAG_SLOP_DP.dp.toPx() }

                            while (true) {
                                val event = awaitPointerEvent()
                                val change = event.changes.firstOrNull { it.id == pointerId } ?: break

                                if (!change.pressed) {
                                    change.consume()
                                    if (!isCursorDrag) {
                                        onApply(KeyAction.Space)
                                    }
                                    break
                                }

                                val dxTotal = change.position.x - startX
                                if (!isCursorDrag) {
                                    if (abs(dxTotal) > slopPx) {
                                        isCursorDrag = true
                                        originCaret = caretProvider()
                                        slopCrossingX = if (dxTotal > 0) startX + slopPx else startX - slopPx
                                        lastColumns = 0
                                    }
                                }

                                if (isCursorDrag) {
                                    val dxFromSlopPx = change.position.x - slopCrossingX
                                    val dxFromSlopDp = with(density) { dxFromSlopPx.toDp().value }
                                    val cols = cursorColumns(dxFromSlopDp)
                                    if (cols != lastColumns) {
                                        lastColumns = cols
                                        onApply(KeyAction.MoveCaretBy(cols, originCaret))
                                        onFeedback(KeyAction.MoveCaretBy(cols, originCaret))
                                    }
                                }
                            }

                            animatePressUp()
                        }

                        is EnterKey -> {
                            onFeedback(KeyAction.Enter)
                            onApply(KeyAction.Enter)

                            while (true) {
                                val event = awaitPointerEvent()
                                val change = event.changes.firstOrNull { it.id == pointerId } ?: break
                                if (!change.pressed) {
                                    change.consume()
                                    break
                                }
                            }
                            animatePressUp()
                        }

                        is BackspaceKey -> {
                            onFeedback(KeyAction.Backspace)
                            onApply(KeyAction.Backspace)

                            val repeatJob = scope.launch {
                                delay(KeyboardTiming.BACKSPACE_FIRST_REPEAT_MS)
                                var repeatCount = 1
                                while (isActive) {
                                    onFeedback(KeyAction.Backspace)
                                    onApply(KeyAction.Backspace)
                                    val delayMs = KeyboardTiming.backspaceRepeatDelayMs(repeatCount)
                                    repeatCount++
                                    delay(delayMs)
                                }
                            }

                            while (true) {
                                val event = awaitPointerEvent()
                                val change = event.changes.firstOrNull { it.id == pointerId } ?: break
                                if (!change.pressed) {
                                    change.consume()
                                    break
                                }
                            }

                            repeatJob.cancel()
                            animatePressUp()
                        }

                        is ShiftKey -> {
                            var released = false
                            while (true) {
                                val event = awaitPointerEvent()
                                val change = event.changes.firstOrNull { it.id == pointerId } ?: break
                                if (!change.pressed) {
                                    change.consume()
                                    released = true
                                    break
                                }
                            }
                            if (released) {
                                onStateChange(currentState.tapShift(clock()))
                            }
                            animatePressUp()
                        }

                        is PageKey -> {
                            var released = false
                            while (true) {
                                val event = awaitPointerEvent()
                                val change = event.changes.firstOrNull { it.id == pointerId } ?: break
                                if (!change.pressed) {
                                    change.consume()
                                    released = true
                                    break
                                }
                            }
                            if (released) {
                                onStateChange(currentState.toPage(spec.target))
                            }
                            animatePressUp()
                        }

                        is GlobeKey -> {
                            var released = false
                            while (true) {
                                val event = awaitPointerEvent()
                                val change = event.changes.firstOrNull { it.id == pointerId } ?: break
                                if (!change.pressed) {
                                    change.consume()
                                    released = true
                                    break
                                }
                            }
                            if (released) {
                                onFeedback(KeyAction.SystemKeyboard)
                                onApply(KeyAction.SystemKeyboard)
                            }
                            animatePressUp()
                        }

                        else -> {
                            while (true) {
                                val event = awaitPointerEvent()
                                val change = event.changes.firstOrNull { it.id == pointerId } ?: break
                                if (!change.pressed) {
                                    change.consume()
                                    break
                                }
                            }
                            animatePressUp()
                        }
                    }
                }
            }
            .padding(
                start = (metrics.gapXDp / 2).dp,
                end = (metrics.gapXDp / 2).dp,
                top = capPaddingTop,
                bottom = capPaddingBottom
            )
    ) {
        // Visible Key Cap
        Box(
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer {
                    val s = if (reduceEffects) 1f else scaleAnim.value
                    scaleX = s
                    scaleY = s
                }
                .clip(RoundedCornerShape(8.dp))
                .background(capFill)
                .drawBehind {
                    val strokeH = 1.dp.toPx()
                    drawLine(
                        color = material.sh1,
                        start = Offset(0f, size.height - strokeH / 2f),
                        end = Offset(size.width, size.height - strokeH / 2f),
                        strokeWidth = strokeH
                    )
                },
            contentAlignment = Alignment.Center
        ) {
            when (spec) {
                is CharKey -> {
                    Text(
                        text = outputFor(spec.label, state),
                        color = labelColor,
                        fontSize = metrics.letterSp.sp,
                        fontWeight = FontWeight.Normal,
                        fontFamily = FontFamily.Default
                    )
                }
                is PageKey -> {
                    Text(
                        text = spec.label,
                        color = labelColor,
                        fontSize = metrics.functionSp.sp,
                        fontWeight = FontWeight.Medium,
                        fontFamily = FontFamily.Default
                    )
                }
                is ShiftKey -> {
                    ShiftIcon(shift = state.shift, color = labelColor)
                }
                is BackspaceKey -> {
                    BackspaceIcon(color = labelColor)
                }
                is EnterKey -> {
                    EnterIcon(color = labelColor)
                }
                is GlobeKey -> {
                    GlobeIcon(color = labelColor)
                }
                is SpaceKey -> {
                    // Space: no label
                }
                else -> {}
            }
        }

        // Callout Bubble for CharKey
        if (spec is CharKey && showCallout && !showPopup) {
            val out = outputFor(spec.label, stateAtDownShown ?: state)
            val bubbleWidthDp = with(density) { keyWidthPx.toDp() + 16.dp }
            val bubbleHeightDp = with(density) { keyHeightPx.toDp() + 22.dp }
            val bubbleWidthPx = with(density) { bubbleWidthDp.toPx() }

            val coords = keyCoordinates
            val keyLeftInRoot = coords?.positionInRoot()?.x ?: 0f
            val keyCenterXInRoot = keyLeftInRoot + keyWidthPx / 2f

            val nominalLeft = keyCenterXInRoot - bubbleWidthPx / 2f
            val minLeft = with(density) { 4.dp.toPx() }
            val maxRight = keyboardWidthPx - with(density) { 4.dp.toPx() }

            var shiftXPx = 0
            if (nominalLeft < minLeft) {
                shiftXPx = (minLeft - nominalLeft).toInt()
            } else if (nominalLeft + bubbleWidthPx > maxRight) {
                shiftXPx = (maxRight - (nominalLeft + bubbleWidthPx)).toInt()
            }

            val calloutYOffset = -(keyHeightPx + with(density) { 6.dp.toPx() }).toInt()

            Popup(
                alignment = Alignment.TopCenter,
                offset = IntOffset(shiftXPx, calloutYOffset),
                properties = PopupProperties(focusable = false, clippingEnabled = false)
            ) {
                Box(
                    modifier = Modifier
                        .size(bubbleWidthDp, bubbleHeightDp)
                        .shadow(6.dp, RoundedCornerShape(12.dp))
                        .clip(RoundedCornerShape(12.dp))
                        .background(colors.g1),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = out,
                        color = colors.t1,
                        fontSize = 34.sp,
                        fontWeight = FontWeight.Normal,
                        fontFamily = FontFamily.Default
                    )
                }
            }
        }

        // Popup Alternatives for CharKey
        if (spec is CharKey && showPopup && spec.popup.isNotEmpty()) {
            val slotCount = spec.popup.size
            val slotsWidthDp = KeyboardTiming.POPUP_SLOT_WIDTH_DP.dp * slotCount
            val popupWidthDp = slotsWidthDp + 8.dp // 4 dp padding each side
            val popupHeightDp = with(density) { keyHeightPx.toDp() + 14.dp }
            val popupWidthPx = with(density) { popupWidthDp.toPx() }

            val coords = keyCoordinates
            val keyLeftInRoot = coords?.positionInRoot()?.x ?: 0f
            val keyCenterXInRoot = keyLeftInRoot + keyWidthPx / 2f

            val inRightHalf = keyCenterXInRoot > keyboardWidthPx / 2f
            val nominalLeft = if (inRightHalf) {
                keyLeftInRoot + keyWidthPx - popupWidthPx
            } else {
                keyLeftInRoot
            }

            val minLeft = with(density) { 4.dp.toPx() }
            val maxLeft = keyboardWidthPx - popupWidthPx - with(density) { 4.dp.toPx() }
            val clampedLeft = nominalLeft.coerceIn(minLeft, maxLeft.coerceAtLeast(minLeft))
            val shiftXPx = (clampedLeft - keyLeftInRoot).toInt()

            val popupYOffset = -(with(density) { popupHeightDp.toPx() + 6.dp.toPx() }).toInt()

            Popup(
                alignment = Alignment.TopStart,
                offset = IntOffset(shiftXPx, popupYOffset),
                properties = PopupProperties(focusable = false, clippingEnabled = false)
            ) {
                Box(
                    modifier = Modifier
                        .width(popupWidthDp)
                        .height(popupHeightDp)
                        .shadow(8.dp, RoundedCornerShape(14.dp))
                        .clip(RoundedCornerShape(14.dp))
                        .background(colors.g1)
                        .padding(4.dp)
                ) {
                    Row(modifier = Modifier.fillMaxSize()) {
                        for ((idx, alt) in spec.popup.withIndex()) {
                            val isSelected = (idx == popupSelectedIndex)
                            val slotBg = if (isSelected) colors.ui else Color.Transparent
                            val slotTextColor = if (isSelected) colors.uiInk else colors.t1

                            Box(
                                modifier = Modifier
                                    .width(KeyboardTiming.POPUP_SLOT_WIDTH_DP.dp)
                                    .fillMaxHeight()
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(slotBg),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = alternativeFor(alt, stateAtDownShown ?: state),
                                    color = slotTextColor,
                                    fontSize = 24.sp,
                                    fontWeight = FontWeight.Normal,
                                    fontFamily = FontFamily.Default
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

private fun lerpColor(start: Color, stop: Color, fraction: Float): Color {
    val f = fraction.coerceIn(0f, 1f)
    return Color(
        red = start.red + (stop.red - start.red) * f,
        green = start.green + (stop.green - start.green) * f,
        blue = start.blue + (stop.blue - start.blue) * f,
        alpha = start.alpha + (stop.alpha - start.alpha) * f
    )
}

@Composable
private fun ShiftIcon(shift: Shift, color: Color) {
    Canvas(modifier = Modifier.size(22.dp)) {
        val w = size.width
        val h = size.height
        val strokeW = 1.8.dp.toPx()

        val arrowPath = Path().apply {
            moveTo(w * 0.5f, h * 0.14f)
            lineTo(w * 0.82f, h * 0.46f)
            lineTo(w * 0.64f, h * 0.46f)
            lineTo(w * 0.64f, h * 0.78f)
            lineTo(w * 0.36f, h * 0.78f)
            lineTo(w * 0.36f, h * 0.46f)
            lineTo(w * 0.18f, h * 0.46f)
            close()
        }

        when (shift) {
            Shift.Off -> {
                drawPath(
                    path = arrowPath,
                    color = color,
                    style = Stroke(width = strokeW, cap = StrokeCap.Round, join = StrokeJoin.Round)
                )
            }
            Shift.Once -> {
                drawPath(
                    path = arrowPath,
                    color = color,
                    style = Fill
                )
            }
            Shift.Caps -> {
                drawPath(
                    path = arrowPath,
                    color = color,
                    style = Fill
                )
                drawLine(
                    color = color,
                    start = Offset(w * 0.22f, h * 0.90f),
                    end = Offset(w * 0.78f, h * 0.90f),
                    strokeWidth = strokeW,
                    cap = StrokeCap.Round
                )
            }
        }
    }
}

@Composable
private fun BackspaceIcon(color: Color) {
    Canvas(modifier = Modifier.size(22.dp)) {
        val w = size.width
        val h = size.height
        val strokeW = 1.8.dp.toPx()

        // Pentagon tag pointing left
        val tagPath = Path().apply {
            moveTo(w * 0.12f, h * 0.50f)
            lineTo(w * 0.40f, h * 0.20f)
            lineTo(w * 0.88f, h * 0.20f)
            lineTo(w * 0.88f, h * 0.80f)
            lineTo(w * 0.40f, h * 0.80f)
            close()
        }

        drawPath(
            path = tagPath,
            color = color,
            style = Stroke(width = strokeW, cap = StrokeCap.Round, join = StrokeJoin.Round)
        )

        // Diagonal cross
        drawLine(
            color = color,
            start = Offset(w * 0.52f, h * 0.38f),
            end = Offset(w * 0.74f, h * 0.62f),
            strokeWidth = strokeW,
            cap = StrokeCap.Round
        )
        drawLine(
            color = color,
            start = Offset(w * 0.52f, h * 0.62f),
            end = Offset(w * 0.74f, h * 0.38f),
            strokeWidth = strokeW,
            cap = StrokeCap.Round
        )
    }
}

@Composable
private fun EnterIcon(color: Color) {
    Canvas(modifier = Modifier.size(22.dp)) {
        val w = size.width
        val h = size.height
        val strokeW = 1.8.dp.toPx()

        // L-shaped return arrow
        val arrowPath = Path().apply {
            moveTo(w * 0.78f, h * 0.25f)
            lineTo(w * 0.78f, h * 0.60f)
            lineTo(w * 0.22f, h * 0.60f)
        }

        drawPath(
            path = arrowPath,
            color = color,
            style = Stroke(width = strokeW, cap = StrokeCap.Round, join = StrokeJoin.Round)
        )

        // Arrow head pointing left
        drawLine(
            color = color,
            start = Offset(w * 0.42f, h * 0.40f),
            end = Offset(w * 0.22f, h * 0.60f),
            strokeWidth = strokeW,
            cap = StrokeCap.Round
        )
        drawLine(
            color = color,
            start = Offset(w * 0.42f, h * 0.80f),
            end = Offset(w * 0.22f, h * 0.60f),
            strokeWidth = strokeW,
            cap = StrokeCap.Round
        )
    }
}

@Composable
private fun GlobeIcon(color: Color) {
    Canvas(modifier = Modifier.size(22.dp)) {
        val w = size.width
        val h = size.height
        val strokeW = 1.8.dp.toPx()
        val radius = w * 0.40f
        val center = Offset(w * 0.5f, h * 0.5f)

        // Outer circle
        drawCircle(
            color = color,
            radius = radius,
            center = center,
            style = Stroke(width = strokeW)
        )

        // Equator
        drawLine(
            color = color,
            start = Offset(w * 0.10f, h * 0.5f),
            end = Offset(w * 0.90f, h * 0.5f),
            strokeWidth = strokeW,
            cap = StrokeCap.Round
        )

        // Meridian lines
        val meridianPath = Path().apply {
            moveTo(w * 0.5f, h * 0.10f)
            cubicTo(
                w * 0.25f, h * 0.25f,
                w * 0.25f, h * 0.75f,
                w * 0.5f, h * 0.90f
            )
            moveTo(w * 0.5f, h * 0.10f)
            cubicTo(
                w * 0.75f, h * 0.25f,
                w * 0.75f, h * 0.75f,
                w * 0.5f, h * 0.90f
            )
        }
        drawPath(
            path = meridianPath,
            color = color,
            style = Stroke(width = strokeW, cap = StrokeCap.Round)
        )
    }
}
