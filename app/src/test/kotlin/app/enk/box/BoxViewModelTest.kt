package app.enk.box

import android.content.Context
import app.enk.data.DividerDao
import app.enk.data.DividerEntity
import app.enk.data.NoteRepository
import app.enk.testContainer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
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

@RunWith(RobolectricTestRunner::class)
class BoxViewModelTest {

    @get:Rule
    val tempFolder = TemporaryFolder()

    private lateinit var context: Context
    private val testScope = CoroutineScope(Dispatchers.IO + Job())

    @Before
    fun setUp() {
        context = RuntimeEnvironment.getApplication()
    }

    @After
    fun tearDown() {
        testScope.cancel()
    }

    @Test
    fun selectUnfiledShowsNullDividerNotes() = runBlocking {
        val container = testContainer(context, tempFolder.root)
        val dividerId = container.db.dividerDao().insert(
            DividerEntity(name = "Personal", position = 0)
        )
        val noteUnfiled = container.repo.createNote(dividerId = null)
        val noteInDivider = container.repo.createNote(dividerId = dividerId)

        val vm = BoxViewModel(container.repo, container.db.dividerDao())

        // Initial/default or explicit selection of unfiled (null)
        vm.select(null)
        val stateUnfiled = vm.state.first { state ->
            state.selectedDividerId == null && state.notes.any { it.id == noteUnfiled.id }
        }
        assertEquals(null, stateUnfiled.selectedDividerId)
        assertTrue(stateUnfiled.notes.any { it.id == noteUnfiled.id })
        assertFalse(stateUnfiled.notes.any { it.id == noteInDivider.id })

        // Switch selection to divider
        vm.select(dividerId)
        val stateDivider = vm.state.first { state ->
            state.selectedDividerId == dividerId && state.notes.any { it.id == noteInDivider.id }
        }
        assertEquals(dividerId, stateDivider.selectedDividerId)
        assertTrue(stateDivider.notes.any { it.id == noteInDivider.id })
        assertFalse(stateDivider.notes.any { it.id == noteUnfiled.id })
    }

    @Test
    fun deleteDividerMovesNotesToUnfiled() = runBlocking {
        val container = testContainer(context, tempFolder.root)
        val dividerId = container.db.dividerDao().insert(
            DividerEntity(name = "Work", position = 0)
        )
        val note = container.repo.createNote(dividerId = dividerId)

        val vm = BoxViewModel(container.repo, container.db.dividerDao())
        vm.select(dividerId)
        vm.state.first { it.selectedDividerId == dividerId && it.notes.any { n -> n.id == note.id } }

        // Deleting the divider moves its notes to Unfiled
        vm.deleteDivider(dividerId)
        vm.state.first { state -> state.dividers.none { it.id == dividerId } }

        // Verify in repository that note's dividerId is now null
        val loadedNote = container.repo.load(note.id)
        assertNotNull(loadedNote)
        assertNull(loadedNote?.dividerId)

        // Verify in unfiled view
        vm.select(null)
        val unfiledState = vm.state.first { state ->
            state.selectedDividerId == null && state.notes.any { it.id == note.id }
        }
        assertTrue(unfiledState.notes.any { it.id == note.id })
        assertFalse(unfiledState.dividers.any { it.id == dividerId })
    }

    @Test
    fun trashThenRestore() = runBlocking {
        val container = testContainer(context, tempFolder.root)
        val note = container.repo.createNote(dividerId = null)

        val vm = BoxViewModel(container.repo, container.db.dividerDao())
        vm.select(null)
        vm.state.first { it.notes.any { n -> n.id == note.id } }

        // Trash the note
        vm.trash(note.id)

        val trashedState = vm.state.first { state ->
            state.notes.none { it.id == note.id } && state.trashCount == 1
        }
        assertFalse(trashedState.notes.any { it.id == note.id })
        assertEquals(1, trashedState.trashCount)

        // Restore the note
        vm.restore(note.id)

        val restoredState = vm.state.first { state ->
            state.notes.any { it.id == note.id } && state.trashCount == 0
        }
        assertTrue(restoredState.notes.any { it.id == note.id })
        assertEquals(0, restoredState.trashCount)
        assertEquals(note.id, restoredState.notes.first().id)
    }

    @Test
    fun blankDividerNameRejected() = runBlocking {
        val container = testContainer(context, tempFolder.root)
        val vm = BoxViewModel(container.repo, container.db.dividerDao())

        assertFalse(vm.dividerNameValid(""))
        assertFalse(vm.dividerNameValid("   "))
        assertFalse(vm.dividerNameValid("\t\n"))

        val initialDividers = container.db.dividerDao().all()
        vm.addDivider("   ")

        val state = vm.state.value
        assertEquals(initialDividers.size, state.dividers.size)
        assertEquals(initialDividers.size, container.db.dividerDao().all().size)
    }

    @Test
    fun duplicateDividerNameRejected() = runBlocking {
        val container = testContainer(context, tempFolder.root)
        val vm = BoxViewModel(container.repo, container.db.dividerDao())

        vm.addDivider("Projects")
        val withProjects = vm.state.first { state -> state.dividers.any { it.name == "Projects" } }
        assertEquals(1, withProjects.dividers.size)

        // Case-insensitive duplicates must be rejected
        assertFalse(vm.dividerNameValid("projects"))
        assertFalse(vm.dividerNameValid("PROJECTS"))
        assertFalse(vm.dividerNameValid("  Projects  "))
        assertFalse(vm.dividerNameValid("pRoJeCtS"))

        vm.addDivider("PROJECTS")
        val stateAfterDuplicate = vm.state.value
        assertEquals(1, stateAfterDuplicate.dividers.size)
        assertEquals(1, container.db.dividerDao().all().size)
    }

