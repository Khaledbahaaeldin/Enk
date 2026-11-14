package app.enk.design.golden

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import app.enk.design.R
import app.enk.design.logo.EnkSymbol
import app.enk.design.logo.EnkWordmark
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = "w400dp-h533dp-xxhdpi")
class LogoGoldenTest {

    @get:Rule
    val snapshots = EnkSnapshots(
        testClass = LogoGoldenTest::class.java,
        screenWidthDp = 400,
        screenHeightDp = 533
    )

    @Test
    fun foregroundMasks() {
        snapshots.snapshot {
            ForegroundMasksScene()
        }
    }

    @Test
    fun monochromeTints() {
        snapshots.snapshot {
            MonochromeTintsScene()
        }
    }

    @Test
    fun wordmarkVariants() {
        snapshots.snapshot {
            WordmarkVariantsScene()
        }
    }

    @Test
    fun symbolSizes() {
        snapshots.snapshot {
            SymbolSizesScene()
        }
    }
}

@Composable
private fun ForegroundMasksScene() {
    val tileBrush = Brush.verticalGradient(
        0.0f to Color(0xFF3A3835),
        1.0f to Color(0xFF1D1C1B)
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0F0E10))
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(28.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Circle mask (72 dp visible diameter centered on tile)
            Box(
                modifier = Modifier
                    .size(80.dp)
                    .clip(CircleShape)
                    .background(tileBrush),
                contentAlignment = Alignment.Center
            ) {
                EnkSymbol(modifier = Modifier.size(108.dp))
            }

            // Squircle mask (radius 20 dp)
            Box(
                modifier = Modifier
                    .size(84.dp)
                    .clip(RoundedCornerShape(20.dp))
                    .background(tileBrush),
                contentAlignment = Alignment.Center
            ) {
                EnkSymbol(modifier = Modifier.size(108.dp))
            }

            // Rounded-square mask (radius 14 dp)
            Box(
                modifier = Modifier
                    .size(80.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(tileBrush),
                contentAlignment = Alignment.Center
            ) {
                EnkSymbol(modifier = Modifier.size(104.dp))
            }
        }
    }
}

@Composable
private fun MonochromeTintsScene() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0F0E10))
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(32.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Apricot tint on dark surface
            Box(
                modifier = Modifier
                    .size(96.dp)
                    .clip(RoundedCornerShape(22.dp))
                    .background(Color(0xFF1C120F)),
                contentAlignment = Alignment.Center
            ) {
                Image(
                    painter = painterResource(R.drawable.ic_launcher_monochrome),
                    contentDescription = null,
                    modifier = Modifier.size(84.dp),
                    colorFilter = ColorFilter.tint(Color(0xFFE2725B))
                )
            }

            // Sky tint on light surface
            Box(
                modifier = Modifier
                    .size(96.dp)
                    .clip(RoundedCornerShape(22.dp))
                    .background(Color(0xFFFFF6D4)),
                contentAlignment = Alignment.Center
            ) {
                Image(
                    painter = painterResource(R.drawable.ic_launcher_monochrome),
                    contentDescription = null,
                    modifier = Modifier.size(84.dp),
                    colorFilter = ColorFilter.tint(Color(0xFF5E92B2))
                )
            }
        }
    }
}

@Composable
private fun WordmarkVariantsScene() {
    val tileBrush = Brush.verticalGradient(
        0.0f to Color(0xFF3A3835),
        1.0f to Color(0xFF1D1C1B)
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0F0E10))
            .padding(32.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Dark lockup
        Box(
            modifier = Modifier
                .width(360.dp)
                .clip(RoundedCornerShape(18.dp))
                .background(Color(0xFF1A1918))
                .padding(20.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(68.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(tileBrush),
                    contentAlignment = Alignment.Center
                ) {
                    EnkSymbol(modifier = Modifier.size(88.dp))
                }
                EnkWordmark(
                    modifier = Modifier.width(160.dp),
                    onDark = true
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Light lockup
        Box(
            modifier = Modifier
                .width(360.dp)
                .clip(RoundedCornerShape(18.dp))
                .background(Color(0xFFF2ECDF))
                .padding(20.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(68.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(tileBrush),
                    contentAlignment = Alignment.Center
                ) {
                    EnkSymbol(modifier = Modifier.size(88.dp))
                }
                EnkWordmark(
                    modifier = Modifier.width(160.dp),
                    onDark = false
                )
            }
        }
    }
}

@Composable
private fun SymbolSizesScene() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF1A1918))
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(28.dp),
            verticalAlignment = Alignment.Bottom
        ) {
            EnkSymbol(modifier = Modifier.size(16.dp))
            EnkSymbol(modifier = Modifier.size(24.dp))
            EnkSymbol(modifier = Modifier.size(32.dp))
            EnkSymbol(modifier = Modifier.size(48.dp))
        }
    }
}
