package app.enk.design.glass

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.enk.design.effects.LocalEffects
import app.enk.design.effects.ResolvedEffects
import app.enk.design.motion.ShellMotion
import app.enk.design.shell.GlassSource
import app.enk.design.shell.LocalReduceEffects
import app.enk.design.shell.rememberGlassSource
import app.enk.design.theme.LocalShellColors

@Composable
fun GlassToast(
    text: String,
    modifier: Modifier = Modifier,
    tag: String = "message_pill",
    source: GlassSource = rememberGlassSource(),
    visible: Boolean = true
) {
    val colors = LocalShellColors.current
    val shape = RoundedCornerShape(20.dp)
    val effects = LocalEffects.current
    val reduceEffects = LocalReduceEffects.current

    val anim = remember { Animatable(if (visible) 1f else 0f) }
    LaunchedEffect(visible, effects, reduceEffects) {
        val target = if (visible) 1f else 0f
        val spec: androidx.compose.animation.core.AnimationSpec<Float> = if (effects == ResolvedEffects.Light || reduceEffects) {
            tween(100, easing = LinearEasing)
        } else {
            ShellMotion.sheet.toSpec()
        }
        anim.animateTo(target, animationSpec = spec)
    }

    val progress = anim.value
    if (progress > 0.001f || visible) {
        val riseDp = (1f - progress.coerceIn(0f, 1f)) * 16f
        Box(
            modifier = modifier
                .testTag(tag)
                .offset(y = riseDp.dp)
                .graphicsLayer { alpha = progress.coerceIn(0f, 1f) }
                .glassSurface(spec = GlassSpecs.Toast, shape = shape, source = source)
                .clip(shape)
                .padding(horizontal = 18.dp, vertical = 12.dp)
        ) {
            Text(
                text = text,
                color = colors.t1,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium
            )
        }
    }
}