    @Test
    fun dividerNameOver40CharsRejected() = runBlocking {
        val container = testContainer(context, tempFolder.root)
        val vm = BoxViewModel(container.repo, container.db.dividerDao())

        val valid40 = "a".repeat(40)
        val invalid41 = "a".repeat(41)

        assertTrue(vm.dividerNameValid(valid40))
        assertFalse(vm.dividerNameValid(invalid41))

        vm.addDivider(invalid41)
        val state = vm.state.value
        assertFalse(state.dividers.any { it.name == invalid41 })
    }

    @Test
    fun reorderNotesPersists() = runBlocking {
        val container = testContainer(context, tempFolder.root)
        val n1 = container.repo.createNote(dividerId = null)
        val n2 = container.repo.createNote(dividerId = null)
        val n3 = container.repo.createNote(dividerId = null)

        val vm = BoxViewModel(container.repo, container.db.dividerDao())
        vm.select(null)
        vm.state.first { it.notes.size >= 3 }

        val newOrder = listOf(n3.id, n1.id, n2.id)
        vm.reorderNotes(newOrder)

        val reorderedState = vm.state.first { state ->
            state.notes.size == 3 && state.notes.map { it.id } == newOrder
        }
        assertEquals(newOrder, reorderedState.notes.map { it.id })

        // Recheck from repository directly
        val repoNotes = container.db.noteDao().allActive()
        assertEquals(newOrder, repoNotes.map { it.id })
    }

    @Test
    fun trashCountUpdates() = runBlocking {
        val container = testContainer(context, tempFolder.root)
        val n1 = container.repo.createNote(dividerId = null)
        val n2 = container.repo.createNote(dividerId = null)

        val vm = BoxViewModel(container.repo, container.db.dividerDao())
        vm.select(null)
        val initialState = vm.state.first { it.notes.size == 2 }
        assertEquals(0, initialState.trashCount)

        vm.trash(n1.id)
        val state1 = vm.state.first { it.trashCount == 1 }
        assertEquals(1, state1.trashCount)

        vm.trash(n2.id)
        val state2 = vm.state.first { it.trashCount == 2 }
        assertEquals(2, state2.trashCount)

        vm.restore(n1.id)
        val state3 = vm.state.first { it.trashCount == 1 }
        assertEquals(1, state3.trashCount)
    }

    @Test
    fun mutationFailureKeepsStateAndSetsMessage() = runBlocking {
        val container = testContainer(context, tempFolder.root)
        container.repo.createNote(dividerId = null)

        // Create a throwing DividerDao wrapper
        val realDao = container.db.dividerDao()
        val throwingDao = object : DividerDao by realDao {
            override suspend fun insert(d: DividerEntity): Long {
                throw RuntimeException("Simulated disk error")
            }
        }

        val vm = BoxViewModel(container.repo, throwingDao)
        vm.select(null)
        val stateBefore = vm.state.first { it.notes.isNotEmpty() }
        val dividerCountBefore = stateBefore.dividers.size
        val noteCountBefore = stateBefore.notes.size

        // Attempt mutation that fails
        vm.addDivider("Failing Divider")

        val stateAfter = vm.state.first { it.userMessage != null }
        assertNotNull(stateAfter.userMessage)
        assertEquals(dividerCountBefore, stateAfter.dividers.size)
        assertEquals(noteCountBefore, stateAfter.notes.size)
    }

    @Test
    fun dividerNameTrimmed() = runBlocking {
        val container = testContainer(context, tempFolder.root)
        val vm = BoxViewModel(container.repo, container.db.dividerDao())

        vm.addDivider("   Trimmed Name   ")
        val state = vm.state.first { it.dividers.any { d -> d.name == "Trimmed Name" } }
        assertTrue(state.dividers.any { it.name == "Trimmed Name" })
        assertFalse(state.dividers.any { it.name == "   Trimmed Name   " })
    }

    @Test
    fun dividerNameUnicodeBoundaryAccepted() = runBlocking {
        val container = testContainer(context, tempFolder.root)
        val vm = BoxViewModel(container.repo, container.db.dividerDao())

        // 40 unicode code units
        val unicode40 = "É".repeat(40)
        assertTrue(vm.dividerNameValid(unicode40))

        vm.addDivider(unicode40)
        val state = vm.state.first { it.dividers.any { d -> d.name == unicode40 } }
        assertTrue(state.dividers.any { it.name == unicode40 })
    }

    @Test
    fun reorderDividersPersists() = runBlocking {
        val container = testContainer(context, tempFolder.root)
        val d1 = container.db.dividerDao().insert(DividerEntity(name = "Div 1", position = 0))
        val d2 = container.db.dividerDao().insert(DividerEntity(name = "Div 2", position = 1))

        val vm = BoxViewModel(container.repo, container.db.dividerDao())
        vm.state.first { it.dividers.size == 2 }

        val newOrder = listOf(d2, d1)
        vm.reorderDividers(newOrder)

        val stateAfter = vm.state.first { it.dividers.map { d -> d.id } == newOrder }
        assertEquals(newOrder, stateAfter.dividers.map { it.id })
    }
}
