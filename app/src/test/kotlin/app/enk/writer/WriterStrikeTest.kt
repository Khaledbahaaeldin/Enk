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
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
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
class WriterStrikeTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @get:Rule
    val tempFolder = TemporaryFolder()

    private lateinit var db: EnkDatabase
    private lateinit var repo: NoteRepository
    private lateinit var settingsStore: SettingsStore
    private val testScope = CoroutineScope(Dispatchers.IO + Job())

    private val playedSounds = mutableListOf<SoundEvent>()
    private val recordingPlayer = object : SoundPlayer {
        override fun play(event: SoundEvent, set: SoundSet, volume: Float) {
            playedSounds.add(event)
        }
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

        val settingsFile = File(tempFolder.root, "writer_strike_test_settings.preferences_pb")
        val dataStore = PreferenceDataStoreFactory.create(produceFile = { settingsFile })
        settingsStore = SettingsStore(dataStore)
    }

    @After
    fun tearDown() {
        testScope.cancel()
        db.close()
    }

    @Test
    fun typingWithSystemKeyboardCreatesStrikeLogKeyEntryAndOneSound(): Unit = runBlocking {
        val note = repo.createNote(dividerId = null)
        val viewModel = WriterViewModel(note.id, repo, settingsStore, testScope)
        val testStrikes = StrikeLog()

        composeTestRule.setContent {
            WriterScreen(
                viewModel = viewModel,
                settings = Settings(keyboard = KeyboardMode.System),
                player = recordingPlayer,
                haptics = dummyHaptics,
                onOpenBox = {},
                strikes = testStrikes
            )
        }
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithTag("writer_input").performTextInput("a")
        composeTestRule.waitForIdle()

        val key = testStrikes.lastRecordedKey
        assertEquals("Must record exactly one StrikeLog key entry", 0, key?.index)
        assertEquals(1, playedSounds.count { it == SoundEvent.Key })
    }

    @Test
    fun typingWithEnkKeyboardCreatesStrikeLogKeyEntryAndOneSound(): Unit = runBlocking {
        val context: Context = RuntimeEnvironment.getApplication()
        val note = repo.createNote(dividerId = null)
        val viewModel = WriterViewModel(note.id, repo, settingsStore, testScope)
        val testStrikes = StrikeLog()
        val layout = LayoutLoader.load(context)

        composeTestRule.setContent {
            WriterScreen(
                viewModel = viewModel,
                settings = Settings(keyboard = KeyboardMode.Enk),
                player = recordingPlayer,
                haptics = dummyHaptics,
                onOpenBox = {},
                keyboardLayout = layout,
                strikes = testStrikes
            )
        }
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithTag("key_a").performClick()
        composeTestRule.waitForIdle()

        val key = testStrikes.lastRecordedKey
        assertEquals("Must record exactly one StrikeLog key entry", 0, key?.index)
        assertEquals(1, playedSounds.count { it == SoundEvent.Key })
    }
}
