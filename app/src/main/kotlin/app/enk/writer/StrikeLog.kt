package app.enk.writer

class StrikeLog {
    sealed interface Entry { val startMs: Float }
    data class Key(val index: Int, override val startMs: Float, val weight: Float, val fast: Boolean) : Entry
    data class Erase(val text: String, val line: Int, val col: Int, override val startMs: Float) : Entry
    data class Struck(val index: Int, override val startMs: Float, val weight: Float, val fast: Boolean) : Entry

    private val keyAndStruckEntries = mutableListOf<Entry>()
    private val eraseEntries = mutableListOf<Erase>()
    private var lastStrikeStartMs: Float? = null

    var lastRecordedKey: Key? = null
        private set
    var lastRecordedStruck: Struck? = null
        private set
    var lastRecordedErase: Erase? = null
        private set

    private fun prune(nowMs: Float) {
        if (nowMs.isNaN() || nowMs < 0f) return

        keyAndStruckEntries.removeAll { entry ->
            val age = nowMs - entry.startMs
            age >= StrikeTiming.TOTAL_MS
        }
        while (keyAndStruckEntries.size > StrikeTiming.MAX_ANIMATING) {
            keyAndStruckEntries.removeAt(0)
        }

        eraseEntries.removeAll { entry ->
            val age = nowMs - entry.startMs
            age >= StrikeTiming.ERASE_WIPE_MS
        }
    }

    fun allKeys(): List<Key> = keyAndStruckEntries.filterIsInstance<Key>()
    fun allStruck(): List<Struck> = keyAndStruckEntries.filterIsInstance<Struck>()
    fun allErases(): List<Erase> = eraseEntries.toList()

    /** Records a strike. A Key or Struck entry's `fast` is NOT taken from the caller: pass the entry with fast = false and the log sets it. */
    fun recordKey(index: Int, weight: Float, nowMs: Float): Key {
        if (nowMs.isNaN() || nowMs < 0f) {
            return Key(index, nowMs, weight, false)
        }
        prune(nowMs)
        val isFast = lastStrikeStartMs?.let { nowMs - it < StrikeTiming.FAST_GAP_MS } ?: false
        lastStrikeStartMs = nowMs
        val entry = Key(index, nowMs, weight, isFast)
        lastRecordedKey = entry
        keyAndStruckEntries.add(entry)
        if (keyAndStruckEntries.size > StrikeTiming.MAX_ANIMATING) {
            keyAndStruckEntries.removeAt(0)
        }
        return entry
    }

    fun recordStruck(index: Int, weight: Float, nowMs: Float): Struck {
        if (nowMs.isNaN() || nowMs < 0f) {
            return Struck(index, nowMs, weight, false)
        }
        prune(nowMs)
        val isFast = lastStrikeStartMs?.let { nowMs - it < StrikeTiming.FAST_GAP_MS } ?: false
        lastStrikeStartMs = nowMs
        val entry = Struck(index, nowMs, weight, isFast)
        lastRecordedStruck = entry
        keyAndStruckEntries.add(entry)
        if (keyAndStruckEntries.size > StrikeTiming.MAX_ANIMATING) {
            keyAndStruckEntries.removeAt(0)
        }
        return entry
    }

    fun recordErase(text: String, line: Int, col: Int, nowMs: Float) {
        if (nowMs.isNaN() || nowMs < 0f) return
        prune(nowMs)
        val entry = Erase(text, line, col, nowMs)
        lastRecordedErase = entry
        eraseEntries.add(entry)
    }

    fun keyAt(index: Int, nowMs: Float): Key? {
        prune(nowMs)
        return keyAndStruckEntries.filterIsInstance<Key>().lastOrNull { it.index == index }
    }

    fun struckAt(index: Int, nowMs: Float): Struck? {
        prune(nowMs)
        return keyAndStruckEntries.filterIsInstance<Struck>().lastOrNull { it.index == index }
    }

    fun erases(nowMs: Float): List<Erase> {
        prune(nowMs)
        return eraseEntries.toList()
    }

    fun latestKey(nowMs: Float): Key? {
        prune(nowMs)
        val latest = keyAndStruckEntries.lastOrNull() ?: return null
        return when (latest) {
            is Key -> latest
            is Struck -> Key(latest.index, latest.startMs, latest.weight, latest.fast)
            is Erase -> null
        }
    }

    fun hasActive(nowMs: Float): Boolean {
        prune(nowMs)
        return keyAndStruckEntries.isNotEmpty() || eraseEntries.isNotEmpty()
    }

    fun clear() {
        keyAndStruckEntries.clear()
        eraseEntries.clear()
        lastStrikeStartMs = null
        lastRecordedKey = null
        lastRecordedStruck = null
        lastRecordedErase = null
    }
}
