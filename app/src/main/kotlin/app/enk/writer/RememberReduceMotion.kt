package app.enk.writer

import android.provider.Settings as AndroidSettings
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext

/** True when the user's Reduce effects setting is on or the system has animations switched off. */
@Composable
fun rememberReduceMotion(reduceEffects: Boolean): Boolean {
    val context = LocalContext.current
    val animatorDurationScale = remember(context) {
        try {
            AndroidSettings.Global.getFloat(
                context.contentResolver,
                AndroidSettings.Global.ANIMATOR_DURATION_SCALE,
                1.0f
            )
        } catch (_: Exception) {
            1.0f
        }
    }
    return remember(reduceEffects, animatorDurationScale) {
        MotionPolicy.shouldReduceMotion(reduceEffects, animatorDurationScale)
    }
}
