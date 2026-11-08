package app.enk.data

/** The first [maxChars] characters of this string, never ending between the halves of a surrogate pair. */
internal fun String.truncateSafely(maxChars: Int): String {
    if (length <= maxChars) return this
    var cut = maxChars
    if (cut in 1 until length && Character.isHighSurrogate(this[cut - 1]) && Character.isLowSurrogate(this[cut])) {
        cut--
    }
    return substring(0, cut)
}
