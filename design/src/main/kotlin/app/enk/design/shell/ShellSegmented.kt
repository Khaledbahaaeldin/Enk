package app.enk.design.shell

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.enk.design.motion.ShellMotion
import app.enk.design.theme.LocalShellColors
import app.enk.design.theme.LocalShellMaterial
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
 * Shell segmented control per DS 12.8:
 * Pill track height 36 dp, fill field. Draggable lens with spring physics.
 * Touch height is at least 48 dp.
 */
@Composable
fun ShellSegmented(
    items: List<String>,
    selected: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = LocalShellColors.current
    val material = LocalShellMaterial.current
    val density = LocalDensity.current
    val currentOnSelect by rememberUpdatedState(onSelect)

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

    val shape = RoundedCornerShape(18.dp)

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(ShellSegmentedMath.touchHeightDp.dp),
        contentAlignment = Alignment.Center
    ) {
        // Control track (36 dp)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(ShellSegmentedMath.controlHeightDp.dp)
                .clip(shape)
                .background(material.field)
                .onSizeChanged { controlWidthPx = it.width }
                .pointerInput(controlWidthPx, items.size) {
                    awaitEachGesture {
                        val down = awaitFirstDown(requireUnconsumed = false)
                        val centers = List(items.size) { i -> (i + 0.5f) * itemWidthPx }
                        isDragging = true
                        dragXPx = down.position.x

                        while (true) {
                            val event = awaitPointerEvent(PointerEventPass.Main)
                            val change = event.changes.firstOrNull { it.id == down.id } ?: break
                            if (!change.pressed) {
                                isDragging = false
                                val nearest = ShellSegmentedMath.nearestItem(dragXPx, centers)
                                currentOnSelect(nearest)
                                break
                            }
                            dragXPx = change.position.x
                        }
                    }
                }
        ) {
            // Lens behind selected item
            if (controlWidthPx > 0 && items.isNotEmpty()) {
                val itemWidthDp = with(density) { itemWidthPx.toDp() }
                val lensShape = RoundedCornerShape(18.dp)
                Box(
                    modifier = Modifier
                        .offset { IntOffset(animatedXPx.roundToInt(), 0) }
                        .width(itemWidthDp)
                        .fillMaxHeight()
                        .padding(2.dp)
                        .clip(lensShape)
                        .background(material.lensA)
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
                            .clickable { onSelect(index) },
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
