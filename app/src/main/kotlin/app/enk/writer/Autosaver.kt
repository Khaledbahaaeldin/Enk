package app.enk.writer

import app.enk.text.StruckPage
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

/**
 * Debounced, serialised saver.
 *
 * - [onChange] remembers the newest pages and (re)starts a [debounceMs] timer; a burst of changes
 *   produces one save.
 * - Saves never overlap (one mutex) and a started save is never cancelled half way.
 * - A failed background save is retried every [debounceMs] until it succeeds or newer pages replace it.
 * - [flush] always attempts the pending save now. If it fails it throws and the pages stay pending,
 *   so a later flush or the background retry still saves them.
 */
class Autosaver(
    private val scope: CoroutineScope,
    private val debounceMs: Long = 500,
    private val save: suspend (List<StruckPage>) -> Unit,
) {
    private val mutex = Mutex()
    private var timerJob: Job? = null
    @Volatile private var pendingPages: List<StruckPage>? = null
    @Volatile private var isDirty: Boolean = false

    fun onChange(pages: List<StruckPage>) {
        pendingPages = pages
        isDirty = true
        timerJob?.cancel()
        timerJob = scope.launch { saveWithRetry() }
    }

    suspend fun flush() {
        timerJob?.cancel()
        timerJob = null
        try {
            performSave()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            timerJob = scope.launch { saveWithRetry() }
            throw e
        }
    }

    private suspend fun saveWithRetry() {
        while (true) {
            delay(debounceMs)
            try {
                performSave()
                return
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                // Keep the pages pending and try again after the next debounce interval.
            }
        }
    }

    private suspend fun performSave() {
        mutex.withLock {
            if (!isDirty) return
            val toSave = pendingPages ?: return
            withContext(NonCancellable) { save(toSave) }
            if (pendingPages === toSave) isDirty = false
        }
    }
}
