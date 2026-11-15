package app.enk.design.theme

import org.junit.Assert.assertEquals
import org.junit.Test

class ThemeTransitionTest {

    @Test
    fun themeTransitionDurationIsExactly500Ms() {
        assertEquals("Theme cross-fade transition must remain 500 ms", 500, THEME_TRANSITION_MS)
    }
}
