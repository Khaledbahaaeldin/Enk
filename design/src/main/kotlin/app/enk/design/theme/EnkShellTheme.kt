package app.enk.design.theme

import android.content.Context
import android.os.Build
import android.provider.Settings
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.AnimationSpec
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.tween
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ProvidableCompositionLocal
import androidx.compose.runtime.getValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

val LocalShellColors: ProvidableCompositionLocal<ShellColors> = staticCompositionLocalOf {
    ShellPalettes.colors(ThemeId.Graphite, dark = true)
}

val LocalShellMaterial: ProvidableCompositionLocal<ShellMaterialTokens> = staticCompositionLocalOf {
    ShellPalettes.material(dark = true)
}

@Composable
fun EnkShellTheme(
    theme: ThemeId,
    mode: ThemeMode,
    content: @Composable () -> Unit
) {
    val isDark = when (mode) {
        ThemeMode.Light -> false
        ThemeMode.Dark -> true
        ThemeMode.System -> isSystemInDarkTheme()
    }

    val targetColors = if (theme == ThemeId.Dynamic) {
        val context = LocalContext.current
        val scheme = resolveDynamicColorScheme(isDark, context)
        DynamicShellColors.from(scheme, isDark)
    } else {
        ShellPalettes.colors(theme, isDark)
    }

    val targetMaterial = ShellPalettes.material(isDark)

    val reducedMotion = isReducedMotionEnabled()
    val animSpec: AnimationSpec<Color> = if (reducedMotion) {
        snap()
    } else {
        tween(durationMillis = 500)
    }

    val animatedColors = animateShellColors(targetColors, animSpec)

    CompositionLocalProvider(
        LocalShellColors provides animatedColors,
        LocalShellMaterial provides targetMaterial,
        content = content
    )
}

private fun resolveDynamicColorScheme(isDark: Boolean, context: Context): ColorScheme {
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        runCatching {
            if (isDark) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }.getOrElse {
            if (isDark) darkColorScheme() else lightColorScheme()
        }
    } else {
        if (isDark) darkColorScheme() else lightColorScheme()
    }
}

@Composable
private fun isReducedMotionEnabled(): Boolean {
    val context = runCatching { LocalContext.current }.getOrNull() ?: return false
    return runCatching {
        Settings.Global.getFloat(
            context.contentResolver,
            Settings.Global.ANIMATOR_DURATION_SCALE,
            1f
        ) == 0f
    }.getOrDefault(false)
}

@Composable
private fun animateShellColors(colors: ShellColors, animSpec: AnimationSpec<Color>): ShellColors {
    val bg by animateColorAsState(colors.bg, animSpec, label = "bg")
    val g1 by animateColorAsState(colors.g1, animSpec, label = "g1")
    val thick by animateColorAsState(colors.thick, animSpec, label = "thick")
    val hair by animateColorAsState(colors.hair, animSpec, label = "hair")
    val t1 by animateColorAsState(colors.t1, animSpec, label = "t1")
    val t2 by animateColorAsState(colors.t2, animSpec, label = "t2")
    val t3 by animateColorAsState(colors.t3, animSpec, label = "t3")
    val ui by animateColorAsState(colors.ui, animSpec, label = "ui")
    val uiInk by animateColorAsState(colors.uiInk, animSpec, label = "uiInk")
    val danger by animateColorAsState(colors.danger, animSpec, label = "danger")
    val auraTop by animateColorAsState(colors.auraTop, animSpec, label = "auraTop")
    val auraSide by animateColorAsState(colors.auraSide, animSpec, label = "auraSide")
    val auraBottom by animateColorAsState(colors.auraBottom, animSpec, label = "auraBottom")
    return ShellColors(
        bg = bg,
        g1 = g1,
        thick = thick,
        hair = hair,
        t1 = t1,
        t2 = t2,
        t3 = t3,
        ui = ui,
        uiInk = uiInk,
        danger = danger,
        auraTop = auraTop,
        auraSide = auraSide,
        auraBottom = auraBottom
    )
}
