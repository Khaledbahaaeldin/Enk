package app.enk.writer

import androidx.compose.ui.unit.Density
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.foundation.shape.CornerSize
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.layout.layout
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
import app.enk.design.motion.ShellSpring
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
    // Height follows width by about 40 ms and settles with a hair of give (about 2 percent).
    val lampHeightAnim = remember { Animatable(if (initialState == IslandState.Lamp) 1f else 0f) }

    val toolbarPressState = rememberLiquidPressState()
    val lampPressState = rememberLiquidPressState()

    // Animate open state
    LaunchedEffect(state) {
        val targetOpen = if (state == IslandState.Rest) 0f else 1f
        val targetLamp = if (state == IslandState.Lamp) 1f else 0f
        scope.launch { openAnim.animateTo(targetOpen, spec()) }
        val lampSpec: AnimationSpec<Float> = if (reduceEffects) tween(100) else ShellMotion.default.toSpec()
        val heightSpec: AnimationSpec<Float> = if (reduceEffects) tween(100) else ShellSpring(0.40f, 0.78f).toSpec()
        scope.launch { lampAnim.animateTo(targetLamp, lampSpec) }
        scope.launch {
            if (targetLamp > 0f && !reduceEffects) delay(40)
            lampHeightAnim.animateTo(targetLamp, heightSpec)
        }
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
    // What is composed follows the state and the animations' running flags only; every
    // position, size and alpha below is read from the Animatables in layout, layer and draw
    // lambdas, so the island's split and the lamp panel's growth never recompose it per frame.
    val composeOpen = isOpen || openAnim.isRunning
    val composeRest = !isOpen || openAnim.isRunning
    val composeLamp = state == IslandState.Lamp || lampAnim.isRunning || lampHeightAnim.isRunning

    val splitGroup = rememberLiquidGroupState()
    val lampGroup = rememberLiquidGroupState()

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

        // Island area at safeTop + 10 dp: one row 46 dp high, plus the lamp panel below it.
        BoxWithConstraints(
            modifier = Modifier
                .padding(top = safeTop + 10.dp)
                .fillMaxWidth()
                .height(if (composeLamp) IslandGeometry.ROW_DP.dp + IslandGeometry.PANEL_GAP_DP.dp + IslandGeometry.PANEL_H_DP.dp else IslandGeometry.ROW_DP.dp)
        ) {
            val px = density.density
            val centreXPx = constraints.maxWidth / 2f

            fun toolbarRect(): IslandRect = IslandGeometry.toolbar(openAnim.value).toPx(centreXPx, px)
            fun lampRect(): IslandRect = IslandGeometry.lampButton(openAnim.value).toPx(centreXPx, px)
            fun panelRect(): IslandRect = IslandGeometry.panel(lampAnim.value, lampHeightAnim.value, openAnim.value).toPx(centreXPx, px)
            val panelFinal = IslandGeometry.panel(1f, 1f).toPx(centreXPx, px)

            // Melt 1: the capsule splitting into the toolbar and the round Lamp button.
            LiquidGroup(
                state = splitGroup,
                shapes = { listOf(toolbarRect().toShape(), lampRect().toShape()) },
                spec = LiquidSpecs.Island,
                source = glassSource,
                pressed = { openAnim.isRunning },
                blend = { if (openAnim.isRunning) IslandGeometry.meltBlend(openAnim.value) else 0f },
                modifier = Modifier.fillMaxSize()
            )
            // Melt 2: the lamp panel growing out of the Lamp button.
            if (composeLamp) {
                LiquidGroup(
                    state = lampGroup,
                    shapes = { listOf(lampRect().toShape(), panelRect().toShape()) },
                    spec = LiquidSpecs.Panel,
                    source = glassSource,
                    pressed = { lampAnim.isRunning || lampHeightAnim.isRunning },
                    blend = { if (lampAnim.isRunning || lampHeightAnim.isRunning) IslandGeometry.meltBlend(lampAnim.value) else 0f },
                    modifier = Modifier.fillMaxSize()
                )
            }

            // Lamp panel glass: below the toolbar and the Lamp button, so it never covers the glyph.
            if (composeLamp) {
                Box(
                    modifier = Modifier
                        .placeAt { panelRect() }
                        .graphicsLayer { alpha = if (lampGroup.merging) 0f else 1f }
                        .glassSurface(
                            spec = GlassSpecs.Floating,
                            liquid = LiquidSpecs.Panel,
                            shape = RoundedCornerShape(
                                object : CornerSize {
                                    override fun toPx(shapeSize: Size, density: Density): Float =
                                        minOf(panelRect().radius ?: 0f, shapeSize.minDimension / 2f)
                                }
                            ),
                            source = glassSource
                        )
                )
            }

            // Toolbar glass (the rest capsule at openAnim 0, the toolbar at 1).
            Box(
                modifier = Modifier
                    .placeAt { toolbarRect() }
                    .graphicsLayer { alpha = if (splitGroup.merging) 0f else 1f }
                    .glassSurface(
                        spec = GlassSpecs.Floating,
                        liquid = LiquidSpecs.Island,
                        shape = RoundedCornerShape(50),
                        source = glassSource,
                        press = toolbarPressState
                    )
            )

            // Toolbar content and touch target.
            Box(
                modifier = Modifier
                    .placeAt { toolbarRect() }
                    .testTag("top_island")
                    .semantics {
                        role = Role.Button
                        contentDescription = "Writer menu"
                        stateDescription = if (isOpen) "Open" else "Closed"
                    }
                    .liquidPress(toolbarPressState)
                    .clickable(
                        enabled = interactive && !isOpen,
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) {
                        lastInteractionMs = System.currentTimeMillis()
                        state = IslandState.Actions
                    },
                contentAlignment = Alignment.Center
            ) {
                if (composeRest) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .graphicsLayer { alpha = IslandGeometry.restContentAlpha(openAnim.value) },
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
                }
                if (composeOpen) {
                    Row(
                        modifier = Modifier
                            .fillMaxSize()
                            .graphicsLayer { alpha = IslandGeometry.openContentAlpha(openAnim.value) }
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
                                    enabled = isOpen,
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
                                    enabled = isOpen,
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
            }

            if (composeOpen) {
                // Round Lamp button glass: pinches off the capsule's right end.
                Box(
                    modifier = Modifier
                        .placeAt { lampRect() }
                        .graphicsLayer {
                            alpha = if (splitGroup.merging || lampGroup.merging) 0f else 1f
                        }
                        .glassSurface(
                            spec = GlassSpecs.Button,
                            liquid = LiquidSpecs.Button,
                            shape = CircleShape,
                            source = glassSource,
                            press = lampPressState
                        )
                )
                // Lamp button content and touch target.
                Box(
                    modifier = Modifier
                        .placeAt { lampRect() }
                        .graphicsLayer { alpha = IslandGeometry.openContentAlpha(openAnim.value) }
                        .testTag("lamp_button")
                        .semantics {
                            role = Role.Button
                            contentDescription = "Lamp"
                            stateDescription = if (lampOn) "On" else "Off"
                        }
                        .liquidPress(lampPressState)
                        .clickable(
                            enabled = isOpen,
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
                    LampGlyph(
                        lampOn = lampOn,
                        lampColor = lampColor,
                        ink = if (lampOn) lampColor else colors.t1,
                        modifier = Modifier.size(22.dp)
                    )
                }
            }

            // Lamp panel: 280 x 230 dp, right-aligned with the Lamp button, 8 dp below it,
            // growing out of the button. The glass grows; the content is laid out once at its
            // final place and revealed by clipping to the live glass shape (never re-measured).
            if (composeLamp) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .drawWithContent {
                            val r = panelRect()
                            val rad = minOf(r.radius ?: 0f, minOf(r.width, r.height) / 2f)
                            val clip = Path().apply {
                                addRoundRect(
                                    RoundRect(r.left, r.top, r.left + r.width, r.top + r.height, CornerRadius(rad))
                                )
                            }
                            clipPath(clip) { this@drawWithContent.drawContent() }
                        }
                ) {
                    Box(modifier = Modifier.placeAt { panelFinal }) {
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
                            reveal = { lampAnim.value },
                            modifier = Modifier.align(Alignment.TopStart)
                        )
                    }
                }
            }
        }
    }
}

