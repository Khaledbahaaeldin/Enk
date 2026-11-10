package app.enk.design.shell

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.enk.design.theme.LocalShellColors

/**
 * Grouped section per DS 12.8 / 12.10:
 * Header: uppercase 12.5 sp weight 600, letter spacing +0.04em, t2, padding 22 top, 28 sides, 8 bottom.
 * Surface: g1 background, radius 16 dp, 0.5 dp hair border.
 * Footer: 12.5 sp / 17 sp line height, t2, padding 4 top, 28 sides.
 */
@Composable
fun GroupedSection(
    title: String,
    modifier: Modifier = Modifier,
    footer: String? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    val colors = LocalShellColors.current
    val shape = RoundedCornerShape(16.dp)

    Column(modifier = modifier.fillMaxWidth()) {
        // Section Header
        Text(
            text = title.uppercase(),
            fontSize = 12.5.sp,
            fontWeight = FontWeight.SemiBold,
            letterSpacing = 0.5.sp,
            color = colors.t2,
            modifier = Modifier.padding(start = 28.dp, end = 28.dp, top = 22.dp, bottom = 8.dp)
        )

        // Opaque grouped surface (g1)
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .clip(shape)
                .background(colors.g1)
                .border(0.5.dp, colors.hair, shape)
        ) {
            content()
        }

        // Section Footer
        if (footer != null) {
            Text(
                text = footer,
                fontSize = 12.5.sp,
                lineHeight = 17.sp,
                color = colors.t2,
                modifier = Modifier.padding(start = 28.dp, end = 28.dp, top = 6.dp)
            )
        }
    }
}

/**
 * Row inside a grouped section. Meets 48 dp minimum touch height.
 */
@Composable
fun GroupedRow(
    title: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    onClick: (() -> Unit)? = null,
    trailing: @Composable () -> Unit = {}
) {
    val colors = LocalShellColors.current

    Row(
        modifier = modifier
            .fillMaxWidth()
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f, fill = false)) {
            Text(
                text = title,
                fontSize = 16.sp,
                color = colors.t1
            )
            if (subtitle != null) {
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = subtitle,
                    fontSize = 13.sp,
                    color = colors.t2
                )
            }
        }

        Box(
            modifier = Modifier.padding(start = 12.dp),
            contentAlignment = Alignment.CenterEnd
        ) {
            trailing()
        }
    }
}

/**
 * 0.5 dp hair separator between rows.
 */
@Composable
fun GroupedDivider(
    modifier: Modifier = Modifier
) {
    val colors = LocalShellColors.current
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(0.5.dp)
            .padding(start = 16.dp)
            .background(colors.hair)
    )
}
