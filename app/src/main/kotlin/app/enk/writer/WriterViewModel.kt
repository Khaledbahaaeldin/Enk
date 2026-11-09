package app.enk.writer

import androidx.lifecycle.ViewModel
import app.enk.data.NoteRepository
import app.enk.settings.CorrectionMode
import app.enk.settings.SettingsStore
import app.enk.text.StruckPage
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
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

    private val ownsScope = ioScope == null
    private val scope = ioScope ?: CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var settingsJob: Job? = null
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
            // Take the user's correction mode before applying any held event.
            currentCorrection = settings.settings.first().correction
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

        settingsJob = scope.launch {
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

    /** Flushes on the view model's own scope so a closing screen cannot cancel the write. */
    fun flushAsync() {
        scope.launch { flush() }
    }

    suspend fun flush() {
        if (_state.value.missing) return
        autosaver.flush()
    }

    override fun onCleared() {
        super.onCleared()
        settingsJob?.cancel()
        if (_state.value.missing) {
            if (ownsScope) scope.cancel()
            return
        }
        // Flush the last edits, then release the scope we own. A failed flush must not crash the process.
        scope.launch {
            try {
                autosaver.flush()
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                // Nothing more can be done here; the pages were retried by the autosaver already.
            } finally {
                if (ownsScope) scope.cancel()
            }
        }
    }
}
