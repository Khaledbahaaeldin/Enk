package app.enk.writer

import app.cash.paparazzi.DeviceConfig
import app.cash.paparazzi.Paparazzi
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
            Carriage(
                railYDp = referenceGeometry.railYDp,
                centerXDp = referenceGeometry.carriageCenterXDp(12),
                mode = CarriageMotion.Rest,
                slugChar = 'k',
                strikeKey = 1,
                night = false,
                reduceEffects = false
            )
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
    fun carriageMidSnapOvershoot() {
        paparazzi.snapshot {
            Carriage(
                railYDp = referenceGeometry.railYDp,
                centerXDp = referenceGeometry.carriageCenterXDp(13),
                mode = CarriageMotion.Snap,
                slugChar = null,
                strikeKey = 0,
                night = false,
                reduceEffects = false
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
}
