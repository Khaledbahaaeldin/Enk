package app.enk.design

import androidx.compose.ui.graphics.toArgb
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class TokensTest {

    private fun colorToHex(color: androidx.compose.ui.graphics.Color): Long {
        return color.toArgb().toLong() and 0xFFFFFFFFL
    }

    @Test
    fun accentHex() {
        assertEquals(0xFFFF5A1FL, colorToHex(EnkColors.accent))
    }

    @Test
    fun verifyAllColorTokens() {
        assertEquals(0xFF34322FL, colorToHex(EnkColors.chassisTop))
        assertEquals(0xFF222120L, colorToHex(EnkColors.chassisBottom))
        assertEquals(0xFF171615L, colorToHex(EnkColors.chassisNightTop))
        assertEquals(0xFF0D0C0CL, colorToHex(EnkColors.chassisNightBottom))
        assertEquals(0xFFF2ECDFL, colorToHex(EnkColors.paper))
        assertEquals(0xFF161513L, colorToHex(EnkColors.paperNight))
        assertEquals(0xFF2B2824L, colorToHex(EnkColors.ink))
        assertEquals(0xFFFF5A1FL, colorToHex(EnkColors.accent))
        assertEquals(0xB3FF5A1FL, colorToHex(EnkColors.accentGlowNight))
        assertEquals(0xFFCFC8BAL, colorToHex(EnkColors.textOnChassis))
        assertEquals(0xFF8B8578L, colorToHex(EnkColors.textMuted))
        assertEquals(0xFF161514L, colorToHex(EnkColors.slotDark))
        assertEquals(0xFF121110L, colorToHex(EnkColors.trackDark))
        assertEquals(0xFF3A3936L, colorToHex(EnkColors.panelTop))
        assertEquals(0xFF262523L, colorToHex(EnkColors.panelBottom))
        assertEquals(0xFF0D0C0BL, colorToHex(EnkColors.panelRing))
        assertEquals(0xFF3A3A38L, colorToHex(EnkColors.slugLetter))
    }

    @Test
    fun verifyMotionTokens() {
        assertEquals(200, EnkMotion.feedMs)
        assertEquals(110, EnkMotion.carriageGlideMs)
        assertEquals(400, EnkMotion.carriageSnapMs)
        assertEquals(110, EnkMotion.slugStrikeMs)
        assertEquals(160, EnkMotion.stampMs)
        assertEquals(1000, EnkMotion.caretBlinkMs)
        assertEquals(600, EnkMotion.themeFadeMs)
        assertEquals(560, EnkMotion.springPanelMs)
        assertEquals(550, EnkMotion.springRowMs)
        assertEquals(400, EnkMotion.rowAlphaMs)
        assertEquals(350, EnkMotion.panelAlphaMs)
        assertEquals(listOf(120, 200, 280), EnkMotion.rowStaggerMs)
        assertEquals(300, EnkMotion.springKnobMs)
        assertEquals(200, EnkMotion.knobLiftMs)
        assertEquals(480L, EnkMotion.longPressMs)
        assertEquals(6f, EnkMotion.longPressSlopDp, 0.001f)
    }

    @Test
    fun verifyEasingCurves() {
        val easings = listOf(
            "feed" to EnkMotion.feed,
            "carriageGlide" to EnkMotion.carriageGlide,
            "carriageSnap" to EnkMotion.carriageSnap,
            "slugStrike" to EnkMotion.slugStrike,
            "stamp" to EnkMotion.stamp,
            "themeFade" to EnkMotion.themeFade,
            "springPanel" to EnkMotion.springPanel,
            "springRow" to EnkMotion.springRow,
            "rowAlpha" to EnkMotion.rowAlpha,
            "panelAlpha" to EnkMotion.panelAlpha,
            "springKnob" to EnkMotion.springKnob
        )

        for ((name, easing) in easings) {
            assertEquals("$name: transform(0f) should be 0f", 0f, easing.transform(0f), 0.0001f)
            assertEquals("$name: transform(1f) should be 1f", 1f, easing.transform(1f), 0.0001f)
        }

        // Mechanical spring / bounce overshoot curves (y > 1.0f during transition)
        val overshootEasings = listOf(
            "carriageGlide" to EnkMotion.carriageGlide,
            "carriageSnap" to EnkMotion.carriageSnap,
            "springPanel" to EnkMotion.springPanel,
            "springRow" to EnkMotion.springRow,
            "springKnob" to EnkMotion.springKnob
        )
        for ((name, easing) in overshootEasings) {
            val maxTransform = (1..99).maxOf { step -> easing.transform(step / 100f) }
            assertTrue("$name should overshoot 1.0f (max was $maxTransform)", maxTransform > 1.0f)
        }
    }
}
