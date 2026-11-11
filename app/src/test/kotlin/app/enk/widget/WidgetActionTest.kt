package app.enk.widget

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import app.enk.MainViewModel
import app.enk.shell.Route
import app.enk.testContainer
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class WidgetActionTest {

    private lateinit var context: Context

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
    }

    @Test
    fun newNoteActionCreatesExactlyOneNoteInUnfiledAndNavigates() = runBlocking {
        val container = testContainer(context, context.cacheDir)
        val vm = MainViewModel(context, container)
        vm.isLoaded.first { it }

        val initialNoteCount = container.repo.observeBox(null).first().size

        // Deliver NEW_NOTE action
        vm.handleWidgetAction(WidgetIntentAction.NewNote)
        vm.currentRoute.first { it is Route.Note }

        val notesAfter = container.repo.observeBox(null).first()
        assertEquals(initialNoteCount + 1, notesAfter.size)

        val currentRoute = vm.currentRoute.value
        assertTrue(currentRoute is Route.Note)
        assertEquals(notesAfter.first().id, (currentRoute as Route.Note).id)
    }

    @Test
    fun eachTapCreatesItsOwnNote() = runBlocking {
        val container = testContainer(context, context.cacheDir)
        val vm = MainViewModel(context, container)
        vm.isLoaded.first { it }

        val initialNoteCount = container.repo.observeBox(null).first().size

        vm.handleWidgetAction(WidgetIntentAction.NewNote)
        vm.currentRoute.first { it is Route.Note }

        vm.handleWidgetAction(WidgetIntentAction.NewNote)
        val notesAfter = container.repo.observeBox(null).first { it.size == initialNoteCount + 2 }
        assertEquals(initialNoteCount + 2, notesAfter.size)
    }

    @Test
    fun openNoteOpensExistingActiveNote() = runBlocking {
        val container = testContainer(context, context.cacheDir)
        val note = container.repo.createNote(dividerId = null)
        val vm = MainViewModel(context, container)
        vm.isLoaded.first { it }

        vm.handleWidgetAction(WidgetIntentAction.OpenNote(note.id))
        val currentRoute = vm.currentRoute.first { it is Route.Note && it.id == note.id }
        assertTrue(currentRoute is Route.Note)
        assertEquals(note.id, (currentRoute as Route.Note).id)
    }

    @Test
    fun openNoteOnTrashedOrMissingNoteFallsBackToRecentNote() = runBlocking {
        val container = testContainer(context, context.cacheDir)
        val activeNote = container.repo.createNote(dividerId = null)
        val trashedNote = container.repo.createNote(dividerId = null)
        container.repo.trash(trashedNote.id)

        val vm = MainViewModel(context, container)
        vm.isLoaded.first { it }

        // Attempt to open trashed note
        vm.handleWidgetAction(WidgetIntentAction.OpenNote(trashedNote.id))
        val currentRoute = vm.currentRoute.first { it is Route.Note && it.id == activeNote.id }
        assertTrue(currentRoute is Route.Note)
        assertEquals(activeNote.id, (currentRoute as Route.Note).id)

        // Attempt to open nonexistent note
        vm.handleWidgetAction(WidgetIntentAction.OpenNote("non-existent-id"))
        val fallbackRoute = vm.currentRoute.first { it is Route.Note && it.id == activeNote.id }
        assertTrue(fallbackRoute is Route.Note)
        assertEquals(activeNote.id, (fallbackRoute as Route.Note).id)
    }
}
