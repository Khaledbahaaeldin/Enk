package app.enk.widget

import app.enk.data.NoteRepository
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

data class WidgetModel(
    val title: String?,
    val excerpt: String,
    val noteId: String?,
    val meta: String? = null
)

suspend fun buildWidgetModel(
    repo: NoteRepository,
    locale: Locale = Locale.getDefault(),
    zoneId: ZoneId = ZoneId.systemDefault()
): WidgetModel {
    val recentNote = repo.mostRecent()
    if (recentNote == null) {
        return WidgetModel(
            title = "Nothing yet",
            excerpt = "Tap to write your first note.",
            noteId = null,
            meta = null
        )
    }

    val page1Text = recentNote.pages.firstOrNull()?.text.orEmpty()
    val excerpt = formatExcerpt(page1Text)
    val meta = formatMetaLine(recentNote.modifiedAt, recentNote.pages.size, locale, zoneId)

    val firstNonEmptyLine = page1Text.lines()
        .map { it.trim() }
        .firstOrNull { it.isNotEmpty() }

    return WidgetModel(
        title = firstNonEmptyLine ?: "Empty note",
        excerpt = excerpt,
        noteId = recentNote.id,
        meta = meta
    )
}

fun formatExcerpt(text: String, maxChars: Int = 120): String {
    val nonBlankLines = text.lines()
        .map { it.trim() }
        .filter { it.isNotEmpty() }

    if (nonBlankLines.isEmpty()) {
        return "Empty note"
    }

    val raw = nonBlankLines.take(2).joinToString("\n")
    if (raw.length <= maxChars) {
        return raw
    }

    var cutIndex = maxChars
    // Ensure we do not split a surrogate pair
    if (Character.isHighSurrogate(raw[cutIndex - 1])) {
        cutIndex -= 1
    } else if (cutIndex < raw.length && Character.isLowSurrogate(raw[cutIndex])) {
        cutIndex -= 1
    }

    return raw.substring(0, cutIndex) + "…"
}

fun formatMetaLine(modifiedAt: Long, pageCount: Int, locale: Locale, zoneId: ZoneId): String {
    val dateTime = Instant.ofEpochMilli(modifiedAt).atZone(zoneId)
    val formatter = DateTimeFormatter.ofPattern("d MMM", locale)
    val dateStr = formatter.format(dateTime)
    val pages = if (pageCount <= 0) 1 else pageCount
    return "Recent · $dateStr · p.$pages"
}
