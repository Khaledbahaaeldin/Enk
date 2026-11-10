package app.enk.box

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.enk.design.shell.GlassLevel
import app.enk.design.shell.GlassSource
import app.enk.design.shell.glass
import app.enk.design.shell.rememberGlassSource
import app.enk.design.theme.LocalShellColors

@Composable
fun DeleteForeverAlert(
    count: Int,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    isDeleting: Boolean = false,
    glassSource: GlassSource = rememberGlassSource()
) {
    val colors = LocalShellColors.current
    val alertShape = RoundedCornerShape(22.dp)

    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        // Scrim
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(colors.thick.copy(alpha = 0.4f))
                .clickable(
                    enabled = !isDeleting,
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = onDismiss
                )
        )

        // Alert box
        Box(
            modifier = Modifier
                .width(272.dp)
                .glass(GlassLevel.Thick, alertShape, glassSource)
                .clip(alertShape)
        ) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Spacer(modifier = Modifier.height(20.dp))

                Text(
                    text = "Delete forever",
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold,
                    color = colors.t1,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(8.dp))

                val noteWord = if (count == 1) "note" else "notes"
                Text(
                    text = "$count $noteWord will be erased. This can’t be undone.",
                    fontSize = 13.sp,
                    lineHeight = 18.sp,
                    color = colors.t2,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(horizontal = 18.dp)
                )

                Spacer(modifier = Modifier.height(18.dp))

                HorizontalDivider(thickness = 0.5.dp, color = colors.hair)

                // Buttons row
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(46.dp)
                ) {
                    // Keep button
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .clickable(
                                enabled = !isDeleting,
                                onClick = onDismiss
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Keep",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Medium,
                            color = colors.t1
                        )
                    }

                    VerticalDivider(thickness = 0.5.dp, color = colors.hair)

                    // Delete button
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .clickable(
                                enabled = !isDeleting,
                                onClick = onConfirm
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Delete",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = if (isDeleting) colors.danger.copy(alpha = 0.5f) else colors.danger
                        )
                    }
                }
            }
        }
    }
}
