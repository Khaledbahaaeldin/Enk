package app.enk.writer

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class CarriageMotionTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun carriageTickAnimatesToTarget() {
        var centerXDp by mutableFloatStateOf(100f)
        var mode by mutableStateOf(CarriageMotion.Rest)

        composeTestRule.setContent {
            Carriage(
                railYDp = 300f,
                centerXDp = centerXDp,
                mode = mode,
                slugChar = 'A',
                night = false,
                reduceEffects = false
            )
        }

        mode = CarriageMotion.Tick
        centerXDp = 120f
        composeTestRule.waitForIdle()

        val bounds = composeTestRule.onNodeWithTag("carriage").getUnclippedBoundsInRoot()
        val currentCenter = (bounds.left.value + bounds.right.value) / 2f
        assertEquals(120f, currentCenter, 1.0f)
    }

    @Test
    fun carriageReturnAnimatesToTarget() {
        var centerXDp by mutableFloatStateOf(200f)
        var mode by mutableStateOf(CarriageMotion.Rest)

        composeTestRule.setContent {
            Carriage(
                railYDp = 300f,
                centerXDp = centerXDp,
                mode = mode,
                slugChar = null,
                night = false,
                reduceEffects = false
            )
        }

        mode = CarriageMotion.Return
        centerXDp = 50f
        composeTestRule.waitForIdle()

        val bounds = composeTestRule.onNodeWithTag("carriage").getUnclippedBoundsInRoot()
        val currentCenter = (bounds.left.value + bounds.right.value) / 2f
        assertEquals(50f, currentCenter, 1.0f)
    }

    @Test
    fun carriageReduceEffectsSnapsDirectly() {
        var centerXDp by mutableFloatStateOf(100f)
        var mode by mutableStateOf(CarriageMotion.Rest)

        composeTestRule.setContent {
            Carriage(
                railYDp = 300f,
                centerXDp = centerXDp,
                mode = mode,
                slugChar = 'A',
                night = false,
                reduceEffects = true
            )
        }

        mode = CarriageMotion.Tick
        centerXDp = 150f
        composeTestRule.waitForIdle()

        val bounds = composeTestRule.onNodeWithTag("carriage").getUnclippedBoundsInRoot()
        val currentCenter = (bounds.left.value + bounds.right.value) / 2f
        assertEquals(150f, currentCenter, 0.5f)
    }
}
