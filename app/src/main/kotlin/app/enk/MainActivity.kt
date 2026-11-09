package app.enk

import android.content.res.Configuration
import android.os.Bundle
import android.os.SystemClock
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.platform.LocalConfiguration
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import app.enk.design.theme.EnkShellTheme
import app.enk.settings.Settings
import app.enk.shell.EnkNavHost
import app.enk.start.SplashHost
import app.enk.start.StartGate
import app.enk.start.StartTargets
import app.enk.text.GridLayout
import app.enk.writer.WriterGeometry
import kotlinx.coroutines.delay

class MainActivity : ComponentActivity() {
    private val mainViewModel: MainViewModel by viewModels {
        MainViewModel.factory(applicationContext)
    }

    private var coldStart = false
    private val startTimeMs = SystemClock.uptimeMillis()
    private var loadDurationMs: Long? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        val splashScreen = installSplashScreen()
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        coldStart = savedInstanceState == null
        val launchedFromWidgetOrIntent = intent?.action != null && intent.action != "android.intent.action.MAIN"
        val restoredState = savedInstanceState != null

        // Install exit animation listener on splash screen
        splashScreen.setOnExitAnimationListener { splashScreenView ->
            splashScreenView.remove()
        }

        splashScreen.setKeepOnScreenCondition {
            val elapsed = SystemClock.uptimeMillis() - startTimeMs
            val loaded = mainViewModel.isLoaded.value
            if (loaded && loadDurationMs == null) {
                loadDurationMs = elapsed
            } else if (elapsed >= 1500L && loadDurationMs == null) {
                loadDurationMs = elapsed
            }
            !loaded && elapsed < 1500L
        }

        setContent {
            val isLoaded by mainViewModel.isLoaded.collectAsState()
            val restoredNote by mainViewModel.restoredNote.collectAsState()
            val container = mainViewModel.container
            val settings by container.settingsStore.settings.collectAsState(initial = Settings())

            var timedOut by remember { mutableStateOf(false) }
            LaunchedEffect(Unit) {
                delay(1500L)
                timedOut = true
            }

            if (!isLoaded && !timedOut) {
                return@setContent
            }

            val elapsed = SystemClock.uptimeMillis() - startTimeMs
            if (loadDurationMs == null) {
                loadDurationMs = elapsed
            }
            val loadMs = loadDurationMs ?: elapsed

            val shouldPlay = remember {
                StartGate.shouldPlay(
                    coldStart = coldStart,
                    launchedFromWidgetOrIntent = launchedFromWidgetOrIntent,
                    restoredState = restoredState,
                    loadMs = loadMs
                )
            }

            val configuration = LocalConfiguration.current
            val screenWidthDp = configuration.screenWidthDp.toFloat()
            val screenHeightDp = configuration.screenHeightDp.toFloat()
            val isNight = (configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES
            val lightSplash = !isNight

            val targets = remember(screenWidthDp, screenHeightDp, lightSplash, restoredNote) {
                val geometry = WriterGeometry.compute(screenWidthDp, screenHeightDp, 1.0f)
                val firstPageText = restoredNote?.pages?.firstOrNull()?.text ?: ""
                val lines = GridLayout.wrap(firstPageText, geometry.cols)
                val caretPos = GridLayout.caret(lines, firstPageText.length, geometry.cols)

                val caretLeft = geometry.textLeftDp + caretPos.col * geometry.columnWidthDp
                val caretTop = geometry.railYDp - 23f - geometry.lineHeightDp + 1.42f * geometry.fontSp
                val caretRect = Rect(
                    left = caretLeft,
                    top = caretTop,
                    right = caretLeft + geometry.columnWidthDp,
                    bottom = caretTop + 2f
                )
                StartTargets(
                    railY = geometry.railYDp,
                    screenWidth = screenWidthDp,
                    screenHeight = screenHeightDp,
                    carriageX = geometry.carriageCenterXDp(caretPos.col),
                    caretRect = caretRect,
                    paperHeight = (screenHeightDp - WriterGeometry.PAPER_TOP_DP - WriterGeometry.PAPER_SIDE_DP).coerceAtLeast(100f),
                    lightSplash = lightSplash
                )
            }

            EnkShellTheme(theme = settings.theme, mode = settings.themeMode) {
                SplashHost(
                    targets = targets,
                    shouldPlay = shouldPlay,
                    reduceMotion = settings.reduceEffects
                ) {
                    EnkNavHost(
                        container = container,
                        mainViewModel = mainViewModel
                    )
                }
            }
        }
    }
}
