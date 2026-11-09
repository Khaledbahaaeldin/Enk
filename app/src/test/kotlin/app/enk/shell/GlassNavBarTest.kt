package app.enk.shell

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.unit.dp
import app.enk.design.theme.EnkShellTheme
import app.enk.design.theme.ThemeId
import app.enk.design.theme.ThemeMode
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], qualifiers = "w400dp-h820dp")
class GlassNavBarTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun navBarHasSelectableGroup() {
        composeTestRule.setContent {
            EnkShellTheme(theme = ThemeId.Graphite, mode = ThemeMode.Dark) {
                GlassNavBar(selected = Dest.Write, onSelect = {})
            }
        }
        composeTestRule.waitForIdle()

        composeTestRule.onNode(
            SemanticsMatcher.keyIsDefined(SemanticsProperties.SelectableGroup)
        ).assertExists()
    }

    @Test
    fun navBarHasFourLabelledItems() {
        composeTestRule.setContent {
            EnkShellTheme(theme = ThemeId.Graphite, mode = ThemeMode.Dark) {
                GlassNavBar(selected = Dest.Write, onSelect = {})
            }
        }
        composeTestRule.waitForIdle()

        val dests = listOf("Write", "Box", "Find", "Setup")
        for (name in dests) {
            val node = composeTestRule.onNode(
                hasText(name) and SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.Tab)
            )
            node.assertExists()
            node.assertContentDescriptionEquals(name)
            if (name == "Write") {
                node.assertIsSelected()
            } else {
                node.assertIsNotSelected()
            }
        }
    }

    @Test
    fun tapOnItemNavigates() {
        var selectedDest: Dest? = null
        composeTestRule.setContent {
            EnkShellTheme(theme = ThemeId.Graphite, mode = ThemeMode.Dark) {
                GlassNavBar(selected = Dest.Write, onSelect = { selectedDest = it })
            }
        }
        composeTestRule.waitForIdle()

        composeTestRule.onNode(
            hasText("Box") and SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.Tab)
        ).performClick()
        composeTestRule.waitForIdle()

        assertEquals(Dest.Box, selectedDest)
    }

    @Test
    fun sameItemTappedTwice() {
        var callCount = 0
        composeTestRule.setContent {
            EnkShellTheme(theme = ThemeId.Graphite, mode = ThemeMode.Dark) {
                GlassNavBar(selected = Dest.Write, onSelect = { callCount++ })
            }
        }
        composeTestRule.waitForIdle()

        composeTestRule.onNode(
            hasText("Write") and SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.Tab)
        ).performClick()
        composeTestRule.waitForIdle()

        assertEquals(0, callCount)
    }

    @Test
    fun dragLensFromFindToWriteNavigates() {
        var selectedDest: Dest? = null
        composeTestRule.setContent {
            EnkShellTheme(theme = ThemeId.Graphite, mode = ThemeMode.Dark) {
                GlassNavBar(selected = Dest.Find, onSelect = { selectedDest = it })
            }
        }
        composeTestRule.waitForIdle()

        val findNode = composeTestRule.onNode(
            hasText("Find") and SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.Tab)
        )
        val writeNode = composeTestRule.onNode(
            hasText("Write") and SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.Tab)
        )

        val findCenter = findNode.fetchSemanticsNode().boundsInRoot.center
        val writeCenter = writeNode.fetchSemanticsNode().boundsInRoot.center

        composeTestRule.onNodeWithTag("glass_nav_bar").performTouchInput {
            down(findCenter)
            moveTo(writeCenter)
            up()
        }
        composeTestRule.waitForIdle()

        assertEquals(Dest.Write, selectedDest)
    }

    @Test
    fun tapDuringLensDrag() {
        var selectedDest: Dest? = null
        composeTestRule.setContent {
            EnkShellTheme(theme = ThemeId.Graphite, mode = ThemeMode.Dark) {
                GlassNavBar(selected = Dest.Box, onSelect = { selectedDest = it })
            }
        }
        composeTestRule.waitForIdle()

        val boxNode = composeTestRule.onNode(
            hasText("Box") and SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.Tab)
        )
        val findNode = composeTestRule.onNode(
            hasText("Find") and SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.Tab)
        )
        val boxCenter = boxNode.fetchSemanticsNode().boundsInRoot.center
        val findCenter = findNode.fetchSemanticsNode().boundsInRoot.center

        // Small drag towards Find that stays closer to Box (quarter distance)
        val intermediate = boxCenter + (findCenter - boxCenter) * 0.25f

        composeTestRule.onNodeWithTag("glass_nav_bar").performTouchInput {
            down(boxCenter)
            moveTo(intermediate)
            up()
        }
        composeTestRule.waitForIdle()

        assertNull("Drag closer to Box should not navigate away from Box", selectedDest)
    }

    @Test
    fun dragCancelledBySystemGesture() {
        var selectedDest: Dest? = null
        composeTestRule.setContent {
            EnkShellTheme(theme = ThemeId.Graphite, mode = ThemeMode.Dark) {
                GlassNavBar(selected = Dest.Box, onSelect = { selectedDest = it })
            }
        }
        composeTestRule.waitForIdle()

        val boxNode = composeTestRule.onNode(
            hasText("Box") and SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.Tab)
        )
        val boxCenter = boxNode.fetchSemanticsNode().boundsInRoot.center

        composeTestRule.onNodeWithTag("glass_nav_bar").performTouchInput {
            down(boxCenter)
            moveBy(Offset(50f, 0f))
            cancel()
        }
        composeTestRule.waitForIdle()

        assertNull(selectedDest)
    }

    @Test
    fun dragReleasedOutsideBar() {
        var selectedDest: Dest? = null
        composeTestRule.setContent {
            EnkShellTheme(theme = ThemeId.Graphite, mode = ThemeMode.Dark) {
                GlassNavBar(selected = Dest.Find, onSelect = { selectedDest = it })
            }
        }
        composeTestRule.waitForIdle()

        val findNode = composeTestRule.onNode(
            hasText("Find") and SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.Tab)
        )
        val findCenter = findNode.fetchSemanticsNode().boundsInRoot.center

        composeTestRule.onNodeWithTag("glass_nav_bar").performTouchInput {
            down(findCenter)
            moveTo(Offset(findCenter.x, -500f))
            up()
        }
        composeTestRule.waitForIdle()

        assertNull("Releasing far outside bar bounds should not navigate", selectedDest)
    }

    @Test
    fun touchTargetsAreAtLeast48Dp() {
        composeTestRule.setContent {
            EnkShellTheme(theme = ThemeId.Graphite, mode = ThemeMode.Dark) {
                GlassNavBar(selected = Dest.Write, onSelect = {})
            }
        }
        composeTestRule.waitForIdle()

        val density = composeTestRule.density
        for (name in listOf("Write", "Box", "Find", "Setup")) {
            val node = composeTestRule.onNode(
                hasText(name) and SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.Tab)
            )
            val bounds = node.fetchSemanticsNode().boundsInRoot
            val widthDp = with(density) { bounds.width.toDp() }
            val heightDp = with(density) { bounds.height.toDp() }
            assertTrue("Tab $name width ($widthDp) must be >= 48 dp", widthDp >= 48.dp)
            assertTrue("Tab $name height ($heightDp) must be >= 48 dp", heightDp >= 48.dp)
        }
    }
}
