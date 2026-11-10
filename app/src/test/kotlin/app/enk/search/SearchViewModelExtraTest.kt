package app.enk.search

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import app.enk.data.SearchHit
import app.enk.data.SearchRepository
import app.enk.testContainer
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/** Out-of-order results, failures, and what a query that only looks like text does. */
@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
class SearchViewModelExtraTest {

    @get:Rule
    val tempFolder = TemporaryFolder()

    private class GatedRepository(db: app.enk.data.EnkDatabase) : SearchRepository(db) {
        val calls = mutableListOf<String>()
        val gates = mutableMapOf<String, CompletableDeferred<List<SearchHit>>>()
        var failNext = false

        override suspend fun search(query: String, limit: Int): List<SearchHit> {
            calls += query
            if (failNext) {
                failNext = false
                throw IllegalStateException("database is closed")
            }
            return gates.getOrPut(query) { CompletableDeferred() }.await()
        }
    }

    private fun TestScope.viewModel(repo: GatedRepository) =
        SearchViewModel(repo, StandardTestDispatcher(testScheduler), externalScope = backgroundScope)

    private fun repo(): GatedRepository {
        val context = ApplicationProvider.getApplicationContext<Context>()
        return GatedRepository(testContainer(context, tempFolder.root).db)
    }

    @Test
    fun aSlowEarlierSearchNeverOverwritesALaterOne() = runTest {
        val repo = repo()
        val vm = viewModel(repo)

        vm.query.value = "ra"
        advanceTimeBy(250)
        runCurrent()
        assertEquals(listOf("ra"), repo.calls)

        vm.query.value = "rain"
        advanceTimeBy(250)
        runCurrent()
        assertEquals(listOf("ra", "rain"), repo.calls)

        // The later query finishes first, the earlier one finishes afterwards.
        repo.gates.getValue("rain").complete(listOf(SearchHit("late", 0, "rain")))
        runCurrent()
        repo.gates.getValue("ra").complete(listOf(SearchHit("stale", 0, "ra")))
        runCurrent()

        assertEquals(listOf("late"), vm.results.value.map { it.noteId })
    }

    @Test
    fun resultsAreReplacedNotAppended() = runTest {
        val repo = repo()
        val vm = viewModel(repo)

        vm.query.value = "rain"
        advanceTimeBy(250)
        repo.gates.getValue("rain").complete(listOf(SearchHit("a", 0, "x"), SearchHit("b", 0, "y")))
        runCurrent()
        assertEquals(2, vm.results.value.size)

        vm.query.value = "rainy"
        advanceTimeBy(250)
        repo.gates.getValue("rainy").complete(listOf(SearchHit("c", 0, "z")))
        runCurrent()

        assertEquals(listOf("c"), vm.results.value.map { it.noteId })
    }

    @Test
    fun aFailingSearchShowsNoResultsAndTheNextOneStillWorks() = runTest {
        val repo = repo().apply { failNext = true }
        val vm = viewModel(repo)

        vm.query.value = "rain"
        advanceTimeBy(250)
        runCurrent()
        assertTrue("a failure shows nothing instead of killing the screen", vm.results.value.isEmpty())

        vm.query.value = "rainy"
        advanceTimeBy(250)
        repo.gates.getValue("rainy").complete(listOf(SearchHit("ok", 0, "rainy")))
        runCurrent()
        assertEquals(listOf("ok"), vm.results.value.map { it.noteId })
    }

    @Test
    fun surroundingSpacesAreTrimmedAndAnAllBlankQueryNeverSearches() = runTest {
        val repo = repo()
        val vm = viewModel(repo)

        vm.query.value = "   "
        advanceTimeBy(250)
        runCurrent()
        assertTrue(repo.calls.isEmpty())

        vm.query.value = "  rain  "
        advanceTimeBy(250)
        runCurrent()
        assertEquals(listOf("rain"), repo.calls)
    }
}
