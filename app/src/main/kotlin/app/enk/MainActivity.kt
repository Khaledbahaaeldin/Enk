package app.enk

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import app.enk.design.EnkMaterials
import app.enk.settings.Settings
import app.enk.writer.WriterScreen
import app.enk.writer.WriterViewModel
import kotlinx.coroutines.launch
import androidx.compose.runtime.rememberCoroutineScope

class MainActivity : ComponentActivity() {
    private val mainViewModel: MainViewModel by viewModels {
        MainViewModel.factory(applicationContext)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        setContent {
            val container = mainViewModel.container
            val noteId by mainViewModel.currentNoteId.collectAsState()
            val scope = rememberCoroutineScope()

            val id = noteId
            if (id == null) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(EnkMaterials.chassisDay)
                )
            } else {
                // Owned by the activity's ViewModelStore: it survives rotation and onCleared()
                // flushes the last edits when the activity is finished.
                val vm = remember(id) {
                    ViewModelProvider(
                        this@MainActivity,
                        object : ViewModelProvider.Factory {
                            @Suppress("UNCHECKED_CAST")
                            override fun <T : ViewModel> create(modelClass: Class<T>): T =
                                WriterViewModel(id, container.repo, container.settingsStore) as T
                        }
                    )["writer-$id", WriterViewModel::class.java]
                }

                val settings by container.settingsStore.settings.collectAsState(initial = Settings())

                WriterScreen(
                    viewModel = vm,
                    settings = settings,
                    player = container.soundPlayer,
                    soundSets = container.soundManifest,
                    haptics = container.haptics,
                    onOpenBox = { /* no-op in Batch 6 */ },
                    onSettingsChange = { change ->
                        scope.launch { container.settingsStore.update { it.change() } }
                    }
                )
            }
        }
    }
}
