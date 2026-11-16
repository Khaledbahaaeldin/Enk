package app.enk.writer

import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.enk.design.EnkColors
import app.enk.design.LocalMonoFont
import app.enk.design.glass.GlassSlider
import app.enk.design.shell.GlassSource
import app.enk.design.shell.rememberGlassSource
import app.enk.design.shell.ShellToggle
import app.enk.design.theme.LocalShellColors

@Composable
fun LampPopupContent(
    lampOn: Boolean,
    onToggleLamp: () -> Unit,
    warmth: Int,
    onWarmthChange: (Int) -> Unit,
    intensity: Int,
    onIntensityChange: (Int) -> Unit,
    beam: Int,
    onBeamChange: (Int) -> Unit,
    onDetent: (Int) -> Unit,
    modifier: Modifier = Modifier,
    accent: Color = EnkColors.accent,
    glassSource: GlassSource = rememberGlassSource(),
    onSliderTouchChange: (Boolean) -> Unit = {},
    /** Open progress 0..1, read in layer lambdas only; rows fade in one after another. */
    reveal: () -> Float = { 1f }
) {
    val colors = LocalShellColors.current
    val rise = with(androidx.compose.ui.platform.LocalDensity.current) { PanelReveal.RISE_DP.dp.toPx() }
    fun Modifier.stagger(index: Int): Modifier = this.graphicsLayer {
        val a = PanelReveal.rowAlpha(reveal(), index)
        alpha = a
        translationY = rise * (1f - a)
    }

    Column(
        modifier = modifier
            .testTag("lamp_popup")
            .requiredSize(280.dp, 230.dp)
            .padding(horizontal = 18.dp)
    ) {
        // Row 1: "Lamp" + ShellToggle (32 dp)
        Row(
            modifier = Modifier
                .stagger(0)
                .fillMaxWidth()
                .height(32.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "Lamp",
                color = colors.t1,
                fontSize = 17.sp,
                fontWeight = FontWeight.Medium
            )
            ShellToggle(
                checked = lampOn,
                onChange = { onToggleLamp() },
                modifier = Modifier.testTag("lamp_toggle"),
                glassSource = glassSource
            )
        }

        // Block 1: Warmth (66 dp)
        SliderBlock(
            modifier = Modifier.stagger(1),
            caption = "WARMTH",
            valueText = "$warmth",
            value = warmth,
            min = 0,
            max = 100,
            onValueChange = onWarmthChange,
            onDetent = onDetent,
            sliderTag = "slider_warmth",
            accent = accent,
            glassSource = glassSource,
            onSliderTouchChange = onSliderTouchChange
        )

        // Block 2: Intensity (66 dp)
        SliderBlock(
            modifier = Modifier.stagger(2),
            caption = "INTENSITY",
            valueText = "$intensity",
            value = intensity,
            min = 20,
            max = 100,
            onValueChange = onIntensityChange,
            onDetent = onDetent,
            sliderTag = "slider_intensity",
            accent = accent,
            glassSource = glassSource,
            onSliderTouchChange = onSliderTouchChange
        )

        // Block 3: Beam (66 dp)
        SliderBlock(
            modifier = Modifier.stagger(3),
            caption = "BEAM",
            valueText = "$beam",
            value = beam,
            min = 30,
            max = 100,
            onValueChange = onBeamChange,
            onDetent = onDetent,
            sliderTag = "slider_beam",
            accent = accent,
            glassSource = glassSource,
            onSliderTouchChange = onSliderTouchChange
        )
    }
}

@Composable
private fun SliderBlock(
    modifier: Modifier = Modifier,
    caption: String,
    valueText: String,
    value: Int,
    min: Int,
    max: Int,
    onValueChange: (Int) -> Unit,
    onDetent: (Int) -> Unit,
    sliderTag: String,
    accent: Color,
    glassSource: GlassSource,
    onSliderTouchChange: (Boolean) -> Unit
) {
    val colors = LocalShellColors.current
    val monoFont = LocalMonoFont.current

    Column(
        modifier = modifier
            .fillMaxWidth()
            .height(66.dp),
        verticalArrangement = Arrangement.Center
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = caption,
                color = colors.t2,
                fontSize = 11.sp,
                letterSpacing = 0.06.sp * 11
            )
            Text(
                text = valueText,
                color = colors.t1,
                fontSize = 11.sp,
                fontFamily = monoFont
            )
        }
        GlassSlider(
            value = value,
            min = min,
            max = max,
            onValueChange = onValueChange,
            onDetent = onDetent,
            accent = accent,
            label = caption,
            glassSource = glassSource,
            onTouchChange = onSliderTouchChange,
            modifier = Modifier.testTag(sliderTag)
        )
    }
}

/** The lamp panel's content reveal: rows fade in and rise 6 dp, one after another, once the glass is mostly open. */
internal object PanelReveal {
    const val RISE_DP = 6f
    private const val START = 0.62f
    private const val STEP = 0.05f
    private const val SPAN = 0.22f

    fun rowAlpha(progress: Float, index: Int): Float =
        ((progress - START - index * STEP) / SPAN).coerceIn(0f, 1f)
}
