package app.enk.design

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

internal data class RowMotionValues(val translateY: Dp, val alpha: Float)
internal data class PanelMotionValues(
    val translateY: Dp,
    val scale: Float,
    val alpha: Float,
    val rows: List<RowMotionValues>
)

internal fun computePanelMotion(tMs: Long): PanelMotionValues {
    val panelTranslateProgress = (tMs / 560f).coerceIn(0f, 1f)
    val panelTranslateEased = EnkMotion.springPanel.transform(panelTranslateProgress)
    val panelTranslateY = 40.dp * (1f - panelTranslateEased)
    val panelScale = 0.97f + 0.03f * panelTranslateEased

    val panelAlphaProgress = (tMs / 350f).coerceIn(0f, 1f)
    val panelAlphaEased = EnkMotion.panelAlpha.transform(panelAlphaProgress)
    val panelAlpha = panelAlphaEased

    val rows = EnkMotion.rowStaggerMs.map { delay ->
        val elapsed = (tMs - delay).coerceAtLeast(0L)
        val rowTranslateProgress = (elapsed / 550f).coerceIn(0f, 1f)
        val rowTranslateEased = EnkMotion.springRow.transform(rowTranslateProgress)
        val rowTranslateY = 10.dp * (1f - rowTranslateEased)

        val rowAlphaProgress = (elapsed / 400f).coerceIn(0f, 1f)
        val rowAlphaEased = EnkMotion.rowAlpha.transform(rowAlphaProgress)
        val rowAlpha = rowAlphaEased

        RowMotionValues(translateY = rowTranslateY, alpha = rowAlpha)
    }

    return PanelMotionValues(
        translateY = panelTranslateY,
        scale = panelScale,
        alpha = panelAlpha,
        rows = rows
    )
}

@Composable
fun BoxScope.ControlPanel(
    visible: Boolean,
    lampColor: Color,
    settings: LampSettings,
    onChange: (LampSettings) -> Unit,
    onDetent: (Int) -> Unit,
    modifier: Modifier = Modifier,
    enterProgressMs: Long? = null,
    liftedFaderIndex: Int? = null,
    reduceEffects: Boolean = false
) {
    val animTimeMs = remember { Animatable(if (visible) 1000f else 0f) }
    val animAlpha = remember { Animatable(if (visible) 1f else 0f) }

    LaunchedEffect(visible, reduceEffects) {
        if (reduceEffects) {
            animAlpha.animateTo(
                targetValue = if (visible) 1f else 0f,
                animationSpec = tween(durationMillis = 100, easing = LinearEasing)
            )
        } else {
            if (visible) {
                if (animTimeMs.value < 1000f) {
                    animTimeMs.animateTo(1000f, tween(durationMillis = 830))
                }
            } else {
                animTimeMs.animateTo(0f, tween(durationMillis = 350))
            }
        }
    }

    val isHidden = if (reduceEffects) {
        animAlpha.value <= 0.001f
    } else {
        animTimeMs.value <= 0.001f
    }

    if (enterProgressMs == null && !visible && isHidden) {
        return
    }

    val motion = if (enterProgressMs != null) {
        if (reduceEffects) {
            val alpha = (enterProgressMs / 100f).coerceIn(0f, 1f)
            PanelMotionValues(
                translateY = 0.dp,
                scale = 1.0f,
                alpha = alpha,
                rows = List(3) { RowMotionValues(0.dp, 1.0f) }
            )
        } else {
            computePanelMotion(enterProgressMs)
        }
    } else if (reduceEffects) {
        PanelMotionValues(
            translateY = 0.dp,
            scale = 1.0f,
            alpha = animAlpha.value,
            rows = List(3) { RowMotionValues(0.dp, 1.0f) }
        )
    } else {
        computePanelMotion(animTimeMs.value.toLong())
    }

    Box(
        modifier = modifier
            .align(Alignment.BottomCenter)
            .fillMaxWidth()
            .padding(start = 26.dp, end = 26.dp, bottom = 30.dp)
            .graphicsLayer {
                translationY = motion.translateY.toPx()
                scaleX = motion.scale
                scaleY = motion.scale
                alpha = motion.alpha
            }
            .enkShadow(EnkShadows.panel, RoundedCornerShape(16.dp))
            .background(EnkMaterials.panelFace, RoundedCornerShape(16.dp))
            .padding(4.dp)
            .enkShadow(EnkShadows.panelWell, RoundedCornerShape(12.dp))
            .padding(top = 14.dp, start = 18.dp, end = 18.dp, bottom = 16.dp)
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Row 0: Warmth (0..100, default 25)
            ControlPanelRow(
                label = "WARMTH",
                value = settings.warmth,
                min = 0,
                max = 100,
                lampColor = lampColor,
                onValueChange = { newVal -> onChange(settings.copy(warmth = newVal)) },
                onDetent = onDetent,
                lifted = if (liftedFaderIndex != null) liftedFaderIndex == 0 else null,
                reduceEffects = reduceEffects,
                rowMotion = motion.rows[0]
            )

            // Row 1: Intensity (20..100, default 80)
            ControlPanelRow(
                label = "INTENSITY",
                value = settings.intensity,
                min = 20,
                max = 100,
                lampColor = lampColor,
                onValueChange = { newVal -> onChange(settings.copy(intensity = newVal)) },
                onDetent = onDetent,
                lifted = if (liftedFaderIndex != null) liftedFaderIndex == 1 else null,
                reduceEffects = reduceEffects,
                rowMotion = motion.rows[1]
            )

            // Row 2: Beam (30..100, default 55)
            ControlPanelRow(
                label = "BEAM",
                value = settings.beam,
                min = 30,
                max = 100,
                lampColor = lampColor,
                onValueChange = { newVal -> onChange(settings.copy(beam = newVal)) },
                onDetent = onDetent,
                lifted = if (liftedFaderIndex != null) liftedFaderIndex == 2 else null,
                reduceEffects = reduceEffects,
                rowMotion = motion.rows[2]
            )
        }
    }
}

