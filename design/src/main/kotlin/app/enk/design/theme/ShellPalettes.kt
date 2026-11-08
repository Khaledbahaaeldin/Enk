package app.enk.design.theme

import androidx.compose.ui.graphics.Color

object ShellPalettes {

    fun colors(theme: ThemeId, dark: Boolean): ShellColors {
        require(theme != ThemeId.Dynamic) {
            "Dynamic theme cannot be resolved from static palettes"
        }
        return if (dark) {
            when (theme) {
                ThemeId.Graphite -> ShellColors(
                    bg = Color(0xFF161514),
                    g1 = Color(0xFF201F1D),
                    thick = rgba(38, 36, 34, 0.66f),
                    hair = rgba(255, 255, 255, 0.08f),
                    t1 = Color(0xFFF3EEE3),
                    t2 = Color(0xFFA9A398),
                    t3 = Color(0xFF77726A),
                    ui = Color(0xFFFF5A1F),
                    uiInk = Color(0xFF1A0A03),
                    danger = Color(0xFFFF8A7D),
                    auraTop = rgba(255, 190, 130, 0.09f),
                    auraSide = Color.Transparent,
                    auraBottom = Color.Transparent
                )
                ThemeId.Apricot -> ShellColors(
                    bg = Color(0xFF1C120F),
                    g1 = Color(0xFF2A1A15),
                    thick = rgba(46, 28, 22, 0.70f),
                    hair = rgba(255, 218, 185, 0.10f),
                    t1 = Color(0xFFFFDAB9),
                    t2 = Color(0xFFC9A58B),
                    t3 = Color(0xFF8F6F5D),
                    ui = Color(0xFFE2725B),
                    uiInk = Color(0xFF2A1410),
                    danger = Color(0xFFFF9A86),
                    auraTop = rgba(226, 114, 91, 0.30f),
                    auraSide = rgba(78, 44, 35, 0.85f),
                    auraBottom = rgba(255, 218, 185, 0.07f)
                )
                ThemeId.Sky -> ShellColors(
                    bg = Color(0xFF1B1413),
                    g1 = Color(0xFF2A201E),
                    thick = rgba(42, 32, 31, 0.70f),
                    hair = rgba(193, 219, 232, 0.10f),
                    t1 = Color(0xFFFBF1C9),
                    t2 = Color(0xFFB7C8D2),
                    t3 = Color(0xFF7D8A92),
                    ui = Color(0xFFC1DBE8),
                    uiInk = Color(0xFF1B1413),
                    danger = Color(0xFFFF9D8F),
                    auraTop = rgba(193, 219, 232, 0.26f),
                    auraSide = rgba(67, 47, 46, 0.90f),
                    auraBottom = rgba(255, 240, 181, 0.10f)
                )
                ThemeId.Slate -> ShellColors(
                    bg = Color(0xFF0D0F11),
                    g1 = Color(0xFF171B1F),
                    thick = rgba(26, 31, 36, 0.70f),
                    hair = rgba(229, 228, 226, 0.09f),
                    t1 = Color(0xFFE5E4E2),
                    t2 = Color(0xFF9AA5AD),
                    t3 = Color(0xFF66727B),
                    ui = Color(0xFF8FA6B8),
                    uiInk = Color(0xFF0A0A0A),
                    danger = Color(0xFFFF948A),
                    auraTop = rgba(83, 104, 120, 0.40f),
                    auraSide = rgba(10, 10, 10, 0.90f),
                    auraBottom = rgba(229, 228, 226, 0.05f)
                )
                ThemeId.Mono -> ShellColors(
                    bg = Color(0xFF0B0B0B),
                    g1 = Color(0xFF1A1A1A),
                    thick = rgba(30, 30, 30, 0.70f),
                    hair = rgba(255, 255, 255, 0.09f),
                    t1 = Color(0xFFF2F2F2),
                    t2 = Color(0xFF9C9C9C),
                    t3 = Color(0xFF6A6A6A),
                    ui = Color(0xFFF2F2F2),
                    uiInk = Color(0xFF0B0B0B),
                    danger = Color(0xFFFF8A80),
                    auraTop = rgba(255, 255, 255, 0.06f),
                    auraSide = rgba(255, 255, 255, 0.03f),
                    auraBottom = rgba(255, 255, 255, 0.03f)
                )
                ThemeId.Dynamic -> error("Unreachable")
            }
        } else {
            when (theme) {
                ThemeId.Graphite -> ShellColors(
                    bg = Color(0xFFECE8DF),
                    g1 = Color(0xFFF7F4EC),
                    thick = rgba(247, 244, 236, 0.78f),
                    hair = rgba(43, 40, 36, 0.12f),
                    t1 = Color(0xFF2B2824),
                    t2 = Color(0xFF6B665C),
                    t3 = Color(0xFF8A8375),
                    ui = Color(0xFFEE5416),
                    uiInk = Color(0xFF1A0A03),
                    danger = Color(0xFFB3261E),
                    auraTop = rgba(255, 150, 90, 0.20f),
                    auraSide = rgba(255, 255, 255, 0.45f),
                    auraBottom = rgba(200, 190, 170, 0.45f)
                )
                ThemeId.Apricot -> ShellColors(
                    bg = Color(0xFFFFEBDC),
                    g1 = Color(0xFFFFF7F0),
                    thick = rgba(255, 244, 235, 0.76f),
                    hair = rgba(78, 44, 35, 0.12f),
                    t1 = Color(0xFF4E2C23),
                    t2 = Color(0xFF85584A),
                    t3 = Color(0xFFA97D6A),
                    ui = Color(0xFFD4634C),
                    uiInk = Color(0xFF2A1410),
                    danger = Color(0xFFB3261E),
                    auraTop = rgba(226, 114, 91, 0.38f),
                    auraSide = rgba(255, 218, 185, 0.95f),
                    auraBottom = rgba(255, 218, 185, 0.90f)
                )
                ThemeId.Sky -> ShellColors(
                    bg = Color(0xFFFFF6D4),
                    g1 = Color(0xFFFFFBEA),
                    thick = rgba(255, 251, 234, 0.78f),
                    hair = rgba(67, 47, 46, 0.12f),
                    t1 = Color(0xFF432F2E),
                    t2 = Color(0xFF7A6361),
                    t3 = Color(0xFF9F8984),
                    ui = Color(0xFF5E92B2),
                    uiInk = Color(0xFF1B1413),
                    danger = Color(0xFFB3261E),
                    auraTop = rgba(193, 219, 232, 0.95f),
                    auraSide = rgba(193, 219, 232, 0.60f),
                    auraBottom = rgba(255, 240, 181, 0.95f)
                )
                ThemeId.Slate -> ShellColors(
                    bg = Color(0xFFE5E4E2),
                    g1 = Color(0xFFF3F2F0),
                    thick = rgba(243, 242, 240, 0.78f),
                    hair = rgba(10, 10, 10, 0.10f),
                    t1 = Color(0xFF0A0A0A),
                    t2 = Color(0xFF4F5A63),
                    t3 = Color(0xFF758088),
                    ui = Color(0xFF536878),
                    uiInk = Color(0xFFFFFFFF),
                    danger = Color(0xFFB3261E),
                    auraTop = rgba(83, 104, 120, 0.34f),
                    auraSide = rgba(83, 104, 120, 0.20f),
                    auraBottom = rgba(255, 255, 255, 0.70f)
                )
                ThemeId.Mono -> ShellColors(
                    bg = Color(0xFFEEEEEE),
                    g1 = Color(0xFFFAFAFA),
                    thick = rgba(250, 250, 250, 0.78f),
                    hair = rgba(0, 0, 0, 0.10f),
                    t1 = Color(0xFF111111),
                    t2 = Color(0xFF6B6B6B),
                    t3 = Color(0xFF868686),
                    ui = Color(0xFF111111),
                    uiInk = Color(0xFFFFFFFF),
                    danger = Color(0xFFB3261E),
                    auraTop = rgba(255, 255, 255, 0.80f),
                    auraSide = rgba(0, 0, 0, 0.03f),
                    auraBottom = rgba(255, 255, 255, 0.60f)
                )
                ThemeId.Dynamic -> error("Unreachable")
            }
        }
    }

