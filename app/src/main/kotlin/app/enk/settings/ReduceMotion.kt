package app.enk.settings

import android.content.Context
import android.provider.Settings as AndroidSettings

/**
 * Single source of truth for motion reduction per Task 26:
 * Returns true if the user's `settings.reduceEffects` is true, or if the system animator scale is 0.
 */
fun reduceMotion(context: Context, settings: Settings): Boolean {
    if (settings.reduceEffects) return true
    return isSystemAnimatorScaleZero(context)
}

/**
 * Pure overload for testing or when animator duration scale is already resolved.
 */
fun reduceMotion(settingsReduceEffects: Boolean, animatorDurationScale: Float): Boolean {
    return settingsReduceEffects || animatorDurationScale == 0f
}

internal fun isSystemAnimatorScaleZero(context: Context): Boolean {
    val scale = try {
        AndroidSettings.Global.getFloat(
            context.contentResolver,
            AndroidSettings.Global.ANIMATOR_DURATION_SCALE,
            1.0f
        )
    } catch (_: Exception) {
        1.0f
    }
    return scale == 0f
}
