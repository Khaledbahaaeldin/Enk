package app.enk.design.glass

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.state.ToggleableState
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.dp
import app.enk.design.effects.LocalEffects
import app.enk.design.effects.ResolvedEffects
import app.enk.design.shell.LocalReduceEffects
import app.enk.design.shell.ShellFader
import app.enk.design.shell.ShellSegmented
import app.enk.design.shell.ShellToggle
import app.enk.design.theme.EnkShellTheme
import app.enk.design.theme.ThemeId
import app.enk.design.theme.ThemeMode
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class GlassControlsTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private fun show(content: @Composable () -> Unit) {
        composeTestRule.setContent {
            CompositionLocalProvider(
                LocalEffects provides ResolvedEffects.Light,
                LocalReduceEffects provides true
            ) {
                EnkShellTheme(theme = ThemeId.Graphite, mode = ThemeMode.Light) {
                    Box(modifier = Modifier.padding(16.dp)) {
                        content()
                    }
                }
            }
        }
        composeTestRule.waitForIdle()
    }

    @Test
    fun glassButtonHasButtonRoleAndFiresOnClick() {
        var clicked = false
        show {
            GlassButton(
                onClick = { clicked = true },
                modifier = Modifier.testTag("test_glass_button")
            ) {
                Text("Click me")
            }
        }

        val node = composeTestRule.onNodeWithTag("test_glass_button").fetchSemanticsNode()
        assertEquals(Role.Button, node.config.getOrNull(SemanticsProperties.Role))

        composeTestRule.onNodeWithTag("test_glass_button").performClick()
        composeTestRule.waitForIdle()
        assertTrue(clicked)
    }

    @Test
    fun glassButtonDisabledDoesNotFireOnClick() {
        var clicked = false
        show {
            GlassButton(
                onClick = { clicked = true },
                enabled = false,
                modifier = Modifier.testTag("disabled_button")
            ) {
                Text("Disabled")
            }
        }

        composeTestRule.onNodeWithTag("disabled_button").performClick()
        composeTestRule.waitForIdle()
        assertFalse(clicked)
    }

    @Test
    fun shellToggleSwitchesStateAndExposesSwitchRole() {
        var checked by mutableStateOf(false)
        show {
            ShellToggle(
                checked = checked,
                onChange = { checked = it }
            )
        }

        val node = composeTestRule.onNodeWithTag("shell_toggle").fetchSemanticsNode()
        assertEquals(Role.Switch, node.config.getOrNull(SemanticsProperties.Role))
        assertEquals(ToggleableState.Off, node.config.getOrNull(SemanticsProperties.ToggleableState))

        composeTestRule.onNodeWithTag("shell_toggle").performClick()
        composeTestRule.waitForIdle()
        assertTrue(checked)
    }

    @Test
    fun shellSegmentedExposesRadioButtonRoleAndSelectsItem() {
        var selectedIndex by mutableStateOf(0)
        show {
            ShellSegmented(
                items = listOf("First", "Second", "Third"),
                selected = selectedIndex,
                onSelect = { selectedIndex = it }
            )
        }

        val seg1 = composeTestRule.onNodeWithTag("shell_segment_1").fetchSemanticsNode()
        assertEquals(Role.RadioButton, seg1.config.getOrNull(SemanticsProperties.Role))
        assertEquals(false, seg1.config.getOrNull(SemanticsProperties.Selected))

        composeTestRule.onNodeWithTag("shell_segment_1").performClick()
        composeTestRule.waitForIdle()
        assertEquals(1, selectedIndex)
    }

    @Test
    fun shellFaderExposesProgressBarRangeInfo() {
        show {
            ShellFader(
                value = 0.4f,
                onChange = {},
                modifier = Modifier.fillMaxWidth()
            )
        }

        val node = composeTestRule.onNodeWithTag("shell_fader").fetchSemanticsNode()
        val rangeInfo = node.config.getOrNull(SemanticsProperties.ProgressBarRangeInfo)
        assertEquals(0.4f, rangeInfo?.current ?: 0f, 0.01f)
    }
}
