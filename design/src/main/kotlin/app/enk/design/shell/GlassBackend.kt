package app.enk.design.shell

import android.os.Build
import androidx.compose.runtime.Composable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import app.enk.design.effects.LocalEffects
import app.enk.design.effects.ResolvedEffects
import app.enk.design.isHighContrastTextEnabled

enum class GlassLevel { Regular, Thick }

enum class GlassBackendKind { Refraction, Blur, Solid }

val LocalReduceEffects = compositionLocalOf { false }

fun resolveGlassBackend(
    level: ResolvedEffects,
    sdkInt: Int,
    highContrastText: Boolean
): GlassBackendKind {
    return if (highContrastText || level == ResolvedEffects.Light) {
        GlassBackendKind.Solid
    } else if (level == ResolvedEffects.Balanced || sdkInt < 33) {
        GlassBackendKind.Blur
    } else {
        GlassBackendKind.Refraction
    }
}

/** The backend every glass-like surface uses, read once per composition from the system settings. */
@Composable
fun rememberGlassBackend(): GlassBackendKind {
    val context = LocalContext.current
    val level = LocalEffects.current
    return remember(context, level) {
        resolveGlassBackend(
            level = level,
            sdkInt = Build.VERSION.SDK_INT,
            highContrastText = isHighContrastTextEnabled(context)
        )
    }
}
