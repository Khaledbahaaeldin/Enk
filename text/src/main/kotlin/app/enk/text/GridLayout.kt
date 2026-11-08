package app.enk.text

data class LineSpan(val start: Int, val end: Int) // end exclusive; excludes the '\n'

data class CaretPos(val line: Int, val col: Int)

object GridLayout {
    fun wrap(text: String, cols: Int): List<LineSpan> {
        require(cols > 0) { "cols must be positive, was $cols" }

        val lines = mutableListOf<LineSpan>()
        var idx = 0
        val paragraphs = text.split('\n')

        for (p in paragraphs) {
            if (p.isEmpty()) {
                lines.add(LineSpan(idx, idx))
            } else {
                var i = 0
                while (i < p.length) {
                    if (p.length - i <= cols) {
                        lines.add(LineSpan(idx + i, idx + p.length))
                        break
                    }
                    val maxCut = i + cols
                    val sp = p.lastIndexOf(' ', startIndex = maxCut)
                    if (sp > i) {
                        lines.add(LineSpan(idx + i, idx + sp + 1))
                        i = sp + 1
                    } else {
                        var cut = maxCut
                        if (cut < p.length && Character.isHighSurrogate(p[cut - 1]) && Character.isLowSurrogate(p[cut])) {
                            cut--
                        }
                        lines.add(LineSpan(idx + i, idx + cut))
                        i = cut
                    }
                }
            }
            idx += p.length + 1
        }

        return lines
    }

    fun caret(lines: List<LineSpan>, index: Int, cols: Int): CaretPos {
        for (k in lines.indices.reversed()) {
            if (index >= lines[k].start) {
                return CaretPos(k, minOf(index - lines[k].start, cols))
            }
        }
        return CaretPos(0, 0)
    }
}
