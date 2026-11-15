package app.enk.shell

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import app.enk.design.theme.EnkShellTheme
import app.enk.design.theme.ThemeId
import app.enk.design.theme.ThemeMode
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class NavWriteButtonTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun barHasThreeTabsAndAWriteButton() {
        composeTestRule.setContent {
            EnkShellTheme(theme = ThemeId.Graphite, mode = ThemeMode.Dark) {
                GlassNavBar(selected = Dest.Box, onSelect = {})
            }
        }
        composeTestRule.waitForIdle()

        for (tab in listOf("Box", "Find", "Setup")) {
            composeTestRule.onNode(
                hasText(tab) and SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.Tab)
            ).assertExists().assertIsDisplayed()
        }

        val writeNode = composeTestRule.onNodeWithTag("write_button")
        writeNode.assertExists().assertIsDisplayed()
        writeNode.assert(SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.Button))
        writeNode.assertContentDescriptionEquals("Write")
    }

    @Test
    fun writeButtonOpensTheWriter() {
        var selectedDest: Dest? = null
        composeTestRule.setContent {
            EnkShellTheme(theme = ThemeId.Graphite, mode = ThemeMode.Dark) {
                GlassNavBar(selected = Dest.Box, onSelect = { selectedDest = it })
            }
        }
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithTag("write_button").performClick()
        composeTestRule.waitForIdle()

        assertEquals(Dest.Write, selectedDest)
    }

    @Test
    fun lensReleasedOverWriteDoesNotOpenTheWriter() {
        var selectedDest: Dest? = null
        composeTestRule.setContent {
            EnkShellTheme(theme = ThemeId.Graphite, mode = ThemeMode.Dark) {
                GlassNavBar(selected = Dest.Box, onSelect = { selectedDest = it })
            }
        }
        composeTestRule.waitForIdle()

        val boxCenter = composeTestRule.onNode(
            hasText("Box") and SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.Tab)
        ).fetchSemanticsNode().boundsInRoot.center

        val writeCenter = composeTestRule.onNodeWithTag("write_button")
            .fetchSemanticsNode().boundsInRoot.center

        composeTestRule.onNodeWithTag("glass_nav_bar").performTouchInput {
            down(boxCenter)
            moveTo(writeCenter)
            up()
        }
        composeTestRule.waitForIdle()

        assertEquals(Dest.Setup, selectedDest)
    }

    @Test
    fun layoutAt360And411AndFontScale13And2() {
        val widths = listOf(360.dp, 411.dp)
        val fontScales = listOf(1.3f, 2.0f)

        composeTestRule.setContent {
            Column {
                for (w in widths) {
                    for (fs in fontScales) {
                        val currentDensity = LocalDensity.current
                        CompositionLocalProvider(
                            LocalDensity provides Density(density = currentDensity.density, fontScale = fs)
                        ) {
                            EnkShellTheme(theme = ThemeId.Graphite, mode = ThemeMode.Dark) {
                                Box(modifier = Modifier.width(w).testTag("container_${w.value.toInt()}_${(fs * 10).toInt()}")) {
                                    GlassNavBar(selected = Dest.Box, onSelect = {})
                                }
                            }
                        }
                    }
                }
            }
        }
        composeTestRule.waitForIdle()

        val density = composeTestRule.density

        for (w in widths) {
            for (fs in fontScales) {
                val tag = "container_${w.value.toInt()}_${(fs * 10).toInt()}"
                val containerNode = composeTestRule.onNodeWithTag(tag)
                containerNode.assertIsDisplayed()

                val barNode = composeTestRule.onNode(
                    hasTestTag("nav_bar_track") and hasAnyAncestor(hasTestTag(tag))
                )
                val writeNode = composeTestRule.onNode(
                    hasTestTag("write_button") and hasAnyAncestor(hasTestTag(tag))
                )

                barNode.assertIsDisplayed()
                writeNode.assertIsDisplayed()

                val containerBounds = containerNode.fetchSemanticsNode().boundsInRoot
                val barBounds = barNode.fetchSemanticsNode().boundsInRoot
                val writeBounds = writeNode.fetchSemanticsNode().boundsInRoot

                val barLeftDp = with(density) { (barBounds.left - containerBounds.left).toDp() }
                val barRightDp = with(density) { (barBounds.right - containerBounds.left).toDp() }
                val writeLeftDp = with(density) { (writeBounds.left - containerBounds.left).toDp() }
                val writeRightDp = with(density) { (writeBounds.right - containerBounds.left).toDp() }

                assertTrue("Left margin ($barLeftDp) must be >= 12 dp at width $w fs $fs", barLeftDp >= 12.dp)
                val rightMarginDp = w - writeRightDp
                assertTrue("Right margin ($rightMarginDp) must be >= 12 dp at width $w fs $fs", rightMarginDp >= 12.dp)
                assertTrue("Gap between bar and write (${writeLeftDp - barRightDp}) must be >= 8 dp at width $w fs $fs", writeLeftDp - barRightDp >= 8.dp)
            }
        }
    }
}
