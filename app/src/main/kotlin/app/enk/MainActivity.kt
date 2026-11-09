package app.enk

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import app.enk.design.theme.EnkShellTheme
import app.enk.settings.Settings
import app.enk.shell.EnkNavHost

class MainActivity : ComponentActivity() {
    private val mainViewModel: MainViewModel by viewModels {
        MainViewModel.factory(applicationContext)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        setContent {
            val container = mainViewModel.container
            val settings by container.settingsStore.settings.collectAsState(initial = Settings())

            EnkShellTheme(theme = settings.theme, mode = settings.themeMode) {
                EnkNavHost(
                    container = container,
                    mainViewModel = mainViewModel
                )
            }
        }
    }
}
