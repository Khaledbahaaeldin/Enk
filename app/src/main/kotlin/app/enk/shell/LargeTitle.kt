package app.enk.shell

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.enk.design.LocalMonoFont
import app.enk.design.theme.LocalShellColors

@Composable
fun LargeTitle(
    title: String,
    meta: String?,
    modifier: Modifier = Modifier
) {
    val colors = LocalShellColors.current
    val monoFont = LocalMonoFont.current

    Row(
        modifier = modifier
            .semantics(mergeDescendants = true) {
                testTag = "large_title"
            }
            .fillMaxWidth()
            .padding(top = 70.dp, start = 20.dp, end = 20.dp, bottom = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Bottom
    ) {
        Text(
            text = title,
            fontSize = 34.sp,
            fontWeight = FontWeight.Bold,
            color = colors.t1,
            modifier = Modifier.weight(1f, fill = false)
        )
        if (meta != null) {
            Text(
                text = meta,
                fontFamily = monoFont,
                fontSize = 12.sp,
                color = colors.t3,
                modifier = Modifier.padding(bottom = 6.dp)
            )
        }
    }
}
