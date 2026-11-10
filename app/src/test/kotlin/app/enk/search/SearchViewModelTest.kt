package app.enk.search

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import app.enk.data.SearchHit
import app.enk.data.SearchRepository
import app.enk.testContainer
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
class SearchViewModelTest {

    @get:Rule
    val tempFolder = TemporaryFolder()

    private class FakeSearchRepository(db: app.enk.data.EnkDatabase) : SearchRepository(db) {
        var callCount = 0
        var lastQuery: String? = null
        var returnHits: List<SearchHit> = emptyList()

        override suspend fun search(query: String, limit: Int): List<SearchHit> {
            callCount++
            lastQuery = query
            return returnHits
        }
    }

    @Test
    fun debounceCollapsesRapidTyping() = runTest {
        val testDispatcher = StandardTestDispatcher(testScheduler)
        val testScope = TestScope(testDispatcher)

        val context = ApplicationProvider.getApplicationContext<Context>()
        val container = testContainer(context, tempFolder.root)

        val fakeRepo = FakeSearchRepository(container.db).apply {
            returnHits = listOf(SearchHit("note1", 0, "rains on the roof"))
        }

        val vm = SearchViewModel(
            repo = fakeRepo,
            ioDispatcher = testDispatcher,
            externalScope = testScope
        )

        // 5 keystrokes in 100 ms: 0ms, 20ms, 40ms, 60ms, 80ms
        vm.query.value = "r"
        testScheduler.advanceTimeBy(20)
        vm.query.value = "ra"
        testScheduler.advanceTimeBy(20)
        vm.query.value = "rai"
        testScheduler.advanceTimeBy(20)
        vm.query.value = "rain"
        testScheduler.advanceTimeBy(20)
        vm.query.value = "rains"

        // Before 200 ms debounce finishes
        testScheduler.advanceTimeBy(100)
        assertEquals(0, fakeRepo.callCount)

        // After 200 ms debounce finishes
        testScheduler.advanceTimeBy(150)
        assertEquals(1, fakeRepo.callCount)
        assertEquals("rains", fakeRepo.lastQuery)

        assertEquals(1, vm.results.value.size)
        assertEquals("note1", vm.results.value[0].noteId)
    }

    @Test
    fun blankQueryClearsResults() = runTest {
        val testDispatcher = StandardTestDispatcher(testScheduler)
        val testScope = TestScope(testDispatcher)

        val context = ApplicationProvider.getApplicationContext<Context>()
        val container = testContainer(context, tempFolder.root)

        val fakeRepo = FakeSearchRepository(container.db).apply {
            returnHits = listOf(SearchHit("note1", 0, "rain drops"))
        }

        val vm = SearchViewModel(
            repo = fakeRepo,
            ioDispatcher = testDispatcher,
            externalScope = testScope
        )

        vm.query.value = "rain"
        testScheduler.advanceTimeBy(250)
        assertEquals(1, vm.results.value.size)

        vm.query.value = ""
        testScheduler.advanceTimeBy(250)
        assertEquals(0, vm.results.value.size)

        vm.query.value = "   "
        testScheduler.advanceTimeBy(250)
        assertEquals(0, vm.results.value.size)
    }
}
