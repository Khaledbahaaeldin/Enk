package app.enk.start

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier

@Composable
fun SplashHost(
    targets: StartTargets,
    shouldPlay: Boolean,
    reduceMotion: Boolean,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    var showOverlay by remember { mutableStateOf(shouldPlay) }

    Box(modifier = modifier.fillMaxSize()) {
        content()
        if (showOverlay) {
            StartOverlay(
                targets = targets,
                reduceMotion = reduceMotion,
                onFinished = { showOverlay = false }
            )
        }
    }
}
