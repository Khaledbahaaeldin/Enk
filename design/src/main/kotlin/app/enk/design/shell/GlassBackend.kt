package app.enk.design.shell

import android.content.Context
import android.provider.Settings
import android.view.accessibility.AccessibilityManager
import androidx.compose.runtime.compositionLocalOf

enum class GlassLevel { Regular, Thick }

enum class GlassBackendKind { Refraction, Blur, Solid }

val LocalReduceEffects = compositionLocalOf { false }

fun chooseGlassBackend(
    sdkInt: Int,
    reduceEffects: Boolean,
    highContrastText: Boolean
): GlassBackendKind {
    return if (reduceEffects || highContrastText) {
        GlassBackendKind.Solid
    } else if (sdkInt >= 33) {
        GlassBackendKind.Refraction
    } else {
        GlassBackendKind.Blur
    }
}

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
