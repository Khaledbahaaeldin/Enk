package app.enk.design.glass

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class LiquidPressMathTest {

    @Test
    fun swellIsFourDpAtFullPress() {
        val density = 2.625f
        val heightPx = 40f * density
        val p0 = LiquidPressMath.swellScale(heightPx, density, 0f)
        val p1 = LiquidPressMath.swellScale(heightPx, density, 1f)

        assertEquals(1f, p0, 0.0001f)
        assertEquals(1f + 4f / 40f, p1, 0.0001f)
    }

    @Test
    fun leanSaturates() {
        val maxOffset = 100f
        val saturated = LiquidPressMath.lean(10000f, maxOffset)
        assertTrue("lean must saturate below maxOffset ($saturated < $maxOffset)", saturated < maxOffset)
        assertTrue("lean must be positive for positive offset", saturated > 0f)

        val zero = LiquidPressMath.lean(0f, maxOffset)
        assertEquals(0f, zero, 0.0001f)

        val neg = LiquidPressMath.lean(-10000f, maxOffset)
        assertTrue("lean sign follows negative offset", neg < 0f)
        assertTrue("negative lean must saturate above -maxOffset", neg > -maxOffset)
    }

    @Test
    fun stretchFollowsTheAngle() {
        val density = 2f
        val size = Size(200f, 100f) // height 100px = 50dp
        val horizontalPull = Offset(50f, 0f)

        val (sx, sy) = LiquidPressMath.stretch(horizontalPull, size, density)
        assertTrue("horizontal pull adds to scaleX ($sx > 0)", sx > 0f)
        assertEquals("horizontal pull adds 0 to scaleY", 0f, sy, 0.0001f)
    }

    @Test
    fun lightIsStill() {
        // At Light level or reduced motion, swellScale is forced to 1f and lean/stretch are 0f
        val density = 2.625f
        val heightPx = 40f * density
        val scale = LiquidPressMath.resolveScale(isLight = true, heightPx = heightPx, density = density, progress = 1f)
        val translation = LiquidPressMath.resolveTranslation(isLight = true, offsetPx = 50f, maxOffsetPx = 100f)
        val stretch = LiquidPressMath.resolveStretch(isLight = true, offset = Offset(50f, 0f), size = Size(200f, 100f), density = density)

        assertEquals(1f, scale, 0.0001f)
        assertEquals(0f, translation, 0.0001f)
        assertEquals(0f, stretch.first, 0.0001f)
        assertEquals(0f, stretch.second, 0.0001f)
    }
}
