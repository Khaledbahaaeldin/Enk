package app.enk.audio

enum class SoundEvent {
    Key,
    Space,
    Enter,
    Backspace,
    Bell,
    LampClick,
    FaderTick
}

data class Sample(
    val file: String,
    val gain: Float = 1f
)

data class SoundSet(
    val id: String,
    val name: String,
    val keys: List<Sample>,
    val space: Sample,
    val enter: Sample,
    val backspace: Sample,
    val bell: Sample
)
