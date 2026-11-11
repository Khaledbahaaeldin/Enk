package app.enk.widget

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Coalesces frequent update requests so at most one update runs every [minIntervalMs] ms,
 * while scheduling a trailing update so the final state change is never lost.
 */
class UpdateGate(
    private val clock: () -> Long = { System.currentTimeMillis() },
    private val minIntervalMs: Long = 5000L
) {
    @Volatile
    private var lastRunMs: Long = -minIntervalMs

    @Volatile
    private var pendingJob: Job? = null

    private val lock = Any()

    fun schedule(scope: CoroutineScope, action: suspend () -> Unit) {
        synchronized(lock) {
            val now = clock()
            val elapsed = now - lastRunMs

            // Immediate execution if interval has passed or if clock moved backwards
            if (elapsed >= minIntervalMs || elapsed < 0) {
                lastRunMs = now
                pendingJob?.cancel()
                pendingJob = null
                scope.launch {
                    try {
                        action()
                    } catch (e: CancellationException) {
                        throw e
                    } catch (_: Throwable) {
                        // Silent no-op on failure: widget errors must never affect app state
                    }
                }
            } else if (pendingJob == null || pendingJob?.isCompleted == true) {
                // Schedule trailing update for the remainder of the interval
                val delayMs = (minIntervalMs - elapsed).coerceAtLeast(0L)
                pendingJob = scope.launch {
                    delay(delayMs)
                    synchronized(lock) {
                        lastRunMs = clock()
                        pendingJob = null
                    }
                    try {
                        action()
                    } catch (e: CancellationException) {
                        throw e
                    } catch (_: Throwable) {
                        // Silent no-op on failure
                    }
                }
            }
        }
    }
}
