package app.enk.writer

import app.enk.text.Paginator

object TopBarFormatter {
    fun formatMeta(pageIndex: Int, chars: Int): String = "p.${pageIndex + 1} · $chars / ${Paginator.PAGE_SIZE}"
}
