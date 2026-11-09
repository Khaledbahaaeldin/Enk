package app.enk.writer

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import app.enk.design.EnkColors
import app.enk.design.EnkMaterials
import app.enk.design.EnkMotion
import app.enk.design.EnkType
import app.enk.design.LocalMonoFont

fun slugOffsetDp(t: Float, startOffset: Float = 9f): Float {
    val clampedT = t.coerceIn(0f, 1f)
    return if (clampedT <= 0.45f) {
        val fraction = clampedT / 0.45f
        val eased = EnkMotion.slugStrike.transform(fraction)
        startOffset + (-5f - startOffset) * eased
    } else {
        val fraction = (clampedT - 0.45f) / 0.55f
        val eased = EnkMotion.slugStrike.transform(fraction)
        -5f + (9f - (-5f)) * eased
    }
}

fun filterSlugChar(char: Char?): Char? =
    if (char == null || char.isWhitespace() || char.isISOControl()) null else char

@Composable
fun TypeSlug(
    slugChar: Char?,
    strikeKey: Int,
    night: Boolean,
    modifier: Modifier = Modifier,
    /** Freezes the strike at this progress (0..1) for goldens and previews; null runs the real animation. */
    slugProgress: Float? = null
) {
    val slugShape = RoundedCornerShape(topStart = 2.dp, topEnd = 2.dp, bottomEnd = 0.dp, bottomStart = 0.dp)
    val filteredChar = filterSlugChar(slugChar)

    val progressAnim = remember { Animatable(1f) }
    var inFlightStartOffset by remember { mutableFloatStateOf(9f) }
    // A key already present when the slug first appears (for example after rotation) must not strike.
    var seenKey by remember { mutableIntStateOf(strikeKey) }

    LaunchedEffect(strikeKey) {
        if (strikeKey != seenKey) {
            seenKey = strikeKey
            val currentOffset = slugOffsetDp(progressAnim.value, inFlightStartOffset)
            inFlightStartOffset = currentOffset
            progressAnim.snapTo(0f)
            progressAnim.animateTo(
                targetValue = 1f,
                animationSpec = tween(
                    durationMillis = EnkMotion.slugStrikeMs,
                    easing = LinearEasing
                )
            )
        }
    }

    val offsetDp = slugOffsetDp(slugProgress ?: progressAnim.value, if (slugProgress != null) 9f else inFlightStartOffset)
    val monoFont = LocalMonoFont.current

    Box(
        modifier = modifier
            .offset(y = offsetDp.dp)
            .size(width = 14.dp, height = 12.dp)
            .background(EnkMaterials.slugSteel, slugShape)
            .then(
                if (night) Modifier.border(1.dp, Color.Black.copy(alpha = 0.25f), slugShape) else Modifier
            ),
        contentAlignment = Alignment.Center
    ) {
        if (filteredChar != null) {
            Text(
                text = filteredChar.toString(),
                style = EnkType.slug.copy(fontFamily = monoFont)
            )
        }
    }
}
