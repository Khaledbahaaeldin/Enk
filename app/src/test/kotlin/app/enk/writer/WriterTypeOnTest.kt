package app.enk.writer

import android.content.Context
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.room.Room
import app.enk.audio.SoundEvent
import app.enk.audio.SoundPlayer
import app.enk.audio.SoundSet
import app.enk.data.EnkDatabase
import app.enk.data.NoteRepository
import app.enk.data.SystemClock
import app.enk.keyboard.LayoutLoader
import app.enk.settings.KeyboardMode
import app.enk.settings.Settings
import app.enk.settings.SettingsStore
import app.enk.text.StruckPage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import java.io.File

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], qualifiers = "w400dp-h820dp")
class WriterTypeOnTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @get:Rule
    val tempFolder = TemporaryFolder()

    private lateinit var db: EnkDatabase
    private lateinit var repo: NoteRepository
    private lateinit var settingsStore: SettingsStore
    private val testScope = CoroutineScope(Dispatchers.IO + Job())

    private val dummyPlayer = object : SoundPlayer {
        override fun play(event: SoundEvent, set: SoundSet, volume: Float) {}
        override fun release() {}
    }

    private val dummyHaptics = object : Haptics {
        override fun pulse(ms: Int) {}
    }

    @Before
    fun setUp() {
        val context: Context = RuntimeEnvironment.getApplication()
        db = Room.inMemoryDatabaseBuilder(context, EnkDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        repo = NoteRepository(db, SystemClock)

        val settingsFile = File(tempFolder.root, "writer_type_on_test_settings.preferences_pb")
        val dataStore = PreferenceDataStoreFactory.create(produceFile = { settingsFile })
        settingsStore = SettingsStore(dataStore)
    }

    @After
    fun tearDown() {
        testScope.cancel()
        db.close()
    }

    @Test
    fun openingWriterWithTextStartsTypeOnAndKeyPressCancelsIt(): Unit = runBlocking {
        val note = repo.createNote(dividerId = null)
        val loadedPage = "Hello Typewriter"
        repo.savePages(note.id, listOf(StruckPage(loadedPage)))

        val viewModel = WriterViewModel(note.id, repo, settingsStore, testScope)
        viewModel.state.first { it.loaded }

        var typeOnActive = false

        composeTestRule.mainClock.autoAdvance = false
        composeTestRule.setContent {
            WriterScreen(
                viewModel = viewModel,
                settings = Settings(keyboard = KeyboardMode.System),
                player = dummyPlayer,
                haptics = dummyHaptics,
                onOpenBox = {},
                onTypeOnActiveChange = { active ->
                    typeOnActive = active
                }
            )
        }

        // Advance a few frames for loading and first type-on frame
        repeat(5) { composeTestRule.mainClock.advanceTimeByFrame() }
        composeTestRule.mainClock.advanceTimeBy(30)

        assertTrue("Opening writer with text must start type-on", typeOnActive)

        // Typing a key immediately ends the type-on
        composeTestRule.onNodeWithTag("writer_input").performTextInput("X")
        composeTestRule.mainClock.advanceTimeByFrame()

        assertFalse("Typing any key must immediately cancel type-on", typeOnActive)
    }

    @Test
    fun reduceEffectsStartsNoTypeOn(): Unit = runBlocking {
        val note = repo.createNote(dividerId = null)
        repo.savePages(note.id, listOf(StruckPage("Hello World")))

        val viewModel = WriterViewModel(note.id, repo, settingsStore, testScope)
        viewModel.state.first { it.loaded }
        var typeOnStarted = false

        composeTestRule.setContent {
            WriterScreen(
                viewModel = viewModel,
                settings = Settings(reduceEffects = true),
                player = dummyPlayer,
                haptics = dummyHaptics,
                onOpenBox = {},
                onTypeOnActiveChange = { active ->
                    if (active) typeOnStarted = true
                }
            )
        }

        composeTestRule.waitForIdle()
        assertFalse("reduceEffects = true must never start type-on", typeOnStarted)
    }

    @Test
    fun pageChangeStartsNewTypeOn(): Unit = runBlocking {
        val note = repo.createNote(dividerId = null)
        repo.savePages(note.id, listOf(StruckPage("Page 1 Text"), StruckPage("Page 2 Text")))

        val viewModel = WriterViewModel(note.id, repo, settingsStore, testScope)
        viewModel.state.first { it.loaded }
        var typeOnActive = false

        composeTestRule.mainClock.autoAdvance = false
        composeTestRule.setContent {
            WriterScreen(
                viewModel = viewModel,
                settings = Settings(keyboard = KeyboardMode.System),
                player = dummyPlayer,
                haptics = dummyHaptics,
                onOpenBox = {},
                onTypeOnActiveChange = { active ->
                    typeOnActive = active
                }
            )
        }

        // Advance until initial page 0 type-on finishes (takes ~450ms)
        composeTestRule.mainClock.advanceTimeBy(600)
        assertFalse("Initial type-on should have finished", typeOnActive)

        // Change to page 1
        viewModel.dispatch(WriterEvent.GoToPage(1))
        composeTestRule.mainClock.advanceTimeByFrame()
        composeTestRule.mainClock.advanceTimeBy(30)

        assertTrue("Page change must trigger type-on", typeOnActive)
    }
}
