package app.enk.design

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class MaterialsTest {

    @Test
    fun railStops() {
        val expected = listOf(
            0f to 0xFFF4F4F2L,
            0.40f to 0xFFA9A9A6L,
            0.62f to 0xFF6F6F6CL,
            1f to 0xFFC9C9C6L
        )
        val actual = EnkMaterials.railStopsForTest.map {
            it.first to (it.second.toArgb().toLong() and 0xFFFFFFFFL)
        }
        assertEquals(expected, actual)
    }

    @Test
    fun capShadowLayers() {
        assertEquals(3, EnkShadows.cap.size) // one inset highlight + two drops (DS 5)
        assertTrue(EnkShadows.cap[0].inset)
        assertEquals(1.dp, EnkShadows.cap[0].dy)
        assertEquals(0.dp, EnkShadows.cap[0].blur)
        assertEquals(Color(0x59FFFFFF), EnkShadows.cap[0].color)

        assertEquals(false, EnkShadows.cap[1].inset)
        assertEquals(12.dp, EnkShadows.cap[1].dy)
        assertEquals(16.dp, EnkShadows.cap[1].blur)
        assertEquals(Color(0x8C000000), EnkShadows.cap[1].color)

        assertEquals(false, EnkShadows.cap[2].inset)
        assertEquals(2.dp, EnkShadows.cap[2].dy)
        assertEquals(3.dp, EnkShadows.cap[2].blur)
        assertEquals(Color(0x99000000), EnkShadows.cap[2].color)
    }

    @Test
    fun shadowLayersStructure() {
        assertEquals(2, EnkShadows.paper.size)
        assertEquals(2, EnkShadows.rail.size)
        assertEquals(3, EnkShadows.knob.size)
        assertEquals(3, EnkShadows.knobLifted.size)
        assertEquals(4, EnkShadows.panel.size)
        assertEquals(1, EnkShadows.panelWell.size)
        assertEquals(2, EnkShadows.slot.size)
        assertEquals(2, EnkShadows.track.size)

        // Verify panel ring layer has spread 1.dp and color panelRing
        val panelRingLayer = EnkShadows.panel[2]
        assertEquals(1.dp, panelRingLayer.spread)
        assertEquals(EnkColors.panelRing, panelRingLayer.color)
        assertEquals(false, panelRingLayer.inset)
    }

    @Test
    fun paperShadowLayers() {
        assertEquals(2, EnkShadows.paper.size)
        // Layer 0: (0, -1, blur 0, spread 0, white 12%)
        assertEquals(0.dp, EnkShadows.paper[0].dx)
        assertEquals((-1).dp, EnkShadows.paper[0].dy)
        assertEquals(0.dp, EnkShadows.paper[0].blur)
        assertEquals(0.dp, EnkShadows.paper[0].spread)
        assertEquals(Color(0x1FFFFFFF), EnkShadows.paper[0].color)
        assertEquals(false, EnkShadows.paper[0].inset)

        // Layer 1: (0, 8, blur 20, spread 0, black 45%)
        assertEquals(0.dp, EnkShadows.paper[1].dx)
        assertEquals(8.dp, EnkShadows.paper[1].dy)
        assertEquals(20.dp, EnkShadows.paper[1].blur)
        assertEquals(0.dp, EnkShadows.paper[1].spread)
        assertEquals(Color(0x73000000), EnkShadows.paper[1].color)
        assertEquals(false, EnkShadows.paper[1].inset)
    }

    @Test
    fun railShadowLayers() {
        assertEquals(2, EnkShadows.rail.size)
        // Layer 0: (0, 7, blur 10, spread 0, black 45%)
        assertEquals(0.dp, EnkShadows.rail[0].dx)
        assertEquals(7.dp, EnkShadows.rail[0].dy)
        assertEquals(10.dp, EnkShadows.rail[0].blur)
        assertEquals(0.dp, EnkShadows.rail[0].spread)
        assertEquals(Color(0x73000000), EnkShadows.rail[0].color)
        assertEquals(false, EnkShadows.rail[0].inset)

        // Layer 1: (0, 1, blur 0, spread 0, black 40%)
        assertEquals(0.dp, EnkShadows.rail[1].dx)
        assertEquals(1.dp, EnkShadows.rail[1].dy)
        assertEquals(0.dp, EnkShadows.rail[1].blur)
        assertEquals(0.dp, EnkShadows.rail[1].spread)
        assertEquals(Color(0x66000000), EnkShadows.rail[1].color)
        assertEquals(false, EnkShadows.rail[1].inset)
    }

    @Test
    fun knobShadowLayers() {
        assertEquals(3, EnkShadows.knob.size)
        // Layer 0: inset (0, 1, blur 0, spread 0, white 30%)
        assertEquals(0.dp, EnkShadows.knob[0].dx)
        assertEquals(1.dp, EnkShadows.knob[0].dy)
        assertEquals(0.dp, EnkShadows.knob[0].blur)
        assertEquals(0.dp, EnkShadows.knob[0].spread)
        assertEquals(Color(0x4DFFFFFF), EnkShadows.knob[0].color)
        assertEquals(true, EnkShadows.knob[0].inset)

        // Layer 1: (0, 6, blur 8, spread 0, black 55%)
        assertEquals(0.dp, EnkShadows.knob[1].dx)
        assertEquals(6.dp, EnkShadows.knob[1].dy)
        assertEquals(8.dp, EnkShadows.knob[1].blur)
        assertEquals(0.dp, EnkShadows.knob[1].spread)
        assertEquals(Color(0x8C000000), EnkShadows.knob[1].color)
        assertEquals(false, EnkShadows.knob[1].inset)

        // Layer 2: (0, 1, blur 2, spread 0, black 100%)
        assertEquals(0.dp, EnkShadows.knob[2].dx)
        assertEquals(1.dp, EnkShadows.knob[2].dy)
        assertEquals(2.dp, EnkShadows.knob[2].blur)
        assertEquals(0.dp, EnkShadows.knob[2].spread)
        assertEquals(Color(0xFF000000), EnkShadows.knob[2].color)
        assertEquals(false, EnkShadows.knob[2].inset)
    }

    @Test
    fun knobLiftedShadowLayers() {
        assertEquals(3, EnkShadows.knobLifted.size)
        // Layer 0: inset (0, 1, blur 0, spread 0, white 30%)
        assertEquals(0.dp, EnkShadows.knobLifted[0].dx)
        assertEquals(1.dp, EnkShadows.knobLifted[0].dy)
        assertEquals(0.dp, EnkShadows.knobLifted[0].blur)
        assertEquals(0.dp, EnkShadows.knobLifted[0].spread)
        assertEquals(Color(0x4DFFFFFF), EnkShadows.knobLifted[0].color)
        assertEquals(true, EnkShadows.knobLifted[0].inset)

        // Layer 1: (0, 12, blur 14, spread 0, black 60%)
        assertEquals(0.dp, EnkShadows.knobLifted[1].dx)
        assertEquals(12.dp, EnkShadows.knobLifted[1].dy)
        assertEquals(14.dp, EnkShadows.knobLifted[1].blur)
        assertEquals(0.dp, EnkShadows.knobLifted[1].spread)
        assertEquals(Color(0x99000000), EnkShadows.knobLifted[1].color)
        assertEquals(false, EnkShadows.knobLifted[1].inset)

        // Layer 2: (0, 2, blur 3, spread 0, black 100%)
        assertEquals(0.dp, EnkShadows.knobLifted[2].dx)
        assertEquals(2.dp, EnkShadows.knobLifted[2].dy)
        assertEquals(3.dp, EnkShadows.knobLifted[2].blur)
        assertEquals(0.dp, EnkShadows.knobLifted[2].spread)
        assertEquals(Color(0xFF000000), EnkShadows.knobLifted[2].color)
        assertEquals(false, EnkShadows.knobLifted[2].inset)
    }

    @Test
    fun panelShadowLayers() {
        assertEquals(4, EnkShadows.panel.size)
        // Layer 0: inset (0, 1, blur 0, spread 0, white 22%)
        assertEquals(0.dp, EnkShadows.panel[0].dx)
        assertEquals(1.dp, EnkShadows.panel[0].dy)
        assertEquals(0.dp, EnkShadows.panel[0].blur)
        assertEquals(0.dp, EnkShadows.panel[0].spread)
        assertEquals(Color(0x38FFFFFF), EnkShadows.panel[0].color)
        assertEquals(true, EnkShadows.panel[0].inset)

        // Layer 1: inset (0, -1, blur 0, spread 0, black 50%)
        assertEquals(0.dp, EnkShadows.panel[1].dx)
        assertEquals((-1).dp, EnkShadows.panel[1].dy)
        assertEquals(0.dp, EnkShadows.panel[1].blur)
        assertEquals(0.dp, EnkShadows.panel[1].spread)
        assertEquals(Color(0x80000000), EnkShadows.panel[1].color)
        assertEquals(true, EnkShadows.panel[1].inset)

        // Layer 2: ring (0, 0, blur 0, spread 1, panelRing)
        assertEquals(0.dp, EnkShadows.panel[2].dx)
        assertEquals(0.dp, EnkShadows.panel[2].dy)
        assertEquals(0.dp, EnkShadows.panel[2].blur)
        assertEquals(1.dp, EnkShadows.panel[2].spread)
        assertEquals(EnkColors.panelRing, EnkShadows.panel[2].color)
        assertEquals(false, EnkShadows.panel[2].inset)

        // Layer 3: (0, 14, blur 24, spread 0, black 60%)
        assertEquals(0.dp, EnkShadows.panel[3].dx)
        assertEquals(14.dp, EnkShadows.panel[3].dy)
        assertEquals(24.dp, EnkShadows.panel[3].blur)
        assertEquals(0.dp, EnkShadows.panel[3].spread)
        assertEquals(Color(0x99000000), EnkShadows.panel[3].color)
        assertEquals(false, EnkShadows.panel[3].inset)
    }

    @Test
    fun panelWellShadowLayers() {
        assertEquals(1, EnkShadows.panelWell.size)
        // Layer 0: inset (0, 2, blur 6, spread 0, black 50%)
        assertEquals(0.dp, EnkShadows.panelWell[0].dx)
        assertEquals(2.dp, EnkShadows.panelWell[0].dy)
        assertEquals(6.dp, EnkShadows.panelWell[0].blur)
        assertEquals(0.dp, EnkShadows.panelWell[0].spread)
        assertEquals(Color(0x80000000), EnkShadows.panelWell[0].color)
        assertEquals(true, EnkShadows.panelWell[0].inset)
    }

    @Test
    fun slotShadowLayers() {
        assertEquals(2, EnkShadows.slot.size)
        // Layer 0: inset (0, 2, blur 3, spread 0, black 100%)
        assertEquals(0.dp, EnkShadows.slot[0].dx)
        assertEquals(2.dp, EnkShadows.slot[0].dy)
        assertEquals(3.dp, EnkShadows.slot[0].blur)
        assertEquals(0.dp, EnkShadows.slot[0].spread)
        assertEquals(Color(0xFF000000), EnkShadows.slot[0].color)
        assertEquals(true, EnkShadows.slot[0].inset)

        // Layer 1: (0, 1, blur 0, spread 0, white 15%)
        assertEquals(0.dp, EnkShadows.slot[1].dx)
        assertEquals(1.dp, EnkShadows.slot[1].dy)
        assertEquals(0.dp, EnkShadows.slot[1].blur)
        assertEquals(0.dp, EnkShadows.slot[1].spread)
        assertEquals(Color(0x26FFFFFF), EnkShadows.slot[1].color)
        assertEquals(false, EnkShadows.slot[1].inset)
    }

    @Test
    fun trackShadowLayers() {
        assertEquals(2, EnkShadows.track.size)
        // Layer 0: inset (0, 2, blur 3, spread 0, black 100%)
        assertEquals(0.dp, EnkShadows.track[0].dx)
        assertEquals(2.dp, EnkShadows.track[0].dy)
        assertEquals(3.dp, EnkShadows.track[0].blur)
        assertEquals(0.dp, EnkShadows.track[0].spread)
        assertEquals(Color(0xFF000000), EnkShadows.track[0].color)
        assertEquals(true, EnkShadows.track[0].inset)

        // Layer 1: (0, 1, blur 0, spread 0, white 14%)
        assertEquals(0.dp, EnkShadows.track[1].dx)
        assertEquals(1.dp, EnkShadows.track[1].dy)
        assertEquals(0.dp, EnkShadows.track[1].blur)
        assertEquals(0.dp, EnkShadows.track[1].spread)
        assertEquals(Color(0x24FFFFFF), EnkShadows.track[1].color)
        assertEquals(false, EnkShadows.track[1].inset)
    }

    @Test
    fun shadowLayerDefaultInsetIsFalse() {
        val layer = ShadowLayer(1.dp, 2.dp, 3.dp, 4.dp, Color.Black)
        assertEquals(false, layer.inset)
    }

    @Test
    fun typographyTokens() {
        // Wordmark: system sans, 13 sp, weight 600, 0.16 em, textOnChassis
        assertEquals(FontFamily.SansSerif, EnkType.wordmark.fontFamily)
        assertEquals(13.sp, EnkType.wordmark.fontSize)
        assertEquals(FontWeight.SemiBold, EnkType.wordmark.fontWeight)
        assertEquals(0.16.em, EnkType.wordmark.letterSpacing)
        assertEquals(EnkColors.textOnChassis, EnkType.wordmark.color)

        // Meta: Courier Prime, 10 sp, line height 13 sp, 0.04 em, textMuted
        assertEquals(10.sp, EnkType.meta.fontSize)
        assertEquals(13.sp, EnkType.meta.lineHeight)
        assertEquals(0.04.em, EnkType.meta.letterSpacing)
        assertEquals(EnkColors.textMuted, EnkType.meta.color)

        // Panel label: 9 sp, 0.16 em, textMuted
        assertEquals(9.sp, EnkType.panelLabel.fontSize)
        assertEquals(0.16.em, EnkType.panelLabel.letterSpacing)
        assertEquals(EnkColors.textMuted, EnkType.panelLabel.color)

        // Panel value: 9 sp, 0.04 em, textOnChassis
        assertEquals(9.sp, EnkType.panelValue.fontSize)
        assertEquals(0.04.em, EnkType.panelValue.letterSpacing)
        assertEquals(EnkColors.textOnChassis, EnkType.panelValue.color)

        // Hint: 10 sp, 0 em, textMuted
        assertEquals(10.sp, EnkType.hint.fontSize)
        assertEquals(0.em, EnkType.hint.letterSpacing)
        assertEquals(EnkColors.textMuted, EnkType.hint.color)

        // Slug: Courier Prime bold, 9 sp, line height 12 sp, slugLetter
        assertEquals(FontWeight.Bold, EnkType.slug.fontWeight)
        assertEquals(9.sp, EnkType.slug.fontSize)
        assertEquals(12.sp, EnkType.slug.lineHeight)
        assertEquals(EnkColors.slugLetter, EnkType.slug.color)

        // Paper: tracking 0, ink
        assertEquals(0.em, EnkType.paper.letterSpacing)
        assertEquals(EnkColors.ink, EnkType.paper.color)
    }
}
