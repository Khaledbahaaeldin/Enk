package app.enk.shell

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import app.enk.EnkSnapshots
import app.enk.design.EnkColors
import app.enk.design.LocalMonoFont
import app.enk.design.effects.LocalEffects
import app.enk.design.effects.ResolvedEffects
import app.enk.design.glass.GlassSpecs
import app.enk.design.glass.glassSurface
import app.enk.design.shell.rememberGlassSource
import app.enk.design.shell.LocalReduceEffects
import app.enk.text.StruckPage
import app.enk.writer.Carriage
import app.enk.writer.CarriageMotion
import app.enk.writer.PaperSheet
import app.enk.writer.Rail
import app.enk.writer.TopIsland
import app.enk.writer.WriterGeometry
import app.enk.writer.WriterState
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.GraphicsMode
import java.io.File
import kotlin.math.roundToInt

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class RailTransitionGoldenTest {

    @get:Rule
    val snapshots = EnkSnapshots(RailTransitionGoldenTest::class.java)

    private val courier: FontFamily
        get() = FontFamily(Font(File("../design/src/main/res/font/courier_prime_regular.ttf")))

    @Composable
    private fun TransitionFrame(p: Float) {
        val widthDp = 360f
        val heightDp = 720f
        val geometry = WriterGeometry.compute(widthDp, heightDp, 1f)
        val railY = geometry.railYDp // 446f
        val paperTop = 8f
        val paperHeight = heightDp - 240f - paperTop // paper above keyboard
        val lr = railY - paperTop
        val glassSource = rememberGlassSource()

        val samplePage = StruckPage("The carriage lands with a ding.")
        val writerState = WriterState(
            noteId = "gold-trans",
            pages = listOf(samplePage),
            pageIndex = 0,
            caret = 15,
            loaded = true,
            lampOn = false
        )

        Box(
            modifier = Modifier
                .size(widthDp.dp, heightDp.dp)
                .background(EnkColors.chassisTop)
        ) {
            // Beat 1 & 7: Box content recedes and fades
            val boxScale = RailTransitionMath.boxScale(p)
            val boxTransY = RailTransitionMath.boxTranslationY(p)
            val boxAlpha = RailTransitionMath.boxAlpha(p)

            if (boxAlpha > 0.01f) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .graphicsLayer {
                            scaleX = boxScale
                            scaleY = boxScale
                            translationY = boxTransY * density
                            alpha = boxAlpha
                        }
                        .background(Color(0xFFEFECE6))
                )
            }

            // Beat 4: Paper is fed
            val u = RailTransitionMath.paperClipU(p)
            val clipTop = lr * (1f - u)
            val clipBottom = (paperHeight - lr) * (1f - u)

            if (u > 0.01f) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(
                            top = (paperTop + clipTop).dp,
                            bottom = (heightDp - (paperTop + paperHeight - clipBottom)).coerceAtLeast(0f).dp
                        )
                ) {
                    PaperSheet(
                        state = writerState,
                        geometry = geometry,
                        nightProgress = 0f,
                        reduceEffects = true
                    )
                }
            }

            // Beat 2 & 3: Nav bar becomes the rail
            val navLeft = RailTransitionMath.navLeft(p)
            val navW = RailTransitionMath.navWidth(p)
            val navH = RailTransitionMath.navHeight(p)
            val navTop = RailTransitionMath.navTop(p, startTop = heightDp - 84f, railY = railY)
            val navRadius = RailTransitionMath.navRadius(p)
            val steelAlpha = RailTransitionMath.steelRailAlpha(p)

            Box(
                modifier = Modifier
                    .offset { IntOffset((navLeft * density).roundToInt(), (navTop * density).roundToInt()) }
                    .size(navW.dp, navH.dp)
                    .clip(RoundedCornerShape(navRadius.dp))
                    .glassSurface(
                        spec = GlassSpecs.Floating,
                        shape = RoundedCornerShape(navRadius.dp),
                        source = glassSource
                    )
            ) {
                if (steelAlpha > 0.01f) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .alpha(steelAlpha)
                            .background(
                                Brush.verticalGradient(
                                    listOf(
                                        Color(0xFFD3D0C8),
                                        Color(0xFF96948E),
                                        Color(0xFF5F5D58)
                                    )
                                )
                            )
                    )
                }
            }

            // Beat 5: Carriage lands
            val carriageTransY = RailTransitionMath.carriageTranslationY(p)
            val carriageAlpha = RailTransitionMath.carriageAlpha(p)

            if (carriageAlpha > 0.01f) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .graphicsLayer {
                            translationY = carriageTransY * density
                            alpha = carriageAlpha
                        }
                ) {
                    Carriage(
                        railYDp = railY,
                        centerXDp = geometry.carriageCenterXDp(15),
                        mode = CarriageMotion.Rest,
                        slugChar = null,
                        night = false,
                        reduceEffects = true
                    )
                }
            }

            // Beat 6: Island arrives
            val islandScale = RailTransitionMath.islandScale(p)
            val islandAlpha = RailTransitionMath.islandAlpha(p)

            if (islandAlpha > 0.01f) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .graphicsLayer {
                            scaleX = islandScale
                            scaleY = islandScale
                            alpha = islandAlpha
                        }
                ) {
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
    fun transitionP0() {
        snapshots.snapshot {
            CompositionLocalProvider(
                LocalMonoFont provides courier,
                LocalEffects provides ResolvedEffects.Light,
                LocalReduceEffects provides true
            ) {
                TransitionFrame(0f)
            }
        }
    }

    @Test
    fun transitionP035() {
        snapshots.snapshot {
            CompositionLocalProvider(
                LocalMonoFont provides courier,
                LocalEffects provides ResolvedEffects.Light,
                LocalReduceEffects provides true
            ) {
                TransitionFrame(0.35f)
            }
        }
    }

    @Test
    fun transitionP060() {
        snapshots.snapshot {
            CompositionLocalProvider(
                LocalMonoFont provides courier,
                LocalEffects provides ResolvedEffects.Light,
                LocalReduceEffects provides true
            ) {
                TransitionFrame(0.60f)
            }
        }
    }

    @Test
    fun transitionP1() {
        snapshots.snapshot {
            CompositionLocalProvider(
                LocalMonoFont provides courier,
                LocalEffects provides ResolvedEffects.Light,
                LocalReduceEffects provides true
            ) {
                TransitionFrame(1f)
            }
        }
    }
}
