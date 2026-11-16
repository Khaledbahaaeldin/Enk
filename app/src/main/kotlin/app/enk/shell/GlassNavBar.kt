package app.enk.shell

import kotlin.math.sign
import app.enk.writer.AndroidHaptics
import app.enk.design.glass.LiquidControlsMath
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ClipOp
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.runtime.snapshotFlow
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.EaseOut
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.layout.layout
import androidx.compose.animation.core.Animatable
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.changedToUp
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.enk.design.effects.LocalEffects
import app.enk.design.effects.ResolvedEffects
import app.enk.design.glass.GlassInteraction
import app.enk.design.glass.GlassLensMath
import app.enk.design.glass.GlassSpecs
import app.enk.design.glass.LiquidGroup
import app.enk.design.glass.LiquidShape
import app.enk.design.glass.LiquidSpecs
import app.enk.design.glass.glassSurface
import app.enk.design.glass.liquidPress
import app.enk.design.glass.rememberLiquidGroupState
import app.enk.design.glass.rememberLiquidPressState
import app.enk.design.motion.ItemBounds
import app.enk.design.motion.LensController
import app.enk.design.motion.ShellMotion
import app.enk.design.shell.GlassBackendKind
import app.enk.design.shell.GlassSource
import app.enk.design.shell.LocalReduceEffects
import app.enk.design.shell.rememberGlassBackend
import app.enk.design.shell.rememberGlassSource
import app.enk.design.EnkColors
import app.enk.design.theme.LocalShellColors
import app.enk.design.theme.LocalShellMaterial
import kotlin.math.abs
import kotlin.math.roundToInt
import kotlinx.coroutines.launch

