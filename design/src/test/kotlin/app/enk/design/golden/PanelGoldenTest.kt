package app.enk.design.golden

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import app.cash.paparazzi.DeviceConfig
import app.cash.paparazzi.Paparazzi
import app.enk.design.ControlPanel
import app.enk.design.EnkColors
import app.enk.design.Fader
import app.enk.design.LampColor
import app.enk.design.LampSettings
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class PanelGoldenTest {

    @get:Rule
    val paparazzi = Paparazzi(
        deviceConfig = DeviceConfig.NEXUS_5.copy(
            screenWidth = 1200,
            screenHeight = 2460
        ),
        maxPercentDifference = 2.0
    )

    @Test
    fun panelAtRest() {
        val settings = LampSettings(warmth = 25, intensity = 80, beam = 55)
        paparazzi.snapshot {
            Box(
                modifier = Modifier
                    .size(400.dp, 820.dp)
                    .background(EnkColors.paperNight)
            ) {
                ControlPanel(
                    visible = true,
                    lampColor = LampColor.fromWarmth(settings.warmth),
                    settings = settings,
                    onChange = {},
                    onDetent = {}
                )
            }
        }
    }

    @Test
    fun panelFadersAt0Percent() {
        val settings = LampSettings(warmth = 0, intensity = 20, beam = 30)
        paparazzi.snapshot {
            Box(
                modifier = Modifier
                    .size(400.dp, 820.dp)
                    .background(EnkColors.paperNight)
            ) {
                ControlPanel(
                    visible = true,
                    lampColor = LampColor.fromWarmth(settings.warmth),
                    settings = settings,
                    onChange = {},
                    onDetent = {}
                )
            }
        }
    }

    @Test
    fun panelFadersAt25Percent() {
        val settings = LampSettings(warmth = 25, intensity = 40, beam = 48)
        paparazzi.snapshot {
            Box(
                modifier = Modifier
                    .size(400.dp, 820.dp)
                    .background(EnkColors.paperNight)
            ) {
                ControlPanel(
                    visible = true,
                    lampColor = LampColor.fromWarmth(settings.warmth),
                    settings = settings,
                    onChange = {},
                    onDetent = {}
                )
            }
        }
    }

    @Test
    fun panelFadersAt100Percent() {
        val settings = LampSettings(warmth = 100, intensity = 100, beam = 100)
        paparazzi.snapshot {
            Box(
                modifier = Modifier
                    .size(400.dp, 820.dp)
                    .background(EnkColors.paperNight)
            ) {
                ControlPanel(
                    visible = true,
                    lampColor = LampColor.fromWarmth(settings.warmth),
                    settings = settings,
                    onChange = {},
                    onDetent = {}
                )
            }
        }
    }

    @Test
    fun panelOneKnobLifted() {
        val settings = LampSettings(warmth = 25, intensity = 80, beam = 55)
        paparazzi.snapshot {
            Box(
                modifier = Modifier
                    .size(400.dp, 820.dp)
                    .background(EnkColors.paperNight)
            ) {
                ControlPanel(
                    visible = true,
                    lampColor = LampColor.fromWarmth(settings.warmth),
                    settings = settings,
                    onChange = {},
                    onDetent = {},
                    liftedFaderIndex = 0
                )
            }
        }
    }

    @Test
    fun panelMidEnterAt280ms() {
        val settings = LampSettings(warmth = 25, intensity = 80, beam = 55)
        paparazzi.snapshot {
            Box(
                modifier = Modifier
                    .size(400.dp, 820.dp)
                    .background(EnkColors.paperNight)
            ) {
                ControlPanel(
                    visible = true,
                    lampColor = LampColor.fromWarmth(settings.warmth),
                    settings = settings,
                    onChange = {},
                    onDetent = {},
                    enterProgressMs = 280
                )
            }
        }
    }

    @Test
    fun panelReduceEffects() {
        val settings = LampSettings(warmth = 25, intensity = 80, beam = 55)
        paparazzi.snapshot {
            Box(
                modifier = Modifier
                    .size(400.dp, 820.dp)
                    .background(EnkColors.paperNight)
            ) {
                ControlPanel(
                    visible = true,
                    lampColor = LampColor.fromWarmth(settings.warmth),
                    settings = settings,
                    onChange = {},
                    onDetent = {},
                    reduceEffects = true
                )
            }
        }
    }

    @Test
    fun faderTouchHeightIs48Dp() {
        var measuredHeightDp = 0f
        paparazzi.snapshot {
            val density = LocalDensity.current
            Fader(
                label = "WARMTH",
                value = 25,
                min = 0,
                max = 100,
                lampColor = LampColor.fromWarmth(25),
                onValueChange = {},
                onDetent = {},
                modifier = Modifier.onGloballyPositioned { coords ->
                    measuredHeightDp = with(density) { coords.size.height.toDp().value }
                }
            )
        }
        assertEquals(48f, measuredHeightDp, 0.5f)
    }
}
