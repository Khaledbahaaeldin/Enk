package app.enk.data

import android.content.Context
import androidx.room.Room
import app.enk.text.StruckPage
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
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.time.Instant
import java.util.zip.CRC32
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

@RunWith(RobolectricTestRunner::class)
class ExportImportTest {

    private lateinit var db: EnkDatabase
    private lateinit var repo: NoteRepository
    private lateinit var exporter: Exporter
    private lateinit var importer: Importer

    @Before
    fun setUp() {
        val context: Context = RuntimeEnvironment.getApplication()
        db = Room.inMemoryDatabaseBuilder(context, EnkDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        repo = NoteRepository(db, SystemClock)
        exporter = Exporter(repo, db.dividerDao())
        importer = Importer(repo, db.dividerDao())
    }

    @After
    fun tearDown() {
        db.close()
    }

    @Test
    fun roundTripPreservesTextDatesAndDividers() = runBlocking {
        val dividerId = db.dividerDao().insert(DividerEntity(name = "Journal", position = 0))
        val note = repo.createNote(dividerId)

        val createdTime = 1700000000000L
        val modifiedTime = 1700000500000L
        db.noteDao().upsert(
            NoteEntity(
                id = note.id,
                dividerId = dividerId,
                position = 0,
                createdAt = createdTime,
                modifiedAt = modifiedTime,
                trashedAt = null
            )
        )

        val pages = listOf(
            StruckPage("Dear future me,\nthe rain is soft on the roof today."),
            StruckPage("Page two has Arabic: مرحباً and emojis: 😀🌟.")
        )
        repo.savePages(note.id, pages)
        // Reset modifiedAt because savePages bumps it
        db.noteDao().updateModifiedAt(note.id, modifiedTime)

        val out = ByteArrayOutputStream()
        exporter.export(out)

        // Clear database
        db.pageDao().deleteForNote(note.id)
        db.pageFtsDao().deleteForNote(note.id)
        db.noteDao().delete(note.id)
        db.dividerDao().delete(dividerId)

        // Import into clean database
        val result = importer.import(ByteArrayInputStream(out.toByteArray()))
        assertTrue(result is ImportResult.Done)
        val done = result as ImportResult.Done
        assertEquals(1, done.added)
        assertEquals(0, done.replaced)
        assertEquals(0, done.skipped)

        val imported = repo.load(note.id)
        assertNotNull(imported)
        assertEquals(createdTime, imported?.createdAt)
        assertEquals(modifiedTime, imported?.modifiedAt)
        assertEquals(2, imported?.pages?.size)
        assertEquals(pages[0].text, imported?.pages?.get(0)?.text)
        assertEquals(pages[1].text, imported?.pages?.get(1)?.text)

        val dividers = db.dividerDao().all()
        assertEquals(1, dividers.size)
        assertEquals("Journal", dividers[0].name)
        assertEquals(dividers[0].id, imported?.dividerId)
    }

    @Test
    fun pageMarkerBetweenPages() = runBlocking {
        val note = repo.createNote(dividerId = null)
        val page1 = "Line with fake marker:\n<!-- enk:page -->\nAnd a line with dashes:\n---"
        val page2 = "Actual page two content"
        repo.savePages(note.id, listOf(StruckPage(page1), StruckPage(page2)))

        val out = ByteArrayOutputStream()
        exporter.export(out)

        // Clear and reimport
        db.pageDao().deleteForNote(note.id)
        db.noteDao().delete(note.id)

        val result = importer.import(ByteArrayInputStream(out.toByteArray()))
        assertTrue(result is ImportResult.Done)

        val loaded = repo.load(note.id)
        assertNotNull(loaded)
        assertEquals(2, loaded?.pages?.size)
        assertEquals(page1, loaded?.pages?.get(0)?.text)
        assertEquals(page2, loaded?.pages?.get(1)?.text)
    }

    @Test
    fun trashedNotExported() = runBlocking {
        val active = repo.createNote(dividerId = null)
        repo.savePages(active.id, listOf(StruckPage("Active note")))

        val trashed = repo.createNote(dividerId = null)
        repo.savePages(trashed.id, listOf(StruckPage("Trashed note")))
        repo.trash(trashed.id)

        val out = ByteArrayOutputStream()
        exporter.export(out)

        // Clear DB
        db.pageDao().deleteForNote(active.id)
        db.pageDao().deleteForNote(trashed.id)
        db.noteDao().delete(active.id)
        db.noteDao().delete(trashed.id)

        val result = importer.import(ByteArrayInputStream(out.toByteArray()))
        assertTrue(result is ImportResult.Done)
        assertEquals(1, (result as ImportResult.Done).added)

        assertNotNull(repo.load(active.id))
        assertNull(repo.load(trashed.id))
    }

    @Test
    fun struckOmitted() = runBlocking {
        val note = repo.createNote(dividerId = null)
        // "hello world" with "world" struck (indices 6..10)
        repo.savePages(note.id, listOf(StruckPage("hello world", listOf(6..10))))

        val out = ByteArrayOutputStream()
        exporter.export(out)

        // Clear DB
        db.pageDao().deleteForNote(note.id)
        db.noteDao().delete(note.id)

        val result = importer.import(ByteArrayInputStream(out.toByteArray()))
        assertTrue(result is ImportResult.Done)

        val loaded = repo.load(note.id)
        assertNotNull(loaded)
        assertEquals("hello ", loaded?.pages?.get(0)?.text)
    }

    @Test
    fun reimportIsIdempotent() = runBlocking {
        val n1 = repo.createNote(dividerId = null)
        val n2 = repo.createNote(dividerId = null)
        repo.savePages(n1.id, listOf(StruckPage("Note one")))
        repo.savePages(n2.id, listOf(StruckPage("Note two")))

        val out = ByteArrayOutputStream()
        exporter.export(out)
        val zipBytes = out.toByteArray()

        val r1 = importer.import(ByteArrayInputStream(zipBytes))
        assertTrue(r1 is ImportResult.Done)
        assertEquals(ImportResult.Done(added = 0, replaced = 0, skipped = 2), r1)

        val r2 = importer.import(ByteArrayInputStream(zipBytes))
        assertTrue(r2 is ImportResult.Done)
        assertEquals(ImportResult.Done(added = 0, replaced = 0, skipped = 2), r2)
    }

    @Test
    fun newerFileReplacesOlder() = runBlocking {
        val note = repo.createNote(dividerId = null)
        val initialModified = 1000L
        db.noteDao().upsert(
            NoteEntity(
                id = note.id,
                dividerId = null,
                position = 0,
                createdAt = 1000L,
                modifiedAt = initialModified,
                trashedAt = null
            )
        )
        repo.savePages(note.id, listOf(StruckPage("Old text")))
        db.noteDao().updateModifiedAt(note.id, initialModified)

        // Build a zip where the note has newer modifiedAt (2000L)
        val newerZip = createZipWithNote(
            id = note.id,
            divider = "",
            created = 1000L,
            modified = 2000L,
            pages = listOf("New replaced text")
        )

        val result = importer.import(ByteArrayInputStream(newerZip))
        assertTrue(result is ImportResult.Done)
        assertEquals(ImportResult.Done(added = 0, replaced = 1, skipped = 0), result)

        val loaded = repo.load(note.id)
        assertNotNull(loaded)
        assertEquals("New replaced text", loaded?.pages?.get(0)?.text)
        assertEquals(2000L, loaded?.modifiedAt)
    }

    @Test
    fun zipSlipRejected() = runBlocking {
        val badZip = ByteArrayOutputStream().apply {
            ZipOutputStream(this).use { zos ->
                zos.putNextEntry(ZipEntry("../evil.md"))
                zos.write("evil".toByteArray())
                zos.closeEntry()
            }
        }.toByteArray()

        val result = importer.import(ByteArrayInputStream(badZip))
        assertTrue("Zip-slip path must be rejected", result is ImportResult.Failed)
    }

    @Test
    fun zipBombRejected() = runBlocking {
        // Create an entry that expands to > 50 MB
        val bombZip = ByteArrayOutputStream().apply {
            ZipOutputStream(this).use { zos ->
                zos.putNextEntry(ZipEntry("Unfiled/huge.md"))
                val chunk = ByteArray(1024 * 1024) { 'A'.code.toByte() }
                repeat(51) {
                    zos.write(chunk)
                }
                zos.closeEntry()
            }
        }.toByteArray()

        val result = importer.import(ByteArrayInputStream(bombZip))
        assertTrue("Zip-bomb exceeding 50 MB must be rejected", result is ImportResult.Failed)
    }

    @Test
    fun invalidUtf8Rejected() = runBlocking {
        val badUtf8Zip = ByteArrayOutputStream().apply {
            ZipOutputStream(this).use { zos ->
                zos.putNextEntry(ZipEntry("Unfiled/invalid.md"))
                // Invalid UTF-8 sequence
                zos.write(byteArrayOf(0xC0.toByte(), 0xAF.toByte(), 0xFF.toByte()))
                zos.closeEntry()
            }
        }.toByteArray()

        val result = importer.import(ByteArrayInputStream(badUtf8Zip))
        assertTrue("Invalid UTF-8 must be rejected", result is ImportResult.Failed)
    }

    @Test
    fun emptyArchiveDone() = runBlocking {
        val emptyZip = ByteArrayOutputStream().apply {
            ZipOutputStream(this).use { }
        }.toByteArray()

        val result = importer.import(ByteArrayInputStream(emptyZip))
        assertTrue(result is ImportResult.Done)
        assertEquals(ImportResult.Done(0, 0, 0), result)
    }

    @Test
    fun importedNoteIsSearchable() = runBlocking {
        val noteZip = createZipWithNote(
            id = "searchable-123",
            divider = "",
            created = 1000L,
            modified = 1000L,
            pages = listOf("Supercalifragilistic note content")
        )

        val result = importer.import(ByteArrayInputStream(noteZip))
        assertTrue(result is ImportResult.Done)

        val engine = SearchRepository(db)
        val hits = engine.search("supercali")
        assertEquals(1, hits.size)
        assertEquals("searchable-123", hits[0].noteId)
    }

    @Test
    fun zipSlipVariantsRejected() = runBlocking {
        val slipPaths = listOf(
            "../escape.md",
            "folder/../../escape.md",
            "/absolute.md",
            "\\backslash.md",
            "C:\\windows.md"
        )
        for (path in slipPaths) {
            val badZip = ByteArrayOutputStream().apply {
                ZipOutputStream(this).use { zos ->
                    zos.putNextEntry(ZipEntry(path))
                    zos.write("content".toByteArray())
                    zos.closeEntry()
                }
            }.toByteArray()

            val result = importer.import(ByteArrayInputStream(badZip))
            assertTrue("Path '$path' must be rejected as zip-slip", result is ImportResult.Failed)
        }
    }

    @Test
    fun zipBombLyingHeaderRejected() = runBlocking {
        val bombZip = ByteArrayOutputStream().apply {
            ZipOutputStream(this).use { zos ->
                val entry = ZipEntry("Unfiled/lying.md")
                // Local header claims 10 bytes uncompressed
                entry.size = 10L
                zos.putNextEntry(entry)
                val chunk = ByteArray(1024 * 1024) { 'X'.code.toByte() }
                repeat(51) {
                    zos.write(chunk)
                }
                zos.closeEntry()
            }
        }.toByteArray()

        val result = importer.import(ByteArrayInputStream(bombZip))
        assertTrue("Zip-bomb exceeding 50MB uncompressed must fail even if header lied", result is ImportResult.Failed)
    }

    @Test
    fun roundTripSpecialTextCharacters() = runBlocking {
        val note = repo.createNote(dividerId = null)
        val p1 = "Trailing newlines:\n\n\n"
        val p2 = "" // Empty page
        val p3 = "Line 1\r\nLine 2\r\nLine 3"
        val p4 = "Emojis: 🌟🚀🎉 and non-Latin: مرحبا بك في إنك"

        repo.savePages(note.id, listOf(StruckPage(p1), StruckPage(p2), StruckPage(p3), StruckPage(p4)))

        val out = ByteArrayOutputStream()
        exporter.export(out)

        // Clear database
        db.pageDao().deleteForNote(note.id)
        db.pageFtsDao().deleteForNote(note.id)
        db.noteDao().delete(note.id)

        val result = importer.import(ByteArrayInputStream(out.toByteArray()))
        assertTrue(result is ImportResult.Done)

        val loaded = repo.load(note.id)
        assertNotNull(loaded)
        assertEquals(4, loaded?.pages?.size)
        assertEquals(p1, loaded?.pages?.get(0)?.text)
        assertEquals(p2, loaded?.pages?.get(1)?.text)
        // CRLF is normalized to LF by markdown import
        assertEquals("Line 1\nLine 2\nLine 3", loaded?.pages?.get(2)?.text)
        assertEquals(p4, loaded?.pages?.get(3)?.text)
    }

    @Test
    fun pageMarkerAndFrontMatterEscaping() = runBlocking {
        val note = repo.createNote(dividerId = null)
        val p1 = listOf(
            "<!-- enk:page -->",
            "\\<!-- enk:page -->",
            "---",
            "\\---"
        ).joinToString("\n")

        repo.savePages(note.id, listOf(StruckPage(p1)))

        val out = ByteArrayOutputStream()
        exporter.export(out)

        // Clear and reimport
        db.pageDao().deleteForNote(note.id)
        db.pageFtsDao().deleteForNote(note.id)
        db.noteDao().delete(note.id)

        val result = importer.import(ByteArrayInputStream(out.toByteArray()))
        assertTrue(result is ImportResult.Done)

        val loaded = repo.load(note.id)
        assertNotNull(loaded)
        assertEquals(1, loaded?.pages?.size)
        assertEquals(p1, loaded?.pages?.get(0)?.text)
    }

    @Test
    fun malformedFrontMatterRejected() = runBlocking {
        fun makeZipWithContent(content: String): ByteArray {
            return ByteArrayOutputStream().apply {
                ZipOutputStream(this).use { zos ->
                    zos.putNextEntry(ZipEntry("Unfiled/note.md"))
                    zos.write(content.toByteArray(Charsets.UTF_8))
                    zos.closeEntry()
                }
            }.toByteArray()
        }

        // Missing id
        val noId = "---\ndivider: \ncreated: 2024-01-01T00:00:00Z\nmodified: 2024-01-01T00:00:00Z\npages: 1\n---\nbody"
        assertTrue(importer.import(ByteArrayInputStream(makeZipWithContent(noId))) is ImportResult.Failed)

        // Missing created
        val noCreated = "---\nid: test-1\ndivider: \nmodified: 2024-01-01T00:00:00Z\npages: 1\n---\nbody"
        assertTrue(importer.import(ByteArrayInputStream(makeZipWithContent(noCreated))) is ImportResult.Failed)

        // Missing modified
        val noModified = "---\nid: test-1\ndivider: \ncreated: 2024-01-01T00:00:00Z\npages: 1\n---\nbody"
        assertTrue(importer.import(ByteArrayInputStream(makeZipWithContent(noModified))) is ImportResult.Failed)

        // Missing pages count
        val noPages = "---\nid: test-1\ndivider: \ncreated: 2024-01-01T00:00:00Z\nmodified: 2024-01-01T00:00:00Z\n---\nbody"
        assertTrue(importer.import(ByteArrayInputStream(makeZipWithContent(noPages))) is ImportResult.Failed)

        // Non-integer pages count
        val nonIntPages = "---\nid: test-1\ndivider: \ncreated: 2024-01-01T00:00:00Z\nmodified: 2024-01-01T00:00:00Z\npages: two\n---\nbody"
        assertTrue(importer.import(ByteArrayInputStream(makeZipWithContent(nonIntPages))) is ImportResult.Failed)

        // Page count mismatch: declared 3, actual 1
        val mismatchPages = "---\nid: test-1\ndivider: \ncreated: 2024-01-01T00:00:00Z\nmodified: 2024-01-01T00:00:00Z\npages: 3\n---\nbody"
        assertTrue(importer.import(ByteArrayInputStream(makeZipWithContent(mismatchPages))) is ImportResult.Failed)
    }

    private fun makeZipWithDuplicateEntries(name: String, content1: String, content2: String): ByteArray {
        val baos = ByteArrayOutputStream()
        fun writeStoredEntry(text: String) {
            val nameBytes = name.toByteArray(Charsets.UTF_8)
            val dataBytes = text.toByteArray(Charsets.UTF_8)
            val crc = CRC32()
            crc.update(dataBytes)

            val header = ByteBuffer.allocate(30).order(ByteOrder.LITTLE_ENDIAN)
            header.putInt(0x04034b50) // LOC signature
            header.putShort(20) // version needed
            header.putShort(0) // flags
            header.putShort(0) // method: STORED (0)
            header.putShort(0) // time
            header.putShort(0) // date
            header.putInt(crc.value.toInt()) // crc32
            header.putInt(dataBytes.size) // compressed size
            header.putInt(dataBytes.size) // uncompressed size
            header.putShort(nameBytes.size.toShort()) // name length
            header.putShort(0) // extra field length
            baos.write(header.array())
            baos.write(nameBytes)
            baos.write(dataBytes)
        }
        writeStoredEntry(content1)
        writeStoredEntry(content2)
        return baos.toByteArray()
    }

    @Test
    fun duplicateEntryNamesInZipRejected() = runBlocking {
        val dupZip = makeZipWithDuplicateEntries(
            "Unfiled/note.md",
            "---\nid: id-1\ncreated: 2024-01-01T00:00:00Z\nmodified: 2024-01-01T00:00:00Z\npages: 1\n---\nbody 1",
            "---\nid: id-2\ncreated: 2024-01-01T00:00:00Z\nmodified: 2024-01-01T00:00:00Z\npages: 1\n---\nbody 2"
        )

        val result = importer.import(ByteArrayInputStream(dupZip))
        assertTrue("Duplicate file names in archive must be rejected", result is ImportResult.Failed)
        assertEquals("Duplicate entry name in archive: Unfiled/note.md", (result as ImportResult.Failed).reason)
    }

    @Test
    fun duplicateNoteIdsInZipRejected() = runBlocking {
        val dupIdZip = ByteArrayOutputStream().apply {
            ZipOutputStream(this).use { zos ->
                zos.putNextEntry(ZipEntry("Unfiled/note-a.md"))
                zos.write("---\nid: same-id\ncreated: 2024-01-01T00:00:00Z\nmodified: 2024-01-01T00:00:00Z\npages: 1\n---\nbody A".toByteArray())
                zos.closeEntry()

                zos.putNextEntry(ZipEntry("Journal/note-b.md"))
                zos.write("---\nid: same-id\ncreated: 2024-01-01T00:00:00Z\nmodified: 2024-01-01T00:00:00Z\npages: 1\n---\nbody B".toByteArray())
                zos.closeEntry()
            }
        }.toByteArray()

        val result = importer.import(ByteArrayInputStream(dupIdZip))
        assertTrue("Duplicate note IDs in archive must be rejected", result is ImportResult.Failed)
    }

    @Test
    fun nonMarkdownAndEmptyFilesRejected() = runBlocking {
        // Non-markdown file
        val imgZip = ByteArrayOutputStream().apply {
            ZipOutputStream(this).use { zos ->
                zos.putNextEntry(ZipEntry("Unfiled/photo.png"))
                zos.write(byteArrayOf(0x89.toByte(), 0x50.toByte()))
                zos.closeEntry()
            }
        }.toByteArray()
        assertTrue("Non-markdown file must be rejected", importer.import(ByteArrayInputStream(imgZip)) is ImportResult.Failed)

        // Empty file
        val emptyFileZip = ByteArrayOutputStream().apply {
            ZipOutputStream(this).use { zos ->
                zos.putNextEntry(ZipEntry("Unfiled/empty.md"))
                zos.closeEntry()
            }
        }.toByteArray()
        assertTrue("Empty file in archive must be rejected", importer.import(ByteArrayInputStream(emptyFileZip)) is ImportResult.Failed)
    }

    @Test
    fun transactionRollsBackOnFailure() = runBlocking {
        val validContent = "---\nid: valid-note\ncreated: 2024-01-01T00:00:00Z\nmodified: 2024-01-01T00:00:00Z\npages: 1\n---\nValid note body"
        val invalidContent = "This is not front matter at all"

        val mixedZip = ByteArrayOutputStream().apply {
            ZipOutputStream(this).use { zos ->
                zos.putNextEntry(ZipEntry("Unfiled/valid.md"))
                zos.write(validContent.toByteArray(Charsets.UTF_8))
                zos.closeEntry()

                zos.putNextEntry(ZipEntry("Unfiled/invalid.md"))
                zos.write(invalidContent.toByteArray(Charsets.UTF_8))
                zos.closeEntry()
            }
        }.toByteArray()

        val result = importer.import(ByteArrayInputStream(mixedZip))
        assertTrue(result is ImportResult.Failed)

        // Valid note must NOT have been committed to the database
        assertNull(repo.load("valid-note"))
        assertTrue(db.noteDao().allActive().isEmpty())
    }

    private fun createZipWithNote(
        id: String,
        divider: String,
        created: Long,
        modified: Long,
        pages: List<String>
    ): ByteArray {
        val content = buildString {
            appendLine("---")
            appendLine("id: $id")
            appendLine("divider: $divider")
            appendLine("created: ${Instant.ofEpochMilli(created)}")
            appendLine("modified: ${Instant.ofEpochMilli(modified)}")
            appendLine("pages: ${pages.size}")
            appendLine("---")
            append(pages.joinToString("\n<!-- enk:page -->\n"))
        }

        return ByteArrayOutputStream().apply {
            ZipOutputStream(this).use { zos ->
                val folder = if (divider.isBlank()) "Unfiled" else divider
                zos.putNextEntry(ZipEntry("$folder/note-${id.take(8)}.md"))
                zos.write(content.toByteArray(Charsets.UTF_8))
                zos.closeEntry()
            }
        }.toByteArray()
    }
}
