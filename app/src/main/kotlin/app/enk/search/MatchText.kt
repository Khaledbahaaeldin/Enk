package app.enk.search

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import java.util.Locale

/**
 * Marks each case-insensitive match of words in [query] with weight 700,
 * color #14110D, and an underline, with no background color (per DS 12.5).
 */
fun matchStyled(text: String, query: String): AnnotatedString {
    val trimmed = query.trim()
    if (trimmed.isEmpty() || text.isEmpty()) {
        return AnnotatedString(text)
    }

    val terms = trimmed.split("\\s+".toRegex()).filter { it.isNotEmpty() }
    if (terms.isEmpty()) {
        return AnnotatedString(text)
    }

    val lowerText = text.lowercase(Locale.ROOT)
    val intervals = mutableListOf<IntRange>()

    for (term in terms) {
        val lowerTerm = term.lowercase(Locale.ROOT)
        var startIndex = 0
        while (startIndex < lowerText.length) {
            val idx = lowerText.indexOf(lowerTerm, startIndex)
            if (idx == -1) break
            intervals.add(idx until (idx + lowerTerm.length))
            startIndex = idx + maxOf(1, lowerTerm.length)
        }
    }

    if (intervals.isEmpty()) {
        return AnnotatedString(text)
    }

    intervals.sortBy { it.first }
    val merged = mutableListOf<IntRange>()
    var current = intervals[0]
    for (i in 1 until intervals.size) {
        val next = intervals[i]
        if (next.first <= current.last + 1) {
            current = current.first..maxOf(current.last, next.last)
        } else {
            merged.add(current)
            current = next
        }
    }
    merged.add(current)

    return buildAnnotatedString {
        append(text)
        val style = SpanStyle(
            fontWeight = FontWeight.Bold,
            color = Color(0xFF14110D),
            textDecoration = TextDecoration.Underline,
            background = Color.Unspecified
        )
        for (range in merged) {
            val start = range.first.coerceIn(0, text.length)
            val end = (range.last + 1).coerceIn(0, text.length)
            if (start < end) {
                addStyle(style, start, end)
            }
        }
    }
}
