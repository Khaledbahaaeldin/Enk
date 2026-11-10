package app.enk.writer

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import app.enk.design.ControlPanel
import app.enk.design.EnkColors
import app.enk.design.EnkType
import app.enk.design.LampColor
import app.enk.design.LampLight
import app.enk.design.LocalMonoFont
import app.enk.design.lampTextMask
import app.enk.design.logo.EnkWordmark
import app.enk.settings.Settings
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
    intro: WriterIntro? = null
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

    val reduceMotion = rememberReduceMotion(settings.reduceEffects)

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

    // Focusing the field raises the keyboard, which resizes the writer; during the start animation
    // that would move the rail under the overlay, so the field waits until the animation is over.
    LaunchedEffect(intro == null) {
        if (intro == null) focusRequester.requestFocus()
    }

    // Keep TextFieldValue in sync with state
    var textFieldValue by remember {
        mutableStateOf(TextFieldValue(text = page.text, selection = TextRange(state.caret)))
    }

    LaunchedEffect(state) {
        // Read the live state, not this composition's snapshot, so keystrokes in flight survive.
        textFieldValue = syncField(textFieldValue, viewModel.state.value)
    }

    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            // While the start animation runs, its backdrop paints the chassis.
            .then(if (intro == null) Modifier.background(chassisBrush) else Modifier)
            .windowInsetsPadding(WindowInsets.safeDrawing)
    ) {
        val density = LocalDensity.current
        val screenWidthDp = maxWidth.value
        val screenHeightDp = maxHeight.value
        val fontScale = density.fontScale.coerceIn(0.85f, 1.3f)
        val geometry = remember(screenWidthDp, screenHeightDp, fontScale) {
            WriterGeometry.compute(screenWidthDp, screenHeightDp, fontScale)
        }
        val railY = geometry.railYDp

        val lines = remember(page.text, geometry.cols) { GridLayout.wrap(page.text, geometry.cols) }
        val caretPos = remember(lines, state.caret, geometry.cols) { GridLayout.caret(lines, state.caret, geometry.cols) }
        val restingCarriageX = geometry.carriageCenterXDp(caretPos.col)
        val currentCarriageX = dragCarriageXDp ?: restingCarriageX
        val bellPolicy = remember(geometry.cols) { BellPolicy(geometry.cols) }
        fun ringBellAt(text: String, index: Int) {
            val pos = GridLayout.caret(GridLayout.wrap(text, geometry.cols), index, geometry.cols)
            if (bellPolicy.onCaret(pos.line, pos.col)) {
                playSound(SoundEvent.Bell, latestSettings.bellVolume)
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
                                onSettingsChange { s: Settings -> s.copy(hintDismissed = true) }
                            }
                        }
                    },
                    onLongPress = {
                        viewModel.dispatch(WriterEvent.ToggleLamp)
                        playSound(SoundEvent.LampClick)
                        pulse(14)
                        hintPolicy = hintPolicy.onLongPress()
                        if (hintPolicy.isDismissed && !settings.hintDismissed) {
                            onSettingsChange { s: Settings -> s.copy(hintDismissed = true) }
                        }
                    },
                    currentCarriageXDp = { dragCarriageXDp ?: geometry.carriageCenterXDp(caretPos.col) }
                )
        ) {
            // PaperSheet with PaperText and Caret
            PaperSheet(
                nightProgress = nightProgress,
                reduceEffects = reduceMotion,
                drawChassis = false,
                modifier = Modifier.fillMaxSize().introPaper(intro)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .pointerInput(geometry, page.text) {
                            detectTapGestures { offset ->
                                focusRequester.requestFocus()
                                keyboardController?.show()
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
                    PaperText(
                        page = page,
                        caret = state.caret,
                        lastTypedIndex = state.lastTypedIndex,
                        geometry = geometry,
                        textColor = textColor(nightProgress, lampRgb),
                        reduceMotion = reduceMotion,
                        modifier = Modifier
                            .fillMaxSize()
                            .introAlpha(intro?.textAlpha)
                            .lampTextMask(
                                carriageXDp = currentCarriageX,
                                railYDp = railY,
                                paperTopDp = geometry.paperTopDp,
                                settings = lampSettings,
                                active = state.lampOn
                            )
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
                strikeKey = strikeKey,
                night = state.lampOn,
                reduceEffects = reduceMotion,
                lampIntensity = settings.intensity / 100f,
                lamp = lampRgb,
                modifier = Modifier.introAlpha(intro?.ownPartsAlpha)
            )

            // Top bar (DS 6.1, Finding 11)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(60.dp)
                    .introAlpha(intro?.topBarAlpha)
                    .padding(horizontal = 26.dp)
            ) {
                // Left: dot + gap + wordmark
                Row(
                    modifier = Modifier.align(Alignment.CenterStart),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .background(EnkColors.accent, CircleShape)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Box(
                        modifier = Modifier
                            .defaultMinSize(minWidth = 48.dp, minHeight = 48.dp)
                            .testTag("wordmark")
                            .semantics {
                                role = Role.Button
                                contentDescription = "Open the Box"
                            }
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null,
                                onClick = onOpenBox
                            ),
                        contentAlignment = Alignment.CenterStart
                    ) {
                        EnkWordmark(
                            modifier = Modifier.height(18.dp),
                            onDark = true
                        )
                    }
                }

                // Right: Meta line (Finding 11)
                Text(
                    text = TopBarFormatter.formatMeta(state.pageIndex, page.text.length),
                    style = EnkType.meta.copy(fontFamily = LocalMonoFont.current),
                    modifier = Modifier.align(Alignment.CenterEnd)
                )
            }

            // ControlPanel
            ControlPanel(
                visible = state.lampOn,
                lampColor = lampColor,
                settings = lampSettings,
                onChange = { lamp ->
                    onSettingsChange { s: Settings ->
                        s.copy(warmth = lamp.warmth, intensity = lamp.intensity, beam = lamp.beam)
                    }
                },
                onDetent = { pos ->
                    if (!latestSettings.muted) player.playTick(pos, 1f)
                    pulse(4)
                },
                reduceEffects = reduceMotion,
                modifier = Modifier.testTag("control_panel")
            )

            // Hint text (DS 6.9, Finding 5): hidden when paper is short (landscape keyboard, etc.)
            val isShortPaper = screenHeightDp < 360f || (screenHeightDp - geometry.railYDp) < 100f
            if (!isShortPaper && !hintPolicy.isDismissed && !settings.hintDismissed && !state.lampOn) {
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
            BasicTextField(
                value = textFieldValue,
                onValueChange = { newValue ->
                    val oldText = textFieldValue.text
                    val wasComposing = viewModel.state.value.composingRange != null
                    val events = imeBridge.onTextChanged(
                        old = oldText,
                        new = newValue,
                        composingNow = newValue.composition != null
                    )
                    for (event in events) {
                        when (event) {
                            is WriterEvent.Insert -> {
                                val codePoints = event.text.codePointCount(0, event.text.length)
                                val isSingleCodePoint = !event.composing && codePoints == 1
                                val isPaste = !event.composing && !wasComposing && codePoints > 1
                                val isCommittedWord = !event.composing && wasComposing

                                viewModel.dispatch(event)
                                lastKeyMs = System.currentTimeMillis()
                                if (!event.composing) {
                                    // One stamp per committed insert, never per composing update.
                                    slugChar = event.text.lastCodePointOrNull()?.takeIf { it <= 0xFFFF }?.toChar()
                                    strikeKey++
                                }

                                if (event.composing || isPaste) {
                                    // Silent while composing or pasting.
                                } else if (isCommittedWord) {
                                    playSound(SoundEvent.Key)
                                    pulse(8)
                                } else if (isSingleCodePoint) {
                                    val (sound, pulseMs) = when (event.text) {
                                        " " -> Pair(SoundEvent.Space, 14)
                                        "\n" -> Pair(SoundEvent.Enter, 14)
                                        else -> Pair(SoundEvent.Key, 8)
                                    }
                                    playSound(sound)
                                    pulse(pulseMs)
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

private fun String.lastCodePointOrNull(): Int? =
    if (isEmpty()) null else codePointBefore(length)
