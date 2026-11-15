package app.enk.shell

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import app.enk.design.glass.GlassToast
import app.enk.design.shell.GlassSource
import app.enk.design.shell.rememberGlassSource

/** A short one-shot message above the navigation bar (a failed move, an import result). */
@Composable
fun MessagePill(
    text: String,
    modifier: Modifier = Modifier,
    tag: String = "message_pill",
    glassSource: GlassSource = rememberGlassSource()
) {
    GlassToast(
        text = text,
        modifier = modifier,
        tag = tag,
        source = glassSource
    )
}
