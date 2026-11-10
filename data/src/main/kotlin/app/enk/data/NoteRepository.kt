package app.enk.data

import androidx.room.withTransaction
import app.enk.text.StruckPage
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.util.UUID

data class Note(
    val id: String,
    val dividerId: Long?,
    val pages: List<StruckPage>,
    val createdAt: Long,
    val modifiedAt: Long
)

data class NoteSummary(
    val id: String,
    val firstLines: String, // first two non-empty lines of page 1, each cut at 60 characters, joined by newline
    val pageCount: Int,
    val modifiedAt: Long
)

data class TrashedSummary(
    val id: String,
    val title: String,
    val trashedAt: Long,
    val daysRemaining: Int
)

class NoteRepository(
    internal val db: EnkDatabase,
    private val clock: Clock
) {

    suspend fun createNote(dividerId: Long?): Note {
        val id = UUID.randomUUID().toString()
        val now = clock.now()
        db.withTransaction {
            db.noteDao().shiftPositions(dividerId)
            db.noteDao().upsert(
                NoteEntity(
                    id = id,
                    dividerId = dividerId,
                    position = 0,
                    createdAt = now,
                    modifiedAt = now,
                    trashedAt = null
                )
            )
            db.pageDao().upsert(
                PageEntity(
                    noteId = id,
                    pageIndex = 0,
                    text = "",
                    struck = ""
                )
            )
        }
        return Note(
            id = id,
            dividerId = dividerId,
            pages = listOf(StruckPage("")),
            createdAt = now,
            modifiedAt = now
        )
    }

    suspend fun savePages(noteId: String, pages: List<StruckPage>) {
        val now = clock.now()
        db.withTransaction {
            db.pageDao().deleteForNote(noteId)
            db.pageFtsDao().deleteForNote(noteId)
            if (pages.isEmpty()) {
                db.pageDao().upsert(
                    PageEntity(
                        noteId = noteId,
                        pageIndex = 0,
                        text = "",
                        struck = ""
                    )
                )
            } else {
                pages.forEachIndexed { idx, p ->
                    db.pageDao().upsert(
                        PageEntity(
                            noteId = noteId,
                            pageIndex = idx,
                            text = p.text,
                            struck = StruckCodec.encode(p.struck)
                        )
                    )
                    val searchableText = p.searchable()
                    if (searchableText.isNotEmpty()) {
                        db.pageFtsDao().insert(
                            PageFts(
                                noteId = noteId,
                                pageIndex = idx,
                                text = searchableText
                            )
                        )
                    }
                }
            }
            db.noteDao().updateModifiedAt(noteId, now)
        }
    }

    suspend fun load(noteId: String): Note? {
        val entity = db.noteDao().get(noteId) ?: return null
        val pageEntities = db.pageDao().forNote(noteId)
        val pages = pageEntities.map {
            StruckPage(
                text = it.text,
                struck = StruckCodec.decode(it.struck)
            )
        }
        val effectivePages = if (pages.isEmpty()) listOf(StruckPage("")) else pages
        return Note(
            id = entity.id,
            dividerId = entity.dividerId,
            pages = effectivePages,
            createdAt = entity.createdAt,
            modifiedAt = entity.modifiedAt
        )
    }

    suspend fun moveToDivider(noteId: String, dividerId: Long?) {
        db.withTransaction {
            db.noteDao().shiftPositions(dividerId)
            db.noteDao().setDividerAndPosition(noteId, dividerId, 0)
        }
    }

    suspend fun reorderNotes(dividerId: Long?, orderedIds: List<String>) {
        db.withTransaction {
            orderedIds.forEachIndexed { index, id ->
                db.noteDao().setDividerAndPosition(id, dividerId, index)
            }
        }
    }

    suspend fun reorderDividers(orderedIds: List<Long>) {
        db.withTransaction {
            orderedIds.forEachIndexed { index, id ->
                db.dividerDao().setPosition(id, index)
            }
        }
    }

    suspend fun isTrashed(noteId: String): Boolean {
        return db.noteDao().get(noteId)?.trashedAt != null
    }

    suspend fun trash(noteId: String) {
        db.withTransaction {
            db.noteDao().trash(noteId, clock.now())
        }
    }

    suspend fun restore(noteId: String) {
        db.withTransaction {
            val note = db.noteDao().get(noteId) ?: return@withTransaction
            db.noteDao().restore(noteId)
            db.noteDao().shiftPositions(note.dividerId)
            db.noteDao().setPosition(noteId, 0)
        }
    }

    suspend fun purgeExpired(): Int {
        return db.withTransaction {
            val cutoff = clock.now() - TRASH_DAYS * MILLIS_PER_DAY
            val expired = db.noteDao().findTrashedBefore(cutoff)
            for (n in expired) {
                db.pageDao().deleteForNote(n.id)
                db.pageFtsDao().deleteForNote(n.id)
            }
            db.noteDao().purgeTrashedBefore(cutoff)
        }
    }

    suspend fun deleteAllForever(): Int {
        return db.withTransaction {
            val trashed = db.noteDao().findTrashedBefore(Long.MAX_VALUE)
            for (n in trashed) {
                db.pageDao().deleteForNote(n.id)
                db.pageFtsDao().deleteForNote(n.id)
            }
            db.noteDao().purgeTrashedBefore(Long.MAX_VALUE)
        }
    }

    fun observeBox(dividerId: Long?): Flow<List<NoteSummary>> {
        return db.noteDao().observeByDivider(dividerId).map { notes ->
            notes.map { note ->
                NoteSummary(
                    id = note.id,
                    firstLines = extractFirstLines(db.pageDao().firstPageText(note.id) ?: ""),
                    pageCount = maxOf(1, db.pageDao().count(note.id)),
                    modifiedAt = note.modifiedAt
                )
            }
        }
    }

    fun observeDividers(): Flow<List<DividerEntity>> = db.dividerDao().observeAll()

    fun observeTrashed(): Flow<List<TrashedSummary>> {
        return db.noteDao().observeTrashed().map { trashed ->
            trashed.map { note ->
                val title = extractTitle(db.pageDao().firstPageText(note.id) ?: "")
                val trashedAt = note.trashedAt ?: clock.now()
                val daysRemaining = maxOf(0, TRASH_DAYS - ((clock.now() - trashedAt) / MILLIS_PER_DAY).toInt())
                TrashedSummary(
                    id = note.id,
                    title = title,
                    trashedAt = trashedAt,
                    daysRemaining = daysRemaining
                )
            }
        }
    }

    suspend fun mostRecent(): Note? = db.noteDao().recent()?.let { load(it.id) }

    companion object {
        const val TRASH_DAYS = 30
        private const val MAX_LINE = 60
        private const val MILLIS_PER_DAY = 86_400_000L

        private fun extractFirstLines(text: String): String {
            val nonBlank = text.split('\n')
                .map { it.trim() }
                .filter { it.isNotEmpty() }
            return nonBlank.take(2).joinToString("\n") { it.truncateSafely(MAX_LINE) }
        }

        private fun extractTitle(text: String): String {
            val firstNonEmpty = text.split('\n')
                .map { it.trim() }
                .firstOrNull { it.isNotEmpty() }
            return firstNonEmpty?.let { it.truncateSafely(MAX_LINE) } ?: "Untitled"
        }
    }
}
