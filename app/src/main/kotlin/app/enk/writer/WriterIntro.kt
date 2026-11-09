package app.enk.writer

import androidx.compose.runtime.Stable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer

/**
 * The start animation's hold on the writer: while it is non-null the writer lets the paper rise,
 * fades its text, top bar and own rail, carriage and caret in, and leaves its chassis to the
 * animation's backdrop. Every value is read in the draw phase, so nothing recomposes per frame.
 * The app passes null once the animation is over (and on every non-cold start).
 */
@Stable
class WriterIntro(
    val paperOffsetDp: () -> Float,
    val textAlpha: () -> Float,
    val topBarAlpha: () -> Float,
    val ownPartsAlpha: () -> Float
)

fun Modifier.introPaper(intro: WriterIntro?): Modifier =
    if (intro == null) this else graphicsLayer { translationY = intro.paperOffsetDp() * density }

fun Modifier.introAlpha(alpha: (() -> Float)?): Modifier =
    if (alpha == null) this else graphicsLayer { this.alpha = alpha().coerceIn(0f, 1f) }
