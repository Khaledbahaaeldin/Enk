package app.enk.audio

class BellPolicy(private val cols: Int) {
    private var currentLine: Int? = null
    private var firedOnCurrentLine: Boolean = false

    fun onCaret(line: Int, col: Int): Boolean {
        if (cols <= 4) return false

        if (currentLine != line) {
            currentLine = line
            firedOnCurrentLine = false
        }

        if (!firedOnCurrentLine && col >= cols - 4) {
            firedOnCurrentLine = true
            return true
        }

        return false
    }
}