    fun material(dark: Boolean): ShellMaterialTokens = if (dark) {
        ShellMaterialTokens(
            glassA = rgba(255, 255, 255, 0.12f),
            glassB = rgba(255, 255, 255, 0.05f),
            glassEdge = rgba(255, 255, 255, 0.42f),
            glassLine = rgba(255, 255, 255, 0.10f),
            sh1 = rgba(0, 0, 0, 0.30f),
            sh2 = rgba(0, 0, 0, 0.50f),
            lensA = rgba(255, 255, 255, 0.22f),
            lensB = rgba(255, 255, 255, 0.11f),
            lensEdge = rgba(255, 255, 255, 0.55f),
            track = rgba(255, 255, 255, 0.12f),
            field = rgba(255, 255, 255, 0.08f),
            field2 = rgba(0, 0, 0, 0.28f),
            scrim = rgba(8, 7, 6, 0.50f),
            grab = rgba(255, 255, 255, 0.28f),
            cardShadow = rgba(0, 0, 0, 0.50f),
            auraGrain = 0.10f
        )
    } else {
        ShellMaterialTokens(
            glassA = rgba(255, 255, 255, 0.66f),
            glassB = rgba(255, 255, 255, 0.36f),
            glassEdge = rgba(255, 255, 255, 0.95f),
            glassLine = rgba(0, 0, 0, 0.06f),
            sh1 = rgba(60, 40, 20, 0.14f),
            sh2 = rgba(60, 40, 20, 0.22f),
            lensA = rgba(255, 255, 255, 0.98f),
            lensB = rgba(255, 255, 255, 0.80f),
            lensEdge = Color(1f, 1f, 1f, 1.0f),
            track = rgba(0, 0, 0, 0.13f),
            field = rgba(0, 0, 0, 0.055f),
            field2 = rgba(0, 0, 0, 0.06f),
            scrim = rgba(20, 12, 6, 0.30f),
            grab = rgba(0, 0, 0, 0.22f),
            cardShadow = rgba(70, 40, 20, 0.28f),
            auraGrain = 0.16f
        )
    }

    private fun rgba(r: Int, g: Int, b: Int, a: Float): Color =
        Color(red = r / 255f, green = g / 255f, blue = b / 255f, alpha = a)
}
