package app.enk.design.theme

import android.content.Context
import android.os.Build
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
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import app.enk.design.isReducedMotion

const val THEME_TRANSITION_MS = 500

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

    val context = LocalContext.current

    val targetColors = if (theme == ThemeId.Dynamic) {
        val scheme = resolveDynamicColorScheme(isDark, context)
        DynamicShellColors.from(scheme, isDark)
    } else {
        ShellPalettes.colors(theme, isDark)
    }

    val targetMaterial = ShellPalettes.material(isDark)

    val reducedMotion = remember(context) { isReducedMotion(context) }
    val animSpec: AnimationSpec<Color> = if (reducedMotion) {
        snap()
    } else {
        tween(durationMillis = THEME_TRANSITION_MS)
    }

    val animatedColors = animateShellColors(targetColors, animSpec)
    val animatedMaterial = animateShellMaterial(targetMaterial, animSpec)

    CompositionLocalProvider(
        LocalShellColors provides animatedColors,
        LocalShellMaterial provides animatedMaterial,
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

@Composable
private fun animateShellMaterial(
    material: ShellMaterialTokens,
    animSpec: AnimationSpec<Color>
): ShellMaterialTokens {
    val glassA by animateColorAsState(material.glassA, animSpec, label = "glassA")
    val glassB by animateColorAsState(material.glassB, animSpec, label = "glassB")
    val glassEdge by animateColorAsState(material.glassEdge, animSpec, label = "glassEdge")
    val glassLine by animateColorAsState(material.glassLine, animSpec, label = "glassLine")
    val sh1 by animateColorAsState(material.sh1, animSpec, label = "sh1")
    val sh2 by animateColorAsState(material.sh2, animSpec, label = "sh2")
    val lensA by animateColorAsState(material.lensA, animSpec, label = "lensA")
    val lensB by animateColorAsState(material.lensB, animSpec, label = "lensB")
    val lensEdge by animateColorAsState(material.lensEdge, animSpec, label = "lensEdge")
    val track by animateColorAsState(material.track, animSpec, label = "track")
    val field by animateColorAsState(material.field, animSpec, label = "field")
    val field2 by animateColorAsState(material.field2, animSpec, label = "field2")
    val scrim by animateColorAsState(material.scrim, animSpec, label = "scrim")
    val grab by animateColorAsState(material.grab, animSpec, label = "grab")
    val cardShadow by animateColorAsState(material.cardShadow, animSpec, label = "cardShadow")
    return ShellMaterialTokens(
        glassA = glassA,
        glassB = glassB,
        glassEdge = glassEdge,
        glassLine = glassLine,
        sh1 = sh1,
        sh2 = sh2,
        lensA = lensA,
        lensB = lensB,
        lensEdge = lensEdge,
        track = track,
        field = field,
        field2 = field2,
        scrim = scrim,
        grab = grab,
        cardShadow = cardShadow,
        auraGrain = material.auraGrain
    )
}
