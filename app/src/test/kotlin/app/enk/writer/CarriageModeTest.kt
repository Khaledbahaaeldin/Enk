package app.enk.writer

import app.enk.settings.CorrectionMode
import app.enk.text.GridLayout
import app.enk.text.StruckPage
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Random

class CarriageModeTest {

    private val cols = 28
    private val correction = CorrectionMode.CleanErase

    private fun initState(text: String = "", caret: Int = text.length, loaded: Boolean = true): WriterState {
        return WriterState(
            noteId = "note-1",
            pages = listOf(StruckPage(text)),
            pageIndex = 0,
            caret = caret,
            loaded = loaded,
            lampOn = false
        )
    }

    @Test
    fun unloadedReturnsRest() {
        val before = initState("abc", loaded = true)
        val event = WriterEvent.Insert("d")
        val after = reduce(before, event, correction).copy(loaded = false)
        assertEquals(CarriageMotion.Rest, carriageModeFor(event, before, after, cols))
    }

    @Test
    fun pageChangeReturnsRest() {
        val before = WriterState(
            noteId = "note-1",
            pages = listOf(StruckPage("page 0"), StruckPage("page 1")),
            pageIndex = 0,
            caret = 6,
            loaded = true
        )
        val event = WriterEvent.GoToPage(1)
        val after = reduce(before, event, correction)
        assertEquals(CarriageMotion.Rest, carriageModeFor(event, before, after, cols))
    }

    @Test
    fun enterReturnsReturn() {
        val before = initState("hello")
        val event = WriterEvent.Insert("\n")
        val after = reduce(before, event, correction)
        assertEquals(CarriageMotion.Return, carriageModeFor(event, before, after, cols))
    }

    @Test
    fun wrapReturnsWrapReturn() {
        // Space-less 28-char line: typing the 29th character wraps to line 1
        val beforeText = "a".repeat(28)
        val before = initState(beforeText)
        val event = WriterEvent.Insert("b")
        val after = reduce(before, event, correction)
        assertEquals(CarriageMotion.WrapReturn, carriageModeFor(event, before, after, cols))
    }

    @Test
    fun typedSpaceReturnsTickSoft() {
        val before = initState("hello")
        val event = WriterEvent.Insert(" ")
        val after = reduce(before, event, correction)
        assertEquals(CarriageMotion.TickSoft, carriageModeFor(event, before, after, cols))
    }

    @Test
    fun singleCharReturnsTick() {
        val before = initState("hello")
        val event = WriterEvent.Insert("w")
        val after = reduce(before, event, correction)
        assertEquals(CarriageMotion.Tick, carriageModeFor(event, before, after, cols))
    }

    @Test
    fun emojiInsertReturnsTick() {
        val before = initState("hello ")
        // 🚂 U+1F682 is 1 code point, 2 chars in UTF-16
        val event = WriterEvent.Insert("\uD83D\uDE82")
        val after = reduce(before, event, correction)
        assertEquals(CarriageMotion.Tick, carriageModeFor(event, before, after, cols))
    }

    @Test
    fun backspaceJoiningLineAboveReturnsReturn() {
        val beforeText = "first line\n"
        val before = initState(beforeText)
        val event = WriterEvent.Backspace
        val after = reduce(before, event, correction)
        assertEquals(CarriageMotion.Return, carriageModeFor(event, before, after, cols))
    }

    @Test
    fun backspaceOnSameLineReturnsTick() {
        val before = initState("hello")
        val event = WriterEvent.Backspace
        val after = reduce(before, event, correction)
        assertEquals(CarriageMotion.Tick, carriageModeFor(event, before, after, cols))
    }

    @Test
    fun otherEventsReturnRest() {
        val before = initState("hello world")

        val move = WriterEvent.MoveCaret(2)
        assertEquals(CarriageMotion.Rest, carriageModeFor(move, before, reduce(before, move, correction), cols))

        val replace = WriterEvent.Replace(0, 5, "hi")
        assertEquals(CarriageMotion.Rest, carriageModeFor(replace, before, reduce(before, replace, correction), cols))

        val multiInsert = WriterEvent.Insert("multi")
        assertEquals(CarriageMotion.Rest, carriageModeFor(multiInsert, before, reduce(before, multiInsert, correction), cols))

        val lamp = WriterEvent.ToggleLamp
        assertEquals(CarriageMotion.Rest, carriageModeFor(lamp, before, reduce(before, lamp, correction), cols))
    }

    @Test
    fun randomSequencesPropertyTest() {
        val rng = Random(42)
        var state = initState("")
        val letters = ('a'..'z').toList()

        for (step in 0 until 2000) {
            val event = when (rng.nextInt(6)) {
                0, 1, 2 -> {
                    // Single char insert: random letter or space or newline
                    val ch = when (rng.nextInt(10)) {
                        0 -> "\n"
                        1 -> " "
                        else -> letters[rng.nextInt(letters.size)].toString()
                    }
                    WriterEvent.Insert(ch)
                }
                3 -> WriterEvent.Backspace
                4 -> {
                    val pageLen = state.currentPage.length
                    val target = if (pageLen > 0) rng.nextInt(pageLen + 1) else 0
                    WriterEvent.MoveCaret(target)
                }
                else -> {
                    // Small multi-char insert
                    WriterEvent.Insert("abc")
                }
            }

            val nextState = reduce(state, event, correction)
            val mode = carriageModeFor(event, state, nextState, cols)

            if (mode == CarriageMotion.Return) {
                val isEnter = event is WriterEvent.Insert && event.text == "\n"
                val isBackspaceLineJoin = event is WriterEvent.Backspace && run {
                    val lineBefore = GridLayout.caret(GridLayout.wrap(state.currentPage.text, cols), state.caret, cols).line
                    val lineAfter = GridLayout.caret(GridLayout.wrap(nextState.currentPage.text, cols), nextState.caret, cols).line
                    lineAfter < lineBefore
                }
                assertTrue("Return must only occur after Enter or Backspace line join", isEnter || isBackspaceLineJoin)
            }

            state = nextState
        }
    }
}
