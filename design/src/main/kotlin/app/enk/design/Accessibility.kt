package app.enk.design

import android.content.Context
import android.provider.Settings
import android.view.accessibility.AccessibilityManager

/** True when the system animator scale is 0, the "remove animations" setting. */
internal fun isReducedMotion(context: Context?): Boolean {
    if (context == null) return false
    return runCatching {
        Settings.Global.getFloat(
            context.contentResolver,
            Settings.Global.ANIMATOR_DURATION_SCALE,
            1f
        ) == 0f
    }.getOrDefault(false)
}

/** True when the system "high contrast text" accessibility setting is on. */
internal fun isHighContrastTextEnabled(context: Context?): Boolean {
    if (context == null) return false
    val am = context.getSystemService(Context.ACCESSIBILITY_SERVICE) as? AccessibilityManager
    val fromAm = runCatching {
        am?.javaClass?.getMethod("isHighTextContrastEnabled")?.invoke(am) as? Boolean
    }.getOrNull()
    if (fromAm != null) return fromAm
    return runCatching {
        Settings.Secure.getInt(context.contentResolver, "high_text_contrast_enabled", 0) == 1
    }.getOrDefault(false)
}
