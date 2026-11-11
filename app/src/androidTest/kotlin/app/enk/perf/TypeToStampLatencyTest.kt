package app.enk.perf

import android.os.Build
import android.os.Handler
import android.os.Looper
import android.view.FrameMetrics
import android.view.Window
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performTextInput
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import app.enk.MainActivity
import app.enk.audio.SoundEvent
import app.enk.audio.SoundManifest
import app.enk.audio.SoundPoolPlayer
import org.junit.Assert.assertTrue
import org.junit.Assume.assumeFalse
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.util.Collections

/**
 * Types into the real writer at about eight keys a second and measures the frames drawn while
 * doing so, then times the sound player's `play()` call with the real bundled sounds.
 *
 * This is a smoke-level budget check on whatever device runs it, not an acoustic measurement:
 * true tap-to-sound latency needs a phone and a microphone (see docs/qa/1.0-acceptance.md).
 * Run it with `./gradlew :app:connectedDebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=app.enk.perf.TypeToStampLatencyTest`.
 */
@RunWith(AndroidJUnit4::class)
class TypeToStampLatencyTest {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<MainActivity>()

    private fun waitForWriter() {
        composeTestRule.waitUntil(timeoutMillis = 20_000) {
            composeTestRule.onAllNodesWithTag("writer_input").fetchSemanticsNodes().isNotEmpty()
        }
        composeTestRule.waitForIdle()
    }

    @Test
    fun typingStaysWithinTheFrameBudget() {
        waitForWriter()
        val window = composeTestRule.activity.window
        val frameMs = Collections.synchronizedList(mutableListOf<Double>())
        val listener = Window.OnFrameMetricsAvailableListener { _, metrics, _ ->
            frameMs.add(metrics.getMetric(FrameMetrics.TOTAL_DURATION) / 1_000_000.0)
        }

        // Warm up so first-use costs (font, glyph cache, sound load) are not counted.
        repeat(10) {
            composeTestRule.onNodeWithTag("writer_input").performTextInput("w")
            Thread.sleep(100)
        }
        composeTestRule.waitForIdle()

        window.addOnFrameMetricsAvailableListener(listener, Handler(Looper.getMainLooper()))
        try {
            repeat(TYPED_KEYS) { i ->
                composeTestRule.onNodeWithTag("writer_input").performTextInput(('a' + i % 26).toString())
                Thread.sleep(KEY_INTERVAL_MS)
            }
            composeTestRule.waitForIdle()
        } finally {
            window.removeOnFrameMetricsAvailableListener(listener)
        }

        val frames = synchronized(frameMs) { frameMs.toList() }
        val p95 = FrameMetricsMath.percentile(frames, 95.0)
        val jank = FrameMetricsMath.jankFraction(frames, JANK_MS)
        println("typing frames=${frames.size} p95=%.2f ms jank=%.2f %%".format(p95, jank * 100))

        // Without this a missing writer or a frozen display would pass with no frames at all.
        assertTrue("expected frames while typing, got ${frames.size}", frames.size >= MIN_FRAMES)
        // An emulator draws in software, so its frame times say nothing about the app: the numbers
        // are printed above and the budget is enforced only on a physical device.
        assumeFalse("emulator: frame budget not enforced", isEmulator())
        assertTrue("p95 frame time $p95 ms must be <= $P95_BUDGET_MS ms", p95 <= P95_BUDGET_MS)
        assertTrue("jank ${jank * 100} % must be <= 1 %", jank <= 0.01)
    }

    @Test
    fun soundPlayDispatchReturnsQuicklyWithTheRealSounds() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val manifest = context.assets.open("sounds/manifest.json").bufferedReader().use { it.readText() }
        val set = SoundManifest.parse(manifest).first()
        val player = SoundPoolPlayer(context)
        try {
            player.prepare(set)
            Thread.sleep(1_000) // let SoundPool finish decoding
            val durations = (1..30).map {
                val start = System.nanoTime()
                player.play(SoundEvent.Key, set)
                val elapsed = (System.nanoTime() - start) / 1_000_000.0
                Thread.sleep(50)
                elapsed
            }
            val p95 = FrameMetricsMath.percentile(durations, 95.0)
            println("SoundPool play() p95 dispatch = %.3f ms over ${durations.size} calls".format(p95))
            assertTrue("play() dispatch p95 $p95 ms must be <= $DISPATCH_BUDGET_MS ms", p95 <= DISPATCH_BUDGET_MS)
        } finally {
            player.release()
        }
    }

    private fun isEmulator(): Boolean =
        Build.HARDWARE.contains("ranchu") || Build.HARDWARE.contains("goldfish") ||
            Build.FINGERPRINT.startsWith("generic") || Build.PRODUCT.contains("sdk")

    private companion object {
        const val TYPED_KEYS = 120
        const val KEY_INTERVAL_MS = 125L
        const val MIN_FRAMES = 60
        const val JANK_MS = 16.666
        const val P95_BUDGET_MS = 16.0
        const val DISPATCH_BUDGET_MS = 5.0
    }
}
