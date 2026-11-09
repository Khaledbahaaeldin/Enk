package app.enk.writer

import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import app.enk.text.StruckPage
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Test

class TextSyncTest {
    private fun state(text: String, caret: Int, loaded: Boolean = true) = WriterState(
        noteId = "n",
        pages = listOf(StruckPage(text)),
        pageIndex = 0,
        caret = caret,
        loaded = loaded
    )

    @Test
    fun matchingTextKeepsTheCompositionAlive() {
        val field = TextFieldValue("hello wor", selection = TextRange(9), composition = TextRange(6, 9))
        assertSame(field, syncField(field, state("hello wor", 9)))
    }

    @Test
    fun compositionIsNotResetEvenWhenTheCaretDiffers() {
        val field = TextFieldValue("hello wor", selection = TextRange(7), composition = TextRange(6, 9))
        assertSame(field, syncField(field, state("hello wor", 9)))
    }

    @Test
    fun caretMovedOutsideTheImeIsAppliedWhenNotComposing() {
        val field = TextFieldValue("hello", selection = TextRange(5))
        val synced = syncField(field, state("hello", 2))
        assertEquals(TextRange(2), synced.selection)
        assertEquals("hello", synced.text)
    }

    @Test
    fun differingTextIsReplacedByThePageText() {
        val field = TextFieldValue("hello!!", selection = TextRange(7))
        val synced = syncField(field, state("hello", 5))
        assertEquals("hello", synced.text)
        assertEquals(TextRange(5), synced.selection)
    }

    @Test
    fun anUnloadedStateNeverOverwritesTypedText() {
        val field = TextFieldValue("typed early", selection = TextRange(11))
        assertSame(field, syncField(field, state("", 0, loaded = false)))
    }
}
