package app.enk.data

data class SearchHit(val noteId: String, val pageIndex: Int, val snippet: String)

/**
 * Turns user input into a safe FTS4 query: every character that is not a letter, digit or
 * whitespace becomes a separator, so no operator, quote or column filter survives. FTS4 operators
 * are uppercase only and the words are lowercased, so ordinary words such as "not" stay searchable.
 * The last word is a prefix match. Returns null when nothing searchable remains.
 */
fun sanitizeQuery(raw: String): String? {
    val folded = StringBuilder(raw.length)
    raw.codePoints().forEach { cp ->
        folded.appendCodePoint(if (Character.isLetterOrDigit(cp)) Character.toLowerCase(cp) else ' '.code)
    }
    val words = folded.split(' ').filter { it.isNotEmpty() }
    if (words.isEmpty()) return null
    return words.dropLast(1).plus(words.last() + "*").joinToString(" ")
}

open class SearchRepository(private val db: EnkDatabase) {

    open suspend fun search(query: String, limit: Int = 50): List<SearchHit> {
        val sanitized = sanitizeQuery(query) ?: return emptyList()

        val results = db.pageFtsDao().search(sanitized, limit)
        if (results.isEmpty()) return emptyList()

        val queryTerms = sanitized.split(" ").map { it.removeSuffix("*") }

        return results.map { row ->
            createHit(row.noteId, row.pageIndex, row.text, queryTerms)
        }
    }

    private fun createHit(
        noteId: String,
        pageIndex: Int,
        text: String,
        queryTerms: List<String>
    ): SearchHit {
        val lowerText = text.lowercase()
        var firstMatchStart = -1
        for (term in queryTerms) {
            val idx = lowerText.indexOf(term)
            if (idx >= 0 && (firstMatchStart == -1 || idx < firstMatchStart)) {
                firstMatchStart = idx
            }
        }

        val snippet: String

        if (firstMatchStart == -1) {
            snippet = text.truncateSafely(60)
        } else {
            val lineStart = text.lastIndexOf('\n', firstMatchStart).let { if (it == -1) 0 else it + 1 }
            val lineEnd = text.indexOf('\n', firstMatchStart).let { if (it == -1) text.length else it }
            val lineLength = lineEnd - lineStart

            if (lineLength <= 60) {
                snippet = text.substring(lineStart, lineEnd)
            } else {
                val start = maxOf(lineStart, firstMatchStart - 20)
                val end = minOf(lineEnd, start + 60)
                val safeStart = adjustSurrogateBoundary(text, start)
                val safeEnd = adjustSurrogateBoundary(text, end)
                snippet = text.substring(safeStart, safeEnd)
            }
        }

        return SearchHit(noteId, pageIndex, snippet)
    }

    private fun adjustSurrogateBoundary(s: String, idx: Int): Int {
        if (idx <= 0 || idx >= s.length) return idx
        if (Character.isLowSurrogate(s[idx]) && Character.isHighSurrogate(s[idx - 1])) {
            return idx - 1
        }
        return idx
    }
}
