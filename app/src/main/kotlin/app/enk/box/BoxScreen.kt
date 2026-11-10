package app.enk.box

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.enk.design.LocalMonoFont
import app.enk.design.shell.rememberGlassSource
import app.enk.design.theme.LocalShellColors
import app.enk.shell.Dest
import app.enk.shell.MessagePill
import app.enk.shell.ShellScaffold
import kotlinx.coroutines.delay

/** How long a one-shot message stays on screen. */
internal const val MESSAGE_VISIBLE_MS = 3_000L

/** "0 notes", "1 note", "N notes". */
internal fun noteCountText(count: Int): String = when (count) {
    1 -> "1 note"
    else -> "$count notes"
}

/**
 * The Box (design system 12.10): large title with the note count, the divider strip, the ghost card,
 * the note cards and a Trash row, inside the shared [ShellScaffold]. The context menu, the sheets and
 * the message are drawn above the scaffold so they cover its navigation bar.
 *
 * [entranceProgress] freezes the cards' entrance for goldens (0 to 1); it is null in the app, where the
 * entrance runs when the list first appears and when the divider changes.
 */
@Composable
fun BoxScreen(
    state: BoxUiState,
    modifier: Modifier = Modifier,
    onSelectDivider: (Long?) -> Unit = {},
    onAddDivider: (String) -> Unit = {},
    onNewNote: () -> Unit = {},
    onNoteClick: (String) -> Unit = {},
    onTrashNote: (String) -> Unit = {},
    onMoveToDivider: (String, Long?) -> Unit = { _, _ -> },
    onOpenTrash: () -> Unit = {},
    onMessageShown: () -> Unit = {},
    onNavigateBack: () -> Unit = {},
    onNavigate: (Dest) -> Unit = {},
    entranceProgress: Float? = null
) {
    var showNewDividerSheet by remember { mutableStateOf(false) }
    var heldNoteId by remember { mutableStateOf<String?>(null) }
    var movingNoteId by remember { mutableStateOf<String?>(null) }
    val cardBoundsMap = remember { mutableStateMapOf<String, Rect>() }
    val glassSource = rememberGlassSource()

    BackHandler { onNavigateBack() }
    // Registered after the one above, so it wins while something is open: back closes it first.
    BackHandler(enabled = heldNoteId != null || showNewDividerSheet || movingNoteId != null) {
        heldNoteId = null
        showNewDividerSheet = false
        movingNoteId = null
    }

    Box(modifier = modifier.fillMaxSize()) {
        ShellScaffold(
            title = "The Box",
            meta = noteCountText(state.notes.size),
            showNav = true,
            selected = Dest.Box,
            onNavigate = onNavigate
        ) { listState, contentPadding ->
            BoxList(
                state = state,
                listState = listState,
                contentPadding = contentPadding,
                heldNoteId = heldNoteId,
                cardBoundsMap = cardBoundsMap,
                onSelectDivider = onSelectDivider,
                onAddDividerClick = { showNewDividerSheet = true },
                onNewNote = onNewNote,
                onNoteClick = onNoteClick,
                onNoteHold = { heldNoteId = it },
                onNoteMove = { movingNoteId = it },
                onNoteTrash = onTrashNote,
                onOpenTrash = onOpenTrash,
                entranceProgress = entranceProgress
            )
        }

        state.userMessage?.let { message ->
            val latestShown by rememberUpdatedState(onMessageShown)
            LaunchedEffect(message) {
                delay(MESSAGE_VISIBLE_MS)
                latestShown()
            }
            MessagePill(
                text = message,
                tag = "box_message",
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .windowInsetsPadding(WindowInsets.navigationBars)
                    .padding(bottom = 110.dp, start = 24.dp, end = 24.dp)
            )
        }

        if (heldNoteId != null) {
            ContextMenu(
                cardBounds = cardBoundsMap[heldNoteId],
                onMoveToDivider = {
                    movingNoteId = heldNoteId
                    heldNoteId = null
                },
                onMoveToTrash = {
                    val id = heldNoteId
                    heldNoteId = null
                    if (id != null) onTrashNote(id)
                },
                onDismiss = { heldNoteId = null },
                glassSource = glassSource
            )
        }

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

        if (movingNoteId != null) {
            MoveToDividerSheet(
                dividers = state.dividers,
                onSelectDivider = { dividerId ->
                    val noteId = movingNoteId
                    movingNoteId = null
                    if (noteId != null) onMoveToDivider(noteId, dividerId)
                },
                onDismiss = { movingNoteId = null },
                glassSource = glassSource
            )
        }
    }
}

