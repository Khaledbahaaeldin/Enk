package app.enk.writer

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import app.enk.settings.Settings
import app.enk.settings.reduceMotion

/** True when the user's Reduce effects setting is on or the system has animations switched off. */
@Composable
fun rememberReduceMotion(reduceEffects: Boolean): Boolean {
    val context = LocalContext.current
    return remember(context, reduceEffects) {
        reduceMotion(context, Settings(reduceEffects = reduceEffects))
    }
}
