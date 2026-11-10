package app.enk.search

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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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
    onSearch: () -> Unit = {}
) {
    val colors = LocalShellColors.current
    val material = LocalShellMaterial.current
    val shape = RoundedCornerShape(22.dp)

    Box(
        modifier = modifier
            .testTag("search_field")
            .fillMaxWidth()
            .height(44.dp)
            .clip(shape)
            .background(material.field)
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
                modifier = Modifier.fillMaxWidth(),
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
