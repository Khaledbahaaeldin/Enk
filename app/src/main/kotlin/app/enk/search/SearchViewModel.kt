package app.enk.search

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import app.enk.AppContainer
import app.enk.data.SearchHit
import app.enk.data.SearchRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.withContext

@OptIn(FlowPreview::class, ExperimentalCoroutinesApi::class)
class SearchViewModel(
    private val repo: SearchRepository,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
    externalScope: CoroutineScope? = null
) : ViewModel() {

    private val ownsScope = externalScope == null
    private val scope = externalScope ?: CoroutineScope(SupervisorJob() + ioDispatcher)

    val query = MutableStateFlow("")

    val results: StateFlow<List<SearchHit>> = query
        .debounce(200L)
        .flatMapLatest { q ->
            flow {
                val trimmed = q.trim()
                if (trimmed.isEmpty()) {
                    emit(emptyList())
                } else {
                    val hits = try {
                        withContext(ioDispatcher) { repo.search(trimmed) }
                    } catch (e: CancellationException) {
                        throw e
                    } catch (_: Exception) {
                        emptyList()
                    }
                    emit(hits)
                }
            }
        }
        .stateIn(
            scope = scope,
            started = SharingStarted.Eagerly,
            initialValue = emptyList()
        )

    override fun onCleared() {
        super.onCleared()
        if (ownsScope) {
            scope.cancel()
        }
    }

    companion object {
        fun factory(
            container: AppContainer,
            ioDispatcher: CoroutineDispatcher = Dispatchers.IO
        ): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    return SearchViewModel(container.searchRepo, ioDispatcher) as T
                }
            }
    }
}
