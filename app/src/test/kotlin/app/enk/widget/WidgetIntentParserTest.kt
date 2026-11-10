package app.enk.widget

import app.enk.start.StartGate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class WidgetIntentParserTest {

    @Test
    fun newNoteActionParsesCorrectly() {
        val result = WidgetIntentParser.parse(
            action = WidgetIntentParser.ACTION_NEW_NOTE,
            extras = { null }
        )
        assertTrue(result is WidgetIntentAction.NewNote)
    }

    @Test
    fun openNoteActionWithValidIdParsesCorrectly() {
        val noteId = "note-uuid-12345"
        val result = WidgetIntentParser.parse(
            action = WidgetIntentParser.ACTION_OPEN_NOTE,
            extras = { key -> if (key == WidgetIntentParser.EXTRA_NOTE_ID) noteId else null }
        )
        assertEquals(WidgetIntentAction.OpenNote(noteId), result)
    }

    @Test
    fun openNoteWithBlankOrMissingIdIsRejected() {
        val missing = WidgetIntentParser.parse(
            action = WidgetIntentParser.ACTION_OPEN_NOTE,
            extras = { null }
        )
        assertEquals(WidgetIntentAction.None, missing)

        val blank = WidgetIntentParser.parse(
            action = WidgetIntentParser.ACTION_OPEN_NOTE,
            extras = { "   " }
        )
        assertEquals(WidgetIntentAction.None, blank)
    }

    @Test
    fun openNoteWith10000CharIdIsRejected() {
        val hugeId = "A".repeat(10000)
        val result = WidgetIntentParser.parse(
            action = WidgetIntentParser.ACTION_OPEN_NOTE,
            extras = { hugeId }
        )
        assertEquals(WidgetIntentAction.None, result)
    }

    @Test
    fun openNoteWithMaliciousExtraTypeDoesNotCrash() {
        val result = WidgetIntentParser.parse(
            action = WidgetIntentParser.ACTION_OPEN_NOTE,
            extras = { 123456 } // Integer instead of String
        )
        assertEquals(WidgetIntentAction.None, result)
    }

    @Test
    fun unknownActionIsIgnored() {
        val result = WidgetIntentParser.parse(
            action = "com.unknown.action",
            extras = { "some-data" }
        )
        assertEquals(WidgetIntentAction.None, result)
    }

    @Test
    fun widgetActionsSkipStartAnimationInStartGate() {
        // Cold start, not restored, loadMs < 1500
        val normalColdStart = StartGate.shouldPlay(
            coldStart = true,
            launchedFromWidgetOrIntent = false,
            restoredState = false,
            loadMs = 300L
        )
        assertTrue(normalColdStart)

        // Launched from widget intent
        val widgetColdStart = StartGate.shouldPlay(
            coldStart = true,
            launchedFromWidgetOrIntent = true,
            restoredState = false,
            loadMs = 300L
        )
        assertFalse(widgetColdStart)
    }
}
