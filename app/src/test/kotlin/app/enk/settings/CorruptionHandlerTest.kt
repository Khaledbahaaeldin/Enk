package app.enk.settings

import androidx.datastore.core.handlers.ReplaceFileCorruptionHandler
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.emptyPreferences
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File

class CorruptionHandlerTest {

    @get:Rule
    val tempFolder = TemporaryFolder()

    @Test
    fun corruptPreferencesFileRecoversToDefaults() = runBlocking {
        val file = File(tempFolder.root, "corrupted.preferences_pb")
        file.writeBytes(byteArrayOf(0xFF.toByte(), 0xFE.toByte(), 0xFD.toByte(), 0x00, 0x01, 0x02))

        val dataStore = PreferenceDataStoreFactory.create(
            corruptionHandler = ReplaceFileCorruptionHandler {
                try { file.delete() } catch (_: Throwable) {}
                emptyPreferences()
            },
            produceFile = { file }
        )
        val store = SettingsStore(dataStore)

        // Reading settings from corrupted file must return defaults without crashing
        val initialSettings = store.settings.first()
        assertEquals(Settings(), initialSettings)
        assertFalse(initialSettings.reduceEffects)

        // Writing updates after corruption replaces the corrupt file cleanly
        store.update { it.copy(reduceEffects = true) }
        val updated = store.settings.first()
        assertEquals(true, updated.reduceEffects)
    }
}
