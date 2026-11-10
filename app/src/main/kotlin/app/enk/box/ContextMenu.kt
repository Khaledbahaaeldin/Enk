package app.enk.box

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.enk.design.shell.GlassLevel
import app.enk.design.shell.GlassSource
import app.enk.design.shell.glass
import app.enk.design.shell.rememberGlassSource
import app.enk.design.theme.LocalShellColors
import kotlin.math.roundToInt

@Composable
fun ContextMenu(
    cardBounds: Rect?,
    onMoveToDivider: () -> Unit,
    onMoveToTrash: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    glassSource: GlassSource = rememberGlassSource()
) {
    val colors = LocalShellColors.current
    val density = LocalDensity.current
    val config = LocalConfiguration.current

    val screenHeightDp = config.screenHeightDp.dp
    val menuWidth = 232.dp
    val menuHeight = 92.dp

    // Calculate menu position relative to root
    val (menuLeftDp, menuTopDp) = remember(cardBounds, screenHeightDp) {
        if (cardBounds == null) {
            20.dp to 200.dp
        } else {
            val cardLeftDp = with(density) { cardBounds.left.toDp() }
            val cardBottomDp = with(density) { cardBounds.bottom.toDp() }
            val cardTopDp = with(density) { cardBounds.top.toDp() }

            val standardTop = cardBottomDp + 10.dp
            val top = if (standardTop + menuHeight > screenHeightDp - 60.dp) {
                // Flip above card
                cardTopDp - 10.dp - menuHeight
            } else {
                standardTop
            }
            cardLeftDp to top
        }
    }

    val menuShape = RoundedCornerShape(16.dp)

    Box(
        modifier = modifier
            .fillMaxSize()
    ) {
        // Scrim
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(colors.thick.copy(alpha = 0.4f))
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = onDismiss
                )
        )

        // Menu container
        Box(
            modifier = Modifier
                .offset {
                    IntOffset(
                        with(density) { menuLeftDp.toPx() }.roundToInt(),
                        with(density) { menuTopDp.toPx() }.roundToInt()
                    )
                }
                .width(menuWidth)
                .testTag("context_menu")
                .glass(GlassLevel.Thick, menuShape, glassSource)
                .clip(menuShape)
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                // Move to divider row
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(46.dp)
                        .clickable(onClick = onMoveToDivider)
                        .padding(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Move to divider",
                        color = colors.t1,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        text = "›",
                        color = colors.t3,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                HorizontalDivider(thickness = 0.5.dp, color = colors.hair)

                // Move to trash row
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(46.dp)
                        .clickable(onClick = onMoveToTrash)
                        .padding(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.Start,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Move to trash",
                        color = colors.danger,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }
    }
}
