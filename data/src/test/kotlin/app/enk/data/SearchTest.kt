package app.enk.data

import android.content.Context
import androidx.room.Room
import app.enk.text.StruckPage
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment

@RunWith(RobolectricTestRunner::class)
class SearchTest {

    private lateinit var db: EnkDatabase
    private lateinit var repo: NoteRepository
    private lateinit var searchEngine: SearchEngine

    @Before
    fun setUp() {
        val context: Context = RuntimeEnvironment.getApplication()
        db = Room.inMemoryDatabaseBuilder(context, EnkDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        repo = NoteRepository(db, SystemClock)
        searchEngine = SearchEngine(db)
    }

    @After
    fun tearDown() {
        db.close()
    }

    @Test
    fun indexesSearchableTextOnly() = runBlocking {
        val note = repo.createNote(dividerId = null)
        // "hello world": "world" is at indices 6..10
        val page = StruckPage("hello world", listOf(6..10))
        repo.savePages(note.id, listOf(page))

        // "world" was struck out, so it must not be found in FTS
        val struckMatches = searchEngine.search("world")
        assertTrue("Struck text must not be searchable", struckMatches.isEmpty())

        // "hello" is intact, must be found
        val matches = searchEngine.search("hello")
        assertEquals(1, matches.size)
        assertEquals(note.id, matches[0].noteId)
        assertEquals(0, matches[0].pageIndex)
        assertTrue(matches[0].snippet.contains("hello"))
        assertEquals(listOf(0..4), matches[0].matchRanges)
    }

    @Test
    fun ignoresStruckLetters() = runBlocking {
        val note = repo.createNote(dividerId = null)
        // "cat" with "a" (index 1..1) struck -> searchable is "ct"
        val page = StruckPage("cat", listOf(1..1))
        repo.savePages(note.id, listOf(page))

        val catMatches = searchEngine.search("cat")
        assertTrue("Struck 'a' means 'cat' does not match", catMatches.isEmpty())

        val ctMatches = searchEngine.search("ct")
        assertEquals(1, ctMatches.size)
        assertEquals(note.id, ctMatches[0].noteId)
    }

    @Test
    fun findsWordOnSecondPage() = runBlocking {
        val note = repo.createNote(dividerId = null)
        repo.savePages(
            note.id,
            listOf(
                StruckPage("First page text"),
                StruckPage("Second page has emeralds inside")
            )
        )

        val matches = searchEngine.search("emerald")
        assertEquals(1, matches.size)
        assertEquals(note.id, matches[0].noteId)
        assertEquals(1, matches[0].pageIndex)
        assertTrue(matches[0].snippet.contains("emeralds"))
    }

    @Test
    fun prefixMatchingWorks() = runBlocking {
        val note = repo.createNote(dividerId = null)
        repo.savePages(note.id, listOf(StruckPage("the rain is falling")))

        val matchesRain = searchEngine.search("rain")
        assertEquals(1, matchesRain.size)

        val matchesFall = searchEngine.search("fall")
        assertEquals(1, matchesFall.size)
        assertEquals(note.id, matchesFall[0].noteId)
    }

    @Test
    fun sanitizeQueryStripsOperators() {
        assertEquals("a* b*", searchEngine.sanitizeQuery("\"a\" OR b*"))
        assertEquals("", searchEngine.sanitizeQuery("NEAR("))
        assertEquals("term*", searchEngine.sanitizeQuery("-term"))
        assertEquals("", searchEngine.sanitizeQuery("*"))
        assertEquals("", searchEngine.sanitizeQuery("!@#$%^&*()"))
        assertEquals("unmatched* quote*", searchEngine.sanitizeQuery("unmatched \" quote"))
        assertEquals("", searchEngine.sanitizeQuery("    "))

        // Top-level sanitizeQuery helper
        assertEquals("a* b*", sanitizeQuery("\"a\" OR b*"))
        assertNull(sanitizeQuery("   "))
        assertNull(sanitizeQuery("!@#$"))
    }

    @Test
    fun trashedNotesExcludedFromSearch() = runBlocking {
        val note = repo.createNote(dividerId = null)
        repo.savePages(note.id, listOf(StruckPage("secret message")))

        val matchesBefore = searchEngine.search("secret")
        assertEquals(1, matchesBefore.size)

        // Trash note -> excluded
        repo.trash(note.id)
        val matchesTrashed = searchEngine.search("secret")
        assertTrue("Trashed notes must be excluded from search", matchesTrashed.isEmpty())

        // Restore note -> included again
        repo.restore(note.id)
        val matchesRestored = searchEngine.search("secret")
        assertEquals(1, matchesRestored.size)
    }

    @Test
    fun savePagesUpdatesFtsIndex() = runBlocking {
        val note = repo.createNote(dividerId = null)
        repo.savePages(note.id, listOf(StruckPage("first version")))

        val matchesFirst = searchEngine.search("first")
        assertEquals(1, matchesFirst.size)

        // Save new pages -> old index cleared
        repo.savePages(note.id, listOf(StruckPage("second version")))

        val matchesOld = searchEngine.search("first")
        assertTrue("Old content must not be indexed", matchesOld.isEmpty())

        val matchesNew = searchEngine.search("second")
        assertEquals(1, matchesNew.size)
    }

    @Test
    fun emptyQueryReturnsEmptyList() = runBlocking {
        val note = repo.createNote(dividerId = null)
        repo.savePages(note.id, listOf(StruckPage("content")))

        assertTrue(searchEngine.search("").isEmpty())
        assertTrue(searchEngine.search("   ").isEmpty())
        assertTrue(searchEngine.search("***").isEmpty())
    }
}
