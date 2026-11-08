package app.enk.audio

import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

class TickSynthTest {

    @Test
    fun tickIs20msNoisePlus30msThump() {
        val sampleRate = 44100
        val rendered = TickSynth.render(position = 50, sampleRate = sampleRate)

        // 30 ms at 44.1 kHz = 1323 samples
        val expected30ms = Math.round(sampleRate * 0.03).toInt()
        assertEquals(expected30ms, rendered.size)

        // Noise burst lasts 20 ms = 882 samples
        val expected20ms = Math.round(sampleRate * 0.02).toInt()
        assertEquals(882, expected20ms)

        // The first 882 samples have energy from both noise and thump
        val firstHalfEnergy = rendered.take(expected20ms).map { abs(it.toInt()) }.sum()
        assertTrue("First 20 ms must carry sound energy", firstHalfEnergy > 0)

        // Peak amplitude is strictly within Short bounds without clipping
        var maxAmp = 0
        for (s in rendered) {
            val a = abs(s.toInt())
            if (a > maxAmp) maxAmp = a
        }
        assertTrue("Must not clip: maxAmp=$maxAmp", maxAmp <= Short.MAX_VALUE)
        assertTrue("Must have audible amplitude", maxAmp > 1000)
    }

    @Test
    fun tickPitchRisesWithPosition() {
        val sampleRate = 44100
        val lowTick = TickSynth.render(position = 0, sampleRate = sampleRate)
        val highTick = TickSynth.render(position = 100, sampleRate = sampleRate)

        val centroidLow = computeSpectralCentroid(lowTick, sampleRate)
        val centroidHigh = computeSpectralCentroid(highTick, sampleRate)

        assertTrue(
            "Spectral centroid at position 100 ($centroidHigh Hz) must be strictly higher than at position 0 ($centroidLow Hz)",
            centroidHigh > centroidLow
        )
    }

    @Test
    fun clampingPosition() {
        val neg10 = TickSynth.render(position = -10)
        val pos0 = TickSynth.render(position = 0)
        assertArrayEquals("position -10 must clamp to 0", pos0, neg10)

        val pos150 = TickSynth.render(position = 150)
        val pos100 = TickSynth.render(position = 100)
        assertArrayEquals("position 150 must clamp to 100", pos100, pos150)
    }

    @Test
    fun deterministicForGivenPosition() {
        val run1 = TickSynth.render(position = 42)
        val run2 = TickSynth.render(position = 42)
        assertArrayEquals("Same position must produce deterministic output", run1, run2)
    }

    @Test
    fun sampleRate48000CommonOnAndroid() {
        val sampleRate = 48000
        val rendered = TickSynth.render(position = 50, sampleRate = sampleRate)

        // 30 ms at 48 kHz = 1440 samples
        val expected30ms = Math.round(sampleRate * 0.03).toInt()
        assertEquals(1440, expected30ms)
        assertEquals(1440, rendered.size)

        // Noise burst lasts 20 ms = 960 samples
        val expected20ms = Math.round(sampleRate * 0.02).toInt()
        assertEquals(960, expected20ms)

        val firstPartEnergy = rendered.take(expected20ms).map { abs(it.toInt()) }.sum()
        assertTrue("First 20 ms must carry sound energy at 48kHz", firstPartEnergy > 0)

        var maxAmp = 0
        for (s in rendered) {
            val a = abs(s.toInt())
            if (a > maxAmp) maxAmp = a
        }
        assertTrue("Must not clip at 48kHz: maxAmp=$maxAmp", maxAmp <= Short.MAX_VALUE)
        assertTrue("Must have audible amplitude at 48kHz", maxAmp > 1000)
    }

    @Test
    fun extremePositionsClampingAndNoClipping() {
        val positions = listOf(-1000, -100, 0, 50, 100, 200, 1000)

        for (pos in positions) {
            val rendered = TickSynth.render(position = pos)
            assertEquals(1323, rendered.size)

            var maxAmp = 0
            for (s in rendered) {
                val a = abs(s.toInt())
                if (a > maxAmp) maxAmp = a
            }
            assertTrue("Position $pos must not clip: maxAmp=$maxAmp", maxAmp <= Short.MAX_VALUE)
            assertTrue("Position $pos must have audible amplitude", maxAmp > 1000)
        }

        assertArrayEquals("pos -1000 must match pos 0", TickSynth.render(0), TickSynth.render(-1000))
        assertArrayEquals("pos 1000 must match pos 100", TickSynth.render(100), TickSynth.render(1000))
    }

    private fun computeSpectralCentroid(samples: ShortArray, sampleRate: Int): Double {
        val n = samples.size
        val numBins = 64
        var weightedSum = 0.0
        var totalMag = 0.0

        for (bin in 1 until numBins) {
            val freq = bin * (sampleRate / 2.0) / numBins
            val omega = 2.0 * Math.PI * freq / sampleRate
            var real = 0.0
            var imag = 0.0
            for (i in 0 until n) {
                val v = samples[i].toDouble()
                real += v * cos(omega * i)
                imag -= v * sin(omega * i)
            }
            val mag = sqrt(real * real + imag * imag)
            weightedSum += freq * mag
            totalMag += mag
        }

        return if (totalMag > 0.0) weightedSum / totalMag else 0.0
    }
}
