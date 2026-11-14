package app.enk.writer

import app.enk.settings.CorrectionMode
import app.enk.text.StruckPage
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class WriterReducerTest {

    private fun initialState(
        text: String = "",
        pageIndex: Int = 0,
        caret: Int = text.length,
        lampOn: Boolean = false,
        pages: List<StruckPage>? = null,
        composingRange: IntRange? = null
    ): WriterState {
        val pageList = pages ?: listOf(StruckPage(text))
        return WriterState(
            noteId = "test-note",
            pages = pageList,
            pageIndex = pageIndex,
            caret = caret,
            lampOn = lampOn,
            composingRange = composingRange
        )
    }

    @Test
    fun typingStampsOnlyNewestChar() {
        val s0 = initialState("")
        val s1 = reduce(s0, WriterEvent.Insert("a"), CorrectionMode.CleanErase)
        assertEquals("Typing 'a' stamps index 0", 0, s1.lastTypedIndex)
        assertEquals(1, s1.caret)
        assertEquals("a", s1.pages[0].text)

        val s2 = reduce(s1, WriterEvent.Insert("b"), CorrectionMode.CleanErase)
        assertEquals("Typing 'b' stamps index 1", 1, s2.lastTypedIndex)
        assertEquals(2, s2.caret)
        assertEquals("ab", s2.pages[0].text)
    }

    @Test
    fun pasteHasNoStamp() {
        val s0 = initialState("")
        val s1 = reduce(s0, WriterEvent.Insert("hello world"), CorrectionMode.CleanErase)
        assertNull("Pasting multi-character text must leave lastTypedIndex null", s1.lastTypedIndex)
        assertEquals(11, s1.caret)
        assertEquals("hello world", s1.pages[0].text)
    }

    @Test
    fun composingDoesNotCommitTwice() {
        val s0 = initialState("")
        val s1 = reduce(s0, WriterEvent.Insert("hel", composing = true), CorrectionMode.CleanErase)
        assertEquals("hel", s1.pages[0].text)
        assertEquals(0..2, s1.composingRange)

        val s2 = reduce(s1, WriterEvent.Insert("hello", composing = true), CorrectionMode.CleanErase)
        assertEquals("hello", s2.pages[0].text)
        assertEquals(0..4, s2.composingRange)

        val s3 = reduce(s2, WriterEvent.Insert("hello", composing = false), CorrectionMode.CleanErase)
        assertEquals("hello", s3.pages[0].text)
        assertNull("Composing range must be cleared when non-composing commit occurs", s3.composingRange)
    }

    @Test
    fun overflowStartsNewPage() {
        val fullText = "a".repeat(3000)
        val s0 = initialState(fullText)
        val s1 = reduce(s0, WriterEvent.Insert("x "), CorrectionMode.CleanErase)

        assertEquals("Overflow must split into 2 pages", 2, s1.pages.size)
        assertEquals("Caret must advance to page 1", 1, s1.pageIndex)
        assertTrue("Caret position on page 1 must be positive", s1.caret > 0)
    }

    @Test(timeout = 10000)
    fun hugePasteSplitsIntoPages() {
        val s0 = initialState("")

        // Unbroken text
        val unbroken = "a".repeat(200_000)
        val s1 = reduce(s0, WriterEvent.Insert(unbroken), CorrectionMode.CleanErase)
        assertTrue("Page count must be in 67..68: actual=${s1.pages.size}", s1.pages.size in 67..68)
        assertEquals(unbroken, s1.pages.joinToString("") { it.text })
        s1.pages.forEach { page ->
            assertTrue("Every page length must be <= 3000", page.text.length <= 3000)
        }

        // Text with spaces
        val withSpaces = "word ".repeat(40_000) // 200,000 chars
        val s2 = reduce(s0, WriterEvent.Insert(withSpaces), CorrectionMode.CleanErase)
        assertTrue("Page count with spaces in 67..68: actual=${s2.pages.size}", s2.pages.size in 67..68)
        assertEquals(withSpaces, s2.pages.joinToString("") { it.text })
        s2.pages.forEach { page ->
            assertTrue("Every page length must be <= 3000", page.text.length <= 3000)
        }
    }

    @Test
    fun backspaceCleanErase() {
        val s0 = initialState("ab", caret = 2)
        val s1 = reduce(s0, WriterEvent.Backspace, CorrectionMode.CleanErase)

        assertEquals("a", s1.pages[0].text)
        assertEquals(1, s1.caret)
    }

    @Test
    fun backspaceStrikeKeepsText() {
        val s0 = initialState("ab", caret = 2)
        val s1 = reduce(s0, WriterEvent.Backspace, CorrectionMode.StrikeThrough)

        assertEquals("Text must remain unchanged in StrikeThrough", "ab", s1.pages[0].text)
        assertEquals("Caret must step backward by 1", 1, s1.caret)
        assertEquals("Struck range must mark the erased character", listOf(1..1), s1.pages[0].struck)
    }

    @Test
    fun backspaceAtPageStartMovesToPreviousPage() {
        val s0 = initialState(
            pages = listOf(StruckPage("Hello"), StruckPage("World")),
            pageIndex = 1,
            caret = 0
        )
        val s1 = reduce(s0, WriterEvent.Backspace, CorrectionMode.CleanErase)

        assertEquals(0, s1.pageIndex)
        assertEquals(5, s1.caret)

        // Empty page is removed when moving back from caret 0
        val sEmpty = initialState(
            pages = listOf(StruckPage("Hello"), StruckPage("")),
            pageIndex = 1,
            caret = 0
        )
        val sEmptyRes = reduce(sEmpty, WriterEvent.Backspace, CorrectionMode.CleanErase)
        assertEquals(1, sEmptyRes.pages.size)
        assertEquals(0, sEmptyRes.pageIndex)
        assertEquals(5, sEmptyRes.caret)

        // On page 0 at caret 0 is a no-op
        val sZero = initialState("", pageIndex = 0, caret = 0)
        val sZeroRes = reduce(sZero, WriterEvent.Backspace, CorrectionMode.CleanErase)
        assertEquals(0, sZeroRes.pageIndex)
        assertEquals(0, sZeroRes.caret)
    }

    @Test
    fun moveCaretClamps() {
        val s0 = initialState("hello", caret = 2)
        val sHigh = reduce(s0, WriterEvent.MoveCaret(9999), CorrectionMode.CleanErase)
        assertEquals(5, sHigh.caret)

        val sLow = reduce(s0, WriterEvent.MoveCaret(-10), CorrectionMode.CleanErase)
        assertEquals(0, sLow.caret)
    }

    @Test
    fun goToPageClamps() {
        val s0 = initialState(
            pages = listOf(StruckPage("Hello"), StruckPage("World")),
            pageIndex = 0,
            caret = 2
        )
        val sHigh = reduce(s0, WriterEvent.GoToPage(99), CorrectionMode.CleanErase)
        assertEquals(1, sHigh.pageIndex)

        val sLow = reduce(s0, WriterEvent.GoToPage(-5), CorrectionMode.CleanErase)
        assertEquals(0, sLow.pageIndex)
    }

    @Test
    fun toggleLamp() {
        val s0 = initialState(lampOn = false)
        val s1 = reduce(s0, WriterEvent.ToggleLamp, CorrectionMode.CleanErase)
        assertTrue(s1.lampOn)

        val s2 = reduce(s1, WriterEvent.ToggleLamp, CorrectionMode.CleanErase)
        assertFalse(s2.lampOn)
    }

    @Test
    fun surrogatePairHandling() {
        // "Hi 😀" consists of 'H', 'i', ' ', and 2 Char surrogate pair (length 5)
        val s0 = initialState("Hi 😀", caret = 5)
        val s1 = reduce(s0, WriterEvent.Backspace, CorrectionMode.CleanErase)

        assertEquals("Backspace must remove both surrogate characters", "Hi ", s1.pages[0].text)
        assertEquals(3, s1.caret)

        val sStrike = initialState("Hi 😀", caret = 5)
        val sStrikeRes = reduce(sStrike, WriterEvent.Backspace, CorrectionMode.StrikeThrough)
        assertEquals("Hi 😀", sStrikeRes.pages[0].text)
        assertEquals(3, sStrikeRes.caret)
        assertEquals(listOf(3..4), sStrikeRes.pages[0].struck)
    }

    @Test
    fun struckRangesShiftOnInsert() {
        val initialPage = StruckPage(text = "ab", struck = listOf(1..1))
        val s0 = initialState(pages = listOf(initialPage), caret = 0)

        val s1 = reduce(s0, WriterEvent.Insert("XYZ"), CorrectionMode.CleanErase)
        assertEquals("XYZab", s1.pages[0].text)
        assertEquals(3, s1.caret)
        assertEquals(listOf(4..4), s1.pages[0].struck)
    }

    @Test
    fun emptyInsertDoesNotMoveCaretOrStamp() {
        val s0 = initialState("abc", caret = 2)
        val s1 = reduce(s0, WriterEvent.Insert(""), CorrectionMode.CleanErase)
        assertEquals("abc", s1.pages[0].text)
        assertEquals(2, s1.caret)
        assertNull(s1.lastTypedIndex)
        assertNull(s1.composingRange)
    }

    @Test
    fun insertInMiddleShiftsTailAndStruckRanges() {
        // "ab" with 'b' struck at index 1
        val initialPage = StruckPage(text = "ab", struck = listOf(1..1))
        val s0 = initialState(pages = listOf(initialPage), caret = 1)

        // Type 'X' at index 1 -> "aXb", 'b' is now at index 2
        val s1 = reduce(s0, WriterEvent.Insert("X"), CorrectionMode.CleanErase)
        assertEquals("aXb", s1.pages[0].text)
        assertEquals(2, s1.caret)
        assertEquals(1, s1.lastTypedIndex)
        assertEquals(listOf(2..2), s1.pages[0].struck)
    }

    @Test
    fun caretSanitizerNeverSplitsSurrogate() {
        // "A" (idx 0), surrogate pair "\uD83D\uDE00" (idx 1..2), "B" (idx 3)
        val text = "A\uD83D\uDE00B"
        val s0 = initialState(text = text, caret = 0)

        // Attempting to move caret into the middle of the surrogate pair (index 2)
        val s1 = reduce(s0, WriterEvent.MoveCaret(2), CorrectionMode.CleanErase)
        // sanitizeCaret must adjust caret back to 1 to not split the surrogate pair
        assertEquals("Caret must not split surrogate pair", 1, s1.caret)

        // Moving to index 3 (after surrogate pair) is valid
        val s2 = reduce(s0, WriterEvent.MoveCaret(3), CorrectionMode.CleanErase)
        assertEquals(3, s2.caret)
    }

    @Test
    fun strikeThroughSequentialBackspaces() {
        val s0 = initialState("abc", caret = 3)

        // 1st backspace strikes 'c' (index 2)
        val s1 = reduce(s0, WriterEvent.Backspace, CorrectionMode.StrikeThrough)
        assertEquals("abc", s1.pages[0].text)
        assertEquals(2, s1.caret)
        assertEquals(listOf(2..2), s1.pages[0].struck)

        // 2nd backspace strikes 'b' (index 1)
        val s2 = reduce(s1, WriterEvent.Backspace, CorrectionMode.StrikeThrough)
        assertEquals("abc", s2.pages[0].text)
        assertEquals(1, s2.caret)
        // withStruck merges adjacent ranges 1..1 and 2..2 into 1..2
        assertEquals(listOf(1..2), s2.pages[0].struck)

        // 3rd backspace strikes 'a' (index 0)
        val s3 = reduce(s2, WriterEvent.Backspace, CorrectionMode.StrikeThrough)
        assertEquals("abc", s3.pages[0].text)
        assertEquals(0, s3.caret)
        assertEquals(listOf(0..2), s3.pages[0].struck)

        // 4th backspace at caret 0 is a no-op
        val s4 = reduce(s3, WriterEvent.Backspace, CorrectionMode.StrikeThrough)
        assertEquals("abc", s4.pages[0].text)
        assertEquals(0, s4.caret)
        assertEquals(listOf(0..2), s4.pages[0].struck)
    }

    @Test
    fun overflowPreservesStruckRangesOnSplit() {
        // Page near limit 3000 chars
        val baseText = "hello ".repeat(499) + "world " // 2994 + 6 = 3000 chars
        // Struck range at beginning (0..4) and near end (2994..2998)
        val page = StruckPage(baseText, listOf(0..4, 2994..2998))
        val s0 = initialState(pages = listOf(page), caret = baseText.length)

        // Typing overflow: "extra " (6 chars) -> 3006 chars total, will trigger overflowBreak
        val s1 = reduce(s0, WriterEvent.Insert("extra "), CorrectionMode.CleanErase)
        assertEquals(2, s1.pages.size)
        // Check struck ranges are preserved across pages
        assertTrue("Page 0 must preserve its struck range", s1.pages[0].struck.isNotEmpty())
        assertEquals(0..4, s1.pages[0].struck[0])
    }

    @Test
    fun singleEmojiInsertSetsLastTypedIndex() {
        val s0 = initialState("")
        // "😀" is \uD83D\uDE00 (length 2 Chars, 1 code point)
        val s1 = reduce(s0, WriterEvent.Insert("😀"), CorrectionMode.CleanErase)
        assertEquals(0, s1.lastTypedIndex)
        assertEquals(2, s1.caret)
        assertEquals("😀", s1.pages[0].text)
    }

    @Test
    fun composingCommitsBeforeMoveCaretGoToPageAndBackspace() {
        // Case 1: MoveCaret commits composing text
        val s0 = reduce(initialState(""), WriterEvent.Insert("abc", composing = true), CorrectionMode.CleanErase)
        assertEquals(0..2, s0.composingRange)
        val sMove = reduce(s0, WriterEvent.MoveCaret(1), CorrectionMode.CleanErase)
        assertNull(sMove.composingRange)
        assertEquals("abc", sMove.pages[0].text)
        assertEquals(1, sMove.caret)

        // Case 2: GoToPage commits composing text
        val sMultiPage = initialState(pages = listOf(StruckPage(""), StruckPage("page 2")))
        val sCompMulti = reduce(sMultiPage, WriterEvent.Insert("xyz", composing = true), CorrectionMode.CleanErase)
        assertEquals(0..2, sCompMulti.composingRange)
        val sGo = reduce(sCompMulti, WriterEvent.GoToPage(1), CorrectionMode.CleanErase)
        assertNull(sGo.composingRange)
        assertEquals("xyz", sGo.pages[0].text)
        assertEquals(1, sGo.pageIndex)

        // Case 3: Backspace commits composing text before backspacing
        val sCompBack = reduce(s0, WriterEvent.Backspace, CorrectionMode.CleanErase)
        assertNull(sCompBack.composingRange)
        assertEquals("ab", sCompBack.pages[0].text)
        assertEquals(2, sCompBack.caret)
    }

    @Test
    fun insertInsideStruckRangeSplits() {
        // "abcde", with 'b', 'c', 'd' struck: range 1..3
        val page = StruckPage("abcde", listOf(1..3))
        val s0 = initialState(pages = listOf(page), caret = 2) // Caret between 'b' (1) and 'c' (2)

        // Insert "X" at index 2
        val s1 = reduce(s0, WriterEvent.Insert("X"), CorrectionMode.CleanErase)
        assertEquals("abXcde", s1.pages[0].text)
        assertEquals(3, s1.caret)
        // Original 1..3 splits into 1..1 ('b') and 3..4 ('c', 'd')
        assertEquals(listOf(1..1, 3..4), s1.pages[0].struck)
    }

    @Test
    fun backspaceStrikeThroughSkipsAlreadyStruckChars() {
        // "abc" where 'c' (idx 2) is already struck
        val page = StruckPage("abc", listOf(2..2))
        val s0 = initialState(pages = listOf(page), caret = 3)

        // Backspace should skip 'c' and strike 'b' (idx 1)
        val s1 = reduce(s0, WriterEvent.Backspace, CorrectionMode.StrikeThrough)
        assertEquals("abc", s1.pages[0].text)
        assertEquals(1, s1.caret)
        assertEquals(listOf(1..2), s1.pages[0].struck)

        // Another backspace should skip 'b' and strike 'a' (idx 0)
        val s2 = reduce(s1, WriterEvent.Backspace, CorrectionMode.StrikeThrough)
        assertEquals("abc", s2.pages[0].text)
        assertEquals(0, s2.caret)
        assertEquals(listOf(0..2), s2.pages[0].struck)
    }

    @Test
    fun goToPagePutsCaretAtEndOfPage() {
        val s0 = initialState(
            pages = listOf(StruckPage("Hello"), StruckPage("Wonderful World")),
            pageIndex = 0,
            caret = 0
        )
        val s1 = reduce(s0, WriterEvent.GoToPage(1), CorrectionMode.CleanErase)
        assertEquals(1, s1.pageIndex)
        assertEquals(15, s1.caret) // length of "Wonderful World"

        val s2 = reduce(s1, WriterEvent.GoToPage(0), CorrectionMode.CleanErase)
        assertEquals(0, s2.pageIndex)
        assertEquals(5, s2.caret) // length of "Hello"
    }

    @Test
    fun composingInsertNeverPaginates() {
        val hugeText = "a".repeat(3050)
        val s0 = initialState("")
        val s1 = reduce(s0, WriterEvent.Insert(hugeText, composing = true), CorrectionMode.CleanErase)
        assertEquals(1, s1.pages.size)
        assertEquals(3050, s1.pages[0].text.length)

        // Committing it triggers pagination
        val s2 = reduce(s1, WriterEvent.Insert(hugeText, composing = false), CorrectionMode.CleanErase)
        assertTrue("Committing text > 3000 chars must paginate", s2.pages.size > 1)
    }

    @Test
    fun overflowSplitsStruckRangeAcrossPagesWithFollowingPagePresent() {
        val basePart1 = "a".repeat(2995)
        val basePart2 = " bbbbbbbbbb"
        val page0 = StruckPage(basePart1, listOf(2990..2994))
        val page1 = StruckPage("Existing page 1 text")

        val s0 = initialState(pages = listOf(page0, page1), pageIndex = 0, caret = basePart1.length)
        val s1 = reduce(s0, WriterEvent.Insert(basePart2), CorrectionMode.CleanErase)

        assertEquals("Should now have 3 pages", 3, s1.pages.size)
        assertEquals("Existing page 1 must now be at index 2", "Existing page 1 text", s1.pages[2].text)
        assertTrue("Page 0 has part of struck text", s1.pages[0].struck.isNotEmpty())
    }

    @Test
    fun seededPropertyTest() {
        val random = java.util.Random(42)
        var state = initialState("")
        val chars = listOf("a", "b", " ", "\n", "😀", "🎉", "cde")

        repeat(1000) {
            val event = when (random.nextInt(5)) {
                0 -> {
                    val text = chars[random.nextInt(chars.size)]
                    val composing = random.nextBoolean()
                    WriterEvent.Insert(text, composing)
                }
                1 -> WriterEvent.Backspace
                2 -> WriterEvent.MoveCaret(random.nextInt(5000) - 100)
                3 -> WriterEvent.GoToPage(random.nextInt(10) - 2)
                else -> WriterEvent.ToggleLamp
            }

            state = reduce(state, event, CorrectionMode.CleanErase)

            // Invariants
            assertTrue("State must have at least 1 page", state.pages.isNotEmpty())
            assertTrue("pageIndex must be within bounds", state.pageIndex in state.pages.indices)
            val currentPage = state.pages[state.pageIndex]
            if (state.composingRange == null) {
                state.pages.forEach { page ->
                    assertTrue("Page length must be <= 3000", page.text.length <= 3000)
                }
            }
            assertTrue("Caret must be in [0, page.length]", state.caret in 0..currentPage.length)

            val text = currentPage.text
            if (state.caret in 1 until text.length) {
                val isBetweenSurrogates = Character.isHighSurrogate(text[state.caret - 1]) &&
                    Character.isLowSurrogate(text[state.caret])
                assertFalse("Caret must never be inside surrogate pair", isBetweenSurrogates)
            }

            // Check no lone surrogates in any page
            state.pages.forEach { page ->
                val pText = page.text
                for (i in pText.indices) {
                    if (Character.isHighSurrogate(pText[i])) {
                        assertTrue("High surrogate must be followed by low surrogate", i + 1 < pText.length && Character.isLowSurrogate(pText[i + 1]))
                    }
                    if (Character.isLowSurrogate(pText[i])) {
                        assertTrue("Low surrogate must be preceded by high surrogate", i > 0 && Character.isHighSurrogate(pText[i - 1]))
                    }
                }
            }
        }
    }

    @Test
    fun replaceSwapsTheSpanAndPutsTheCaretAfterIt() {
        val state = WriterState(noteId = "n", pages = listOf(StruckPage("teh cat")), pageIndex = 0, caret = 7, loaded = true)
        val next = reduce(state, WriterEvent.Replace(0, 3, "the"), CorrectionMode.CleanErase)
        assertEquals("the cat", next.pages[0].text)
        assertEquals(3, next.caret)
        assertEquals(null, next.composingRange)
    }

    @Test
    fun replaceShiftsAndTrimsStruckRanges() {
        val page = StruckPage("abcdef", listOf(0..0, 2..4, 5..5))
        val state = WriterState(noteId = "n", pages = listOf(page), pageIndex = 0, caret = 6, loaded = true)
        val next = reduce(state, WriterEvent.Replace(2, 4, "XYZ"), CorrectionMode.CleanErase)
        assertEquals("abXYZef", next.pages[0].text)
        // 0..0 stays; 2..4 is cut by the edit so only its tail (4..4) survives, moved past the new text; 5..5 shifts by +1.
        assertEquals(listOf(0..0, 5..5, 6..6), next.pages[0].struck)
    }

    @Test
    fun replaceClampsOutOfRangeSpansAndHandlesAnEmptyPage() {
        val empty = WriterState(noteId = "n", pages = listOf(StruckPage("")), pageIndex = 0, caret = 0, loaded = true)
        assertEquals("x", reduce(empty, WriterEvent.Replace(-5, 99, "x"), CorrectionMode.CleanErase).pages[0].text)
        val text = WriterState(noteId = "n", pages = listOf(StruckPage("abc")), pageIndex = 0, caret = 3, loaded = true)
        assertEquals("abcz", reduce(text, WriterEvent.Replace(99, 120, "z"), CorrectionMode.CleanErase).pages[0].text)
        assertEquals("abc", reduce(text, WriterEvent.Replace(1, 1, ""), CorrectionMode.CleanErase).pages[0].text)
    }
}
