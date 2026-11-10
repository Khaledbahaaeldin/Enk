package app.enk.box

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.enk.data.TrashedSummary
import app.enk.design.LocalMonoFont
import app.enk.design.ShadowLayer
import app.enk.design.enkShadow
import app.enk.design.paperGrain
import app.enk.design.shell.GlassLevel
import app.enk.design.shell.GlassSource
import app.enk.design.shell.glass
import app.enk.design.shell.rememberGlassSource
import app.enk.design.theme.LocalShellColors
import app.enk.design.theme.LocalShellMaterial
import app.enk.shell.Dest
import app.enk.shell.ShellScaffold

val TrashPillFill = Color(0xEB201E1C)
val TrashPillText = Color(0xFFF3EEE3)

@Composable
fun TrashContent(
    trashedNotes: List<TrashedSummary>,
    modifier: Modifier = Modifier,
    onRestore: (String) -> Unit = {},
    onDeleteAllForever: () -> Unit = {},
    listState: LazyListState = rememberLazyListState(),
    contentPadding: PaddingValues = PaddingValues(top = 124.dp, bottom = 120.dp),
    glassSource: GlassSource = rememberGlassSource()
) {
    val colors = LocalShellColors.current
    val material = LocalShellMaterial.current
    val density = LocalDensity.current
    val mono = LocalMonoFont.current

    val cardShape = RoundedCornerShape(14.dp)
    val pillShape = RoundedCornerShape(14.dp)
    val buttonShape = RoundedCornerShape(18.dp)

    val shadowLayers = remember(material.cardShadow) {
        listOf(
            ShadowLayer(0.dp, 0.5.dp, 0.dp, 0.dp, Color.White.copy(alpha = 0.60f), inset = true),
            ShadowLayer(0.dp, 1.dp, 2.dp, 0.dp, Color.Black.copy(alpha = 0.25f)),
            ShadowLayer(0.dp, 8.dp, 18.dp, (-8).dp, material.cardShadow)
        )
    }

    Box(modifier = modifier.fillMaxSize()) {
        LazyColumn(
            state = listState,
            contentPadding = contentPadding,
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp)
        ) {
            items(trashedNotes, key = { it.id }) { item ->
                val daysText = when (item.daysRemaining) {
                    0 -> "Today"
                    1 -> "1 day left"
                    else -> "${item.daysRemaining} days left"
                }

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .enkShadow(shadowLayers, cardShape)
                        .clip(cardShape)
                        .background(PaperColor)
                        .paperGrain(0.3f)
                        .padding(top = 14.dp, start = 16.dp, end = 16.dp, bottom = 12.dp)
                ) {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        // Title with 1.5 dp strikethrough line
                        Box(modifier = Modifier.fillMaxWidth()) {
                            Text(
                                text = item.title,
                                fontFamily = mono,
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp,
                                color = CardTextColor.copy(alpha = 0.7f),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                textDecoration = TextDecoration.LineThrough,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .drawWithContent {
                                        drawContent()
                                        val strokeWidthPx = with(density) { 1.5.dp.toPx() }
                                        val y = size.height / 2f
                                        drawLine(
                                            color = CardTextColor,
                                            start = Offset(0f, y),
                                            end = Offset(size.width, y),
                                            strokeWidth = strokeWidthPx
                                        )
                                    }
                            )
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        // Footer: days left and Restore button
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = daysText,
                                fontFamily = mono,
                                fontSize = 12.sp,
                                color = CardSubtextColor
                            )

                            // Paper-pill Restore button with min 48 dp touch target
                            Box(
                                modifier = Modifier
                                    .sizeIn(minWidth = 48.dp, minHeight = 48.dp)
                                    .clickable { onRestore(item.id) },
                                contentAlignment = Alignment.Center
                            ) {
                                Box(
                                    modifier = Modifier
                                        .height(28.dp)
                                        .clip(pillShape)
                                        .background(TrashPillFill)
                                        .padding(horizontal = 13.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = "Restore",
                                        color = TrashPillText,
                                        fontSize = 12.5.sp,
                                        fontWeight = FontWeight.Medium
                                    )
                                }
                            }
                        }
                    }
                }
            }

            if (trashedNotes.isEmpty()) {
                item(key = "trash_empty") {
                    Text(
                        text = "Nothing in the trash.",
                        fontSize = 14.sp,
                        color = colors.t3,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth().padding(top = 24.dp)
                    )
                }
            }

            // Footer info
            item {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Notes in the trash are removed after 30 days.",
                    fontSize = 13.sp,
                    color = colors.t3,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            // Delete all forever button
            if (trashedNotes.isNotEmpty()) {
                item {
                    Spacer(modifier = Modifier.height(12.dp))
                    Box(
                        modifier = Modifier.fillMaxWidth(),
                        contentAlignment = Alignment.Center
                    ) {
                        Box(
                            modifier = Modifier
                                .height(36.dp)
                                .glass(GlassLevel.Regular, buttonShape, glassSource)
                                .clip(buttonShape)
                                .clickable(onClick = onDeleteAllForever)
                                .padding(horizontal = 18.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "Delete all forever",
                                color = colors.danger,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }
            }
        }
    }
}

/** The Trash with its Delete all forever alert, from plain values; the screen below feeds it from the view model. */
@Composable
fun TrashScreen(
    trashed: List<TrashedSummary>,
    onRestore: (String) -> Unit,
    onDeleteAllForever: () -> Unit,
    onNavigate: (Dest) -> Unit,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    var alertOpen by remember { mutableStateOf(false) }

    BackHandler { onNavigateBack() }
    // Registered after the one above, so it wins while the alert is open.
    BackHandler(enabled = alertOpen) { alertOpen = false }

    Box(modifier = modifier.fillMaxSize()) {
        ShellScaffold(
            title = "Trash",
            meta = null,
            showNav = true,
            selected = Dest.Box,
            onNavigate = onNavigate
        ) { listState, contentPadding ->
            TrashContent(
                trashedNotes = trashed,
                onRestore = onRestore,
                onDeleteAllForever = { alertOpen = true },
                listState = listState,
                contentPadding = contentPadding
            )
        }

        // Above the scaffold, so the alert and its scrim cover the navigation bar.
        if (alertOpen) {
            DeleteForeverAlert(
                count = trashed.size,
                onConfirm = {
                    alertOpen = false
                    onDeleteAllForever()
                },
                onDismiss = { alertOpen = false }
            )
        }
    }
}

@Composable
fun TrashScreen(
    viewModel: BoxViewModel,
    onNavigate: (Dest) -> Unit,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val state by viewModel.state.collectAsState()
    TrashScreen(
        trashed = state.trashed,
        onRestore = { viewModel.restore(it) },
        onDeleteAllForever = { viewModel.deleteAllForever() },
        onNavigate = onNavigate,
        onNavigateBack = onNavigateBack,
        modifier = modifier
    )
}
