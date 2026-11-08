package app.enk.audio

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import android.media.SoundPool
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.random.Random

interface SoundPlayer {
    fun play(event: SoundEvent, set: SoundSet, volume: Float = 1f)
    fun release()
}

object TickSynth {
    fun render(position: Int, sampleRate: Int = 44100): ShortArray {
        val clampedPos = position.coerceIn(0, 100)
        val totalSamples = Math.round(sampleRate * 0.03).toInt() // 30 ms
        val noiseSamples = Math.round(sampleRate * 0.02).toInt() // 20 ms

        val output = ShortArray(totalSamples)

        // Center frequency for bandpass filter: f0 = 2200 + position * 18
        val f0 = 2200.0 + clampedPos * 18.0
        val q = 2.5

        // Biquad bandpass filter coefficients (constant 0 dB peak gain)
        val w0 = 2.0 * Math.PI * f0 / sampleRate
        val alpha = Math.sin(w0) / (2.0 * q)
        val cosW0 = Math.cos(w0)

        val b0 = alpha
        val b1 = 0.0
        val b2 = -alpha
        val a0 = 1.0 + alpha
        val a1 = -2.0 * cosW0
        val a2 = 1.0 - alpha

        // Normalized filter coefficients
        val nb0 = b0 / a0
        val nb1 = b1 / a0
        val nb2 = b2 / a0
        val na1 = a1 / a0
        val na2 = a2 / a0

        // Deterministic noise generator with fixed seed
        val noiseRng = java.util.Random(1337L)

        var x1 = 0.0
        var x2 = 0.0
        var y1 = 0.0
        var y2 = 0.0

        val thumpFreq = 140.0

        for (i in 0 until totalSamples) {
            val t = i.toDouble() / sampleRate

            // 1. Noise burst (lasts 20 ms, then 0)
            val noiseInput = if (i < noiseSamples) {
                val rawNoise = noiseRng.nextDouble() * 2.0 - 1.0
                val decay = (1.0 - t / 0.020).coerceAtLeast(0.0)
                rawNoise * decay
            } else {
                0.0
            }

            // Biquad filter difference equation
            val filteredNoise = nb0 * noiseInput + nb1 * x1 + nb2 * x2 - na1 * y1 - na2 * y2
            x2 = x1
            x1 = noiseInput
            y2 = y1
            y1 = filteredNoise

            // 2. 140 Hz sine thump decaying over 30 ms
            val thumpDecay = (1.0 - t / 0.030).coerceAtLeast(0.0)
            val thump = Math.sin(2.0 * Math.PI * thumpFreq * t) * thumpDecay

            // Mixed output: combined noise burst and thump
            val mixed = 0.65 * filteredNoise + 0.35 * thump
            val sampleVal = (mixed * 24000.0).coerceIn(Short.MIN_VALUE.toDouble(), Short.MAX_VALUE.toDouble())
            output[i] = sampleVal.toInt().toShort()
        }

        return output
    }
}

class SoundPoolPlayer(private val context: Context) : SoundPlayer {

    private val isReleased = AtomicBoolean(false)
    private val picker = SoundPicker()

    // Created on first use and never created just to be released.
    private var pool: SoundPool? = null

    @Synchronized
    private fun soundPool(): SoundPool? {
        if (isReleased.get()) return null
        return pool ?: SoundPool.Builder()
            .setMaxStreams(8)
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_ASSISTANCE_SONIFICATION)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build()
            )
            .build()
            .also { pool = it }
    }

    private val loadedSampleIds = java.util.concurrent.ConcurrentHashMap<String, Int>()

    /**
     * Starts loading every sample of [set]. SoundPool decodes asynchronously, so call this when the
     * set is chosen (not on the first keystroke); a sample that has not finished loading is silent.
     */
    fun prepare(set: SoundSet) {
        if (isReleased.get()) return
        val allSamples = set.keys + listOf(set.space, set.enter, set.backspace, set.bell)
        for (sample in allSamples) {
            loadSample(sample.file)
        }
    }

    private fun loadSample(file: String): Int {
        if (isReleased.get()) return 0
        val existing = loadedSampleIds[file]
        if (existing != null) return existing

        val sp = soundPool() ?: return 0
        return try {
            val id = context.assets.openFd(file).use { sp.load(it, 1) }
            loadedSampleIds[file] = id
            id
        } catch (_: Exception) {
            0
        }
    }

    fun playTick(position: Int, volume: Float = 1f) {
        if (isReleased.get() || volume <= 0f) return
        var track: AudioTrack? = null
        try {
            val pcm = TickSynth.render(position)
            val audioTrack = AudioTrack.Builder()
                .setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_ASSISTANCE_SONIFICATION)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .build()
                )
                .setAudioFormat(
                    AudioFormat.Builder()
                        .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                        .setSampleRate(44100)
                        .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                        .build()
                )
                .setBufferSizeInBytes(pcm.size * 2)
                .setTransferMode(AudioTrack.MODE_STATIC)
                .build()
            track = audioTrack

            audioTrack.setVolume(volume.coerceIn(0f, 1f))
            audioTrack.write(pcm, 0, pcm.size)
            // A static track must be released after it has played, or every tick leaks a native track.
            audioTrack.setPlaybackPositionUpdateListener(object : AudioTrack.OnPlaybackPositionUpdateListener {
                override fun onMarkerReached(track: AudioTrack) = track.release()
                override fun onPeriodicNotification(track: AudioTrack) = Unit
            })
            audioTrack.setNotificationMarkerPosition(pcm.size)
            audioTrack.play()
        } catch (_: Exception) {
            // AudioTrack unavailable or headless environment
            try { track?.release() } catch (_: Exception) {}
        }
    }

    override fun play(event: SoundEvent, set: SoundSet, volume: Float) {
        if (isReleased.get() || volume <= 0f) return

        if (event == SoundEvent.FaderTick) {
            // The position-aware call is playTick(position); this generic event plays a mid-range tick.
            playTick(position = 50, volume = volume)
            return
        }

        val sample = when (event) {
            SoundEvent.Key -> picker.nextKey(set)
            SoundEvent.Space, SoundEvent.LampClick -> set.space
            SoundEvent.Enter -> set.enter
            SoundEvent.Backspace -> set.backspace
            SoundEvent.Bell -> set.bell
            SoundEvent.FaderTick -> return
        }

        val sampleId = loadSample(sample.file)
        if (sampleId == 0) return

        // Rate variation +/-3% (0.97f .. 1.03f) (DS 10)
        val rate = 1f + (Random.nextFloat() * 0.06f - 0.03f)
        val effectiveVolume = (volume * sample.gain).coerceIn(0f, 1f)

        try {
            soundPool()?.play(sampleId, effectiveVolume, effectiveVolume, 1, 0, rate)
        } catch (_: Exception) {
            // Ignored in headless/test environments
        }
    }

    override fun release() {
        if (isReleased.compareAndSet(false, true)) {
            synchronized(this) {
                try {
                    pool?.release()
                } catch (_: Exception) {
                }
                pool = null
            }
            loadedSampleIds.clear()
        }
    }
}
