package app.enk.data

import app.enk.text.StruckPage
import java.io.OutputStream
import java.time.Instant
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

/**
 * Exporter creates a ZIP archive containing all non-trashed notes in Markdown format.
 *
 * Folder & File Structure:
 * - One directory per divider (e.g. "Journal/"), or "Unfiled/" for notes with no divider.
 * - Each note is saved as `<slug>-<first 8 of id>.md`.
 * - The `<slug>` is derived from the first non-empty line of page 1, sanitizing invalid
 *   filesystem characters (`/\:*?"<>|` and control codes) to `-`, falling back to `"untitled"`
 *   if blank or containing no letters/digits (emoji-only). Max length is 50 chars.
 *
 * File Format:
 * ---
 * id: <uuid>
 * divider: <name or empty>
 * created: <ISO-8601 instant>
 * modified: <ISO-8601 instant>
 * pages: <count>
 * ---
 * <page 1 text>
 * <!-- enk:page -->
 * <page 2 text>
 *
 * Page Marker & Front Matter Delimiter Escaping:
 * - Pages are separated by `\n<!-- enk:page -->\n`. Lines are split on `\n` only, so `\r` stays part of the text
 *   and CRLF and lone CR round-trip byte for byte.
 * - Struck letters are omitted from export (`StruckPage.searchable()`).
 * - To prevent collisions, any line in page content matching `^\s*\\*(<!-- enk:page -->|---)\s*$`
 *   is escaped by prepending a leading backslash `\`.
 * - When re-imported, lines starting with `\` followed by `\*<!-- enk:page -->` or `\*---` have
 *   one leading backslash stripped, faithfully restoring the original user text.
 */
class Exporter(
    private val repo: NoteRepository,
    private val dividers: DividerDao
) {

    suspend fun export(out: OutputStream) {
        val dividerMap = dividers.all().associate { it.id to it.name }
        val activeNotes = repo.db.noteDao().allActive()

        val zos = ZipOutputStream(out, Charsets.UTF_8)
        val usedPaths = mutableSetOf<String>()

        for (entity in activeNotes) {
            val note = repo.load(entity.id) ?: continue

            val dividerName = note.dividerId?.let { dividerMap[it] } ?: ""
            val folder = if (dividerName.isBlank()) "Unfiled" else sanitizePathSegment(dividerName)

            val firstPageText = note.pages.firstOrNull()?.exported() ?: ""
            val slug = generateSlug(firstPageText)
            val baseFilename = "$slug-${note.id.take(8)}.md"

            var finalPath = "$folder/$baseFilename"
            var counter = 1
            while (usedPaths.contains(finalPath)) {
                finalPath = "$folder/$slug-${note.id.take(8)}-$counter.md"
                counter++
            }
            usedPaths.add(finalPath)

            val content = buildNoteContent(note, dividerName)
            val bytes = content.toByteArray(Charsets.UTF_8)

            val entry = ZipEntry(finalPath)
            zos.putNextEntry(entry)
            zos.write(bytes)
            zos.closeEntry()
        }

        zos.finish()
        zos.flush()
    }

    companion object {
        private val ESCAPE_REGEX = Regex("""^(\s*)(\\*(?:<!-- enk:page -->|---)\s*)$""")

        fun escapePage(pageText: String): String {
            val lines = pageText.split('\n')
            return lines.joinToString("\n") { line ->
                val match = ESCAPE_REGEX.matchEntire(line)
                if (match != null) {
                    "${match.groupValues[1]}\\${match.groupValues[2]}"
                } else {
                    line
                }
            }
        }

        fun generateSlug(text: String): String {
            val firstNonEmpty = text.lines()
                .map { it.trim() }
                .firstOrNull { it.isNotEmpty() } ?: return "untitled"

            val sanitized = buildString {
                for (c in firstNonEmpty) {
                    if (c in "/\\:*?\"<>|" || c.isISOControl()) {
                        append('-')
                    } else {
                        append(c)
                    }
                }
            }.trim('-', ' ')

            if (sanitized.isEmpty() || sanitized.none { it.isLetterOrDigit() }) {
                return "untitled"
            }

            val truncated = sanitized.takeSafe(50).trimEnd('-', ' ')
            return if (truncated.isEmpty()) "untitled" else truncated
        }

        /**
         * The divider name in the front matter is the truth (the folder name is only a sanitised label).
         * A plain single-line name is written as is. A name with a line break, a leading or trailing
         * space, or a leading quote is written as a double-quoted string with `\\`, `\"`, `\n` and `\r`
         * escapes. A blank name is written as empty and imports as Unfiled.
         */
        fun encodeDivider(name: String): String {
            if (name.isBlank()) return ""
            val needsQuote = name.any { it == '\n' || it == '\r' } || name != name.trim() || name.startsWith('"')
            if (!needsQuote) return name
            val body = name
                .replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
            return "\"" + body + "\""
        }

        private fun String.takeSafe(n: Int): String {
            if (length <= n) return this
            val cut = if (Character.isHighSurrogate(this[n - 1])) n - 1 else n
            return substring(0, cut)
        }

        fun sanitizePathSegment(name: String): String {
            val sanitized = buildString {
                for (c in name) {
                    if (c in "/\\:*?\"<>|" || c.isISOControl()) {
                        append('-')
                    } else {
                        append(c)
                    }
                }
            }.trim('-', ' ', '.').takeSafe(50).trimEnd('-', ' ', '.')
            return if (sanitized.isEmpty()) "Unfiled" else sanitized
        }

        fun buildNoteContent(note: Note, dividerName: String): String {
            val exportedPages = note.pages.map { page ->
                escapePage(page.exported())
            }
            val effectivePages = if (exportedPages.isEmpty()) listOf("") else exportedPages

            return buildString {
                append("---\n")
                append("id: ${note.id}\n")
                append("divider: ${encodeDivider(dividerName)}\n")
                append("created: ${Instant.ofEpochMilli(note.createdAt)}\n")
                append("modified: ${Instant.ofEpochMilli(note.modifiedAt)}\n")
                append("pages: ${effectivePages.size}\n")
                append("---\n")
                append(effectivePages.joinToString("\n<!-- enk:page -->\n"))
            }
        }
    }
}
