package app.enk.writer

object TypingPolicy {
    fun isTyping(nowMs: Long, lastKeyMs: Long, dragging: Boolean): Boolean {
        if (dragging) return true
        return (nowMs - lastKeyMs) in 0..600
    }
}
