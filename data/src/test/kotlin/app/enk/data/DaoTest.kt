package app.enk.data

import android.content.Context
import androidx.room.Room
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

@RunWith(RobolectricTestRunner::class)
class DaoTest {

    private lateinit var db: EnkDatabase
    private lateinit var dividerDao: DividerDao
    private lateinit var noteDao: NoteDao
    private lateinit var pageDao: PageDao

    @Before
    fun setUp() {
        val context: Context = RuntimeEnvironment.getApplication()
        db = Room.inMemoryDatabaseBuilder(context, EnkDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        dividerDao = db.dividerDao()
        noteDao = db.noteDao()
        pageDao = db.pageDao()
    }

    @After
    fun tearDown() {
        db.close()
    }

    @Test
    fun insertAndReadPages() = runBlocking {
        val note = NoteEntity(
            id = "note-1",
            dividerId = null,
            position = 0,
            createdAt = 1000L,
            modifiedAt = 1000L,
            trashedAt = null
        )
        noteDao.upsert(note)

        val page0 = PageEntity(noteId = "note-1", pageIndex = 0, text = "Page zero text", struck = "1-2")
        val page1 = PageEntity(noteId = "note-1", pageIndex = 1, text = "Page one text", struck = "")
        pageDao.upsert(page0)
        pageDao.upsert(page1)

        val pages = pageDao.forNote("note-1")
        assertEquals(2, pages.size)
        assertEquals(0, pages[0].pageIndex)
        assertEquals("Page zero text", pages[0].text)
        assertEquals("1-2", pages[0].struck)
        assertEquals(1, pages[1].pageIndex)
        assertEquals("Page one text", pages[1].text)
        assertEquals("", pages[1].struck)
    }

    @Test
    fun trashHidesFromObserveByDivider() = runBlocking {
        val note = NoteEntity(
            id = "note-1",
            dividerId = null,
            position = 0,
            createdAt = 1000L,
            modifiedAt = 1000L,
            trashedAt = null
        )
        noteDao.upsert(note)

        val activeBefore = noteDao.observeByDivider(null).first()
        assertEquals(1, activeBefore.size)
        assertEquals("note-1", activeBefore[0].id)

        noteDao.trash("note-1", at = 2000L)

        val activeAfter = noteDao.observeByDivider(null).first()
        assertTrue("Trashed note must not appear in observeByDivider", activeAfter.isEmpty())

        val trashed = noteDao.observeTrashed().first()
        assertEquals(1, trashed.size)
        assertEquals("note-1", trashed[0].id)
        assertEquals(2000L, trashed[0].trashedAt)

        noteDao.restore("note-1")
        val restored = noteDao.observeByDivider(null).first()
        assertEquals(1, restored.size)
        assertNull(restored[0].trashedAt)
    }

    @Test
    fun purgeRemovesOnlyOlderThanCutoff() = runBlocking {
        val note1 = NoteEntity(id = "n1", dividerId = null, position = 0, createdAt = 10L, modifiedAt = 10L, trashedAt = 0L)
        val note2 = NoteEntity(id = "n2", dividerId = null, position = 1, createdAt = 20L, modifiedAt = 20L, trashedAt = 1000L)
        noteDao.upsert(note1)
        noteDao.upsert(note2)

        val purgedCount = noteDao.purgeTrashedBefore(cutoff = 500L)
        assertEquals(1, purgedCount)

        assertNull("n1 was trashed at 0 < 500, must be purged", noteDao.get("n1"))
        assertNotNull("n2 was trashed at 1000 >= 500, must remain", noteDao.get("n2"))
    }

    @Test
    fun deleteDividerMovesNotesToUnfiled() = runBlocking {
        val dividerId = dividerDao.insert(DividerEntity(name = "Personal", position = 0))

        val note = NoteEntity(
            id = "note-in-divider",
            dividerId = dividerId,
            position = 0,
            createdAt = 100L,
            modifiedAt = 100L,
            trashedAt = null
        )
        noteDao.upsert(note)

        val notesBefore = noteDao.observeByDivider(dividerId).first()
        assertEquals(1, notesBefore.size)

        dividerDao.delete(dividerId)

        assertTrue(dividerDao.all().isEmpty())

        val unfiledNotes = noteDao.observeByDivider(null).first()
        assertEquals(1, unfiledNotes.size)
        assertEquals("note-in-divider", unfiledNotes[0].id)
        assertNull(unfiledNotes[0].dividerId)
    }

    @Test
    fun recentIgnoresTrashed() = runBlocking {
        val note1 = NoteEntity(id = "n1", dividerId = null, position = 0, createdAt = 100L, modifiedAt = 100L, trashedAt = null)
        val note2 = NoteEntity(id = "n2", dividerId = null, position = 1, createdAt = 100L, modifiedAt = 200L, trashedAt = 250L)
        noteDao.upsert(note1)
        noteDao.upsert(note2)

        val recent = noteDao.recent()
        assertNotNull(recent)
        assertEquals("n1", recent?.id)
    }

    @Test
    fun deleteDividerAffectsOnlyTargetDivider() = runBlocking {
        val div1 = dividerDao.insert(DividerEntity(name = "Work", position = 0))
        val div2 = dividerDao.insert(DividerEntity(name = "Personal", position = 1))

        val noteWork = NoteEntity("n-work", dividerId = div1, position = 0, createdAt = 10L, modifiedAt = 10L, trashedAt = null)
        val notePersonal = NoteEntity("n-pers", dividerId = div2, position = 0, createdAt = 10L, modifiedAt = 10L, trashedAt = null)
        val noteUnfiled = NoteEntity("n-unf", dividerId = null, position = 1, createdAt = 10L, modifiedAt = 10L, trashedAt = null)

        noteDao.upsert(noteWork)
        noteDao.upsert(notePersonal)
        noteDao.upsert(noteUnfiled)

        // Delete div1
        dividerDao.delete(div1)

        val unfiled = noteDao.observeByDivider(null).first()
        assertEquals(2, unfiled.size)
        assertTrue(unfiled.any { it.id == "n-work" && it.dividerId == null })
        assertTrue(unfiled.any { it.id == "n-unf" && it.dividerId == null })

        val personalNotes = noteDao.observeByDivider(div2).first()
        assertEquals(1, personalNotes.size)
        assertEquals("n-pers", personalNotes[0].id)
        assertEquals(div2, personalNotes[0].dividerId)
    }

    @Test
    fun purgeTrashedBeforeBoundaryAndUntrashedNeverPurged() = runBlocking {
        val noteActive = NoteEntity("active", dividerId = null, position = 0, createdAt = 10L, modifiedAt = 10L, trashedAt = null)
        val noteOlder = NoteEntity("older", dividerId = null, position = 1, createdAt = 10L, modifiedAt = 10L, trashedAt = 499L)
        val noteEqual = NoteEntity("equal", dividerId = null, position = 2, createdAt = 10L, modifiedAt = 10L, trashedAt = 500L)
        val noteNewer = NoteEntity("newer", dividerId = null, position = 3, createdAt = 10L, modifiedAt = 10L, trashedAt = 501L)

        noteDao.upsert(noteActive)
        noteDao.upsert(noteOlder)
        noteDao.upsert(noteEqual)
        noteDao.upsert(noteNewer)

        // Cutoff is 500L -> only < 500L (noteOlder) should be purged
        val purged = noteDao.purgeTrashedBefore(cutoff = 500L)
        assertEquals(1, purged)

        assertNull("trashedAt 499 < 500 must be purged", noteDao.get("older"))
        assertNotNull("trashedAt 500 == 500 must NOT be purged", noteDao.get("equal"))
        assertNotNull("trashedAt 501 > 500 must NOT be purged", noteDao.get("newer"))
        assertNotNull("trashedAt null must NEVER be purged", noteDao.get("active"))
    }

    @Test
    fun recentMultipleNotesAndOrdering() = runBlocking {
        val n1 = NoteEntity("n1", dividerId = null, position = 0, createdAt = 10L, modifiedAt = 100L, trashedAt = null)
        val n2 = NoteEntity("n2", dividerId = null, position = 1, createdAt = 10L, modifiedAt = 300L, trashedAt = null)
        val n3 = NoteEntity("n3", dividerId = null, position = 2, createdAt = 10L, modifiedAt = 200L, trashedAt = null)
        val n4 = NoteEntity("n4", dividerId = null, position = 3, createdAt = 10L, modifiedAt = 500L, trashedAt = 600L) // most recent but trashed

        noteDao.upsert(n1)
        noteDao.upsert(n2)
        noteDao.upsert(n3)
        noteDao.upsert(n4)

        val recent = noteDao.recent()
        assertNotNull(recent)
        assertEquals("n2", recent?.id)
        assertEquals(300L, recent?.modifiedAt)
    }

    @Test
    fun observeByDividerOrderingAndSeparation() = runBlocking {
        val divId = dividerDao.insert(DividerEntity(name = "Projects", position = 0))

        val dNote2 = NoteEntity("d2", dividerId = divId, position = 2, createdAt = 1L, modifiedAt = 1L, trashedAt = null)
        val dNote0 = NoteEntity("d0", dividerId = divId, position = 0, createdAt = 1L, modifiedAt = 1L, trashedAt = null)
        val dNote1 = NoteEntity("d1", dividerId = divId, position = 1, createdAt = 1L, modifiedAt = 1L, trashedAt = null)

        val uNote1 = NoteEntity("u1", dividerId = null, position = 1, createdAt = 1L, modifiedAt = 1L, trashedAt = null)
        val uNote0 = NoteEntity("u0", dividerId = null, position = 0, createdAt = 1L, modifiedAt = 1L, trashedAt = null)

        noteDao.upsert(dNote2)
        noteDao.upsert(dNote0)
        noteDao.upsert(dNote1)
        noteDao.upsert(uNote1)
        noteDao.upsert(uNote0)

        val dividerNotes = noteDao.observeByDivider(divId).first()
        assertEquals(listOf("d0", "d1", "d2"), dividerNotes.map { it.id })

        val unfiledNotes = noteDao.observeByDivider(null).first()
        assertEquals(listOf("u0", "u1"), unfiledNotes.map { it.id })
    }

    @Test
    fun pageDaoDeleteFromAndIsolation() = runBlocking {
        for (i in 0..4) {
            pageDao.upsert(PageEntity("note-A", pageIndex = i, text = "Page $i", struck = ""))
        }
        for (i in 0..2) {
            pageDao.upsert(PageEntity("note-B", pageIndex = i, text = "B Page $i", struck = ""))
        }

        // Delete from index 2 for note-A
        pageDao.deleteFrom("note-A", pageIndex = 2)

        val pagesA = pageDao.forNote("note-A")
        assertEquals(2, pagesA.size)
        assertEquals(listOf(0, 1), pagesA.map { it.pageIndex })

        val pagesB = pageDao.forNote("note-B")
        assertEquals(3, pagesB.size)
        assertEquals(listOf(0, 1, 2), pagesB.map { it.pageIndex })

        // Delete all for note-A
        pageDao.deleteForNote("note-A")
        assertTrue(pageDao.forNote("note-A").isEmpty())
        assertEquals(3, pageDao.forNote("note-B").size)
    }
}
