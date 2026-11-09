package app.enk.shell

import android.content.Context
import android.content.ContextWrapper
import androidx.activity.ComponentActivity
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

/** True when the surface behind the system bars is dark, so the bars need light icons. */
fun isDarkSurface(background: Color): Boolean = background.luminance() < 0.5f

/**
 * Keeps the status and navigation bar icons readable on the current screen. The writer sits on a
 * dark graphite chassis in day and night; shell screens follow their theme, so the system's own
 * choice (made once, from the system's mode) is wrong whenever the two disagree.
 */
@Composable
fun SystemBarIcons(onDarkSurface: Boolean) {
    val view = LocalView.current
    SideEffect {
        val window = view.context.findActivity()?.window ?: return@SideEffect
        val controller = WindowCompat.getInsetsController(window, view)
        controller.isAppearanceLightStatusBars = !onDarkSurface
        controller.isAppearanceLightNavigationBars = !onDarkSurface
    }
}

private fun Context.findActivity(): ComponentActivity? {
    var context = this
    while (context is ContextWrapper) {
        if (context is ComponentActivity) return context
        context = context.baseContext
    }
    return null
}
