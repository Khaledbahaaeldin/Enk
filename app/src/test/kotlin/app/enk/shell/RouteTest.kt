package app.enk.shell

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class RouteTest {

    @Test
    fun writerAndNoteSelectWrite() {
        assertEquals(Dest.Write, navSelection(Route.Writer))
        assertEquals(Dest.Write, navSelection(Route.Note("n", 0)))
    }

    @Test
    fun boxAndTrashSelectBox() {
        assertEquals(Dest.Box, navSelection(Route.Box))
        assertEquals(Dest.Box, navSelection(Route.Trash))
    }

    @Test
    fun findAndSetup() {
        assertEquals(Dest.Find, navSelection(Route.Find))
        assertEquals(Dest.Setup, navSelection(Route.Setup))
    }

    @Test
    fun navOrderIsWriteBoxFindSetup() {
        assertEquals(listOf("Write", "Box", "Find", "Setup"), Dest.values().map { it.name })
    }

    @Test
    fun backFromBoxGoesToWriter() {
        assertEquals(Route.Writer, routeBack(Route.Box))
    }

    @Test
    fun backFromFindGoesToWriter() {
        assertEquals(Route.Writer, routeBack(Route.Find))
    }

    @Test
    fun backFromSetupGoesToWriter() {
        assertEquals(Route.Writer, routeBack(Route.Setup))
    }

    @Test
    fun backFromTrashGoesToBox() {
        assertEquals(Route.Box, routeBack(Route.Trash))
    }

    @Test
    fun backFromNoteGoesToBox() {
        assertEquals(Route.Box, routeBack(Route.Note("n", 0)))
    }

    @Test
    fun backFromWriterIsUnhandled() {
        assertNull(routeBack(Route.Writer))
    }

    // Additional Review Focus & Edge Cases
    @Test
    fun noteWithDifferentPageAndIdSelectsWrite() {
        assertEquals(Dest.Write, navSelection(Route.Note("note-xyz", 42)))
    }

    @Test
    fun noteWithEmptyIdSelectsWrite() {
        assertEquals(Dest.Write, navSelection(Route.Note("", 0)))
    }

    @Test
    fun backFromNoteWithArbitraryPageGoesToBox() {
        assertEquals(Route.Box, routeBack(Route.Note("arbitrary-note-id", 99)))
    }

    @Test
    fun destCountIsExactlyFour() {
        assertEquals(4, Dest.values().size)
    }
}
