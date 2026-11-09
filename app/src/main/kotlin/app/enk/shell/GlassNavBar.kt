package app.enk.shell

import androidx.compose.animation.core.Animatable
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathOperation
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.asAndroidPath
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.changedToUp
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
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
import app.enk.design.ShadowLayer
import app.enk.design.enkShadow
import app.enk.design.motion.ItemBounds
import app.enk.design.motion.LensController
import app.enk.design.motion.ShellMotion
import app.enk.design.shell.GlassBackendKind
import app.enk.design.shell.GlassLevel
import app.enk.design.shell.GlassSource
import app.enk.design.shell.glass
import app.enk.design.shell.rememberGlassBackend
import app.enk.design.shell.rememberGlassSource
import app.enk.design.theme.LocalShellColors
import app.enk.design.theme.LocalShellMaterial
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.min
import kotlin.math.roundToInt

@Composable
fun GlassNavBar(
    selected: Dest,
    onSelect: (Dest) -> Unit,
    modifier: Modifier = Modifier,
    glassSource: GlassSource = rememberGlassSource()
) {
    val colors = LocalShellColors.current
    val material = LocalShellMaterial.current
    val density = LocalDensity.current
    val isRtl = LocalLayoutDirection.current == LayoutDirection.Rtl
    val reduceEffects = rememberGlassBackend() == GlassBackendKind.Solid

    val dests = remember { listOf(Dest.Write, Dest.Box, Dest.Find, Dest.Setup) }

    BoxWithConstraints(
        modifier = modifier
            .testTag("glass_nav_bar")
            .fillMaxWidth()
            .height(64.dp)
            .glass(level = GlassLevel.Regular, shape = RoundedCornerShape(32.dp), source = glassSource)
    ) {
        val totalWidthPx = constraints.maxWidth.toFloat()
        val paddingPx = with(density) { 5.dp.toPx() }
        val availableWidthPx = totalWidthPx - 2 * paddingPx
        val itemWidthPx = availableWidthPx / 4f

        val itemBoundsList = remember(totalWidthPx, paddingPx, itemWidthPx) {
            (0 until 4).map { i ->
                ItemBounds(left = paddingPx + i * itemWidthPx, width = itemWidthPx)
            }
        }
        val lensController = remember(itemBoundsList) { LensController(itemBoundsList) }

        val rawIndex = dests.indexOf(selected).coerceAtLeast(0)
        val selectedIndex = if (isRtl) 3 - rawIndex else rawIndex
        val targetLeft = paddingPx + selectedIndex * itemWidthPx

        val lensLeftAnim = remember { Animatable(targetLeft) }
        val scope = rememberCoroutineScope()
        val currentOnSelect by rememberUpdatedState(onSelect)

        var isDragging by remember { mutableStateOf(false) }
        var dragHighlightedDest by remember { mutableStateOf<Dest?>(null) }
        var velocity by remember { mutableFloatStateOf(0f) }

        LaunchedEffect(selectedIndex, targetLeft, reduceEffects) {
            if (!isDragging) {
                if (reduceEffects) {
                    lensLeftAnim.snapTo(targetLeft)
                } else {
                    lensLeftAnim.animateTo(targetLeft, ShellMotion.default.toSpec())
                }
            }
        }

        // Pointer gestures for tap and drag
        val barHeightPx = with(density) { 64.dp.toPx() }
        val lensTopOffsetPx = paddingPx.roundToInt()
        val dragSlopPx = with(density) { ShellMotion.DRAG_SLOP_DP.dp.toPx() }

        fun resolveDest(colIndex: Int): Dest? {
            val idx = if (isRtl) 3 - colIndex else colIndex
            return dests.getOrNull(idx)
        }

        fun resolveCol(dest: Dest): Int {
            val idx = dests.indexOf(dest).coerceAtLeast(0)
            return if (isRtl) 3 - idx else idx
        }

        val gestureModifier = Modifier.pointerInput(totalWidthPx, isRtl, selected) {
            while (true) {
                awaitPointerEventScope {
                    val down = awaitPointerEvent(PointerEventPass.Initial).changes.firstOrNull { it.pressed } ?: return@awaitPointerEventScope
                    val startX = down.position.x
                    val startY = down.position.y
                    val startTime = System.currentTimeMillis()

                    lensController.down(startX, lensLeftAnim.value, itemWidthPx)

                    var currentX = startX
                    var currentY = startY
                    var lastTime = startTime
                    var dragged = false
                    var cancelled = false

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

                        currentX = change.position.x
                        currentY = change.position.y
                        lastTime = System.currentTimeMillis()

                        if (change.changedToUp()) {
                            break
                        }

                        if (!dragged && abs(currentX - startX) >= dragSlopPx) {
                            dragged = true
                            isDragging = true
                        }

                        if (dragged) {
                            change.consume()
                            val frame = lensController.move(currentX, lastTime)
                            dragHighlightedDest = resolveDest(frame.highlighted)
                            scope.launch { lensLeftAnim.snapTo(frame.left) }
                        }
                    }

                    val isOutside = currentY < -30f || currentY > barHeightPx + 30f

                    if (cancelled || isOutside) {
                        isDragging = false
                        dragHighlightedDest = null
                        scope.launch {
                            val resetTarget = paddingPx + resolveCol(selected) * itemWidthPx
                            if (reduceEffects) {
                                lensLeftAnim.snapTo(resetTarget)
                            } else {
                                lensLeftAnim.animateTo(resetTarget, ShellMotion.default.toSpec())
                            }
                        }
                    } else if (dragged) {
                        val release = lensController.up(currentX, lastTime)
                        isDragging = false
                        dragHighlightedDest = null
                        velocity = release.velocity
                        val targetDest = resolveDest(release.targetIndex)
                        if (targetDest != null && targetDest != selected) {
                            currentOnSelect(targetDest)
                        } else {
                            scope.launch {
                                val resetTarget = paddingPx + release.targetIndex * itemWidthPx
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
                        dragHighlightedDest = null
                        val tapIdx = lensController.tapIndex(currentX)
                        val targetDest = tapIdx?.let { resolveDest(it) }
                        if (targetDest != null && targetDest != selected) {
                            currentOnSelect(targetDest)
                        }
                    }
                }
            }
        }

        // Lens stretch calculation
        val k = if (reduceEffects) 0f else min(0.14f, abs(velocity) / 6000f)
        val lensScaleX = 1f + k
        val lensScaleY = 1f - k / 2f
        val lensWidthDp = with(density) { itemWidthPx.toDp() }

        // Root container for items and lens
        Box(
            modifier = Modifier
                .fillMaxSize()
                .then(gestureModifier)
        ) {
            // Lens behind items
            Box(
                modifier = Modifier
                    .offset { IntOffset(lensLeftAnim.value.roundToInt(), lensTopOffsetPx) }
                    .width(lensWidthDp)
                    .height(54.dp)
                    .graphicsLayer {
                        scaleX = lensScaleX
                        scaleY = lensScaleY
                    }
                    .enkShadow(
                        listOf(
                            ShadowLayer(0.dp, 1.dp, 3.dp, 0.dp, material.sh1),
                            ShadowLayer(0.dp, 6.dp, 14.dp, (-4).dp, material.sh2)
                        ),
                        RoundedCornerShape(27.dp)
                    )
                    .background(
                        brush = Brush.verticalGradient(listOf(material.lensA, material.lensB)),
                        shape = RoundedCornerShape(27.dp)
                    )
            ) {
                // Inset top highlight: 0.5 dp lensEdge
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val shape = RoundedCornerShape(27.dp)
                    val outline = shape.createOutline(size, layoutDirection, this)
                    val shapePath = outline.toPath()
                    clipPath(shapePath) {
                        val highlightPx = 0.5.dp.toPx()
                        val translatedHole = Path().apply {
                            addPath(shapePath)
                            asAndroidPath().offset(0f, highlightPx)
                        }
                        val topHighlightPath = Path().apply {
                            op(shapePath, translatedHole, PathOperation.Difference)
                        }
                        drawPath(topHighlightPath, color = material.lensEdge)
                    }
                }
            }

            // Items Row
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(5.dp)
                    .selectableGroup(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                for (dest in dests) {
                    val isSelected = (dest == selected)
                    val isHighlighted = (dest == (dragHighlightedDest ?: selected))
                    val itemColor = if (isHighlighted) colors.t1 else colors.t2
                    val strokePx = with(density) { 1.6.dp.toPx() }

                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .semantics(mergeDescendants = true) {
                                this.role = Role.Tab
                                this.selected = isSelected
                                this.contentDescription = dest.name
                                this.onClick(label = dest.name) {
                                    if (dest != selected) {
                                        currentOnSelect(dest)
                                    }
                                    true
                                }
                            },
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = androidx.compose.foundation.layout.Arrangement.Center
                    ) {
                        // 22 dp line icon
                        Canvas(modifier = Modifier.size(22.dp)) {
                            when (dest) {
                                Dest.Write -> {
                                    // Rail line
                                    drawLine(
                                        color = itemColor,
                                        start = Offset(2f.dp.toPx(), 14f.dp.toPx()),
                                        end = Offset(20f.dp.toPx(), 14f.dp.toPx()),
                                        strokeWidth = strokePx,
                                        cap = StrokeCap.Round
                                    )
                                    // Carriage
                                    drawRoundRect(
                                        color = itemColor,
                                        topLeft = Offset(7f.dp.toPx(), 5f.dp.toPx()),
                                        size = Size(8f.dp.toPx(), 8f.dp.toPx()),
                                        cornerRadius = CornerRadius(1.5f.dp.toPx()),
                                        style = Stroke(width = strokePx)
                                    )
                                    // Strip in carriage
                                    drawLine(
                                        color = colors.ui,
                                        start = Offset(11f.dp.toPx(), 6.5f.dp.toPx()),
                                        end = Offset(11f.dp.toPx(), 11.5f.dp.toPx()),
                                        strokeWidth = strokePx,
                                        cap = StrokeCap.Round
                                    )
                                }
                                Dest.Box -> {
                                    // Drawer rect
                                    drawRoundRect(
                                        color = itemColor,
                                        topLeft = Offset(3f.dp.toPx(), 6f.dp.toPx()),
                                        size = Size(16f.dp.toPx(), 12f.dp.toPx()),
                                        cornerRadius = CornerRadius(3f.dp.toPx()),
                                        style = Stroke(width = strokePx)
                                    )
                                    // Tab line
                                    drawLine(
                                        color = itemColor,
                                        start = Offset(8f.dp.toPx(), 12f.dp.toPx()),
                                        end = Offset(14f.dp.toPx(), 12f.dp.toPx()),
                                        strokeWidth = strokePx,
                                        cap = StrokeCap.Round
                                    )
                                }
                                Dest.Find -> {
                                    // Circle magnifier
                                    drawCircle(
                                        color = itemColor,
                                        radius = 5.5f.dp.toPx(),
                                        center = Offset(9.5f.dp.toPx(), 9.5f.dp.toPx()),
                                        style = Stroke(width = strokePx)
                                    )
                                    // Handle
                                    drawLine(
                                        color = itemColor,
                                        start = Offset(13.5f.dp.toPx(), 13.5f.dp.toPx()),
                                        end = Offset(18.5f.dp.toPx(), 18.5f.dp.toPx()),
                                        strokeWidth = strokePx,
                                        cap = StrokeCap.Round
                                    )
                                }
                                Dest.Setup -> {
                                    // 3 slider lines with 3 rings
                                    drawLine(
                                        color = itemColor,
                                        start = Offset(3f.dp.toPx(), 6f.dp.toPx()),
                                        end = Offset(19f.dp.toPx(), 6f.dp.toPx()),
                                        strokeWidth = strokePx,
                                        cap = StrokeCap.Round
                                    )
                                    drawCircle(
                                        color = itemColor,
                                        radius = 2f.dp.toPx(),
                                        center = Offset(8f.dp.toPx(), 6f.dp.toPx()),
                                        style = Fill
                                    )
                                    drawLine(
                                        color = itemColor,
                                        start = Offset(3f.dp.toPx(), 11f.dp.toPx()),
                                        end = Offset(19f.dp.toPx(), 11f.dp.toPx()),
                                        strokeWidth = strokePx,
                                        cap = StrokeCap.Round
                                    )
                                    drawCircle(
                                        color = itemColor,
                                        radius = 2f.dp.toPx(),
                                        center = Offset(14f.dp.toPx(), 11f.dp.toPx()),
                                        style = Fill
                                    )
                                    drawLine(
                                        color = itemColor,
                                        start = Offset(3f.dp.toPx(), 16f.dp.toPx()),
                                        end = Offset(19f.dp.toPx(), 16f.dp.toPx()),
                                        strokeWidth = strokePx,
                                        cap = StrokeCap.Round
                                    )
                                    drawCircle(
                                        color = itemColor,
                                        radius = 2f.dp.toPx(),
                                        center = Offset(9f.dp.toPx(), 16f.dp.toPx()),
                                        style = Fill
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(3.dp))

                        Text(
                            text = dest.name,
                            fontSize = 10.5.sp,
                            fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                            color = itemColor
                        )
                    }
                }
            }
        }
    }
}

private fun Outline.toPath(): Path = Path().apply {
    when (val outline = this@toPath) {
        is Outline.Rectangle -> addRect(outline.rect)
        is Outline.Rounded -> addRoundRect(outline.roundRect)
        is Outline.Generic -> addPath(outline.path)
    }
}
