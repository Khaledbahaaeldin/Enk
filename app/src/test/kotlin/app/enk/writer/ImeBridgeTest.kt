package app.enk.writer

import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import app.enk.settings.CorrectionMode
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Random

class ImeBridgeTest {

    @Test
    fun singleCharIsInsert() {
        val bridge = ImeBridge()
        val old = "hello"
        val new = TextFieldValue("helloa", selection = TextRange(6))
        val events = bridge.onTextChanged(old, new, composingNow = false)
        assertEquals(listOf(WriterEvent.Insert("a", composing = false)), events)
    }

    @Test
    fun deletionIsBackspace() {
        val bridge = ImeBridge()
        // Single character deletion
        val oldSingle = "hello"
        val newSingle = TextFieldValue("hell", selection = TextRange(4))
        val eventsSingle = bridge.onTextChanged(oldSingle, newSingle, composingNow = false)
        assertEquals(listOf(WriterEvent.Backspace), eventsSingle)

        // Multiple characters deletion produces multiple Backspaces
        val oldMulti = "hello"
        val newMulti = TextFieldValue("he", selection = TextRange(2))
        val eventsMulti = bridge.onTextChanged(oldMulti, newMulti, composingNow = false)
        assertEquals(listOf(WriterEvent.Backspace, WriterEvent.Backspace, WriterEvent.Backspace), eventsMulti)
    }

    @Test
    fun selectionChangeIsMoveCaret() {
        val bridge = ImeBridge()
        val old = "hello"
        val new = TextFieldValue("hello", selection = TextRange(2))
        val events = bridge.onTextChanged(old, new, composingNow = false)
        assertEquals(listOf(WriterEvent.MoveCaret(2)), events)
    }

    @Test
    fun initialSelectionChangeToOldLengthIsMoveCaret() {
        val bridge = ImeBridge()
        val old = "hello"
        val new = TextFieldValue("hello", selection = TextRange(5))
        val events = bridge.onTextChanged(old, new, composingNow = false)
        assertEquals(listOf(WriterEvent.MoveCaret(5)), events)
    }

    @Test
    fun unchangedTextReturnsEmptyList() {
        val bridge = ImeBridge()
        val old = "hello"
        val new = TextFieldValue("hello", selection = TextRange(5))
        bridge.onTextChanged(old, new, composingNow = false)
        val events = bridge.onTextChanged(old, new, composingNow = false)
        assertEquals(emptyList<WriterEvent>(), events)
    }

    @Test
    fun emptyInputReturnsEmptyList() {
        val bridge = ImeBridge()
        val old = ""
        val new = TextFieldValue("", selection = TextRange(0))
        val events = bridge.onTextChanged(old, new, composingNow = false)
        assertEquals(emptyList<WriterEvent>(), events)
    }

    @Test
    fun emojiInsertIsPreserved() {
        val bridge = ImeBridge()
        val old = "hello "
        val emoji = "🖋️"
        val new = TextFieldValue("hello $emoji", selection = TextRange(6 + emoji.length))
        val events = bridge.onTextChanged(old, new, composingNow = false)
        assertEquals(listOf(WriterEvent.Insert(emoji, composing = false)), events)
    }

    @Test
    fun emojiInsert() {
        val bridge = ImeBridge()
        val old = "pen "
        val emoji = "\uD83D\uDE00" // smiley
        val new = TextFieldValue("pen $emoji", selection = TextRange(4 + emoji.length))
        val events = bridge.onTextChanged(old, new, composingNow = false)
        assertEquals(listOf(WriterEvent.Insert(emoji, composing = false)), events)
    }

    @Test
    fun emojiDelete() {
        val bridge = ImeBridge()
        val emoji = "\uD83D\uDE00" // surrogate pair (2 UTF-16 code units, 1 code point)
        val old = "pen $emoji"
        val new = TextFieldValue("pen ", selection = TextRange(4))
        val events = bridge.onTextChanged(old, new, composingNow = false)
        // Deleting the emoji removes one code point, yielding exactly one Backspace
        assertEquals(listOf(WriterEvent.Backspace), events)
    }

    @Test
    fun multiCharacterInsertion() {
        val bridge = ImeBridge()
        val old = "hello "
        val new = TextFieldValue("hello world", selection = TextRange(11))
        val events = bridge.onTextChanged(old, new, composingNow = false)
        assertEquals(listOf(WriterEvent.Insert("world", composing = false)), events)
    }

    // ---- K0: the keyboard's composing region is not an edit ----

    private fun run(
        start: String,
        vararg steps: TextFieldValue,
        correction: CorrectionMode = CorrectionMode.CleanErase
    ): WriterState {
        val bridge = ImeBridge()
        var state = WriterState(
            noteId = "n",
            pages = listOf(app.enk.text.StruckPage(start)),
            pageIndex = 0,
            caret = start.length,
            loaded = true
        )
        var field = start
        for (step in steps) {
            for (e in bridge.onTextChanged(field, step, step.composition != null)) {
                state = reduce(state, e, correction)
            }
            field = step.text
        }
        return state
    }

