package app.enk.writer

import android.provider.Settings as AndroidSettings
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
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
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
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
import app.enk.audio.BellPolicy
import app.enk.audio.Sample
import app.enk.audio.SoundEvent
import app.enk.audio.SoundManifest
import app.enk.audio.SoundPlayer
import app.enk.audio.SoundPoolPlayer
import app.enk.audio.SoundSet
import app.enk.design.ControlPanel
import app.enk.design.EnkColors
import app.enk.design.EnkType
import app.enk.design.LampColor
import app.enk.design.LampLight
import app.enk.design.LampSettings
import app.enk.design.LocalMonoFont
import app.enk.design.lampTextMask
import app.enk.design.logo.EnkWordmark
import app.enk.settings.Settings
import app.enk.text.GridLayout
import app.enk.text.LineSpan
import app.enk.text.StruckPage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
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

fun Modifier.lampTextMask(
    visible: Boolean,
    railYDp: Float,
    paperTopDp: Float = 64f,
    lampColor: Color? = null,
    textMuted: Color? = null,
    carriageXDp: Float = 0f,
    settings: LampSettings = LampSettings(25, 80, 55)
): Modifier = this.lampTextMask(
    carriageXDp = carriageXDp,
    railYDp = railYDp,
    paperTopDp = paperTopDp,
    settings = settings,
    active = visible
)

