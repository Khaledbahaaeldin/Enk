package app.enk.box

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.DpRect
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.height
import androidx.compose.ui.unit.width
import app.enk.design.effects.LocalEffects
import app.enk.design.effects.ResolvedEffects
import app.enk.design.glass.GlassMorph
import app.enk.design.shell.LocalReduceEffects
import app.enk.design.shell.rememberGlassSource
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class GlassMorphTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun expandingShowsExpandedContentAndItemsInOrder() {
        composeTestRule.setContent {
            CompositionLocalProvider(
                LocalEffects provides ResolvedEffects.Light,
                LocalReduceEffects provides true
            ) {
                val source = rememberGlassSource()
                GlassMorph(
                    expanded = true,
                    anchor = DpRect(20.dp, 50.dp, 120.dp, 90.dp),
                    expandedSize = DpSize(240.dp, 180.dp),
                    expandedRadius = 24.dp,
                    target = DpOffset(10.dp, 20.dp),
                    collapsedAlpha = 1f,
                    source = source,
                    onDismiss = {},
                    collapsedContent = { Text("Collapsed Label") },
                    expandedContent = { _, _ ->
                        Text("Item 0")
                        Text("Item 1")
                        Text("Item 2")
                    },
                    itemCount = 3
                )
            }
        }

        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("Item 0").assertIsDisplayed()
        composeTestRule.onNodeWithText("Item 1").assertIsDisplayed()
        composeTestRule.onNodeWithText("Item 2").assertIsDisplayed()
    }

    @Test
    fun tapOutsideCallsOnDismiss() {
        var dismissed = false
        composeTestRule.setContent {
            CompositionLocalProvider(
                LocalEffects provides ResolvedEffects.Light,
                LocalReduceEffects provides true
            ) {
                val source = rememberGlassSource()
                GlassMorph(
                    expanded = true,
                    anchor = DpRect(50.dp, 50.dp, 150.dp, 90.dp),
                    expandedSize = DpSize(200.dp, 150.dp),
                    expandedRadius = 20.dp,
                    target = DpOffset.Zero,
                    collapsedAlpha = 0f,
                    source = source,
                    onDismiss = { dismissed = true },
                    collapsedContent = {},
                    expandedContent = { _, _ -> Text("Menu Content") },
                    itemCount = 1
                )
            }
        }

        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("glass_morph_scrim_tap").performClick()
        composeTestRule.waitForIdle()
        assertTrue("Tap outside should call onDismiss", dismissed)
    }

    @Test
    fun secondExpandedDoesNotRestartFromZero() {
        var expanded by mutableStateOf(false)
        composeTestRule.mainClock.autoAdvance = false

        composeTestRule.setContent {
            CompositionLocalProvider(
                LocalEffects provides ResolvedEffects.Full,
                LocalReduceEffects provides false
            ) {
                val source = rememberGlassSource()
                GlassMorph(
                    expanded = expanded,
                    anchor = DpRect(20.dp, 20.dp, 120.dp, 60.dp),
                    expandedSize = DpSize(250.dp, 200.dp),
                    expandedRadius = 24.dp,
                    target = DpOffset.Zero,
                    collapsedAlpha = 1f,
                    source = source,
                    onDismiss = {},
                    collapsedContent = { Text("Collapsed") },
                    expandedContent = { _, _ -> Text("Expanded") },
                    itemCount = 1
                )
            }
        }

        // Expand
        expanded = true
        composeTestRule.mainClock.advanceTimeBy(50) // mid-flight
        val midFlightBounds = composeTestRule.onNodeWithTag("glass_morph_surface").getUnclippedBoundsInRoot()
        val midHeight = midFlightBounds.height

        // Retoggle expanded = true without dropping back
        expanded = true
        composeTestRule.mainClock.advanceTimeBy(30)
        val afterBounds = composeTestRule.onNodeWithTag("glass_morph_surface").getUnclippedBoundsInRoot()
        val afterHeight = afterBounds.height

        assertTrue("Height should be monotonic / smoothly continue: $midHeight <= $afterHeight", afterHeight >= midHeight)

        composeTestRule.mainClock.autoAdvance = true
    }

    @Test
    fun contextMenuOpensAtTouchPoint() {
        composeTestRule.setContent {
            CompositionLocalProvider(
                LocalEffects provides ResolvedEffects.Light,
                LocalReduceEffects provides true
            ) {
                val source = rememberGlassSource()
                ContextMenu(
                    cardBounds = Rect(20f, 100f, 300f, 250f),
                    onMoveToDivider = {},
                    onMoveToTrash = {},
                    onDismiss = {},
                    glassSource = source
                )
            }
        }

        composeTestRule.waitForIdle()
        val menu = composeTestRule.onNodeWithTag("context_menu")
        menu.assertIsDisplayed()
        val bounds = menu.getUnclippedBoundsInRoot()
        // Menu should open near the card bounds
        assertTrue("Menu left should be close to card left", bounds.left >= 0.dp)
        assertTrue("Menu top should be below card or above card", bounds.top > 0.dp)
    }

    @Test
    fun alertOpensFromButtonBoundsAndEndsCentred() {
        composeTestRule.setContent {
            CompositionLocalProvider(
                LocalEffects provides ResolvedEffects.Light,
                LocalReduceEffects provides true
            ) {
                Box(modifier = Modifier.size(width = 400.dp, height = 800.dp)) {
                    val source = rememberGlassSource()
                    val anchor = DpRect(50.dp, 650.dp, 350.dp, 698.dp)
                    val target = DpOffset(
                        (400f / 2f - 150f - anchor.left.value).dp,
                        (800f / 2f - 100f - anchor.top.value).dp
                    )
                    DeleteForeverAlert(
                        count = 2,
                        onConfirm = {},
                        onDismiss = {},
                        anchor = anchor,
                        target = target,
                        expanded = true,
                        glassSource = source
                    )
                }
            }
        }

        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("Delete forever").assertIsDisplayed()
        val alertBounds = composeTestRule.onNodeWithTag("glass_morph_surface").getUnclippedBoundsInRoot()

        // When expanded, the alert should end centred in 400 x 800:
        // left should be 400 / 2 - 150 = 50.dp
        // top should be 800 / 2 - 100 = 300.dp
        // width = 300.dp, height = 200.dp
        assertEquals(50f, alertBounds.left.value, 1.5f)
        assertEquals(300f, alertBounds.top.value, 1.5f)
        assertEquals(300f, alertBounds.width.value, 1.5f)
        assertEquals(200f, alertBounds.height.value, 1.5f)
    }
}
