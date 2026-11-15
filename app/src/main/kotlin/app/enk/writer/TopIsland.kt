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
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredSize
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
import androidx.compose.ui.graphics.graphicsLayer
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
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import app.enk.design.EnkColors
import app.enk.design.LocalMonoFont
import app.enk.design.glass.GlassSpecs
import app.enk.design.glass.LiquidGroup
import app.enk.design.glass.LiquidShape
import app.enk.design.glass.LiquidSpecs
import app.enk.design.glass.glassSurface
import app.enk.design.glass.liquidPress
import app.enk.design.glass.rememberLiquidGroupState
import app.enk.design.glass.rememberLiquidPressState
import app.enk.design.motion.ShellMotion
import app.enk.design.shell.GlassSource
import app.enk.design.shell.LocalReduceEffects
import app.enk.design.shell.rememberGlassSource
import app.enk.design.theme.LocalShellColors
import kotlin.math.roundToInt
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
    accent: Color = EnkColors.accent,
    interactive: Boolean = true
) {
    val colors = LocalShellColors.current
    val monoFont = LocalMonoFont.current
    val reduceEffects = LocalReduceEffects.current
    val density = LocalDensity.current
    val scope = rememberCoroutineScope()

    var state by remember { mutableStateOf(initialState) }
    var lastInteractionMs by remember { mutableLongStateOf(System.currentTimeMillis()) }
    var isSliderTouched by remember { mutableStateOf(false) }

    fun spec(): AnimationSpec<Float> =
        if (reduceEffects) tween(100) else ShellMotion.sheet.toSpec()

    val openAnim = remember { Animatable(if (initialState == IslandState.Rest) 0f else 1f) }
    val lampAnim = remember { Animatable(if (initialState == IslandState.Lamp) 1f else 0f) }

    val restPressState = rememberLiquidPressState()
    val toolbarPressState = rememberLiquidPressState()
    val lampPressState = rememberLiquidPressState()

    val liquidGroupState = rememberLiquidGroupState()

    // Animate open state
    LaunchedEffect(state) {
        val targetOpen = if (state == IslandState.Rest) 0f else 1f
        val targetLamp = if (state == IslandState.Lamp) 1f else 0f
        scope.launch { openAnim.animateTo(targetOpen, spec()) }
        scope.launch { lampAnim.animateTo(targetLamp, spec()) }
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

    val isOpen = state != IslandState.Rest

    Box(
        modifier = modifier
            .then(if (isOpen) Modifier.fillMaxSize() else Modifier.fillMaxWidth())
            .zIndex(10f),
        contentAlignment = Alignment.TopCenter
    ) {
        // Tap outside to close
        if (isOpen) {
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

        // Island container positioned at safeTop + 10.dp
        BoxWithConstraints(
            modifier = Modifier
                .padding(top = safeTop + 10.dp)
                .fillMaxWidth(),
            contentAlignment = Alignment.TopCenter
        ) {
            val pairWidth = 258.dp
            val pairLeft = (maxWidth - pairWidth) / 2

            if (openAnim.value == 0f) {
                // Rest capsule: 150 x 40 dp
                Box(
                    modifier = Modifier
                        .size(150.dp, 40.dp)
                        .testTag("top_island")
                        .semantics {
                            role = Role.Button
                            contentDescription = "Writer menu"
                            stateDescription = "Closed"
                        }
                        .liquidPress(restPressState)
                        .glassSurface(
                            spec = GlassSpecs.Floating,
                            liquid = LiquidSpecs.Island,
                            shape = RoundedCornerShape(20.dp),
                            source = glassSource,
                            press = restPressState
                        )
                        .clickable(
                            enabled = interactive,
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null
                        ) {
                            lastInteractionMs = System.currentTimeMillis()
                            state = IslandState.Actions
                        },
                    contentAlignment = Alignment.Center
                ) {
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
            } else {
                // Open: Toolbar (200 x 46 dp) + 12 dp gap + Lamp button (46 dp circle) = 258 dp
                Box(
                    modifier = Modifier
                        .width(pairWidth)
                        .height(46.dp)
                ) {
                    // Toolbar (200 x 46 dp)
                    Box(
                        modifier = Modifier
                            .size(200.dp, 46.dp)
                            .align(Alignment.CenterStart)
                            .testTag("top_island")
                            .semantics {
                                role = Role.Button
                                contentDescription = "Writer menu"
                                stateDescription = "Open"
                            }
                            .liquidPress(toolbarPressState)
                            .glassSurface(
                                spec = GlassSpecs.Floating,
                                liquid = LiquidSpecs.Island,
                                shape = RoundedCornerShape(23.dp),
                                source = glassSource,
                                press = toolbarPressState
                            )
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(horizontal = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Keyboard button
                            Box(
                                modifier = Modifier
                                    .size(44.dp)
                                    .testTag("keyboard_switch")
                                    .semantics {
                                        role = Role.Button
                                        contentDescription = "Switch keyboard"
                                    }
                                    .clickable(
                                        interactionSource = remember { MutableInteractionSource() },
                                        indication = null
                                    ) {
                                        lastInteractionMs = System.currentTimeMillis()
                                        onToggleKeyboard()
                                        state = IslandState.Rest
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                KeyboardSwitchGlyph(ink = colors.t1, modifier = Modifier.size(22.dp))
                            }

                            // Box button
                            Box(
                                modifier = Modifier
                                    .size(44.dp)
                                    .testTag("box_button")
                                    .semantics {
                                        role = Role.Button
                                        contentDescription = "Open the Box"
                                    }
                                    .clickable(
                                        interactionSource = remember { MutableInteractionSource() },
                                        indication = null
                                    ) {
                                        lastInteractionMs = System.currentTimeMillis()
                                        onOpenBox()
                                        state = IslandState.Rest
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                BoxGlyph(ink = colors.t1, modifier = Modifier.size(22.dp))
                            }

                            // Short count: e.g. "85 / 3000"
                            val shortCount = metaText.replace(Regex("^p\\.\\d+\\s*·\\s*"), "")
                            Text(
                                text = shortCount,
                                color = colors.t2,
                                fontFamily = monoFont,
                                fontSize = 10.5.sp,
                                modifier = Modifier
                                    .weight(1f)
                                    .padding(end = 12.dp),
                                textAlign = TextAlign.End,
                                maxLines = 1
                            )
                        }
                    }

                    // Round Lamp button (46 dp circle)
                    Box(
                        modifier = Modifier
                            .size(46.dp)
                            .align(Alignment.CenterEnd)
                            .testTag("lamp_button")
                            .semantics {
                                role = Role.Button
                                contentDescription = "Lamp"
                                stateDescription = if (lampOn) "On" else "Off"
                            }
                            .liquidPress(lampPressState)
                            .glassSurface(
                                spec = GlassSpecs.Button,
                                liquid = LiquidSpecs.Button,
                                shape = CircleShape,
                                source = glassSource,
                                press = lampPressState
                            )
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null
                            ) {
                                lastInteractionMs = System.currentTimeMillis()
                                state = if (state == IslandState.Lamp) IslandState.Actions else IslandState.Lamp
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        // Soft glow when lit
                        if (lampOn) {
                            Box(
                                modifier = Modifier
                                    .size(34.dp)
                                    .background(lampColor.copy(alpha = 0.28f), CircleShape)
                            )
                        }

                        // Lamp glyph
                        LampGlyph(
                            lampOn = lampOn,
                            lampColor = lampColor,
                            ink = if (lampOn) lampColor else colors.t1,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }

                // Lamp popup: 280 x 230 dp, right-aligned with Lamp button, top 8 dp below
                if (state == IslandState.Lamp) {
                    val popupWidth = 280.dp
                    val popupHeight = 230.dp
                    val popupLeft = pairLeft + pairWidth - popupWidth
                    val popupTop = 46.dp + 8.dp // 54 dp

                    Box(
                        modifier = Modifier
                            .align(Alignment.TopStart)
                            .offset(x = popupLeft, y = popupTop)
                            .requiredSize(popupWidth, popupHeight)
                            .glassSurface(
                                spec = GlassSpecs.Floating,
                                liquid = LiquidSpecs.Panel,
                                shape = RoundedCornerShape(28.dp),
                                source = glassSource
                            )
                    ) {
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
                            },
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun LampGlyph(
    lampOn: Boolean,
    lampColor: Color,
    ink: Color,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier) {
        val strokePx = 1.6f.dp.toPx()
        val c = if (lampOn) lampColor else ink

        // Shade bell
        val bellPath = Path().apply {
            moveTo(size.width * 0.35f, size.height * 0.28f)
            lineTo(size.width * 0.65f, size.height * 0.28f)
            lineTo(size.width * 0.82f, size.height * 0.58f)
            lineTo(size.width * 0.18f, size.height * 0.58f)
            close()
        }
        drawPath(bellPath, color = c, style = Stroke(width = strokePx, cap = StrokeCap.Round, join = StrokeJoin.Round))

        // Stem
        drawLine(
            color = c,
            start = Offset(size.width * 0.5f, size.height * 0.58f),
            end = Offset(size.width * 0.5f, size.height * 0.82f),
            strokeWidth = strokePx,
            cap = StrokeCap.Round
        )

        // Base
        drawLine(
            color = c,
            start = Offset(size.width * 0.28f, size.height * 0.82f),
            end = Offset(size.width * 0.72f, size.height * 0.82f),
            strokeWidth = strokePx,
            cap = StrokeCap.Round
        )
    }
}

@Composable
private fun KeyboardSwitchGlyph(ink: Color, modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        val strokePx = 1.6f.dp.toPx()

        // Keyboard outline
        drawRoundRect(
            color = ink,
            topLeft = Offset(2f.dp.toPx(), 4f.dp.toPx()),
            size = Size(18f.dp.toPx(), 14f.dp.toPx()),
            cornerRadius = CornerRadius(2.5f.dp.toPx()),
            style = Stroke(width = strokePx)
        )

        // Keys row 1
        drawLine(color = ink, start = Offset(5f.dp.toPx(), 8f.dp.toPx()), end = Offset(7f.dp.toPx(), 8f.dp.toPx()), strokeWidth = strokePx, cap = StrokeCap.Round)
        drawLine(color = ink, start = Offset(10f.dp.toPx(), 8f.dp.toPx()), end = Offset(12f.dp.toPx(), 8f.dp.toPx()), strokeWidth = strokePx, cap = StrokeCap.Round)
        drawLine(color = ink, start = Offset(15f.dp.toPx(), 8f.dp.toPx()), end = Offset(17f.dp.toPx(), 8f.dp.toPx()), strokeWidth = strokePx, cap = StrokeCap.Round)

        // Spacebar
        drawLine(color = ink, start = Offset(6f.dp.toPx(), 14f.dp.toPx()), end = Offset(16f.dp.toPx(), 14f.dp.toPx()), strokeWidth = strokePx, cap = StrokeCap.Round)
    }
}

@Composable
private fun BoxGlyph(ink: Color, modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        val strokePx = 1.6f.dp.toPx()

        // Box drawer
        drawRoundRect(
            color = ink,
            topLeft = Offset(3f.dp.toPx(), 4f.dp.toPx()),
            size = Size(16f.dp.toPx(), 14f.dp.toPx()),
            cornerRadius = CornerRadius(2.5f.dp.toPx()),
            style = Stroke(width = strokePx)
        )

        // Handle
        drawLine(
            color = ink,
            start = Offset(8f.dp.toPx(), 10f.dp.toPx()),
            end = Offset(14f.dp.toPx(), 10f.dp.toPx()),
            strokeWidth = strokePx,
            cap = StrokeCap.Round
        )
    }
}