    @Test
    fun aComposingWordIsInsertedFromTheTextAloneAndTheCommitAddsNothing() {
        val bridge = ImeBridge()
        val composing = TextFieldValue("hello world", composition = TextRange(6, 11), selection = TextRange(11))
        assertEquals(listOf(WriterEvent.Insert("world", composing = false)), bridge.onTextChanged("hello ", composing, true))
        val committed = TextFieldValue("hello world", selection = TextRange(11))
        assertEquals(emptyList<WriterEvent>(), bridge.onTextChanged("hello world", committed, false))
    }

    @Test
    fun markingAnExistingWordAsComposingIsNotAnEdit() {
        // SwiftKey does this on every space and Enter: same text, the word is composing again.
        val bridge = ImeBridge()
        bridge.onTextChanged("hello", TextFieldValue("hello", selection = TextRange(5)), false)
        val events = bridge.onTextChanged(
            "hello",
            TextFieldValue("hello", composition = TextRange(0, 5), selection = TextRange(5)),
            true
        )
        assertEquals(emptyList<WriterEvent>(), events)
    }

    @Test
    fun theSwiftKeyRepeatDoesNotRepeat() {
        // A keyboard that re-marks the last word after every write the app makes: the page must not change.
        val remark = TextFieldValue("hello", composition = TextRange(0, 5), selection = TextRange(5))
        val state = run("hello", remark, remark, remark, remark, remark)
        assertEquals("hello", state.pages[0].text)
        assertEquals(5, state.caret)
    }

    @Test
    fun spaceAfterASwiftKeyRemarkAddsOneSpace() {
        val state = run(
            "hello",
            TextFieldValue("hello", composition = TextRange(0, 5), selection = TextRange(5)),
            TextFieldValue("hello ", selection = TextRange(6))
        )
        assertEquals("hello ", state.pages[0].text)
        assertEquals(6, state.caret)
    }

    @Test
    fun autocorrectReplacesTheCommittedWordInsteadOfAddingBoth() {
        val state = run(
            "teh ",
            TextFieldValue("teh ", composition = TextRange(0, 3), selection = TextRange(4)),
            TextFieldValue("the ", composition = TextRange(0, 3), selection = TextRange(4)),
            TextFieldValue("the ", selection = TextRange(4))
        )
        assertEquals("the ", state.pages[0].text)
        assertEquals(4, state.caret)
    }

    @Test
    fun autocorrectInStrikeThroughModeStrikesNothing() {
        val state = run(
            "teh ",
            TextFieldValue("the ", composition = TextRange(0, 3), selection = TextRange(4)),
            correction = CorrectionMode.StrikeThrough
        )
        assertEquals("the ", state.pages[0].text)
        assertEquals(emptyList<IntRange>(), state.pages[0].struck)
    }

    @Test
    fun aSuggestionReplacesTheTypedWord() {
        val state = run("hi tehre", TextFieldValue("hi there ", selection = TextRange(9)))
        assertEquals("hi there ", state.pages[0].text)
        assertEquals(9, state.caret)
    }

    @Test
    fun aCommitWithTrailingSpaceInsertsTheSpace() {
        val bridge = ImeBridge()
        val events = bridge.onTextChanged("hello world", TextFieldValue("hello world ", selection = TextRange(12)), false)
        assertEquals(listOf(WriterEvent.Insert(" ", composing = false)), events)
    }

    @Test
    fun aGlideTypedWordArrivesAsOneInsert() {
        val bridge = ImeBridge()
        val events = bridge.onTextChanged(
            "hello ",
            TextFieldValue("hello beautiful ", selection = TextRange(16)),
            false
        )
        assertEquals(listOf(WriterEvent.Insert("beautiful ", composing = false)), events)
    }

    @Test
    fun doubleSpaceBecomingAFullStopKeepsTheCaretWhereTheKeyboardPutIt() {
        val state = run("hello ", TextFieldValue("hello. ", selection = TextRange(7)))
        assertEquals("hello. ", state.pages[0].text)
        assertEquals(7, state.caret)
    }

    @Test
    fun aCharacterTypedOverASelectionReplacesIt() {
        val state = run("hello world", TextFieldValue("hello x", selection = TextRange(7)))
        assertEquals("hello x", state.pages[0].text)
        assertEquals(7, state.caret)
    }

    @Test
    fun aReplacementThatTouchesASurrogatePairKeepsThePairWhole() {
        val state = run("a😀b", TextFieldValue("a😁b", selection = TextRange(4)))
        assertEquals("a😁b", state.pages[0].text)
    }

