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
    externalScope: kotlinx.coroutines.CoroutineScope? = null
) : ViewModel() {

    private val ownsScope = externalScope == null
    private val scope = externalScope ?: kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.SupervisorJob() + ioDispatcher)

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

    fun addDivider(name: String) {
        val validation = dividerNameValid(name, state.value.dividers.map { it.name })
        if (validation !is DividerValidationResult.Valid) return
        scope.launch(ioDispatcher) {
            try {
                val position = state.value.dividers.size
                dividers.insert(DividerEntity(name = validation.name, position = position))
            } catch (e: Exception) {
                _userMessage.value = e.message ?: "Failed to add divider"
            }
        }
    }

    fun renameDivider(id: Long, name: String) {
        val validation = dividerNameValid(name, state.value.dividers.filter { it.id != id }.map { it.name })
        if (validation !is DividerValidationResult.Valid) return
        scope.launch(ioDispatcher) {
            try {
                dividers.rename(id, validation.name)
            } catch (e: Exception) {
                _userMessage.value = e.message ?: "Failed to rename divider"
            }
        }
    }

    fun deleteDivider(id: Long) {
        if (_selectedDividerId.value == id) {
            _selectedDividerId.value = null
        }
        scope.launch(ioDispatcher) {
            try {
                dividers.delete(id)
            } catch (e: Exception) {
                _userMessage.value = e.message ?: "Failed to delete divider"
            }
        }
    }

    fun move(noteId: String, dividerId: Long?) {
        scope.launch(ioDispatcher) {
            try {
                repo.moveToDivider(noteId, dividerId)
            } catch (e: Exception) {
                _userMessage.value = e.message ?: "Failed to move note"
            }
        }
    }

    fun trash(noteId: String) {
        scope.launch(ioDispatcher) {
            try {
                repo.trash(noteId)
            } catch (e: Exception) {
                _userMessage.value = e.message ?: "Failed to trash note"
            }
        }
    }

    fun restore(noteId: String) {
        scope.launch(ioDispatcher) {
            try {
                repo.restore(noteId)
            } catch (e: Exception) {
                _userMessage.value = e.message ?: "Failed to restore note"
            }
        }
    }

    fun reorderNotes(ids: List<String>) {
        scope.launch(ioDispatcher) {
            try {
                repo.reorderNotes(_selectedDividerId.value, ids)
            } catch (e: Exception) {
                _userMessage.value = e.message ?: "Failed to reorder notes"
            }
        }
    }

    fun reorderDividers(ids: List<Long>) {
        scope.launch(ioDispatcher) {
            try {
                repo.reorderDividers(ids)
            } catch (e: Exception) {
                _userMessage.value = e.message ?: "Failed to reorder dividers"
            }
        }
    }

    fun deleteAllForever() {
        scope.launch(ioDispatcher) {
            try {
                repo.deleteAllForever()
            } catch (e: Exception) {
                _userMessage.value = e.message ?: "Failed to delete forever"
            }
        }
    }

    fun clearUserMessage() {
        _userMessage.value = null
    }

    companion object {
        fun factory(
            container: AppContainer,
            ioDispatcher: CoroutineDispatcher = Dispatchers.IO
        ) = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T =
                BoxViewModel(container.repo, container.db.dividerDao(), ioDispatcher) as T
        }
    }
}
