package app.enk.start

import androidx.compose.ui.geometry.Rect
import app.cash.paparazzi.DeviceConfig
import app.cash.paparazzi.Paparazzi
import org.junit.Rule
import org.junit.Test

class StartGoldenTest {

    @get:Rule
    val paparazzi = Paparazzi(
        deviceConfig = DeviceConfig.NEXUS_5.copy(
            screenWidth = 1200,
            screenHeight = 2460
        ),
        maxPercentDifference = 2.0
    )

    private val referenceDarkTargets = StartTargets(
        railY = 508f,
        screenWidth = 400f,
        screenHeight = 820f,
        carriageX = 276f,
        caretRect = Rect(270.3f, 479.5f, 282.01f, 481.5f),
        paperHeight = 760f,
        lightSplash = false
    )

    private val referenceLightTargets = referenceDarkTargets.copy(lightSplash = true)

    @Test
    fun darkSplash_t0() {
        paparazzi.snapshot {
            StartOverlay(targets = referenceDarkTargets, timeMs = 0f)
        }
    }

    @Test
    fun darkSplash_t200() {
        paparazzi.snapshot {
            StartOverlay(targets = referenceDarkTargets, timeMs = 200f)
        }
    }

    @Test
    fun darkSplash_t460() {
        paparazzi.snapshot {
            StartOverlay(targets = referenceDarkTargets, timeMs = 460f)
        }
    }

    @Test
    fun darkSplash_t630() {
        paparazzi.snapshot {
            StartOverlay(targets = referenceDarkTargets, timeMs = 630f)
        }
    }

    @Test
    fun darkSplash_t900() {
        paparazzi.snapshot {
            StartOverlay(targets = referenceDarkTargets, timeMs = 900f)
        }
    }

    @Test
    fun lightSplash_t0() {
        paparazzi.snapshot {
            StartOverlay(targets = referenceLightTargets, timeMs = 0f)
        }
    }

    @Test
    fun lightSplash_t200() {
        paparazzi.snapshot {
            StartOverlay(targets = referenceLightTargets, timeMs = 200f)
        }
    }

    @Test
    fun lightSplash_t460() {
        paparazzi.snapshot {
            StartOverlay(targets = referenceLightTargets, timeMs = 460f)
        }
    }

    @Test
    fun lightSplash_t630() {
        paparazzi.snapshot {
            StartOverlay(targets = referenceLightTargets, timeMs = 630f)
        }
    }

    @Test
    fun lightSplash_t900() {
        paparazzi.snapshot {
            StartOverlay(targets = referenceLightTargets, timeMs = 900f)
        }
    }
}