    @Test
    fun backspaceIntoACommittedWordWhileTheKeyboardRecomposesIt() {
        // The keyboard re-marks "hello" (no edit), then deletes the last letter.
        val state = run(
            "hello",
            TextFieldValue("hello", composition = TextRange(0, 5), selection = TextRange(5)),
            TextFieldValue("hell", composition = TextRange(0, 4), selection = TextRange(4))
        )
        assertEquals("hell", state.pages[0].text)
        assertEquals(4, state.caret)
    }

    @Test
    fun pasteOf200kCharacters() {
        val bridge = ImeBridge()
        val bigText = "a".repeat(200_000)
        val start = System.currentTimeMillis()
        val events = bridge.onTextChanged("", TextFieldValue(bigText, selection = TextRange(200_000)), composingNow = false)
        val duration = System.currentTimeMillis() - start
        assertEquals(listOf(WriterEvent.Insert(bigText, composing = false)), events)
        assertTrue("Paste must take linear time (< 1000ms), took $duration ms", duration < 1000)
    }

    @Test
    fun crlfPaste() {
        val bridge = ImeBridge()
        val events = bridge.onTextChanged(
            "",
            TextFieldValue("foo\r\nbar\r\nbaz", selection = TextRange(14)),
            composingNow = false
        )
        assertEquals(listOf(WriterEvent.Insert("foo\nbar\nbaz", composing = false)), events)
    }

    @Test
    fun emptyToEmpty() {
        val bridge = ImeBridge()
        val events = bridge.onTextChanged("", TextFieldValue("", selection = TextRange(0)), composingNow = false)
        assertEquals(emptyList<WriterEvent>(), events)
    }

    @Test
    fun propertyTestOverRandomEditSequences() {
        val random = Random(42)
        val bridge = ImeBridge()
        var state = WriterState(noteId = "test-note", pages = listOf(app.enk.text.StruckPage("")), pageIndex = 0, caret = 0)
        var modelText = ""
        var modelCaret = 0

        val alphabet = "abcdefghijklmnopqrstuvwxyz 0123456789\n"

        for (step in 0 until 100) {
            val op = random.nextInt(4)
            when (op) {
                0 -> {
                    // Insert random string (1 to 5 chars)
                    val len = 1 + random.nextInt(5)
                    val str = buildString { repeat(len) { append(alphabet[random.nextInt(alphabet.length)]) } }
                    val newText = modelText.substring(0, modelCaret) + str + modelText.substring(modelCaret)
                    val newCaret = modelCaret + str.length
                    val events = bridge.onTextChanged(modelText, TextFieldValue(newText, selection = TextRange(newCaret)), composingNow = false)
                    for (e in events) {
                        state = reduce(state, e, CorrectionMode.CleanErase)
                    }
                    modelText = newText
                    modelCaret = newCaret
                }
                1 -> {
                    // Backspace if caret > 0
                    if (modelCaret > 0) {
                        val newText = modelText.substring(0, modelCaret - 1) + modelText.substring(modelCaret)
                        val newCaret = modelCaret - 1
                        val events = bridge.onTextChanged(modelText, TextFieldValue(newText, selection = TextRange(newCaret)), composingNow = false)
                        for (e in events) {
                            state = reduce(state, e, CorrectionMode.CleanErase)
                        }
                        modelText = newText
                        modelCaret = newCaret
                    }
                }
                2 -> {
                    // Move caret
                    if (modelText.isNotEmpty()) {
                        val targetCaret = random.nextInt(modelText.length + 1)
                        val events = bridge.onTextChanged(modelText, TextFieldValue(modelText, selection = TextRange(targetCaret)), composingNow = false)
                        for (e in events) {
                            state = reduce(state, e, CorrectionMode.CleanErase)
                        }
                        modelCaret = targetCaret
                    }
                }
                3 -> {
                    // Composing word then commit
                    val word = "word" + random.nextInt(100)
                    val composingText = modelText.substring(0, modelCaret) + word + modelText.substring(modelCaret)
                    val compEvents = bridge.onTextChanged(
                        modelText,
                        TextFieldValue(composingText, composition = TextRange(modelCaret, modelCaret + word.length), selection = TextRange(modelCaret + word.length)),
                        composingNow = true
                    )
                    for (e in compEvents) {
                        state = reduce(state, e, CorrectionMode.CleanErase)
                    }
                    // The field still holds the composing text when the IME commits it.
                    val commitEvents = bridge.onTextChanged(
                        composingText,
                        TextFieldValue(composingText, composition = null, selection = TextRange(modelCaret + word.length)),
                        composingNow = false
                    )
                    for (e in commitEvents) {
                        state = reduce(state, e, CorrectionMode.CleanErase)
                    }
                    modelText = composingText
                    modelCaret = modelCaret + word.length
                }
            }
            assertEquals("Step $step text mismatch", modelText, state.pages[0].text)
            assertEquals("Step $step caret mismatch", modelCaret, state.caret)
        }
    }
}
