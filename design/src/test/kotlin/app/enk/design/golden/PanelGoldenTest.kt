package app.enk.design.golden

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import app.enk.design.ControlPanel
import app.enk.design.ControlPanelMetrics
import app.enk.design.EnkColors
import app.enk.design.Fader
import app.enk.design.LampColor
import app.enk.design.LampSettings
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class PanelGoldenTest {

    @get:Rule
    val snapshots = EnkSnapshots(PanelGoldenTest::class.java)

    @Test
    fun panelAtRest() {
        val settings = LampSettings(warmth = 25, intensity = 80, beam = 55)
        snapshots.snapshot {
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
        snapshots.snapshot {
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
        snapshots.snapshot {
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
        snapshots.snapshot {
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
        snapshots.snapshot {
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
        snapshots.snapshot {
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
    fun panelReduceEffectsMidEnterIsAFadeWithNoSlide() {
        val settings = LampSettings(warmth = 25, intensity = 80, beam = 55)
        snapshots.snapshot {
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
                    enterProgressMs = 50,
                    reduceEffects = true
                )
            }
        }
    }

    @Test
    fun faderRowsInsideThePanelKeepA48DpTouchHeight() {
        // Panel = 3 rows + 4 dp frame twice + 8/10 dp well padding + 30 dp bottom margin. If a row
        // were clamped below the fader's 48 dp, the panel would be shorter than this.
        var panelHeightDp = 0f
        snapshots.snapshot {
            val density = LocalDensity.current
            Box(modifier = Modifier.size(400.dp, 820.dp).background(EnkColors.paperNight)) {
                ControlPanel(
                    visible = true,
                    lampColor = LampColor.fromWarmth(25),
                    settings = LampSettings(warmth = 25, intensity = 80, beam = 55),
                    onChange = {},
                    onDetent = {},
                    modifier = Modifier.onGloballyPositioned { coords ->
                        panelHeightDp = with(density) { coords.size.height.toDp().value }
                    }
                )
            }
        }
        assertEquals(ControlPanelMetrics.FOOTPRINT_DP, panelHeightDp, 0.5f)
    }

    @Test
    fun faderTouchHeightIs48Dp() {
        var measuredHeightDp = 0f
        snapshots.snapshot {
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
