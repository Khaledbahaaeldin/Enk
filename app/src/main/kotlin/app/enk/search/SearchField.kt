package app.enk.search

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.enk.design.effects.LocalEffects
import app.enk.design.effects.ResolvedEffects
import app.enk.design.glass.GlassSpecs
import app.enk.design.glass.LiquidSpecs
import app.enk.design.glass.glassSurface
import app.enk.design.glass.liquidPress
import app.enk.design.glass.rememberLiquidPressState
import app.enk.design.motion.ShellMotion
import app.enk.design.shell.GlassSource
import app.enk.design.shell.LocalReduceEffects
import app.enk.design.shell.rememberGlassSource
import app.enk.design.theme.LocalShellColors
import app.enk.design.theme.LocalShellMaterial

/**
 * Search field per DS 12.8:
 * Height 44 dp, radius 22 dp, fill field, 16 dp horizontal padding,
 * an 18 dp magnifier icon in t2, and a 16 sp text field with caret in ui.
 * Placeholder: "Search your notes".
 */
@Composable
fun SearchField(
    query: String,
    onQueryChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    placeholder: String = "Search your notes",
    onSearch: () -> Unit = {},
    glassSource: GlassSource = rememberGlassSource()
) {
    val colors = LocalShellColors.current
    val material = LocalShellMaterial.current
    val level = LocalEffects.current
    val reduceEffects = LocalReduceEffects.current
    val isLight = level == ResolvedEffects.Light || reduceEffects

    var isFocused by remember { mutableStateOf(false) }

    val baseHeight = 44.dp
    val targetHeight = if (isFocused && !isLight) baseHeight + 4.dp else baseHeight
    val animatedHeight by animateDpAsState(
        targetValue = targetHeight,
        animationSpec = spring(
            dampingRatio = ShellMotion.default.damping,
            stiffness = ShellMotion.default.stiffness
        ),
        label = "search_field_height"
    )

    val shape = RoundedCornerShape(22.dp)
    val pressState = rememberLiquidPressState()

    Box(
        modifier = modifier
            .testTag("search_field")
            .fillMaxWidth()
            .height(animatedHeight)
            .liquidPress(pressState)
            .glassSurface(
                spec = GlassSpecs.Field,
                liquid = LiquidSpecs.Button,
                shape = shape,
                source = glassSource,
                press = pressState
            )
            .padding(horizontal = 16.dp),
        contentAlignment = Alignment.CenterStart
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 18 dp magnifier icon in t2
            val iconColor = colors.t2
            Canvas(modifier = Modifier.size(18.dp)) {
                val strokeWidth = 1.8.dp.toPx()
                val r = 5.5.dp.toPx()
                val center = Offset(7.5.dp.toPx(), 7.5.dp.toPx())
                drawCircle(
                    color = iconColor,
                    radius = r,
                    center = center,
                    style = Stroke(width = strokeWidth)
                )
                val handleStart = Offset(center.x + r * 0.7071f, center.y + r * 0.7071f)
                val handleEnd = Offset(15.5.dp.toPx(), 15.5.dp.toPx())
                drawLine(
                    color = iconColor,
                    start = handleStart,
                    end = handleEnd,
                    strokeWidth = strokeWidth,
                    cap = StrokeCap.Round
                )
            }

            Spacer(modifier = Modifier.width(10.dp))

            BasicTextField(
                value = query,
                onValueChange = onQueryChange,
                modifier = Modifier
                    .fillMaxWidth()
                    .onFocusChanged { isFocused = it.isFocused },
                singleLine = true,
                textStyle = TextStyle(
                    color = colors.t1,
                    fontSize = 16.sp
                ),
                cursorBrush = SolidColor(colors.ui),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                keyboardActions = KeyboardActions(onSearch = { onSearch() }),
                decorationBox = { innerTextField ->
                    Box(contentAlignment = Alignment.CenterStart) {
                        if (query.isEmpty()) {
                            Text(
                                text = placeholder,
                                color = colors.t2,
                                fontSize = 16.sp
                            )
                        }
                        innerTextField()
                    }
                }
            )
        }
    }
}
