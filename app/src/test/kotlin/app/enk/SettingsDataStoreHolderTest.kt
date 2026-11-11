package app.enk

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/**
 * The activity is finished and reopened inside one process, and each time it builds a new container.
 * A second DataStore on the same file throws, so every container must share one.
 */
@RunWith(RobolectricTestRunner::class)
class SettingsDataStoreHolderTest {

    private val context: Context = ApplicationProvider.getApplicationContext()

    @Test
    fun oneContextAlwaysGetsTheSameDataStore() {
        assertSame(SettingsDataStoreHolder.get(context), SettingsDataStoreHolder.get(context))
    }

    @Test
    fun twoContainersInOneProcessShareTheSettingsFileWithoutThrowing() = runBlocking {
        val first = DefaultAppContainer(context)
        val second = DefaultAppContainer(context)

        first.settingsStore.update { it.copy(muted = true) }

        assertEquals(true, second.settingsStore.settings.first().muted)
    }
}
