package app.enk.writer

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import app.enk.design.effects.LocalEffects
import app.enk.design.effects.ResolvedEffects
import app.enk.settings.CorrectionMode
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.platform.LocalConfiguration
import app.enk.design.shell.glassSource
import app.enk.design.shell.rememberGlassSource
import app.enk.keyboard.EnkKeyboard
import app.enk.keyboard.KeyAction
import app.enk.keyboard.KeyboardLayout
import app.enk.keyboard.KeyboardMetrics
import app.enk.keyboard.KeyboardToWriter
import app.enk.keyboard.hardwareKeyToAction
import app.enk.settings.KeyboardMode
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import app.enk.design.logo.LogoSpec
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import app.enk.R
import app.enk.audio.BellPolicy
import app.enk.audio.Sample
import app.enk.audio.SoundEvent
import app.enk.audio.SoundPlayer
import app.enk.audio.SoundSet
import app.enk.design.EnkColors
import app.enk.design.EnkType
import app.enk.design.LampColor
import app.enk.design.LampLight
import app.enk.design.LocalMonoFont
import app.enk.design.lampTextMask
import app.enk.settings.DarkPaper
import app.enk.settings.Settings
import androidx.compose.foundation.isSystemInDarkTheme
import app.enk.text.GridLayout
import app.enk.text.LineSpan
import app.enk.text.StruckPage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext

