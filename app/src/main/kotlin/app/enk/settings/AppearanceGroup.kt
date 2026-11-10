package app.enk.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import app.enk.design.shell.GroupedDivider
import app.enk.design.shell.GroupedSection
import app.enk.design.shell.ShellSegmented
import app.enk.design.shell.ThemeTile
import app.enk.design.theme.ThemeId
import app.enk.design.theme.ThemeMode

/**
 * Appearance group per DS 12.8 / 12.10:
 * A 3 x 2 grid of theme tiles (Graphite, Apricot, Sky / Slate, Mono, Dynamic),
 * followed by a segmented control for Mode (Light, Dark, System).
 */
@Composable
fun AppearanceGroup(
    selectedTheme: ThemeId,
    onThemeSelect: (ThemeId) -> Unit,
    selectedMode: ThemeMode,
    onModeSelect: (ThemeMode) -> Unit,
    modifier: Modifier = Modifier
) {
    GroupedSection(
        title = "Appearance",
        footer = "The resolved mode also drives the typewriter's night lamp behavior.",
        modifier = modifier
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 14.dp, start = 12.dp, end = 12.dp, bottom = 8.dp)
        ) {
            // Row 1: Graphite, Apricot, Sky
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                THEME_TILE_ORDER.take(3).forEach { theme ->
                    ThemeTile(
                        theme = theme,
                        selected = theme == selectedTheme,
                        onClick = { onThemeSelect(theme) },
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Row 2: Slate, Mono, Dynamic
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                THEME_TILE_ORDER.drop(3).take(3).forEach { theme ->
                    ThemeTile(
                        theme = theme,
                        selected = theme == selectedTheme,
                        onClick = { onThemeSelect(theme) },
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }

        GroupedDivider()

        // Mode Segmented Control: Light, Dark, System
        Box(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
            val modeItems = MODE_ORDER.map { it.name }
            val selectedIndex = MODE_ORDER.indexOf(selectedMode).coerceAtLeast(0)
            ShellSegmented(
                items = modeItems,
                selected = selectedIndex,
                onSelect = { onModeSelect(MODE_ORDER[it]) }
            )
        }
    }
}
