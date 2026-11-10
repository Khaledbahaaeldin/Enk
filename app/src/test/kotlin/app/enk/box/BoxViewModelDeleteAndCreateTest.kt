package app.enk.box

import android.content.Context
import app.enk.data.DividerDao
import app.enk.data.DividerEntity
import app.enk.data.SearchRepository
import app.enk.testContainer
import app.enk.text.StruckPage
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

/** Delete all forever, creating a note, and what the user is told when something fails. */
@RunWith(RobolectricTestRunner::class)
class BoxViewModelDeleteAndCreateTest {

    @get:Rule
    val tempFolder = TemporaryFolder()

    private lateinit var context: Context

    @Before
    fun setUp() {
        context = RuntimeEnvironment.getApplication()
    }

    @Test
    fun deleteAllForeverErasesTrashedNotesTheirPagesAndTheirSearchRows() = runBlocking {
        val container = testContainer(context, tempFolder.root)
        val trashed = container.repo.createNote(dividerId = null)
        container.repo.savePages(trashed.id, listOf(StruckPage("a unique heron note")))
        val kept = container.repo.createNote(dividerId = null)
        container.repo.savePages(kept.id, listOf(StruckPage("a unique heron survivor")))

        val search = SearchRepository(container.db)
        assertEquals("both notes are searchable before", 2, search.search("heron").size)
        container.repo.trash(trashed.id)

        val vm = BoxViewModel(container.repo, container.db.dividerDao())
        vm.state.first { it.trashCount == 1 }
        vm.deleteAllForever()
        vm.state.first { it.trashCount == 0 }

        assertNull("the note is gone", container.repo.load(trashed.id))
        val hits = search.search("heron")
        assertEquals("only the live note is searchable", listOf(kept.id), hits.map { it.noteId })
        assertNotNull("a note that was not trashed is untouched", container.repo.load(kept.id))
    }

    @Test
    fun deleteAllForeverWithAnEmptyTrashIsHarmless() = runBlocking {
        val container = testContainer(context, tempFolder.root)
        val note = container.repo.createNote(dividerId = null)
        val vm = BoxViewModel(container.repo, container.db.dividerDao())

        vm.deleteAllForever()
        vm.deleteAllForever()
        vm.state.first { it.notes.any { n -> n.id == note.id } }

        assertNotNull(container.repo.load(note.id))
        assertNull(vm.state.value.userMessage)
    }

    @Test
    fun newNoteIsCreatedInTheSelectedDividerAndItsIdIsReported() = runBlocking {
        val container = testContainer(context, tempFolder.root)
        val dividerId = container.db.dividerDao().insert(DividerEntity(name = "Journal", position = 0))
        val vm = BoxViewModel(container.repo, container.db.dividerDao())
        vm.select(dividerId)

        val latch = CountDownLatch(1)
        var reported: String? = null
        vm.newNote { id ->
            reported = id
            latch.countDown()
        }
        assertTrue("the id must be reported", latch.await(60, TimeUnit.SECONDS))

        val created = container.repo.load(reported!!)
        assertNotNull(created)
        assertEquals(dividerId, created?.dividerId)
    }

    @Test
    fun aFailedMutationShowsAFixedSentenceNotExceptionText() = runBlocking {
        val container = testContainer(context, tempFolder.root)
        val real = container.db.dividerDao()
        val id = real.insert(DividerEntity(name = "Journal", position = 0))
        val failing = object : DividerDao by real {
            override suspend fun delete(id: Long) = throw IllegalStateException("disk I/O error at 0x7f")
        }
        val vm = BoxViewModel(container.repo, failing)
        vm.state.first { it.dividers.size == 1 }

        vm.deleteDivider(id)
        val message = vm.state.first { it.userMessage != null }.userMessage!!

        assertEquals("Couldn't delete that divider.", message)
        vm.clearUserMessage()
        assertNull(vm.state.first { it.userMessage == null }.userMessage)
    }
}
