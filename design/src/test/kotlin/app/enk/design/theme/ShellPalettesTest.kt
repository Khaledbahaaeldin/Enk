package app.enk.design.theme

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.graphics.toArgb
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class ShellPalettesTest {

    private fun Color.hex(): Long = toArgb().toLong() and 0xFFFFFFFFL

    @Test
    fun graphiteDarkIsTheOriginal() {
        val c = ShellPalettes.colors(ThemeId.Graphite, true)
        assertEquals(0xFF161514L, c.bg.hex())
        assertEquals(0xFF201F1DL, c.g1.hex())
        assertEquals(0xFFFF5A1FL, c.ui.hex())
        assertEquals(0xFFF3EEE3L, c.t1.hex())
    }

    @Test
    fun apricotDark() {
        val c = ShellPalettes.colors(ThemeId.Apricot, true)
        assertEquals(0xFF1C120FL, c.bg.hex())
        assertEquals(0xFF2A1A15L, c.g1.hex())
        assertEquals(0xFFFFDAB9L, c.t1.hex())
        assertEquals(0xFFE2725BL, c.ui.hex())
        assertEquals(0xFF2A1410L, c.uiInk.hex())
    }

    @Test
    fun skyDark() {
        val c = ShellPalettes.colors(ThemeId.Sky, true)
        assertEquals(0xFF1B1413L, c.bg.hex())
        assertEquals(0xFFFBF1C9L, c.t1.hex())
        assertEquals(0xFFC1DBE8L, c.ui.hex())
    }

    @Test
    fun slateDark() {
        val c = ShellPalettes.colors(ThemeId.Slate, true)
        assertEquals(0xFF0D0F11L, c.bg.hex())
        assertEquals(0xFFE5E4E2L, c.t1.hex())
        assertEquals(0xFF8FA6B8L, c.ui.hex())
    }

    @Test
    fun monoDark() {
        val c = ShellPalettes.colors(ThemeId.Mono, true)
        assertEquals(0xFF0B0B0BL, c.bg.hex())
        assertEquals(0xFFF2F2F2L, c.t1.hex())
        assertEquals(0xFFF2F2F2L, c.ui.hex())
    }

    @Test
    fun graphiteLight() {
        val c = ShellPalettes.colors(ThemeId.Graphite, false)
        assertEquals(0xFFECE8DFL, c.bg.hex())
        assertEquals(0xFFF7F4ECL, c.g1.hex())
        assertEquals(0xFF2B2824L, c.t1.hex())
        assertEquals(0xFFEE5416L, c.ui.hex())
    }

    @Test
    fun apricotLight() {
        val c = ShellPalettes.colors(ThemeId.Apricot, false)
        assertEquals(0xFFFFEBDCL, c.bg.hex())
        assertEquals(0xFF4E2C23L, c.t1.hex())
        assertEquals(0xFFD4634CL, c.ui.hex())
    }

    @Test
    fun skyLight() {
        val c = ShellPalettes.colors(ThemeId.Sky, false)
        assertEquals(0xFFFFF6D4L, c.bg.hex())
        assertEquals(0xFF432F2EL, c.t1.hex())
        assertEquals(0xFF5E92B2L, c.ui.hex())
    }

    @Test
    fun slateLight() {
        val c = ShellPalettes.colors(ThemeId.Slate, false)
        assertEquals(0xFFE5E4E2L, c.bg.hex())
        assertEquals(0xFF0A0A0AL, c.t1.hex())
        assertEquals(0xFF536878L, c.ui.hex())
    }

    @Test
    fun monoLight() {
        val c = ShellPalettes.colors(ThemeId.Mono, false)
        assertEquals(0xFFEEEEEEL, c.bg.hex())
        assertEquals(0xFF111111L, c.t1.hex())
        assertEquals(0xFF111111L, c.ui.hex())
    }

    @Test
    fun dynamicIsRejectedByStaticPalettes() {
        assertThrows(IllegalArgumentException::class.java) {
            ShellPalettes.colors(ThemeId.Dynamic, true)
        }
        assertThrows(IllegalArgumentException::class.java) {
            ShellPalettes.colors(ThemeId.Dynamic, false)
        }
    }

    @Test
    fun darkMaterialTokens() {
        val m = ShellPalettes.material(true)
        assertEquals(0.10f, m.auraGrain, 0.001f)
        assertEquals(Color(1f, 1f, 1f, 0.12f), m.glassA)
        assertEquals(Color(8 / 255f, 7 / 255f, 6 / 255f, 0.50f), m.scrim)
    }

    @Test
    fun lightMaterialTokens() {
        val m = ShellPalettes.material(false)
        assertEquals(0.16f, m.auraGrain, 0.001f)
        assertEquals(Color(1f, 1f, 1f, 0.66f), m.glassA)
        assertEquals(Color(20 / 255f, 12 / 255f, 6 / 255f, 0.30f), m.scrim)
    }

    @Test
    fun contrastMinimumsHoldForEveryStaticCombo() {
        val themes = listOf(ThemeId.Graphite, ThemeId.Apricot, ThemeId.Sky, ThemeId.Slate, ThemeId.Mono)
        for (theme in themes) {
            for (dark in listOf(true, false)) {
                val modeName = if (dark) "Dark" else "Light"
                val name = "$theme $modeName"
                val c = ShellPalettes.colors(theme, dark)
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
        }
    }

    @Test
    fun allMaterialTokensCompleteVerification() {
        val dark = ShellPalettes.material(true)
        assertEquals(Color(1f, 1f, 1f, 0.12f), dark.glassA)
        assertEquals(Color(1f, 1f, 1f, 0.05f), dark.glassB)
        assertEquals(Color(1f, 1f, 1f, 0.42f), dark.glassEdge)
        assertEquals(Color(1f, 1f, 1f, 0.10f), dark.glassLine)
        assertEquals(Color(0f, 0f, 0f, 0.30f), dark.sh1)
        assertEquals(Color(0f, 0f, 0f, 0.50f), dark.sh2)
        assertEquals(Color(1f, 1f, 1f, 0.22f), dark.lensA)
        assertEquals(Color(1f, 1f, 1f, 0.11f), dark.lensB)
        assertEquals(Color(1f, 1f, 1f, 0.55f), dark.lensEdge)
        assertEquals(Color(1f, 1f, 1f, 0.12f), dark.track)
        assertEquals(Color(1f, 1f, 1f, 0.08f), dark.field)
        assertEquals(Color(0f, 0f, 0f, 0.28f), dark.field2)
        assertEquals(Color(8 / 255f, 7 / 255f, 6 / 255f, 0.50f), dark.scrim)
        assertEquals(Color(1f, 1f, 1f, 0.28f), dark.grab)
        assertEquals(Color(0f, 0f, 0f, 0.50f), dark.cardShadow)
        assertEquals(0.10f, dark.auraGrain, 0.001f)

        val light = ShellPalettes.material(false)
        assertEquals(Color(1f, 1f, 1f, 0.66f), light.glassA)
        assertEquals(Color(1f, 1f, 1f, 0.36f), light.glassB)
        assertEquals(Color(1f, 1f, 1f, 0.95f), light.glassEdge)
        assertEquals(Color(0f, 0f, 0f, 0.06f), light.glassLine)
        assertEquals(Color(60 / 255f, 40 / 255f, 20 / 255f, 0.14f), light.sh1)
        assertEquals(Color(60 / 255f, 40 / 255f, 20 / 255f, 0.22f), light.sh2)
        assertEquals(Color(1f, 1f, 1f, 0.98f), light.lensA)
        assertEquals(Color(1f, 1f, 1f, 0.80f), light.lensB)
        assertEquals(Color(1f, 1f, 1f, 1.0f), light.lensEdge)
        assertEquals(Color(0f, 0f, 0f, 0.13f), light.track)
        assertEquals(Color(0f, 0f, 0f, 0.055f), light.field)
        assertEquals(Color(0f, 0f, 0f, 0.06f), light.field2)
        assertEquals(Color(20 / 255f, 12 / 255f, 6 / 255f, 0.30f), light.scrim)
        assertEquals(Color(0f, 0f, 0f, 0.22f), light.grab)
        assertEquals(Color(70 / 255f, 40 / 255f, 20 / 255f, 0.28f), light.cardShadow)
        assertEquals(0.16f, light.auraGrain, 0.001f)
    }

    @Test
    fun staticThemesDangerColorsMatchSpec() {
        assertEquals(0xFFFF8A7DL, ShellPalettes.colors(ThemeId.Graphite, true).danger.hex())
        assertEquals(0xFFFF9A86L, ShellPalettes.colors(ThemeId.Apricot, true).danger.hex())
        assertEquals(0xFFFF9D8FL, ShellPalettes.colors(ThemeId.Sky, true).danger.hex())
        assertEquals(0xFFFF948AL, ShellPalettes.colors(ThemeId.Slate, true).danger.hex())
        assertEquals(0xFFFF8A80L, ShellPalettes.colors(ThemeId.Mono, true).danger.hex())

        val lightThemes = listOf(ThemeId.Graphite, ThemeId.Apricot, ThemeId.Sky, ThemeId.Slate, ThemeId.Mono)
        for (t in lightThemes) {
            assertEquals(0xFFB3261EL, ShellPalettes.colors(t, false).danger.hex())
        }
    }
}
