package app.enk.writer

import app.enk.audio.SoundEvent

/**
 * Which sound one text change from the keyboard earns.
 *
 * Most phone keyboards keep the word being typed as composing text and only commit it on the
 * space, so a sound that waits for a commit stays silent for every letter. The sound therefore
 * follows the length of the composing text: longer is a key, shorter is a backspace.
 */
object InsertFeedback {

    fun soundFor(
        text: String,
        composing: Boolean,
        previousComposingLength: Int,
        wasComposing: Boolean
    ): SoundEvent? {
        val grew = text.length > previousComposingLength
        if (composing) {
            return when {
                grew -> SoundEvent.Key
                text.length < previousComposingLength -> SoundEvent.Backspace
                else -> null
            }
        }
        if (wasComposing && text.length < previousComposingLength) return SoundEvent.Backspace
        if (text.isEmpty()) return null
        val isPaste = !wasComposing && text.codePointCount(0, text.length) > 1
        if (isPaste) return null
        return when {
            text.last() == '\n' -> SoundEvent.Enter
            text.last() == ' ' -> SoundEvent.Space
            grew || !wasComposing -> SoundEvent.Key
            else -> null
        }
    }

    /** A key, space or enter strikes the slug; a backspace or silence does not. */
    fun strikes(sound: SoundEvent?): Boolean =
        sound == SoundEvent.Key || sound == SoundEvent.Space || sound == SoundEvent.Enter

    fun pulseMs(sound: SoundEvent?): Int = when (sound) {
        SoundEvent.Space, SoundEvent.Enter -> 14
        null -> 0
        else -> 8
    }
}
