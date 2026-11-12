package app.enk.settings

import app.enk.design.theme.ThemeId
import app.enk.design.theme.ThemeMode

enum class CorrectionMode {
    CleanErase,
    StrikeThrough
}

enum class SoundSetId {
    Classic,
    Antique,
    Soft,
    Keyboard
}

/** What the writer's paper does when the app is in dark mode. */
enum class DarkPaper {
    /** Not chosen yet: the writer offers the lamp once. */
    Ask,
    Normal,
    Lamp
}

data class Settings(
    val correction: CorrectionMode = CorrectionMode.CleanErase,
    val autocorrect: Boolean = false,
    val soundSet: SoundSetId = SoundSetId.Classic,
    val muted: Boolean = false,
    val bellVolume: Float = 0.6f,
    val haptics: Boolean = true,
    val reduceEffects: Boolean = false,
    val theme: ThemeId = ThemeId.Graphite,
    val themeMode: ThemeMode = ThemeMode.System,
    val warmth: Int = 25,
    val intensity: Int = 80,
    val beam: Int = 55,
    val hintDismissed: Boolean = false,
    val darkPaper: DarkPaper = DarkPaper.Ask
)
