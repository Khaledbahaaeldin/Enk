package app.enk.settings

import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.enk.design.shell.GroupedDivider
import app.enk.design.shell.GroupedRow
import app.enk.design.shell.GroupedSection
import app.enk.design.shell.ShellFader
import app.enk.design.shell.ShellSegmented
import app.enk.design.shell.ShellToggle
import app.enk.design.theme.LocalShellColors
import app.enk.design.theme.LocalShellMaterial
import app.enk.shell.Dest
import app.enk.shell.ShellScaffold
import kotlinx.coroutines.launch

@Composable
fun SettingsContent(
    settings: Settings,
    onSettingsChange: ((Settings) -> Settings) -> Unit,
    modifier: Modifier = Modifier,
    onExport: () -> Unit = {},
    onImport: () -> Unit = {},
    listState: LazyListState = rememberLazyListState(),
    showNav: Boolean = true,
    onNavigate: (Dest) -> Unit = {}
) {
    val colors = LocalShellColors.current
    val material = LocalShellMaterial.current

    ShellScaffold(
        title = "Setup",
        meta = "Enk 1.0",
        showNav = showNav,
        selected = Dest.Setup,
        onNavigate = onNavigate,
        listState = listState,
        modifier = modifier
    ) { _, contentPadding ->
        LazyColumn(
            state = listState,
            contentPadding = contentPadding,
            verticalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier.fillMaxSize()
        ) {
            // 1. Appearance Group
            item(key = "group_appearance") {
                AppearanceGroup(
                    selectedTheme = settings.theme,
                    onThemeSelect = { theme -> onSettingsChange { it.copy(theme = theme) } },
                    selectedMode = settings.themeMode,
                    onModeSelect = { mode -> onSettingsChange { it.copy(themeMode = mode) } }
                )
            }

            // 2. Writing Group
            item(key = "group_writing") {
                GroupedSection(title = "Writing") {
                    Box(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
                        val corrItems = listOf("Clean erase", "Strike-through")
                        val corrIndex = if (settings.correction == CorrectionMode.CleanErase) 0 else 1
                        ShellSegmented(
                            items = corrItems,
                            selected = corrIndex,
                            onSelect = { idx ->
                                val mode = if (idx == 0) CorrectionMode.CleanErase else CorrectionMode.StrikeThrough
                                onSettingsChange { it.copy(correction = mode) }
                            }
                        )
                    }

                    GroupedDivider()

                    GroupedRow(
                        title = "Autocorrect",
                        trailing = {
                            ShellToggle(
                                checked = settings.autocorrect,
                                onChange = { checked -> onSettingsChange { it.copy(autocorrect = checked) } }
                            )
                        }
                    )
                }
            }

            // 3. Sound Group
            item(key = "group_sound") {
                Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    GroupedSection(title = "Sound") {
                        val soundSets = SoundSetId.entries
                        soundSets.forEachIndexed { index, set ->
                            val isSelected = settings.soundSet == set
                            GroupedRow(
                                title = set.name,
                                onClick = { onSettingsChange { it.copy(soundSet = set) } },
                                trailing = {
                                    if (isSelected) {
                                        Box(
                                            modifier = Modifier
                                                .width(14.dp)
                                                .height(3.dp)
                                                .clip(RoundedCornerShape(2.dp))
                                                .background(colors.ui)
                                        )
                                    }
                                }
                            )
                            if (index < soundSets.size - 1) {
                                GroupedDivider()
                            }
                        }
                    }

                    // Secondary sound controls: Mute, Bell, Haptics
                    val shape = RoundedCornerShape(16.dp)
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp)
                            .clip(shape)
                            .background(colors.g1)
                    ) {
                        GroupedRow(
                            title = "Mute",
                            trailing = {
                                ShellToggle(
                                    checked = settings.muted,
                                    onChange = { checked -> onSettingsChange { it.copy(muted = checked) } }
                                )
                            }
                        )

                        GroupedDivider()

                        GroupedRow(
                            title = "Bell",
                            trailing = {
                                Box(modifier = Modifier.width(140.dp)) {
                                    ShellFader(
                                        value = settings.bellVolume,
                                        onChange = { vol -> onSettingsChange { it.copy(bellVolume = vol) } }
                                    )
                                }
                            }
                        )

                        GroupedDivider()

                        GroupedRow(
                            title = "Haptics",
                            trailing = {
                                ShellToggle(
                                    checked = settings.haptics,
                                    onChange = { checked -> onSettingsChange { it.copy(haptics = checked) } }
                                )
                            }
                        )
                    }
                }
            }

            // 4. Display Group
            item(key = "group_display") {
                GroupedSection(title = "Display") {
                    GroupedRow(
                        title = "Reduce effects",
                        trailing = {
                            ShellToggle(
                                checked = settings.reduceEffects,
                                onChange = { checked -> onSettingsChange { it.copy(reduceEffects = checked) } }
                            )
                        }
                    )
                }
            }

            // 5. Data Group
            item(key = "group_data") {
                GroupedSection(title = "Data") {
                    val buttonShape = RoundedCornerShape(15.dp)

                    GroupedRow(
                        title = "Export all notes",
                        trailing = {
                            Box(
                                modifier = Modifier
                                    .height(30.dp)
                                    .clip(buttonShape)
                                    .background(material.field2)
                                    .clickable(onClick = onExport)
                                    .padding(horizontal = 14.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "Export",
                                    color = colors.t1,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                    )

                    GroupedDivider()

                    GroupedRow(
                        title = "Import notes",
                        trailing = {
                            Box(
                                modifier = Modifier
                                    .height(30.dp)
                                    .clip(buttonShape)
                                    .background(material.field2)
                                    .clickable(onClick = onImport)
                                    .padding(horizontal = 14.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "Import",
                                    color = colors.t1,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                    )
                }
            }

            // 6. About Group
            item(key = "group_about") {
                GroupedSection(
                    title = "About",
                    footer = "Free and open source · Apache-2.0\nNo network, no tracking."
                ) {
                    GroupedRow(
                        title = "Enk",
                        trailing = {
                            Text(
                                text = "1.0.0",
                                color = colors.t2,
                                fontSize = 14.sp
                            )
                        }
                    )
                }
            }

            item(key = "bottom_spacer") {
                Spacer(modifier = Modifier.height(20.dp))
            }
        }
    }
}

@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel,
    onNavigate: (Dest) -> Unit,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val settings by viewModel.settings.collectAsState()
    val scope = rememberCoroutineScope()

    val exportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/zip")
    ) { uri ->
        if (uri != null) {
            scope.launch { viewModel.exportTo(uri) }
        }
    }

    val importLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            scope.launch { viewModel.importFrom(uri) }
        }
    }

    BackHandler {
        onNavigateBack()
    }

    SettingsContent(
        settings = settings,
        onSettingsChange = { change ->
            val updated = change(settings)
            if (updated.theme != settings.theme) viewModel.setTheme(updated.theme)
            if (updated.themeMode != settings.themeMode) viewModel.setThemeMode(updated.themeMode)
            if (updated.correction != settings.correction) viewModel.setCorrection(updated.correction)
            if (updated.autocorrect != settings.autocorrect) viewModel.setAutocorrect(updated.autocorrect)
            if (updated.soundSet != settings.soundSet) viewModel.setSoundSet(updated.soundSet)
            if (updated.muted != settings.muted) viewModel.setMuted(updated.muted)
            if (updated.bellVolume != settings.bellVolume) viewModel.setBellVolume(updated.bellVolume)
            if (updated.haptics != settings.haptics) viewModel.setHaptics(updated.haptics)
            if (updated.reduceEffects != settings.reduceEffects) viewModel.setReduceEffects(updated.reduceEffects)
        },
        onExport = { exportLauncher.launch("enk-export.zip") },
        onImport = { importLauncher.launch(arrayOf("application/zip", "application/octet-stream")) },
        onNavigate = onNavigate,
        modifier = modifier
    )
}
