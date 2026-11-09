package app.enk

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class MainViewModel(context: Context) : ViewModel() {
    val container: AppContainer = DefaultAppContainer(context.applicationContext)

    private val _currentNoteId = MutableStateFlow<String?>(null)
    val currentNoteId: StateFlow<String?> = _currentNoteId.asStateFlow()

    init {
        viewModelScope.launch(Dispatchers.IO) {
            container.soundManifest // read the asset manifest off the main thread
            val note = container.repo.mostRecent() ?: container.repo.createNote(dividerId = null)
            _currentNoteId.value = note.id
        }
    }

    override fun onCleared() {
        super.onCleared()
        container.soundPlayer.release()
        (container as? DefaultAppContainer)?.db?.close()
    }

    companion object {
        fun factory(context: Context) = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return MainViewModel(context.applicationContext) as T
            }
        }
    }
}
