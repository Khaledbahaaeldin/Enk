package app.enk.shell

import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.LocalViewModelStoreOwner
import androidx.lifecycle.viewmodel.compose.viewModel
import app.enk.AppContainer
import app.enk.MainViewModel
import app.enk.design.theme.LocalShellColors
import app.enk.box.BoxScreen
import app.enk.box.BoxViewModel
import app.enk.box.TrashScreen
import kotlinx.coroutines.Dispatchers
import app.enk.writer.WriterScreen
import app.enk.writer.WriterIntro
import app.enk.writer.WriterViewModel
import kotlinx.coroutines.launch

@Composable
fun EnkNavHost(
    container: AppContainer,
    modifier: Modifier = Modifier,
    mainViewModel: MainViewModel = viewModel(
        factory = MainViewModel.factory(LocalContext.current)
    ),
    /** Passed to the writer while the start animation runs; null otherwise. */
    intro: WriterIntro? = null
) {
    val currentRoute by mainViewModel.currentRoute.collectAsState()
    val noteIdState by mainViewModel.currentNoteId.collectAsState()

    BackHandler(enabled = currentRoute != Route.Writer) {
        mainViewModel.handleBack()
    }

    when (val route = currentRoute) {
        Route.Writer, is Route.Note -> {
            // The writer's chassis is dark graphite in day and night.
            SystemBarIcons(onDarkSurface = true)
            val noteId = if (route is Route.Note) route.id else noteIdState
            if (noteId != null) {
                val context = LocalContext.current
                val activity = context as? ComponentActivity
                val storeOwner = LocalViewModelStoreOwner.current

                val writerVm = remember(noteId, activity, storeOwner) {
                    val page = if (route is Route.Note) route.page else 0
                    val factory = object : ViewModelProvider.Factory {
                        @Suppress("UNCHECKED_CAST")
                        override fun <T : ViewModel> create(modelClass: Class<T>): T =
                            WriterViewModel(noteId, container.repo, container.settingsStore, initialPage = page) as T
                    }
                    if (activity != null) {
                        ViewModelProvider(activity, factory)["writer-$noteId", WriterViewModel::class.java]
                    } else if (storeOwner != null) {
                        ViewModelProvider(storeOwner, factory)["writer-$noteId", WriterViewModel::class.java]
                    } else {
                        WriterViewModel(noteId, container.repo, container.settingsStore, initialPage = page)
                    }
                }

                val settings by container.settingsStore.settings.collectAsState(initial = mainViewModel.initialSettings)
                val scope = rememberCoroutineScope()

                WriterScreen(
                    viewModel = writerVm,
                    settings = settings,
                    player = container.soundPlayer,
                    soundSets = container.soundManifest,
                    haptics = container.haptics,
                    intro = intro,
                    onOpenBox = { mainViewModel.navigate(Route.Box) },
                    onSettingsChange = { change ->
                        scope.launch { container.settingsStore.update { it.change() } }
                    }
                )
            } else {
                Box(modifier = modifier.fillMaxSize())
            }
        }
        Route.Box, Route.Trash -> {
            val context = LocalContext.current
            val activity = context as? ComponentActivity
            val storeOwner = LocalViewModelStoreOwner.current

            val boxVm = remember(activity, storeOwner) {
                val factory = BoxViewModel.factory(container)
                if (activity != null) {
                    ViewModelProvider(activity, factory)["box-vm", BoxViewModel::class.java]
                } else if (storeOwner != null) {
                    ViewModelProvider(storeOwner, factory)["box-vm", BoxViewModel::class.java]
                } else {
                    BoxViewModel(container.repo, container.db.dividerDao())
                }
            }

            SystemBarIcons(onDarkSurface = isDarkSurface(LocalShellColors.current.bg))
            val scope = rememberCoroutineScope()

            if (route == Route.Box) {
                BoxScreen(
                    viewModel = boxVm,
                    onNavigate = { dest ->
                        val nextRoute = when (dest) {
                            Dest.Write -> Route.Writer
                            Dest.Box -> Route.Box
                            Dest.Find -> Route.Find
                            Dest.Setup -> Route.Setup
                        }
                        mainViewModel.navigate(nextRoute)
                    },
                    onNavigateToNote = { noteId ->
                        mainViewModel.navigate(Route.Note(noteId, 0))
                    },
                    onNewNote = {
                        scope.launch(Dispatchers.IO) {
                            try {
                                val selectedDivider = boxVm.state.value.selectedDividerId
                                val newNote = container.repo.createNote(dividerId = selectedDivider)
                                mainViewModel.navigate(Route.Note(newNote.id, 0))
                            } catch (_: Exception) {
                            }
                        }
                    },
                    onNavigateBack = {
                        mainViewModel.handleBack()
                    },
                    modifier = modifier
                )
            } else {
                TrashScreen(
                    viewModel = boxVm,
                    onNavigate = { dest ->
                        val nextRoute = when (dest) {
                            Dest.Write -> Route.Writer
                            Dest.Box -> Route.Box
                            Dest.Find -> Route.Find
                            Dest.Setup -> Route.Setup
                        }
                        mainViewModel.navigate(nextRoute)
                    },
                    onNavigateBack = {
                        mainViewModel.handleBack()
                    },
                    modifier = modifier
                )
            }
        }
        Route.Find -> {
            val searchVm: app.enk.search.SearchViewModel = viewModel(
                factory = app.enk.search.SearchViewModel.factory(container)
            )
            SystemBarIcons(onDarkSurface = isDarkSurface(LocalShellColors.current.bg))
            app.enk.search.SearchScreen(
                viewModel = searchVm,
                onNavigate = { dest ->
                    val nextRoute = when (dest) {
                        Dest.Write -> Route.Writer
                        Dest.Box -> Route.Box
                        Dest.Find -> Route.Find
                        Dest.Setup -> Route.Setup
                    }
                    mainViewModel.navigate(nextRoute)
                },
                onNavigateToNote = { noteId, pageIndex ->
                    mainViewModel.navigate(Route.Note(noteId, pageIndex))
                },
                onNavigateBack = {
                    mainViewModel.handleBack()
                },
                modifier = modifier
            )
        }
        Route.Setup -> {
            val title = "Setup"
            val meta = "Enk 1.0"

            SystemBarIcons(onDarkSurface = isDarkSurface(LocalShellColors.current.bg))
            ShellScaffold(
                title = title,
                meta = meta,
                showNav = true,
                selected = navSelection(route),
                onNavigate = { dest ->
                    val nextRoute = when (dest) {
                        Dest.Write -> Route.Writer
                        Dest.Box -> Route.Box
                        Dest.Find -> Route.Find
                        Dest.Setup -> Route.Setup
                    }
                    mainViewModel.navigate(nextRoute)
                },
                modifier = modifier
            ) { _, _ ->
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = title,
                        color = LocalShellColors.current.t3
                    )
                }
            }
        }
    }
}
