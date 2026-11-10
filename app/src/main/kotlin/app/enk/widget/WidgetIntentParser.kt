package app.enk.widget

import java.util.UUID

sealed interface WidgetIntentAction {
    data class NewNote(val tapId: String = UUID.randomUUID().toString()) : WidgetIntentAction
    data class OpenNote(val noteId: String) : WidgetIntentAction
    object None : WidgetIntentAction
}

object WidgetIntentParser {
    const val ACTION_NEW_NOTE = "app.enk.action.NEW_NOTE"
    const val ACTION_OPEN_NOTE = "app.enk.action.OPEN_NOTE"
    const val EXTRA_NOTE_ID = "app.enk.extra.NOTE_ID"
    const val EXTRA_TAP_ID = "app.enk.extra.TAP_ID"
    const val MAX_ID_LENGTH = 128

    fun parse(action: String?, extras: (String) -> Any?): WidgetIntentAction {
        return when (action) {
            ACTION_NEW_NOTE -> {
                val tapId = try { extras(EXTRA_TAP_ID) as? String } catch (_: Exception) { null }
                WidgetIntentAction.NewNote(tapId = tapId ?: UUID.randomUUID().toString())
            }
            ACTION_OPEN_NOTE -> {
                val rawId = try { extras(EXTRA_NOTE_ID) as? String } catch (_: Exception) { null }
                if (rawId != null && rawId.isNotBlank() && rawId.length <= MAX_ID_LENGTH) {
                    WidgetIntentAction.OpenNote(rawId)
                } else {
                    WidgetIntentAction.None
                }
            }
            else -> WidgetIntentAction.None
        }
    }
}
