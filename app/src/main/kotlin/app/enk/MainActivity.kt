package app.enk

import android.content.Intent
import android.content.res.Configuration
import android.os.Bundle
import android.os.Handler
import android.os.HandlerThread
import android.os.SystemClock
import android.view.FrameMetrics
import android.view.Window
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.core.splashscreen.SplashScreenViewProvider
import androidx.lifecycle.Lifecycle
import app.enk.design.effects.EffectsLevel
import app.enk.design.effects.FrameGovernor
import app.enk.design.effects.LocalEffects
import app.enk.design.effects.ResolvedEffects
import app.enk.design.effects.resolveEffects
import app.enk.design.shell.LocalReduceEffects
import app.enk.design.theme.EnkShellTheme
import app.enk.settings.rememberEffectsEnvironment
import app.enk.shell.EnkNavHost
import app.enk.start.SplashHold
import app.enk.start.SplashHost
import app.enk.start.StartGate
import app.enk.widget.EnkWidgetManager
import app.enk.widget.WidgetIntentAction
import app.enk.widget.WidgetIntentParser
import app.enk.writer.rememberReduceMotion
import kotlinx.coroutines.flow.MutableStateFlow

class MainActivity : ComponentActivity() {
    private val mainViewModel: MainViewModel by viewModels {
        MainViewModel.factory(applicationContext)
    }

    /** Null until the system splash has handed over; then whether the start animation plays. */
    private var playStartAnimation by mutableStateOf<Boolean?>(null)
    private var systemSplash: SplashScreenViewProvider? = null
    private val releaseSplashRunnable = Runnable { releaseSystemSplash() }

    private val governor = FrameGovernor()
    private val slowFramesFlow = MutableStateFlow<ResolvedEffects?>(null)
    private var frameThread: HandlerThread? = null
    private var frameHandler: Handler? = null
    private var frameMetricsListener: Window.OnFrameMetricsAvailableListener? = null
    private var framesToSkip = 60
    // Written on the main thread, read on the frame-metrics thread.
    @Volatile private var currentEffectsChoice: EffectsLevel = EffectsLevel.Auto
    @Volatile private var currentResolved: ResolvedEffects = ResolvedEffects.Full

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
            val slowFrames by slowFramesFlow.collectAsState()

            val env = rememberEffectsEnvironment(slowFrames)
            val resolved = resolveEffects(settings.effects, env)
            SideEffect {
                currentResolved = resolved
                // Choosing Auto again while the app is in front must start the governor now, and
                // choosing a fixed level must stop it, not wait for the next resume.
                if (currentEffectsChoice != settings.effects) {
                    currentEffectsChoice = settings.effects
                    if (lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)) updateFrameMetricsListener()
                }
            }
            val isLight = resolved == ResolvedEffects.Light

            CompositionLocalProvider(
                LocalEffects provides resolved,
                LocalReduceEffects provides isLight
            ) {
                EnkShellTheme(theme = settings.theme, mode = settings.themeMode) {
                    SplashHost(
                        firstPageText = restoredNote?.pages?.firstOrNull()?.text.orEmpty(),
                        lightSplash = lightSplash,
                        shouldPlay = play,
                        reduceMotion = rememberReduceMotion(isLight),
                        reservedBottomDp = app.enk.keyboard.enkKeyboardReservedDp(
                            settings.keyboard,
                            androidx.compose.ui.platform.LocalConfiguration.current.screenHeightDp.toFloat()
                        ),
                        onFirstFrame = ::releaseSystemSplash
                    ) { intro ->
                        EnkNavHost(container = container, mainViewModel = mainViewModel, intro = intro)
                    }
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        framesToSkip = 60
        updateFrameMetricsListener()
    }

    override fun onPause() {
        detachFrameMetricsListener()
        super.onPause()
    }

    private fun updateFrameMetricsListener() {
        if (currentEffectsChoice == EffectsLevel.Auto) {
            if (frameMetricsListener == null) {
                if (frameThread == null) {
                    val thread = HandlerThread("enk-frames").apply { start() }
                    frameThread = thread
                    frameHandler = Handler(thread.looper)
                }
                val listener = Window.OnFrameMetricsAvailableListener { _, frameMetrics, _ ->
                    if (framesToSkip > 0) {
                        framesToSkip--
                        return@OnFrameMetricsAvailableListener
                    }
                    val totalNs = frameMetrics.getMetric(FrameMetrics.TOTAL_DURATION)
                    val totalMs = totalNs / 1_000_000f
                    val demotion = governor.onFrame(totalMs, currentResolved)
                    if (demotion != null) {
                        slowFramesFlow.value = demotion
                    }
                }
                frameMetricsListener = listener
                window.addOnFrameMetricsAvailableListener(listener, frameHandler)
            }
        } else {
            detachFrameMetricsListener()
        }
    }

    private fun detachFrameMetricsListener() {
        frameMetricsListener?.let {
            try {
                window.removeOnFrameMetricsAvailableListener(it)
            } catch (_: Exception) {}
            frameMetricsListener = null
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
        detachFrameMetricsListener()
        frameThread?.quitSafely()
        frameThread = null
        frameHandler = null
        super.onDestroy()
    }

    private companion object {
        const val SPLASH_RELEASE_FALLBACK_MS = 300L

        /** Survives activity recreation, dies with the process: what "cold start" means. */
        var processHasLaunched = false
    }
}
