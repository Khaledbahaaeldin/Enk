package app.enk.box

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.border
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.enk.design.LocalMonoFont
import app.enk.design.shell.GlassSource
import app.enk.design.shell.rememberGlassSource
import app.enk.design.theme.LocalShellColors
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTag
import app.enk.shell.Dest
import app.enk.shell.GlassNavBar
import app.enk.shell.ShellScaffold

@Composable
fun BoxContent(
    state: BoxUiState,
    modifier: Modifier = Modifier,
    onSelectDivider: (Long?) -> Unit = {},
    onAddDivider: (String) -> Unit = {},
    onAddDividerClick: () -> Unit = {},
    onNewNote: () -> Unit = {},
    onNoteClick: (String) -> Unit = {},
    onNoteHold: (String) -> Unit = {},
    onMoveToDivider: (String, Long?) -> Unit = { _, _ -> },
    onTrashNote: (String) -> Unit = {},
    listState: LazyListState = rememberLazyListState(),
    contentPadding: PaddingValues = PaddingValues(top = 16.dp, bottom = 120.dp),
    glassSource: GlassSource = rememberGlassSource()
) {
    val colors = LocalShellColors.current
    val mono = LocalMonoFont.current
    val isInspection = LocalInspectionMode.current

    var showNewDividerSheet by remember { mutableStateOf(false) }
    var heldNoteId by remember { mutableStateOf<String?>(null) }
    var movingNoteId by remember { mutableStateOf<String?>(null) }

    val hasOverlay = heldNoteId != null || showNewDividerSheet || movingNoteId != null
    BackHandler(enabled = hasOverlay) {
        heldNoteId = null
        showNewDividerSheet = false
        movingNoteId = null
    }

    val cardBoundsMap = remember { mutableStateMapOf<String, Rect>() }

    val metaText = when (state.notes.size) {
        0 -> "0 notes"
        1 -> "1 note"
        else -> "${state.notes.size} notes"
    }

    val entranceProgress = remember(state.selectedDividerId) {
        Animatable(if (isInspection) 1f else 0f)
    }

    LaunchedEffect(state.selectedDividerId) {
        if (!isInspection) {
            entranceProgress.snapTo(0f)
            entranceProgress.animateTo(
                targetValue = 1f,
                animationSpec = tween(
                    durationMillis = 500,
                    easing = CubicBezierEasing(0.2f, 0.9f, 0.3f, 1f)
                )
            )
        }
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
            // Header: Title and Note count
            item(key = "box_header") {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier
                            .semantics(mergeDescendants = true) {
                                testTag = "large_title"
                            }
                            .fillMaxWidth()
                            .padding(top = 8.dp, bottom = 12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.Bottom
                    ) {
                        Text(
                            text = "The Box",
                            fontSize = 32.sp,
                            fontWeight = FontWeight.Bold,
                            color = colors.t1
                        )
                        Text(
                            text = metaText,
                            fontFamily = mono,
                            fontSize = 14.sp,
                            color = colors.t2,
                            modifier = Modifier.padding(bottom = 4.dp)
                        )
                    }

                    // Divider strip
                    DividerStrip(
                        dividers = state.dividers,
                        selectedId = state.selectedDividerId,
                        onSelect = onSelectDivider,
                        onAdd = {
                            showNewDividerSheet = true
                            onAddDividerClick()
                        },
                        glassSource = glassSource,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(12.dp))
                }
            }

            // Ghost Card: Always first!
            item(key = "ghost_card") {
                val ghostShape = RoundedCornerShape(14.dp)
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                        .clip(ghostShape)
                        .border(1.dp, colors.hair, ghostShape)
                        .clickable(onClick = onNewNote)
                        .padding(16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "New note",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                        color = colors.t2
                    )
                }
            }

            // Real Note Cards
            itemsIndexed(state.notes, key = { _, item -> item.id }) { index, note ->
                val delayFraction = (index * 0.08f).coerceAtMost(0.5f)
                val cardProgress = if (isInspection) 1f else {
                    ((entranceProgress.value - delayFraction) / (1f - delayFraction)).coerceIn(0f, 1f)
                }
                val cardAlpha = cardProgress
                val cardOffsetY = 14.dp * (1f - cardProgress)

                val isLifted = heldNoteId == note.id

                NoteCard(
                    summary = note,
                    onClick = { onNoteClick(note.id) },
                    onHold = {
                        heldNoteId = note.id
                        onNoteHold(note.id)
                    },
                    lifted = isLifted,
                    onMoveToDivider = {
                        movingNoteId = note.id
                    },
                    onMoveToTrash = {
                        onTrashNote(note.id)
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .onGloballyPositioned { coordinates ->
                            cardBoundsMap[note.id] = coordinates.boundsInRoot()
                        }
                        .graphicsLayer {
                            alpha = cardAlpha
                            translationY = cardOffsetY.toPx()
                        }
                )
            }

            // End hint
            if (state.notes.isNotEmpty()) {
                item(key = "end_hint") {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Hold a note for options",
                        fontFamily = mono,
                        fontSize = 11.sp,
                        color = colors.t3,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }

        // Context menu overlay
        if (heldNoteId != null) {
            val bounds = cardBoundsMap[heldNoteId]
            ContextMenu(
                cardBounds = bounds,
                onMoveToDivider = {
                    val id = heldNoteId
                    heldNoteId = null
                    movingNoteId = id
                },
                onMoveToTrash = {
                    val id = heldNoteId
                    heldNoteId = null
                    if (id != null) onTrashNote(id)
                },
                onDismiss = {
                    heldNoteId = null
                },
                glassSource = glassSource
            )
        }

        // New divider sheet
        if (showNewDividerSheet) {
            NewDividerSheet(
                onDismiss = { showNewDividerSheet = false },
                onAdd = { name ->
                    showNewDividerSheet = false
                    onAddDivider(name)
                },
                existingNames = state.dividers.map { it.name },
                glassSource = glassSource
            )
        }

        // Move to divider sheet
        if (movingNoteId != null) {
            MoveToDividerSheet(
                dividers = state.dividers,
                onSelectDivider = { divId ->
                    val noteId = movingNoteId
                    movingNoteId = null
                    if (noteId != null) onMoveToDivider(noteId, divId)
                },
                onDismiss = { movingNoteId = null },
                glassSource = glassSource
            )
        }
    }
}

