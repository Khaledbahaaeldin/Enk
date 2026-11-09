package app.enk.writer

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.test.junit4.createComposeRule
import app.enk.design.EnkColors
import app.enk.design.LampColor
import app.enk.design.LampSettings
import app.enk.settings.Settings
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class NightThemeTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun paperColorDayNightAndMidpoint() {
        // paperColor(0f) == EnkColors.paperDay, paperColor(1f) == EnkColors.paperNight, paperColor(0.5f) is midpoint
        assertEquals(EnkColors.paperDay, paperColor(0f))
        assertEquals(EnkColors.paperNight, paperColor(1f))

        val mid = paperColor(0.5f)
        val expectedRed = (EnkColors.paperDay.red + EnkColors.paperNight.red) / 2f
        val expectedGreen = (EnkColors.paperDay.green + EnkColors.paperNight.green) / 2f
        val expectedBlue = (EnkColors.paperDay.blue + EnkColors.paperNight.blue) / 2f
        assertEquals(expectedRed, mid.red, 0.01f)
        assertEquals(expectedGreen, mid.green, 0.01f)
        assertEquals(expectedBlue, mid.blue, 0.01f)
    }

    @Test
    fun textColorInkLampAndMidpoint() {
        val lamp = LampColor.fromWarmth(25)
        val lampColor = Color(lamp.r, lamp.g, lamp.b)

        assertEquals(EnkColors.ink, textColor(0f, lamp))
        assertEquals(lampColor, textColor(1f, lamp))

        val mid = textColor(0.5f, lamp)
        val expectedRed = (EnkColors.ink.red + lampColor.red) / 2f
        val expectedGreen = (EnkColors.ink.green + lampColor.green) / 2f
        val expectedBlue = (EnkColors.ink.blue + lampColor.blue) / 2f
        assertEquals(expectedRed, mid.red, 0.01f)
        assertEquals(expectedGreen, mid.green, 0.01f)
        assertEquals(expectedBlue, mid.blue, 0.01f)
    }

    @Test
    fun grainOpacityValues() {
        assertEquals(0.35f, grainOpacity(0f), 0.001f)
        assertEquals(0.10f, grainOpacity(1f), 0.001f)
        assertEquals(0.225f, grainOpacity(0.5f), 0.001f)

        // Clamped bounds
        assertEquals(0.35f, grainOpacity(-0.5f), 0.001f)
        assertEquals(0.10f, grainOpacity(1.5f), 0.001f)
    }

    @Test
    fun lampSettingsFromSettings() {
        val settings = Settings(warmth = 30, intensity = 70, beam = 60)
        val lampSettings: LampSettings = lampSettingsFromSettings(settings)
        assertEquals(30, lampSettings.warmth)
        assertEquals(70, lampSettings.intensity)
        assertEquals(60, lampSettings.beam)
    }

    @Test
    fun rememberNightProgressAnimates() {
        var lampOn by mutableStateOf(false)
        var progress = -1f

        composeTestRule.setContent {
            val progressState = rememberNightProgress(lampOn = lampOn, reduceEffects = false)
            progress = progressState.value
        }

        assertEquals(0f, progress, 0.001f)

        lampOn = true
        composeTestRule.waitForIdle()
        assertEquals(1f, progress, 0.001f)

        lampOn = false
        composeTestRule.waitForIdle()
        assertEquals(0f, progress, 0.001f)
    }

    @Test
    fun rememberNightProgressReduceEffects() {
        var lampOn by mutableStateOf(false)
        var progress = -1f

        composeTestRule.setContent {
            val progressState = rememberNightProgress(lampOn = lampOn, reduceEffects = true)
            progress = progressState.value
        }

        assertEquals(0f, progress, 0.001f)

        lampOn = true
        composeTestRule.waitForIdle()
        assertEquals(1f, progress, 0.001f)
    }
}
