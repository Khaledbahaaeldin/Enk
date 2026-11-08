package app.enk.data

import org.junit.Assert.assertEquals
import org.junit.Test

class StruckCodecTest {

    @Test
    fun roundTripMultipleRanges() {
        val ranges = listOf(1..2, 7..7, 10..15)
        val encoded = StruckCodec.encode(ranges)
        assertEquals("1-2,7-7,10-15", encoded)
        val decoded = StruckCodec.decode(encoded)
        assertEquals(ranges, decoded)
    }

    @Test
    fun roundTripSingleRange() {
        val ranges = listOf(3..3)
        val encoded = StruckCodec.encode(ranges)
        assertEquals("3-3", encoded)
        val decoded = StruckCodec.decode(encoded)
        assertEquals(ranges, decoded)
    }

    @Test
    fun emptyRangesEncodeToEmptyString() {
        assertEquals("", StruckCodec.encode(emptyList()))
        assertEquals(emptyList<IntRange>(), StruckCodec.decode(""))
        assertEquals(emptyList<IntRange>(), StruckCodec.decode(null))
        assertEquals(emptyList<IntRange>(), StruckCodec.decode("   "))
    }

    @Test
    fun malformedInputsHandledGracefully() {
        assertEquals(emptyList<IntRange>(), StruckCodec.decode("abc"))
        assertEquals(emptyList<IntRange>(), StruckCodec.decode("5-2")) // backwards range ignored
        assertEquals(emptyList<IntRange>(), StruckCodec.decode("1-"))
        assertEquals(emptyList<IntRange>(), StruckCodec.decode("-2"))
        assertEquals(emptyList<IntRange>(), StruckCodec.decode("1-2-3"))

        // Ignores bad entries while preserving valid ones
        val mixed = "1-2,,foo-bar,7-7,9-4,10-12"
        assertEquals(listOf(1..2, 7..7, 10..12), StruckCodec.decode(mixed))
    }
}