@Composable
fun BoxScreen(
    state: BoxUiState,
    modifier: Modifier = Modifier,
    onSelectDivider: (Long?) -> Unit = {},
    onAddDivider: (String) -> Unit = {},
    onAddDividerClick: () -> Unit = {},
    onNewNote: () -> Unit = {},
    onNoteClick: (String) -> Unit = {},
    onTrashNote: (String) -> Unit = {},
    onMoveToDivider: (String, Long?) -> Unit = { _, _ -> },
    onNavigateBack: () -> Unit = {},
    onNavigate: (Dest) -> Unit = {}
) {
    BackHandler {
        onNavigateBack()
    }

    Box(modifier = modifier.fillMaxSize()) {
        BoxContent(
            state = state,
            onSelectDivider = onSelectDivider,
            onAddDivider = onAddDivider,
            onAddDividerClick = onAddDividerClick,
            onNewNote = onNewNote,
            onNoteClick = onNoteClick,
            onTrashNote = onTrashNote,
            onMoveToDivider = onMoveToDivider,
            modifier = Modifier.fillMaxSize()
        )

        GlassNavBar(
            selected = Dest.Box,
            onSelect = onNavigate,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .windowInsetsPadding(WindowInsets.navigationBars)
                .padding(horizontal = 24.dp, vertical = 16.dp)
        )
    }
}

@Composable
fun BoxScreen(
    viewModel: BoxViewModel,
    onNavigate: (Dest) -> Unit,
    onNavigateToNote: (String) -> Unit,
    onNewNote: () -> Unit,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val state by viewModel.state.collectAsState()

    BoxScreen(
        state = state,
        onSelectDivider = { viewModel.select(it) },
        onAddDivider = { viewModel.addDivider(it) },
        onNewNote = onNewNote,
        onNoteClick = onNavigateToNote,
        onTrashNote = { viewModel.trash(it) },
        onMoveToDivider = { noteId, divId -> viewModel.move(noteId, divId) },
        onNavigateBack = onNavigateBack,
        onNavigate = onNavigate,
        modifier = modifier
    )
}
