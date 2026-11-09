package app.enk.writer

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import app.enk.design.CourierPrime
import androidx.compose.ui.unit.dp
import app.enk.design.EnkColors
import app.enk.design.EnkMaterials
import app.enk.design.EnkMotion
import app.enk.design.EnkShadows
import app.enk.design.LampColor
import app.enk.design.Rgb
import app.enk.design.ShadowLayer
import app.enk.design.enkShadow

enum class CarriageMotion { Rest, Glide, Dragging, Snap }

object CarriageGeometry {
    const val interactiveWidthDp = 54f
    const val interactiveHeightDp = 48f
    const val bodyWidthDp = 54f
    const val bodyHeightDp = 36f
    const val slotWidthDp = 18f
    const val slotHeightDp = 11f
    const val slugWidthDp = 14f
    const val slugHeightDp = 12f
}

@Composable
fun Carriage(
    railYDp: Float,
    centerXDp: Float,
    mode: CarriageMotion,
    slugChar: Char?,
    strikeKey: Int,
    night: Boolean,
    reduceEffects: Boolean,
    lampIntensity: Float = 1f,
    lamp: Rgb = LampColor.fromWarmth(25),
    modifier: Modifier = Modifier,
    /** Freezes the slug strike at this progress (0..1) for goldens and previews; null runs the animation. */
    slugProgress: Float? = null,
    /** Reports the measured size of the interactive area in dp (54 x 48); used by the layout test. */
    onHitAreaMeasured: ((widthDp: Float, heightDp: Float) -> Unit)? = null
) {
    val density = LocalDensity.current
    val bodyShape = RoundedCornerShape(9.dp)
    val slotShape = RoundedCornerShape(topStart = 3.dp, topEnd = 3.dp, bottomEnd = 0.dp, bottomStart = 0.dp)
    val stripShape = RoundedCornerShape(2.dp)

    val lampColor = Color(red = lamp.r, green = lamp.g, blue = lamp.b)

    // Snap animates to the caret column; the overshoot comes from the easing curve itself.
    val targetX = centerXDp

    val xAnim = remember { Animatable(targetX) }

    LaunchedEffect(targetX, mode, reduceEffects) {
        when (mode) {
            CarriageMotion.Dragging -> xAnim.snapTo(targetX)
            CarriageMotion.Glide -> {
                val duration = if (reduceEffects) 100 else EnkMotion.carriageGlideMs
                val easing = if (reduceEffects) EnkMotion.themeFade else EnkMotion.carriageGlide
                xAnim.animateTo(
                    targetValue = targetX,
                    animationSpec = tween(durationMillis = duration, easing = easing)
                )
            }
            CarriageMotion.Snap -> {
                val duration = if (reduceEffects) 100 else EnkMotion.carriageSnapMs
                val easing = if (reduceEffects) EnkMotion.themeFade else EnkMotion.carriageSnap
                xAnim.animateTo(
                    targetValue = targetX,
                    animationSpec = tween(durationMillis = duration, easing = easing)
                )
            }
            CarriageMotion.Rest -> xAnim.snapTo(targetX)
        }
    }

    val currentCenterXDp = xAnim.value

    // Interactive area bounds: 54 x 48 dp, centred on body (body top = railYDp - 17, center = railYDp + 1)
    val interactiveLeftDp = currentCenterXDp - (CarriageGeometry.interactiveWidthDp / 2f)
    val interactiveTopDp = railYDp - 23f

    Box(
        modifier = modifier.fillMaxSize()
    ) {
        Box(
            modifier = Modifier
                .offset(x = interactiveLeftDp.dp, y = interactiveTopDp.dp)
                .size(
                    width = CarriageGeometry.interactiveWidthDp.dp,
                    height = CarriageGeometry.interactiveHeightDp.dp
                )
                .testTag("carriage")
                .then(
                    if (onHitAreaMeasured == null) Modifier else Modifier.onGloballyPositioned {
                        onHitAreaMeasured(it.size.width / density.density, it.size.height / density.density)
                    }
                )
        ) {
            // 1. Slot window: 18 x 11 dp, centred horizontally (left = (54 - 18)/2 = 18 dp), top = 0 dp
            val slotShadowLayers = if (night && lampIntensity > 0f) {
                listOf(
                    ShadowLayer(0.dp, 0.dp, 14.dp, 5.dp, lampColor.copy(alpha = 0.75f * lampIntensity), false)
                ) + EnkShadows.slot
            } else {
                EnkShadows.slot
            }

            Box(
                modifier = Modifier
                    .offset(x = 18.dp, y = 0.dp)
                    .size(width = CarriageGeometry.slotWidthDp.dp, height = CarriageGeometry.slotHeightDp.dp)
                    .enkShadow(slotShadowLayers, slotShape)
                    .clip(slotShape)
                    .background(if (night) lampColor else EnkColors.slotDark)
            ) {
                // Inside slot window: TypeSlug centred at x = 2 dp, y = 2 dp
                TypeSlug(
                    slugChar = slugChar,
                    strikeKey = strikeKey,
                    night = night,
                    slugProgress = slugProgress,
                    modifier = Modifier.offset(x = 2.dp, y = 2.dp)
                )
            }

            // 2. Body: 54 x 36 dp, top at y = 6 dp, left = 0 dp
            Box(
                modifier = Modifier
                    .offset(x = 0.dp, y = 6.dp)
                    .size(width = CarriageGeometry.bodyWidthDp.dp, height = CarriageGeometry.bodyHeightDp.dp)
                    .enkShadow(EnkShadows.cap, bodyShape)
                    .background(EnkMaterials.graphiteCap, bodyShape)
            ) {
                // Top collar: 5 dp band
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(5.dp)
                        .background(
                            EnkMaterials.collarTop,
                            RoundedCornerShape(topStart = 9.dp, topEnd = 9.dp)
                        )
                )

                // Bottom collar: 5 dp band
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth()
                        .height(5.dp)
                        .background(
                            EnkMaterials.collarBottom,
                            RoundedCornerShape(bottomStart = 9.dp, bottomEnd = 9.dp)
                        )
                )

                // Grip grooves: inset 11 dp sides, 10 dp bottom, 8 dp height, 2 dp radius
                Canvas(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = 10.dp)
                        .size(width = 32.dp, height = 8.dp)
                        .clip(RoundedCornerShape(2.dp))
                ) {
                    val lineWidth = 1.5.dp.toPx()
                    val period = 5.dp.toPx()
                    var x = 0f
                    while (x <= size.width) {
                        drawRect(
                            color = Color.White.copy(alpha = 0.16f),
                            topLeft = Offset(x, 0f),
                            size = Size(lineWidth, size.height)
                        )
                        x += period
                    }
                }

                // Indicator strip: 16 x 3 dp horizontally centred, top inset 10 dp
                val stripShadow = if (night) {
                    listOf(ShadowLayer(0.dp, 0.dp, 8.dp, 2.dp, EnkColors.accentGlowNight, false))
                } else {
                    emptyList()
                }

                Box(
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .padding(top = 10.dp)
                        .size(width = 16.dp, height = 3.dp)
                        .enkShadow(stripShadow, stripShape)
                        .background(EnkColors.accent, stripShape)
                        .border(1.dp, Color.Black.copy(alpha = 0.35f), stripShape)
                )
            }
        }
    }
}
