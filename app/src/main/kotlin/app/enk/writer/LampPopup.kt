package app.enk.writer

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
    onSliderTouchChange: (Boolean) -> Unit = {}
) {
    val colors = LocalShellColors.current

    Column(
        modifier = modifier
            .testTag("lamp_popup")
            .requiredSize(280.dp, 230.dp)
            .padding(horizontal = 18.dp)
    ) {
        // Row 1: "Lamp" + ShellToggle (32 dp)
        Row(
            modifier = Modifier
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
        modifier = Modifier
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
