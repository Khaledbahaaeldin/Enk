package app.enk.data

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(
    entities = [DividerEntity::class, NoteEntity::class, PageEntity::class, PageFts::class],
    version = 1,
    exportSchema = true
)
abstract class EnkDatabase : RoomDatabase() {
    abstract fun dividerDao(): DividerDao
    abstract fun noteDao(): NoteDao
    abstract fun pageDao(): PageDao
    abstract fun pageFtsDao(): PageFtsDao
}
