package app.enk.writer

import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class CarriageStrikeTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun slugRise17PlacesSlugTopAtRailYMinus29() {
        val railYDp = 300f
        val centerXDp = 150f

        composeTestRule.setContent {
            Carriage(
                railYDp = railYDp,
                centerXDp = centerXDp,
                mode = CarriageMotion.Rest,
                slugChar = 'A',
                night = false,
                reduceEffects = false,
                slugFrame = { StrikeFrame.Rest.copy(slugRiseDp = 17f) }
            )
        }

        val slugTopDp = composeTestRule.onNodeWithTag("type_slug").getUnclippedBoundsInRoot().top.value
        val expectedTopDp = railYDp - 29f
        assertEquals("With rise 17 the slug's top must be at railY - 29 dp", expectedTopDp, slugTopDp, 0.5f)
    }

    @Test
    fun slugAtRestIsAtRailYMinus12() {
        val railYDp = 300f
        val centerXDp = 150f

        composeTestRule.setContent {
            Carriage(
                railYDp = railYDp,
                centerXDp = centerXDp,
                mode = CarriageMotion.Rest,
                slugChar = 'A',
                night = false,
                reduceEffects = false,
                slugFrame = { StrikeFrame.Rest }
            )
        }

        val slugTopDp = composeTestRule.onNodeWithTag("type_slug").getUnclippedBoundsInRoot().top.value
        val expectedTopDp = railYDp - 12f
        assertEquals("At rest slug top must be at railY - 12 dp", expectedTopDp, slugTopDp, 1.0f)
    }
}
