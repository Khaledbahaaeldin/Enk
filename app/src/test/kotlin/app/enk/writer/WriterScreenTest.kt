package app.enk.writer

import android.content.Context
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.room.Room
import app.enk.audio.SoundEvent
import app.enk.audio.SoundPlayer
import app.enk.audio.SoundSet
import app.enk.data.EnkDatabase
import app.enk.data.NoteRepository
import app.enk.data.SystemClock
import app.enk.settings.Settings
import app.enk.settings.SettingsStore
import app.enk.text.StruckPage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
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
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelStore
import kotlinx.coroutines.delay
import kotlinx.coroutines.withTimeout

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], qualifiers = "w400dp-h820dp")
class WriterScreenTest {

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

        val settingsFile = File(tempFolder.root, "writer_screen_test_settings.preferences_pb")
        val dataStore = PreferenceDataStoreFactory.create(produceFile = { settingsFile })
        settingsStore = SettingsStore(dataStore)
    }

    @After
    fun tearDown() {
        testScope.cancel()
        db.close()
    }

    @Test
    fun typingShowsText(): Unit = runBlocking {
        val note = repo.createNote(dividerId = null)
        val viewModel = WriterViewModel(note.id, repo, settingsStore, testScope)

        composeTestRule.setContent {
            WriterScreen(
                viewModel = viewModel,
                settings = Settings(),
                player = dummyPlayer,
                haptics = dummyHaptics,
                onOpenBox = {}
            )
        }
        composeTestRule.waitForIdle()

        viewModel.dispatch(WriterEvent.Insert("Hello Enk"))
        composeTestRule.waitForIdle()

        assertEquals("Hello Enk", viewModel.state.value.pages[0].text)
        composeTestRule.onNodeWithText("Hello Enk", substring = true).assertExists()
    }

    @Test
    fun longPressShowsPanel(): Unit = runBlocking {
        val note = repo.createNote(dividerId = null)
        val viewModel = WriterViewModel(note.id, repo, settingsStore, testScope)

        composeTestRule.setContent {
            WriterScreen(
                viewModel = viewModel,
                settings = Settings(),
                player = dummyPlayer,
                haptics = dummyHaptics,
                onOpenBox = {}
            )
        }
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithTag("carriage").performTouchInput {
            down(center)
            advanceEventTime(500L)
            up()
        }
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithTag("control_panel").assertExists()
    }

    @Test
    fun dragCarriageMovesCaret(): Unit = runBlocking {
        val note = repo.createNote(dividerId = null)
        val pageText = "The quick brown fox jumps over the lazy dog"
        repo.savePages(note.id, listOf(StruckPage(pageText)))
        val viewModel = WriterViewModel(note.id, repo, settingsStore, testScope)

        composeTestRule.setContent {
            WriterScreen(
                viewModel = viewModel,
                settings = Settings(),
                player = dummyPlayer,
                haptics = dummyHaptics,
                onOpenBox = {}
            )
        }
        composeTestRule.waitForIdle()

        val initialCaret = viewModel.state.value.caret
        composeTestRule.onNodeWithTag("carriage").performTouchInput {
            down(center)
            moveBy(Offset(100f, 0f))
            up()
        }
        composeTestRule.waitForIdle()

        assertTrue(
            "Dragging carriage must move caret from initial $initialCaret",
            viewModel.state.value.caret != initialCaret
        )
    }

    @Test
    fun panelHiddenInDay(): Unit = runBlocking {
        val note = repo.createNote(dividerId = null)
        val viewModel = WriterViewModel(note.id, repo, settingsStore, testScope)

        composeTestRule.setContent {
            WriterScreen(
                viewModel = viewModel,
                settings = Settings(),
                player = dummyPlayer,
                haptics = dummyHaptics,
                onOpenBox = {}
            )
        }
        composeTestRule.waitForIdle()

        assertFalse("Lamp is off initially in day", viewModel.state.value.lampOn)
        composeTestRule.onNodeWithTag("control_panel").assertDoesNotExist()
    }

    @Test
    fun wordmarkOpensBox(): Unit = runBlocking {
        var boxOpened = false
        val note = repo.createNote(dividerId = null)
        val viewModel = WriterViewModel(note.id, repo, settingsStore, testScope)

        composeTestRule.setContent {
            WriterScreen(
                viewModel = viewModel,
                settings = Settings(),
                player = dummyPlayer,
                haptics = dummyHaptics,
                onOpenBox = { boxOpened = true }
            )
        }
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithTag("wordmark").performClick()
        composeTestRule.waitForIdle()
        assertTrue("onOpenBox must be called when wordmark button is clicked", boxOpened)
    }

    @Test
    fun theBoxButtonIsVisibleAndOpensBox(): Unit = runBlocking {
        var boxOpened = false
        val note = repo.createNote(dividerId = null)
        val viewModel = WriterViewModel(note.id, repo, settingsStore, testScope)

        composeTestRule.setContent {
            WriterScreen(
                viewModel = viewModel,
                settings = Settings(),
                player = dummyPlayer,
                haptics = dummyHaptics,
                onOpenBox = { boxOpened = true }
            )
        }
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithTag("box_button").assertIsDisplayed()
        composeTestRule.onNodeWithTag("box_button").performClick()
        composeTestRule.waitForIdle()
        assertTrue("the Box button must open the Box", boxOpened)
    }

    @Test
    fun rotationKeepsText(): Unit = runBlocking {
        val note = repo.createNote(dividerId = null)
        val sampleText = "Persisted typing across screen rotation"
        repo.savePages(note.id, listOf(StruckPage(sampleText)))
        val viewModel = WriterViewModel(note.id, repo, settingsStore, testScope)

        val screenKey = androidx.compose.runtime.mutableStateOf(0)

        composeTestRule.setContent {
            androidx.compose.runtime.key(screenKey.value) {
                WriterScreen(
                    viewModel = viewModel,
                    settings = Settings(),
                    player = dummyPlayer,
                    haptics = dummyHaptics,
                    onOpenBox = {}
                )
            }
        }
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("Persisted typing", substring = true).assertExists()

        // Simulate activity recreation / rotation by resetting content with the same ViewModel
        screenKey.value++
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("Persisted typing", substring = true).assertExists()
    }

    @Test
    fun hintHiddenWhileLampOn(): Unit = runBlocking {
        val note = repo.createNote(dividerId = null)
        val viewModel = WriterViewModel(note.id, repo, settingsStore, testScope)

        composeTestRule.setContent {
            WriterScreen(
                viewModel = viewModel,
                settings = Settings(hintDismissed = false),
                player = dummyPlayer,
                haptics = dummyHaptics,
                onOpenBox = {}
            )
        }
        composeTestRule.waitForIdle()

        // Hint is visible when lamp is off
        composeTestRule.onNodeWithText("drag marker to scrub", substring = true).assertExists()

        // Toggle lamp on
        viewModel.dispatch(WriterEvent.ToggleLamp)
        composeTestRule.waitForIdle()

        // Hint must not be visible when lamp is on
        composeTestRule.onNodeWithText("drag marker to scrub", substring = true).assertDoesNotExist()
    }

    @Test
    fun typingThroughTheImeFieldReachesTheViewModel(): Unit = runBlocking {
        val note = repo.createNote(dividerId = null)
        val viewModel = WriterViewModel(note.id, repo, settingsStore, testScope)

        composeTestRule.setContent {
            WriterScreen(
                viewModel = viewModel,
                settings = Settings(),
                player = dummyPlayer,
                haptics = dummyHaptics,
                onOpenBox = {}
            )
        }
        composeTestRule.waitForIdle()
        withTimeout(5_000) { while (!viewModel.state.value.loaded) delay(10) }

        val field = composeTestRule.onNode(hasSetTextAction())
        // Burst of separate commits, the way fast key presses arrive.
        for (ch in "hello") {
            field.performTextInput(ch.toString())
        }
        field.performTextInput(" world")
        composeTestRule.waitForIdle()

        assertEquals("hello world", viewModel.state.value.pages[0].text)
        assertEquals(11, viewModel.state.value.caret)
        composeTestRule.onNodeWithText("hello world", substring = true).assertExists()
    }

    @Test
    fun activityViewModelStoreSurvivesRotationAndFlushesWhenCleared(): Unit = runBlocking {
        val note = repo.createNote(dividerId = null)
        val store = ViewModelStore()
        val factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T =
                WriterViewModel(note.id, repo, settingsStore) as T
        }
        val key = "writer-${note.id}"
        val first = ViewModelProvider(store, factory)[key, WriterViewModel::class.java]
        withTimeout(5_000) { while (!first.state.value.loaded) delay(10) }
        first.dispatch(WriterEvent.Insert("last words"))

        // A rotation recreates the activity but keeps the store: same instance, same text.
        val second = ViewModelProvider(store, factory)[key, WriterViewModel::class.java]
        assertTrue("rotation must reuse the writer view model", first === second)
        assertEquals("last words", second.state.value.pages[0].text)

        // Finishing the activity clears the store, and onCleared must flush the pending edit.
        store.clear()
        withTimeout(5_000) {
            while (repo.load(note.id)?.pages?.firstOrNull()?.text != "last words") delay(25)
        }
    }
}
