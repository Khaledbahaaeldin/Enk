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
}
