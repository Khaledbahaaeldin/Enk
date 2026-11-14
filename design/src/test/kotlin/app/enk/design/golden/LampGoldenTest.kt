package app.enk.design.golden

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import app.enk.design.EnkColors
import app.enk.design.LampLight
import app.enk.design.LampSettings
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class LampGoldenTest {

    @get:Rule
    val snapshots = EnkSnapshots(LampGoldenTest::class.java)

    @Test
    fun lampDefaultSettingsCol12() {
        snapshots.snapshot {
            Box(
                modifier = Modifier
                    .size(400.dp, 820.dp)
                    .background(EnkColors.paperNight)
            ) {
                LampLight(
                    carriageXDp = 180f,
                    railYDp = 509f,
                    settings = LampSettings(warmth = 25, intensity = 80, beam = 55),
                    visible = true,
                    reduceEffects = false
                )
            }
        }
    }

    @Test
    fun lampMaxBeamCol12() {
        snapshots.snapshot {
            Box(
                modifier = Modifier
                    .size(400.dp, 820.dp)
                    .background(EnkColors.paperNight)
            ) {
                LampLight(
                    carriageXDp = 180f,
                    railYDp = 509f,
                    settings = LampSettings(warmth = 25, intensity = 80, beam = 100),
                    visible = true,
                    reduceEffects = false
                )
            }
        }
    }

    @Test
    fun lampReduceEffectsCol12() {
        snapshots.snapshot {
            Box(
                modifier = Modifier
                    .size(400.dp, 820.dp)
                    .background(EnkColors.paperNight)
            ) {
                LampLight(
                    carriageXDp = 180f,
                    railYDp = 509f,
                    settings = LampSettings(warmth = 25, intensity = 80, beam = 55),
                    visible = true,
                    reduceEffects = true
                )
            }
        }
    }
}
