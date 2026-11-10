package app.enk.settings

import android.content.Context
import android.net.Uri
import androidx.test.core.app.ApplicationProvider
import app.enk.AppContainer
import app.enk.audio.Sample
import app.enk.audio.SoundEvent
import app.enk.audio.SoundPlayer
import app.enk.audio.SoundSet
import app.enk.data.Exporter
import app.enk.data.ImportResult
import app.enk.data.Importer
import app.enk.design.theme.ThemeId
import app.enk.design.theme.ThemeMode
import app.enk.testContainer
import app.enk.text.StruckPage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.io.ByteArrayOutputStream
import java.io.File
import java.time.LocalDate
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream
import java.util.concurrent.CopyOnWriteArrayList

@RunWith(RobolectricTestRunner::class)
class SettingsViewModelTest {

    @get:Rule
    val tempFolder = TemporaryFolder()

    private lateinit var context: Context
    private lateinit var container: AppContainer

    private val prepared = CopyOnWriteArrayList<SoundSet>()
    private val played = CopyOnWriteArrayList<Pair<SoundEvent, String>>()
    private val player = object : SoundPlayer {
        override fun prepare(soundSet: SoundSet) {
            prepared += soundSet
        }

        override fun play(event: SoundEvent, soundSet: SoundSet, volume: Float) {
            played += event to soundSet.id
        }

        override fun release() {}
    }

