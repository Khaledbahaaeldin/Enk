package app.enk.settings

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import app.enk.AppContainer
import app.enk.audio.SoundPlayer
import app.enk.audio.SoundSet
import app.enk.data.Exporter
import app.enk.data.ImportResult
import app.enk.data.Importer
import app.enk.design.theme.ThemeId
import app.enk.design.theme.ThemeMode
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import java.io.IOException
import app.enk.audio.SoundEvent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

val THEME_TILE_ORDER: List<ThemeId> = listOf(
    ThemeId.Graphite,
    ThemeId.Apricot,
    ThemeId.Sky,
    ThemeId.Slate,
    ThemeId.Mono,
    ThemeId.Dynamic
)

val MODE_ORDER: List<ThemeMode> = listOf(
    ThemeMode.Light,
    ThemeMode.Dark,
    ThemeMode.System
)

val SETUP_GROUP_ORDER: List<String> = listOf(
    "Appearance",
    "Writing",
    "Sound",
    "Display",
    "Data",
    "About"
)

/** An export or import in progress, or its last result. */
sealed interface TransferState {
    data object Idle : TransferState
    data class Running(val message: String) : TransferState
    data class Done(val message: String) : TransferState
    data class Failed(val message: String) : TransferState
}

/** "Imported 12 notes, 2 skipped." */
internal fun importSummary(result: ImportResult.Done): String {
    val imported = result.added + result.replaced
    val notes = if (imported == 1) "1 note" else "$imported notes"
    return if (result.skipped == 0) "Imported $notes." else "Imported $notes, ${result.skipped} skipped."
}

/** The name offered to the file picker for an export made on [date]. */
internal fun exportFileName(date: java.time.LocalDate): String = "enk-notes-$date.zip"

class SettingsViewModel(
    private val store: SettingsStore,
    private val exporter: Exporter,
    private val importer: Importer,
    private val soundPlayer: SoundPlayer? = null,
    private val soundManifest: List<SoundSet> = emptyList(),
    private val context: Context? = null,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
    externalScope: CoroutineScope? = null
) : ViewModel() {

    private val ownsScope = externalScope == null
    private val scope = externalScope ?: CoroutineScope(SupervisorJob() + ioDispatcher)

    val settings: StateFlow<Settings> = store.settings
        .stateIn(
            scope = scope,
            started = SharingStarted.Eagerly,
            initialValue = Settings()
        )

    fun setTheme(t: ThemeId) {
        scope.launch(ioDispatcher) {
            store.update { it.copy(theme = t) }
        }
    }

    fun setThemeMode(m: ThemeMode) {
        scope.launch(ioDispatcher) {
            store.update { it.copy(themeMode = m) }
        }
    }

    fun setCorrection(m: CorrectionMode) {
        scope.launch(ioDispatcher) {
            store.update { it.copy(correction = m) }
        }
    }

    fun setAutocorrect(b: Boolean) {
        scope.launch(ioDispatcher) {
            store.update { it.copy(autocorrect = b) }
        }
    }

    /** Saves the choice, prepares the set off the main thread and plays one key so it can be heard. */
    fun setSoundSet(id: SoundSetId) {
        scope.launch(ioDispatcher) {
            store.update { it.copy(soundSet = id) }
            val soundSet = soundManifest.find { it.id.equals(id.name, ignoreCase = true) } ?: return@launch
            soundPlayer?.prepare(soundSet)
            if (!store.settings.first().muted) soundPlayer?.play(SoundEvent.Key, soundSet, 1f)
        }
    }

    fun setMuted(b: Boolean) {
        scope.launch(ioDispatcher) {
            store.update { it.copy(muted = b) }
        }
    }

    fun setBellVolume(v: Float) {
        scope.launch(ioDispatcher) {
            store.update { it.copy(bellVolume = v) }
        }
    }

    fun setHaptics(b: Boolean) {
        scope.launch(ioDispatcher) {
            store.update { it.copy(haptics = b) }
        }
    }

    fun setReduceEffects(b: Boolean) {
        scope.launch(ioDispatcher) {
            store.update { it.copy(reduceEffects = b) }
        }
    }

    private val _transfer = MutableStateFlow<TransferState>(TransferState.Idle)

    /** The state of the export or import in progress, or its last result until it is cleared. */
    val transfer: StateFlow<TransferState> = _transfer.asStateFlow()

    fun clearTransfer() {
        if (_transfer.value !is TransferState.Running) _transfer.value = TransferState.Idle
    }

    /**
     * Exports on this view model's scope, so leaving the screen cannot cancel it half way, and reports
     * the result. A second request while one is running does nothing.
     */
    fun exportNotes(uri: Uri) {
        if (!startTransfer("Exporting your notes…")) return
        scope.launch(ioDispatcher) {
            _transfer.value = try {
                exportTo(uri)
                TransferState.Done("Notes exported.")
            } catch (e: CancellationException) {
                throw e
            } catch (_: Exception) {
                TransferState.Failed("Couldn't export your notes.")
            }
        }
    }

    /** Imports on this view model's scope and reports what was added, replaced or skipped. */
    fun importNotes(uri: Uri) {
        if (!startTransfer("Importing notes…")) return
        scope.launch(ioDispatcher) {
            _transfer.value = try {
                when (val result = importFrom(uri)) {
                    is ImportResult.Done -> TransferState.Done(importSummary(result))
                    is ImportResult.Failed -> TransferState.Failed("Couldn't import: ${result.reason}")
                }
            } catch (e: CancellationException) {
                throw e
            } catch (_: Exception) {
                TransferState.Failed("Couldn't import those notes.")
            }
        }
    }

    private fun startTransfer(message: String): Boolean {
        while (true) {
            val current = _transfer.value
            if (current is TransferState.Running) return false
            if (_transfer.compareAndSet(current, TransferState.Running(message))) return true
        }
    }

    suspend fun exportTo(uri: Uri) = withContext(ioDispatcher) {
        val cr = context?.contentResolver ?: throw IOException("No context to open the file with.")
        val out = cr.openOutputStream(uri) ?: throw IOException("Could not open the file to write.")
        out.use { exporter.export(it) }
    }

    suspend fun importFrom(uri: Uri): ImportResult = withContext(ioDispatcher) {
        val cr = context?.contentResolver
            ?: return@withContext ImportResult.Failed("Context required to open URI")
        cr.openInputStream(uri)?.use { inStream ->
            importer.import(inStream)
        } ?: ImportResult.Failed("Could not open input stream for URI")
    }

    override fun onCleared() {
        super.onCleared()
        if (ownsScope) {
            scope.cancel()
        }
    }

    companion object {
        fun factory(
            context: Context,
            container: AppContainer,
            ioDispatcher: CoroutineDispatcher = Dispatchers.IO
        ): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    val dividers = container.db.dividerDao()
                    return SettingsViewModel(
                        store = container.settingsStore,
                        exporter = Exporter(container.repo, dividers),
                        importer = Importer(container.repo, dividers),
                        soundPlayer = container.soundPlayer,
                        soundManifest = container.soundManifest,
                        context = context.applicationContext,
                        ioDispatcher = ioDispatcher
                    ) as T
                }
            }
    }
}
