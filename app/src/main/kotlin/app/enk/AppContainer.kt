package app.enk

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.core.handlers.ReplaceFileCorruptionHandler
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.preferencesDataStoreFile
import androidx.room.Room
import app.enk.audio.SoundManifest
import app.enk.audio.SoundPlayer
import app.enk.audio.SoundPoolPlayer
import app.enk.audio.SoundSet
import app.enk.data.EnkDatabase
import app.enk.data.NoteRepository
import app.enk.data.SearchRepository
import app.enk.data.SystemClock
import app.enk.settings.SettingsStore
import app.enk.writer.AndroidHaptics
import app.enk.writer.Haptics

interface AppContainer {
    val db: EnkDatabase
    val repo: NoteRepository
    val searchRepo: SearchRepository
    val settingsStore: SettingsStore
    val soundManifest: List<SoundSet>
    val soundPlayer: SoundPlayer
    val haptics: Haptics
}

class DefaultAppContainer(private val context: Context) : AppContainer {
    override val db: EnkDatabase by lazy {
        Room.databaseBuilder(context.applicationContext, EnkDatabase::class.java, "enk.db").build()
    }

    override val repo: NoteRepository by lazy {
        NoteRepository(db, SystemClock)
    }

    override val searchRepo: SearchRepository by lazy {
        SearchRepository(db)
    }

    override val settingsStore: SettingsStore by lazy {
        SettingsStore(SettingsDataStoreHolder.get(context.applicationContext))
    }

    override val soundManifest: List<SoundSet> by lazy {
        try {
            val json = context.applicationContext.assets.open("sounds/manifest.json").bufferedReader().use { it.readText() }
            SoundManifest.parse(json)
        } catch (_: Throwable) {
            emptyList()
        }
    }

    override val soundPlayer: SoundPlayer by lazy {
        SoundPoolPlayer(context.applicationContext)
    }

    override val haptics: Haptics by lazy {
        AndroidHaptics(context.applicationContext)
    }
}

/**
 * One DataStore per settings file for the whole process. DataStore throws when a second instance is
 * opened on the same file while the first is still active, and a container is built again whenever
 * the activity is finished and reopened (or the widget builds its own) while the process lives on.
 */
internal object SettingsDataStoreHolder {
    private val stores = HashMap<String, DataStore<Preferences>>()

    @Synchronized
    fun get(appContext: Context): DataStore<Preferences> {
        val settingsFile = appContext.preferencesDataStoreFile("settings")
        return stores.getOrPut(settingsFile.absolutePath) {
            PreferenceDataStoreFactory.create(
                corruptionHandler = ReplaceFileCorruptionHandler {
                    try { settingsFile.delete() } catch (_: Throwable) {}
                    emptyPreferences()
                },
                produceFile = { settingsFile }
            )
        }
    }
}
