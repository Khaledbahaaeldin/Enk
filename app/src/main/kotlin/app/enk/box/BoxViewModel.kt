package app.enk.box

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import app.enk.AppContainer
import app.enk.data.DividerDao
import app.enk.data.DividerEntity
import app.enk.data.NoteRepository
import app.enk.data.NoteSummary
import app.enk.data.TrashedSummary
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class BoxUiState(
    val dividers: List<DividerEntity> = emptyList(),
    val selectedDividerId: Long? = null,
    val notes: List<NoteSummary> = emptyList(),
    val trashCount: Int = 0,
    val trashed: List<TrashedSummary> = emptyList(),
    val userMessage: String? = null
)

sealed interface DividerValidationResult {
    data object Blank : DividerValidationResult
    data object TooLong : DividerValidationResult
    data object Duplicate : DividerValidationResult
    data class Valid(val name: String) : DividerValidationResult
}

class BoxViewModel(
    val repo: NoteRepository,
    val dividers: DividerDao,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
    externalScope: kotlinx.coroutines.CoroutineScope? = null,
    private val onWidgetUpdate: (() -> Unit)? = null
) : ViewModel() {

    private val ownsScope = externalScope == null
    private val scope = externalScope ?: kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.SupervisorJob() + ioDispatcher)

    private val deleting = java.util.concurrent.atomic.AtomicBoolean(false)
    private val _selectedDividerId = MutableStateFlow<Long?>(null)
    private val _userMessage = MutableStateFlow<String?>(null)

    @OptIn(ExperimentalCoroutinesApi::class)
    private val _notesFlow = _selectedDividerId.flatMapLatest { id ->
        repo.observeBox(id)
    }

    val state: StateFlow<BoxUiState> = combine(
        dividers.observeAll(),
        _selectedDividerId,
        _notesFlow,
        repo.observeTrashed(),
        _userMessage
    ) { divList, selectedId, noteList, trashedList, msg ->
        BoxUiState(
            dividers = divList,
            selectedDividerId = selectedId,
            notes = noteList,
            trashCount = trashedList.size,
            trashed = trashedList,
            userMessage = msg
        )
    }.stateIn(
        scope = scope,
        started = SharingStarted.Eagerly,
        initialValue = BoxUiState()
    )

    override fun onCleared() {
        super.onCleared()
        if (ownsScope) {
            scope.cancel()
        }
    }

    fun select(dividerId: Long?) {
        _selectedDividerId.value = dividerId
    }

    fun dividerNameValid(name: String, existingNames: List<String>): DividerValidationResult {
        val trimmed = name.trim()
        if (trimmed.isEmpty()) return DividerValidationResult.Blank
        if (trimmed.length > 40) return DividerValidationResult.TooLong
        if (existingNames.any { it.equals(trimmed, ignoreCase = true) }) return DividerValidationResult.Duplicate
        return DividerValidationResult.Valid(trimmed)
    }

    fun dividerNameValid(name: String): Boolean =
        dividerNameValid(name, state.value.dividers.map { it.name }) is DividerValidationResult.Valid

    fun validateDividerName(name: String, existingNames: List<String> = emptyList()): DividerValidationResult =
        dividerNameValid(name, existingNames)

    /**
     * Runs [block] on the view model's scope. A failure leaves the list as it was and shows [failure]
     * once; cancellation is never swallowed.
     */
    private fun guarded(failure: String, block: suspend () -> Unit) {
        scope.launch(ioDispatcher) {
            try {
                block()
            } catch (e: CancellationException) {
                throw e
            } catch (_: Exception) {
                _userMessage.value = failure
            }
        }
    }

    fun addDivider(name: String) {
        val validation = dividerNameValid(name, state.value.dividers.map { it.name })
        if (validation !is DividerValidationResult.Valid) return
        guarded("Couldn't add that divider.") {
            dividers.insert(DividerEntity(name = validation.name, position = state.value.dividers.size))
        }
    }

    fun renameDivider(id: Long, name: String) {
        val validation = dividerNameValid(name, state.value.dividers.filter { it.id != id }.map { it.name })
        if (validation !is DividerValidationResult.Valid) return
        guarded("Couldn't rename that divider.") { dividers.rename(id, validation.name) }
    }

    fun deleteDivider(id: Long) {
        if (_selectedDividerId.value == id) {
            _selectedDividerId.value = null
        }
        guarded("Couldn't delete that divider.") { dividers.delete(id) }
    }

    fun move(noteId: String, dividerId: Long?) = guarded("Couldn't move that note.") { repo.moveToDivider(noteId, dividerId) }

    fun trash(noteId: String) = guarded("Couldn't move that note to the trash.") {
        repo.trash(noteId)
        onWidgetUpdate?.invoke()
    }

    fun restore(noteId: String) = guarded("Couldn't restore that note.") {
        repo.restore(noteId)
        onWidgetUpdate?.invoke()
    }

    fun reorderNotes(ids: List<String>) =
        guarded("Couldn't reorder the notes.") { repo.reorderNotes(_selectedDividerId.value, ids) }

    fun reorderDividers(ids: List<Long>) = guarded("Couldn't reorder the dividers.") { repo.reorderDividers(ids) }

    /** Erases every trashed note for good. A second call while one is running does nothing. */
    fun deleteAllForever() {
        if (!deleting.compareAndSet(false, true)) return
        guarded("Couldn't erase the trash.") {
            try {
                repo.deleteAllForever()
            } finally {
                deleting.set(false)
            }
        }
    }

    /**
     * Creates a note in the selected divider and reports its id. On failure the Box stays as it is and
     * a message is shown.
     */
    fun newNote(onCreated: (String) -> Unit) {
        scope.launch(ioDispatcher) {
            val created = try {
                repo.createNote(dividerId = _selectedDividerId.value)
            } catch (e: CancellationException) {
                throw e
            } catch (_: Exception) {
                _userMessage.value = "Couldn't create a note."
                return@launch
            }
            onCreated(created.id)
        }
    }

    fun clearUserMessage() {
        _userMessage.value = null
    }

    companion object {
        fun factory(
            container: AppContainer,
            ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
            onWidgetUpdate: (() -> Unit)? = null
        ) = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T =
                BoxViewModel(container.repo, container.db.dividerDao(), ioDispatcher, onWidgetUpdate = onWidgetUpdate) as T
        }
    }
}
