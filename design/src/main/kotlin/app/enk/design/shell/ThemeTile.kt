package app.enk.design.shell

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.enk.design.theme.LocalShellColors
import app.enk.design.theme.ThemeId

/**
 * Theme tile per DS 12.8 / 12.10:
 * 62 dp tall swatch with radius 16 dp, 0.5 dp hair outline.
 * Label (12.5 sp weight 500) below.
 * When selected: scale 1.03, 2 dp ring in g1 + 1.5 dp ring in ui, and a 14 x 3 dp index mark.
 */
@Composable
fun ThemeTile(
    theme: ThemeId,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = LocalShellColors.current
    val shape = RoundedCornerShape(16.dp)

    val scale by animateFloatAsState(
        targetValue = if (selected) 1.03f else 1.0f,
        animationSpec = tween(200),
        label = "theme_tile_scale"
    )

    val indexMarkScaleX by animateFloatAsState(
        targetValue = if (selected) 1.0f else 0.0f,
        animationSpec = tween(200),
        label = "theme_tile_index_mark_scale"
    )

    val swatchBrush = when (theme) {
        ThemeId.Graphite -> Brush.verticalGradient(
            0.0f to Color(0xFF161514),
            0.18f to Color(0xFF161514),
            1.0f to Color(0xFFF3EEE3)
        )
        ThemeId.Apricot -> Brush.verticalGradient(
            0.0f to Color(0xFF1C120F),
            0.18f to Color(0xFF4E2C23),
            1.0f to Color(0xFFFFDAB9)
        )
        ThemeId.Sky -> Brush.verticalGradient(
            0.0f to Color(0xFF1B1413),
            0.18f to Color(0xFF432F2E),
            1.0f to Color(0xFFFFF0B5)
        )
        ThemeId.Slate -> Brush.verticalGradient(
            0.0f to Color(0xFF0D0F11),
            0.18f to Color(0xFF1A1F24),
            1.0f to Color(0xFFE5E4E2)
        )
        ThemeId.Mono -> Brush.verticalGradient(
            0.0f to Color(0xFF0B0B0B),
            0.18f to Color(0xFF1E1E1E),
            1.0f to Color(0xFFF2F2F2)
        )
        ThemeId.Dynamic -> Brush.linearGradient(
            listOf(
                Color(0xFF2B3A42),
                Color(0xFF5B7E91),
                Color(0xFFE0EAF0)
            )
        )
    }

    Column(
        modifier = modifier
            .testTag("theme_tile_${theme.name.lowercase()}")
            .clickable(onClick = onClick),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Swatch container
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(62.dp)
                .graphicsLayer {
                    scaleX = scale
                    scaleY = scale
                }
                .then(
                    if (selected) {
                        Modifier
                            .border(3.5.dp, colors.ui, shape)
                            .border(2.dp, colors.g1, shape)
                    } else {
                        Modifier.border(0.5.dp, colors.hair, shape)
                    }
                )
                .clip(shape)
                .background(swatchBrush)
        )

        Spacer(modifier = Modifier.height(6.dp))

        // Label
        Text(
            text = theme.name,
            fontSize = 12.5.sp,
            fontWeight = FontWeight.Medium,
            color = if (selected) colors.t1 else colors.t2,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(3.dp))

        // Index mark: 14 x 3 dp in ui
        Box(
            modifier = Modifier
                .testTag("theme_tile_${theme.name.lowercase()}_index_mark")
                .width(14.dp)
                .height(3.dp)
                .graphicsLayer {
                    scaleX = indexMarkScaleX
                    scaleY = 1.0f
                }
                .clip(RoundedCornerShape(2.dp))
                .background(if (selected) colors.ui else Color.Transparent)
        )
    }
}
