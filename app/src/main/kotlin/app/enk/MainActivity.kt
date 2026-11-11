package app.enk

import android.content.Intent
import android.content.res.Configuration
import android.os.Bundle
import android.os.SystemClock
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.core.splashscreen.SplashScreenViewProvider
import app.enk.design.theme.EnkShellTheme
import app.enk.shell.EnkNavHost
import app.enk.start.SplashHold
import app.enk.start.SplashHost
import app.enk.start.StartGate
import app.enk.widget.EnkWidgetManager
import app.enk.widget.WidgetIntentAction
import app.enk.widget.WidgetIntentParser
import app.enk.writer.rememberReduceMotion

class MainActivity : ComponentActivity() {
    private val mainViewModel: MainViewModel by viewModels {
        MainViewModel.factory(applicationContext)
    }

    /** Null until the system splash has handed over; then whether the start animation plays. */
    private var playStartAnimation by mutableStateOf<Boolean?>(null)
    private var systemSplash: SplashScreenViewProvider? = null
    private val releaseSplashRunnable = Runnable { releaseSystemSplash() }

    override fun onCreate(savedInstanceState: Bundle?) {
        val splashScreen = installSplashScreen()
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        // A cold start is the first activity of this process; rotation and re-opening are not.
        val coldStart = !processHasLaunched
        processHasLaunched = true
        val restoredState = savedInstanceState != null
        val launchedByIntent = intent?.action != null && intent.action != Intent.ACTION_MAIN
        if (!restoredState) handleWidgetIntent(intent)
        // The system splash follows the launch configuration, not the app's Mode setting.
        val lightSplash = (resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) !=
            Configuration.UI_MODE_NIGHT_YES

        val hold = SplashHold(startMs = SystemClock.uptimeMillis())
        splashScreen.setKeepOnScreenCondition {
            hold.shouldHold(mainViewModel.isLoaded.value, SystemClock.uptimeMillis())
        }
        // Without a system splash (rotation, a running process) there is nothing to hand over from.
        if (restoredState || !coldStart) playStartAnimation = false
        splashScreen.setOnExitAnimationListener { splash ->
            if (playStartAnimation == null) {
                playStartAnimation = StartGate.shouldPlay(
                    coldStart = coldStart,
                    launchedFromWidgetOrIntent = launchedByIntent,
                    restoredState = restoredState,
                    loadMs = hold.loadMs ?: SplashHold.CAP_MS
                )
            }
            systemSplash = splash
            // The first composed frame normally releases it; this is the safety net.
            window.decorView.postDelayed(releaseSplashRunnable, SPLASH_RELEASE_FALLBACK_MS)
        }

        setContent {
            val play = playStartAnimation ?: return@setContent
            val container = mainViewModel.container
            val settings by container.settingsStore.settings.collectAsState(initial = mainViewModel.initialSettings)
            val restoredNote by mainViewModel.restoredNote.collectAsState()

            EnkShellTheme(theme = settings.theme, mode = settings.themeMode) {
                SplashHost(
                    firstPageText = restoredNote?.pages?.firstOrNull()?.text.orEmpty(),
                    lightSplash = lightSplash,
                    shouldPlay = play,
                    reduceMotion = rememberReduceMotion(settings.reduceEffects),
                    onFirstFrame = ::releaseSystemSplash
                ) { intro ->
                    EnkNavHost(container = container, mainViewModel = mainViewModel, intro = intro)
                }
            }
        }
    }

    private fun releaseSystemSplash() {
        systemSplash?.remove()
        systemSplash = null
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleWidgetIntent(intent)
    }

    /** Acts on a widget intent once: consuming marks it handled, so it is never replayed. */
    private fun handleWidgetIntent(intent: Intent?) {
        val action = WidgetIntentParser.consume(intent)
        if (action !is WidgetIntentAction.None) mainViewModel.handleWidgetAction(action)
    }

    override fun onStop() {
        super.onStop()
        EnkWidgetManager.requestUpdate(applicationContext)
    }

    override fun onDestroy() {
        window.decorView.removeCallbacks(releaseSplashRunnable)
        super.onDestroy()
    }

    private companion object {
        const val SPLASH_RELEASE_FALLBACK_MS = 300L

        /** Survives activity recreation, dies with the process: what "cold start" means. */
        var processHasLaunched = false
    }
}
