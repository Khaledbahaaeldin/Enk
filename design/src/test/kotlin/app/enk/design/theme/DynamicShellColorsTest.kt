package app.enk.design.theme

import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.compositeOver
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.roundToInt

class DynamicShellColorsTest {

    @Test
    fun dynamicMapsPrimaryToUi() {
        val s = darkColorScheme(primary = Color(0xFF7CDB8E), onPrimary = Color(0xFF00390F))
        val c = DynamicShellColors.from(s, true)
        assertEquals(Color(0xFF7CDB8E), c.ui)
    }

    @Test
    fun dynamicMapsTokensDark() {
        val s = darkColorScheme(
            surface = Color(0xFF141218),
            surfaceContainer = Color(0xFF211F26),
            surfaceContainerHigh = Color(0xFF2B2930),
            outlineVariant = Color(0xFF49454F),
            onSurface = Color(0xFFE6E0E9),
            onSurfaceVariant = Color(0xFFCAC4D0),
            primary = Color(0xFFD0BCFF),
            onPrimary = Color(0xFF381E72),
            error = Color(0xFFF2B8B5),
            primaryContainer = Color(0xFF4F378B),
            tertiaryContainer = Color(0xFF4A4458)
        )
        val c = DynamicShellColors.from(s, true)
        assertEquals(s.surface, c.bg)
        assertEquals(s.surfaceContainer, c.g1)
        assertEquals(s.surfaceContainerHigh.copy(alpha = 0.70f), c.thick)
        assertEquals(s.outlineVariant.copy(alpha = 0.50f), c.hair)
        assertEquals(s.primary.copy(alpha = 0.30f), c.auraTop)
        assertEquals(s.tertiaryContainer.copy(alpha = 0.50f), c.auraSide)
        assertEquals(s.primaryContainer.copy(alpha = 0.07f), c.auraBottom)
    }

    @Test
    fun dynamicMapsTokensLight() {
        val s = lightColorScheme(
            surface = Color(0xFFFEF7FF),
            surfaceContainerLow = Color(0xFFF7F2FA),
            surfaceContainerLowest = Color(0xFFFFFFFF),
            outlineVariant = Color(0xFFCAC4D0),
            onSurface = Color(0xFF1D1B20),
            onSurfaceVariant = Color(0xFF49454F),
            primary = Color(0xFF6750A4),
            onPrimary = Color(0xFFFFFFFF),
            error = Color(0xFFB3261E),
            primaryContainer = Color(0xFFEADDFF),
            tertiaryContainer = Color(0xFFFFD8E4)
        )
        val c = DynamicShellColors.from(s, false)
        assertEquals(s.surface, c.bg)
        assertEquals(s.surfaceContainerLow, c.g1)
        assertEquals(s.surfaceContainerLowest.copy(alpha = 0.78f), c.thick)
        assertEquals(s.outlineVariant.copy(alpha = 0.50f), c.hair)
        assertEquals(s.primary.copy(alpha = 0.38f), c.auraTop)
        assertEquals(s.tertiaryContainer.copy(alpha = 0.60f), c.auraSide)
        assertEquals(s.primaryContainer.copy(alpha = 0.80f), c.auraBottom)
    }

    @Test
    fun contrastMinimumsHoldForDynamicSweep() {
        val hues = (0 until 12).map { it * 30f }
        for (hue in hues) {
            // Dark dynamic scheme
            val darkScheme = generateTestScheme(hue, dark = true)
            val darkColors = DynamicShellColors.from(darkScheme, dark = true)
            assertShellContrastMinimums("Dynamic Dark (hue $hue)", darkColors)

            // Light dynamic scheme
            val lightScheme = generateTestScheme(hue, dark = false)
            val lightColors = DynamicShellColors.from(lightScheme, dark = false)
            assertShellContrastMinimums("Dynamic Light (hue $hue)", lightColors)
        }
    }

    @Test
    fun dynamicHandlesExtremeDarkSchemeWithoutCrashing() {
        val extremeDark = darkColorScheme(
            surface = Color.Black,
            surfaceContainer = Color(0xFF050505),
            surfaceContainerHigh = Color(0xFF0A0A0A),
            onSurface = Color.Black, // deliberately identical to surface
            onSurfaceVariant = Color(0xFF050505),
            primary = Color.Black,
            onPrimary = Color.Black,
            error = Color(0xFF100000)
        )
        val c = DynamicShellColors.from(extremeDark, dark = true)
        assertShellContrastMinimums("Extreme Dark", c)
    }

    @Test
    fun dynamicHandlesExtremeLightSchemeWithoutCrashing() {
        val extremeLight = lightColorScheme(
            surface = Color.White,
            surfaceContainerLow = Color(0xFFFAFAFA),
            surfaceContainerLowest = Color.White,
            onSurface = Color.White, // deliberately identical to surface
            onSurfaceVariant = Color(0xFFFAFAFA),
            primary = Color.White,
            onPrimary = Color.White,
            error = Color(0xFFFFFAFA)
        )
        val c = DynamicShellColors.from(extremeLight, dark = false)
        assertShellContrastMinimums("Extreme Light", c)
    }

    @Test
    fun dynamicResolvesLowContrastPrimaryAgainstSurfaceContainer() {
        val lowContrast = darkColorScheme(
            surface = Color(0xFF121212),
            surfaceContainer = Color(0xFF808080),
            primary = Color(0xFF808080),
            onPrimary = Color.Black
        )
        val c = DynamicShellColors.from(lowContrast, dark = true)
        assertTrue("ui on g1 contrast >= 3.0", Contrast.ratio(c.ui, c.g1) >= 3.0)
    }

