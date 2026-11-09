package app.enk.writer

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import app.enk.design.EnkMaterials
import app.enk.design.EnkShadows
import app.enk.design.enkShadow

data class RailGeometry(
    val rodHeightDp: Float,
    val rodTopDp: Float,
    val bracketHeightDp: Float,
    val bracketWidthDp: Float,
    val bracketTopDp: Float,
    val bracketCornerRadiusDp: Float,
) {
    companion object {
        fun compute(railYDp: Float) = RailGeometry(
            rodHeightDp = 12f,
            rodTopDp = railYDp - 6f,
            bracketHeightDp = 24f,
            bracketWidthDp = 12f,
            bracketTopDp = railYDp - 12f,
            bracketCornerRadiusDp = 3f,
        )
    }
}

@Composable
fun Rail(railYDp: Float, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier.fillMaxSize()
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .offset(y = (railYDp - 12f).dp)
                .height(24.dp)
        ) {
            // Rod: height 12 dp at y = 6 dp (centered vertically at railYDp)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(12.dp)
                    .offset(y = 6.dp)
                    .testTag("rail_rod")
                    .enkShadow(EnkShadows.rail, RectangleShape)
                    .background(EnkMaterials.railSteel)
            )

            // Left bracket: width 12 dp, height 24 dp, top at railYDp - 12
            Box(
                modifier = Modifier
                    .size(width = 12.dp, height = 24.dp)
                    .align(Alignment.TopStart)
                    .background(EnkMaterials.bracketSteel, RoundedCornerShape(3.dp))
            )

            // Right bracket: width 12 dp, height 24 dp, top at railYDp - 12
            Box(
                modifier = Modifier
                    .size(width = 12.dp, height = 24.dp)
                    .align(Alignment.TopEnd)
                    .background(EnkMaterials.bracketSteel, RoundedCornerShape(3.dp))
            )
        }
    }
}
