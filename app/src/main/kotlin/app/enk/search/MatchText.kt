package app.enk.search

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration

/** The ink of a match: near-black, the same on every theme because the card is always paper. */
internal val MatchInk = Color(0xFF14110D)

private fun splitsPair(text: String, start: Int, end: Int): Boolean =
    (start > 0 && text[start - 1].isHighSurrogate() && text[start].isLowSurrogate()) ||
        (end < text.length && text[end - 1].isHighSurrogate() && text[end].isLowSurrogate())

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

    val intervals = mutableListOf<IntRange>()

    // Compared in place, ignoring case, so a character whose lower case has another length can never
    // shift the positions; a match never starts or ends inside a surrogate pair.
    for (term in terms) {
        var i = 0
        while (i <= text.length - term.length) {
            if (text.regionMatches(i, term, 0, term.length, ignoreCase = true) && !splitsPair(text, i, i + term.length)) {
                intervals.add(i until (i + term.length))
                i += term.length
            } else {
                i++
            }
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
            color = MatchInk,
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
