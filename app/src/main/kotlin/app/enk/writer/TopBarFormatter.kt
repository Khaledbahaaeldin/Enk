package app.enk.writer

object TopBarFormatter {
    fun formatMeta(pageIndex: Int, chars: Int): String = "p.${pageIndex + 1} · $chars / 3000"
}
