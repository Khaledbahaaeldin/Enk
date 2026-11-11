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
import app.enk.about.AboutScreen
import app.enk.box.BoxScreen
import app.enk.box.BoxViewModel
import app.enk.box.TrashScreen
import app.enk.search.SearchScreen
import app.enk.search.SearchViewModel
import app.enk.settings.SettingsScreen
import app.enk.settings.SettingsViewModel
import app.enk.writer.WriterScreen
import app.enk.writer.WriterIntro
import app.enk.widget.EnkWidgetManager
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
                            WriterViewModel(
                                noteId,
                                container.repo,
                                container.settingsStore,
                                initialPage = page,
                                onPostSave = { EnkWidgetManager.requestUpdate(context.applicationContext) }
                            ) as T
                    }
                    if (activity != null) {
                        ViewModelProvider(activity, factory)["writer-$noteId", WriterViewModel::class.java]
                    } else if (storeOwner != null) {
                        ViewModelProvider(storeOwner, factory)["writer-$noteId", WriterViewModel::class.java]
                    } else {
                        WriterViewModel(
                            noteId,
                            container.repo,
                            container.settingsStore,
                            initialPage = page,
                            onPostSave = { EnkWidgetManager.requestUpdate(context.applicationContext) }
                        )
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
        Route.Box -> {
            SystemBarIcons(onDarkSurface = isDarkSurface(LocalShellColors.current.bg))
            val context = LocalContext.current
            val boxVm: BoxViewModel = viewModel(
                key = BOX_VM_KEY,
                factory = BoxViewModel.factory(
                    container,
                    onWidgetUpdate = { EnkWidgetManager.requestUpdate(context.applicationContext) }
                )
            )
            BoxScreen(
                viewModel = boxVm,
                onNavigate = { mainViewModel.navigate(it.toRoute()) },
                onNavigateToNote = { noteId -> mainViewModel.navigate(Route.Note(noteId, 0)) },
                onNewNote = { boxVm.newNote { id -> mainViewModel.navigate(Route.Note(id, 0)) } },
                onOpenTrash = { mainViewModel.navigate(Route.Trash) },
                onNavigateBack = { mainViewModel.handleBack() },
                modifier = modifier
            )
        }
        Route.Trash -> {
            SystemBarIcons(onDarkSurface = isDarkSurface(LocalShellColors.current.bg))
            val context = LocalContext.current
            val boxVm: BoxViewModel = viewModel(
                key = BOX_VM_KEY,
                factory = BoxViewModel.factory(
                    container,
                    onWidgetUpdate = { EnkWidgetManager.requestUpdate(context.applicationContext) }
                )
            )
            TrashScreen(
                viewModel = boxVm,
                onNavigate = { mainViewModel.navigate(it.toRoute()) },
                onNavigateBack = { mainViewModel.handleBack() },
                modifier = modifier
            )
        }
        Route.Find -> {
            SystemBarIcons(onDarkSurface = isDarkSurface(LocalShellColors.current.bg))
            val searchVm: SearchViewModel = viewModel(key = SEARCH_VM_KEY, factory = SearchViewModel.factory(container))
            SearchScreen(
                viewModel = searchVm,
                onNavigate = { mainViewModel.navigate(it.toRoute()) },
                onNavigateToNote = { noteId, pageIndex -> mainViewModel.navigate(Route.Note(noteId, pageIndex)) },
                onNavigateBack = { mainViewModel.handleBack() },
                modifier = modifier
            )
        }
        Route.Setup -> {
            SystemBarIcons(onDarkSurface = isDarkSurface(LocalShellColors.current.bg))
            val context = LocalContext.current
            val settingsVm: SettingsViewModel =
                viewModel(key = SETTINGS_VM_KEY, factory = SettingsViewModel.factory(context, container))
            SettingsScreen(
                viewModel = settingsVm,
                onNavigate = { mainViewModel.navigate(it.toRoute()) },
                onNavigateBack = { mainViewModel.handleBack() },
                onOpenCredits = { mainViewModel.navigate(Route.Credits) },
                modifier = modifier
            )
        }
        Route.Credits -> {
            SystemBarIcons(onDarkSurface = isDarkSurface(LocalShellColors.current.bg))
            AboutScreen(
                onNavigate = { mainViewModel.navigate(it.toRoute()) },
                onNavigateBack = { mainViewModel.handleBack() },
                modifier = modifier
            )
        }
    }
}

/** Keys of the screen view models in the activity's store, so a rotation keeps their state. */
internal const val BOX_VM_KEY = "box-vm"
internal const val SEARCH_VM_KEY = "search-vm"
internal const val SETTINGS_VM_KEY = "settings-vm"