@Composable
private fun ControlPanelRow(
    label: String,
    value: Int,
    min: Int,
    max: Int,
    lampColor: Color,
    onValueChange: (Int) -> Unit,
    onDetent: (Int) -> Unit,
    lifted: Boolean?,
    reduceEffects: Boolean,
    rowMotion: RowMotionValues
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(36.dp)
            .graphicsLayer {
                translationY = rowMotion.translateY.toPx()
                alpha = rowMotion.alpha
            },
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Label: 60 dp wide, uppercase, engrave shadow
        Box(
            modifier = Modifier.width(60.dp),
            contentAlignment = Alignment.CenterStart
        ) {
            val mono = LocalMonoFont.current
            val baseStyle = EnkType.panelLabel.copy(fontFamily = mono)

            // Engrave shadow: (0, 1 dp, white 8%)
            Text(
                text = label,
                style = baseStyle.copy(color = Color.White.copy(alpha = 0.08f)),
                maxLines = 1,
                softWrap = false,
                modifier = Modifier.offset(x = 0.dp, y = 1.dp)
            )
            // Engrave shadow: (0, -1 dp, black 60%)
            Text(
                text = label,
                style = baseStyle.copy(color = Color.Black.copy(alpha = 0.60f)),
                maxLines = 1,
                softWrap = false,
                modifier = Modifier.offset(x = 0.dp, y = (-1).dp)
            )
            // Foreground text
            Text(
                text = label,
                style = baseStyle,
                maxLines = 1,
                softWrap = false
            )
        }

        // Fader: filling remaining width
        Fader(
            label = label,
            value = value,
            min = min,
            max = max,
            lampColor = lampColor,
            onValueChange = onValueChange,
            onDetent = onDetent,
            modifier = Modifier.weight(1f),
            lifted = lifted,
            reduceEffects = reduceEffects
        )

        // Value text: 22 dp wide right-aligned, displayPercent
        Text(
            text = FaderMath.displayPercent(value, min, max).toString(),
            style = EnkType.panelValue.copy(
                fontFamily = LocalMonoFont.current,
                textAlign = TextAlign.End
            ),
            modifier = Modifier.width(22.dp)
        )
    }
}

@Composable
fun BoxScope.ControlPanel(
    visible: Boolean,
    lampColor: Rgb,
    settings: LampSettings,
    onChange: (LampSettings) -> Unit,
    onDetent: (Int) -> Unit,
    modifier: Modifier = Modifier,
    enterProgressMs: Long? = null,
    liftedFaderIndex: Int? = null,
    reduceEffects: Boolean = false
) = ControlPanel(
    visible = visible,
    lampColor = Color(lampColor.r, lampColor.g, lampColor.b),
    settings = settings,
    onChange = onChange,
    onDetent = onDetent,
    modifier = modifier,
    enterProgressMs = enterProgressMs,
    liftedFaderIndex = liftedFaderIndex,
    reduceEffects = reduceEffects
)

@Composable
fun ControlPanel(
    visible: Boolean,
    lampColor: Color,
    settings: LampSettings,
    onChange: (LampSettings) -> Unit,
    onDetent: (Int) -> Unit,
    modifier: Modifier = Modifier,
    enterProgressMs: Long? = null,
    liftedFaderIndex: Int? = null,
    reduceEffects: Boolean = false
) {
    Box(modifier = Modifier.fillMaxWidth()) {
        ControlPanel(
            visible = visible,
            lampColor = lampColor,
            settings = settings,
            onChange = onChange,
            onDetent = onDetent,
            modifier = modifier,
            enterProgressMs = enterProgressMs,
            liftedFaderIndex = liftedFaderIndex,
            reduceEffects = reduceEffects
        )
    }
}

@Composable
fun ControlPanel(
    visible: Boolean,
    lampColor: Rgb,
    settings: LampSettings,
    onChange: (LampSettings) -> Unit,
    onDetent: (Int) -> Unit,
    modifier: Modifier = Modifier,
    enterProgressMs: Long? = null,
    liftedFaderIndex: Int? = null,
    reduceEffects: Boolean = false
) = ControlPanel(
    visible = visible,
    lampColor = Color(lampColor.r, lampColor.g, lampColor.b),
    settings = settings,
    onChange = onChange,
    onDetent = onDetent,
    modifier = modifier,
    enterProgressMs = enterProgressMs,
    liftedFaderIndex = liftedFaderIndex,
    reduceEffects = reduceEffects
)
