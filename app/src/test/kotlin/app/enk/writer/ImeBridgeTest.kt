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
    fun swipeCommitsWholeWordOnce() {
        val bridge = ImeBridge()
        val old = "hello "
        val composingNew = TextFieldValue("hello world", selection = TextRange(11))
        val composingEvents = bridge.onTextChanged(old, composingNew, composingNow = true)
        assertEquals(listOf(WriterEvent.Insert("world", composing = true)), composingEvents)

        val committedNew = TextFieldValue("hello world", selection = TextRange(11))
        val committedEvents = bridge.onTextChanged(old, committedNew, composingNow = false)
        assertEquals(listOf(WriterEvent.Insert("world", composing = false)), committedEvents)
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

    @Test
    fun multiWordSwipeCommit() {
        val bridge = ImeBridge()
        val old = "hello "
        val composingNew = TextFieldValue(
            "hello beautiful world",
            composition = TextRange(6, 21),
            selection = TextRange(21)
        )
        val composingEvents = bridge.onTextChanged(old, composingNew, composingNow = true)
        assertEquals(listOf(WriterEvent.Insert("beautiful world", composing = true)), composingEvents)

        val committedNew = TextFieldValue(
            "hello beautiful world",
            composition = null,
            selection = TextRange(21)
        )
        val committedEvents = bridge.onTextChanged(old, committedNew, composingNow = false)
        assertEquals(listOf(WriterEvent.Insert("beautiful world", composing = false)), committedEvents)
    }

    @Test
    fun composingReplacedByDifferentWord() {
        val bridge = ImeBridge()
        val old = "hello "
        val newFoo = TextFieldValue("hello foo", composition = TextRange(6, 9), selection = TextRange(9))
        val eventsFoo = bridge.onTextChanged(old, newFoo, composingNow = true)
        assertEquals(listOf(WriterEvent.Insert("foo", composing = true)), eventsFoo)

        val newBar = TextFieldValue("hello bar", composition = TextRange(6, 9), selection = TextRange(9))
        val eventsBar = bridge.onTextChanged(old, newBar, composingNow = true)
        assertEquals(listOf(WriterEvent.Insert("bar", composing = true)), eventsBar)

        val commitBar = TextFieldValue("hello bar", composition = null, selection = TextRange(9))
        val eventsCommit = bridge.onTextChanged(old, commitBar, composingNow = false)
        assertEquals(listOf(WriterEvent.Insert("bar", composing = false)), eventsCommit)
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
                        TextFieldValue(composingText, composition = TextRange(modelCaret, modelCaret + word.length)),
                        composingNow = true
                    )
                    for (e in compEvents) {
                        state = reduce(state, e, CorrectionMode.CleanErase)
                    }
                    val commitEvents = bridge.onTextChanged(
                        modelText,
                        TextFieldValue(composingText, composition = null),
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
