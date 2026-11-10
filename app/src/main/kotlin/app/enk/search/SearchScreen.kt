package app.enk.search

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.enk.box.NoteCard
import app.enk.data.SearchHit
import app.enk.design.theme.LocalShellColors
import app.enk.shell.Dest
import app.enk.shell.ShellScaffold

@Composable
fun SearchContent(
    query: String,
    onQueryChange: (String) -> Unit,
    results: List<SearchHit>,
    onHitClick: (noteId: String, pageIndex: Int) -> Unit,
    modifier: Modifier = Modifier,
    listState: LazyListState = rememberLazyListState(),
    showNav: Boolean = true,
    onNavigate: (Dest) -> Unit = {}
) {
    val colors = LocalShellColors.current

    val meta = remember(query, results.size) {
        if (query.trim().isEmpty()) {
            "all notes"
        } else when (results.size) {
            1 -> "1 match"
            else -> "${results.size} matches"
        }
    }

    ShellScaffold(
        title = "Find",
        meta = meta,
        showNav = showNav,
        selected = Dest.Find,
        onNavigate = onNavigate,
        listState = listState,
        modifier = modifier
    ) { _, contentPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = contentPadding.calculateTopPadding())
        ) {
            // Pinned SearchField
            SearchField(
                query = query,
                onQueryChange = onQueryChange,
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp)
            )

            if (query.trim().isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(top = 40.dp, start = 20.dp, end = 20.dp),
                    contentAlignment = Alignment.TopCenter
                ) {
                    Text(
                        text = "Type to search your notes.",
                        fontFamily = FontFamily.Default,
                        fontSize = 14.sp,
                        color = colors.t3,
                        textAlign = TextAlign.Center
                    )
                }
            } else if (results.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(top = 40.dp, start = 20.dp, end = 20.dp),
                    contentAlignment = Alignment.TopCenter
                ) {
                    Text(
                        text = "Nothing found for “${query.trim()}”.",
                        fontFamily = FontFamily.Default,
                        fontSize = 14.sp,
                        color = colors.t3,
                        textAlign = TextAlign.Center
                    )
                }
            } else {
                LazyColumn(
                    state = listState,
                    contentPadding = PaddingValues(
                        top = 8.dp,
                        bottom = contentPadding.calculateBottomPadding(),
                        start = 20.dp,
                        end = 20.dp
                    ),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier
                        .fillMaxSize()
                        .testTag("search_results_list")
                ) {
                    items(results, key = { "${it.noteId}_${it.pageIndex}" }) { hit ->
                        NoteCard(
                            hit = hit,
                            query = query,
                            onClick = { onHitClick(hit.noteId, hit.pageIndex) },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun SearchScreen(
    viewModel: SearchViewModel,
    onNavigate: (Dest) -> Unit,
    onNavigateToNote: (noteId: String, pageIndex: Int) -> Unit,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val query by viewModel.query.collectAsState()
    val results by viewModel.results.collectAsState()

    BackHandler {
        onNavigateBack()
    }

    SearchContent(
        query = query,
        onQueryChange = { viewModel.query.value = it },
        results = results,
        onHitClick = onNavigateToNote,
        onNavigate = onNavigate,
        modifier = modifier
    )
}
