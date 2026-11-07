package app.enk.design.golden

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.unit.dp
import app.cash.paparazzi.Paparazzi
import app.enk.design.EnkMaterials
import app.enk.design.EnkShadows
import app.enk.design.enkShadow
import org.junit.Rule
import org.junit.Test

class MaterialsGoldenTest {

    @get:Rule
    val paparazzi = Paparazzi(
        maxPercentDifference = 2.0
    )

    @Test
    fun materialsSnapshot() {
        paparazzi.snapshot {
            MaterialsSample()
        }
    }
}

@Composable
fun MaterialsSample() {
    Box(
        modifier = Modifier
            .size(400.dp, 820.dp)
            .background(EnkMaterials.chassisDay)
    ) {
        // Rail rod: height 12.dp at railY = 508.dp (top = 502.dp)
        Box(
            modifier = Modifier
                .offset(x = 0.dp, y = 502.dp)
                .fillMaxWidth()
                .height(12.dp)
                .enkShadow(EnkShadows.rail, RectangleShape)
                .background(EnkMaterials.railSteel)
        )

        // Left bracket: width 12.dp, height 24.dp, top at 496.dp
        Box(
            modifier = Modifier
                .offset(x = 0.dp, y = 496.dp)
                .size(12.dp, 24.dp)
                .background(EnkMaterials.bracketSteel, RoundedCornerShape(3.dp))
        )

        // Right bracket
        Box(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .offset(x = 0.dp, y = 496.dp)
                .size(12.dp, 24.dp)
                .background(EnkMaterials.bracketSteel, RoundedCornerShape(3.dp))
        )

        // Carriage body placeholder: 54x36 dp, center x = 200 dp (left = 173 dp), top = 491 dp
        Box(
            modifier = Modifier
                .offset(x = 173.dp, y = 491.dp)
                .size(54.dp, 36.dp)
                .enkShadow(EnkShadows.cap, RoundedCornerShape(9.dp))
                .background(EnkMaterials.graphiteCap, RoundedCornerShape(9.dp))
        ) {
            // Top collar (5 dp band)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(5.dp)
                    .background(
                        EnkMaterials.collarTop,
                        RoundedCornerShape(topStart = 9.dp, topEnd = 9.dp)
                    )
            )
            // Bottom collar (5 dp band)
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .height(5.dp)
                    .background(
                        EnkMaterials.collarBottom,
                        RoundedCornerShape(bottomStart = 9.dp, bottomEnd = 9.dp)
                    )
            )
        }

        // Panel face: left/right inset 26 dp (width 348 dp), bottom inset 30 dp (top = 620 dp, height 170 dp)
        Box(
            modifier = Modifier
                .offset(x = 26.dp, y = 620.dp)
                .size(348.dp, 170.dp)
                .enkShadow(EnkShadows.panel, RoundedCornerShape(16.dp))
                .background(EnkMaterials.panelFace, RoundedCornerShape(16.dp))
        ) {
            // Inner well: inset 4 dp, corner radius 12 dp
            Box(
                modifier = Modifier
                    .offset(x = 4.dp, y = 4.dp)
                    .size(340.dp, 162.dp)
                    .enkShadow(EnkShadows.panelWell, RoundedCornerShape(12.dp))
            )
        }
    }
}
