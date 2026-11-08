package app.enk.design.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.compositeOver

object DynamicShellColors {

    fun from(scheme: ColorScheme, dark: Boolean): ShellColors {
        val bg = scheme.surface
        val g1 = if (dark) scheme.surfaceContainer else scheme.surfaceContainerLow
        val thick = if (dark) {
            scheme.surfaceContainerHigh.copy(alpha = 0.70f)
        } else {
            scheme.surfaceContainerLowest.copy(alpha = 0.78f)
        }
        val hair = scheme.outlineVariant.copy(alpha = 0.50f)
        var t1 = scheme.onSurface
        var t2 = scheme.onSurfaceVariant
        var t3 = scheme.onSurfaceVariant.copy(alpha = 0.65f)
        var ui = scheme.primary
        var uiInk = scheme.onPrimary
        var danger = scheme.error
        val auraTop = scheme.primary.copy(alpha = if (dark) 0.30f else 0.38f)
        val auraSide = scheme.tertiaryContainer.copy(alpha = if (dark) 0.50f else 0.60f)
        val auraBottom = scheme.primaryContainer.copy(alpha = if (dark) 0.07f else 0.80f)

        val towardLightForSurface = dark

        // Ensure t1 on bg and g1 >= 4.5
        while (Contrast.ratio(t1, bg) < 4.5 || Contrast.ratio(t1, g1) < 4.5) {
            val prev = t1
            if (Contrast.ratio(t1, bg) < 4.5) t1 = Contrast.ensure(t1, bg, 4.5, towardLightForSurface)
            if (Contrast.ratio(t1, g1) < 4.5) t1 = Contrast.ensure(t1, g1, 4.5, towardLightForSurface)
            if (t1 == prev) break
        }

        // Ensure t2 on bg and g1 >= 4.5
        while (Contrast.ratio(t2, bg) < 4.5 || Contrast.ratio(t2, g1) < 4.5) {
            val prev = t2
            if (Contrast.ratio(t2, bg) < 4.5) t2 = Contrast.ensure(t2, bg, 4.5, towardLightForSurface)
            if (Contrast.ratio(t2, g1) < 4.5) t2 = Contrast.ensure(t2, g1, 4.5, towardLightForSurface)
            if (t2 == prev) break
        }

        // Ensure t3 on bg >= 3.0
        t3 = Contrast.ensure(t3, bg, 3.0, towardLightForSurface)

        // Ensure ui on g1 >= 3.0
        ui = Contrast.ensure(ui, g1, 3.0, towardLightForSurface)

        // Ensure uiInk on ui >= 4.5
        val towardLightForUi = Contrast.ratio(Color.White, ui) > Contrast.ratio(Color.Black, ui)
        uiInk = Contrast.ensure(uiInk, ui, 4.5, towardLightForUi)

        // Ensure danger on g1 >= 4.5 and on thick (composited on bg) >= 4.5
        val thickBg = thick.compositeOver(bg)
        while (Contrast.ratio(danger, g1) < 4.5 || Contrast.ratio(danger, thickBg) < 4.5) {
            val prev = danger
            if (Contrast.ratio(danger, g1) < 4.5) danger = Contrast.ensure(danger, g1, 4.5, towardLightForSurface)
            if (Contrast.ratio(danger, thickBg) < 4.5) danger = Contrast.ensure(danger, thickBg, 4.5, towardLightForSurface)
            if (danger == prev) break
        }

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
}