val WriterState.currentPage: StruckPage
    get() = pages.getOrElse(pageIndex) { StruckPage("") }

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
fun WriterScreen(
    viewModel: WriterViewModel,
    settings: Settings,
    player: SoundPlayer,
    haptics: Haptics,
    onOpenBox: () -> Unit,
    modifier: Modifier = Modifier,
    soundSets: List<SoundSet> = emptyList(),
    onSettingsChange: (Settings.() -> Settings) -> Unit = {},
    /** Non-null only while the start animation runs; see [WriterIntro]. */
    intro: WriterIntro? = null,
    keyboardLayout: KeyboardLayout? = null,
    strikes: StrikeLog = remember { StrikeLog() },
    onTypeOnActiveChange: ((Boolean) -> Unit)? = null,
    typeOnGate: () -> Boolean = { true }
) {
    val state by viewModel.state.collectAsState()
    val page = state.currentPage
    val keyboardController = LocalSoftwareKeyboardController.current

    // Lifecycle ON_STOP flush
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_STOP) {
                viewModel.flushAsync()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    val resolvedEffects = LocalEffects.current
    val reduceMotion = rememberReduceMotion(resolvedEffects)

    // Sound set resolution
    val defaultSoundSet = remember {
        SoundSet(
            id = "classic",
            name = "Classic",
            keys = listOf(Sample("sounds/classic/key_01.ogg")),
            space = Sample("sounds/classic/space.ogg"),
            enter = Sample("sounds/classic/enter.ogg"),
            backspace = Sample("sounds/classic/backspace.ogg"),
            bell = Sample("sounds/classic/bell.ogg")
        )
    }
    val soundSet = remember(settings.soundSet, soundSets) {
        soundSets.firstOrNull { it.id.equals(settings.soundSet.name, ignoreCase = true) } ?: defaultSoundSet
    }

    // Prepare sound set off main thread on changes (Finding 10)
    LaunchedEffect(settings.soundSet, soundSet) {
        withContext(Dispatchers.IO) {
            try {
                player.prepare(soundSet)
            } catch (_: Exception) {}
        }
    }

    // Night progress & lamp tokens
    val nightProgress by rememberNightProgress(lampOn = state.lampOn, reduceEffects = reduceMotion)
    val lampRgb = remember(settings.warmth) { LampColor.fromWarmth(settings.warmth) }
    val lampColor = Color(lampRgb.r, lampRgb.g, lampRgb.b)
    val lampSettings = remember(settings.warmth, settings.intensity, settings.beam) {
        lampSettingsFromSettings(settings)
    }

    val chassisBrush = Brush.verticalGradient(
        colors = listOf(
            lerpColor(EnkColors.chassisTop, EnkColors.chassisNightTop, nightProgress),
            lerpColor(EnkColors.chassisBottom, EnkColors.chassisNightBottom, nightProgress)
        )
    )

    // Strike and typing feedback (Finding 6: TypingPolicy)
    var strikeKey by remember { mutableStateOf(0) }
    var slugChar by remember { mutableStateOf<Char?>(null) }
    var lastKeyMs by remember { mutableStateOf(0L) }

    // Carriage motion state
    var carriageMotion by remember { mutableStateOf(CarriageMotion.Rest) }
    var dragCarriageXDp by remember { mutableStateOf<Float?>(null) }
    var motionToken by remember { mutableIntStateOf(0) }

    // Type-on state
    var typeOnActive by remember { mutableStateOf(false) }
    var typeOnToken by remember { mutableIntStateOf(0) }
    var typeOnElapsedMs by remember { mutableFloatStateOf(-1f) }

    LaunchedEffect(typeOnActive) {
        onTypeOnActiveChange?.invoke(typeOnActive)
    }

    val thump = remember { Spring1D(StrikeTiming.THUMP_RESPONSE_S, StrikeTiming.THUMP_DAMPING) }
    val clockMs = remember { mutableFloatStateOf(0f) }
    var pendingThumpAtMs by remember { mutableFloatStateOf(-1f) }
    var pendingThumpImpulse by remember { mutableFloatStateOf(0f) }

    // Strike times are small Floats counted from when the writer appeared: a Float of the raw uptime
    // is 8 ms coarse after a day and 64 ms after ten days, which would wreck 26 ms timing.
    val clockBaseMs = remember { android.os.SystemClock.uptimeMillis().toDouble() }
    fun frameMs(frameNanos: Long): Float = (frameNanos / 1_000_000.0 - clockBaseMs).toFloat()
    fun refreshClock(): Float {
        val t = (android.os.SystemClock.uptimeMillis() - clockBaseMs).toFloat()
        clockMs.floatValue = t
        return t
    }
    val thumpDp = remember { mutableFloatStateOf(0f) }

    LaunchedEffect(strikeKey) {
        if (strikeKey > 0) {
            var lastFrameMs = 0f
            while (strikes.hasActive(clockMs.floatValue) || !thump.atRest || pendingThumpAtMs >= 0f) {
                val now = frameMs(withFrameNanos { it })
                val dt = if (lastFrameMs > 0f) (now - lastFrameMs) / 1000f else 0.016f
                lastFrameMs = now
                clockMs.floatValue = now
                if (pendingThumpAtMs >= 0f && now >= pendingThumpAtMs) {
                    thump.impulse(pendingThumpImpulse)
                    pendingThumpAtMs = -1f
                    pendingThumpImpulse = 0f
                }
                thump.step(dt)
                thumpDp.floatValue = thump.x
            }
            thumpDp.floatValue = thump.x
        }
    }

    // The caret is solid while typing and goes back to blinking once the idle window has passed.
    var typingActive by remember { mutableStateOf(false) }
    LaunchedEffect(lastKeyMs) {
        if (lastKeyMs != 0L) {
            typingActive = true
            delay(TypingPolicy.IDLE_MS)
            typingActive = false
        }
    }
    val isTyping = typingActive || carriageMotion == CarriageMotion.Dragging

    // Dark mode: "Lamp on" lights the lamp itself and follows the phone both ways; the lamp can
    // still be toggled by hand until the phone changes mode again.
    val dark = DarkPaperPolicy.isDark(settings.themeMode, isSystemInDarkTheme())
    val wantLamp = DarkPaperPolicy.autoLamp(settings.darkPaper, dark)
    val ready = state.loaded && intro == null
    LaunchedEffect(wantLamp, ready) {
        if (ready && wantLamp != null && viewModel.state.value.lampOn != wantLamp) {
            viewModel.dispatch(WriterEvent.ToggleLamp)
        }
    }
    // The question is asked once: an answer is kept, and writing on without one means normal paper.
    val showAsk = ready && DarkPaperPolicy.shouldAsk(settings.darkPaper, dark)
    LaunchedEffect(showAsk, lastKeyMs) {
        if (showAsk && lastKeyMs != 0L) {
            delay(4000)
            onSettingsChange { copy(darkPaper = DarkPaper.Normal) }
        }
    }

    val latestSettings by rememberUpdatedState(settings)
    fun playSound(event: SoundEvent, volume: Float = 1f) {
        if (latestSettings.muted) return
        try { player.play(event, soundSet, volume) } catch (_: Exception) {}
    }
    fun pulse(ms: Int) {
        if (!latestSettings.haptics) return
        try { haptics.pulse(ms) } catch (_: Exception) {}
    }

    // Hint policy (Finding 5)
    var hintPolicy by remember(settings.hintDismissed) {
        mutableStateOf(
            HintPolicy(
                hasScrubbed = settings.hintDismissed,
                hasLongPressed = settings.hintDismissed
            )
        )
    }

    val imeBridge = remember { ImeBridge() }
    val focusRequester = remember { FocusRequester() }

    // Mode determination
    var systemOverride by rememberSaveable { mutableStateOf(false) }
    val enkWanted = if (settings.keyboard == KeyboardMode.Enk) !systemOverride else systemOverride
    // The Enk keyboard is part of the layout from the first frame, so the paper never resizes when
    // the start animation ends; only focus waits for the animation (see the effects below).
    val useEnk = enkWanted && keyboardLayout != null
    val useSystemKeyboard = !useEnk

    val glassSource = rememberGlassSource()
    val rootFocus = remember { FocusRequester() }
    val configuration = LocalConfiguration.current
    val keyboardMetrics = remember(configuration.screenHeightDp) {
        KeyboardMetrics.forWindow(configuration.screenHeightDp.toFloat())
    }
    var columnsForKeys by remember { mutableIntStateOf(28) }
    val bellPolicy = remember(columnsForKeys) { BellPolicy(columnsForKeys) }
    fun ringBellAt(text: String, index: Int) {
        val cols = columnsForKeys
        val pos = GridLayout.caret(GridLayout.wrap(text, cols), index, cols)
        if (bellPolicy.onCaret(pos.line, pos.col)) {
            playSound(SoundEvent.Bell, latestSettings.bellVolume)
        }
    }

    fun onStrike(event: WriterEvent, before: WriterState) {
        refreshClock()
        val now = clockMs.floatValue
        val after = viewModel.state.value

        when (event) {
            is WriterEvent.Insert -> {
                if (event.text != "\n" && event.text != " " && event.text.codePointCount(0, event.text.length) == 1) {
                    val cp = event.text.codePointAt(0)
                    val key = strikes.recordKey(
                        index = after.caret - event.text.length,
                        weight = strikeWeight(cp),
                        nowMs = now
                    )
                    val impactDelay = if (key.fast) StrikeTiming.FAST_IMPACT_MS else StrikeTiming.IMPACT_MS
                    pendingThumpAtMs = now + impactDelay
                    pendingThumpImpulse = StrikeTiming.THUMP_IMPULSE_DPS * key.weight * (if (key.fast) 0.6f else 1f)
                    slugChar = cp.takeIf { it <= 0xFFFF }?.toChar()
                } else {
                    slugChar = null
                }
            }
            is WriterEvent.Backspace -> {
                if (settings.correction == CorrectionMode.StrikeThrough) {
                    if (before.pageIndex == after.pageIndex && before.caret > after.caret &&
                        before.currentPage.text.codePointCount(after.caret, before.caret) == 1
                    ) {
                        val struck = strikes.recordStruck(index = after.caret, weight = 1f, nowMs = now)
                        val impactDelay = if (struck.fast) StrikeTiming.FAST_IMPACT_MS else StrikeTiming.IMPACT_MS
                        pendingThumpAtMs = now + impactDelay
                        pendingThumpImpulse = StrikeTiming.THUMP_IMPULSE_DPS * 1f * (if (struck.fast) 0.6f else 1f)
                        slugChar = '-'
                    } else {
                        slugChar = null
                    }
                } else {
                    if (before.pageIndex == after.pageIndex && before.caret > after.caret) {
                        val cols = columnsForKeys
                        val beforeLines = GridLayout.wrap(before.currentPage.text, cols)
                        val beforePos = GridLayout.caret(beforeLines, before.caret, cols)
                        val afterLines = GridLayout.wrap(after.currentPage.text, cols)
                        val afterPos = GridLayout.caret(afterLines, after.caret, cols)
                        if (afterPos.line == beforePos.line) {
                            val removedText = before.currentPage.text.substring(after.caret, before.caret)
                            strikes.recordErase(text = removedText, line = afterPos.line, col = afterPos.col, nowMs = now)
                        }
                    }
                    slugChar = null
                }
            }
            else -> {
                slugChar = null
            }
        }

        if (carriageMotion != CarriageMotion.Dragging) {
            carriageMotion = carriageModeFor(event, before, after, columnsForKeys)
            motionToken++
        }
        typeOnActive = false
        typeOnElapsedMs = -1f
        strikeKey++
    }

    LaunchedEffect(motionToken) {
        if (motionToken > 0) {
            delay(700)
            if (carriageMotion != CarriageMotion.Dragging) {
                carriageMotion = CarriageMotion.Rest
            }
        }
    }

    LaunchedEffect(useEnk) {
        if (useEnk) keyboardController?.hide()
    }
    LaunchedEffect(useEnk, intro == null) {
        if (useEnk && intro == null) rootFocus.requestFocus()
    }
    LaunchedEffect(useSystemKeyboard, intro == null) {
        if (useSystemKeyboard && intro == null) focusRequester.requestFocus()
    }

    // Keep TextFieldValue in sync with state
    var textFieldValue by remember(useEnk) {
        mutableStateOf(TextFieldValue(text = page.text, selection = TextRange(state.caret)))
    }

    LaunchedEffect(state) {
        // Read the live state, not this composition's snapshot, so keystrokes in flight survive.
        textFieldValue = syncField(textFieldValue, viewModel.state.value)
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            // While the start animation runs, its backdrop paints the chassis.
            .then(if (intro == null) Modifier.background(chassisBrush) else Modifier)
            .windowInsetsPadding(WindowInsets.safeDrawing)
            .focusRequester(rootFocus)
            .focusable()
            .onPreviewKeyEvent { e ->
                if (!useEnk) {
                    false
                } else {
                    val st = viewModel.state.value
                    val a = hardwareKeyToAction(e, st.caret, columnsForKeys)
                    if (a != null) {
                        val fb = KeyboardToWriter.feedbackFor(a, st)
                        fb.sound?.let { playSound(it) }
                        if (fb.pulseMs > 0) pulse(fb.pulseMs)

                        if (a == KeyAction.SystemKeyboard) {
                            systemOverride = !systemOverride
                        } else {
                            val before = viewModel.state.value
                            val out = KeyboardToWriter.outcomeFor(a, before)
                            out.event?.let { ev ->
                                viewModel.dispatch(ev)
                                onStrike(ev, before)
                            }
                            lastKeyMs = System.currentTimeMillis()
                            val live = viewModel.state.value
                            ringBellAt(live.currentPage.text, live.caret)
                        }
                        true
                    } else {
                        false
                    }
                }
            }
    ) {
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .glassSource(glassSource)
        ) {
            BoxWithConstraints(
                modifier = Modifier.fillMaxSize()
            ) {
                val density = LocalDensity.current
                val screenWidthDp = maxWidth.value
                val screenHeightDp = maxHeight.value
                val fontScale = density.fontScale.coerceIn(0.85f, 1.3f)
                val geometry = remember(screenWidthDp, screenHeightDp, fontScale) {
                    WriterGeometry.compute(screenWidthDp, screenHeightDp, fontScale)
                }
                SideEffect { columnsForKeys = geometry.cols }
                val railY = geometry.railYDp

                val lines = remember(page.text, geometry.cols) { GridLayout.wrap(page.text, geometry.cols) }
                val caretPos = remember(lines, state.caret, geometry.cols) { GridLayout.caret(lines, state.caret, geometry.cols) }
                val visibleLines = remember(lines, caretPos.line, geometry) {
                    visibleLineRange(
                        textTopOffsetDp = geometry.textTopOffsetDp(caretPos.line),
                        lineHeightDp = geometry.lineHeightDp,
                        viewportHeightDp = screenHeightDp,
                        totalLines = lines.size
                    )
                }
                val visibleCount = visibleLines.count()
                val visibleLineLengths = remember(visibleLines, lines) {
                    visibleLines.map { lines[it].end - lines[it].start }
                }
                val visibleChars = remember(visibleLineLengths) { visibleLineLengths.sum() }
                val skipTypeOn = TypeOn.skip(visibleChars, visibleCount) || page.text.isEmpty()

                // Remembered across rotation and other recreation: the text is typed on once per page.
                var typedOnPage by rememberSaveable { mutableIntStateOf(-1) }
                LaunchedEffect(state.loaded, state.pageIndex, intro == null, typeOnGate()) {
                    if (state.loaded && intro != null) {
                        // The start animation does its own type-on for this page.
                        typedOnPage = state.pageIndex
                    } else if (state.loaded && !reduceMotion && !skipTypeOn && typedOnPage != state.pageIndex && typeOnGate()) {
                        typedOnPage = state.pageIndex
                        typeOnActive = true
                        typeOnToken++
                    }
                }

                LaunchedEffect(typeOnToken) {
                    if (typeOnActive) {
                        typeOnElapsedMs = 0f
                        var startMs = -1f
                        while (typeOnActive) {
                            val now = frameMs(withFrameNanos { it })
                            if (startMs < 0f) startMs = now
                            val elapsed = now - startMs
                            typeOnElapsedMs = elapsed
                            clockMs.floatValue = now
                            val stagger = TypeOn.staggerMs(visibleCount)
                            val total = TypeOn.totalMs(visibleCount, stagger)
                            if (elapsed > total) {
                                typeOnActive = false
                                typeOnElapsedMs = -1f
                                // The carriage glides from where the sweep left it to the caret.
                                if (carriageMotion != CarriageMotion.Dragging) {
                                    carriageMotion = CarriageMotion.Return
                                    motionToken++
                                }
                                break
                            }
                        }
                    } else {
                        typeOnElapsedMs = -1f
                    }
                }

                // The start animation hands over at 450 ms of reveal time: a 200 ms budget keeps the last
                // line's sweep (it starts at most 120 + 200 ms in, and takes 112 ms) inside that window.
                val typeOnBudget = if (intro != null) 200f else TypeOn.BUDGET_MS
                val typeOnMs: (() -> Float?)? = if (reduceMotion || skipTypeOn) {
                    null
                } else if (intro != null) {
                    {
                        val t = intro.textRevealMs()
                        val stagger = TypeOn.staggerMs(visibleCount, typeOnBudget)
                        val tot = TypeOn.totalMs(visibleCount, stagger)
                        if (t > tot) null else t
                    }
                } else {
                    { if (typeOnActive && typeOnElapsedMs >= 0f) typeOnElapsedMs else null }
                }

                val carriageSweepXDp: (() -> Float?)? = if (reduceMotion || skipTypeOn) {
                    null
                } else if (intro != null) {
                    {
                        val t = intro.textRevealMs()
                        val stagger = TypeOn.staggerMs(visibleCount, typeOnBudget)
                        val col = TypeOn.carriageColumn(t, visibleLineLengths, geometry.cols, stagger)
                        if (col != null) geometry.carriageCenterXDp(col) else null
                    }
                } else {
                    {
                        if (typeOnActive && typeOnElapsedMs >= 0f) {
                            val stagger = TypeOn.staggerMs(visibleCount, typeOnBudget)
                            val col = TypeOn.carriageColumn(typeOnElapsedMs, visibleLineLengths, geometry.cols, stagger)
                            if (col != null) geometry.carriageCenterXDp(col) else null
                        } else {
                            null
                        }
                    }
                }

                val restingCarriageX = geometry.carriageCenterXDp(caretPos.col)
                val currentCarriageX = dragCarriageXDp ?: (if (state.lampOn) carriageSweepXDp?.invoke() else null) ?: restingCarriageX

                val toggleLamp: () -> Unit = {
                    viewModel.dispatch(WriterEvent.ToggleLamp)
                    playSound(SoundEvent.LampClick)
                    pulse(14)
                    hintPolicy = hintPolicy.onLongPress()
                    if (hintPolicy.isDismissed && !settings.hintDismissed) {
                        onSettingsChange { copy(hintDismissed = true) }
                    }
                }

                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .markerGestures(
                            geometry = geometry,
                            pageLength = { state.currentPage.text.length },
                            onEvent = { event ->
                                viewModel.dispatch(event)
                                if (event is WriterEvent.MoveCaret) ringBellAt(page.text, event.index)
                            },
                            onDragState = { motion, xDp ->
                                carriageMotion = motion
                                dragCarriageXDp = if (motion == CarriageMotion.Dragging) xDp else null
                                if (motion == CarriageMotion.Dragging) {
                                    hintPolicy = hintPolicy.onScrub()
                                    if (hintPolicy.isDismissed && !settings.hintDismissed) {
                                        onSettingsChange { copy(hintDismissed = true) }
                                    }
                                }
                            },
                            onLongPress = toggleLamp,
                            currentCarriageXDp = { dragCarriageXDp ?: geometry.carriageCenterXDp(caretPos.col) }
                        )
                ) {
                    // PaperSheet with PaperText and Caret
                    PaperSheet(
                        nightProgress = nightProgress,
                        reduceEffects = reduceMotion,
                        drawChassis = false,
                        paperTopDp = geometry.paperTopDp,
                        fadeHeightDp = if (WriterGeometry.isCompact(screenHeightDp)) 8f else 70f,
                        modifier = Modifier.fillMaxSize().introPaper(intro)
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .pointerInput(geometry, page.text) {
                                    detectTapGestures { offset ->
                                        if (useSystemKeyboard) {
                                            focusRequester.requestFocus()
                                            keyboardController?.show()
                                        } else {
                                            rootFocus.requestFocus()
                                        }
                                        val tapXDp = offset.x.toDp().value + WriterGeometry.PAPER_SIDE_DP
                                        val tapYDp = offset.y.toDp().value + geometry.paperTopDp
                                        val paperLines = GridLayout.wrap(page.text, geometry.cols)
                                        val gridLines = paperLines.map { span ->
                                            val t = if (span.start <= page.text.length) {
                                                page.text.substring(span.start, minOf(span.end, page.text.length))
                                            } else ""
                                            GridLine(span.start, span.end, t)
                                        }
                                        val targetIndex = TapCaretPolicy.indexAtTap(
                                            xDp = tapXDp,
                                            yDp = tapYDp,
                                            geometry = geometry,
                                            lines = gridLines,
                                            feedOffsetDp = geometry.textTopOffsetDp(caretPos.line),
                                            paperTopDp = geometry.paperTopDp
                                        )
                                        viewModel.dispatch(WriterEvent.MoveCaret(targetIndex))
                                        ringBellAt(page.text, targetIndex)
                                    }
                                }
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .graphicsLayer {
                                        translationY = if (reduceMotion) 0f else thumpDp.floatValue * density.density
                                    }
                            ) {
                                val paperTextIntroAlpha: (() -> Float)? = if (intro != null && reduceMotion) {
                                    { if (intro.textAlpha() > 0.5f) 1f else 0f }
                                } else {
                                    null
                                }

                                val effectivePage = if (typeOnGate()) page else page.copy(text = "")
                                PaperText(
                                    page = effectivePage,
                                    caret = state.caret,
                                    geometry = geometry,
                                    textColor = textColor(nightProgress, lampRgb),
                                    reduceMotion = reduceMotion,
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .introAlpha(paperTextIntroAlpha)
                                        .lampTextMask(
                                            carriageXDp = currentCarriageX,
                                            railYDp = railY,
                                            paperTopDp = geometry.paperTopDp,
                                            settings = lampSettings,
                                            active = state.lampOn
                                        ),
                                    strikes = strikes,
                                    clockMs = { clockMs.floatValue },
                                    flashColor = lerp(Color.White, lampColor, nightProgress),
                                    typeOnMs = typeOnMs,
                                    typeOnBudgetMs = typeOnBudget
                                )
                                Caret(
                                    geometry = geometry,
                                    pos = caretPos,
                                    typing = isTyping,
                                    reduceMotion = reduceMotion,
                                    modifier = Modifier.introAlpha(intro?.ownPartsAlpha)
                                )
                            }
                        }
                    }

                    // LampLight
                    LampLight(
                        carriageXDp = currentCarriageX,
                        railYDp = railY,
                        settings = lampSettings,
                        visible = state.lampOn,
                        reduceEffects = reduceMotion
                    )

                    // Rail
                    Rail(railYDp = railY, modifier = Modifier.introAlpha(intro?.ownPartsAlpha))

                    // Carriage
                    Carriage(
                        railYDp = railY,
                        centerXDp = currentCarriageX,
                        mode = carriageMotion,
                        slugChar = slugChar ?: (state.lastTypedIndex?.let { idx -> page.text.getOrNull(idx) }),
                        slugFrame = if (reduceMotion) {
                            { null }
                        } else {
                            {
                                strikes.latestKey(clockMs.floatValue)?.let {
                                    strikeFrame(clockMs.floatValue - it.startMs, it.weight, it.fast)
                                }
                            }
                        },
                        carriageSweepXDp = carriageSweepXDp,
                        night = state.lampOn,
                        reduceEffects = reduceMotion,
                        lampIntensity = settings.intensity / 100f,
                        lamp = lampRgb,
                        onToggleLamp = toggleLamp,
                        modifier = Modifier.introAlpha(intro?.ownPartsAlpha)
                    )

                    // TopIsland (replaces top bar and control panel)
                    TopIsland(
                        metaText = TopBarFormatter.formatMeta(state.pageIndex, page.text.length),
                        lampOn = state.lampOn,
                        lampColor = lampColor,
                        onToggleKeyboard = { systemOverride = !systemOverride },
                        onOpenBox = onOpenBox,
                        onToggleLamp = toggleLamp,
                        warmth = settings.warmth,
                        onWarmthChange = { w -> onSettingsChange { copy(warmth = w) } },
                        intensity = settings.intensity,
                        onIntensityChange = { i -> onSettingsChange { copy(intensity = i) } },
                        beam = settings.beam,
                        onBeamChange = { b -> onSettingsChange { copy(beam = b) } },
                        onDetent = { pos ->
                            if (!latestSettings.muted) player.playTick(pos, 1f)
                            pulse(4)
                        },
                        lastKeyMs = lastKeyMs,
                        modifier = Modifier.introAlpha(intro?.topBarAlpha),
                        glassSource = glassSource
                    )

                    // Hint text (DS 6.9, Finding 5): hidden when paper is short (landscape keyboard, etc.)
                    val isShortPaper = WriterGeometry.isCompact(screenHeightDp) || (screenHeightDp - geometry.railYDp) < 100f
                    if (showAsk) {
                        DarkPaperAsk(
                            onLamp = { onSettingsChange { copy(darkPaper = DarkPaper.Lamp) } },
                            onNormal = { onSettingsChange { copy(darkPaper = DarkPaper.Normal) } },
                            modifier = Modifier
                                .align(Alignment.BottomCenter)
                                .padding(horizontal = 20.dp, vertical = 16.dp)
                        )
                    }

                    if (!showAsk && !isShortPaper && !hintPolicy.isDismissed && !settings.hintDismissed && !state.lampOn) {
                        Text(
                            text = stringResource(R.string.writer_hint),
                            style = EnkType.hint.copy(fontFamily = LocalMonoFont.current),
                            modifier = Modifier
                                .align(Alignment.BottomCenter)
                                .introAlpha(intro?.textAlpha)
                                .padding(bottom = 22.dp)
                        )
                    }

                    // IME input: transparent BasicTextField (Finding 10)
                    if (useSystemKeyboard) {
                        BasicTextField(
                            value = textFieldValue,
                            onValueChange = { newValue ->
                                val oldText = textFieldValue.text
                                val events = imeBridge.onTextChanged(
                                    old = oldText,
                                    new = newValue,
                                    composingNow = newValue.composition != null
                                )
                                for (event in events) {
                                    when (event) {
                                        is WriterEvent.Insert -> {
                                            // The bridge reports every typed character as its own insert, so the sound
                                            // follows the text, never a commit.
                                            val sound = InsertFeedback.soundFor(
                                                text = event.text,
                                                composing = false,
                                                previousComposingLength = 0,
                                                wasComposing = false
                                            )

                                            val before = viewModel.state.value
                                            viewModel.dispatch(event)
                                            lastKeyMs = System.currentTimeMillis()
                                            onStrike(event, before)
                                            if (sound != null) {
                                                playSound(sound)
                                                pulse(InsertFeedback.pulseMs(sound))
                                            }

                                            val live = viewModel.state.value
                                            ringBellAt(live.currentPage.text, live.caret)
                                        }
                                        is WriterEvent.Backspace -> {
                                            val before = viewModel.state.value
                                            val canBackspace = !(before.pageIndex == 0 && before.caret == 0)
                                            viewModel.dispatch(event)
                                            lastKeyMs = System.currentTimeMillis()
                                            if (canBackspace) {
                                                playSound(SoundEvent.Backspace)
                                                pulse(8)
                                            }
                                            onStrike(event, before)
                                        }
                                        is WriterEvent.MoveCaret -> {
                                            viewModel.dispatch(event)
                                            ringBellAt(viewModel.state.value.currentPage.text, event.index)
                                        }
                                        else -> viewModel.dispatch(event)
                                    }
                                }
                                // Reconcile right away against the live state so a later keystroke never sees
                                // a stale field.
                                textFieldValue = syncField(newValue, viewModel.state.value)
                            },
                            modifier = Modifier
                                .size(1.dp)
                                .testTag("writer_input")
                                .focusRequester(focusRequester),
                            textStyle = TextStyle(color = Color.Transparent),
                            cursorBrush = SolidColor(Color.Transparent),
                            keyboardOptions = KeyboardOptions(
                                autoCorrectEnabled = settings.autocorrect,
                                capitalization = KeyboardCapitalization.None
                            )
                        )
                    }
                }
            }
        }

        if (useEnk) {
            EnkKeyboard(
                layout = keyboardLayout,
                metrics = keyboardMetrics,
                onFeedback = { action ->
                    val fb = KeyboardToWriter.feedbackFor(action, viewModel.state.value)
                    fb.sound?.let { playSound(it) }
                    if (fb.pulseMs > 0) pulse(fb.pulseMs)
                },
                onApply = { action ->
                    if (action == KeyAction.SystemKeyboard) {
                        systemOverride = !systemOverride
                        return@EnkKeyboard
                    }
                    val before = viewModel.state.value
                    val out = KeyboardToWriter.outcomeFor(action, before)
                    out.event?.let { ev ->
                        viewModel.dispatch(ev)
                        onStrike(ev, before)
                    }
                    lastKeyMs = System.currentTimeMillis()
                    val live = viewModel.state.value
                    ringBellAt(live.currentPage.text, live.caret)
                },
                onAlternative = { base, alt ->
                    val st = viewModel.state.value
                    val before = st.caret - base.length
                    viewModel.dispatch(KeyboardToWriter.alternativeEvent(base, alt, before, st))
                    val live = viewModel.state.value
                    ringBellAt(live.currentPage.text, live.caret)
                },
                caretProvider = { viewModel.state.value.caret },
                glassSource = glassSource
            )
        }
    }
}

private fun String.lastCodePointOrNull(): Int? =
    if (isEmpty()) null else codePointBefore(length)
