package app.enk.data

import androidx.room.withTransaction
import app.enk.text.Paginator
import app.enk.text.StruckPage
import java.io.ByteArrayOutputStream
import java.io.InputStream
import java.nio.ByteBuffer
import java.nio.charset.CharacterCodingException
import java.nio.charset.CodingErrorAction
import java.time.Instant
import java.util.zip.ZipInputStream

sealed interface ImportResult {
    data class Done(val added: Int, val replaced: Int, val skipped: Int) : ImportResult
    data class Failed(val reason: String) : ImportResult
}

/**
 * Imports an archive made by [Exporter]. The whole import is validated first and written in one
 * transaction; any problem returns `Failed` and changes nothing.
 *
 * Decisions:
 * - A note is matched on `id`. A missing note is added; an existing note is replaced only when the
 *   file's `modified` is newer; otherwise it is skipped.
 * - A note that is in the trash stays there: its file is skipped, never restored.
 * - Dividers are matched by exact name or created; two dividers with the same name merge into one.
 *   A blank divider name means Unfiled.
 * - Pages are split again with [Paginator] when a hand-edited page is longer than the page size.
 */
class Importer(
    private val repo: NoteRepository,
    private val dividers: DividerDao
) {

    suspend fun import(input: InputStream): ImportResult {
        val parsedNotes = mutableListOf<ParsedNote>()
        val seenEntries = mutableSetOf<String>()
        val seenNoteIds = mutableSetOf<String>()

        var totalBytes = 0L
        var totalEntries = 0

        try {
            val zis = ZipInputStream(input, Charsets.UTF_8)
            val buffer = ByteArray(8192)

            while (true) {
                val entry = zis.nextEntry ?: break
                totalEntries++
                if (totalEntries > MAX_ENTRIES) {
                    return ImportResult.Failed("Archive exceeds maximum limit of $MAX_ENTRIES entries")
                }

                val entryName = entry.name
                if (isZipSlip(entryName)) {
                    return ImportResult.Failed("Zip slip detected in entry: $entryName")
                }

                if (entry.isDirectory || entryName.endsWith("/")) {
                    zis.closeEntry()
                    continue
                }

                if (!entryName.endsWith(".md", ignoreCase = true)) {
                    return ImportResult.Failed("Non-markdown file rejected: $entryName")
                }

                if (!seenEntries.add(entryName)) {
                    return ImportResult.Failed("Duplicate entry name in archive: $entryName")
                }

                val entryBaos = ByteArrayOutputStream()
                var entryBytesRead = 0L

                while (true) {
                    val read = zis.read(buffer)
                    if (read == -1) break
                    totalBytes += read
                    entryBytesRead += read

                    if (totalBytes > MAX_UNCOMPRESSED_BYTES || entryBytesRead > MAX_UNCOMPRESSED_BYTES) {
                        return ImportResult.Failed("Archive exceeds 50 MB uncompressed limit")
                    }
                    entryBaos.write(buffer, 0, read)
                }
                zis.closeEntry()

                val rawBytes = entryBaos.toByteArray()
                if (rawBytes.isEmpty()) {
                    return ImportResult.Failed("Empty file in archive: $entryName")
                }

                val decoder = Charsets.UTF_8.newDecoder()
                    .onMalformedInput(CodingErrorAction.REPORT)
                    .onUnmappableCharacter(CodingErrorAction.REPORT)

                val content = try {
                    decoder.decode(ByteBuffer.wrap(rawBytes)).toString()
                } catch (e: CharacterCodingException) {
                    return ImportResult.Failed("Invalid UTF-8 encoding in $entryName")
                }

                when (val parsed = parseNoteContent(entryName, content)) {
                    is ParseResult.Failure -> return ImportResult.Failed(parsed.reason)
                    is ParseResult.Success -> {
                        if (!seenNoteIds.add(parsed.note.id)) {
                            return ImportResult.Failed("Duplicate note id '${parsed.note.id}' in archive")
                        }
                        parsedNotes.add(parsed.note)
                    }
                }
            }
        } catch (e: kotlinx.coroutines.CancellationException) {
            throw e
        } catch (e: Exception) {
            return ImportResult.Failed("Failed to read archive: ${e.message}")
        }

        return repo.db.withTransaction {
            var added = 0
            var replaced = 0
            var skipped = 0

            val dividerCache = mutableMapOf<String, Long>()

            suspend fun resolveDivider(dividerName: String): Long? {
                if (dividerName.isBlank()) return null
                val cached = dividerCache[dividerName]
                if (cached != null) return cached

                val dbDividers = dividers.all()
                val match = dbDividers.firstOrNull { it.name == dividerName }
                if (match != null) {
                    dividerCache[dividerName] = match.id
                    return match.id
                }

                val nextPos = dbDividers.size
                val newId = dividers.insert(DividerEntity(name = dividerName, position = nextPos))
                dividerCache[dividerName] = newId
                return newId
            }

            for (note in parsedNotes) {
                val existing = repo.db.noteDao().get(note.id)
                if (existing == null) {
                    val dividerId = resolveDivider(note.divider)
                    repo.db.noteDao().shiftPositions(dividerId)
                    repo.db.noteDao().upsert(
                        NoteEntity(
                            id = note.id,
                            dividerId = dividerId,
                            position = 0,
                            createdAt = note.createdAt,
                            modifiedAt = note.modifiedAt,
                            trashedAt = null
                        )
                    )
                    saveNotePages(note.id, note.pages)
                    added++
                } else if (existing.trashedAt != null) {
                    // A note the user trashed stays in the trash; the file is skipped.
                    skipped++
                } else {
                    if (note.modifiedAt > existing.modifiedAt) {
                        val dividerId = resolveDivider(note.divider)
                        val moved = existing.dividerId != dividerId
                        if (moved) repo.db.noteDao().shiftPositions(dividerId)
                        repo.db.noteDao().upsert(
                            existing.copy(
                                dividerId = dividerId,
                                position = if (moved) 0 else existing.position,
                                createdAt = note.createdAt,
                                modifiedAt = note.modifiedAt,
                            )
                        )
                        saveNotePages(note.id, note.pages)
                        replaced++
                    } else {
                        skipped++
                    }
                }
            }

            ImportResult.Done(added = added, replaced = replaced, skipped = skipped)
        }
    }

    private suspend fun saveNotePages(noteId: String, importedPages: List<String>) {
        // Pages from a file may be longer than the writer allows (hand-edited files): split them with
        // the same Paginator the writer uses. A page already within the limit is kept as it is.
        val pages = importedPages.flatMap { if (it.isEmpty()) listOf(it) else Paginator.split(it) }
        repo.db.pageDao().deleteForNote(noteId)
        repo.db.pageFtsDao().deleteForNote(noteId)
        pages.forEachIndexed { idx, text ->
            repo.db.pageDao().upsert(
                PageEntity(
                    noteId = noteId,
                    pageIndex = idx,
                    text = text,
                    struck = ""
                )
            )
            val p = StruckPage(text)
            val searchable = p.searchable()
            if (searchable.isNotEmpty()) {
                repo.db.pageFtsDao().insert(
                    PageFts(
                        noteId = noteId,
                        pageIndex = idx,
                        text = searchable
                    )
                )
            }
        }
    }

    companion object {
        const val MAX_UNCOMPRESSED_BYTES = 50L * 1024 * 1024 // 50 MB
        const val MAX_ENTRIES = 20_000

        private val UNESCAPE_REGEX = Regex("""^(\s*)\\(\\*(?:<!-- enk:page -->|---)\s*)$""")

        fun isZipSlip(name: String): Boolean {
            if (name.startsWith("/") || name.startsWith("\\")) return true
            if (name.contains('\\') || name.contains(':')) return true
            val segments = name.split('/')
            for (segment in segments) {
                if (segment == ".." || segment == ".") return true
            }
            return false
        }

        /** Inverse of [Exporter.encodeDivider]; null when a quoted value is malformed. */
        fun decodeDivider(value: String): String? {
            if (!value.startsWith('"')) return value
            if (value.length < 2 || !value.endsWith('"')) return null
            val out = StringBuilder()
            var i = 1
            val end = value.length - 1
            while (i < end) {
                val c = value[i]
                if (c == '\\') {
                    if (i + 1 >= end) return null
                    when (value[i + 1]) {
                        '\\' -> out.append('\\')
                        '"' -> out.append('"')
                        'n' -> out.append('\n')
                        'r' -> out.append('\r')
                        else -> return null
                    }
                    i += 2
                } else if (c == '"') {
                    return null
                } else {
                    out.append(c)
                    i++
                }
            }
            return out.toString()
        }

        fun unescapeLine(line: String): String {
            val match = UNESCAPE_REGEX.matchEntire(line)
            return if (match != null) {
                "${match.groupValues[1]}${match.groupValues[2]}"
            } else {
                line
            }
        }

        private fun parseNoteContent(entryName: String, content: String): ParseResult {
            val lines = content.split('\n')
            if (lines.isEmpty() || lines[0].trim() != "---") {
                return ParseResult.Failure("Missing opening front matter delimiter '---' in $entryName")
            }

            var closingIndex = -1
            for (i in 1 until lines.size) {
                if (lines[i].trim() == "---") {
                    closingIndex = i
                    break
                }
            }

            if (closingIndex == -1) {
                return ParseResult.Failure("Unterminated front matter in $entryName")
            }

            val fmLines = lines.subList(1, closingIndex)
            var id: String? = null
            var divider: String = ""
            var createdStr: String? = null
            var modifiedStr: String? = null
            var pagesCount: Int? = null

            for (line in fmLines) {
                val trimmed = line.trim()
                if (trimmed.isEmpty()) continue
                val colonIdx = trimmed.indexOf(':')
                if (colonIdx == -1) {
                    return ParseResult.Failure("Malformed front matter line in $entryName: $line")
                }
                val key = trimmed.substring(0, colonIdx).trim()
                val value = trimmed.substring(colonIdx + 1).trim()
                when (key) {
                    "id" -> id = value.ifEmpty { null }
                    "divider" -> divider = decodeDivider(value) ?: return ParseResult.Failure("Malformed divider in $entryName: $value")
                    "created" -> createdStr = value
                    "modified" -> modifiedStr = value
                    "pages" -> pagesCount = value.toIntOrNull()
                }
            }

            if (id == null) return ParseResult.Failure("Missing or empty 'id' in $entryName")
            if (createdStr == null) return ParseResult.Failure("Missing 'created' in $entryName")
            if (modifiedStr == null) return ParseResult.Failure("Missing 'modified' in $entryName")
            if (pagesCount == null || pagesCount < 1) return ParseResult.Failure("Invalid or missing 'pages' count in $entryName")

            val created = try {
                Instant.parse(createdStr).toEpochMilli()
            } catch (e: Exception) {
                return ParseResult.Failure("Invalid 'created' date format in $entryName: $createdStr")
            }

            val modified = try {
                Instant.parse(modifiedStr).toEpochMilli()
            } catch (e: Exception) {
                return ParseResult.Failure("Invalid 'modified' date format in $entryName: $modifiedStr")
            }

            val bodyLines = lines.subList(closingIndex + 1, lines.size)

            val pages = mutableListOf<String>()
            var currentPageLines = mutableListOf<String>()

            for (line in bodyLines) {
                if (line.trim() == "<!-- enk:page -->") {
                    pages.add(currentPageLines.joinToString("\n") { unescapeLine(it) })
                    currentPageLines = mutableListOf()
                } else {
                    currentPageLines.add(line)
                }
            }
            pages.add(currentPageLines.joinToString("\n") { unescapeLine(it) })

            if (pages.size != pagesCount) {
                return ParseResult.Failure("Page count mismatch in $entryName: expected $pagesCount but found ${pages.size}")
            }

            return ParseResult.Success(
                ParsedNote(
                    id = id,
                    divider = divider,
                    createdAt = created,
                    modifiedAt = modified,
                    pages = pages
                )
            )
        }
    }
}

private data class ParsedNote(
    val id: String,
    val divider: String,
    val createdAt: Long,
    val modifiedAt: Long,
    val pages: List<String>
)

private sealed interface ParseResult {
    data class Success(val note: ParsedNote) : ParseResult
    data class Failure(val reason: String) : ParseResult
}
