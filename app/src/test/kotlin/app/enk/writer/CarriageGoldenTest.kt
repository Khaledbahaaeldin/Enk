package app.enk.writer

import app.cash.paparazzi.DeviceConfig
import app.cash.paparazzi.Paparazzi
import androidx.compose.runtime.CompositionLocalProvider
import app.enk.design.LocalMonoFont
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class CarriageGoldenTest {

    @get:Rule
    val paparazzi = Paparazzi(
        deviceConfig = DeviceConfig.NEXUS_5.copy(
            screenWidth = 1200,
            screenHeight = 2460
        ),
        maxPercentDifference = 2.0
    )

    // Paparazzi cannot load res/font fonts, so the goldens load the real Courier Prime file directly
    // (built on access: it needs layoutlib, which exists only after the rule has started).
    private val courier: FontFamily get() = FontFamily(Font(File("../design/src/main/res/font/courier_prime_bold.ttf")))

    private val referenceGeometry = WriterGeometry.compute(
        widthDp = 400f,
        heightDp = 820f,
        fontScale = 1.0f
    )

    @Test
    fun railAloneAcross400dpScreen() {
        paparazzi.snapshot {
            Rail(railYDp = referenceGeometry.railYDp)
        }
    }

    @Test
    fun carriageAtRestDayMode() {
        paparazzi.snapshot {
            Carriage(
                railYDp = referenceGeometry.railYDp,
                centerXDp = referenceGeometry.carriageCenterXDp(12),
                mode = CarriageMotion.Rest,
                slugChar = null,
                strikeKey = 0,
                night = false,
                reduceEffects = false
            )
        }
    }

    @Test
    fun carriageSlugStrikePeak50ms() {
        paparazzi.snapshot {
            CompositionLocalProvider(LocalMonoFont provides courier) {
                Carriage(
                    railYDp = referenceGeometry.railYDp,
                    centerXDp = referenceGeometry.carriageCenterXDp(12),
                    mode = CarriageMotion.Rest,
                    slugChar = 'k',
                    strikeKey = 0,
                    night = false,
                    reduceEffects = false,
                    slugProgress = 0.45f // 49.5 ms of 110 ms: the peak, 5 dp above rest
                )
            }
        }
    }

    @Test
    fun carriageAtNightLampGlow() {
        paparazzi.snapshot {
            Carriage(
                railYDp = referenceGeometry.railYDp,
                centerXDp = referenceGeometry.carriageCenterXDp(12),
                mode = CarriageMotion.Rest,
                slugChar = null,
                strikeKey = 0,
                night = true,
                reduceEffects = false,
                lampIntensity = 1f
            )
        }
    }

    @Test
    fun carriageDraggingMode() {
        paparazzi.snapshot {
            Carriage(
                railYDp = referenceGeometry.railYDp,
                centerXDp = referenceGeometry.carriageCenterXDp(12) + 20f,
                mode = CarriageMotion.Dragging,
                slugChar = null,
                strikeKey = 0,
                night = false,
                reduceEffects = false
            )
        }
    }

    @Test
    fun hitAreaIs54By48Dp() {
        var w = 0f
        var h = 0f
        paparazzi.snapshot {
            Carriage(
                railYDp = referenceGeometry.railYDp,
                centerXDp = referenceGeometry.carriageCenterXDp(12),
                mode = CarriageMotion.Rest,
                slugChar = null,
                strikeKey = 0,
                night = false,
                reduceEffects = false,
                onHitAreaMeasured = { mw, mh -> w = mw; h = mh }
            )
        }
        assertEquals(54f, w, 0.5f)
        assertEquals(48f, h, 0.5f)
    }

    @Test
    fun snapEasingOvershootsAndSettlesOnTheTarget() {
        val samples = (0..100).map { app.enk.design.EnkMotion.carriageSnap.transform(it / 100f) }
        assertEquals(0f, samples.first(), 0.0001f)
        assertEquals(1f, samples.last(), 0.0001f)
        assert(samples.max() > 1.05f) { "carriageSnap must overshoot by itself" }
    }
}
