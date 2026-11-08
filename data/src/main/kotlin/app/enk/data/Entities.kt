package app.enk.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity
data class DividerEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val position: Int
)

@Entity
data class NoteEntity(
    @PrimaryKey val id: String,
    val dividerId: Long?,
    val position: Int,
    val createdAt: Long,
    val modifiedAt: Long,
    val trashedAt: Long?
)

@Entity(primaryKeys = ["noteId", "pageIndex"])
data class PageEntity(
    val noteId: String,
    val pageIndex: Int,
    val text: String,
    val struck: String // e.g. "1-2,7-7"; empty string if none
)
