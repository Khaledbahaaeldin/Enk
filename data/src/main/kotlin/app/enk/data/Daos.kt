package app.enk.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
interface DividerDao {
    @Insert
    suspend fun insert(d: DividerEntity): Long

    @Query("SELECT * FROM DividerEntity ORDER BY position ASC")
    suspend fun all(): List<DividerEntity>

    @Query("SELECT * FROM DividerEntity ORDER BY position ASC")
    fun observeAll(): Flow<List<DividerEntity>>

    @Query("UPDATE DividerEntity SET name = :name WHERE id = :id")
    suspend fun rename(id: Long, name: String)

    @Query("UPDATE DividerEntity SET position = :position WHERE id = :id")
    suspend fun setPosition(id: Long, position: Int)

    @Transaction
    suspend fun delete(id: Long) {
        moveNotesToUnfiled(id)
        deleteDivider(id)
    }

    @Query("UPDATE NoteEntity SET dividerId = NULL WHERE dividerId = :dividerId")
    suspend fun moveNotesToUnfiled(dividerId: Long)

    @Query("DELETE FROM DividerEntity WHERE id = :id")
    suspend fun deleteDivider(id: Long)
}

@Dao
interface NoteDao {
    @Upsert
    suspend fun upsert(n: NoteEntity)

    @Query(
        """
        SELECT * FROM NoteEntity 
        WHERE ((:dividerId IS NULL AND dividerId IS NULL) OR dividerId = :dividerId) 
          AND trashedAt IS NULL 
        ORDER BY position ASC
    """
    )
    fun observeByDivider(dividerId: Long?): Flow<List<NoteEntity>>

    @Query("SELECT * FROM NoteEntity WHERE id = :id")
    suspend fun get(id: String): NoteEntity?

    @Query("UPDATE NoteEntity SET trashedAt = :at WHERE id = :id")
    suspend fun trash(id: String, at: Long)

    @Query("UPDATE NoteEntity SET trashedAt = NULL WHERE id = :id")
    suspend fun restore(id: String)

    @Query(
        """
        UPDATE NoteEntity SET position = position + 1 
        WHERE ((:dividerId IS NULL AND dividerId IS NULL) OR dividerId = :dividerId)
          AND trashedAt IS NULL
    """
    )
    suspend fun shiftPositions(dividerId: Long?)

    @Query("UPDATE NoteEntity SET position = :position WHERE id = :id")
    suspend fun setPosition(id: String, position: Int)

    @Query("UPDATE NoteEntity SET dividerId = :dividerId, position = :position WHERE id = :id")
    suspend fun setDividerAndPosition(id: String, dividerId: Long?, position: Int)

    @Query("UPDATE NoteEntity SET modifiedAt = :modifiedAt WHERE id = :id")
    suspend fun updateModifiedAt(id: String, modifiedAt: Long)

    @Query("SELECT * FROM NoteEntity WHERE trashedAt IS NOT NULL AND trashedAt < :cutoff")
    suspend fun findTrashedBefore(cutoff: Long): List<NoteEntity>

    @Query("DELETE FROM NoteEntity WHERE trashedAt IS NOT NULL AND trashedAt < :cutoff")
    suspend fun purgeTrashedBefore(cutoff: Long): Int

    @Query("SELECT * FROM NoteEntity WHERE trashedAt IS NOT NULL ORDER BY trashedAt DESC")
    fun observeTrashed(): Flow<List<NoteEntity>>

    @Query("SELECT * FROM NoteEntity WHERE trashedAt IS NULL ORDER BY modifiedAt DESC, createdAt DESC, rowid DESC LIMIT 1")
    suspend fun recent(): NoteEntity?

    @Query("SELECT * FROM NoteEntity WHERE trashedAt IS NULL ORDER BY position ASC")
    suspend fun allActive(): List<NoteEntity>

    @Query("DELETE FROM NoteEntity WHERE id = :id")
    suspend fun delete(id: String)
}

@Dao
interface PageDao {
    @Upsert
    suspend fun upsert(p: PageEntity)

    @Query("SELECT * FROM PageEntity WHERE noteId = :noteId ORDER BY pageIndex ASC")
    suspend fun forNote(noteId: String): List<PageEntity>

    @Query("SELECT text FROM PageEntity WHERE noteId = :noteId AND pageIndex = 0")
    suspend fun firstPageText(noteId: String): String?

    @Query("SELECT COUNT(*) FROM PageEntity WHERE noteId = :noteId")
    suspend fun count(noteId: String): Int

    @Query("DELETE FROM PageEntity WHERE noteId = :noteId AND pageIndex >= :pageIndex")
    suspend fun deleteFrom(noteId: String, pageIndex: Int)

    @Query("DELETE FROM PageEntity WHERE noteId = :noteId")
    suspend fun deleteForNote(noteId: String)
}