@Composable
fun LampLight(
    visible: Boolean,
    railYDp: Float,
    paperTopDp: Float,
    paperHeightDp: Float,
    lampColor: Color,
    reduceEffects: Boolean,
    carriageXDp: Float = 0f,
    modifier: Modifier = Modifier
) {
    LampLight(
        carriageXDp = carriageXDp,
        railYDp = railYDp,
        settings = LampSettings(25, 80, 55),
        visible = visible,
        reduceEffects = reduceEffects,
        modifier = modifier
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
    onSettingsChange: ((Settings) -> Unit)? = null
) {
    val state by viewModel.state.collectAsState()
    val page = state.currentPage
    val coroutineScope = rememberCoroutineScope()
    val context = LocalContext.current
    val keyboardController = LocalSoftwareKeyboardController.current

    // Lifecycle ON_STOP flush
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_STOP) {
                coroutineScope.launch { viewModel.flush() }
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    // MotionPolicy evaluation (Finding 8)
    val animatorDurationScale = remember(context) {
        try {
            AndroidSettings.Global.getFloat(
                context.contentResolver,
                AndroidSettings.Global.ANIMATOR_DURATION_SCALE,
                1.0f
            )
        } catch (_: Exception) {
            1.0f
        }
    }
    val reduceMotion = remember(settings.reduceEffects, animatorDurationScale) {
        MotionPolicy.shouldReduceMotion(settings.reduceEffects, animatorDurationScale)
    }

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
    val soundSet = remember(settings.soundSet, context) {
        try {
            val json = context.assets.open("sounds/manifest.json").bufferedReader().use { it.readText() }
            val sets = SoundManifest.parse(json)
            sets.firstOrNull { it.id.equals(settings.soundSet.name, ignoreCase = true) } ?: defaultSoundSet
        } catch (_: Throwable) {
            defaultSoundSet
        }
    }

    // Prepare sound set off main thread on changes (Finding 10)
    LaunchedEffect(settings.soundSet, soundSet) {
        withContext(Dispatchers.IO) {
            try {
                (player as? SoundPoolPlayer)?.prepare(soundSet)
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

    val isTyping = TypingPolicy.isTyping(
        nowMs = System.currentTimeMillis(),
        lastKeyMs = lastKeyMs,
        dragging = carriageMotion == CarriageMotion.Dragging
    )

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

    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
    }

    // Keep TextFieldValue in sync with state
    var textFieldValue by remember {
        mutableStateOf(TextFieldValue(text = page.text, selection = TextRange(state.caret)))
    }

    LaunchedEffect(page.text, state.caret) {
        if (textFieldValue.text != page.text || textFieldValue.selection.start != state.caret) {
            textFieldValue = TextFieldValue(
                text = page.text,
                selection = TextRange(state.caret.coerceIn(0, page.text.length))
            )
        }
    }

    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .background(chassisBrush)
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

        Box(
            modifier = Modifier
                .fillMaxSize()
                .markerGestures(
                    geometry = geometry,
                    pageLength = { state.currentPage.text.length },
                    onEvent = { event ->
                        viewModel.dispatch(event)
                        if (event is WriterEvent.MoveCaret) {
                            val newCaretPos = GridLayout.caret(lines, event.index, geometry.cols)
                            if (bellPolicy.onCaret(newCaretPos.line, newCaretPos.col)) {
                                try {
                                    player.play(SoundEvent.Bell, soundSet, settings.bellVolume)
                                } catch (_: Exception) {}
                            }
                        }
                    },
                    onDragState = { motion, xDp ->
                        carriageMotion = motion
                        dragCarriageXDp = if (motion == CarriageMotion.Dragging) xDp else null
                        if (motion == CarriageMotion.Dragging) {
                            hintPolicy = hintPolicy.onScrub()
                            if (hintPolicy.isDismissed && !settings.hintDismissed) {
                                onSettingsChange?.invoke(settings.copy(hintDismissed = true))
                            }
                        }
                    },
                    onLongPress = {
                        viewModel.dispatch(WriterEvent.ToggleLamp)
                        try {
                            player.play(SoundEvent.LampClick, soundSet)
                        } catch (_: Exception) {}
                        try {
                            haptics.pulse(14)
                        } catch (_: Exception) {}
                        hintPolicy = hintPolicy.onLongPress()
                        if (hintPolicy.isDismissed && !settings.hintDismissed) {
                            onSettingsChange?.invoke(settings.copy(hintDismissed = true))
                        }
                    },
                    currentCarriageXDp = { dragCarriageXDp ?: geometry.carriageCenterXDp(caretPos.col) }
                )
        ) {
            // PaperSheet with PaperText and Caret
            PaperSheet(
                nightProgress = nightProgress,
                reduceEffects = reduceMotion,
                modifier = Modifier.fillMaxSize()
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .pointerInput(geometry, page.text) {
                            detectTapGestures { offset ->
                                focusRequester.requestFocus()
                                keyboardController?.show()
                                val tapXDp = offset.x.toDp().value + 12f
                                val tapYDp = offset.y.toDp().value + 64f
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
                                    paperTopDp = 64f
                                )
                                viewModel.dispatch(WriterEvent.MoveCaret(targetIndex))
                                val newCaretPos = GridLayout.caret(paperLines, targetIndex, geometry.cols)
                                if (bellPolicy.onCaret(newCaretPos.line, newCaretPos.col)) {
                                    try {
                                        player.play(SoundEvent.Bell, soundSet, settings.bellVolume)
                                    } catch (_: Exception) {}
                                }
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
                            .lampTextMask(
                                carriageXDp = currentCarriageX,
                                railYDp = railY,
                                paperTopDp = 64f,
                                settings = lampSettings,
                                active = state.lampOn
                            )
                    )
                    Caret(
                        geometry = geometry,
                        pos = caretPos,
                        typing = isTyping,
                        reduceMotion = reduceMotion
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
            Rail(railYDp = railY)

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
                lamp = lampRgb
            )

            // Top bar (DS 6.1, Finding 11)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(60.dp)
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
                            .semantics { role = Role.Button }
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null,
                                onClick = onOpenBox
                            ),
                        contentAlignment = Alignment.CenterStart
                    ) {
                        EnkWordmark(
                            modifier = Modifier.height(18.dp),
                            onDark = state.lampOn
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
                onChange = { newSettings ->
                    onSettingsChange?.invoke(
                        settings.copy(
                            warmth = newSettings.warmth,
                            intensity = newSettings.intensity,
                            beam = newSettings.beam
                        )
                    )
                },
                onDetent = { pos ->
                    try {
                        (player as? SoundPoolPlayer)?.playTick(pos, 1f)
                            ?: player.play(SoundEvent.FaderTick, soundSet, 1f)
                    } catch (_: Exception) {}
                    try {
                        haptics.pulse(4)
                    } catch (_: Exception) {}
                },
                reduceEffects = reduceMotion,
                modifier = Modifier.testTag("control_panel")
            )

            // Hint text (DS 6.9, Finding 5)
            if (!hintPolicy.isDismissed && !settings.hintDismissed && !state.lampOn) {
                Text(
                    text = "drag marker to scrub · hold marker for lamp",
                    style = EnkType.hint.copy(fontFamily = LocalMonoFont.current),
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = 22.dp)
                )
            }

            // IME input: transparent BasicTextField (Finding 10)
            BasicTextField(
                value = textFieldValue,
                onValueChange = { newValue ->
                    val oldText = textFieldValue.text
                    val wasComposing = state.composingRange != null
                    textFieldValue = newValue
                    val isComposing = newValue.composition != null
                    val events = imeBridge.onTextChanged(
                        old = oldText,
                        new = newValue,
                        composingNow = isComposing
                    )
                    for (event in events) {
                        when (event) {
                            is WriterEvent.Insert -> {
                                val isComposingEvent = event.composing
                                val isSingleCodePoint = !isComposingEvent && event.text.isNotEmpty() && event.text.codePointCount(0, event.text.length) == 1
                                val isPaste = !isComposingEvent && !wasComposing && event.text.isNotEmpty() && event.text.codePointCount(0, event.text.length) > 1
                                val isCommittedWord = !isComposingEvent && wasComposing

                                viewModel.dispatch(event)
                                lastKeyMs = System.currentTimeMillis()
                                slugChar = event.text.lastOrNull()
                                strikeKey++

                                if (isComposingEvent || isPaste) {
                                    // Silent, no haptic
                                } else if (isCommittedWord) {
                                    if (!settings.muted) {
                                        try { player.play(SoundEvent.Key, soundSet) } catch (_: Exception) {}
                                    }
                                    if (settings.haptics) {
                                        try { haptics.pulse(8) } catch (_: Exception) {}
                                    }
                                } else if (isSingleCodePoint) {
                                    val (sound, pulseMs) = when (event.text) {
                                        " " -> Pair(SoundEvent.Space, 14)
                                        "\n" -> Pair(SoundEvent.Enter, 14)
                                        else -> Pair(SoundEvent.Key, 8)
                                    }
                                    if (!settings.muted) {
                                        try { player.play(sound, soundSet) } catch (_: Exception) {}
                                    }
                                    if (settings.haptics) {
                                        try { haptics.pulse(pulseMs) } catch (_: Exception) {}
                                    }
                                }

                                val curState = viewModel.state.value
                                val curPage = curState.currentPage
                                val curLines = GridLayout.wrap(curPage.text, geometry.cols)
                                val curCaretPos = GridLayout.caret(curLines, curState.caret, geometry.cols)
                                if (bellPolicy.onCaret(curCaretPos.line, curCaretPos.col)) {
                                    try {
                                        player.play(SoundEvent.Bell, soundSet, settings.bellVolume)
                                    } catch (_: Exception) {}
                                }
                            }
                            is WriterEvent.Backspace -> {
                                val canBackspace = !(state.pageIndex == 0 && state.caret == 0)
                                viewModel.dispatch(event)
                                lastKeyMs = System.currentTimeMillis()
                                if (canBackspace) {
                                    if (!settings.muted) {
                                        try { player.play(SoundEvent.Backspace, soundSet) } catch (_: Exception) {}
                                    }
                                    if (settings.haptics) {
                                        try { haptics.pulse(8) } catch (_: Exception) {}
                                    }
                                }
                            }
                            is WriterEvent.MoveCaret -> {
                                viewModel.dispatch(event)
                                val curLines = GridLayout.wrap(state.currentPage.text, geometry.cols)
                                val curCaretPos = GridLayout.caret(curLines, event.index, geometry.cols)
                                if (bellPolicy.onCaret(curCaretPos.line, curCaretPos.col)) {
                                    try {
                                        player.play(SoundEvent.Bell, soundSet, settings.bellVolume)
                                    } catch (_: Exception) {}
                                }
                            }
                            else -> {
                                viewModel.dispatch(event)
                            }
                        }
                    }
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
