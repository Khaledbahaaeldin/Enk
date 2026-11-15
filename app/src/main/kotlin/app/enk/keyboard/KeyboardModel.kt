package app.enk.keyboard

enum class PageId(val json: String) {
    Letters("letters"),
    Numbers("numbers"),
    Symbols("symbols");

    companion object {
        fun fromJson(json: String): PageId? = entries.firstOrNull { it.json == json }
    }
}

sealed interface KeySpec {
    val width: Float
}

data class CharKey(
    val label: String,
    val popup: List<String> = emptyList(),
    override val width: Float = 1f
) : KeySpec

data class ShiftKey(
    override val width: Float = 1.5f
) : KeySpec

data class BackspaceKey(
    override val width: Float = 1.5f
) : KeySpec

data class SpaceKey(
    override val width: Float = 4f
) : KeySpec

data class EnterKey(
    override val width: Float = 2f
) : KeySpec

data class PageKey(
    val target: PageId,
    val label: String,
    override val width: Float = 1.5f
) : KeySpec

data class GlobeKey(
    override val width: Float = 1f
) : KeySpec

data class SpacerKey(
    override val width: Float = 0.5f
) : KeySpec

data class KeyRow(val keys: List<KeySpec>)

data class KeyboardLayout(
    val id: String,
    val name: String,
    val pages: Map<PageId, List<KeyRow>>
)