/** A rectangle in dp relative to the island's centre line (x) and the island area's top (y). */
internal data class IslandRectDp(
    val centreDx: Float,
    val top: Float,
    val width: Float,
    val height: Float,
    /** Corner radius; null means a capsule (half the shorter side). */
    val radius: Float? = null
) {
    fun toPx(centreXPx: Float, density: Float): IslandRect = IslandRect(
        left = centreXPx + (centreDx - width / 2f) * density,
        top = top * density,
        width = width * density,
        height = height * density,
        radius = radius?.let { it * density }
    )
}

/** A rectangle in pixels in the island area's space. */
internal data class IslandRect(
    val left: Float,
    val top: Float,
    val width: Float,
    val height: Float,
    val radius: Float? = null
) {
    fun toShape(): LiquidShape = LiquidShape(
        cx = left + width / 2f,
        cy = top + height / 2f,
        halfW = width / 2f,
        halfH = height / 2f,
        radius = minOf(radius ?: Float.MAX_VALUE, minOf(width, height) / 2f)
    )
}

/** The island's geometry at a given open (p) or lamp (q) progress, in dp. */
internal object IslandGeometry {
    const val ROW_DP = 46f
    const val REST_W = 150f
    const val REST_H = 40f
    const val TOOLBAR_W = 200f
    const val LAMP_D = 46f
    const val GAP = 12f
    const val PAIR_W = TOOLBAR_W + GAP + LAMP_D // 258
    const val PANEL_W = 280f
    const val PANEL_H_DP = 230f
    const val PANEL_GAP_DP = 8f
    const val PANEL_R = 28f
    const val BUD_OVERLAP_DP = 8f
    const val BUD_PEEK_DP = 2f

