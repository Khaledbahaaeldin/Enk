package app.enk

import android.content.Context
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.room.Room
import app.enk.audio.SoundEvent
import app.enk.audio.SoundPlayer
import app.enk.audio.SoundSet
import app.enk.data.EnkDatabase
import app.enk.data.NoteRepository
import app.enk.data.SearchRepository
import app.enk.data.SystemClock
import app.enk.settings.SettingsStore
import app.enk.writer.Haptics
import java.io.File
import java.util.concurrent.Executors

/** An in-memory container with silent audio and haptics, for tests that drive the real screens. */
fun testContainer(context: Context, settingsDir: File): AppContainer {
    // One thread for queries and transactions: Robolectric's SQLite ties a transaction to the thread
    // that began it, so a transaction that hops threads fails with "no current transaction".
    val executor = Executors.newSingleThreadExecutor()
    val database = Room.inMemoryDatabaseBuilder(context, EnkDatabase::class.java)
        .setQueryExecutor(executor)
        .setTransactionExecutor(executor)
        .build()
    val file = File(settingsDir, "settings_${System.nanoTime()}.preferences_pb")
    return object : AppContainer {
        override val db: EnkDatabase = database
        override val repo: NoteRepository = NoteRepository(database, SystemClock)
        override val searchRepo: SearchRepository = SearchRepository(database)
        override val settingsStore: SettingsStore = SettingsStore(PreferenceDataStoreFactory.create(produceFile = { file }))
        override val soundManifest: List<SoundSet> = emptyList()
        override val soundPlayer: SoundPlayer = object : SoundPlayer {
            override fun prepare(soundSet: SoundSet) {}
            override fun play(event: SoundEvent, soundSet: SoundSet, volume: Float) {}
            override fun release() {}
        }
        override val haptics: Haptics = object : Haptics {
            override fun pulse(ms: Int) {}
        }
    }
}
