package app.enk.writer

data class HintPolicy(
    val hasScrubbed: Boolean = false,
    val hasLongPressed: Boolean = false
) {
    val isDismissed: Boolean get() = hasScrubbed && hasLongPressed

    fun onScrub(): HintPolicy = if (hasScrubbed) this else copy(hasScrubbed = true)
    fun onLongPress(): HintPolicy = if (hasLongPressed) this else copy(hasLongPressed = true)
}
