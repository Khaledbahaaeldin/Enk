package app.enk.widget

import android.content.Intent

sealed interface WidgetIntentAction {
    object NewNote : WidgetIntentAction
    data class OpenNote(val noteId: String) : WidgetIntentAction
    object None : WidgetIntentAction
}

/**
 * Turns the intent a widget (or any other app: the activity is exported) sends into an action.
 *
 * A widget's PendingIntent is the same object for every tap, so a tap cannot carry its own token.
 * What tells two taps apart is that each delivery is a new [android.content.Intent]; the activity
 * marks an intent [EXTRA_HANDLED] once it has acted on it, so the same intent object (for example
 * the one `getIntent()` keeps) is never acted on twice, while a new tap always is.
 */
object WidgetIntentParser {
    const val ACTION_NEW_NOTE = "app.enk.action.NEW_NOTE"
    const val ACTION_OPEN_NOTE = "app.enk.action.OPEN_NOTE"
    const val EXTRA_NOTE_ID = "app.enk.extra.NOTE_ID"
    const val EXTRA_HANDLED = "app.enk.extra.HANDLED"
    const val MAX_ID_LENGTH = 128

    fun parse(action: String?, extras: (String) -> Any?): WidgetIntentAction {
        if (action != ACTION_NEW_NOTE && action != ACTION_OPEN_NOTE) return WidgetIntentAction.None
        val handled = try { extras(EXTRA_HANDLED) == true } catch (_: Exception) { false }
        if (handled) return WidgetIntentAction.None
        return when (action) {
            ACTION_NEW_NOTE -> WidgetIntentAction.NewNote
            else -> {
                val rawId = try { extras(EXTRA_NOTE_ID) as? String } catch (_: Exception) { null }
                if (rawId != null && rawId.isNotBlank() && rawId.length <= MAX_ID_LENGTH) {
                    WidgetIntentAction.OpenNote(rawId)
                } else {
                    WidgetIntentAction.None
                }
            }
        }
    }

    /** Parses [intent] and, when it asks for something, marks it handled so it is never replayed. */
    fun consume(intent: Intent?): WidgetIntentAction {
        if (intent == null) return WidgetIntentAction.None
        val action = parse(intent.action) { intent.extras?.get(it) }
        if (action !is WidgetIntentAction.None) intent.putExtra(EXTRA_HANDLED, true)
        return action
    }
}
