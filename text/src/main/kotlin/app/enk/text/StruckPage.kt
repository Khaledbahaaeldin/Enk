package app.enk.text

data class StruckPage(val text: String, val struck: List<IntRange> = emptyList()) {
    val length: Int get() = text.length

    fun searchable(): String {
        if (struck.isEmpty()) return text
        val sb = java.lang.StringBuilder(text.length)
        var current = 0
        val sorted = struck.sortedBy { it.first }
        for (range in sorted) {
            if (range.first > current) {
                val end = minOf(range.first, text.length)
                if (end > current) {
                    sb.append(text, current, end)
                }
            }
            current = maxOf(current, range.last + 1)
            if (current >= text.length) break
        }
        if (current < text.length) {
            sb.append(text, current, text.length)
        }
        return sb.toString()
    }

    fun exported(includeStruck: Boolean = false): String =
        if (includeStruck) text else searchable()

    fun withStruck(index: Int): StruckPage {
        if (index !in text.indices) return this
        val newRange = index..index
        val allRanges = (struck + listOf(newRange)).sortedBy { it.first }
        val merged = mutableListOf<IntRange>()
        for (r in allRanges) {
            if (merged.isEmpty()) {
                merged.add(r)
            } else {
                val last = merged.last()
                if (r.first <= last.last + 1) {
                    val newEnd = maxOf(last.last, r.last)
                    merged[merged.lastIndex] = last.first..newEnd
                } else {
                    merged.add(r)
                }
            }
        }
        return copy(struck = merged)
    }
}
