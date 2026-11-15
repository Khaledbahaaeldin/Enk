package app.enk.design.shell

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.enk.design.glass.GlassInteraction
import app.enk.design.glass.GlassLensMath
import app.enk.design.glass.GlassSpecs
import app.enk.design.glass.glassSurface
import app.enk.design.motion.ShellMotion
import app.enk.design.theme.LocalShellColors
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.roundToInt

object ShellSegmentedMath {
    const val touchHeightDp = 48f
    const val controlHeightDp = 36f

    fun nearestItem(x: Float, itemCenters: List<Float>): Int {
        if (itemCenters.isEmpty()) return 0
        var bestIndex = 0
        var bestDist = abs(x - itemCenters[0])
        for (i in 1 until itemCenters.size) {
            val dist = abs(x - itemCenters[i])
            if (dist < bestDist) {
                bestDist = dist
                bestIndex = i
            }
        }
        return bestIndex
    }
}

/**
 * Shell segmented control on the glass kit:
 * Track with SegmentedBody, draggable lens with SegmentedLens.
 * SEGMENTED_PRESS = 1.08f, stretch with velocity, specular angle following touch.
 * Touch height is at least 48 dp.
 */
@Composable
fun ShellSegmented(
    items: List<String>,
    selected: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier,
    glassSource: GlassSource = rememberGlassSource()
) {
    val colors = LocalShellColors.current
    val density = LocalDensity.current
    val reduceEffects = LocalReduceEffects.current
    val currentOnSelect by rememberUpdatedState(onSelect)
    val scope = rememberCoroutineScope()

    var controlWidthPx by remember { mutableIntStateOf(0) }
    var isDragging by remember { mutableStateOf(false) }
    var dragXPx by remember { mutableStateOf(0f) }

    val itemCount = maxOf(1, items.size)
    val itemWidthPx = if (controlWidthPx > 0) controlWidthPx.toFloat() / itemCount else 0f

    val targetXPx = if (isDragging) {
        dragXPx - itemWidthPx / 2f
    } else {
        selected.coerceIn(0, items.size - 1) * itemWidthPx
    }

    val animatedXPx by animateFloatAsState(
        targetValue = targetXPx,
        animationSpec = spring(
            dampingRatio = ShellMotion.default.damping,
            stiffness = ShellMotion.default.stiffness
        ),
        label = "segmented_lens_x"
    )

    val trackShape = RoundedCornerShape(18.dp)
    val lensShape = RoundedCornerShape(16.dp)

    val lensInteraction = remember { GlassInteraction() }
    val pressScaleAnim = remember { Animatable(1f) }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(ShellSegmentedMath.touchHeightDp.dp)
            .testTag("shell_segmented"),
        contentAlignment = Alignment.Center
    ) {
        // Control track (36 dp) with SegmentedBody
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(ShellSegmentedMath.controlHeightDp.dp)
                .glassSurface(
                    spec = GlassSpecs.SegmentedBody,
                    shape = trackShape,
                    source = glassSource
                )
                .onSizeChanged { controlWidthPx = it.width }
                .pointerInput(controlWidthPx, items.size, reduceEffects) {
                    awaitEachGesture {
                        val down = awaitFirstDown(requireUnconsumed = false)
                        val centers = List(items.size) { i -> (i + 0.5f) * itemWidthPx }
                        isDragging = true
                        dragXPx = down.position.x

                        var lastX = down.position.x
                        var lastTime = System.currentTimeMillis()

                        val targetPress = if (reduceEffects) 1f else GlassLensMath.SEGMENTED_PRESS
                        scope.launch {
                            pressScaleAnim.animateTo(targetPress, ShellMotion.press.toSpec())
                        }

                        while (true) {
                            val event = awaitPointerEvent(PointerEventPass.Main)
                            val change = event.changes.firstOrNull { it.id == down.id } ?: break

                            val now = System.currentTimeMillis()
                            val dtSec = maxOf(0.001f, (now - lastTime) / 1000f)
                            val vx = (change.position.x - lastX) / dtSec / density.density
                            lastX = change.position.x
                            lastTime = now

                            val (sx, sy) = if (reduceEffects) Pair(1f, 1f) else GlassLensMath.stretch(vx)
                            lensInteraction.stretchX = sx
                            lensInteraction.stretchY = sy
                            val hPx = with(density) { ShellSegmentedMath.controlHeightDp.dp.toPx() }
                            lensInteraction.specularAngleDeg = GlassLensMath.specularAngleDeg(
                                centreX = itemWidthPx / 2f,
                                centreY = hPx / 2f,
                                touchX = change.position.x - animatedXPx,
                                touchY = change.position.y
                            )

                            if (!change.pressed) {
                                isDragging = false
                                lensInteraction.stretchX = 1f
                                lensInteraction.stretchY = 1f
                                lensInteraction.specularAngleDeg = null
                                scope.launch {
                                    pressScaleAnim.animateTo(1f, ShellMotion.release.toSpec())
                                }
                                val nearest = ShellSegmentedMath.nearestItem(dragXPx, centers)
                                currentOnSelect(nearest)
                                break
                            }
                            dragXPx = change.position.x
                        }
                    }
                }
        ) {
            lensInteraction.pressScale = pressScaleAnim.value

            // Lens behind selected item with SegmentedLens
            if (controlWidthPx > 0 && items.isNotEmpty()) {
                val itemWidthDp = with(density) { itemWidthPx.toDp() }
                Box(
                    modifier = Modifier
                        .offset { IntOffset(animatedXPx.roundToInt(), 0) }
                        .width(itemWidthDp)
                        .fillMaxHeight()
                        .padding(2.dp)
                        .glassSurface(
                            spec = GlassSpecs.SegmentedLens,
                            shape = lensShape,
                            source = glassSource,
                            interaction = lensInteraction
                        )
                )
            }

            // Labels row
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                items.forEachIndexed { index, label ->
                    val isSelected = index == selected
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .testTag("shell_segment_$index")
                            .semantics {
                                role = Role.RadioButton
                                this.selected = isSelected
                                onClick {
                                    currentOnSelect(index)
                                    true
                                }
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = label,
                            fontSize = 13.sp,
                            fontWeight = if (isSelected) FontWeight.Medium else FontWeight.Normal,
                            color = if (isSelected) colors.t1 else colors.t2,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        }
    }
}
