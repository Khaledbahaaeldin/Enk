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
import androidx.lifecycle.lifecycleScope
import app.enk.design.EnkMaterials
import app.enk.settings.Settings
import app.enk.writer.WriterScreen
import app.enk.writer.WriterViewModel
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    private val mainViewModel: MainViewModel by viewModels {
        MainViewModel.factory(applicationContext)
    }
    private var writerViewModel: WriterViewModel? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        setContent {
            val container = mainViewModel.container
            val noteId by mainViewModel.currentNoteId.collectAsState()

            if (noteId == null) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(EnkMaterials.chassisDay)
                )
            } else {
                val vm = remember(noteId) {
                    WriterViewModel(noteId!!, container.repo, container.settingsStore)
                }.also { writerViewModel = it }

                val settings by container.settingsStore.settings.collectAsState(initial = Settings())

                WriterScreen(
                    viewModel = vm,
                    settings = settings,
                    player = container.soundPlayer,
                    haptics = container.haptics,
                    onOpenBox = { /* no-op in Batch 6 */ },
                    onSettingsChange = { newSettings ->
                        lifecycleScope.launch {
                            container.settingsStore.update { newSettings }
                        }
                    }
                )
            }
        }
    }

    override fun onStop() {
        super.onStop()
        writerViewModel?.let { vm ->
            lifecycleScope.launch {
                vm.flush()
            }
        }
    }
}