@Composable
fun GlassNavBar(
    selected: Dest,
    onSelect: (Dest) -> Unit,
    modifier: Modifier = Modifier,
    glassSource: GlassSource = rememberGlassSource()
) {
    val colors = LocalShellColors.current
    val density = LocalDensity.current
    val isRtl = LocalLayoutDirection.current == LayoutDirection.Rtl
    val reduceEffects = rememberGlassBackend() == GlassBackendKind.Solid || LocalReduceEffects.current || LocalEffects.current == ResolvedEffects.Light

    // 3 tabs: Box, Find, Setup
    val dests = remember { listOf(Dest.Box, Dest.Find, Dest.Setup) }
    val rail = LocalRailTransition.current

    val barSource = rememberGlassSource()
    val liquidGroupState = rememberLiquidGroupState()
    val writePressState = rememberLiquidPressState()
    val currentOnSelect by rememberUpdatedState(onSelect)

    BoxWithConstraints(
        modifier = modifier
            .testTag("glass_nav_bar")
            .fillMaxWidth()
            .padding(horizontal = 22.dp)
            .height(62.dp)
    ) {
        val totalWidthPx = constraints.maxWidth.toFloat()
        val writeButtonSizePx = with(density) { 62.dp.toPx() }
        val gapPx = with(density) { 10.dp.toPx() }
        val barWidthPx = maxOf(0f, totalWidthPx - writeButtonSizePx - gapPx)
        val barWidthDp = with(density) { barWidthPx.toDp() }
        val itemWidthPx = barWidthPx / 3f
        val itemWidthDp = with(density) { itemWidthPx.toDp() }

        val itemBoundsList = remember(barWidthPx, itemWidthPx) {
            (0 until 3).map { i ->
                ItemBounds(left = i * itemWidthPx, width = itemWidthPx)
            }
        }
        val lensController = remember(itemBoundsList) { LensController(itemBoundsList) }

        val rawIndex = dests.indexOf(selected).let { if (it >= 0) it else 0 }
        val selectedIndex = if (isRtl) 2 - rawIndex else rawIndex
        val targetLeft = selectedIndex * itemWidthPx

        val sharedLens = LocalNavLens.current
        val lensLeftAnim = if (sharedLens != null) sharedLens.position(targetLeft) else remember { Animatable(targetLeft) }
        val lensPressAnim = remember { Animatable(0f) } // 0f at rest, 1f when pressed
        val lensPressState = rememberLiquidPressState()
        val meltBlend = remember { Animatable(0f) }
        val scope = rememberCoroutineScope()
        val context = LocalContext.current
        val haptics = remember(context) { AndroidHaptics(context) }
        val lensInteraction = remember { GlassInteraction() }
        // How far the finger has dragged along the bar: the bar leans toward it (rubber-banded).
        val panelOffsetAnim = remember { Animatable(0f) }
        // Smoothed drag velocity (dp/s) that stretches the lens.
        val lensVelocityAnim = remember { Animatable(0f) }

        var isDragging by remember { mutableStateOf(false) }
        var velocity = 0f

        LaunchedEffect(reduceEffects) {
            snapshotFlow { lensVelocityAnim.value }.collect { v ->
                val (sx, sy) = if (reduceEffects) Pair(1f, 1f) else LiquidControlsMath.velocityStretch(v)
                lensInteraction.stretchX = sx
                lensInteraction.stretchY = sy
            }
        }

        LaunchedEffect(selectedIndex, targetLeft, reduceEffects) {
            if (!isDragging) {
                if (reduceEffects) {
                    lensLeftAnim.snapTo(targetLeft)
                } else {
                    lensLeftAnim.animateTo(targetLeft, ShellMotion.lens.toSpec())
                }
            }
        }

        val dragSlopPx = with(density) { ShellMotion.DRAG_SLOP_DP.dp.toPx() }

        fun resolveDest(colIndex: Int): Dest? {
            val idx = if (isRtl) 2 - colIndex else colIndex
            return dests.getOrNull(idx)
        }

        fun resolveCol(dest: Dest): Int {
            val idx = dests.indexOf(dest).coerceAtLeast(0)
            return if (isRtl) 2 - idx else idx
        }

        val gestureModifier = Modifier.pointerInput(barWidthPx, isRtl, selected, reduceEffects) {
            while (true) {
                awaitPointerEventScope {
                    val down = awaitPointerEvent(PointerEventPass.Initial).changes.firstOrNull { it.pressed } ?: return@awaitPointerEventScope
                    val startX = down.position.x
                    val startY = down.position.y
                    val startTime = System.currentTimeMillis()

                    lensController.down(startX, lensLeftAnim.value, itemWidthPx)
                    if (!reduceEffects) {
                        scope.launch { lensPressAnim.animateTo(1f, ShellMotion.press.toSpec()) }
                    }
                    lensPressState.animatePress(scope, 1f, instant = reduceEffects)

                    var currentX = startX
                    var currentY = startY
                    var lastX = startX
                    var lastTime = startTime
                    var dragged = false
                    var cancelled = false
                    var prevDragX = startX
                    var lastTickCol = -1

                    while (true) {
                        val event = awaitPointerEvent(PointerEventPass.Initial)
                        val change = event.changes.firstOrNull { it.id == down.id }

                        if (change == null) {
                            cancelled = true
                            break
                        }

                        if (change.isConsumed && !change.changedToUp()) {
                            cancelled = true
                            break
                        }

                        val now = System.currentTimeMillis()
                        val dtSec = maxOf(0.001f, (now - lastTime) / 1000f)
                        velocity = (change.position.x - lastX) / dtSec / density.density
                        lastX = change.position.x
                        lastTime = now

                        currentX = change.position.x
                        currentY = change.position.y

                        if (change.changedToUp()) {
                            break
                        }

                        if (!dragged && abs(currentX - startX) >= dragSlopPx) {
                            dragged = true
                            isDragging = true
                            if (!reduceEffects) {
                                scope.launch { meltBlend.animateTo(1f, ShellMotion.press.toSpec()) }
                            }
                        }

                        if (dragged) {
                            change.consume()
                            val frame = lensController.move(currentX, lastTime)
                            if (frame.highlighted != lastTickCol) {
                                if (lastTickCol != -1 && !reduceEffects) haptics.pulse(6)
                                lastTickCol = frame.highlighted
                            }
                            val dx = currentX - prevDragX
                            prevDragX = currentX
                            scope.launch { panelOffsetAnim.snapTo(panelOffsetAnim.value + dx) }
                            scope.launch { lensVelocityAnim.animateTo(velocity, VELOCITY_SPRING) }
                            // The lens follows the finger on the Catalog's damped value spring,
                            // so it trails it by a hair, like liquid, instead of being glued to it.
                            scope.launch { lensLeftAnim.animateTo(frame.left, FOLLOW_SPRING) }
                        }
                    }

                    if (!reduceEffects) {
                        scope.launch { lensPressAnim.animateTo(0f, ShellMotion.release.toSpec()) }
                    }
                    lensPressState.animatePress(scope, 0f, instant = reduceEffects)
                    scope.launch { panelOffsetAnim.animateTo(0f, PANEL_RETURN_SPRING) }
                    scope.launch { lensVelocityAnim.animateTo(0f, VELOCITY_SPRING) }
                    scope.launch {
                        if (reduceEffects) meltBlend.snapTo(0f) else meltBlend.animateTo(0f, ShellMotion.release.toSpec())
                    }

                    val barHeightPx = with(density) { 62.dp.toPx() }
                    val isOutside = currentY < -30f || currentY > barHeightPx + 30f

                    if (cancelled || isOutside) {
                        isDragging = false
                        scope.launch {
                            val resetTarget = resolveCol(selected) * itemWidthPx
                            if (reduceEffects) {
                                lensLeftAnim.snapTo(resetTarget)
                            } else {
                                lensLeftAnim.animateTo(resetTarget, ShellMotion.lens.toSpec())
                            }
                        }
                    } else if (dragged) {
                        val release = lensController.up(currentX, lastTime)
                        isDragging = false
                        velocity = release.velocity
                        val targetDest = resolveDest(release.targetIndex)
                        if (targetDest != null && targetDest != selected) {
                            currentOnSelect(targetDest)
                        } else {
                            scope.launch {
                                val resetTarget = release.targetIndex * itemWidthPx
                                if (reduceEffects) {
                                    lensLeftAnim.snapTo(resetTarget)
                                } else {
                                    lensLeftAnim.animateTo(
                                        resetTarget,
                                        release.spring.toSpec(),
                                        initialVelocity = release.velocity
                                    )
                                }
                            }
                        }
                    } else {
                        // Quick tap on pointer
                        val tapIdx = lensController.tapIndex(currentX)
                        val targetDest = tapIdx?.let { resolveDest(it) }
                        if (targetDest != null && targetDest != selected) {
                            currentOnSelect(targetDest)
                        }
                    }
                }
            }
        }

        // Lens sizing: at rest 52 dp height; grows by 16 dp height and 14 dp width when pressed.
        // Read only inside layout, draw and shape lambdas: a lens drag or press never
        // recomposes the bar.
        val lensRestHeightPx = with(density) { 52.dp.toPx() }
        val lensGrowHeightPx = with(density) { 16.dp.toPx() }
        val lensGrowWidthPx = with(density) { 14.dp.toPx() }
        val barHeightPx = with(density) { 62.dp.toPx() }
        fun lensHeightPx(): Float = lensRestHeightPx + lensGrowHeightPx * lensPressAnim.value
        fun lensWidthPx(): Float = itemWidthPx + lensGrowWidthPx * lensPressAnim.value
        fun lensCenterXPx(): Float = lensLeftAnim.value + itemWidthPx / 2f

        // The whole bar reacts to the touch: it swells by 16 dp of width while pressed and
        // leans up to 4 dp toward the finger (rubber-banded, ease-out), as the Catalog's tabs do.
        val barSwellPx = with(density) { 16.dp.toPx() }
        val barLeanPx = with(density) { 4.dp.toPx() }
        fun barScale(): Float = 1f + (barSwellPx / maxOf(1f, barWidthPx)) * lensPressAnim.value
        fun barTx(): Float {
            val frac = (panelOffsetAnim.value / maxOf(1f, barWidthPx)).coerceIn(-1f, 1f)
            return barLeanPx * sign(frac) * EaseOut.transform(abs(frac))
        }

        // LiquidGroup shapes for melting lens & write button
        val barOriginX = if (isRtl) writeButtonSizePx + gapPx else 0f
        val writeOriginX = if (isRtl) 0f else barWidthPx + gapPx
        val centerY = with(density) { 31.dp.toPx() }

        // LiquidGroup backdrop covering the full nav area. The melt only reaches as far as
        // [meltBlend] lets it: it grows while the lens is dragged and shrinks back after, so the
        // lens resting on the last tab (10 dp from Write) is never melted into the button.
        LiquidGroup(
            state = liquidGroupState,
            shapes = {
                val k = barScale()
                val h = lensHeightPx() * k
                val w = lensWidthPx() * k
                listOf(
                    LiquidShape(
                        // The bar scales around its own centre and leans; the melt follows it.
                        cx = barOriginX + barWidthPx / 2f + (lensCenterXPx() - barWidthPx / 2f) * k + barTx(),
                        cy = centerY,
                        halfW = w / 2f,
                        halfH = h / 2f,
                        radius = h / 2f
                    ),
                    LiquidShape(
                        cx = writeOriginX + writeButtonSizePx / 2f,
                        cy = centerY,
                        halfW = writeButtonSizePx / 2f,
                        halfH = writeButtonSizePx / 2f,
                        radius = writeButtonSizePx / 2f
                    )
                )
            },
            spec = LiquidSpecs.BarLens,
            source = glassSource,
            pressed = { isDragging },
            mergeDistance = 24.dp,
            blend = { meltBlend.value },
            modifier = Modifier.fillMaxSize()
        )

        // Row holding [Bar] [Gap] [WriteButton]
        Row(
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer {
                    if (rail != null) {
                        alpha = RailTransitionMath.barAlpha(rail.boxP())
                    }
                },
            verticalAlignment = Alignment.CenterVertically
        ) {
            // The 3-tab Bar
            Box(
                modifier = Modifier
                    .testTag("nav_bar_track")
                    .width(barWidthDp)
                    .height(62.dp)
                    .onGloballyPositioned {
                        val b = it.boundsInRoot()
                        rail?.barBounds = b
                    }
                    .then(gestureModifier)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .graphicsLayer {
                            val k = barScale()
                            scaleX = k
                            scaleY = k
                            translationX = barTx()
                        }
                ) {
                // Bar glass background
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .graphicsLayer { alpha = if (rail?.flightP() != null) 0f else 1f }
                        .glassSurface(
                            spec = GlassSpecs.NavBody,
                            liquid = LiquidSpecs.Bar,
                            shape = RoundedCornerShape(31.dp),
                            source = glassSource,
                            exportedSource = barSource
                        )
                )

                // Lens behind items inside the bar
                Box(
                    modifier = Modifier
                        .layout { measurable, _ ->
                            val w = lensWidthPx().roundToInt()
                            val h = lensHeightPx().roundToInt()
                            val placeable = measurable.measure(Constraints.fixed(w, h))
                            layout(w, h) {
                                placeable.place(
                                    (lensCenterXPx() - w / 2f).roundToInt(),
                                    ((barHeightPx - h) / 2f).roundToInt()
                                )
                            }
                        }
                        .graphicsLayer { alpha = if (liquidGroupState.merging) 0f else 1f }
                        .glassSurface(
                            spec = GlassSpecs.NavLens,
                            liquid = LiquidSpecs.BarLens,
                            shape = RoundedCornerShape(50),
                            source = glassSource,
                            extraSource = barSource,
                            interaction = lensInteraction,
                            press = lensPressState
                        )
                )

                // Tabs container: the base row (ink t2), and a second copy of the row in the accent
                // colour that shows only inside the lens, where the glyphs also grow while pressed:
                // the lens reads as glass that colours and magnifies what it passes over.
                fun lensClip(): Path {
                    val h = lensHeightPx()
                    val w = lensWidthPx()
                    val l = lensCenterXPx() - w / 2f
                    val t = (barHeightPx - h) / 2f
                    return Path().apply {
                        addRoundRect(RoundRect(l, t, l + w, t + h, CornerRadius(h / 2f)))
                    }
                }
                Box(modifier = Modifier.fillMaxSize()) {
                    NavTabsRow(
                        dests = dests,
                        selected = selected,
                        ink = colors.t2,
                        glyphScale = { 1f },
                        interactive = true,
                        onSelectDest = { dest -> if (dest != selected) currentOnSelect(dest) },
                        modifier = Modifier.drawWithContent {
                            clipPath(lensClip(), clipOp = ClipOp.Difference) { this@drawWithContent.drawContent() }
                        }
                    )
                    NavTabsRow(
                        dests = dests,
                        selected = selected,
                        ink = EnkColors.accent,
                        glyphScale = { 1f + 0.2f * lensPressAnim.value },
                        interactive = false,
                        onSelectDest = {},
                        modifier = Modifier
                            .clearAndSetSemantics {}
                            .drawWithContent {
                                clipPath(lensClip()) { this@drawWithContent.drawContent() }
                            }
                    )
                }
                }
            }

            Spacer(modifier = Modifier.width(10.dp))

            // Round Write button (62 dp circle)
            Box(
                modifier = Modifier
                    .testTag("write_button")
                    .size(62.dp)
                    .onGloballyPositioned { rail?.writeButtonBounds = it.boundsInRoot() }
                    .semantics(mergeDescendants = true) {
                        this.role = Role.Button
                        this.contentDescription = "Write"
                        this.onClick(label = "Write") {
                            currentOnSelect(Dest.Write)
                            true
                        }
                    }
                    .liquidPress(writePressState)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = { currentOnSelect(Dest.Write) }
                    ),
                contentAlignment = Alignment.Center
            ) {
                // Button glass surface
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .graphicsLayer { alpha = if (liquidGroupState.merging) 0f else 1f }
                        .glassSurface(
                            spec = GlassSpecs.Button,
                            liquid = LiquidSpecs.Button,
                            shape = CircleShape,
                            source = glassSource,
                            press = writePressState
                        )
                )

                // Write glyph in EnkColors.accent
                Canvas(modifier = Modifier.size(24.dp)) {
                    val strokePx = with(density) { 1.6.dp.toPx() }
                    // Rail line
                    drawLine(
                        color = EnkColors.accent,
                        start = Offset(2f.dp.toPx(), 14f.dp.toPx()),
                        end = Offset(22f.dp.toPx(), 14f.dp.toPx()),
                        strokeWidth = strokePx,
                        cap = StrokeCap.Round
                    )
                    // Carriage
                    drawRoundRect(
                        color = EnkColors.accent,
                        topLeft = Offset(8f.dp.toPx(), 5f.dp.toPx()),
                        size = Size(8f.dp.toPx(), 8f.dp.toPx()),
                        cornerRadius = CornerRadius(1.5f.dp.toPx()),
                        style = Stroke(width = strokePx)
                    )
                    // Strip in carriage
                    drawLine(
                        color = colors.ui,
                        start = Offset(12f.dp.toPx(), 6.5f.dp.toPx()),
                        end = Offset(12f.dp.toPx(), 11.5f.dp.toPx()),
                        strokeWidth = strokePx,
                        cap = StrokeCap.Round
                    )
                }
            }
        }
    }
}

