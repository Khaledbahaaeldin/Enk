package app.enk.widget

import android.content.Context
import android.content.Intent
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/**
 * What reaches the app from the home screen. The activity itself is not launched here: an
 * `ActivityScenario` on `MainActivity` hung the suite under Robolectric, so the activity's whole
 * decision is the pure [WidgetIntentParser.consume] and the view model's `handleWidgetAction`.
 */
@RunWith(RobolectricTestRunner::class)
class WidgetEntryPointTest {

    private val context: Context = ApplicationProvider.getApplicationContext()

    @Test
    fun theWidgetReadsThroughOneContainerForTheWholeProcess() {
        assertSame(WidgetContainer.get(context), WidgetContainer.get(context))
    }

    @Test
    fun aNewNoteIntentIsActedOnOnceAndThenMarkedHandled() {
        val intent = Intent(WidgetIntentParser.ACTION_NEW_NOTE)

        assertEquals(WidgetIntentAction.NewNote, WidgetIntentParser.consume(intent))
        assertTrue(intent.getBooleanExtra(WidgetIntentParser.EXTRA_HANDLED, false))
        assertEquals("the same intent replayed does nothing", WidgetIntentAction.None, WidgetIntentParser.consume(intent))
    }

    @Test
    fun aSecondTapIsANewIntentAndIsActedOn() {
        val first = Intent(WidgetIntentParser.ACTION_NEW_NOTE)
        val second = Intent(WidgetIntentParser.ACTION_NEW_NOTE)

        assertEquals(WidgetIntentAction.NewNote, WidgetIntentParser.consume(first))
        assertEquals(WidgetIntentAction.NewNote, WidgetIntentParser.consume(second))
    }

    @Test
    fun anOpenNoteIntentCarriesItsIdAndIsMarkedHandled() {
        val intent = Intent(WidgetIntentParser.ACTION_OPEN_NOTE).putExtra(WidgetIntentParser.EXTRA_NOTE_ID, "n-1")

        assertEquals(WidgetIntentAction.OpenNote("n-1"), WidgetIntentParser.consume(intent))
        assertEquals(WidgetIntentAction.None, WidgetIntentParser.consume(intent))
    }

    @Test
    fun unknownActionsBadIdsAndMissingIntentsDoNothingAndAreNotMarked() {
        val unknown = Intent("com.evil.ACTION")
        val huge = Intent(WidgetIntentParser.ACTION_OPEN_NOTE).putExtra(WidgetIntentParser.EXTRA_NOTE_ID, "A".repeat(10_000))
        val wrongType = Intent(WidgetIntentParser.ACTION_OPEN_NOTE).putExtra(WidgetIntentParser.EXTRA_NOTE_ID, 5)
        val launcher = Intent(Intent.ACTION_MAIN)

        for (intent in listOf(unknown, huge, wrongType, launcher)) {
            assertEquals(WidgetIntentAction.None, WidgetIntentParser.consume(intent))
            assertFalse(intent.hasExtra(WidgetIntentParser.EXTRA_HANDLED))
        }
        assertEquals(WidgetIntentAction.None, WidgetIntentParser.consume(null))
    }
}
