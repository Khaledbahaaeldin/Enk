package app.enk.data

object StruckCodec {

    fun encode(ranges: List<IntRange>): String {
        if (ranges.isEmpty()) return ""
        return ranges.joinToString(",") { "${it.first}-${it.last}" }
    }

    fun decode(encoded: String?): List<IntRange> {
        if (encoded.isNullOrBlank()) return emptyList()
        val result = mutableListOf<IntRange>()
        val parts = encoded.split(',')
        for (part in parts) {
            val trimmed = part.trim()
            if (trimmed.isEmpty()) continue
            val dashIndex = trimmed.indexOf('-')
            if (dashIndex <= 0 || dashIndex >= trimmed.length - 1) continue
            val startStr = trimmed.substring(0, dashIndex)
            val endStr = trimmed.substring(dashIndex + 1)
            val start = startStr.toIntOrNull() ?: continue
            val end = endStr.toIntOrNull() ?: continue
            if (start <= end) {
                result.add(start..end)
            }
        }
        return result
    }
}