private val FOLLOW_SPRING = spring<Float>(dampingRatio = 1f, stiffness = 1000f)
private val VELOCITY_SPRING = spring<Float>(dampingRatio = 0.5f, stiffness = 300f)
private val PANEL_RETURN_SPRING = spring<Float>(dampingRatio = 0.5f, stiffness = 300f)

/** One row of the three tabs. Drawn twice by the bar: the base row, and an accent copy seen through the lens. */
@Composable
private fun NavTabsRow(
    dests: List<Dest>,
    selected: Dest,
    ink: Color,
    glyphScale: () -> Float,
    interactive: Boolean,
    onSelectDest: (Dest) -> Unit,
    modifier: Modifier = Modifier
) {
    val density = LocalDensity.current
    val strokePx = with(density) { 1.6.dp.toPx() }
    Row(
        modifier = modifier
            .fillMaxSize()
            .padding(vertical = 5.dp)
            .then(if (interactive) Modifier.selectableGroup() else Modifier),
        verticalAlignment = Alignment.CenterVertically
    ) {
        for (dest in dests) {
            val isSelected = dest == selected
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .graphicsLayer {
                        val k = glyphScale()
                        scaleX = k
                        scaleY = k
                    }
                    .then(
                        if (interactive) {
                            Modifier.semantics(mergeDescendants = true) {
                                this.role = Role.Tab
                                this.selected = isSelected
                                this.contentDescription = dest.name
                                this.onClick(label = dest.name) {
                                    onSelectDest(dest)
                                    true
                                }
                            }
                        } else {
                            Modifier
                        }
                    ),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = androidx.compose.foundation.layout.Arrangement.Center
            ) {
                Canvas(modifier = Modifier.size(22.dp)) {
                    when (dest) {
                        Dest.Box -> {
                            drawRoundRect(
                                color = ink,
                                topLeft = Offset(3f.dp.toPx(), 6f.dp.toPx()),
                                size = Size(16f.dp.toPx(), 12f.dp.toPx()),
                                cornerRadius = CornerRadius(3f.dp.toPx()),
                                style = Stroke(width = strokePx)
                            )
                            drawLine(
                                color = ink,
                                start = Offset(8f.dp.toPx(), 12f.dp.toPx()),
                                end = Offset(14f.dp.toPx(), 12f.dp.toPx()),
                                strokeWidth = strokePx,
                                cap = StrokeCap.Round
                            )
                        }
                        Dest.Find -> {
                            drawCircle(
                                color = ink,
                                radius = 5.5f.dp.toPx(),
                                center = Offset(9.5f.dp.toPx(), 9.5f.dp.toPx()),
                                style = Stroke(width = strokePx)
                            )
                            drawLine(
                                color = ink,
                                start = Offset(13.5f.dp.toPx(), 13.5f.dp.toPx()),
                                end = Offset(18.5f.dp.toPx(), 18.5f.dp.toPx()),
                                strokeWidth = strokePx,
                                cap = StrokeCap.Round
                            )
                        }
                        Dest.Setup -> {
                            for ((y, x) in listOf(6f to 8f, 11f to 14f, 16f to 9f)) {
                                drawLine(
                                    color = ink,
                                    start = Offset(3f.dp.toPx(), y.dp.toPx()),
                                    end = Offset(19f.dp.toPx(), y.dp.toPx()),
                                    strokeWidth = strokePx,
                                    cap = StrokeCap.Round
                                )
                                drawCircle(
                                    color = ink,
                                    radius = 2f.dp.toPx(),
                                    center = Offset(x.dp.toPx(), y.dp.toPx()),
                                    style = Fill
                                )
                            }
                        }
                        Dest.Write -> Unit
                    }
                }

                Spacer(modifier = Modifier.height(3.dp))

                Text(
                    text = dest.name,
                    fontSize = 10.5.sp,
                    fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                    color = ink
                )
            }
        }
    }
}
