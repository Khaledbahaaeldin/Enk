package app.enk.writer

import app.enk.design.theme.ThemeMode
import app.enk.settings.DarkPaper
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class DarkPaperPolicyTest {

    @Test
    fun aFixedModeBeatsTheSystem() {
        assertFalse(DarkPaperPolicy.isDark(ThemeMode.Light, systemDark = true))
        assertTrue(DarkPaperPolicy.isDark(ThemeMode.Dark, systemDark = false))
    }

    @Test
    fun systemModeFollowsTheSystem() {
        assertTrue(DarkPaperPolicy.isDark(ThemeMode.System, systemDark = true))
        assertFalse(DarkPaperPolicy.isDark(ThemeMode.System, systemDark = false))
    }

    @Test
    fun lampOnFollowsDarkModeBothWays() {
        assertEquals(true, DarkPaperPolicy.autoLamp(DarkPaper.Lamp, dark = true))
        assertEquals(false, DarkPaperPolicy.autoLamp(DarkPaper.Lamp, dark = false))
    }

    @Test
    fun askAndNormalNeverTouchTheLamp() {
        for (dark in listOf(true, false)) {
            assertNull(DarkPaperPolicy.autoLamp(DarkPaper.Ask, dark))
            assertNull(DarkPaperPolicy.autoLamp(DarkPaper.Normal, dark))
        }
    }

    @Test
    fun theQuestionAppearsOnlyWhileOpenAndDark() {
        assertTrue(DarkPaperPolicy.shouldAsk(DarkPaper.Ask, dark = true))
        assertFalse(DarkPaperPolicy.shouldAsk(DarkPaper.Ask, dark = false))
        assertFalse(DarkPaperPolicy.shouldAsk(DarkPaper.Normal, dark = true))
        assertFalse(DarkPaperPolicy.shouldAsk(DarkPaper.Lamp, dark = true))
    }
}
