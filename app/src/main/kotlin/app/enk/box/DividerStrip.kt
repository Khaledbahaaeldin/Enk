package app.enk.box

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInParent
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.enk.data.DividerEntity
import app.enk.design.motion.ShellMotion
import app.enk.design.shell.GlassLevel
import app.enk.design.shell.GlassSource
import app.enk.design.shell.glass
import app.enk.design.shell.rememberGlassSource
import app.enk.design.theme.LocalShellColors
import app.enk.design.theme.LocalShellMaterial
import kotlin.math.roundToInt

data class ItemBounds(val x: Float, val width: Float)

@Composable
fun DividerStrip(
    dividers: List<DividerEntity>,
    selectedId: Long?,
    onSelect: (Long?) -> Unit,
    onAdd: () -> Unit,
    modifier: Modifier = Modifier,
    glassSource: GlassSource = rememberGlassSource()
) {
    val colors = LocalShellColors.current
    val material = LocalShellMaterial.current
    val density = LocalDensity.current
    val scrollState = rememberScrollState()

    val itemBounds = remember { mutableStateMapOf<Long?, ItemBounds>() }

    val currentSelectedBounds = itemBounds[selectedId] ?: itemBounds[null] ?: ItemBounds(0f, 0f)

    val animatedX by animateFloatAsState(
        targetValue = currentSelectedBounds.x,
        animationSpec = ShellMotion.default.toSpec(),
        label = "lens_x"
    )
    val animatedWidth by animateFloatAsState(
        targetValue = currentSelectedBounds.width,
        animationSpec = ShellMotion.default.toSpec(),
        label = "lens_width"
    )

    val stripShape = RoundedCornerShape(22.dp)
    val lensShape = RoundedCornerShape(18.dp)

    Box(
        modifier = modifier
            .height(44.dp)
            .glass(GlassLevel.Regular, stripShape, glassSource)
            .clip(stripShape)
            .padding(4.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxHeight()
                .horizontalScroll(scrollState)
        ) {
            // Lens behind items
            if (animatedWidth > 0f) {
                Box(
                    modifier = Modifier
                        .offset { IntOffset(animatedX.roundToInt(), 0) }
                        .width(with(density) { animatedWidth.toDp() })
                        .height(36.dp)
                        .clip(lensShape)
                        .background(Brush.verticalGradient(listOf(material.lensA, material.lensB)))
                        .border(0.5.dp, material.lensEdge, lensShape)
                )
            }

            // Items Row
            Row(
                modifier = Modifier.fillMaxHeight(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Unfiled item
                val isUnfiledSelected = selectedId == null
                Box(
                    modifier = Modifier
                        .height(36.dp)
                        .onGloballyPositioned { coordinates ->
                            itemBounds[null] = ItemBounds(
                                coordinates.positionInParent().x,
                                coordinates.size.width.toFloat()
                            )
                        }
                        .clip(lensShape)
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                            onClick = { onSelect(null) }
                        )
                        .padding(horizontal = 15.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Unfiled",
                        color = if (isUnfiledSelected) colors.t1 else colors.t2,
                        fontWeight = if (isUnfiledSelected) FontWeight.SemiBold else FontWeight.Normal,
                        fontSize = 14.sp
                    )
                }

                // Divider items
                for (divider in dividers) {
                    val isSelected = selectedId == divider.id
                    Box(
                        modifier = Modifier
                            .height(36.dp)
                            .onGloballyPositioned { coordinates ->
                                itemBounds[divider.id] = ItemBounds(
                                    coordinates.positionInParent().x,
                                    coordinates.size.width.toFloat()
                                )
                            }
                            .clip(lensShape)
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null,
                                onClick = { onSelect(divider.id) }
                            )
                            .padding(horizontal = 15.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = divider.name,
                            color = if (isSelected) colors.t1 else colors.t2,
                            fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                            fontSize = 14.sp
                        )
                    }
                }

                // "+" button
                Box(
                    modifier = Modifier
                        .height(36.dp)
                        .width(38.dp)
                        .testTag("add_divider_button")
                        .clip(lensShape)
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                            onClick = onAdd
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "+",
                        color = colors.t2,
                        fontWeight = FontWeight.Light,
                        fontSize = 20.sp
                    )
                }
            }
        }
    }
}
