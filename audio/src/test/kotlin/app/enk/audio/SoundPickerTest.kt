package app.enk.audio

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class SoundPickerTest {

    private fun createSet(keys: List<String>): SoundSet {
        return SoundSet(
            id = "test",
            name = "Test Set",
            keys = keys.map { Sample(it) },
            space = Sample("space.ogg"),
            enter = Sample("enter.ogg"),
            backspace = Sample("backspace.ogg"),
            bell = Sample("bell.ogg")
        )
    }

    @Test
    fun pickerNeverRepeats() {
        val picker = SoundPicker(seed = 12345L)
        val set = createSet(listOf("k1.ogg", "k2.ogg", "k3.ogg"))

        var previous: Sample? = null
        val counts = mutableMapOf<String, Int>()

        repeat(1000) {
            val sample = picker.nextKey(set)
            if (previous != null) {
                assertNotEquals("Consecutive samples must never be equal", previous.file, sample.file)
            }
            counts[sample.file] = (counts[sample.file] ?: 0) + 1
            previous = sample
        }

        // Verify all 3 samples were drawn roughly evenly
        assertEquals(3, counts.size)
        counts.values.forEach { count ->
            // In 1000 draws, each should be around 333 (at least 200)
            assert(count > 200) { "Distribution skewed: count=$count" }
        }
    }

    @Test
    fun pickerSingleSampleAllowed() {
        val picker = SoundPicker(seed = 99L)
        val set = createSet(listOf("only_one.ogg"))

        repeat(10) {
            val sample = picker.nextKey(set)
            assertEquals("only_one.ogg", sample.file)
        }
    }

    @Test
    fun pickerDeterministicForSeed() {
        val picker1 = SoundPicker(seed = 42L)
        val picker2 = SoundPicker(seed = 42L)
        val set = createSet(listOf("k1.ogg", "k2.ogg", "k3.ogg", "k4.ogg"))

        val draws1 = List(100) { picker1.nextKey(set).file }
        val draws2 = List(100) { picker2.nextKey(set).file }

        assertEquals(draws1, draws2)
    }

    @Test
    fun pickerThrowsOnEmptyKeys() {
        val picker = SoundPicker()
        val emptySet = createSet(emptyList())

        assertThrows(IllegalArgumentException::class.java) {
            picker.nextKey(emptySet)
        }
    }

    @Test
    fun pickerStrictAlternationWithTwoSamples() {
        val picker = SoundPicker(seed = 777L)
        val set = createSet(listOf("sampleA.ogg", "sampleB.ogg"))

        var previous: String? = null
        repeat(200) {
            val sample = picker.nextKey(set).file
            if (previous != null) {
                assertNotEquals("Consecutive draws in a 2-sample set must alternate", previous, sample)
            }
            previous = sample
        }
    }

    @Test
    fun pickerDifferentSeedsProduceDifferentSequences() {
        val set = createSet(listOf("k1.ogg", "k2.ogg", "k3.ogg", "k4.ogg", "k5.ogg"))
        val pickerA = SoundPicker(seed = 111L)
        val pickerB = SoundPicker(seed = 999999L)

        val drawsA = List(50) { pickerA.nextKey(set).file }
        val drawsB = List(50) { pickerB.nextKey(set).file }

        assertNotEquals("Different seeds must produce different sequences", drawsA, drawsB)
    }

    @Test
    fun pickerDuplicateSampleValuesInSet() {
        val picker = SoundPicker(seed = 42L)
        val set = createSet(listOf("same.ogg", "same.ogg"))

        // Should safely return "same.ogg" without throwing or infinite looping
        repeat(20) {
            val sample = picker.nextKey(set)
            assertEquals("same.ogg", sample.file)
        }
    }
}
