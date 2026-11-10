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
import kotlinx.coroutines.CoroutineDispatcher
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

    fun setSoundSet(id: SoundSetId) {
        val soundSet = soundManifest.find { it.id.equals(id.name, ignoreCase = true) }
        if (soundSet != null) {
            soundPlayer?.prepare(soundSet)
        }
        scope.launch(ioDispatcher) {
            store.update { it.copy(soundSet = id) }
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

    suspend fun exportTo(uri: Uri) = withContext(ioDispatcher) {
        val cr = context?.contentResolver
            ?: throw IllegalStateException("Context required to open URI")
        cr.openOutputStream(uri)?.use { out ->
            exporter.export(out)
        }
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
