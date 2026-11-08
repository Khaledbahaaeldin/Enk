package app.enk.text

object Paginator {
    const val PAGE_SIZE = 3000

    fun split(text: String, pageSize: Int = PAGE_SIZE): List<String> {
        require(pageSize > 0) { "pageSize must be positive, was $pageSize" }
        if (text.isEmpty()) return emptyList()

        val pages = mutableListOf<String>()
        var start = 0
        val len = text.length

        while (start < len) {
            val rem = len - start
            if (rem <= pageSize) {
                pages.add(text.substring(start, len))
                break
            }

            val limit = start + pageSize
            // Find last whitespace in [start until limit]
            var sp = -1
            for (i in (limit - 1) downTo start) {
                if (Character.isWhitespace(text[i])) {
                    sp = i
                    break
                }
            }

            var cut = if (sp != -1) {
                sp + 1
            } else {
                var c = limit
                if (c < len && Character.isHighSurrogate(text[c - 1]) && Character.isLowSurrogate(text[c])) {
                    c--
                }
                c
            }

            // Ensure forward progress
            if (cut <= start) {
                cut = minOf(len, start + if (Character.isHighSurrogate(text[start])) 2 else 1)
            }

            pages.add(text.substring(start, cut))
            start = cut
        }

        return pages
    }

    fun overflowBreak(pageText: String, pageSize: Int = PAGE_SIZE): Int? {
        if (pageText.length < pageSize) return null
        val windowStart = maxOf(0, pageText.length - 200)
        for (i in (pageText.length - 1) downTo windowStart) {
            if (Character.isWhitespace(pageText[i])) {
                return i + 1
            }
        }
        var cut = pageSize
        if (cut in 1 until pageText.length &&
            Character.isHighSurrogate(pageText[cut - 1]) &&
            Character.isLowSurrogate(pageText[cut])
        ) {
            cut--
        }
        return cut
    }
}
