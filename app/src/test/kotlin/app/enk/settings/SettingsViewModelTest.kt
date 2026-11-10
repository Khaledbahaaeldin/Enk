package app.enk.settings

import android.content.Context
import android.net.Uri
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import app.enk.audio.Sample
import app.enk.audio.SoundEvent
import app.enk.audio.SoundPlayer
import app.enk.audio.SoundSet
import app.enk.data.EnkDatabase
import app.enk.data.Exporter
import app.enk.data.ImportResult
import app.enk.data.Importer
import app.enk.data.NoteRepository
import app.enk.data.SystemClock
import app.enk.design.theme.ThemeId
import app.enk.design.theme.ThemeMode
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
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
import java.io.File

@RunWith(RobolectricTestRunner::class)
class SettingsViewModelTest {

    @get:Rule
    val tempFolder = TemporaryFolder()

    private lateinit var db: EnkDatabase
    private lateinit var repo: NoteRepository
    private lateinit var store: SettingsStore
    private lateinit var exporter: Exporter
    private lateinit var importer: Importer

    private var preparedSoundSet: SoundSet? = null
    private val fakePlayer = object : SoundPlayer {
        override fun prepare(soundSet: SoundSet) {
            preparedSoundSet = soundSet
        }
        override fun play(event: SoundEvent, soundSet: SoundSet, volume: Float) {}
        override fun release() {}
    }

    private val fakeSoundManifest = listOf(
        SoundSet("classic", "Classic", emptyList(), Sample(""), Sample(""), Sample(""), Sample("")),
        SoundSet("antique", "Antique", emptyList(), Sample(""), Sample(""), Sample(""), Sample("")),
        SoundSet("soft", "Soft", emptyList(), Sample(""), Sample(""), Sample(""), Sample("")),
        SoundSet("keyboard", "Keyboard", emptyList(), Sample(""), Sample(""), Sample(""), Sample(""))
    )

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, EnkDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        repo = NoteRepository(db, SystemClock)
        val dividers = db.dividerDao()
        exporter = Exporter(repo, dividers)
        importer = Importer(repo, dividers)

        val settingsFile = File(tempFolder.root, "test_settings.preferences_pb")
        val dataStore = PreferenceDataStoreFactory.create(produceFile = { settingsFile })
        store = SettingsStore(dataStore)
    }

    @After
    fun tearDown() {
        db.close()
    }

    @Test
    fun themeChangePersists() = runBlocking {
        val vm = SettingsViewModel(
            store = store,
            exporter = exporter,
            importer = importer,
            soundPlayer = fakePlayer,
            soundManifest = fakeSoundManifest,
            ioDispatcher = Dispatchers.IO
        )

        vm.setTheme(ThemeId.Apricot)
        val persisted = store.settings.first { it.theme == ThemeId.Apricot }
        assertEquals(ThemeId.Apricot, persisted.theme)
        val vmValue = vm.settings.first { it.theme == ThemeId.Apricot }
        assertEquals(ThemeId.Apricot, vmValue.theme)
    }

    @Test
    fun modeChangePersists() = runBlocking {
        val vm = SettingsViewModel(
            store = store,
            exporter = exporter,
            importer = importer,
            soundPlayer = fakePlayer,
            soundManifest = fakeSoundManifest,
            ioDispatcher = Dispatchers.IO
        )

        vm.setThemeMode(ThemeMode.Dark)
        val persisted = store.settings.first { it.themeMode == ThemeMode.Dark }
        assertEquals(ThemeMode.Dark, persisted.themeMode)
        val vmValue = vm.settings.first { it.themeMode == ThemeMode.Dark }
        assertEquals(ThemeMode.Dark, vmValue.themeMode)
    }

    @Test
    fun settingChangesPersist() = runBlocking {
        val vm = SettingsViewModel(
            store = store,
            exporter = exporter,
            importer = importer,
            soundPlayer = fakePlayer,
            soundManifest = fakeSoundManifest,
            ioDispatcher = Dispatchers.IO
        )

        vm.setCorrection(CorrectionMode.StrikeThrough)
        vm.setAutocorrect(true)
        vm.setMuted(true)
        vm.setBellVolume(0.85f)
        vm.setHaptics(false)
        vm.setReduceEffects(true)

        val persisted = store.settings.first {
            it.correction == CorrectionMode.StrikeThrough &&
                    it.autocorrect &&
                    it.muted &&
                    it.bellVolume == 0.85f &&
                    !it.haptics &&
                    it.reduceEffects
        }

        assertEquals(CorrectionMode.StrikeThrough, persisted.correction)
        assertEquals(true, persisted.autocorrect)
        assertEquals(true, persisted.muted)
        assertEquals(0.85f, persisted.bellVolume, 0.001f)
        assertEquals(false, persisted.haptics)
        assertEquals(true, persisted.reduceEffects)
    }

    @Test
    fun importFailureSurfacesReasonAndKeepsData() = runBlocking {
        // Seed a note
        repo.createNote(dividerId = null)
        assertEquals(1, repo.observeBox(null).first().size)

        val context = ApplicationProvider.getApplicationContext<Context>()
        val vm = SettingsViewModel(
            store = store,
            exporter = exporter,
            importer = importer,
            soundPlayer = fakePlayer,
            soundManifest = fakeSoundManifest,
            context = context,
            ioDispatcher = Dispatchers.IO
        )

        // Archive with invalid entry (e.g. non-markdown file)
        val badZipBaos = java.io.ByteArrayOutputStream()
        java.util.zip.ZipOutputStream(badZipBaos).use { zos ->
            zos.putNextEntry(java.util.zip.ZipEntry("invalid.txt"))
            zos.write("Some text".toByteArray())
            zos.closeEntry()
        }
        val badFile = File(tempFolder.root, "corrupted.zip").apply {
            writeBytes(badZipBaos.toByteArray())
        }
        val uri = Uri.fromFile(badFile)

        val result = vm.importFrom(uri)
        assertTrue(result is ImportResult.Failed)
        assertEquals("Non-markdown file rejected: invalid.txt", (result as ImportResult.Failed).reason)

        // Data was kept intact
        assertEquals(1, repo.observeBox(null).first().size)
    }

    @Test
    fun soundSetChangePreparesPlayer() = runBlocking {
        val vm = SettingsViewModel(
            store = store,
            exporter = exporter,
            importer = importer,
            soundPlayer = fakePlayer,
            soundManifest = fakeSoundManifest,
            ioDispatcher = Dispatchers.IO
        )

        vm.setSoundSet(SoundSetId.Antique)
        val persisted = store.settings.first { it.soundSet == SoundSetId.Antique }

        assertEquals("antique", preparedSoundSet?.id)
        assertEquals(SoundSetId.Antique, persisted.soundSet)
    }
}