    private val manifest = listOf("classic", "antique", "soft", "keyboard").map {
        SoundSet(it, it.replaceFirstChar(Char::uppercase), emptyList(), Sample(""), Sample(""), Sample(""), Sample(""))
    }

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        container = testContainer(context, tempFolder.root)
    }

    /**
     * DataStore serialises its writes, so one more write queued behind the test's own returns only when
     * they are all on disk. Without it a write can finish after the temporary folder is deleted and its
     * exception surfaces in an unrelated test.
     */
    @After
    fun settleSettingsWrites() {
        runBlocking { container.settingsStore.update { it } }
    }

    private fun viewModel(): SettingsViewModel {
        val dividers = container.db.dividerDao()
        return SettingsViewModel(
            store = container.settingsStore,
            exporter = Exporter(container.repo, dividers),
            importer = Importer(container.repo, dividers),
            soundPlayer = player,
            soundManifest = manifest,
            context = context,
            ioDispatcher = Dispatchers.IO
        )
    }

    private fun awaitTransfer(vm: SettingsViewModel, done: (TransferState) -> Boolean): TransferState =
        runBlocking { vm.transfer.first(done) }

    @Test
    fun themeChangePersists() = runBlocking {
        val vm = viewModel()
        vm.setTheme(ThemeId.Apricot)
        assertEquals(ThemeId.Apricot, container.settingsStore.settings.first { it.theme == ThemeId.Apricot }.theme)
        assertEquals(ThemeId.Apricot, vm.settings.first { it.theme == ThemeId.Apricot }.theme)
    }

    @Test
    fun modeChangePersists() = runBlocking {
        val vm = viewModel()
        vm.setThemeMode(ThemeMode.Dark)
        assertEquals(ThemeMode.Dark, container.settingsStore.settings.first { it.themeMode == ThemeMode.Dark }.themeMode)
    }

    @Test
    fun settingChangesPersistWithoutOverwritingEachOther() = runBlocking {
        val vm = viewModel()
        vm.setCorrection(CorrectionMode.StrikeThrough)
        vm.setAutocorrect(true)
        vm.setMuted(true)
        vm.setBellVolume(0.85f)
        vm.setHaptics(false)
        vm.setReduceEffects(true)

        val persisted = container.settingsStore.settings.first {
            it.correction == CorrectionMode.StrikeThrough && it.autocorrect && it.muted &&
                it.bellVolume == 0.85f && !it.haptics && it.reduceEffects
        }
        assertEquals(0.85f, persisted.bellVolume, 0.001f)
    }

    // ---- sound sets ----

    @Test
    fun choosingASoundSetPreparesItOnceAndPlaysOneKeyToPreviewIt() {
        val vm = viewModel()
        vm.setSoundSet(SoundSetId.Antique)
        runBlocking { container.settingsStore.settings.first { it.soundSet == SoundSetId.Antique } }
        waitUntil { played.isNotEmpty() }

        assertEquals(listOf("antique"), prepared.map { it.id })
        assertEquals(listOf(SoundEvent.Key to "antique"), played.toList())
    }

    @Test
    fun aMutedAppPreparesTheSetButStaysSilent() {
        val vm = viewModel()
        vm.setMuted(true)
        runBlocking { container.settingsStore.settings.first { it.muted } }

        vm.setSoundSet(SoundSetId.Soft)
        runBlocking { container.settingsStore.settings.first { it.soundSet == SoundSetId.Soft } }
        waitUntil { prepared.isNotEmpty() }

        assertEquals(listOf("soft"), prepared.map { it.id })
        assertTrue("muted: no preview", played.isEmpty())
    }

    // ---- export and import ----

    private fun zipOf(vararg entries: Pair<String, String>): File {
        val bytes = ByteArrayOutputStream()
        ZipOutputStream(bytes).use { zip ->
            for ((name, text) in entries) {
                zip.putNextEntry(ZipEntry(name))
                zip.write(text.toByteArray())
                zip.closeEntry()
            }
        }
        return File(tempFolder.root, "t${System.nanoTime()}.zip").apply { writeBytes(bytes.toByteArray()) }
    }

    @Test
    fun anExportThenAnImportRoundTripsAndReportsWhatHappened() {
        runBlocking {
            val note = container.repo.createNote(dividerId = null)
            container.repo.savePages(note.id, listOf(StruckPage("rain on the glass")))
        }
        val vm = viewModel()
        val file = File(tempFolder.root, "out.zip")

        vm.exportNotes(Uri.fromFile(file))
        val exported = awaitTransfer(vm) { it is TransferState.Done || it is TransferState.Failed }
        assertEquals(TransferState.Done("Notes exported."), exported)
        assertTrue("the file was written", file.length() > 0)

        vm.clearTransfer()
        vm.importNotes(Uri.fromFile(file))
        val imported = awaitTransfer(vm) { it is TransferState.Done || it is TransferState.Failed }
        assertTrue("got $imported", imported is TransferState.Done)
        assertTrue((imported as TransferState.Done).message.startsWith("Imported "))
    }

    @Test
    fun aFailedImportReportsTheReasonAndKeepsEveryNote() {
        runBlocking { container.repo.createNote(dividerId = null) }
        val vm = viewModel()
        val bad = zipOf("invalid.txt" to "Some text")

        vm.importNotes(Uri.fromFile(bad))
        val result = awaitTransfer(vm) { it is TransferState.Done || it is TransferState.Failed }

        assertEquals(TransferState.Failed("Couldn't import: Non-markdown file rejected: invalid.txt"), result)
        assertEquals("the existing note is untouched", 1, runBlocking { container.repo.observeBox(null).first().size })
    }

    @Test
    fun importFromAnUnreadableUriFailsCleanlyInsteadOfCrashing() {
        val vm = viewModel()
        vm.importNotes(Uri.parse("content://app.enk.nonexistent/none"))
        val result = awaitTransfer(vm) { it is TransferState.Done || it is TransferState.Failed }
        assertTrue("got $result", result is TransferState.Failed)
    }

    @Test
    fun anExportToAnUnwritableUriFailsCleanlyInsteadOfCrashing() {
        val vm = viewModel()
        vm.exportNotes(Uri.parse("content://app.enk.nonexistent/none"))
        val result = awaitTransfer(vm) { it is TransferState.Done || it is TransferState.Failed }
        assertEquals(TransferState.Failed("Couldn't export your notes."), result)
    }

    @Test
    fun aSecondTransferWhileOneRunsIsIgnoredAndTheResultCanBeCleared() {
        val vm = viewModel()
        val file = File(tempFolder.root, "double.zip")
        vm.exportNotes(Uri.fromFile(file))
        vm.exportNotes(Uri.fromFile(File(tempFolder.root, "second.zip")))
        awaitTransfer(vm) { it is TransferState.Done || it is TransferState.Failed }

        assertTrue(file.exists())
        assertFalse("the second request was ignored", File(tempFolder.root, "second.zip").exists())
        vm.clearTransfer()
        assertEquals(TransferState.Idle, vm.transfer.value)
    }

    // ---- pure helpers ----

    @Test
    fun theImportSummaryCountsAddedAndReplacedAndMentionsSkips() {
        assertEquals("Imported 1 note.", importSummary(ImportResult.Done(added = 1, replaced = 0, skipped = 0)))
        assertEquals("Imported 12 notes, 2 skipped.", importSummary(ImportResult.Done(added = 10, replaced = 2, skipped = 2)))
        assertEquals("Imported 0 notes.", importSummary(ImportResult.Done(added = 0, replaced = 0, skipped = 0)))
    }

    @Test
    fun theExportFileNameIsDated() {
        assertEquals("enk-notes-2026-11-09.zip", exportFileName(LocalDate.of(2026, 11, 9)))
        assertEquals("enk-notes-2026-01-05.zip", exportFileName(LocalDate.of(2026, 1, 5)))
    }

    private fun waitUntil(condition: () -> Boolean) {
        val deadline = System.nanoTime() + 60_000_000_000L
        while (!condition()) {
            check(System.nanoTime() < deadline) { "timed out" }
            Thread.sleep(10)
        }
    }
}
