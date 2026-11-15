package app.enk.writer

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationSpec
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.vector.PathParser
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import app.enk.design.EnkColors
import app.enk.design.LocalMonoFont
import app.enk.design.glass.GlassSpecs
import app.enk.design.shell.GlassSource
import app.enk.design.shell.LocalReduceEffects
import app.enk.design.glass.glassSurface
import app.enk.design.shell.rememberGlassSource
import app.enk.design.logo.LogoSpec
import app.enk.design.motion.ShellMotion
import app.enk.design.theme.LocalShellColors
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

enum class IslandState {
    Rest, Actions, Lamp
}

@Composable
fun TopIsland(
    metaText: String,
    lampOn: Boolean,
    lampColor: Color,
    onToggleKeyboard: () -> Unit,
    onOpenBox: () -> Unit,
    onToggleLamp: () -> Unit,
    warmth: Int,
    onWarmthChange: (Int) -> Unit,
    intensity: Int,
    onIntensityChange: (Int) -> Unit,
    beam: Int,
    onBeamChange: (Int) -> Unit,
    onDetent: (Int) -> Unit,
    lastKeyMs: Long,
    modifier: Modifier = Modifier,
    initialState: IslandState = IslandState.Rest,
    safeTop: Dp = 0.dp,
    glassSource: GlassSource = rememberGlassSource(),
    accent: Color = EnkColors.accent
) {
    val colors = LocalShellColors.current
    val monoFont = LocalMonoFont.current
    val density = LocalDensity.current
    val reduceEffects = LocalReduceEffects.current
    val scope = rememberCoroutineScope()

    var state by remember { mutableStateOf(initialState) }
    var lastInteractionMs by remember { mutableLongStateOf(System.currentTimeMillis()) }
    var isSliderTouched by remember { mutableStateOf(false) }

    val initialW = when (initialState) {
        IslandState.Rest -> 150f
        IslandState.Actions, IslandState.Lamp -> 320f
    }
    val initialH = when (initialState) {
        IslandState.Rest -> 40f
        IslandState.Actions -> 110f
        IslandState.Lamp -> 270f
    }
    val widthAnim = remember { Animatable(initialW) }
    val heightAnim = remember { Animatable(initialH) }

    fun spec(): AnimationSpec<Float> =
        if (reduceEffects) tween(100) else ShellMotion.sheet.toSpec()

    // Animate size when state changes
    LaunchedEffect(state) {
        val (targetW, targetH) = when (state) {
            IslandState.Rest -> Pair(150f, 40f)
            IslandState.Actions -> Pair(320f, 110f)
            IslandState.Lamp -> Pair(320f, 270f)
        }
        scope.launch { widthAnim.animateTo(targetW, spec()) }
        scope.launch { heightAnim.animateTo(targetH, spec()) }
    }

    // Fold on typing
    LaunchedEffect(lastKeyMs) {
        if (lastKeyMs > 0L && state != IslandState.Rest) {
            state = IslandState.Rest
        }
    }

    // Auto-fold after 4500 ms idle
    LaunchedEffect(state, lastInteractionMs, isSliderTouched) {
        if (state != IslandState.Rest && !isSliderTouched) {
            delay(4500)
            state = IslandState.Rest
        }
    }

    // Back handler folds to Rest
    BackHandler(enabled = state != IslandState.Rest) {
        state = IslandState.Rest
    }

    val currentW = widthAnim.value.dp
    val currentH = heightAnim.value.dp
    val currentRadius = if (state == IslandState.Rest) {
        (minOf(widthAnim.value, heightAnim.value) / 2f).dp
    } else {
        28.dp
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .zIndex(10f),
        contentAlignment = Alignment.TopCenter
    ) {
        // Tap outside to close
        if (state != IslandState.Rest) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .pointerInput(Unit) {
                        detectTapGestures {
                            state = IslandState.Rest
                        }
                    }
            )
        }

        Box(
            modifier = Modifier
                .padding(top = safeTop + 10.dp)
                .size(currentW, currentH)
                .testTag("top_island")
                .semantics {
                    role = Role.Button
                    contentDescription = "Writer menu"
                    stateDescription = if (state == IslandState.Rest) "Closed" else "Open"
                }
                .glassSurface(
                    spec = GlassSpecs.Floating,
                    shape = RoundedCornerShape(currentRadius),
                    source = glassSource
                )
                .pointerInput(state) {
                    detectTapGestures {
                        lastInteractionMs = System.currentTimeMillis()
                        if (state == IslandState.Rest) {
                            state = IslandState.Actions
                        }
                    }
                }
        ) {
            when (state) {
                IslandState.Rest -> {
                    // Rest Content: page meta, centred, Courier 11 sp
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        // Lit lamp dot at left inner edge (12 dp inset)
                        if (lampOn) {
                            Box(
                                modifier = Modifier
                                    .align(Alignment.CenterStart)
                                    .padding(start = 12.dp)
                                    .size(6.dp)
                                    .background(lampColor, CircleShape)
                            )
                        }

                        Text(
                            text = metaText,
                            color = colors.t1,
                            fontFamily = monoFont,
                            fontSize = 11.sp,
                            maxLines = 1,
                            textAlign = TextAlign.Center
                        )
                    }
                }

                IslandState.Actions -> {
                    // Actions: caption row at top, 3 columns
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(top = 10.dp, bottom = 12.dp, start = 12.dp, end = 12.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.SpaceBetween
                    ) {
                        // Top caption row: meta text
                        Text(
                            text = metaText,
                            color = colors.t1,
                            fontFamily = monoFont,
                            fontSize = 11.sp,
                            maxLines = 1,
                            textAlign = TextAlign.Center
                        )

                        // 3 equal columns: Keyboard, Lamp, Box
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceEvenly,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Column 1: Keyboard
                            ActionItem(
                                label = "Keyboard",
                                testTag = "keyboard_switch",
                                onClick = {
                                    lastInteractionMs = System.currentTimeMillis()
                                    onToggleKeyboard()
                                    state = IslandState.Rest
                                },
                                contentDescription = "Switch keyboard"
                            ) {
                                KeyboardSwitchGlyph(modifier = Modifier.size(24.dp))
                            }

                            // Column 2: Lamp
                            ActionItem(
                                label = "Lamp",
                                testTag = "lamp_button",
                                onClick = {
                                    lastInteractionMs = System.currentTimeMillis()
                                    state = IslandState.Lamp
                                },
                                contentDescription = "Lamp",
                                stateDescription = if (lampOn) "On" else "Off"
                            ) {
                                LampGlyph(
                                    lampOn = lampOn,
                                    lampColor = lampColor,
                                    modifier = Modifier.size(24.dp)
                                )
                            }

                            // Column 3: Box
                            ActionItem(
                                label = "Box",
                                testTag = "box_button",
                                onClick = {
                                    lastInteractionMs = System.currentTimeMillis()
                                    onOpenBox()
                                    state = IslandState.Rest
                                },
                                contentDescription = "Open the Box"
                            ) {
                                BoxGlyph(modifier = Modifier.size(24.dp))
                            }
                        }
                    }
                }

                IslandState.Lamp -> {
                    LampPopupContent(
                        lampOn = lampOn,
                        onToggleLamp = {
                            lastInteractionMs = System.currentTimeMillis()
                            onToggleLamp()
                        },
                        warmth = warmth,
                        onWarmthChange = {
                            lastInteractionMs = System.currentTimeMillis()
                            onWarmthChange(it)
                        },
                        intensity = intensity,
                        onIntensityChange = {
                            lastInteractionMs = System.currentTimeMillis()
                            onIntensityChange(it)
                        },
                        beam = beam,
                        onBeamChange = {
                            lastInteractionMs = System.currentTimeMillis()
                            onBeamChange(it)
                        },
                        onDetent = onDetent,
                        accent = accent,
                        glassSource = glassSource,
                        onSliderTouchChange = { touched ->
                            isSliderTouched = touched
                            if (touched) lastInteractionMs = System.currentTimeMillis()
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun ActionItem(
    label: String,
    testTag: String,
    contentDescription: String,
    stateDescription: String? = null,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    glyph: @Composable () -> Unit
) {
    val colors = LocalShellColors.current
    Column(
        modifier = modifier
            .size(width = 68.dp, height = 58.dp)
            .testTag(testTag)
            .semantics {
                role = Role.Button
                this.contentDescription = contentDescription
                if (stateDescription != null) {
                    this.stateDescription = stateDescription
                }
            }
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick
            ),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier.size(28.dp),
            contentAlignment = Alignment.Center
        ) {
            glyph()
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = label,
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium,
            color = colors.t1
        )
    }
}

@Composable
private fun LampGlyph(
    lampOn: Boolean,
    lampColor: Color,
    modifier: Modifier = Modifier
) {
    val density = LocalDensity.current
    val parsedPath = remember {
        PathParser().parsePathString("M9 18h6M10 21h4M8.5 14.5A6 6 0 1 1 15.5 14.5c-.6.5-1 1.2-1 2h-5c0-.8-.4-1.5-1-2z").toPath()
    }
    val ink = if (lampOn) lampColor else LogoSpec.CREAM

    Canvas(modifier = modifier) {
        val stroke = Stroke(
            width = 1.7.dp.toPx(),
            cap = StrokeCap.Round,
            join = StrokeJoin.Round
        )
        if (lampOn) {
            // Soft glow behind it
            drawCircle(
                color = lampColor.copy(alpha = 0.25f),
                radius = 11.dp.toPx(),
                center = center
            )
        }
        scale(scale = size.minDimension / 24f, pivot = Offset.Zero) {
            drawPath(parsedPath, color = ink, style = stroke)
        }
    }
}

@Composable
private fun KeyboardSwitchGlyph(modifier: Modifier = Modifier) {
    val ink = LogoSpec.CREAM
    Canvas(modifier = modifier) {
        val w = 22.dp.toPx()
        val h = 14.dp.toPx()
        val strokeWidth = 1.7.dp.toPx()
        val left = (size.width - w) / 2f
        val top = (size.height - h) / 2f
        val cornerRadius = CornerRadius(2.5.dp.toPx(), 2.5.dp.toPx())
        drawRoundRect(
            color = ink,
            topLeft = Offset(left, top),
            size = Size(w, h),
            cornerRadius = cornerRadius,
            style = Stroke(width = strokeWidth)
        )
        val dotRadius = 1.dp.toPx()
        val dotY = top + h * 0.38f
        val dotSpacing = w * 0.22f
        val centerX = left + w / 2f
        drawCircle(color = ink, radius = dotRadius, center = Offset(centerX - dotSpacing, dotY))
        drawCircle(color = ink, radius = dotRadius, center = Offset(centerX, dotY))
        drawCircle(color = ink, radius = dotRadius, center = Offset(centerX + dotSpacing, dotY))
        val barY = top + h * 0.72f
        val barHalfWidth = w * 0.25f
        drawLine(
            color = ink,
            start = Offset(centerX - barHalfWidth, barY),
            end = Offset(centerX + barHalfWidth, barY),
            strokeWidth = 1.4.dp.toPx(),
            cap = StrokeCap.Round
        )
    }
}

@Composable
private fun BoxGlyph(modifier: Modifier = Modifier) {
    val ink = LogoSpec.CREAM
    Canvas(modifier = modifier) {
        val stroke = Stroke(width = 1.7.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round)
        val w = size.width
        val h = size.height
        drawLine(ink.copy(alpha = 0.55f), Offset(w * 0.28f, h * 0.12f), Offset(w * 0.72f, h * 0.12f), stroke.width, StrokeCap.Round)
        drawLine(ink.copy(alpha = 0.8f), Offset(w * 0.18f, h * 0.30f), Offset(w * 0.82f, h * 0.30f), stroke.width, StrokeCap.Round)
        val tray = Path().apply {
            moveTo(w * 0.08f, h * 0.46f)
            lineTo(w * 0.92f, h * 0.46f)
            lineTo(w * 0.82f, h * 0.90f)
            lineTo(w * 0.18f, h * 0.90f)
            close()
        }
        drawPath(tray, ink, style = stroke)
    }
}
