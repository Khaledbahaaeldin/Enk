package app.enk.writer

import app.enk.design.theme.ThemeMode
import app.enk.settings.DarkPaper

/** Decides when the writer's lamp follows the app's dark mode, and when it asks. */
object DarkPaperPolicy {

    /** Whether the app is dark: its own mode if the user fixed one, otherwise the system's. */
    fun isDark(mode: ThemeMode, systemDark: Boolean): Boolean = when (mode) {
        ThemeMode.Light -> false
        ThemeMode.Dark -> true
        ThemeMode.System -> systemDark
    }

    /**
     * The lamp state to apply on its own, or null when the user's own toggling stands.
     * Only "Lamp on" follows dark mode, in both directions, so a phone that goes back to light
     * does not leave a dark writer in daylight.
     */
    fun autoLamp(paper: DarkPaper, dark: Boolean): Boolean? =
        if (paper == DarkPaper.Lamp) dark else null

    /** The one-time question appears only while the choice is still open and the app is dark. */
    fun shouldAsk(paper: DarkPaper, dark: Boolean): Boolean = paper == DarkPaper.Ask && dark
}
