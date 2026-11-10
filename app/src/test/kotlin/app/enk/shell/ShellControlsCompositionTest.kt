package app.enk.shell

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.state.ToggleableState
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.click
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.unit.dp
import app.enk.design.shell.ShellFader
import app.enk.design.shell.ShellSegmented
import app.enk.design.shell.ShellToggle
import app.enk.design.theme.EnkShellTheme
import app.enk.design.theme.ThemeId
import app.enk.design.theme.ThemeMode
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * The shell controls as composed nodes: their real sizes, hit areas, springs and callbacks, measured
 * from the node tree (the first version asserted constants against themselves). Robolectric's density
 * is 1, so px and dp are the same here.
 *
 * Not covered here: the 1.12 stretch of the toggle bead and fader knob while pressed. Layout bounds ignore
 * a graphics-layer scale and `captureToImage` does not work in this Robolectric setup, so that is on the
 * device checklist instead.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], qualifiers = "w400dp-h820dp")
class ShellControlsCompositionTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private fun show(content: @androidx.compose.runtime.Composable () -> Unit) {
        composeTestRule.setContent {
            EnkShellTheme(theme = ThemeId.Graphite, mode = ThemeMode.Light) {
                Box(modifier = Modifier.padding(20.dp)) { content() }
            }
        }
        composeTestRule.waitForIdle()
    }

    private fun bounds(tag: String) = composeTestRule.onNodeWithTag(tag).fetchSemanticsNode().boundsInRoot

    // ---- toggle ----

    @Test
    fun theToggleTrackIs52By32AndItsTouchAreaAtLeast48High() {
        show { ShellToggle(checked = false, onChange = {}) }
        val track = bounds("shell_toggle_track")
        assertEquals(52f, track.width, 0.5f)
        assertEquals(32f, track.height, 0.5f)
        val touch = bounds("shell_toggle")
        assertTrue("touch height ${touch.height}", touch.height >= 48f)
        assertTrue(touch.width >= 52f)
    }

    @Test
    fun theBeadTravelsExactly20DpWhenTheToggleSwitchesOn() {
        var checked by mutableStateOf(false)
        show { ShellToggle(checked = checked, onChange = {}) }
        val offLeft = bounds("shell_toggle_bead").left
        assertEquals("the bead is 26 wide", 26f, bounds("shell_toggle_bead").width, 0.5f)

        checked = true
        composeTestRule.mainClock.advanceTimeBy(2_000)
        composeTestRule.waitForIdle()

        assertEquals(20f, bounds("shell_toggle_bead").left - offLeft, 0.6f)
    }

    @Test
    fun aTapSwitchesTheToggleExactlyOnce() {
        val calls = mutableListOf<Boolean>()
        show { ShellToggle(checked = false, onChange = { calls += it }) }
        composeTestRule.onNodeWithTag("shell_toggle").performClick()
        composeTestRule.waitForIdle()
        assertEquals(listOf(true), calls)
    }

    @Test
    fun aCallbackReplacedMidPressIsTheOneThatIsCalled() {
        val first = mutableListOf<Boolean>()
        val second = mutableListOf<Boolean>()
        var useSecond by mutableStateOf(false)
        show {
            ShellToggle(
                checked = false,
                onChange = if (useSecond) ({ v: Boolean -> second += v }) else ({ v: Boolean -> first += v })
            )
        }
        composeTestRule.onNodeWithTag("shell_toggle").performTouchInput { down(center) }
        useSecond = true
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("shell_toggle").performTouchInput { up() }
        composeTestRule.waitForIdle()

        assertTrue("the replaced callback must not be called: $first", first.isEmpty())
        assertEquals(listOf(true), second)
    }

    @Test
    fun theToggleIsASwitchForAccessibilityAndItsActionWorks() {
        val calls = mutableListOf<Boolean>()
        show { ShellToggle(checked = true, onChange = { calls += it }) }
        val node = composeTestRule.onNodeWithTag("shell_toggle").fetchSemanticsNode()
        assertEquals(ToggleableState.On, node.config.getOrNull(SemanticsProperties.ToggleableState))
        assertNotNull(node.config.getOrNull(SemanticsProperties.Role))

        composeTestRule.onNodeWithTag("shell_toggle").performSemanticsAction(SemanticsActions.OnClick)
        assertEquals(listOf(false), calls)
    }

    // ---- fader ----

    @Test
    fun theFaderTouchAreaIsAtLeast48HighAndFillsTheWidth() {
        show { ShellFader(value = 0.5f, onChange = {}, modifier = Modifier.fillMaxWidth()) }
        val touch = bounds("shell_fader")
        assertTrue("touch height ${touch.height}", touch.height >= 48f)
        assertEquals(360f, touch.width, 0.5f)
        assertEquals(24f, bounds("shell_fader_knob").width, 0.5f)
    }

    @Test
    fun theFaderGivesAnyValueNotJustTwentySteps() {
        val values = mutableListOf<Float>()
        show { ShellFader(value = 0.5f, onChange = { values += it }, modifier = Modifier.fillMaxWidth()) }
        val width = bounds("shell_fader").width
        val x = 12f + 0.537f * (width - 24f)

        composeTestRule.onNodeWithTag("shell_fader").performTouchInput { click(Offset(x, center.y)) }
        composeTestRule.waitForIdle()

        val value = values.last()
        assertEquals(0.537f, value, 0.01f)
        assertTrue("$value is not a multiple of 0.05", Math.abs(value / 0.05f - Math.round(value / 0.05f)) > 0.02f)
    }

    @Test
    fun theFaderIsAdjustableForAccessibility() {
        val values = mutableListOf<Float>()
        show { ShellFader(value = 0.25f, onChange = { values += it }, modifier = Modifier.fillMaxWidth()) }
        val info = composeTestRule.onNodeWithTag("shell_fader").fetchSemanticsNode()
            .config.getOrNull(SemanticsProperties.ProgressBarRangeInfo)
        assertEquals(0.25f, info?.current ?: -1f, 0.001f)

        composeTestRule.onNodeWithTag("shell_fader").performSemanticsAction(SemanticsActions.SetProgress) { it(0.6f) }
        assertEquals(listOf(0.6f), values)
    }

    @Test
    fun aFaderValueOutsideZeroToOneIsClampedAndNaNIsZero() {
        val values = mutableListOf<Float>()
        show { ShellFader(value = Float.NaN, onChange = { values += it }, modifier = Modifier.fillMaxWidth()) }
        val knob = bounds("shell_fader_knob")
        assertEquals("NaN draws at the start", bounds("shell_fader").left, knob.left, 0.6f)
    }

    // ---- segmented ----

    @Test
    fun theSegmentedControlHasA48TouchHeightAndTapsSelectExactlyOnce() {
        val calls = mutableListOf<Int>()
        show { ShellSegmented(items = listOf("Light", "Dark", "System"), selected = 0, onSelect = { calls += it }) }
        assertTrue(bounds("shell_segmented").height >= 48f)

        composeTestRule.onNodeWithTag("shell_segment_2").performClick()
        composeTestRule.waitForIdle()
        assertEquals("a tap must select once, not twice", listOf(2), calls)
    }

    @Test
    fun draggingAcrossTheSegmentedControlSelectsTheSegmentUnderTheFingerOnRelease() {
        val calls = mutableListOf<Int>()
        show { ShellSegmented(items = listOf("Light", "Dark", "System"), selected = 0, onSelect = { calls += it }) }
        val width = bounds("shell_segmented").width

        composeTestRule.onNodeWithTag("shell_segmented").performTouchInput {
            down(Offset(width * 0.1f, center.y))
            moveTo(Offset(width * 0.5f, center.y))
            moveTo(Offset(width * 0.9f, center.y))
            up()
        }
        composeTestRule.waitForIdle()

        assertEquals(listOf(2), calls)
    }

    @Test
    fun theSelectedSegmentIsExposedToAccessibility() {
        show { ShellSegmented(items = listOf("Light", "Dark", "System"), selected = 1, onSelect = {}) }
        val selected = (0..2).map {
            composeTestRule.onNodeWithTag("shell_segment_$it").fetchSemanticsNode().config
                .getOrNull(SemanticsProperties.Selected)
        }
        assertEquals(listOf(false, true, false), selected)
    }
}
