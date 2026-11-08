package app.enk.data

import androidx.room.Dao
import androidx.room.Entity
import androidx.room.Fts4
import androidx.room.FtsOptions
import androidx.room.Insert
import androidx.room.Query

@Entity
// Only `text` is indexed: noteId and pageIndex are lookup keys, and indexing them made queries
// such as "0" or a UUID fragment match unrelated pages. unicode61 folds case and accents for
// every script, where the default tokenizer folds ASCII only.
@Fts4(tokenizer = FtsOptions.TOKENIZER_UNICODE61, notIndexed = ["noteId", "pageIndex"])
data class PageFts(
    val noteId: String,
    val pageIndex: Int,
    val text: String
)

data class PageFtsResult(
    val noteId: String,
    val pageIndex: Int,
    val text: String
)

@Dao
interface PageFtsDao {
    @Insert
    suspend fun insert(pageFts: PageFts)

    @Query("DELETE FROM PageFts WHERE noteId = :noteId")
    suspend fun deleteForNote(noteId: String)

    @Query(
        """
        SELECT PageFts.noteId, PageFts.pageIndex, PageFts.text 
        FROM PageFts 
        JOIN NoteEntity ON PageFts.noteId = NoteEntity.id 
        WHERE PageFts.text MATCH :query AND NoteEntity.trashedAt IS NULL
        LIMIT :limit
    """
    )
    suspend fun search(query: String, limit: Int = 50): List<PageFtsResult>
}
