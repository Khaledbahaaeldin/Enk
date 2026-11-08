package app.enk.data

import android.content.Context
import androidx.room.Room
import app.enk.text.StruckPage
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment

class TestClock(var time: Long = 1000L) : Clock {
    override fun now(): Long = time
}

@RunWith(RobolectricTestRunner::class)
class NoteRepositoryTest {

    private lateinit var db: EnkDatabase
    private lateinit var clock: TestClock
    private lateinit var repo: NoteRepository

    @Before
    fun setUp() {
        val context: Context = RuntimeEnvironment.getApplication()
        db = Room.inMemoryDatabaseBuilder(context, EnkDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        clock = TestClock(time = 1_000_000L)
        repo = NoteRepository(db, clock)
    }

    @After
    fun tearDown() {
        db.close()
    }

    @Test
    fun createPlacesFirst() = runBlocking {
        val n1 = repo.createNote(dividerId = null)
        assertEquals(1, n1.pages.size)
        assertEquals("", n1.pages[0].text)

        val n2 = repo.createNote(dividerId = null)

        val entities = db.noteDao().observeByDivider(null).first()
        assertEquals(2, entities.size)
        assertEquals(n2.id, entities[0].id)
        assertEquals(0, entities[0].position)
        assertEquals(n1.id, entities[1].id)
        assertEquals(1, entities[1].position)
    }

    @Test
    fun savePagesBumpsModifiedAt() = runBlocking {
        clock.time = 1000L
        val note = repo.createNote(dividerId = null)
        assertEquals(1000L, note.createdAt)
        assertEquals(1000L, note.modifiedAt)

        clock.time = 5000L
        val newPages = listOf(
            StruckPage("First page text", listOf(0..4)),
            StruckPage("Second page text")
        )
        repo.savePages(note.id, newPages)

        val loaded = repo.load(note.id)
        assertNotNull(loaded)
        assertEquals(1000L, loaded?.createdAt)
        assertEquals(5000L, loaded?.modifiedAt)
        assertEquals(2, loaded?.pages?.size)
        assertEquals("First page text", loaded?.pages?.get(0)?.text)
        assertEquals(listOf(0..4), loaded?.pages?.get(0)?.struck)
        assertEquals("Second page text", loaded?.pages?.get(1)?.text)
        assertTrue(loaded?.pages?.get(1)?.struck?.isEmpty() == true)
    }

    @Test
    fun purgeRemovesOlderThan30Days() = runBlocking {
        clock.time = 1_000_000L
        val note = repo.createNote(dividerId = null)
        repo.savePages(note.id, listOf(StruckPage("Permanent or doomed?")))

        repo.trash(note.id)

        // Advance 29 days
        clock.time = 1_000_000L + 29 * 86_400_000L
        val purged29 = repo.purgeExpired(days = 30)
        assertEquals(0, purged29)
        assertNotNull("Note trashed 29 days ago must still exist in DB", db.noteDao().get(note.id))
        assertTrue(db.pageDao().forNote(note.id).isNotEmpty())

        // Advance past 30 days (31 days)
        clock.time = 1_000_000L + 31 * 86_400_000L
        val purged31 = repo.purgeExpired(days = 30)
        assertEquals(1, purged31)
        assertNull("Note trashed 31 days ago must be purged", db.noteDao().get(note.id))
        assertTrue("Pages of purged note must be deleted", db.pageDao().forNote(note.id).isEmpty())
    }

    @Test
    fun reorderUpdatesPositions() = runBlocking {
        val n1 = repo.createNote(dividerId = null)
        val n2 = repo.createNote(dividerId = null)
        val n3 = repo.createNote(dividerId = null)

        repo.reorderNotes(null, listOf(n1.id, n3.id, n2.id))

        val ordered = db.noteDao().observeByDivider(null).first()
        assertEquals(listOf(n1.id, n3.id, n2.id), ordered.map { it.id })
        assertEquals(listOf(0, 1, 2), ordered.map { it.position })
    }

    @Test
    fun firstLinesExtractsFromPage1() = runBlocking {
        val note = repo.createNote(dividerId = null)
        val longLine = "x".repeat(59) + "😀" + "tail" // index 59 is high surrogate, 60 is low
        val text = "  Title line  \n\n   \n$longLine\nFourth line"
        repo.savePages(note.id, listOf(StruckPage(text)))

        val summaries = repo.observeNotes(null).first()
        assertEquals(1, summaries.size)
        val summary = summaries[0]
        assertEquals("Title line", summary.firstLines.first)
        // 59 'x' characters; surrogate pair at 59..60 not split
        assertEquals(59, summary.firstLines.second.length)
        assertEquals("x".repeat(59), summary.firstLines.second)
        assertEquals(1, summary.pageCount)
    }

    @Test
    fun observeNotesReflectsSave() = runBlocking {
        val note = repo.createNote(dividerId = null)
        val initial = repo.observeNotes(null).first()
        assertEquals(1, initial.size)
        assertEquals(Pair("", ""), initial[0].firstLines)
        assertEquals(1, initial[0].pageCount)

        repo.savePages(note.id, listOf(StruckPage("New heading\nSecond line"), StruckPage("Page two")))
        val updated = repo.observeNotes(null).first()
        assertEquals(1, updated.size)
        assertEquals(Pair("New heading", "Second line"), updated[0].firstLines)
        assertEquals(2, updated[0].pageCount)
    }

    @Test
    fun moveToDivider() = runBlocking {
        val dividerId = db.dividerDao().insert(DividerEntity(name = "Personal", position = 0))
        val note = repo.createNote(dividerId = null)

        repo.moveToDivider(note.id, dividerId)

        assertTrue(repo.observeNotes(null).first().isEmpty())
        val inDivider = repo.observeNotes(dividerId).first()
        assertEquals(1, inDivider.size)
        assertEquals(note.id, inDivider[0].id)
    }

    @Test
    fun restorePlacesFirst() = runBlocking {
        val n1 = repo.createNote(dividerId = null)
        val n2 = repo.createNote(dividerId = null)

        repo.trash(n1.id)
        val active = repo.observeNotes(null).first()
        assertEquals(listOf(n2.id), active.map { it.id })

        repo.restore(n1.id)
        val restoredActive = repo.observeNotes(null).first()
        assertEquals(listOf(n1.id, n2.id), restoredActive.map { it.id })
        assertEquals(0, restoredActive[0].id.let { db.noteDao().get(it)?.position })
        assertEquals(1, restoredActive[1].id.let { db.noteDao().get(it)?.position })
    }
}
