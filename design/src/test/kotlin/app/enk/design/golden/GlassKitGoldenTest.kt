package app.enk.design.golden

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.enk.design.effects.LocalEffects
import app.enk.design.effects.ResolvedEffects
import app.enk.design.glass.GlassSpecs
import app.enk.design.glass.glassSurface
import app.enk.design.shell.LocalReduceEffects
import app.enk.design.shell.rememberGlassSource
import app.enk.design.theme.EnkShellTheme
import app.enk.design.theme.LocalShellColors
import app.enk.design.theme.ThemeId
import app.enk.design.theme.ThemeMode
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class GlassKitGoldenTest {

    @get:Rule
    val snapshots = EnkSnapshots(GlassKitGoldenTest::class.java)

    @Test
    fun graphiteLight() {
        snapshots.snapshot {
            CompositionLocalProvider(
                LocalEffects provides ResolvedEffects.Light,
                LocalReduceEffects provides true
            ) {
                EnkShellTheme(theme = ThemeId.Graphite, mode = ThemeMode.Light) {
                    GlassKitSampleScene()
                }
            }
        }
    }

    @Test
    fun graphiteDark() {
        snapshots.snapshot {
            CompositionLocalProvider(
                LocalEffects provides ResolvedEffects.Light,
                LocalReduceEffects provides true
            ) {
                EnkShellTheme(theme = ThemeId.Graphite, mode = ThemeMode.Dark) {
                    GlassKitSampleScene()
                }
            }
        }
    }
}

@Composable
fun GlassKitSampleScene() {
    val colors = LocalShellColors.current
    val glassSource = rememberGlassSource()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.bg)
            .padding(16.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // NavBody
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .glassSurface(GlassSpecs.NavBody, RoundedCornerShape(28.dp), glassSource),
                contentAlignment = Alignment.Center
            ) {
                Text("NavBody", color = colors.ui, fontSize = 12.sp)
            }

            // NavLens
            Box(
                modifier = Modifier
                    .size(width = 80.dp, height = 44.dp)
                    .glassSurface(GlassSpecs.NavLens, RoundedCornerShape(22.dp), glassSource),
                contentAlignment = Alignment.Center
            ) {
                Text("NavLens", color = colors.ui, fontSize = 12.sp)
            }

            // SegmentedBody
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(40.dp)
                    .glassSurface(GlassSpecs.SegmentedBody, RoundedCornerShape(20.dp), glassSource),
                contentAlignment = Alignment.Center
            ) {
                Text("SegmentedBody", color = colors.ui, fontSize = 12.sp)
            }

            // Button
            Box(
                modifier = Modifier
                    .size(width = 120.dp, height = 48.dp)
                    .glassSurface(GlassSpecs.Button, RoundedCornerShape(24.dp), glassSource),
                contentAlignment = Alignment.Center
            ) {
                Text("Button", color = colors.ui, fontSize = 12.sp)
            }

            // Field
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .glassSurface(GlassSpecs.Field, RoundedCornerShape(24.dp), glassSource),
                contentAlignment = Alignment.Center
            ) {
                Text("Field", color = colors.ui, fontSize = 12.sp)
            }

            // Floating
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(80.dp)
                    .glassSurface(GlassSpecs.Floating, RoundedCornerShape(20.dp), glassSource),
                contentAlignment = Alignment.Center
            ) {
                Text("Floating", color = colors.ui, fontSize = 12.sp)
            }

            // Toast
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(44.dp)
                    .glassSurface(GlassSpecs.Toast, RoundedCornerShape(22.dp), glassSource),
                contentAlignment = Alignment.Center
            ) {
                Text("Toast", color = colors.ui, fontSize = 12.sp)
            }
        }
    }
}
