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

    @Test
    fun whitespaceWithinTokensAndSurroundingCommas() {
        // Spaces around tokens and extra commas
        val input = "  1-2 ,,, 7-7  ,  10-15  "
        assertEquals(listOf(1..2, 7..7, 10..15), StruckCodec.decode(input))
    }

    @Test
    fun negativeNumbersAndReversedRanges() {
        assertEquals(emptyList<IntRange>(), StruckCodec.decode("-1-2"))
        assertEquals(emptyList<IntRange>(), StruckCodec.decode("1--2"))
        assertEquals(emptyList<IntRange>(), StruckCodec.decode("-5--2"))
        assertEquals(emptyList<IntRange>(), StruckCodec.decode("10-5"))
    }

    @Test
    fun overlappingRangesArePreserved() {
        // StruckCodec preserves ranges as-is without merging (merging is StruckPage responsibility)
        val input = "1-5,3-7,0-2"
        assertEquals(listOf(1..5, 3..7, 0..2), StruckCodec.decode(input))
    }

    @Test
    fun singlePointRanges() {
        assertEquals(listOf(0..0), StruckCodec.decode("0-0"))
        assertEquals(listOf(999..999), StruckCodec.decode("999-999"))
        assertEquals("0-0", StruckCodec.encode(listOf(0..0)))
    }
}
