package app.enk.shell

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import app.enk.design.shell.GlassSource
import app.enk.design.shell.ShellBackground
import app.enk.design.shell.collapseProgress
import app.enk.design.shell.glassSource
import app.enk.design.shell.rememberGlassSource

@Composable
fun ShellScaffold(
    title: String,
    meta: String?,
    showNav: Boolean,
    selected: Dest?,
    onNavigate: (Dest) -> Unit,
    modifier: Modifier = Modifier,
    listState: LazyListState = rememberLazyListState(),
    content: @Composable (listState: LazyListState, contentPadding: PaddingValues) -> Unit
) {
    val density = LocalDensity.current
    val glassSource = rememberGlassSource()

    val itemHeights = remember { mutableMapOf<Int, Int>() }

    val scrollYDp by remember(listState) {
        derivedStateOf {
            val layoutInfo = listState.layoutInfo
            layoutInfo.visibleItemsInfo.forEach {
                itemHeights[it.index] = it.size
            }
            val firstIndex = listState.firstVisibleItemIndex
            val firstOffset = listState.firstVisibleItemScrollOffset

            var totalPx = 0
            for (i in 0 until firstIndex) {
                totalPx += itemHeights[i] ?: (layoutInfo.visibleItemsInfo.firstOrNull()?.size ?: with(density) { 50.dp.toPx().toInt() })
            }
            totalPx += firstOffset
            with(density) { totalPx.toDp().value }
        }
    }

    val progress = collapseProgress(scrollYDp)

    var largeTitleHeightPx by remember { mutableIntStateOf(0) }
    val topPaddingDp = if (largeTitleHeightPx > 0) with(density) { largeTitleHeightPx.toDp() } else 124.dp
    val contentPadding = PaddingValues(top = topPaddingDp, bottom = 120.dp)

    ShellBackground(modifier = modifier.fillMaxSize()) {
        // Scrolling content layer, registered as glass backdrop source
        Box(
            modifier = Modifier
                .fillMaxSize()
                .glassSource(glassSource)
        ) {
            content(listState, contentPadding)
        }

        // Large title scrolls with content
        LargeTitle(
            title = title,
            meta = meta,
            modifier = Modifier
                .align(Alignment.TopCenter)
                .graphicsLayer {
                    translationY = -with(density) { scrollYDp.dp.toPx() }
                }
                // Measured outside the status bar padding, so the content below starts under the
                // whole title including the bar; measured inside it, titles and content overlapped.
                .onSizeChanged { largeTitleHeightPx = it.height }
                .windowInsetsPadding(WindowInsets.statusBars)
        )

        // Compact bar pinned at top
        CompactBar(
            title = title,
            progress = progress,
            glassSource = glassSource,
            modifier = Modifier
                .align(Alignment.TopCenter)
                .windowInsetsPadding(WindowInsets.statusBars)
        )

        // Floating GlassNavBar at bottom
        if (showNav) {
            val navSelected = selected ?: Dest.Write
            GlassNavBar(
                selected = navSelected,
                onSelect = onNavigate,
                glassSource = glassSource,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .windowInsetsPadding(WindowInsets.navigationBars)
                    .padding(start = 22.dp, end = 22.dp, bottom = 22.dp)
            )
        }
    }
}
