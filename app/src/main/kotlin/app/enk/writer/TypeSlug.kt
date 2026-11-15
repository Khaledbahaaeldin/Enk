package app.enk.writer

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import app.enk.design.EnkMaterials
import app.enk.design.EnkType
import app.enk.design.LocalMonoFont
import kotlin.math.roundToInt

fun filterSlugChar(char: Char?): Char? =
    if (char == null || char.isWhitespace() || char.isISOControl()) null else char

@Composable
fun TypeSlug(
    slugChar: Char?,
    slugFrame: () -> StrikeFrame?,
    night: Boolean,
    modifier: Modifier = Modifier
) {
    val slugShape = RoundedCornerShape(topStart = 2.dp, topEnd = 2.dp, bottomEnd = 0.dp, bottomStart = 0.dp)
    val filteredChar = filterSlugChar(slugChar)
    val monoFont = LocalMonoFont.current

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
            .offset {
                val riseDp = slugFrame()?.slugRiseDp ?: 0f
                IntOffset(0, -(riseDp * density).roundToInt())
            }
            .testTag("type_slug")
    ) {
        Box(
            modifier = Modifier
                .size(width = 14.dp, height = 12.dp)
                .background(EnkMaterials.slugSteel, slugShape)
                .then(
                    if (night) Modifier.border(1.dp, Color.Black.copy(alpha = 0.25f), slugShape) else Modifier
                ),
            contentAlignment = Alignment.Center
        ) {
            if (filteredChar != null) {
                Text(
                    text = filteredChar.toString(),
                    style = EnkType.slug.copy(fontFamily = monoFont)
                )
            }
        }
        Box(
            modifier = Modifier
                .size(width = 2.4.dp, height = 12.dp)
                .background(EnkMaterials.slugSteel)
                .background(Color.Black.copy(alpha = 0.12f))
        )
    }
}
