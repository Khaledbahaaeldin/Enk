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

    @Test
    fun createNotePartitionIsolation() = runBlocking {
        val div1 = db.dividerDao().insert(DividerEntity(name = "Work", position = 0))
        val div2 = db.dividerDao().insert(DividerEntity(name = "Personal", position = 1))

        // Create 2 notes in div1
        val w1 = repo.createNote(div1)
        val w2 = repo.createNote(div1) // w2 pos 0, w1 pos 1

        // Create 2 notes in unfiled
        val u1 = repo.createNote(null)
        val u2 = repo.createNote(null) // u2 pos 0, u1 pos 1

        // Create 1 note in div2
        val p1 = repo.createNote(div2) // p1 pos 0

        // Verify div1 positions
        val div1Notes = db.noteDao().observeByDivider(div1).first()
        assertEquals(listOf(w2.id, w1.id), div1Notes.map { it.id })
        assertEquals(listOf(0, 1), div1Notes.map { it.position })

        // Verify unfiled positions
        val unfiledNotes = db.noteDao().observeByDivider(null).first()
        assertEquals(listOf(u2.id, u1.id), unfiledNotes.map { it.id })
        assertEquals(listOf(0, 1), unfiledNotes.map { it.position })

        // Verify div2 positions
        val div2Notes = db.noteDao().observeByDivider(div2).first()
        assertEquals(listOf(p1.id), div2Notes.map { it.id })
        assertEquals(listOf(0), div2Notes.map { it.position })
    }

    @Test
    fun purgeExpiredMultipleNotesAndPagesDeleted() = runBlocking {
        clock.time = 0L
        val noteA = repo.createNote(null)
        repo.savePages(noteA.id, listOf(StruckPage("Note A Page 0"), StruckPage("Note A Page 1")))

        val noteB = repo.createNote(null)
        repo.savePages(noteB.id, listOf(StruckPage("Note B Page 0")))

        val noteC = repo.createNote(null)
        repo.savePages(noteC.id, listOf(StruckPage("Note C Page 0")))

        // Trash noteA and noteB at day 0
        repo.trash(noteA.id)
        repo.trash(noteB.id)

        // Advance 2 days, then trash noteC
        clock.time = 2 * 86_400_000L
        repo.trash(noteC.id)

        // Advance to 30.5 days from start (noteA, noteB are 30.5 days old; noteC is 28.5 days old)
        clock.time = (30.5 * 86_400_000L).toLong()
        val purgedCount = repo.purgeExpired(days = 30)
        assertEquals(2, purgedCount)

        assertNull("noteA must be purged", db.noteDao().get(noteA.id))
        assertTrue("noteA pages must be deleted", db.pageDao().forNote(noteA.id).isEmpty())

        assertNull("noteB must be purged", db.noteDao().get(noteB.id))
        assertTrue("noteB pages must be deleted", db.pageDao().forNote(noteB.id).isEmpty())

        assertNotNull("noteC must still exist (only 28.5 days old)", db.noteDao().get(noteC.id))
        assertEquals(1, db.pageDao().forNote(noteC.id).size)
    }

    @Test
    fun firstLinesVariations() = runBlocking {
        val noteEmpty = repo.createNote(null)
        repo.savePages(noteEmpty.id, listOf(StruckPage("")))
        val summaryEmpty = repo.observeNotes(null).first().first { it.id == noteEmpty.id }
        assertEquals(Pair("", ""), summaryEmpty.firstLines)

        val noteOneLine = repo.createNote(null)
        repo.savePages(noteOneLine.id, listOf(StruckPage("Single line only")))
        val summaryOne = repo.observeNotes(null).first().first { it.id == noteOneLine.id }
        assertEquals(Pair("Single line only", ""), summaryOne.firstLines)

        val noteBlanks = repo.createNote(null)
        repo.savePages(noteBlanks.id, listOf(StruckPage("\n\n   \nFirst Line\n\n   \nSecond Line\n\nThird Line")))
        val summaryBlanks = repo.observeNotes(null).first().first { it.id == noteBlanks.id }
        assertEquals(Pair("First Line", "Second Line"), summaryBlanks.firstLines)

        val noteLong = repo.createNote(null)
        val l1 = "A".repeat(80)
        val l2 = "B".repeat(75)
        repo.savePages(noteLong.id, listOf(StruckPage("$l1\n$l2")))
        val summaryLong = repo.observeNotes(null).first().first { it.id == noteLong.id }
        assertEquals(Pair("A".repeat(60), "B".repeat(60)), summaryLong.firstLines)
    }

    @Test
    fun observeTrashedDaysRemainingAndTitle() = runBlocking {
        clock.time = 1_000_000L
        val note1 = repo.createNote(null)
        repo.savePages(note1.id, listOf(StruckPage("Trashed Note Title\nSubtext")))
        repo.trash(note1.id)

        // 0 days elapsed -> 30 days remaining
        val trashed0 = repo.observeTrashed().first()
        assertEquals(1, trashed0.size)
        assertEquals("Trashed Note Title", trashed0[0].title)
        assertEquals(30, trashed0[0].daysRemaining)

        // Advance 29.5 days -> 1 day remaining
        clock.time = 1_000_000L + (29.5 * 86_400_000L).toLong()
        val trashed29 = repo.observeTrashed().first()
        assertEquals(1, trashed29[0].daysRemaining)

        // Advance past 30 days -> 0 days remaining (clamped to 0)
        clock.time = 1_000_000L + 35 * 86_400_000L
        val trashed35 = repo.observeTrashed().first()
        assertEquals(0, trashed35[0].daysRemaining)

        // Blank text note displays "Untitled"
        val noteBlank = repo.createNote(null)
        repo.savePages(noteBlank.id, listOf(StruckPage("   \n   \n")))
        repo.trash(noteBlank.id)
        val trashedBlank = repo.observeTrashed().first().first { it.id == noteBlank.id }
        assertEquals("Untitled", trashedBlank.title)
    }

    @Test
    fun reorderEmptyAndSingleList() = runBlocking {
        repo.reorderNotes(null, emptyList())
        repo.reorderDividers(emptyList())

        val note = repo.createNote(null)
        repo.reorderNotes(null, listOf(note.id))
        assertEquals(0, db.noteDao().get(note.id)?.position)

        val div = db.dividerDao().insert(DividerEntity(name = "Single", position = 5))
        repo.reorderDividers(listOf(div))
        assertEquals(0, db.dividerDao().all().first { it.id == div }.position)
    }

    @Test
    fun savePagesEmptyListCreatesEmptyPage() = runBlocking {
        val note = repo.createNote(null)
        repo.savePages(note.id, emptyList())

        val loaded = repo.load(note.id)
        assertNotNull(loaded)
        assertEquals(1, loaded?.pages?.size)
        assertEquals("", loaded?.pages?.get(0)?.text)
        assertTrue(loaded?.pages?.get(0)?.struck?.isEmpty() == true)

        val pageEntities = db.pageDao().forNote(note.id)
        assertEquals(1, pageEntities.size)
        assertEquals(0, pageEntities[0].pageIndex)
        assertEquals("", pageEntities[0].text)
    }

    @Test
    fun loadNonExistentReturnsNull() = runBlocking {
        val result = repo.load("non-existent-uuid")
        assertNull(result)
    }
}
