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
    val firstLines: Pair<String, String>,
    val pageCount: Int,
    val modifiedAt: Long
)

data class TrashedSummary(
    val id: String,
    val title: String,
    val trashedAt: Long,
    val daysRemaining: Int
)

open class NoteRepository(
    protected val db: EnkDatabase,
    protected val clock: Clock
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
                }
            }
            db.noteDao().updateModifiedAt(noteId, now)
            onPagesUpdated(noteId, pages)
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
                db.noteDao().setPosition(id, index)
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

    suspend fun trash(noteId: String) {
        db.withTransaction {
            db.noteDao().trash(noteId, clock.now())
            onNoteTrashed(noteId)
        }
    }

    suspend fun restore(noteId: String) {
        db.withTransaction {
            val note = db.noteDao().get(noteId) ?: return@withTransaction
            db.noteDao().restore(noteId)
            db.noteDao().shiftPositions(note.dividerId)
            db.noteDao().setPosition(noteId, 0)
            onNoteRestored(noteId)
        }
    }

    suspend fun purgeExpired(days: Int = TRASH_DAYS): Int {
        return db.withTransaction {
            val cutoff = clock.now() - days * 86_400_000L
            val expired = db.noteDao().findTrashedBefore(cutoff)
            for (n in expired) {
                db.pageDao().deleteForNote(n.id)
                onNotePurged(n.id)
            }
            db.noteDao().purgeTrashedBefore(cutoff)
        }
    }

    fun observeNotes(dividerId: Long?): Flow<List<NoteSummary>> {
        return db.noteDao().observeByDivider(dividerId).map { notes ->
            notes.map { note ->
                val pages = db.pageDao().forNote(note.id)
                val firstPageText = pages.firstOrNull { it.pageIndex == 0 }?.text ?: ""
                val firstLines = extractFirstLines(firstPageText)
                val pageCount = maxOf(1, pages.size)
                NoteSummary(
                    id = note.id,
                    firstLines = firstLines,
                    pageCount = pageCount,
                    modifiedAt = note.modifiedAt
                )
            }
        }
    }

    fun observeBox(dividerId: Long?): Flow<List<NoteSummary>> = observeNotes(dividerId)

    fun observeDividers(): Flow<List<DividerEntity>> = db.dividerDao().observeAll()

    fun observeTrashed(): Flow<List<TrashedSummary>> {
        return db.noteDao().observeTrashed().map { trashed ->
            trashed.map { note ->
                val pages = db.pageDao().forNote(note.id)
                val firstPageText = pages.firstOrNull { it.pageIndex == 0 }?.text ?: ""
                val title = extractTitle(firstPageText)
                val trashedAt = note.trashedAt ?: clock.now()
                val daysRemaining = maxOf(0, TRASH_DAYS - ((clock.now() - trashedAt) / 86_400_000L).toInt())
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

    // Search hooks for Task 8:
    protected open suspend fun onPagesUpdated(noteId: String, pages: List<StruckPage>) {}
    protected open suspend fun onNoteTrashed(noteId: String) {}
    protected open suspend fun onNoteRestored(noteId: String) {}
    protected open suspend fun onNotePurged(noteId: String) {}

    companion object {
        const val TRASH_DAYS = 30

        private fun truncateLine(line: String, maxChars: Int = 60): String {
            if (line.length <= maxChars) return line
            var cut = maxChars
            if (cut in 1 until line.length &&
                Character.isHighSurrogate(line[cut - 1]) &&
                Character.isLowSurrogate(line[cut])
            ) {
                cut--
            }
            return line.substring(0, cut)
        }

        private fun extractFirstLines(text: String): Pair<String, String> {
            val nonBlank = text.split('\n')
                .map { it.trim() }
                .filter { it.isNotEmpty() }
            val first = nonBlank.getOrNull(0)?.let { truncateLine(it) } ?: ""
            val second = nonBlank.getOrNull(1)?.let { truncateLine(it) } ?: ""
            return Pair(first, second)
        }

        private fun extractTitle(text: String): String {
            val firstNonEmpty = text.split('\n')
                .map { it.trim() }
                .firstOrNull { it.isNotEmpty() }
            return firstNonEmpty?.let { truncateLine(it) } ?: "Untitled"
        }
    }
}
