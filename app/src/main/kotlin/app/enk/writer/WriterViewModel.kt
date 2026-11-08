package app.enk.writer

import androidx.lifecycle.ViewModel
import app.enk.data.NoteRepository
import app.enk.settings.CorrectionMode
import app.enk.settings.SettingsStore
import app.enk.text.StruckPage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class WriterViewModel(
    private val noteId: String,
    private val repo: NoteRepository,
    private val settings: SettingsStore,
    ioScope: CoroutineScope? = null,
) : ViewModel() {

    private val scope = ioScope ?: CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var currentCorrection: CorrectionMode = CorrectionMode.CleanErase

    private val autosaver = Autosaver(
        scope = scope,
        debounceMs = 500,
        save = { pages ->
            if (!_state.value.missing) {
                repo.savePages(noteId, pages)
            }
        }
    )

    private val pendingEvents = mutableListOf<WriterEvent>()
    private val lock = Any()

    private val _state = MutableStateFlow(
        WriterState(
            noteId = noteId,
            pages = emptyList(),
            pageIndex = 0,
            caret = 0,
            loaded = false,
            missing = false
        )
    )
    val state: StateFlow<WriterState> = _state.asStateFlow()

    init {
        scope.launch {
            val isTrashed = repo.observeTrashed().first().any { it.id == noteId }
            val note = if (isTrashed) null else repo.load(noteId)
            if (note == null) {
                _state.value = _state.value.copy(missing = true, loaded = true)
                return@launch
            }
            val initialPages = note.pages.ifEmpty { listOf(StruckPage("")) }
            var current = WriterState(
                noteId = noteId,
                pages = initialPages,
                pageIndex = 0,
                caret = initialPages.first().length,
                loaded = true
            )
            synchronized(lock) {
                for (e in pendingEvents) {
                    current = reduce(current, e, currentCorrection)
                }
                pendingEvents.clear()
                _state.value = current
            }
            if (current.pages != initialPages) {
                autosaver.onChange(current.pages)
            }
        }

        scope.launch {
            settings.settings.collect { s ->
                currentCorrection = s.correction
            }
        }
    }

    fun dispatch(e: WriterEvent) {
        synchronized(lock) {
            val current = _state.value
            if (current.missing) return
            if (!current.loaded) {
                pendingEvents.add(e)
                return
            }
            val oldState = current
            val newState = reduce(oldState, e, currentCorrection)
            _state.value = newState
            if (newState.pages != oldState.pages) {
                autosaver.onChange(newState.pages)
            }
        }
    }

    suspend fun flush() {
        if (_state.value.missing) return
        autosaver.flush()
    }

    override fun onCleared() {
        super.onCleared()
        if (_state.value.missing) return
        scope.launch {
            autosaver.flush()
        }
    }
}
