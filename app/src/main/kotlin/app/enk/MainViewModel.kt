package app.enk

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import app.enk.data.Note
import app.enk.settings.Settings
import app.enk.shell.Route
import app.enk.shell.routeBack
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

/**
 * Activity-scoped state: the container, the route, and the note and settings the app opens with.
 * It survives rotation and releases the container when the activity is finished for good.
 */
class MainViewModel(
    context: Context,
    val container: AppContainer = DefaultAppContainer(context.applicationContext),
    ioDispatcher: CoroutineDispatcher = Dispatchers.IO
) : ViewModel() {

    private val _isLoaded = MutableStateFlow(false)
    val isLoaded: StateFlow<Boolean> = _isLoaded.asStateFlow()

    /** The most recent note as it was when the app opened; null on a first launch. */
    private val _restoredNote = MutableStateFlow<Note?>(null)
    val restoredNote: StateFlow<Note?> = _restoredNote.asStateFlow()

    private val _currentNoteId = MutableStateFlow<String?>(null)
    val currentNoteId: StateFlow<String?> = _currentNoteId.asStateFlow()

    private val _currentRoute = MutableStateFlow<Route>(Route.Writer)
    val currentRoute: StateFlow<Route> = _currentRoute.asStateFlow()

    /** The settings read before the first frame, so the first composition already has the right theme. */
    @Volatile
    var initialSettings: Settings = Settings()
        private set

    fun navigate(route: Route) {
        if (route is Route.Note) {
            _currentNoteId.value = route.id
        }
        _currentRoute.value = route
    }

    /** Applies back to the route; false when the route is the writer and the app should close. */
    fun handleBack(): Boolean {
        val next = routeBack(_currentRoute.value) ?: return false
        _currentRoute.value = next
        return true
    }

    init {
        viewModelScope.launch(ioDispatcher) {
            container.soundManifest // read the asset manifest off the main thread
            try {
                initialSettings = container.settingsStore.settings.first()
            } catch (_: java.io.IOException) {
                // Unreadable settings: open with the defaults rather than not at all.
            }
            val recent = container.repo.mostRecent()
            val note = recent ?: container.repo.createNote(dividerId = null)
            _currentNoteId.value = note.id
            _restoredNote.value = recent
            _isLoaded.value = true
            try {
                container.repo.purgeExpired()
            } catch (e: kotlinx.coroutines.CancellationException) {
                throw e
            } catch (_: Exception) {
                // Old trash stays until the next launch; nothing to tell the user.
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        container.soundPlayer.release()
        container.db.close()
    }

    companion object {
        fun factory(
            context: Context,
            container: AppContainer? = null,
            ioDispatcher: CoroutineDispatcher = Dispatchers.IO
        ) = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T = MainViewModel(
                context.applicationContext,
                container ?: DefaultAppContainer(context.applicationContext),
                ioDispatcher
            ) as T
        }
    }
}
