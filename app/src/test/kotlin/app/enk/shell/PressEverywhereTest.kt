package app.enk.shell

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.unit.dp
import app.enk.box.NoteCard
import app.enk.data.NoteSummary
import app.enk.design.glass.GlassButton
import app.enk.design.motion.PressDepth
import app.enk.design.motion.pressFeedback
import app.enk.design.shell.GroupedRow
import app.enk.design.shell.LocalReduceEffects
import app.enk.design.shell.ThemeTile
import app.enk.design.theme.EnkShellTheme
import app.enk.design.theme.ThemeId
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class PressEverywhereTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun glassButtonRegistersSingleClickAndPressFeedback() {
        var clicks = 0
        composeTestRule.setContent {
            CompositionLocalProvider(LocalReduceEffects provides false) {
                EnkShellTheme {
                    GlassButton(
                        onClick = { clicks++ },
                        modifier = Modifier.testTag("test_glass_button")
                    ) {
                        Text("Action")
                    }
                }
            }
        }
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithTag("test_glass_button").performClick()
        assertEquals(1, clicks)

        composeTestRule.onNodeWithTag("test_glass_button").performTouchInput { down(center) }
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("test_glass_button").performTouchInput { up() }
        composeTestRule.waitForIdle()
        assertEquals(2, clicks)
    }

    @Test
    fun paperPillPressFeedbackClickCountIsOne() {
        var clicks = 0
        composeTestRule.setContent {
            CompositionLocalProvider(LocalReduceEffects provides false) {
                EnkShellTheme {
                    Box(
                        modifier = Modifier
                            .size(120.dp, 48.dp)
                            .testTag("test_paper_pill")
                            .pressFeedback(depth = PressDepth.Pill)
                            .clickable { clicks++ }
                    )
                }
            }
        }
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithTag("test_paper_pill").performClick()
        assertEquals(1, clicks)
    }

    @Test
    fun noteCardPressFeedbackClickCountIsOne() {
        var clicks = 0
        val note = NoteSummary("n1", "Test note\nBody text", 1, 1000L)
        composeTestRule.setContent {
            CompositionLocalProvider(LocalReduceEffects provides false) {
                EnkShellTheme {
                    NoteCard(
                        summary = note,
                        onClick = { clicks++ },
                        onHold = {},
                        lifted = false
                    )
                }
            }
        }
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithTag("note_card_n1").performClick()
        assertEquals(1, clicks)
    }

    @Test
    fun groupedRowPressFeedbackClickCountIsOne() {
        var clicks = 0
        composeTestRule.setContent {
            CompositionLocalProvider(LocalReduceEffects provides false) {
                EnkShellTheme {
                    GroupedRow(
                        title = "Setting Row",
                        onClick = { clicks++ },
                        modifier = Modifier.testTag("test_grouped_row")
                    )
                }
            }
        }
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithTag("test_grouped_row").performClick()
        assertEquals(1, clicks)
    }

    @Test
    fun themeTilePressFeedbackClickCountIsOne() {
        var clicks = 0
        composeTestRule.setContent {
            CompositionLocalProvider(LocalReduceEffects provides false) {
                EnkShellTheme {
                    ThemeTile(
                        theme = ThemeId.Graphite,
                        selected = false,
                        onClick = { clicks++ }
                    )
                }
            }
        }
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithTag("theme_tile_graphite").performClick()
        assertEquals(1, clicks)
    }

    @Test
    fun reduceEffectsPressFeedbackFiresClicksInstantlyWithoutAnimation() {
        var buttonClicks = 0
        var pillClicks = 0
        composeTestRule.setContent {
            CompositionLocalProvider(LocalReduceEffects provides true) {
                EnkShellTheme {
                    Column {
                        GlassButton(
                            onClick = { buttonClicks++ },
                            modifier = Modifier.testTag("button_reduced")
                        ) {
                            Text("Button")
                        }
                        Box(
                            modifier = Modifier
                                .size(100.dp, 40.dp)
                                .testTag("pill_reduced")
                                .pressFeedback(depth = PressDepth.Pill)
                                .clickable { pillClicks++ }
                        )
                    }
                }
            }
        }
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithTag("button_reduced").performClick()
        assertEquals(1, buttonClicks)

        composeTestRule.onNodeWithTag("pill_reduced").performClick()
        assertEquals(1, pillClicks)
    }
}
