package app.enk.data

import android.content.Context
import androidx.room.Room
import app.enk.text.StruckPage
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
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
    private lateinit var clock: TestClock
    private lateinit var repo: NoteRepository
    private lateinit var searchEngine: SearchEngine

    @Before
    fun setUp() {
        val context: Context = RuntimeEnvironment.getApplication()
        db = Room.inMemoryDatabaseBuilder(context, EnkDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        clock = TestClock(time = 1_000_000L)
        repo = NoteRepository(db, clock)
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

    @Test
    fun partiallyStruckWordIgnored() = runBlocking {
        val note = repo.createNote(dividerId = null)
        // "typewriter": strike "writer" (indices 4..9) -> searchable is "type"
        val page = StruckPage("typewriter", listOf(4..9))
        repo.savePages(note.id, listOf(page))

        assertTrue(searchEngine.search("typewriter").isEmpty())
        assertTrue(searchEngine.search("writer").isEmpty())

        val typeMatches = searchEngine.search("type")
        assertEquals(1, typeMatches.size)
        assertEquals(note.id, typeMatches[0].noteId)
        assertEquals("type", typeMatches[0].snippet)
    }

    @Test
    fun purgedNoteDeletesFtsRows() = runBlocking {
        val note = repo.createNote(dividerId = null)
        repo.savePages(note.id, listOf(StruckPage("discardableSecret")))

        val matchesBefore = searchEngine.search("discardableSecret")
        assertEquals(1, matchesBefore.size)

        repo.trash(note.id)
        clock.time += 31 * 86_400_000L
        repo.purgeExpired(days = 30)

        // Note is purged from repository
        assertTrue(searchEngine.search("discardableSecret").isEmpty())
        // FTS virtual table rows directly verified as deleted
        assertTrue(db.pageFtsDao().search("discardableSecret*").isEmpty())
    }

    @Test
    fun sanitizeQueryComprehensiveInjection() {
        assertEquals("test*", searchEngine.sanitizeQuery("\"\"\"test"))
        assertEquals("", searchEngine.sanitizeQuery("!@#$%^&*()_+={}:;\"'<>?,./~"))
        assertEquals("apple* banana*", searchEngine.sanitizeQuery("\t\n  apple \t\r\n banana  \n"))
        assertEquals("", searchEngine.sanitizeQuery("AND OR NOT NEAR and or not near"))
        assertEquals("hello* world*", searchEngine.sanitizeQuery("hello AND world"))

        // Top-level sanitizeQuery helper
        assertEquals("test*", sanitizeQuery("\"\"\"test"))
        assertNull(sanitizeQuery("!@#$%^&*()_+={}:;\"'<>?,./~"))
        assertEquals("apple* banana*", sanitizeQuery("\t\n  apple \t\r\n banana  \n"))
        assertNull(sanitizeQuery("AND OR NOT NEAR and or not near"))
        assertEquals("hello* world*", sanitizeQuery("hello AND world"))
    }

    @Test
    fun searchMatchSnippetAndRangesAccuracy() = runBlocking {
        val note = repo.createNote(dividerId = null)
        val text = "The small cat chases a tiny cat down the street"
        repo.savePages(note.id, listOf(StruckPage(text)))

        // Case-insensitive search with multiple occurrences
        val matches = searchEngine.search("CAT")
        assertEquals(1, matches.size)
        val hit = matches[0]

        // On line <= 60 chars, snippet is exact line
        assertEquals(text, hit.snippet)

        // Both occurrences of 'cat' captured in matchRanges
        assertEquals(2, hit.matchRanges.size)
        for (range in hit.matchRanges) {
            val matchedSubstring = hit.snippet.substring(range.first, range.last + 1)
            assertEquals("cat", matchedSubstring.lowercase())
        }
    }

    @Test
    fun snippetSurrogatePairBoundaryNeverSplit() = runBlocking {
        val note = repo.createNote(dividerId = null)
        // Match word "target" preceded by 20 chars, followed by padding and surrogate pair near index 60
        val prefix = "word ".repeat(4) // 20 chars
        val keyword = "target " // 7 chars (total 27)
        val padding = "a ".repeat(16) // 32 chars (total 59)
        val emoji = "😀" // index 59 is high surrogate, index 60 is low surrogate
        val suffix = " tail of a very long line exceeding sixty characters"
        val longLine = prefix + keyword + padding + emoji + suffix
        assertTrue(longLine.length > 60)

        repo.savePages(note.id, listOf(StruckPage(longLine)))

        val matches = searchEngine.search("target")
        assertEquals(1, matches.size)
        val snippet = matches[0].snippet
        assertTrue("Snippet must not exceed 60 characters", snippet.length <= 60)
        assertTrue("Snippet must contain target", snippet.contains("target"))

        // Must never end with orphaned high surrogate or begin with orphaned low surrogate
        if (snippet.isNotEmpty()) {
            assertFalse("Snippet must not end with dangling high surrogate", Character.isHighSurrogate(snippet.last()))
            assertFalse("Snippet must not start with dangling low surrogate", Character.isLowSurrogate(snippet.first()))
        }
    }
}