    @Test
    fun dynamicResolvesUiInkForMidTonePrimary() {
        val midTone = lightColorScheme(
            surface = Color(0xFFFAF7F5),
            surfaceContainerLow = Color(0xFFF2ECE6),
            primary = Color(0xFFDD6B20),
            onPrimary = Color.White
        )
        val c = DynamicShellColors.from(midTone, dark = false)
        assertTrue("uiInk on ui contrast >= 4.5", Contrast.ratio(c.uiInk, c.ui) >= 4.5)
    }

    private fun assertShellContrastMinimums(name: String, c: ShellColors) {
        val thickBg = c.thick.compositeOver(c.bg)

        val t1OnBg = Contrast.ratio(c.t1, c.bg)
        assertTrue("$name: t1 on bg ($t1OnBg) >= 4.5", t1OnBg >= 4.5)

        val t1OnG1 = Contrast.ratio(c.t1, c.g1)
        assertTrue("$name: t1 on g1 ($t1OnG1) >= 4.5", t1OnG1 >= 4.5)

        val t2OnBg = Contrast.ratio(c.t2, c.bg)
        assertTrue("$name: t2 on bg ($t2OnBg) >= 4.5", t2OnBg >= 4.5)

        val t2OnG1 = Contrast.ratio(c.t2, c.g1)
        assertTrue("$name: t2 on g1 ($t2OnG1) >= 4.5", t2OnG1 >= 4.5)

        val uiInkOnUi = Contrast.ratio(c.uiInk, c.ui)
        assertTrue("$name: uiInk on ui ($uiInkOnUi) >= 4.5", uiInkOnUi >= 4.5)

        val dangerOnG1 = Contrast.ratio(c.danger, c.g1)
        assertTrue("$name: danger on g1 ($dangerOnG1) >= 4.5", dangerOnG1 >= 4.5)

        val dangerOnThick = Contrast.ratio(c.danger, thickBg)
        assertTrue("$name: danger on thick ($dangerOnThick) >= 4.5", dangerOnThick >= 4.5)

        val uiOnG1 = Contrast.ratio(c.ui, c.g1)
        assertTrue("$name: ui on g1 ($uiOnG1) >= 3.0", uiOnG1 >= 3.0)

        val t3OnBg = Contrast.ratio(c.t3, c.bg)
        assertTrue("$name: t3 on bg ($t3OnBg) >= 3.0", t3OnBg >= 3.0)
    }

    private fun generateTestScheme(hue: Float, dark: Boolean): androidx.compose.material3.ColorScheme {
        fun colorFromHsl(h: Float, s: Float, l: Float): Color {
            val q = if (l < 0.5f) l * (1f + s) else l + s - l * s
            val p = 2f * l - q
            fun hue2rgb(tArg: Float): Float {
                var t = tArg
                if (t < 0f) t += 1f
                if (t > 1f) t -= 1f
                if (t < 1f / 6f) return p + (q - p) * 6f * t
                if (t < 1f / 2f) return q
                if (t < 2f / 3f) return p + (q - p) * (2f / 3f - t) * 6f
                return p
            }
            val hNorm = (h % 360f + 360f) % 360f / 360f
            val r = hue2rgb(hNorm + 1f / 3f).coerceIn(0f, 1f)
            val g = hue2rgb(hNorm).coerceIn(0f, 1f)
            val b = hue2rgb(hNorm - 1f / 3f).coerceIn(0f, 1f)
            return Color(r, g, b)
        }

        return if (dark) {
            darkColorScheme(
                surface = colorFromHsl(hue, 0.10f, 0.06f),
                surfaceContainer = colorFromHsl(hue, 0.10f, 0.12f),
                surfaceContainerHigh = colorFromHsl(hue, 0.10f, 0.18f),
                onSurface = colorFromHsl(hue, 0.05f, 0.90f),
                onSurfaceVariant = colorFromHsl(hue, 0.05f, 0.75f),
                outlineVariant = colorFromHsl(hue, 0.05f, 0.30f),
                primary = colorFromHsl(hue, 0.60f, 0.75f),
                onPrimary = colorFromHsl(hue, 0.60f, 0.15f),
                primaryContainer = colorFromHsl(hue, 0.60f, 0.30f),
                tertiaryContainer = colorFromHsl((hue + 60f) % 360f, 0.50f, 0.25f),
                error = Color(0xFFFFB4AB)
            )
        } else {
            lightColorScheme(
                surface = colorFromHsl(hue, 0.10f, 0.98f),
                surfaceContainerLow = colorFromHsl(hue, 0.10f, 0.94f),
                surfaceContainerLowest = colorFromHsl(hue, 0.10f, 1.0f),
                onSurface = colorFromHsl(hue, 0.05f, 0.10f),
                onSurfaceVariant = colorFromHsl(hue, 0.05f, 0.30f),
                outlineVariant = colorFromHsl(hue, 0.05f, 0.80f),
                primary = colorFromHsl(hue, 0.60f, 0.40f),
                onPrimary = colorFromHsl(hue, 0.60f, 1.0f),
                primaryContainer = colorFromHsl(hue, 0.60f, 0.85f),
                tertiaryContainer = colorFromHsl((hue + 60f) % 360f, 0.50f, 0.85f),
                error = Color(0xFFBA1A1A)
            )
        }
    }
}
