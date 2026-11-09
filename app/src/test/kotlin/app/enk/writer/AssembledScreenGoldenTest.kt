package app.enk.writer

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import app.cash.paparazzi.DeviceConfig
import app.cash.paparazzi.Paparazzi
import app.enk.design.ControlPanel
import app.enk.design.LampColor
import app.enk.design.LampSettings
import app.enk.design.LocalMonoFont
import app.enk.text.StruckPage
import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

/** The writing screen's pieces laid out together, at full height and with the keyboard up. */
class AssembledScreenGoldenTest {

    @get:Rule
    val paparazzi = Paparazzi(
        deviceConfig = DeviceConfig.NEXUS_5.copy(screenWidth = 1200, screenHeight = 2460),
        maxPercentDifference = 2.0
    )

    private val courier: FontFamily get() = FontFamily(Font(File("../design/src/main/res/font/courier_prime_regular.ttf")))

    private val state = WriterState(
        noteId = "assembled",
        pages = listOf(StruckPage("Keyboard is up\nand the lamp is on.")),
        pageIndex = 0,
        caret = 12,
        loaded = true,
        lampOn = true
    )

    private fun assemble(heightDp: Float): Pair<Float, Float> {
        val geometry = WriterGeometry.compute(400f, heightDp, 1.0f)
        var panelTopDp = 0f
        paparazzi.snapshot {
            val density = LocalDensity.current
            CompositionLocalProvider(LocalMonoFont provides courier) {
                // The snapshot root is fixed to the device size; the outer Box loosens it so the
                // inner one can be a shorter window (keyboard up).
                Box(modifier = Modifier.fillMaxSize()) {
                Box(modifier = Modifier.align(Alignment.TopStart).size(400.dp, heightDp.dp)) {
                    PaperSheet(
                        state = state,
                        geometry = geometry,
                        nightProgress = 1f,
                        reduceEffects = false
                    )
                    Rail(railYDp = geometry.railYDp)
                    Carriage(
                        railYDp = geometry.railYDp,
                        centerXDp = geometry.carriageCenterXDp(12),
                        mode = CarriageMotion.Rest,
                        slugChar = null,
                        strikeKey = 0,
                        night = true,
                        reduceEffects = false
                    )
                    ControlPanel(
                        visible = true,
                        lampColor = LampColor.fromWarmth(25),
                        settings = LampSettings(warmth = 25, intensity = 80, beam = 55),
                        onChange = {},
                        onDetent = {},
                        modifier = Modifier.onGloballyPositioned { coords ->
                            panelTopDp = with(density) { coords.positionInRoot().y.toDp().value }
                        }
                    )
                }
                }
            }
        }
        return geometry.railYDp to panelTopDp
    }

    @Test
    fun nightScreenAtFullHeight() {
        val (railY, panelTop) = assemble(820f)
        assertEquals("rail keeps its 62% position when there is room", 508f, railY, 0.5f)
        assertTrue("panel top $panelTop must clear the carriage bottom ${railY + 25f}", panelTop >= railY + 25f)
    }

    @Test
    fun nightScreenWithTheKeyboardUpKeepsThePanelBelowTheCarriage() {
        val (railY, panelTop) = assemble(420f)
        assertTrue("panel top $panelTop must clear the carriage bottom ${railY + 25f}", panelTop >= railY + 25f)
    }
}