    private fun lerp(a: Float, b: Float, t: Float) = a + (b - a) * t

    /** The capsule (p = 0) stretching left into the toolbar (p = 1). */
    fun toolbar(p: Float): IslandRectDp {
        val t = p.coerceIn(0f, 1.2f)
        val restLeft = -REST_W / 2f
        val restRight = REST_W / 2f
        val endLeft = -PAIR_W / 2f
        val endRight = endLeft + TOOLBAR_W
        val left = lerp(restLeft, endLeft, t)
        val right = lerp(restRight, endRight, t)
        val h = lerp(REST_H, ROW_DP, t.coerceAtMost(1f))
        // Top-aligned at the island's top, as the rest capsule always was.
        return IslandRectDp((left + right) / 2f, 0f, right - left, h)
    }

    /** The round Lamp button: starts inside the capsule's right end, settles beside the toolbar. */
    fun lampButton(p: Float): IslandRectDp {
        val t = p.coerceIn(0f, 1.2f)
        val startD = REST_H
        val startCentre = REST_W / 2f - startD / 2f
        val endCentre = PAIR_W / 2f - LAMP_D / 2f
        val d = lerp(startD, LAMP_D, t.coerceAtMost(1f))
        return IslandRectDp(lerp(startCentre, endCentre, t), 0f, d, d)
    }

    /**
     * The lamp panel, built from its four edges. The right edge never moves: it stays on the
     * Lamp button's right edge (the anchor), so the panel grows left and down out of the button
     * with no centre drift. Width leads height: [widthP] and [heightP] are separate progresses.
     * The corner radius runs from the round button's to the panel's, so the melt hands back to
     * the real panel with the same shape.
     */
    fun panel(widthP: Float, heightP: Float, openP: Float = 1f): IslandRectDp {
        val wp = widthP.coerceIn(0f, 1f)
        val button = lampButton(openP)
        val buttonRight = button.centreDx + button.width / 2f
        // At full open this is PAIR_W / 2; while the island folds the button slides left and the
        // panel's right edge goes with it, so a closing panel drains into the button, not into
        // the place the button used to be.
        val right = buttonRight
        val buttonLeft = button.centreDx - button.width / 2f
        val left = lerp(buttonLeft, right - PANEL_W, wp)
        // The panel starts as a small pill tucked into the button's lower edge, not on top of the
        // whole button: that leaves a visible neck while it pulls away, like a drop budding off.
        val startTop = button.top + button.height - BUD_OVERLAP_DP
        val startBottom = button.top + button.height + BUD_PEEK_DP
        val top = lerp(startTop, ROW_DP + PANEL_GAP_DP, heightP.coerceIn(0f, 1f))
        val bottom = lerp(startBottom, ROW_DP + PANEL_GAP_DP + PANEL_H_DP, heightP.coerceAtLeast(0f))
        val r = lerp((BUD_OVERLAP_DP + BUD_PEEK_DP) / 2f, PANEL_R, wp)
        return IslandRectDp((left + right) / 2f, top, right - left, bottom - top, r)
    }

    /**
     * How far the melt reaches during a split or growth: full while the shapes are joined,
     * shrinking to nothing over the last fifth so the bridge thins away before the shapes hand
     * back to their own glass (no pop at the end of the motion).
     */
    fun meltBlend(progress: Float): Float {
        val x = ((progress - 0.8f) / 0.2f).coerceIn(0f, 1f)
        return 1f - x * x * (3f - 2f * x)
    }

    /** The capsule's text is gone before the Lamp button's circle reaches it; the toolbar's content follows. */
    fun restContentAlpha(p: Float): Float = ((0.18f - p) / 0.15f).coerceIn(0f, 1f)
    fun openContentAlpha(p: Float): Float = ((p - 0.36f) / 0.3f).coerceIn(0f, 1f)
}

/** Measures this node at [rect]'s size and places it at [rect]'s position, in the layout phase. */
private fun Modifier.placeAt(rect: () -> IslandRect): Modifier = this.layout { measurable, constraints ->
    val r = rect()
    val w = r.width.roundToInt().coerceAtLeast(0)
    val h = r.height.roundToInt().coerceAtLeast(0)
    val placeable = measurable.measure(Constraints.fixed(w, h))
    layout(constraints.maxWidth, constraints.maxHeight) {
        placeable.place(r.left.roundToInt(), r.top.roundToInt())
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
