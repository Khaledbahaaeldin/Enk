package app.enk.shell

import androidx.compose.animation.core.Animatable
import androidx.compose.runtime.staticCompositionLocalOf

/**
 * The nav lens's position, shared by every tab screen's bar.
 *
 * Each tab screen draws its own nav bar, and a tab change cross-fades two screens, so a lens kept
 * inside one bar would simply dissolve from the old tab to the new one. Sharing the position means
 * the outgoing and the incoming bar both draw the lens at the same place, and the lens visibly
 * slides across while the content dissolves.
 */
class NavLensState {
    private var anim: Animatable<Float, *>? = null
    private var fresh = true

    /**
     * The shared position (px from the bar's left). The first bar after the app starts, or after
     * the writer was on screen (no bar was visible), gets a new position placed at [targetLeft],
     * so the lens never slides across a bar that is only just appearing.
     */
    fun position(targetLeft: Float): Animatable<Float, *> {
        val current = anim
        if (current == null || fresh) {
            val created = Animatable(targetLeft)
            anim = created
            fresh = false
            return created
        }
        return current
    }

    /** Call when a screen without the bar (the writer) is on screen. */
    fun forgetPosition() {
        fresh = true
    }
}

val LocalNavLens = staticCompositionLocalOf<NavLensState?> { null }
