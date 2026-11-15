package app.enk.writer

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import app.enk.EnkSnapshots
import app.enk.design.EnkColors
import app.enk.design.LampColor
import app.enk.design.LocalMonoFont
import app.enk.design.effects.LocalEffects
import app.enk.design.effects.ResolvedEffects
import app.enk.design.shell.LocalReduceEffects
import app.enk.text.StruckPage
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.GraphicsMode
import java.io.File

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class TopIslandGoldenTest {

    @get:Rule
    val snapshots = EnkSnapshots(TopIslandGoldenTest::class.java)

    private val courier: FontFamily
        get() = FontFamily(Font(File("../design/src/main/res/font/courier_prime_regular.ttf")))

    private val samplePage = StruckPage("The typewriter carriage waits on the paper.\nA clean top bar rests above.")

    @Test
    fun writerIslandRestDay() {
        val geometry = WriterGeometry.compute(400f, 820f, 1f)
        val state = WriterState(
            noteId = "gold-day",
            pages = listOf(samplePage),
            pageIndex = 0,
            caret = 10,
            loaded = true,
            lampOn = false
        )
        snapshots.snapshot {
            CompositionLocalProvider(
                LocalMonoFont provides courier,
                LocalEffects provides ResolvedEffects.Light,
                LocalReduceEffects provides true
            ) {
                Box(modifier = Modifier.size(400.dp, 820.dp).background(EnkColors.chassisTop)) {
                    PaperSheet(
                        state = state,
                        geometry = geometry,
                        nightProgress = 0f,
                        reduceEffects = true
                    )
                    Rail(railYDp = geometry.railYDp)
                    Carriage(
                        railYDp = geometry.railYDp,
                        centerXDp = geometry.carriageCenterXDp(10),
                        mode = CarriageMotion.Rest,
                        slugChar = null,
                        night = false,
                        reduceEffects = true
                    )
                    TopIsland(
                        metaText = "p.1 · 85 / 3000",
                        lampOn = false,
                        lampColor = Color(0xffffd15c),
                        onToggleKeyboard = {},
                        onOpenBox = {},
                        onToggleLamp = {},
                        warmth = 25,
                        onWarmthChange = {},
                        intensity = 80,
                        onIntensityChange = {},
                        beam = 55,
                        onBeamChange = {},
                        onDetent = {},
                        lastKeyMs = 0L
                    )
                }
            }
        }
    }

    @Test
    fun writerIslandRestNight() {
        val geometry = WriterGeometry.compute(400f, 820f, 1f)
        val state = WriterState(
            noteId = "gold-night",
            pages = listOf(samplePage),
            pageIndex = 0,
            caret = 10,
            loaded = true,
            lampOn = true
        )
        snapshots.snapshot {
            CompositionLocalProvider(
                LocalMonoFont provides courier,
                LocalEffects provides ResolvedEffects.Light,
                LocalReduceEffects provides true
            ) {
                Box(modifier = Modifier.size(400.dp, 820.dp).background(EnkColors.chassisNightTop)) {
                    PaperSheet(
                        state = state,
                        geometry = geometry,
                        nightProgress = 1f,
                        reduceEffects = true
                    )
                    Rail(railYDp = geometry.railYDp)
                    Carriage(
                        railYDp = geometry.railYDp,
                        centerXDp = geometry.carriageCenterXDp(10),
                        mode = CarriageMotion.Rest,
                        slugChar = null,
                        night = true,
                        reduceEffects = true
                    )
                    TopIsland(
                        metaText = "p.1 · 85 / 3000",
                        lampOn = true,
                        lampColor = Color(0xffffd15c),
                        onToggleKeyboard = {},
                        onOpenBox = {},
                        onToggleLamp = {},
                        warmth = 25,
                        onWarmthChange = {},
                        intensity = 80,
                        onIntensityChange = {},
                        beam = 55,
                        onBeamChange = {},
                        onDetent = {},
                        lastKeyMs = 0L,
                        initialState = IslandState.Rest
                    )
                }
            }
        }
    }

    @Test
    fun islandActions() {
        snapshots.snapshot {
            CompositionLocalProvider(
                LocalMonoFont provides courier,
                LocalEffects provides ResolvedEffects.Light,
                LocalReduceEffects provides true
            ) {
                Box(
                    modifier = Modifier.size(360.dp, 200.dp).background(EnkColors.chassisTop),
                    contentAlignment = Alignment.Center
                ) {
                    TopIsland(
                        metaText = "p.1 · 85 / 3000",
                        lampOn = true,
                        lampColor = Color(0xffffd15c),
                        onToggleKeyboard = {},
                        onOpenBox = {},
                        onToggleLamp = {},
                        warmth = 25,
                        onWarmthChange = {},
                        intensity = 80,
                        onIntensityChange = {},
                        beam = 55,
                        onBeamChange = {},
                        onDetent = {},
                        lastKeyMs = 0L,
                        initialState = IslandState.Actions
                    )
                }
            }
        }
    }

    @Test
    fun islandLampSliders0() {
        snapshots.snapshot {
            CompositionLocalProvider(
                LocalMonoFont provides courier,
                LocalEffects provides ResolvedEffects.Light,
                LocalReduceEffects provides true
            ) {
                Box(
                    modifier = Modifier.size(360.dp, 320.dp).background(EnkColors.chassisNightTop),
                    contentAlignment = Alignment.Center
                ) {
                    LampPopupContent(
                        lampOn = true,
                        onToggleLamp = {},
                        warmth = 0,
                        onWarmthChange = {},
                        intensity = 20,
                        onIntensityChange = {},
                        beam = 30,
                        onBeamChange = {},
                        onDetent = {}
                    )
                }
            }
        }
    }

    @Test
    fun islandLampSliders50() {
        snapshots.snapshot {
            CompositionLocalProvider(
                LocalMonoFont provides courier,
                LocalEffects provides ResolvedEffects.Light,
                LocalReduceEffects provides true
            ) {
                Box(
                    modifier = Modifier.size(360.dp, 320.dp).background(EnkColors.chassisNightTop),
                    contentAlignment = Alignment.Center
                ) {
                    LampPopupContent(
                        lampOn = true,
                        onToggleLamp = {},
                        warmth = 50,
                        onWarmthChange = {},
                        intensity = 60,
                        onIntensityChange = {},
                        beam = 65,
                        onBeamChange = {},
                        onDetent = {}
                    )
                }
            }
        }
    }

    @Test
    fun islandLampSliders100() {
        snapshots.snapshot {
            CompositionLocalProvider(
                LocalMonoFont provides courier,
                LocalEffects provides ResolvedEffects.Light,
                LocalReduceEffects provides true
            ) {
                Box(
                    modifier = Modifier.size(360.dp, 320.dp).background(EnkColors.chassisNightTop),
                    contentAlignment = Alignment.Center
                ) {
                    LampPopupContent(
                        lampOn = true,
                        onToggleLamp = {},
                        warmth = 100,
                        onWarmthChange = {},
                        intensity = 100,
                        onIntensityChange = {},
                        beam = 100,
                        onBeamChange = {},
                        onDetent = {}
                    )
                }
            }
        }
    }
}
