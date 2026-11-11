package app.enk.widget

import android.content.Context
import app.enk.AppContainer
import app.enk.DefaultAppContainer

/**
 * The widget reads notes through one container for the life of the process. Building a container per
 * refresh opened a new Room database each time and never closed it.
 */
internal object WidgetContainer {
    @Volatile
    private var instance: AppContainer? = null

    fun get(context: Context): AppContainer =
        instance ?: synchronized(this) {
            instance ?: DefaultAppContainer(context.applicationContext).also { instance = it }
        }
}
