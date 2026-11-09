package app.enk

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import app.enk.data.Note
import app.enk.shell.Route
import app.enk.shell.routeBack
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class MainViewModel(
    context: Context,
    val container: AppContainer = DefaultAppContainer(context.applicationContext)
) : ViewModel() {

    private val _isLoaded = MutableStateFlow(false)
    val isLoaded: StateFlow<Boolean> = _isLoaded.asStateFlow()

    private val _restoredNote = MutableStateFlow<Note?>(null)
    val restoredNote: StateFlow<Note?> = _restoredNote.asStateFlow()

    private val _currentNoteId = MutableStateFlow<String?>(null)
    val currentNoteId: StateFlow<String?> = _currentNoteId.asStateFlow()

    private val _currentRoute = MutableStateFlow<Route>(Route.Writer)
    val currentRoute: StateFlow<Route> = _currentRoute.asStateFlow()

    fun navigate(route: Route) {
        _currentRoute.value = route
    }

    fun handleBack(): Boolean {
        val next = routeBack(_currentRoute.value)
        return if (next != null) {
            _currentRoute.value = next
            true
        } else {
            false
        }
    }

    init {
        viewModelScope.launch(Dispatchers.IO) {
            container.soundManifest // read the asset manifest off the main thread
            try {
                container.settingsStore.settings.first()
            } catch (_: Exception) {}
            val recent = container.repo.mostRecent()
            val note = recent ?: container.repo.createNote(dividerId = null)
            _currentNoteId.value = note.id
            _restoredNote.value = recent
            _isLoaded.value = true
        }
    }

    override fun onCleared() {
        super.onCleared()
        container.soundPlayer.release()
    }

    companion object {
        fun factory(context: Context, container: AppContainer? = null) = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return MainViewModel(
                    context.applicationContext,
                    container ?: DefaultAppContainer(context.applicationContext)
                ) as T
            }
        }
    }
}
