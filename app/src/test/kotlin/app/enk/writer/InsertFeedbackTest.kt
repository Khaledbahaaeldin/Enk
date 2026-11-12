package app.enk.writer

import app.enk.audio.SoundEvent
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class InsertFeedbackTest {

    private fun typed(text: String, composing: Boolean, previous: Int, wasComposing: Boolean) =
        InsertFeedback.soundFor(text, composing, previous, wasComposing)

    @Test
    fun everyLetterOfAComposingWordIsAKey() {
        assertEquals(SoundEvent.Key, typed("h", composing = true, previous = 0, wasComposing = false))
        assertEquals(SoundEvent.Key, typed("he", composing = true, previous = 1, wasComposing = true))
        assertEquals(SoundEvent.Key, typed("hel", composing = true, previous = 2, wasComposing = true))
    }

    @Test
    fun anAutocorrectThatKeepsTheLengthIsSilent() {
        assertNull(typed("the", composing = true, previous = 3, wasComposing = true))
    }

    @Test
    fun deletingFromTheComposingWordIsABackspace() {
        assertEquals(SoundEvent.Backspace, typed("he", composing = true, previous = 3, wasComposing = true))
    }

    @Test
    fun deletingTheLastComposingLetterIsABackspace() {
        assertEquals(SoundEvent.Backspace, typed("", composing = false, previous = 1, wasComposing = true))
    }

    @Test
    fun committingTheWordWithASpaceIsASpace() {
        assertEquals(SoundEvent.Space, typed("hello ", composing = false, previous = 5, wasComposing = true))
    }

    @Test
    fun committingTheWordWithEnterIsAnEnter() {
        assertEquals(SoundEvent.Enter, typed("hello\n", composing = false, previous = 5, wasComposing = true))
    }

    @Test
    fun committingTheSameWordWithoutAKeypressIsSilent() {
        assertNull(typed("hello", composing = false, previous = 5, wasComposing = true))
    }

    @Test
    fun aKeyboardThatDoesNotComposeStillSoundsPerCharacter() {
        assertEquals(SoundEvent.Key, typed("a", composing = false, previous = 0, wasComposing = false))
        assertEquals(SoundEvent.Space, typed(" ", composing = false, previous = 0, wasComposing = false))
        assertEquals(SoundEvent.Enter, typed("\n", composing = false, previous = 0, wasComposing = false))
    }

    @Test
    fun aPasteIsSilent() {
        assertNull(typed("pasted words", composing = false, previous = 0, wasComposing = false))
    }

    @Test
    fun onlyKeysSpacesAndEntersStrikeTheSlug() {
        assertTrue(InsertFeedback.strikes(SoundEvent.Key))
        assertTrue(InsertFeedback.strikes(SoundEvent.Space))
        assertTrue(InsertFeedback.strikes(SoundEvent.Enter))
        assertFalse(InsertFeedback.strikes(SoundEvent.Backspace))
        assertFalse(InsertFeedback.strikes(null))
    }
}
