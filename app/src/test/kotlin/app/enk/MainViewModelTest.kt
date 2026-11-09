package app.enk

import android.content.Context
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.room.Room
import app.enk.audio.SoundPlayer
import app.enk.audio.SoundSet
import app.enk.data.EnkDatabase
import app.enk.data.NoteRepository
import app.enk.data.SystemClock
import app.enk.settings.SettingsStore
import app.enk.text.StruckPage
import app.enk.writer.Haptics
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.ViewModelProvider
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
class MainViewModelTest {

    @get:Rule
    val tempFolder = TemporaryFolder()

    private lateinit var db: EnkDatabase
    private lateinit var repo: NoteRepository
    private lateinit var settingsStore: SettingsStore
    private lateinit var container: AppContainer

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

        container = object : AppContainer {
            override val db: EnkDatabase = this@MainViewModelTest.db
            override val repo: NoteRepository = this@MainViewModelTest.repo
            override val settingsStore: SettingsStore = this@MainViewModelTest.settingsStore
            override val soundManifest: List<SoundSet> = emptyList()
            override val soundPlayer: SoundPlayer = object : SoundPlayer {
                override fun prepare(soundSet: SoundSet) {}
                override fun play(event: app.enk.audio.SoundEvent, soundSet: SoundSet, volume: Float) {}
                override fun release() {}
            }
            override val haptics: Haptics = object : Haptics {
                override fun pulse(ms: Int) {}
            }
        }
    }

    @After
    fun tearDown() {
        db.close()
    }

    @Test
    fun isLoadedBecomesTrueAndCreatesNoteWhenEmpty() = runBlocking {
        val context: Context = RuntimeEnvironment.getApplication()
        val viewModel = MainViewModel(context, container, Dispatchers.Unconfined)

        // Wait for isLoaded to emit true
        val loaded = viewModel.isLoaded.filter { it }.first()
        assertTrue(loaded)
        assertNotNull(viewModel.currentNoteId.value)
        assertNull("No note should be restored when repo was empty", viewModel.restoredNote.value)
    }

    @Test
    fun isLoadedRestoresMostRecentNote() = runBlocking {
        val note = repo.createNote(dividerId = null)
        repo.savePages(note.id, listOf(StruckPage("Hello restored note")))

        val context: Context = RuntimeEnvironment.getApplication()
        val viewModel = MainViewModel(context, container, Dispatchers.Unconfined)

        val loaded = viewModel.isLoaded.filter { it }.first()
        assertTrue(loaded)
        assertEquals(note.id, viewModel.currentNoteId.value)
        val restored = viewModel.restoredNote.value
        assertNotNull("Most recent note should be restored", restored)
        assertEquals(note.id, restored?.id)
        assertEquals("Hello restored note", restored?.pages?.firstOrNull()?.text)

        // Verify caret resolution for start targets
        val firstPageText = restored?.pages?.firstOrNull()?.text ?: ""
        val geometry = app.enk.writer.WriterGeometry.compute(400f, 820f, 1.0f)
        val lines = app.enk.text.GridLayout.wrap(firstPageText, geometry.cols)
        val caretPos = app.enk.text.GridLayout.caret(lines, firstPageText.length, geometry.cols)
        assertEquals("Caret col must reflect text length on single line", firstPageText.length, caretPos.col)
        assertTrue("Carriage X must be shifted right from column 0", geometry.carriageCenterXDp(caretPos.col) > geometry.carriageCenterXDp(0))
    }

    @Test
    fun initialSettingsAreReadBeforeLoadedIsReported() = runBlocking {
        settingsStore.update { it.copy(reduceEffects = true, bellVolume = 0.25f) }
        val context: Context = RuntimeEnvironment.getApplication()
        val viewModel = MainViewModel(context, container, Dispatchers.Unconfined)

        viewModel.isLoaded.filter { it }.first()
        assertTrue("the first composition must already see the stored settings", viewModel.initialSettings.reduceEffects)
        assertEquals(0.25f, viewModel.initialSettings.bellVolume, 0.0001f)
    }

    @Test
    fun clearingTheStoreReleasesTheDatabaseAndThePlayer() {
        val context: Context = RuntimeEnvironment.getApplication()
        var playerReleased = false
        val tracked = object : AppContainer by container {
            override val soundPlayer: SoundPlayer = object : SoundPlayer {
                override fun prepare(soundSet: SoundSet) {}
                override fun play(event: app.enk.audio.SoundEvent, soundSet: SoundSet, volume: Float) {}
                override fun release() { playerReleased = true }
            }
        }
        val store = ViewModelStore()
        val viewModel = ViewModelProvider(store, MainViewModel.factory(context, tracked, Dispatchers.Unconfined))[MainViewModel::class.java]
        runBlocking { viewModel.isLoaded.filter { it }.first() }
        assertTrue("the loading opened the database", db.isOpen)

        store.clear()

        assertFalse("onCleared must close the database", db.isOpen)
        assertTrue("onCleared must release the sound player", playerReleased)
    }
}
