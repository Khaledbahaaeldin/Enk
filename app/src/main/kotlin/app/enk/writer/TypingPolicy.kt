package app.enk.writer

object TypingPolicy {
    /** The caret stays solid for this long after the last key or backspace. */
    const val IDLE_MS: Long = 600

    fun isTyping(nowMs: Long, lastKeyMs: Long, dragging: Boolean): Boolean {
        if (dragging) return true
        return (nowMs - lastKeyMs) in 0..IDLE_MS
    }
}
