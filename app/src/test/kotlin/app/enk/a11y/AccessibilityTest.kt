package app.enk.a11y

import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertContentDescriptionEquals
import androidx.compose.ui.test.assertHeightIsAtLeast
import androidx.compose.ui.test.assertRangeInfoEquals
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import app.enk.design.Fader
import app.enk.design.LocalMonoFont
import app.enk.settings.Settings
import app.enk.settings.reduceMotion
import app.enk.text.StruckPage
import app.enk.writer.Carriage
import app.enk.writer.CarriageMotion
import app.enk.writer.PaperText
import app.enk.writer.WriterGeometry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class AccessibilityTest {

    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun reduceMotionTrueWhenAnimatorScaleZero() {
        assertTrue(reduceMotion(settingsReduceEffects = false, animatorDurationScale = 0f))
        assertTrue(reduceMotion(settingsReduceEffects = true, animatorDurationScale = 1f))
        assertTrue(reduceMotion(settingsReduceEffects = true, animatorDurationScale = 0f))
        assertFalse(reduceMotion(settingsReduceEffects = false, animatorDurationScale = 1f))
    }

    @Test
    fun carriageHasMarkerDescriptionAnd48DpTarget() {
        composeRule.setContent {
            Carriage(
                railYDp = 200f,
                centerXDp = 100f,
                mode = CarriageMotion.Rest,
                slugChar = null,
                strikeKey = 0,
                night = false,
                reduceEffects = false
            )
        }

        composeRule.onNodeWithTag("carriage")
            .assertContentDescriptionEquals("Writing marker. Drag to scrub, long press to toggle lamp")
            .assertHeightIsAtLeast(48.dp)
    }

    @Test
    fun faderExposesRangeAndAction() {
        composeRule.setContent {
            Fader(
                label = "Warmth",
                value = 25,
                min = 0,
                max = 100,
                lampColor = Color.Yellow,
                onValueChange = {},
                onDetent = {}
            )
        }

        composeRule.onNodeWithContentDescription("Warmth")
            .assertRangeInfoEquals(ProgressBarRangeInfo(current = 25f, range = 0f..100f, steps = 20))
            .assert(SemanticsMatcher.keyIsDefined(SemanticsActions.SetProgress))
    }

    @Test
    fun paperExposesPageTextToTalkBack() {
        val testText = "The quick brown fox jumps over the lazy dog."
        val page = StruckPage(testText)
        val geometry = WriterGeometry.compute(widthDp = 400f, heightDp = 800f, fontScale = 1f)

        composeRule.setContent {
            CompositionLocalProvider(LocalMonoFont provides FontFamily.Monospace) {
                PaperText(
                    page = page,
                    caret = 0,
                    lastTypedIndex = null,
                    geometry = geometry,
                    textColor = Color.Black,
                    reduceMotion = false,
                    modifier = Modifier.size(400.dp, 800.dp)
                )
            }
        }

        val textMatcher = SemanticsMatcher("Has page text") { node ->
            val editable = node.config.getOrNull(SemanticsProperties.EditableText)?.text
            val normalText = node.config.getOrNull(SemanticsProperties.Text)?.firstOrNull()?.text
            editable == testText || normalText == testText
        }

        composeRule.onNode(textMatcher).assertExists()
    }

    @Test
    fun geometryAt130And200FontScale() {
        for (scale in listOf(1.3f, 2.0f)) {
            val geometry = WriterGeometry.compute(widthDp = 360f, heightDp = 640f, fontScale = scale)
            assertTrue(geometry.cols in 18..28)
            assertTrue(geometry.fontSp in 1f..22f)
            assertTrue(geometry.railYDp >= WriterGeometry.MIN_RAIL_Y_DP)
        }
    }
}
