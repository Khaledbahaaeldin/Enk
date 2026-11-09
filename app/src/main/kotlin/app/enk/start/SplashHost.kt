package app.enk.start

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.runtime.withFrameNanos
import app.enk.shell.SystemBarIcons
import app.enk.writer.WriterIntro

private const val SKIP_MS = 120

/**
 * Hosts the writer under the start animation. Layers, bottom to top: the splash backdrop, the
 * writer (told through [WriterIntro] to rise, fade in and hold its own chassis back), and the
 * overlay with the logo's rail, carriage and caret. When the animation ends, or is skipped, or is
 * not played at all, the writer is the only thing left and [content] receives null.
 *
 * [progressMs] freezes the animation at a time for goldens; it is null in the app.
 * [onFirstFrame] runs after the first frame with the layers composed, so the caller can drop the
 * system splash view without a gap.
 */
@Composable
fun SplashHost(
    firstPageText: String,
    lightSplash: Boolean,
    shouldPlay: Boolean,
    reduceMotion: Boolean,
    modifier: Modifier = Modifier,
    progressMs: Float? = null,
    onFirstFrame: () -> Unit = {},
    onFinished: () -> Unit = {},
    content: @Composable (intro: WriterIntro?) -> Unit
) {
    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
        val targets = rememberStartTargets(maxWidth, maxHeight, firstPageText, lightSplash)

        var playing by remember { mutableStateOf(shouldPlay) }
        var skipped by remember { mutableStateOf(false) }
        val progress = remember { Animatable(0f) }
        val latestFinished by rememberUpdatedState(onFinished)

        LaunchedEffect(Unit) {
            withFrameNanos { }
            withFrameNanos { }
            onFirstFrame()
        }

        LaunchedEffect(playing, skipped, reduceMotion, progressMs) {
            if (!playing || progressMs != null) return@LaunchedEffect
            val duration = when {
                skipped -> SKIP_MS
                reduceMotion -> StartGate.reducedMotionDurationMs()
                else -> StartTimeline.DURATION_MS
            }
            progress.animateTo(1f, tween(durationMillis = duration, easing = LinearEasing))
            playing = false
            latestFinished()
        }

        val frame = remember(targets, progressMs) {
            derivedStateOf {
                StartTimeline.at(progressMs ?: (progress.value * StartTimeline.DURATION_MS), targets)
            }
        }
        val intro = remember(frame) {
            WriterIntro(
                paperOffsetDp = { frame.value.paperOffsetY },
                textAlpha = { frame.value.textAlpha },
                topBarAlpha = { frame.value.topBarAlpha },
                ownPartsAlpha = { frame.value.ownPartsAlpha }
            )
        }
        val moving = playing && !reduceMotion

        if (moving) StartBackdrop(frame, targets)
        content(if (moving) intro else null)
        if (moving && lightSplash) {
            // The light splash is cream until the chassis circle has covered it; the status bar icons
            // must be dark until then and light after. Declared after the content so it wins.
            val covered by remember(frame) { derivedStateOf { frame.value.apertureRadius >= targets.coverRadius() } }
            SystemBarIcons(onDarkSurface = covered)
        }
        if (playing) {
            BackHandler { skipped = true }
            if (reduceMotion) {
                StartSplashFade(targets, alpha = { 1f - progress.value }, onSkip = { skipped = true })
            } else {
                StartOverlay(frame, targets, onSkip = { skipped = true })
            }
        }
    }
}
