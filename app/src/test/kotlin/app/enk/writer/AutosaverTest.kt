package app.enk.writer

import app.enk.text.StruckPage
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class AutosaverTest {

    @Test
    fun autosaveDebouncesTo500ms() = runTest {
        val savedList = mutableListOf<List<StruckPage>>()
        val autosaver = Autosaver(
            scope = this,
            debounceMs = 500,
            save = { savedList.add(it) }
        )

        repeat(20) { i ->
            advanceTimeBy(20)
            autosaver.onChange(listOf(StruckPage("edit $i")))
        }

        // 400ms passed, debounced timer resets on each call -> 0 saves so far
        assertEquals(0, savedList.size)

        advanceTimeBy(499)
        assertEquals(0, savedList.size)

        advanceTimeBy(2) // Total > 500ms since last change
        assertEquals(1, savedList.size)
        assertEquals("edit 19", savedList[0][0].text)
    }

    @Test
    fun flushSavesImmediately() = runTest {
        val savedList = mutableListOf<List<StruckPage>>()
        val autosaver = Autosaver(
            scope = this,
            debounceMs = 500,
            save = { savedList.add(it) }
        )

        autosaver.onChange(listOf(StruckPage("immediate")))
        assertEquals(0, savedList.size)

        autosaver.flush()
        assertEquals(1, savedList.size)
        assertEquals("immediate", savedList[0][0].text)
    }

    @Test
    fun serialSaves() = runTest {
        val savedList = mutableListOf<List<StruckPage>>()
        var inFlight = 0
        var maxInFlight = 0

        val autosaver = Autosaver(
            scope = this,
            debounceMs = 100,
            save = { pages ->
                inFlight++
                maxInFlight = maxOf(maxInFlight, inFlight)
                delay(200)
                savedList.add(pages)
                inFlight--
            }
        )

        autosaver.onChange(listOf(StruckPage("save 1")))
        advanceTimeBy(101)

        // While save 1 is in-flight, push another change
        autosaver.onChange(listOf(StruckPage("save 2")))
        advanceUntilIdle()

        assertEquals("Never more than 1 save in-flight at once", 1, maxInFlight)
        assertEquals(2, savedList.size)
        assertEquals("save 1", savedList[0][0].text)
        assertEquals("save 2", savedList[1][0].text)
    }

    @Test
    fun errorRetentionAndRetry() = runTest {
        var shouldFail = true
        val savedList = mutableListOf<List<StruckPage>>()

        val autosaver = Autosaver(
            scope = this,
            debounceMs = 100,
            save = { pages ->
                if (shouldFail) {
                    shouldFail = false
                    throw RuntimeException("Disk error")
                }
                savedList.add(pages)
            }
        )

        autosaver.onChange(listOf(StruckPage("retry-me")))
        // Background debounce triggers after 100ms and fails
        advanceTimeBy(101)
        assertEquals(0, savedList.size)

        var thrown: Throwable? = null
        try {
            autosaver.flush()
        } catch (e: Throwable) {
            thrown = e
        }
        org.junit.Assert.assertNotNull("Background failure must be retained and rethrown on flush", thrown)
        assertEquals("Disk error", thrown?.message)

        // Next flush retries the pending pages successfully
        autosaver.flush()
        assertEquals(1, savedList.size)
        assertEquals("retry-me", savedList[0][0].text)
    }

    @Test
    fun flushWhenNoPendingChangesIsNoOp() = runTest {
        val savedList = mutableListOf<List<StruckPage>>()
        val autosaver = Autosaver(
            scope = this,
            debounceMs = 500,
            save = { savedList.add(it) }
        )

        // Calling flush without any onChange
        autosaver.flush()
        assertEquals(0, savedList.size)
    }

    @Test
    fun consecutiveFlushesAfterChangeOnlySaveOnce() = runTest {
        val savedList = mutableListOf<List<StruckPage>>()
        val autosaver = Autosaver(
            scope = this,
            debounceMs = 500,
            save = { savedList.add(it) }
        )

        autosaver.onChange(listOf(StruckPage("test")))
        autosaver.flush()
        assertEquals(1, savedList.size)

        // Immediate second flush without new changes
        autosaver.flush()
        assertEquals(1, savedList.size)
    }
}
