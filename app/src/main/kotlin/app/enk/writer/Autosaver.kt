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

class Autosaver(
    private val scope: CoroutineScope,
    private val debounceMs: Long = 500,
    private val save: suspend (List<StruckPage>) -> Unit,
) {
    private val mutex = Mutex()
    private var timerJob: Job? = null
    @Volatile private var pendingPages: List<StruckPage>? = null
    @Volatile private var isDirty: Boolean = false
    @Volatile private var lastSaveException: Throwable? = null

    fun onChange(pages: List<StruckPage>) {
        pendingPages = pages
        isDirty = true
        timerJob?.cancel()
        timerJob = scope.launch {
            try {
                delay(debounceMs)
                withContext(NonCancellable) {
                    performSave()
                }
            } catch (e: Exception) {
                if (e is CancellationException) throw e
                lastSaveException = e
            }
        }
    }

    suspend fun flush() {
        timerJob?.cancel()
        timerJob = null
        val ex = lastSaveException
        if (ex != null) {
            lastSaveException = null
            throw ex
        }
        performSave()
    }

    private suspend fun performSave() {
        mutex.withLock {
            if (!isDirty) return
            val toSave = pendingPages ?: return
            try {
                withContext(NonCancellable) {
                    save(toSave)
                }
                if (pendingPages === toSave) {
                    isDirty = false
                    lastSaveException = null
                }
            } catch (e: Throwable) {
                if (e is CancellationException) throw e
                lastSaveException = e
                throw e
            }
        }
    }
}