@Composable
private fun BoxList(
    state: BoxUiState,
    listState: LazyListState,
    contentPadding: PaddingValues,
    heldNoteId: String?,
    cardBoundsMap: MutableMap<String, Rect>,
    onSelectDivider: (Long?) -> Unit,
    onAddDividerClick: () -> Unit,
    onNewNote: () -> Unit,
    onNoteClick: (String) -> Unit,
    onNoteHold: (String) -> Unit,
    onNoteMove: (String) -> Unit,
    onNoteTrash: (String) -> Unit,
    onOpenTrash: () -> Unit,
    entranceProgress: Float?
) {
    val colors = LocalShellColors.current
    val mono = LocalMonoFont.current

    val entrance = remember(state.selectedDividerId) { Animatable(entranceProgress ?: 0f) }
    LaunchedEffect(state.selectedDividerId, entranceProgress) {
        if (entranceProgress == null) {
            entrance.snapTo(0f)
            entrance.animateTo(
                targetValue = 1f,
                animationSpec = tween(durationMillis = 500, easing = CubicBezierEasing(0.2f, 0.9f, 0.3f, 1f))
            )
        }
    }

    LazyColumn(
        state = listState,
        contentPadding = contentPadding,
        verticalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp)
    ) {
        item(key = "divider_strip") {
            DividerStrip(
                dividers = state.dividers,
                selectedId = state.selectedDividerId,
                onSelect = onSelectDivider,
                onAdd = onAddDividerClick,
                modifier = Modifier.fillMaxWidth().testTag("divider_strip")
            )
        }

        item(key = "ghost_card") {
            val ghostShape = RoundedCornerShape(14.dp)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .testTag("ghost_card")
                    .clip(ghostShape)
                    .border(1.dp, colors.hair, ghostShape)
                    .clickable(onClick = onNewNote)
                    .padding(16.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(text = "New note", fontSize = 14.sp, fontWeight = FontWeight.Medium, color = colors.t2)
            }
        }

        itemsIndexed(state.notes, key = { _, item -> item.id }) { index, note ->
            val delayFraction = (index * 0.08f).coerceAtMost(0.5f)
            val cardProgress = ((entrance.value - delayFraction) / (1f - delayFraction)).coerceIn(0f, 1f)
            NoteCard(
                summary = note,
                onClick = { onNoteClick(note.id) },
                onHold = { onNoteHold(note.id) },
                lifted = heldNoteId == note.id,
                onMoveToDivider = { onNoteMove(note.id) },
                onMoveToTrash = { onNoteTrash(note.id) },
                modifier = Modifier
                    .fillMaxWidth()
                    .onGloballyPositioned { cardBoundsMap[note.id] = it.boundsInRoot() }
                    .graphicsLayer {
                        alpha = cardProgress
                        translationY = 14.dp.toPx() * (1f - cardProgress)
                    }
            )
        }

        if (state.notes.isNotEmpty()) {
            item(key = "end_hint") {
                Text(
                    text = "Hold a note for options",
                    fontFamily = mono,
                    fontSize = 11.sp,
                    color = colors.t3,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp)
                )
            }
        }

        item(key = "trash_row") {
            TrashRow(count = state.trashCount, onClick = onOpenTrash)
        }
    }
}

/** The way into the Trash: a row below the notes, with the number of trashed notes. */
@Composable
internal fun TrashRow(count: Int, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val colors = LocalShellColors.current
    val shape = RoundedCornerShape(14.dp)
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(52.dp)
            .testTag("trash_row")
            .clip(shape)
            .background(colors.g1)
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = "Trash", fontSize = 15.sp, fontWeight = FontWeight.Medium, color = colors.t1)
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(text = if (count == 0) "Empty" else noteCountText(count), fontSize = 14.sp, color = colors.t2)
            Spacer(modifier = Modifier.padding(start = 8.dp))
            Text(text = "›", fontSize = 18.sp, fontWeight = FontWeight.SemiBold, color = colors.t3)
        }
    }
}

@Composable
fun BoxScreen(
    viewModel: BoxViewModel,
    onNavigate: (Dest) -> Unit,
    onNavigateToNote: (String) -> Unit,
    onNewNote: () -> Unit,
    onOpenTrash: () -> Unit,
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
        onMoveToDivider = { noteId, dividerId -> viewModel.move(noteId, dividerId) },
        onOpenTrash = onOpenTrash,
        onMessageShown = { viewModel.clearUserMessage() },
        onNavigateBack = onNavigateBack,
        onNavigate = onNavigate,
        modifier = modifier
    )
}
