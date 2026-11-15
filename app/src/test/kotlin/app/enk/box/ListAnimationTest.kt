package app.enk.box

import androidx.compose.animation.core.spring
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.getBoundsInRoot
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.unit.dp
import app.enk.data.NoteSummary
import app.enk.design.theme.EnkShellTheme
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class ListAnimationTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun removedNoteCardDisappearsAndCardsBelowMoveUp() {
        var notes by mutableStateOf(
            listOf(
                NoteSummary("n1", "Note 1 title\nBody 1", 1, 1000L),
                NoteSummary("n2", "Note 2 title\nBody 2", 1, 2000L),
                NoteSummary("n3", "Note 3 title\nBody 3", 1, 3000L)
            )
        )

        composeTestRule.setContent {
            EnkShellTheme {
                LazyColumn(modifier = Modifier.fillMaxWidth().height(600.dp)) {
                    items(notes, key = { it.id }) { note ->
                        NoteCard(
                            summary = note,
                            onClick = {},
                            onHold = {},
                            lifted = false,
                            modifier = Modifier
                                .animateItem(
                                    fadeInSpec = spring(1f, 400f),
                                    placementSpec = spring(1f, 400f),
                                    fadeOutSpec = spring(1f, 400f)
                                )
                                .fillMaxWidth()
                        )
                    }
                }
            }
        }
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithTag("note_card_n1").assertExists()
        composeTestRule.onNodeWithTag("note_card_n2").assertExists()
        composeTestRule.onNodeWithTag("note_card_n3").assertExists()

        val initialN3Y = composeTestRule.onNodeWithTag("note_card_n3").getBoundsInRoot().top

        // Remove n2
        notes = listOf(
            NoteSummary("n1", "Note 1 title\nBody 1", 1, 1000L),
            NoteSummary("n3", "Note 3 title\nBody 3", 1, 3000L)
        )
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithTag("note_card_n2").assertDoesNotExist()
        composeTestRule.onNodeWithTag("note_card_n1").assertExists()
        composeTestRule.onNodeWithTag("note_card_n3").assertExists()

        val newN3Y = composeTestRule.onNodeWithTag("note_card_n3").getBoundsInRoot().top
        assertTrue("Card n3 must move up after n2 is removed (new $newN3Y < initial $initialN3Y)", newN3Y < initialN3Y)
    }

    @Test
    fun emptyListRendersWithoutException() {
        val notes = emptyList<NoteSummary>()

        composeTestRule.setContent {
            EnkShellTheme {
                LazyColumn(modifier = Modifier.fillMaxWidth().height(600.dp)) {
                    items(notes, key = { it.id }) { note ->
                        NoteCard(
                            summary = note,
                            onClick = {},
                            onHold = {},
                            lifted = false,
                            modifier = Modifier
                                .animateItem(
                                    fadeInSpec = spring(1f, 400f),
                                    placementSpec = spring(1f, 400f),
                                    fadeOutSpec = spring(1f, 400f)
                                )
                                .fillMaxWidth()
                        )
                    }
                }
            }
        }
        composeTestRule.waitForIdle()
    }

    @Test
    fun singleItemListRendersAndRemovesWithoutException() {
        var notes by mutableStateOf(listOf(NoteSummary("n1", "Solo Note\nContent", 1, 1000L)))

        composeTestRule.setContent {
            EnkShellTheme {
                LazyColumn(modifier = Modifier.fillMaxWidth().height(600.dp)) {
                    items(notes, key = { it.id }) { note ->
                        NoteCard(
                            summary = note,
                            onClick = {},
                            onHold = {},
                            lifted = false,
                            modifier = Modifier
                                .animateItem(
                                    fadeInSpec = spring(1f, 400f),
                                    placementSpec = spring(1f, 400f),
                                    fadeOutSpec = spring(1f, 400f)
                                )
                                .fillMaxWidth()
                        )
                    }
                }
            }
        }
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("note_card_n1").assertExists()

        notes = emptyList()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("note_card_n1").assertDoesNotExist()
    }
}
