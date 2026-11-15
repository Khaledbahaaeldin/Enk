package app.enk.design.shell

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import com.kyant.backdrop.backdrops.LayerBackdrop
import com.kyant.backdrop.backdrops.layerBackdrop
import com.kyant.backdrop.backdrops.rememberLayerBackdrop

class GlassSource(backdrop: LayerBackdrop? = null) {
    var backdrop: LayerBackdrop? = backdrop
        internal set
}

@Composable
fun rememberGlassSource(): GlassSource {
    val backdrop = rememberLayerBackdrop()
    return remember(backdrop) { GlassSource(backdrop) }
}

fun Modifier.glassSource(source: GlassSource): Modifier = composed {
    val backdrop = source.backdrop ?: rememberLayerBackdrop().also { source.backdrop = it }
    this.layerBackdrop(backdrop)
}
