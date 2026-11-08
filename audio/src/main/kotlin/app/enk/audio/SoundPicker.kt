package app.enk.audio

import kotlin.random.Random

class SoundPicker(seed: Long = 0) {
    private val rng = Random(seed)
    private var lastSample: Sample? = null

    fun nextKey(set: SoundSet): Sample {
        if (set.keys.isEmpty()) {
            throw IllegalArgumentException("SoundSet ${set.id} has no keys")
        }
        if (set.keys.size == 1) {
            val single = set.keys[0]
            lastSample = single
            return single
        }

        val candidates = if (lastSample != null) {
            val filtered = set.keys.filter { it != lastSample }
            if (filtered.isEmpty()) set.keys else filtered
        } else {
            set.keys
        }

        val picked = candidates[rng.nextInt(candidates.size)]
        lastSample = picked
        return picked
    }
}
