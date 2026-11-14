package app.enk.widget

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import app.enk.AppContainer
import app.enk.testContainer
import app.enk.text.StruckPage
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.io.File
import java.time.ZoneOffset
import java.util.Locale

@RunWith(RobolectricTestRunner::class)
class WidgetModelTest {

    private lateinit var context: Context
    private lateinit var container: AppContainer
    private val fixedZone = ZoneOffset.UTC
    private val englishLocale = Locale.ENGLISH

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        container = testContainer(context, context.cacheDir)
    }

    @After
    fun tearDown() {
        container.db.close()
    }

    @Test
    fun emptyBoxShowsPrompt() = runBlocking {
        val model = buildWidgetModel(container.repo, englishLocale, fixedZone)
        assertEquals("Nothing yet", model.title)
        assertEquals("Tap to write your first note.", model.excerpt)
        assertNull(model.noteId)
        assertNull(model.meta)
    }

    @Test
    fun trashedNoteNeverShown() = runBlocking {
        val note = container.repo.createNote(dividerId = null)
        container.repo.savePages(note.id, listOf(StruckPage("Active text\nLine 2")))

        val trashedNote = container.repo.createNote(dividerId = null)
        container.repo.savePages(trashedNote.id, listOf(StruckPage("Trashed text")))
        container.repo.trash(trashedNote.id)

        val model = buildWidgetModel(container.repo, englishLocale, fixedZone)
        assertEquals(note.id, model.noteId)
        assertEquals("Active text\nLine 2", model.excerpt)
    }

    @Test
    fun excerptIsFirstTwoLinesMax120Chars() = runBlocking {
        val note = container.repo.createNote(dividerId = null)
        val text = "\n\n  First line of the note  \n\n  Second line of the note  \nThird line should be ignored"
        container.repo.savePages(note.id, listOf(StruckPage(text)))

        val model = buildWidgetModel(container.repo, englishLocale, fixedZone)
        assertEquals("First line of the note\nSecond line of the note", model.excerpt)

        // Trash previous note to avoid same-millisecond ordering ties
        container.repo.trash(note.id)

        // Note with no text
        val emptyNote = container.repo.createNote(dividerId = null)
        container.repo.savePages(emptyNote.id, listOf(StruckPage("   \n\n   ")))
        val emptyModel = buildWidgetModel(container.repo, englishLocale, fixedZone)
        assertEquals("Empty note", emptyModel.excerpt)

        // Trash previous note to avoid same-millisecond ordering ties
        container.repo.trash(emptyNote.id)

        // Text exceeding 120 chars with surrogate pairs / emoji
        val longLine1 = "A".repeat(80)
        val longLine2 = "B".repeat(50) + " \uD83D\uDE00"
        val longNote = container.repo.createNote(dividerId = null)
        container.repo.savePages(longNote.id, listOf(StruckPage("$longLine1\n$longLine2")))

        val longModel = buildWidgetModel(container.repo, englishLocale, fixedZone)
        assertEquals(121, longModel.excerpt.length) // 120 characters + 1 ellipsis char
        assert(longModel.excerpt.endsWith("…"))
        assert(!longModel.excerpt.contains("\uD83D\uDE00"))
    }

    @Test
    fun metaLineFormat() = runBlocking {
        val note = container.repo.createNote(dividerId = null)
        container.repo.savePages(
            note.id,
            listOf(StruckPage("Page 1 content"), StruckPage("Page 2 content"))
        )
        // Force modifiedAt to Nov 7, 2023, 12:00:00 UTC (1699358400000L) after savePages
        container.db.noteDao().upsert(
            container.db.noteDao().get(note.id)!!.copy(modifiedAt = 1699358400000L)
        )

        val model = buildWidgetModel(container.repo, englishLocale, fixedZone)
        assertNotNull(model.meta)
        assertEquals("Recent · 7 Nov · p.2", model.meta)
    }
}
