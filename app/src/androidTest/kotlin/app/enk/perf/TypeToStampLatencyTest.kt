package app.enk.perf

import android.app.Activity
import android.content.Context
import android.media.AudioManager
import android.os.Handler
import android.os.Looper
import android.view.FrameMetrics
import android.view.Window
import androidx.activity.ComponentActivity
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performTextInput
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import app.enk.MainActivity
import app.enk.audio.Sample
import app.enk.audio.SoundEvent
import app.enk.audio.SoundPoolPlayer
import app.enk.audio.SoundSet
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.util.Collections
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

@RunWith(AndroidJUnit4::class)
class TypeToStampLatencyTest {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<MainActivity>()

    @Test
    fun typeToStampFrameMetricsAndSoundLatency() {
        val activity = composeTestRule.activity
        val window = activity.window

        val frameDurationsMs = Collections.synchronizedList(mutableListOf<Double>())
        val frameMetricsHandler = Handler(Looper.getMainLooper())

        val frameMetricsListener = Window.OnFrameMetricsAvailableListener { _, frameMetrics, _ ->
            // TOTAL_DURATION captures the entire frame duration from input to display
            val totalDurationNs = frameMetrics.getMetric(FrameMetrics.TOTAL_DURATION)
            val totalDurationMs = totalDurationNs / 1_000_000.0
            frameDurationsMs.add(totalDurationMs)
        }

        window.addOnFrameMetricsAvailableListener(frameMetricsListener, frameMetricsHandler)

        try {
            // Wait for activity to settle
            composeTestRule.waitForIdle()

            // Inject 200 key events at 8 keys per second (125 ms interval)
            val totalKeyEvents = 200
            val intervalMs = 125L

            for (i in 1..totalKeyEvents) {
                // Type a character into the writer paper
                val char = ('a' + (i % 26)).toString()
                composeTestRule.onNodeWithTag("writer_text_field").performTextInput(char)
                Thread.sleep(intervalMs)
            }

            composeTestRule.waitForIdle()

            // 1. Frame latency evaluation
            val p95 = FrameMetricsMath.percentile(frameDurationsMs, 95.0)
            val jankRate = FrameMetricsMath.jankFraction(frameDurationsMs, 16.666)

            println("=== TypeToStamp Latency Measurements ===")
            println("Collected frames: ${frameDurationsMs.size}")
            println("95th percentile frame time: ${String.format("%.2f", p95)} ms (target <= 16.0 ms)")
            println("Janky frame rate: ${String.format("%.2f", jankRate * 100)} % (target <= 1.0 %)")

            // Assertions per 1.0 plan
            assertTrue(
                "95th percentile frame time during typing must be <= 16.0 ms (was $p95 ms)",
                p95 <= 16.0
            )
            assertTrue(
                "Janky frames must be <= 1.0% (was ${jankRate * 100}%)",
                jankRate <= 0.01
            )

            // 2. Sound latency evaluation
            // SoundPool has no hardware-level playback timestamp callback; we verify play() dispatch time
            // and report AudioManager buffer estimate. True loopback latency requires hardware acoustic probe.
            val context = InstrumentationRegistry.getInstrumentation().targetContext
            val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
            val bufferSizeStr = audioManager.getProperty(AudioManager.PROPERTY_OUTPUT_FRAMES_PER_BUFFER)
            val sampleRateStr = audioManager.getProperty(AudioManager.PROPERTY_OUTPUT_SAMPLE_RATE)

            val bufferFrames = bufferSizeStr?.toIntOrNull() ?: 256
            val sampleRate = sampleRateStr?.toIntOrNull() ?: 44100
            val estimatedBufferLatencyMs = (bufferFrames.toDouble() / sampleRate) * 1000.0

            println("=== Audio Latency Diagnostics ===")
            println("Hardware output frames per buffer: $bufferFrames")
            println("Hardware sample rate: $sampleRate Hz")
            println("Estimated hardware buffer latency: ${String.format("%.2f", estimatedBufferLatencyMs)} ms")

            val soundPlayer = SoundPoolPlayer(context)
            val testSet = SoundSet(
                id = "classic",
                name = "Classic",
                keys = listOf(Sample("audio/classic/key1.ogg")),
                space = Sample("audio/classic/space.ogg"),
                enter = Sample("audio/classic/enter.ogg"),
                backspace = Sample("audio/classic/backspace.ogg"),
                bell = Sample("audio/classic/bell.ogg")
            )
            soundPlayer.prepare(testSet)
            val playDispatchStart = System.nanoTime()
            soundPlayer.play(SoundEvent.Key, testSet)
            val playDispatchDurationMs = (System.nanoTime() - playDispatchStart) / 1_000_000.0

            println("SoundPool play() dispatch return duration: ${String.format("%.3f", playDispatchDurationMs)} ms")
            soundPlayer.release()

            assertTrue(
                "soundPlayer.play() return duration must be <= 5.0 ms (was $playDispatchDurationMs ms)",
                playDispatchDurationMs <= 5.0
            )

        } finally {
            window.removeOnFrameMetricsAvailableListener(frameMetricsListener)
        }
    }
}
