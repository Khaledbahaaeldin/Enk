package app.enk.design.shell

import android.os.Build
import androidx.compose.runtime.Composable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import app.enk.design.isHighContrastTextEnabled
import app.enk.design.isReducedMotion

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

/** The backend every glass-like surface uses, read once per composition from the system settings. */
@Composable
fun rememberGlassBackend(): GlassBackendKind {
    val context = LocalContext.current
    val reduceEffects = LocalReduceEffects.current
    return remember(context, reduceEffects) {
        chooseGlassBackend(
            sdkInt = Build.VERSION.SDK_INT,
            reduceEffects = reduceEffects || isReducedMotion(context),
            highContrastText = isHighContrastTextEnabled(context)
        )
    }
}
