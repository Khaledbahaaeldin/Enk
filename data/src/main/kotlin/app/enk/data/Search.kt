package app.enk.data

data class SearchMatch(
    val noteId: String,
    val pageIndex: Int,
    val snippet: String,
    val matchRanges: List<IntRange>
)

typealias SearchHit = SearchMatch
typealias SearchRepository = SearchEngine

fun sanitizeQuery(raw: String): String? = SearchEngine.sanitizeQuery(raw).ifBlank { null }

class SearchEngine(private val db: EnkDatabase) {

    suspend fun search(query: String, limit: Int = 50): List<SearchMatch> {
        val sanitized = sanitizeQuery(query)
        if (sanitized.isBlank()) return emptyList()

        val results = db.pageFtsDao().search(sanitized, limit)
        if (results.isEmpty()) return emptyList()

        val queryTerms = sanitized.split(" ")
            .map { it.removeSuffix("*").lowercase() }
            .filter { it.isNotBlank() }

        return results.map { row ->
            createSearchMatch(row.noteId, row.pageIndex, row.text, queryTerms)
        }
    }

    private fun createSearchMatch(
        noteId: String,
        pageIndex: Int,
        text: String,
        queryTerms: List<String>
    ): SearchMatch {
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
            snippet = truncateSafely(text, 60)
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

        val snippetLower = snippet.lowercase()
        val matchRanges = mutableListOf<IntRange>()
        for (term in queryTerms) {
            var searchFrom = 0
            while (searchFrom < snippetLower.length) {
                val found = snippetLower.indexOf(term, searchFrom)
                if (found == -1) break
                matchRanges.add(found until (found + term.length))
                searchFrom = found + term.length
            }
        }

        return SearchMatch(noteId, pageIndex, snippet, matchRanges.distinct().sortedBy { it.first })
    }

    private fun truncateSafely(s: String, maxChars: Int): String {
        if (s.length <= maxChars) return s
        var cut = maxChars
        if (cut in 1 until s.length && Character.isHighSurrogate(s[cut - 1]) && Character.isLowSurrogate(s[cut])) {
            cut--
        }
        return s.substring(0, cut)
    }

    private fun adjustSurrogateBoundary(s: String, idx: Int): Int {
        if (idx <= 0 || idx >= s.length) return idx
        if (Character.isLowSurrogate(s[idx]) && Character.isHighSurrogate(s[idx - 1])) {
            return idx - 1
        }
        return idx
    }

    companion object {
        fun sanitizeQuery(raw: String): String {
            if (raw.isBlank()) return ""
            val reserved = setOf("and", "or", "not", "near")
            val cleaned = StringBuilder()
            for (ch in raw) {
                if (ch.isLetterOrDigit() || ch.isWhitespace()) {
                    cleaned.append(ch)
                } else {
                    cleaned.append(' ')
                }
            }
            val tokens = cleaned.toString()
                .split(Regex("\\s+"))
                .map { it.trim().lowercase() }
                .filter { it.isNotBlank() && it !in reserved }

            if (tokens.isEmpty()) return ""
            return tokens.joinToString(" ") { "$it*" }
        }
    }

    fun sanitizeQuery(raw: String): String = Companion.sanitizeQuery(raw)
}
