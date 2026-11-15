package app.enk.shell

import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.LocalViewModelStoreOwner
import androidx.lifecycle.viewmodel.compose.viewModel
import app.enk.AppContainer
import app.enk.MainViewModel
import app.enk.about.AboutScreen
import app.enk.box.BoxScreen
import app.enk.box.BoxViewModel
import app.enk.box.TrashScreen
import app.enk.design.motion.ShellMotion
import app.enk.design.shell.LocalReduceEffects
import app.enk.design.theme.LocalShellColors
import app.enk.search.SearchScreen
import app.enk.search.SearchViewModel
import app.enk.settings.SettingsScreen
import app.enk.settings.SettingsViewModel
import app.enk.widget.EnkWidgetManager
import app.enk.writer.WriterIntro
import app.enk.writer.WriterScreen
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
    val settings by container.settingsStore.settings.collectAsState(initial = mainViewModel.initialSettings)
    val density = LocalDensity.current
    val reduceEffects = LocalReduceEffects.current

    val isWriterRoute = currentRoute == Route.Writer || currentRoute is Route.Note
    SystemBarIcons(onDarkSurface = if (isWriterRoute) true else isDarkSurface(LocalShellColors.current.bg))

    val previousRoute = remember { mutableStateOf<Route?>(null) }
    LaunchedEffect(currentRoute) {
        val prev = previousRoute.value
        previousRoute.value = currentRoute
        if (prev != null && prev != currentRoute) {
            fun isTab(r: Route) = r == Route.Box || r == Route.Find || r == Route.Setup
            if (isTab(prev) && isTab(currentRoute) && settings.haptics) {
                try { container.haptics.pulse(8) } catch (_: Exception) {}
            }
        }
    }

    BackHandler(enabled = currentRoute != Route.Writer) {
        mainViewModel.handleBack()
    }

    AnimatedContent(
        targetState = currentRoute,
        modifier = modifier.fillMaxSize(),
        transitionSpec = {
            if (reduceEffects) {
                EnterTransition.None togetherWith ExitTransition.None
            } else {
                val fromRoute = initialState
                val toRoute = targetState

                fun isWriter(r: Route) = r == Route.Writer || r is Route.Note
                fun isTab(r: Route) = r == Route.Box || r == Route.Find || r == Route.Setup
                fun tabIndex(r: Route) = when (r) {
                    Route.Box -> 0
                    Route.Find -> 1
                    Route.Setup -> 2
                    else -> -1
                }
                fun isPushed(r: Route) = r == Route.Trash || r == Route.Credits

                val tabSlidePx = with(density) { 12.dp.roundToPx() }
                val writerSlidePx = with(density) { 48.dp.roundToPx() }
                val pushedSlidePx = with(density) { 24.dp.roundToPx() }

                if (isTab(fromRoute) && isTab(toRoute)) {
                    val direction = if (tabIndex(toRoute) > tabIndex(fromRoute)) 1 else -1
                    val enter = slideInHorizontally(ShellMotion.page.toSpec()) { tabSlidePx * direction } +
                            fadeIn(ShellMotion.page.toSpec())
                    val exit = slideOutHorizontally(ShellMotion.page.toSpec()) { -tabSlidePx * direction } +
                            fadeOut(ShellMotion.page.toSpec())
                    enter togetherWith exit
                } else if (isWriter(toRoute) && !isWriter(fromRoute)) {
                    val enter = slideInVertically(ShellMotion.page.toSpec()) { writerSlidePx } +
                            fadeIn(ShellMotion.page.toSpec())
                    val exit = scaleOut(ShellMotion.page.toSpec(), targetScale = 0.96f) +
                            fadeOut(ShellMotion.page.toSpec())
                    enter togetherWith exit
                } else if (isWriter(fromRoute) && !isWriter(toRoute)) {
                    val enter = scaleIn(ShellMotion.page.toSpec(), initialScale = 0.96f) +
                            fadeIn(ShellMotion.page.toSpec())
                    val exit = slideOutVertically(ShellMotion.page.toSpec()) { writerSlidePx } +
                            fadeOut(ShellMotion.page.toSpec())
                    enter togetherWith exit
                } else if (isPushed(toRoute)) {
                    val enter = slideInHorizontally(ShellMotion.page.toSpec()) { pushedSlidePx } +
                            fadeIn(ShellMotion.page.toSpec())
                    val exit = slideOutHorizontally(ShellMotion.page.toSpec()) { -pushedSlidePx } +
                            fadeOut(ShellMotion.page.toSpec())
                    enter togetherWith exit
                } else if (isPushed(fromRoute)) {
                    val enter = slideInHorizontally(ShellMotion.page.toSpec()) { -pushedSlidePx } +
                            fadeIn(ShellMotion.page.toSpec())
                    val exit = slideOutHorizontally(ShellMotion.page.toSpec()) { pushedSlidePx } +
                            fadeOut(ShellMotion.page.toSpec())
                    enter togetherWith exit
                } else {
                    fadeIn(ShellMotion.page.toSpec()) togetherWith fadeOut(ShellMotion.page.toSpec())
                }
            }
        },
        label = "screen_transition"
    ) { route ->
        when (route) {
            Route.Writer, is Route.Note -> {
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

                    val scope = rememberCoroutineScope()
                    val keyboardLayout = remember { runCatching { app.enk.keyboard.LayoutLoader.load(context) }.getOrNull() }

                    WriterScreen(
                        viewModel = writerVm,
                        settings = settings,
                        player = container.soundPlayer,
                        soundSets = container.soundManifest,
                        haptics = container.haptics,
                        intro = intro,
                        onOpenBox = { mainViewModel.navigate(Route.Box) },
                        onSettingsChange = { change ->
                            scope.launch { container.settingsStore.update { change(it) } }
                        },
                        keyboardLayout = keyboardLayout,
                        modifier = Modifier.fillMaxSize().testTag("writer_screen")
                    )
                } else {
                    Box(modifier = Modifier.fillMaxSize())
                }
            }
            Route.Box -> {
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
                    modifier = Modifier.fillMaxSize().testTag("box_screen")
                )
            }
            Route.Trash -> {
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
                    modifier = Modifier.fillMaxSize().testTag("trash_screen")
                )
            }
            Route.Find -> {
                val searchVm: SearchViewModel = viewModel(key = SEARCH_VM_KEY, factory = SearchViewModel.factory(container))
                SearchScreen(
                    viewModel = searchVm,
                    onNavigate = { mainViewModel.navigate(it.toRoute()) },
                    onNavigateToNote = { noteId, pageIndex -> mainViewModel.navigate(Route.Note(noteId, pageIndex)) },
                    onNavigateBack = { mainViewModel.handleBack() },
                    modifier = Modifier.fillMaxSize().testTag("search_screen")
                )
            }
            Route.Setup -> {
                val context = LocalContext.current
                val settingsVm: SettingsViewModel =
                    viewModel(key = SETTINGS_VM_KEY, factory = SettingsViewModel.factory(context, container))
                SettingsScreen(
                    viewModel = settingsVm,
                    onNavigate = { mainViewModel.navigate(it.toRoute()) },
                    onNavigateBack = { mainViewModel.handleBack() },
                    onOpenCredits = { mainViewModel.navigate(Route.Credits) },
                    modifier = Modifier.fillMaxSize().testTag("settings_screen")
                )
            }
            Route.Credits -> {
                AboutScreen(
                    onNavigate = { mainViewModel.navigate(it.toRoute()) },
                    onNavigateBack = { mainViewModel.handleBack() },
                    modifier = Modifier.fillMaxSize().testTag("credits_screen")
                )
            }
        }
    }
}

/** Keys of the screen view models in the activity's store, so a rotation keeps their state. */
internal const val BOX_VM_KEY = "box-vm"
internal const val SEARCH_VM_KEY = "search-vm"
internal const val SETTINGS_VM_KEY = "settings-vm"
