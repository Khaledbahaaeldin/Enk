package app.enk.widget

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class UpdateGateTest {

    @Test
    fun burstsAreCoalescedAndTrailingRunsAtExactBoundary() = runTest {
        var currentTime = 10000L
        val gate = UpdateGate(clock = { currentTime }, minIntervalMs = 5000L)
        var executionCount = 0

        val scope = TestScope(StandardTestDispatcher(testScheduler))

        // First call executes immediately
        gate.schedule(scope) { executionCount++ }
        scope.testScheduler.advanceUntilIdle()
        assertEquals(1, executionCount)

        // Burst of 10 requests during the 5000ms window
        for (i in 1..10) {
            currentTime += 200 // advances by 2000ms total
            gate.schedule(scope) { executionCount++ }
        }
        scope.testScheduler.runCurrent()
        // No extra execution yet (still inside 5000ms window)
        assertEquals(1, executionCount)

        // Advance to 4999ms since first run
        scope.testScheduler.advanceTimeBy(2999)
        currentTime = 14999L
        scope.testScheduler.runCurrent()
        assertEquals(1, executionCount)

        // Advance 1ms more to reach exact 5000ms boundary
        scope.testScheduler.advanceTimeBy(1)
        currentTime = 15000L
        scope.testScheduler.advanceUntilIdle()
        // Trailing execution has fired!
        assertEquals(2, executionCount)
    }

    @Test
    fun callAfterIntervalRunsImmediately() = runTest {
        var currentTime = 10000L
        val gate = UpdateGate(clock = { currentTime }, minIntervalMs = 5000L)
        var executionCount = 0

        val scope = TestScope(StandardTestDispatcher(testScheduler))

        gate.schedule(scope) { executionCount++ }
        scope.testScheduler.advanceUntilIdle()
        assertEquals(1, executionCount)

        // Advance beyond 5000ms
        currentTime = 15001L
        scope.testScheduler.advanceTimeBy(5001)

        gate.schedule(scope) { executionCount++ }
        scope.testScheduler.advanceUntilIdle()
        assertEquals(2, executionCount)
    }

    @Test
    fun clockGoingBackwardsIsHandledSafely() = runTest {
        var currentTime = 10000L
        val gate = UpdateGate(clock = { currentTime }, minIntervalMs = 5000L)
        var executionCount = 0

        val scope = TestScope(StandardTestDispatcher(testScheduler))

        gate.schedule(scope) { executionCount++ }
        scope.testScheduler.advanceUntilIdle()
        assertEquals(1, executionCount)

        // Clock moves backwards (e.g. system time sync)
        currentTime = 5000L
        gate.schedule(scope) { executionCount++ }
        scope.testScheduler.advanceUntilIdle()
        // Backward clock is treated as elapsed < 0 and triggers immediate run
        assertEquals(2, executionCount)
    }

    @Test
    fun failureInActionIsSwallowedWithoutCrashing() = runTest {
        var currentTime = 10000L
        val gate = UpdateGate(clock = { currentTime }, minIntervalMs = 5000L)

        val scope = TestScope(StandardTestDispatcher(testScheduler))

        // Action throws exception
        gate.schedule(scope) {
            throw RuntimeException("Widget update failed")
        }
        scope.testScheduler.advanceUntilIdle()
        // Doesn't throw or crash
    }
}
