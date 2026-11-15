package app.enk.keyboard

import app.enk.audio.SoundEvent
import app.enk.settings.CorrectionMode
import app.enk.text.StruckPage
import app.enk.writer.WriterEvent
import app.enk.writer.WriterState
import app.enk.writer.currentPage
import app.enk.writer.reduce
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class KeyboardToWriterTest {

    private fun initialState(text: String = "", pageIndex: Int = 0, caret: Int = text.length): WriterState {
        return WriterState(
            noteId = "test-note",
            pages = listOf(StruckPage(text)),
            pageIndex = pageIndex,
            caret = caret,
            loaded = true
        )
    }

    @Test
    fun feedbackForEveryRow() {
        val s = initialState("abc")

        assertEquals(KeyFeedback(SoundEvent.Key, 8), KeyboardToWriter.feedbackFor(KeyAction.Character("a"), s))
        assertEquals(KeyFeedback(SoundEvent.Space, 14), KeyboardToWriter.feedbackFor(KeyAction.Space, s))
        assertEquals(KeyFeedback(SoundEvent.Enter, 14), KeyboardToWriter.feedbackFor(KeyAction.Enter, s))
        assertEquals(KeyFeedback(SoundEvent.Backspace, 8), KeyboardToWriter.feedbackFor(KeyAction.Backspace, s))
        assertEquals(KeyFeedback(null, 3), KeyboardToWriter.feedbackFor(KeyAction.MoveCaretBy(1, 0), s))
        assertEquals(KeyFeedback(null, 8), KeyboardToWriter.feedbackFor(KeyAction.SystemKeyboard, s))

        // Backspace at very start of page 1 (pageIndex 0, caret 0) gives null sound and 0 pulse
        val startState = initialState("", pageIndex = 0, caret = 0)
        assertEquals(KeyFeedback(null, 0), KeyboardToWriter.feedbackFor(KeyAction.Backspace, startState))

        // Backspace at pageIndex > 0 or caret > 0 has sound
        val page2Start = WriterState(
            noteId = "n",
            pages = listOf(StruckPage("p1"), StruckPage("p2")),
            pageIndex = 1,
            caret = 0,
            loaded = true
        )
        assertEquals(KeyFeedback(SoundEvent.Backspace, 8), KeyboardToWriter.feedbackFor(KeyAction.Backspace, page2Start))
    }

    @Test
    fun outcomeForEveryRow() {
        val s = initialState("hello")

        // Character
        val charOutcome = KeyboardToWriter.outcomeFor(KeyAction.Character("h"), s)
        assertEquals(WriterEvent.Insert("h"), charOutcome.event)
        assertTrue(charOutcome.strikes)
        assertEquals('h', charOutcome.slugChar)

        // Empty Character
        val emptyOutcome = KeyboardToWriter.outcomeFor(KeyAction.Character(""), s)
        assertNull(emptyOutcome.event)
        assertFalse(emptyOutcome.strikes)
        assertNull(emptyOutcome.slugChar)

        // Emoji Character
        val emojiOutcome = KeyboardToWriter.outcomeFor(KeyAction.Character("😀"), s)
        assertEquals(WriterEvent.Insert("😀"), emojiOutcome.event)
        assertTrue(emojiOutcome.strikes)
        assertNull(emojiOutcome.slugChar) // code point > 0xFFFF

        // Space
        val spaceOutcome = KeyboardToWriter.outcomeFor(KeyAction.Space, s)
        assertEquals(WriterEvent.Insert(" "), spaceOutcome.event)
        assertTrue(spaceOutcome.strikes)
        assertEquals(' ', spaceOutcome.slugChar)

        // Enter
        val enterOutcome = KeyboardToWriter.outcomeFor(KeyAction.Enter, s)
        assertEquals(WriterEvent.Insert("\n"), enterOutcome.event)
        assertTrue(enterOutcome.strikes)
        assertNull(enterOutcome.slugChar)

        // Backspace
        val bsOutcome = KeyboardToWriter.outcomeFor(KeyAction.Backspace, s)
        assertEquals(WriterEvent.Backspace, bsOutcome.event)
        assertFalse(bsOutcome.strikes)
        assertNull(bsOutcome.slugChar)

        // SystemKeyboard
        val sysOutcome = KeyboardToWriter.outcomeFor(KeyAction.SystemKeyboard, s)
        assertNull(sysOutcome.event)
        assertFalse(sysOutcome.strikes)
        assertNull(sysOutcome.slugChar)

        // MoveCaretBy
        val move1 = KeyboardToWriter.outcomeFor(KeyAction.MoveCaretBy(2, 1), s)
        assertEquals(WriterEvent.MoveCaret(3), move1.event)
        assertFalse(move1.strikes)

        // MoveCaretBy clamped below 0
        val moveUnder = KeyboardToWriter.outcomeFor(KeyAction.MoveCaretBy(-10, 2), s)
        assertEquals(WriterEvent.MoveCaret(0), moveUnder.event)

        // MoveCaretBy clamped above text.length
        val moveOver = KeyboardToWriter.outcomeFor(KeyAction.MoveCaretBy(20, 2), s)
        assertEquals(WriterEvent.MoveCaret(5), moveOver.event)

        // MoveCaretBy on empty page
        val emptyState = initialState("")
        val moveEmpty = KeyboardToWriter.outcomeFor(KeyAction.MoveCaretBy(5, 0), emptyState)
        assertEquals(WriterEvent.MoveCaret(0), moveEmpty.event)
    }

    @Test
    fun alternativeEventBothBranches() {
        val s = initialState("cat") // caret = 3
        // Base is "t", caretBefore is 2, text at 2 is "t" -> matches!
        val replaceEv = KeyboardToWriter.alternativeEvent("t", "ṯ", 2, s)
        assertEquals(WriterEvent.Replace(2, 3, "ṯ"), replaceEv)

        // Branch 1: caret moved away (caret != caretBefore + base.length)
        val caretMoved = s.copy(caret = 1)
        val insertEv1 = KeyboardToWriter.alternativeEvent("t", "ṯ", 2, caretMoved)
        assertEquals(WriterEvent.Insert("ṯ"), insertEv1)

        // Branch 2: text no longer matches
        val textChanged = initialState("car")
        val insertEv2 = KeyboardToWriter.alternativeEvent("t", "ṯ", 2, textChanged)
        assertEquals(WriterEvent.Insert("ṯ"), insertEv2)
    }

    @Test
    fun applyEventsThroughReduce() {
        // Typing keys "h", "i", Space, Enter, Backspace on an empty page gives text "hi "
        var state = initialState("")

        val actions = listOf(
            KeyAction.Character("h"),
            KeyAction.Character("i"),
            KeyAction.Space,
            KeyAction.Enter,
            KeyAction.Backspace
        )

        for (action in actions) {
            val outcome = KeyboardToWriter.outcomeFor(action, state)
            val event = outcome.event ?: error("Expected event for action $action")
            state = reduce(state, event, CorrectionMode.CleanErase)
        }

        assertEquals("hi ", state.currentPage.text)

        // Replacement of "e" by "é" gives the page text with "é" and the caret after it
        var s2 = initialState("")
        s2 = reduce(s2, KeyboardToWriter.outcomeFor(KeyAction.Character("e"), s2).event!!, CorrectionMode.CleanErase)
        assertEquals("e", s2.currentPage.text)
        assertEquals(1, s2.caret)

        val altEvent = KeyboardToWriter.alternativeEvent("e", "é", 0, s2)
        s2 = reduce(s2, altEvent, CorrectionMode.CleanErase)
        assertEquals("é", s2.currentPage.text)
        assertEquals(1, s2.caret)
    }
}
