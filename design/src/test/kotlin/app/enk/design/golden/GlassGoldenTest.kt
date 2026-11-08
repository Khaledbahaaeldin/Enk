package app.enk.design.golden

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.cash.paparazzi.DeviceConfig
import app.cash.paparazzi.Paparazzi
import app.enk.design.EnkColors
import app.enk.design.ShadowLayer
import app.enk.design.enkShadow
import app.enk.design.paperGrain
import app.enk.design.shell.EdgeKind
import app.enk.design.shell.GlassLevel
import app.enk.design.shell.LocalReduceEffects
import app.enk.design.shell.ShellBackground
import app.enk.design.shell.glass
import app.enk.design.shell.glassSource
import app.enk.design.shell.rememberGlassSource
import app.enk.design.shell.scrollEdge
import app.enk.design.theme.EnkShellTheme
import app.enk.design.theme.LocalShellColors
import app.enk.design.theme.LocalShellMaterial
import app.enk.design.theme.ThemeId
import app.enk.design.theme.ThemeMode
import org.junit.Rule
import org.junit.Test

class GlassGoldenTest {

    @get:Rule
    val paparazzi = Paparazzi(
        deviceConfig = DeviceConfig.NEXUS_5.copy(
            screenWidth = 1200,
            screenHeight = 2460
        ),
        maxPercentDifference = 2.0
    )

    @Test
    fun graphiteDark() {
        paparazzi.snapshot {
            EnkShellTheme(theme = ThemeId.Graphite, mode = ThemeMode.Dark) {
                GlassSampleScene()
            }
        }
    }

    @Test
    fun graphiteLight() {
        paparazzi.snapshot {
            EnkShellTheme(theme = ThemeId.Graphite, mode = ThemeMode.Light) {
                GlassSampleScene()
            }
        }
    }

    @Test
    fun slateDark() {
        paparazzi.snapshot {
            EnkShellTheme(theme = ThemeId.Slate, mode = ThemeMode.Dark) {
                GlassSampleScene()
            }
        }
    }

    @Test
    fun slateLight() {
        paparazzi.snapshot {
            EnkShellTheme(theme = ThemeId.Slate, mode = ThemeMode.Light) {
                GlassSampleScene()
            }
        }
    }

    @Test
    fun scrollEdgeBars() {
        paparazzi.snapshot {
            EnkShellTheme(theme = ThemeId.Graphite, mode = ThemeMode.Light) {
                ScrollEdgeScene()
            }
        }
    }

    @Test
    fun solidBackendReduceEffects() {
        paparazzi.snapshot {
            CompositionLocalProvider(LocalReduceEffects provides true) {
                EnkShellTheme(theme = ThemeId.Graphite, mode = ThemeMode.Dark) {
                    GlassSampleScene()
                }
            }
        }
    }
}

@Composable
fun GlassSampleScene() {
    val glassSource = rememberGlassSource()
    val colors = LocalShellColors.current
    val material = LocalShellMaterial.current

    ShellBackground(modifier = Modifier.fillMaxSize()) {
        // Strip of note cards that the glass materials will sample and blur over
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp, vertical = 30.dp)
                .glassSource(glassSource),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            SampleNoteCard(
                title = "The Architecture of Thought",
                body = "A quiet morning at the typewriter. Keys striking cream paper with steady rhythm.",
                date = "7 Nov",
                page = "p.1"
            )
            SampleNoteCard(
                title = "Aura and Glass Materials",
                body = "Refraction on API 33+ with soft blur fallback. Every surface casts a dual shadow.",
                date = "6 Nov",
                page = "p.2"
            )
            SampleNoteCard(
                title = "Expressive Shell Motion",
                body = "Natural springs with Apple damping. Fluid gestures tracking 1:1 with hand-off velocity.",
                date = "5 Nov",
                page = "p.3"
            )
            SampleNoteCard(
                title = "Mechanical Precision",
                body = "Cast iron chassis, machined brackets, carriage gliding along cold steel rail.",
                date = "4 Nov",
                page = "p.4"
            )
        }

        // Regular glass: nav-bar-sized pill (width 356 dp, height 64 dp, radius 32 dp)
        Box(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .offset(y = 190.dp)
                .size(width = 356.dp, height = 64.dp)
                .glass(
                    level = GlassLevel.Regular,
                    shape = RoundedCornerShape(32.dp),
                    source = glassSource
                )
                .padding(horizontal = 24.dp),
            contentAlignment = Alignment.Center
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Write",
                    color = colors.t1,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = "Box",
                    color = colors.t2,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = "Find",
                    color = colors.t2,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = "Setup",
                    color = colors.t2,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        }

        // Thick glass: sheet-sized rect (DS 12.8: height 280 dp, radius 30 dp)
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .height(280.dp)
                .glass(
                    level = GlassLevel.Thick,
                    shape = RoundedCornerShape(topStart = 30.dp, topEnd = 30.dp),
                    source = glassSource
                )
                .padding(20.dp)
        ) {
            Column {
                // Grabber bar (36 x 5 dp, radius 2.5 dp)
                Box(
                    modifier = Modifier
                        .align(Alignment.CenterHorizontally)
                        .size(36.dp, 5.dp)
                        .background(material.grab, RoundedCornerShape(2.5.dp))
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "Note Options",
                    color = colors.t1,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Move this note to a divider, export to plaintext, or move to trash.",
                    color = colors.t2,
                    fontSize = 14.sp
                )
            }
        }
    }
}

/** Tint and bottom fade of the compact bar and strip veil; layoutlib does not render the backdrop blur. */
@Composable
fun ScrollEdgeScene() {
    val glassSource = rememberGlassSource()
    ShellBackground(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp, vertical = 30.dp)
                .glassSource(glassSource),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            repeat(5) { i ->
                SampleNoteCard(
                    title = "Note ${i + 1}",
                    body = "A line of text that scrolls beneath the compact bar and the strip veil.",
                    date = "${i + 1} Nov",
                    page = "p.1"
                )
            }
        }
        Box(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .fillMaxWidth()
                .height(96.dp)
                .scrollEdge(EdgeKind.CompactBar, 1f, glassSource)
        )
        Box(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .offset(y = 300.dp)
                .fillMaxWidth()
                .height(64.dp)
                .scrollEdge(EdgeKind.StripVeil, 1f, glassSource)
        )
    }
}

@Composable
private fun SampleNoteCard(
    title: String,
    body: String,
    date: String,
    page: String
) {
    val material = LocalShellMaterial.current
    val cardShadowLayers = listOf(
        ShadowLayer(0.dp, 0.5.dp, 0.dp, 0.dp, Color(0x99FFFFFF), inset = true),
        ShadowLayer(0.dp, 1.dp, 2.dp, 0.dp, Color(0x40000000)),
        ShadowLayer(0.dp, 8.dp, 18.dp, (-8).dp, material.cardShadow)
    )

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(120.dp)
            .enkShadow(cardShadowLayers, RoundedCornerShape(14.dp))
            .background(EnkColors.paper, RoundedCornerShape(14.dp))
            .paperGrain(0.3f)
            .padding(14.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text(
                    text = title,
                    color = EnkColors.ink,
                    fontFamily = FontFamily.Default,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = body,
                    color = Color(0xFF5D564A),
                    fontFamily = FontFamily.Default,
                    fontSize = 12.sp,
                    maxLines = 2
                )
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = date,
                    color = Color(0xFF8A8272),
                    fontSize = 11.sp
                )
                Text(
                    text = page,
                    color = Color(0xFF8A8272),
                    fontSize = 11.sp
                )
            }
        }
    }
}
