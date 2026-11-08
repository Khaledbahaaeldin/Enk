package app.enk.writer

import android.content.Context
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.room.Room
import app.enk.data.EnkDatabase
import app.enk.data.NoteRepository
import app.enk.data.SystemClock
import app.enk.settings.CorrectionMode
import app.enk.settings.SettingsStore
import app.enk.text.StruckPage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.After
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
import java.io.File

@RunWith(RobolectricTestRunner::class)
class WriterViewModelTest {

    @get:Rule
    val tempFolder = TemporaryFolder()

    private lateinit var db: EnkDatabase
    private lateinit var repo: NoteRepository
    private lateinit var settingsStore: SettingsStore
    private val testScope = CoroutineScope(Dispatchers.IO + Job())

    @Before
    fun setUp() {
        val context: Context = RuntimeEnvironment.getApplication()
        db = Room.inMemoryDatabaseBuilder(context, EnkDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        repo = NoteRepository(db, SystemClock)

        val settingsFile = File(tempFolder.root, "test_settings.preferences_pb")
        val dataStore = PreferenceDataStoreFactory.create(produceFile = { settingsFile })
        settingsStore = SettingsStore(dataStore)
    }

    @After
    fun tearDown() {
        testScope.cancel()
        db.close()
    }

    @Test
    fun loadsNoteFromRepoOnStart() = runBlocking {
        // Case 1: Existing note with pages
        val note = repo.createNote(dividerId = null)
        val initialPages = listOf(StruckPage("Page 1 text"), StruckPage("Page 2 text"))
        repo.savePages(note.id, initialPages)

        val vmExisting = WriterViewModel(
            noteId = note.id,
            repo = repo,
            settings = settingsStore,
            ioScope = testScope
        )

        val stateExisting = vmExisting.state.first { it.loaded }
        assertEquals(note.id, stateExisting.noteId)
        assertEquals(true, stateExisting.loaded)
        assertEquals(false, stateExisting.missing)
        assertEquals(2, stateExisting.pages.size)
        assertEquals("Page 1 text", stateExisting.pages[0].text)
        assertEquals("Page 2 text", stateExisting.pages[1].text)

        // Case 2: Non-existent note initializes with missing == true
        val vmMissing = WriterViewModel(
            noteId = "non-existent-note-id",
            repo = repo,
            settings = settingsStore,
            ioScope = testScope
        )
        val stateMissing = vmMissing.state.first { it.loaded }
        assertEquals("non-existent-note-id", stateMissing.noteId)
        assertEquals(true, stateMissing.loaded)
        assertEquals(true, stateMissing.missing)
    }

    @Test
    fun nonExistentNoteIgnoresEventsAndSavesNothing() = runBlocking {
        val vmMissing = WriterViewModel(
            noteId = "non-existent-id",
            repo = repo,
            settings = settingsStore,
            ioScope = testScope
        )
        val stateMissing = vmMissing.state.first { it.loaded }
        assertTrue(stateMissing.missing)

        vmMissing.dispatch(WriterEvent.Insert("hello"))
        vmMissing.dispatch(WriterEvent.Backspace)
        vmMissing.flush()

        assertTrue(vmMissing.state.value.missing)
        assertNull(repo.load("non-existent-id"))
    }

    @Test
    fun trashedNoteIsReportedAsMissingAndSavesNothing() = runBlocking {
        val note = repo.createNote(dividerId = null)
        repo.trash(note.id)

        val vm = WriterViewModel(
            noteId = note.id,
            repo = repo,
            settings = settingsStore,
            ioScope = testScope
        )
        val state = vm.state.first { it.loaded }
        assertTrue("state.value.missing must be true", state.missing)

        vm.dispatch(WriterEvent.Insert("Should be ignored"))
        vm.flush()

        assertTrue(vm.state.value.missing)
        val loaded = repo.load(note.id)
        assertNotNull(loaded)
        assertEquals(1, loaded?.pages?.size)
        assertEquals("", loaded?.pages?.get(0)?.text)
    }

    @Test
    fun openingNoteWithoutTypingLeavesModifiedAtUnchanged() = runBlocking {
        val note = repo.createNote(dividerId = null)
        val originalModifiedAt = note.modifiedAt

        kotlinx.coroutines.delay(50)

        val vm = WriterViewModel(
            noteId = note.id,
            repo = repo,
            settings = settingsStore,
            ioScope = testScope
        )
        vm.state.first { it.loaded }
        vm.flush()

        val reloaded = repo.load(note.id)
        assertNotNull(reloaded)
        assertEquals("modifiedAt must remain unchanged when opening without typing", originalModifiedAt, reloaded!!.modifiedAt)
    }

    @Test
    fun slowLoadHoldsEventsAndAppliesAfterLoading() = runTest {
        val testDispatcher = StandardTestDispatcher(testScheduler)
        val controlledScope = CoroutineScope(testDispatcher + Job())

        val note = repo.createNote(dividerId = null)

        val vm = WriterViewModel(
            noteId = note.id,
            repo = repo,
            settings = settingsStore,
            ioScope = controlledScope
        )

        // Before coroutines run on testScheduler:
        assertEquals(false, vm.state.value.loaded)

        // Dispatch events while still loading
        vm.dispatch(WriterEvent.Insert("typed while loading"))
        assertEquals(false, vm.state.value.loaded)

        // Advance dispatcher to execute load Note and apply held events
        testScheduler.advanceUntilIdle()

        // Wait for Room's background query to resume onto testDispatcher
        var state = vm.state.value
        val deadline = System.currentTimeMillis() + 3000
        while (!state.loaded && System.currentTimeMillis() < deadline) {
            testScheduler.advanceUntilIdle()
            kotlinx.coroutines.delay(20)
            state = vm.state.value
        }

        assertEquals(true, state.loaded)
        assertTrue(state.pages[0].text.contains("typed while loading"))
    }

    @Test
    fun holdsOrAppliesEventsCorrectly() = runBlocking {
        val note = repo.createNote(dividerId = null)
        val vm = WriterViewModel(
            noteId = note.id,
            repo = repo,
            settings = settingsStore,
            ioScope = testScope
        )
        vm.state.first { it.loaded }

        vm.dispatch(WriterEvent.Insert("Hello"))
        assertEquals("Hello", vm.state.value.pages[0].text)
        assertEquals(5, vm.state.value.caret)

        vm.dispatch(WriterEvent.Insert(" World"))
        assertEquals("Hello World", vm.state.value.pages[0].text)
        assertEquals(11, vm.state.value.caret)
    }

    @Test
    fun dynamicCorrectionMode() = runBlocking {
        val note = repo.createNote(dividerId = null)
        val vm = WriterViewModel(
            noteId = note.id,
            repo = repo,
            settings = settingsStore,
            ioScope = testScope
        )
        vm.state.first { it.loaded }

        // Default: CleanErase
        vm.dispatch(WriterEvent.Insert("ab"))
        vm.dispatch(WriterEvent.Backspace)
        assertEquals("a", vm.state.value.pages[0].text)

        // Change settings to StrikeThrough
        settingsStore.update { it.copy(correction = CorrectionMode.StrikeThrough) }
        kotlinx.coroutines.delay(200)

        vm.dispatch(WriterEvent.Backspace)
        // In StrikeThrough: text stays "a", index 0 is struck
        assertEquals("a", vm.state.value.pages[0].text)
        assertEquals(listOf(0..0), vm.state.value.pages[0].struck)
    }

    @Test
    fun unsavedTextSurvivesViewModelClear() = runBlocking {
        val note = repo.createNote(dividerId = null)
        val vm = WriterViewModel(
            noteId = note.id,
            repo = repo,
            settings = settingsStore,
            ioScope = testScope
        )
        vm.state.first { it.loaded }

        val store = androidx.lifecycle.ViewModelStore()
        store.put("writer_vm", vm)

        vm.dispatch(WriterEvent.Insert("Surviving text"))
        store.clear()

        // The flush runs in the background: wait for it (up to 5 s) instead of a fixed sleep.
        val deadline = System.currentTimeMillis() + 5000
        while (repo.load(note.id)?.pages?.firstOrNull()?.text != "Surviving text" &&
            System.currentTimeMillis() < deadline
        ) {
            kotlinx.coroutines.delay(20)
        }

        val loaded = repo.load(note.id)
        assertNotNull(loaded)
        assertEquals(1, loaded?.pages?.size)
        assertEquals("Surviving text", loaded?.pages?.get(0)?.text)
    }

    @Test
    fun storedStrikeThroughModeAppliesToEventsHeldDuringLoading() = runBlocking {
        settingsStore.update { it.copy(correction = CorrectionMode.StrikeThrough) }
        val note = repo.createNote(dividerId = null)
        val vm = WriterViewModel(
            noteId = note.id,
            repo = repo,
            settings = settingsStore,
            ioScope = testScope
        )
        // Dispatched before the note has loaded: held, then applied with the stored mode.
        vm.dispatch(WriterEvent.Insert("ab"))
        vm.dispatch(WriterEvent.Backspace)
        val state = vm.state.first { it.loaded }
        assertEquals("ab", state.pages[0].text)
        assertEquals(listOf(1..1), state.pages[0].struck)
    }

    @Test
    fun caretMovementAndBoundsClampingThroughViewModel() = runBlocking {
        val note = repo.createNote(dividerId = null)
        val vm = WriterViewModel(
            noteId = note.id,
            repo = repo,
            settings = settingsStore,
            ioScope = testScope
        )
        vm.state.first { it.loaded }

        vm.dispatch(WriterEvent.Insert("Enk"))
        assertEquals(3, vm.state.value.caret)

        // Move to negative -> clamped to 0
        vm.dispatch(WriterEvent.MoveCaret(-5))
        assertEquals(0, vm.state.value.caret)

        // Move beyond end -> clamped to text length 3
        vm.dispatch(WriterEvent.MoveCaret(100))
        assertEquals(3, vm.state.value.caret)
    }

    @Test
    fun pageNavigationAndLampToggleThroughViewModel() = runBlocking {
        val note = repo.createNote(dividerId = null)
        val vm = WriterViewModel(
            noteId = note.id,
            repo = repo,
            settings = settingsStore,
            ioScope = testScope
        )
        vm.state.first { it.loaded }

        // Lamp toggling
        assertEquals(false, vm.state.value.lampOn)
        vm.dispatch(WriterEvent.ToggleLamp)
        assertEquals(true, vm.state.value.lampOn)
        vm.dispatch(WriterEvent.ToggleLamp)
        assertEquals(false, vm.state.value.lampOn)

        // Page clamping on 1-page document
        vm.dispatch(WriterEvent.GoToPage(5))
        assertEquals(0, vm.state.value.pageIndex)
        vm.dispatch(WriterEvent.GoToPage(-2))
        assertEquals(0, vm.state.value.pageIndex)
    }
}
