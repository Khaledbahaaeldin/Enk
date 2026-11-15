package app.enk.writer

import android.content.Context
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.unit.dp
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.room.Room
import app.enk.audio.SoundEvent
import app.enk.audio.SoundPlayer
import app.enk.audio.SoundSet
import app.enk.data.EnkDatabase
import app.enk.data.NoteRepository
import app.enk.data.SystemClock
import app.enk.keyboard.KeyboardLayout
import app.enk.keyboard.LayoutLoader
import app.enk.settings.KeyboardMode
import app.enk.settings.Settings
import app.enk.settings.SettingsStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
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
class WriterKeyboardTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @get:Rule
    val tempFolder = TemporaryFolder()

    private lateinit var db: EnkDatabase
    private lateinit var repo: NoteRepository
    private lateinit var settingsStore: SettingsStore
    private lateinit var layout: KeyboardLayout
    private val testScope = CoroutineScope(Dispatchers.IO + Job())

    private val playedEvents = mutableListOf<SoundEvent>()
    private val dummyPlayer = object : SoundPlayer {
        override fun play(event: SoundEvent, set: SoundSet, volume: Float) {
            playedEvents.add(event)
        }
        override fun release() {}
    }

    private val recordedPulses = mutableListOf<Int>()
    private val dummyHaptics = object : Haptics {
        override fun pulse(ms: Int) {
            recordedPulses.add(ms)
        }
    }

    @Before
    fun setUp() {
        val context: Context = RuntimeEnvironment.getApplication()
        db = Room.inMemoryDatabaseBuilder(context, EnkDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        repo = NoteRepository(db, SystemClock)

        val settingsFile = File(tempFolder.root, "writer_keyboard_test_settings.preferences_pb")
        val dataStore = PreferenceDataStoreFactory.create(produceFile = { settingsFile })
        settingsStore = SettingsStore(dataStore)

        layout = LayoutLoader.load(context)
    }

    @After
    fun tearDown() {
        testScope.cancel()
        db.close()
    }

    @Test
    fun typingSequenceHiSpaceBackspaceEnter(): Unit = runBlocking {
        val note = repo.createNote(dividerId = null)
        val viewModel = WriterViewModel(note.id, repo, settingsStore, testScope)

        composeTestRule.setContent {
            WriterScreen(
                viewModel = viewModel,
                settings = Settings(keyboard = KeyboardMode.Enk),
                player = dummyPlayer,
                haptics = dummyHaptics,
                onOpenBox = {},
                keyboardLayout = layout
            )
        }
        composeTestRule.waitForIdle()

        // (d) the writer_input node does not exist
        composeTestRule.onNodeWithTag("writer_input").assertDoesNotExist()
        composeTestRule.onNodeWithTag("enk_keyboard").assertExists()

        // (a) tapping key_h then key_i makes the page text "hi"
        composeTestRule.onNodeWithTag("key_h").performClick()
        composeTestRule.onNodeWithTag("key_i").performClick()
        assertEquals("hi", viewModel.state.value.currentPage.text)

        // (b) key_space tap then key_backspace leaves "hi"
        composeTestRule.onNodeWithTag("key_space").performClick()
        assertEquals("hi ", viewModel.state.value.currentPage.text)
        composeTestRule.onNodeWithTag("key_backspace").performClick()
        assertEquals("hi", viewModel.state.value.currentPage.text)

        // (c) key_enter gives "hi\n"
        composeTestRule.onNodeWithTag("key_enter").performClick()
        assertEquals("hi\n", viewModel.state.value.currentPage.text)
    }

    @Test
    fun longPressOnKeyEChangesToAlternative(): Unit = runBlocking {
        val note = repo.createNote(dividerId = null)
        val viewModel = WriterViewModel(note.id, repo, settingsStore, testScope)

        composeTestRule.setContent {
            WriterScreen(
                viewModel = viewModel,
                settings = Settings(keyboard = KeyboardMode.Enk),
                player = dummyPlayer,
                haptics = dummyHaptics,
                onOpenBox = {},
                keyboardLayout = layout
            )
        }
        composeTestRule.waitForIdle()

        composeTestRule.mainClock.autoAdvance = false
        // (e) long press key_e and choose "é" after typing "e" gives "é"
        // Touch down immediately types "e"
        composeTestRule.onNodeWithTag("key_e").performTouchInput {
            down(center)
        }
        assertEquals("e", viewModel.state.value.currentPage.text)

        // Hold for 400 ms to trigger long press popup
        composeTestRule.mainClock.advanceTimeBy(400)

        // Slot 0 is "è", slot 1 is "é" (each slot is 44 dp wide). Move right by 44 dp to choose "é".
        val movePx = with(composeTestRule.density) { 44.dp.toPx() }
        composeTestRule.onNodeWithTag("key_e").performTouchInput {
            moveBy(Offset(movePx, 0f))
            up()
        }
        composeTestRule.mainClock.autoAdvance = true
        composeTestRule.waitForIdle()

        assertEquals("é", viewModel.state.value.currentPage.text)
    }

    @Test
    fun systemKeyboardSettingShowsSystemInput(): Unit = runBlocking {
        val note = repo.createNote(dividerId = null)
        val viewModel = WriterViewModel(note.id, repo, settingsStore, testScope)

        composeTestRule.setContent {
            WriterScreen(
                viewModel = viewModel,
                settings = Settings(keyboard = KeyboardMode.System),
                player = dummyPlayer,
                haptics = dummyHaptics,
                onOpenBox = {},
                keyboardLayout = layout
            )
        }
        composeTestRule.waitForIdle()

        // (f) with Settings(keyboard = KeyboardMode.System) the enk_keyboard node does not exist and writer_input exists
        composeTestRule.onNodeWithTag("enk_keyboard").assertDoesNotExist()
        composeTestRule.onNodeWithTag("writer_input").assertExists()
    }

    @Test
    fun keyboardSwitchTogglesBetweenEnkAndSystem(): Unit = runBlocking {
        val note = repo.createNote(dividerId = null)
        val viewModel = WriterViewModel(note.id, repo, settingsStore, testScope)

        composeTestRule.setContent {
            WriterScreen(
                viewModel = viewModel,
                settings = Settings(keyboard = KeyboardMode.Enk),
                player = dummyPlayer,
                haptics = dummyHaptics,
                onOpenBox = {},
                keyboardLayout = layout
            )
        }
        composeTestRule.waitForIdle()

        // (g) pressing keyboard_switch in Enk mode swaps to the system layout and back
        composeTestRule.onNodeWithTag("enk_keyboard").assertExists()
        composeTestRule.onNodeWithTag("writer_input").assertDoesNotExist()

        composeTestRule.onNodeWithTag("keyboard_switch").performClick()
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithTag("enk_keyboard").assertDoesNotExist()
        composeTestRule.onNodeWithTag("writer_input").assertExists()

        composeTestRule.onNodeWithTag("keyboard_switch").performClick()
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithTag("enk_keyboard").assertExists()
        composeTestRule.onNodeWithTag("writer_input").assertDoesNotExist()
    }

    @Test
    fun globeKeySwapsToSystemLayout(): Unit = runBlocking {
        val note = repo.createNote(dividerId = null)
        val viewModel = WriterViewModel(note.id, repo, settingsStore, testScope)

        composeTestRule.setContent {
            WriterScreen(
                viewModel = viewModel,
                settings = Settings(keyboard = KeyboardMode.Enk),
                player = dummyPlayer,
                haptics = dummyHaptics,
                onOpenBox = {},
                keyboardLayout = layout
            )
        }
        composeTestRule.waitForIdle()

        // (h) pressing key_globe swaps to the system layout
        composeTestRule.onNodeWithTag("key_globe").performClick()
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithTag("enk_keyboard").assertDoesNotExist()
        composeTestRule.onNodeWithTag("writer_input").assertExists()
    }

    @Test
    fun nullLayoutFallsBackToSystemLayout(): Unit = runBlocking {
        val note = repo.createNote(dividerId = null)
        val viewModel = WriterViewModel(note.id, repo, settingsStore, testScope)

        composeTestRule.setContent {
            WriterScreen(
                viewModel = viewModel,
                settings = Settings(keyboard = KeyboardMode.Enk),
                player = dummyPlayer,
                haptics = dummyHaptics,
                onOpenBox = {},
                keyboardLayout = null
            )
        }
        composeTestRule.waitForIdle()

        // (i) with keyboardLayout = null the writer shows the system layout in Enk mode
        composeTestRule.onNodeWithTag("enk_keyboard").assertDoesNotExist()
        composeTestRule.onNodeWithTag("writer_input").assertExists()
    }

    @Test
    fun soundAndHapticFeedback(): Unit = runBlocking {
        val note = repo.createNote(dividerId = null)
        val viewModel = WriterViewModel(note.id, repo, settingsStore, testScope)

        composeTestRule.setContent {
            WriterScreen(
                viewModel = viewModel,
                settings = Settings(keyboard = KeyboardMode.Enk),
                player = dummyPlayer,
                haptics = dummyHaptics,
                onOpenBox = {},
                keyboardLayout = layout
            )
        }
        composeTestRule.waitForIdle()

        playedEvents.clear()
        recordedPulses.clear()

        // Letter: SoundEvent.Key, pulse 8
        composeTestRule.onNodeWithTag("key_a").performClick()
        assertEquals(SoundEvent.Key, playedEvents.last())
        assertEquals(8, recordedPulses.last())

        // Space: SoundEvent.Space, pulse 14
        composeTestRule.onNodeWithTag("key_space").performClick()
        assertEquals(SoundEvent.Space, playedEvents.last())
        assertEquals(14, recordedPulses.last())

        // Enter: SoundEvent.Enter, pulse 14
        composeTestRule.onNodeWithTag("key_enter").performClick()
        assertEquals(SoundEvent.Enter, playedEvents.last())
        assertEquals(14, recordedPulses.last())

        // Backspace: SoundEvent.Backspace, pulse 8
        composeTestRule.onNodeWithTag("key_backspace").performClick()
        assertEquals(SoundEvent.Backspace, playedEvents.last())
        assertEquals(8, recordedPulses.last())
    }

    @Test
    fun mutedSuppressesAllSounds(): Unit = runBlocking {
        val note = repo.createNote(dividerId = null)
        val viewModel = WriterViewModel(note.id, repo, settingsStore, testScope)

        composeTestRule.setContent {
            WriterScreen(
                viewModel = viewModel,
                settings = Settings(keyboard = KeyboardMode.Enk, muted = true),
                player = dummyPlayer,
                haptics = dummyHaptics,
                onOpenBox = {},
                keyboardLayout = layout
            )
        }
        composeTestRule.waitForIdle()

        playedEvents.clear()
        composeTestRule.onNodeWithTag("key_a").performClick()
        composeTestRule.onNodeWithTag("key_space").performClick()
        composeTestRule.onNodeWithTag("key_enter").performClick()
        composeTestRule.onNodeWithTag("key_backspace").performClick()

        assertTrue("mute suppresses all sounds", playedEvents.isEmpty())
    }

    @Test
    fun hardwareKeyComposeTyping(): Unit = runBlocking {
        val note = repo.createNote(dividerId = null)
        val viewModel = WriterViewModel(note.id, repo, settingsStore, testScope)

        composeTestRule.setContent {
            WriterScreen(
                viewModel = viewModel,
                settings = Settings(keyboard = KeyboardMode.Enk),
                player = dummyPlayer,
                haptics = dummyHaptics,
                onOpenBox = {},
                keyboardLayout = layout
            )
        }
        composeTestRule.waitForIdle()

        // In Enk mode, root handler processes hardware key press
        composeTestRule.onRoot().performKeyInput {
            pressKey(Key.A)
        }
        assertEquals("a", viewModel.state.value.currentPage.text)
    }

    @Test
    fun hardwareKeyInSystemModeIgnoredByRoot(): Unit = runBlocking {
        val note = repo.createNote(dividerId = null)
        val viewModel = WriterViewModel(note.id, repo, settingsStore, testScope)

        composeTestRule.setContent {
            WriterScreen(
                viewModel = viewModel,
                settings = Settings(keyboard = KeyboardMode.System),
                player = dummyPlayer,
                haptics = dummyHaptics,
                onOpenBox = {},
                keyboardLayout = layout
            )
        }
        composeTestRule.waitForIdle()

        // In System mode, root handler returns false so nothing is typed by root handler
        composeTestRule.onRoot().performKeyInput {
            pressKey(Key.A)
        }
        assertEquals("", viewModel.state.value.currentPage.text)
    }

    @Test
    fun enkKeyboardIsInTheLayoutWhileTheStartAnimationRuns(): Unit = runBlocking {
        val note = repo.createNote(dividerId = null)
        val viewModel = WriterViewModel(note.id, repo, settingsStore, testScope)

        composeTestRule.setContent {
            WriterScreen(
                viewModel = viewModel,
                settings = Settings(keyboard = KeyboardMode.Enk),
                player = dummyPlayer,
                haptics = dummyHaptics,
                onOpenBox = {},
                intro = WriterIntro(
                    paperOffsetDp = { 0f },
                    textAlpha = { 1f },
                    topBarAlpha = { 1f },
                    ownPartsAlpha = { 1f }
                ),
                keyboardLayout = layout
            )
        }
        composeTestRule.waitForIdle()

        // The keyboard must not appear when the animation ends (the paper would resize and the rail jump),
        // so it is there from the first frame; the system field stays out.
        composeTestRule.onNodeWithTag("enk_keyboard").assertExists()
        composeTestRule.onNodeWithTag("writer_input").assertDoesNotExist()
    }
}
