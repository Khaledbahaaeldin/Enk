package app.enk.writer

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.unit.dp
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class TypeSlugTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun slugRiseOffsetsSlugUpward() {
        var frame by mutableStateOf<StrikeFrame?>(StrikeFrame.Rest)
        composeTestRule.setContent {
            TypeSlug(
                slugChar = 'a',
                slugFrame = { frame },
                night = false
            )
        }
        val restTop = composeTestRule.onNodeWithTag("type_slug").getUnclippedBoundsInRoot().top

        frame = StrikeFrame.Rest.copy(slugRiseDp = 17f)
        composeTestRule.waitForIdle()
        val riseTop = composeTestRule.onNodeWithTag("type_slug").getUnclippedBoundsInRoot().top

        assertEquals(17f, (restTop - riseTop).value, 0.5f)
    }

    @Test
    fun slugNullAndRestAreEqual() {
        var frame by mutableStateOf<StrikeFrame?>(null)
        composeTestRule.setContent {
            TypeSlug(
                slugChar = 'a',
                slugFrame = { frame },
                night = false
            )
        }
        val nullTop = composeTestRule.onNodeWithTag("type_slug").getUnclippedBoundsInRoot().top

        frame = StrikeFrame.Rest
        composeTestRule.waitForIdle()
        val restTop = composeTestRule.onNodeWithTag("type_slug").getUnclippedBoundsInRoot().top

        assertEquals(nullTop.value, restTop.value, 0.001f)
    }

    @Test
    fun railGeometry() {
        val railYDp = 508f
        val geom = RailGeometry.compute(railYDp)
        assertEquals(12f, geom.rodHeightDp, 0.001f)
        assertEquals(502f, geom.rodTopDp, 0.001f) // railY - 6
        assertEquals(24f, geom.bracketHeightDp, 0.001f)
        assertEquals(12f, geom.bracketWidthDp, 0.001f)
        assertEquals(496f, geom.bracketTopDp, 0.001f) // railY - 12
        assertEquals(3f, geom.bracketCornerRadiusDp, 0.001f)
    }

    @Test
    fun interactiveArea54x48() {
        assertEquals(54f, CarriageGeometry.interactiveWidthDp, 0.001f)
        assertEquals(48f, CarriageGeometry.interactiveHeightDp, 0.001f)
        assertEquals(54f, CarriageGeometry.bodyWidthDp, 0.001f)
        assertEquals(36f, CarriageGeometry.bodyHeightDp, 0.001f)
        assertEquals(18f, CarriageGeometry.slotWidthDp, 0.001f)
        assertEquals(11f, CarriageGeometry.slotHeightDp, 0.001f)
        assertEquals(14f, CarriageGeometry.slugWidthDp, 0.001f)
        assertEquals(12f, CarriageGeometry.slugHeightDp, 0.001f)
    }

    @Test
    fun slugCharFiltering() {
        assertEquals('A', filterSlugChar('A'))
        assertEquals('z', filterSlugChar('z'))
        assertEquals('1', filterSlugChar('1'))
        assertEquals('?', filterSlugChar('?'))

        assertNull(filterSlugChar(' '))
        assertNull(filterSlugChar('\n'))
        assertNull(filterSlugChar('\r'))
        assertNull(filterSlugChar('\t'))
        assertNull(filterSlugChar('\b'))
        assertNull(filterSlugChar(null))
        assertNull(filterSlugChar('\u0000'))
        assertNull(filterSlugChar('\u001F'))
    }

    @Test
    fun slugCharFilteringEdgeCases() {
        assertEquals('!', filterSlugChar('!'))
        assertEquals('#', filterSlugChar('#'))
        assertEquals('é', filterSlugChar('é'))
        assertEquals('ñ', filterSlugChar('ñ'))

        assertNull(filterSlugChar('\u000C'))
        assertNull(filterSlugChar('\u001B'))
        assertNull(filterSlugChar('\u007F'))
        assertNull(filterSlugChar('\u00A0'))
    }
}
