package app.enk.settings

import app.enk.design.shell.LocalReduceEffects
import app.enk.design.motion.ShellMotion
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.animation.togetherWith
import androidx.compose.animation.fadeOut
import androidx.compose.animation.fadeIn
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.AnimatedContent
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.platform.testTag
import app.enk.shell.MessagePill
import kotlinx.coroutines.delay
import java.time.LocalDate
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import android.os.Build
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.enk.design.EnkColors
import app.enk.design.LocalMonoFont
import app.enk.design.effects.EffectsLevel
import app.enk.design.effects.FrostTuning
import app.enk.design.effects.GlassLook
import app.enk.design.effects.LiquidTuning
import app.enk.design.effects.resolveGlassLook
import app.enk.design.glass.GlassButton
import app.enk.design.glass.GlassSlider
import kotlin.math.roundToInt
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
    onOpenCredits: () -> Unit = {},
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
                    onModeSelect = { mode -> onSettingsChange { it.copy(themeMode = mode) } },
                    darkPaper = settings.darkPaper,
                    onDarkPaperSelect = { paper -> onSettingsChange { it.copy(darkPaper = paper) } },
                    modifier = Modifier.testTag("group_appearance").settingsEntrance(0)
                )
            }

            // 2. Writing Group
            item(key = "group_writing") {
                GroupedSection(title = "Writing", footer = WRITING_FOOTER, modifier = Modifier.settingsEntrance(1)) {
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

                    val isEnkKeyboard = settings.keyboard == KeyboardMode.Enk
                    val autocorrectAlpha = animateFloatAsState(
                        targetValue = if (isEnkKeyboard) 0.4f else 1f,
                        animationSpec = tween(160),
                        label = "autocorrect_alpha"
                    )
                    GroupedRow(
                        title = "Autocorrect",
                        subtitle = if (isEnkKeyboard) "Only with the system keyboard." else null,
                        modifier = Modifier.graphicsLayer { alpha = autocorrectAlpha.value },
                        trailing = {
                            ShellToggle(
                                checked = settings.autocorrect,
                                onChange = if (isEnkKeyboard) { {} } else { checked -> onSettingsChange { it.copy(autocorrect = checked) } }
                            )
                        }
                    )

                    GroupedDivider()

                    GroupedRow(
                        title = "Keyboard",
                        subtitle = "Enk's keyboard has no autocorrect and plays the typewriter sound itself."
                    )

                    Box(Modifier.padding(start = 16.dp, end = 16.dp, bottom = 10.dp)) {
                        ShellSegmented(
                            items = listOf("Enk", "System"),
                            selected = if (settings.keyboard == KeyboardMode.Enk) 0 else 1,
                            onSelect = {
                                onSettingsChange { s ->
                                    s.copy(keyboard = if (it == 0) KeyboardMode.Enk else KeyboardMode.System)
                                }
                            }
                        )
                    }
                }
            }

            // 3. Sound Group
            item(key = "group_sound") {
                Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    GroupedSection(title = "Sound", modifier = Modifier.settingsEntrance(2)) {
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

                        GroupedDivider()

                        // An app cannot switch another app's key click off; the keyboard owns it.
                        val context = androidx.compose.ui.platform.LocalContext.current
                        GroupedRow(
                            title = "Keyboard click",
                            subtitle = "Your keyboard has its own sound. Turn it off in its settings.",
                            onClick = {
                                try {
                                    context.startActivity(
                                        android.content.Intent(android.provider.Settings.ACTION_INPUT_METHOD_SETTINGS)
                                            .addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
                                    )
                                } catch (_: Exception) {
                                }
                            }
                        )
                    }
                }
            }

            // 4. Display Group
            item(key = "group_display") {
                val isBelowApi33 = Build.VERSION.SDK_INT < 33
                val effectiveLook = resolveGlassLook(settings.glassLook, Build.VERSION.SDK_INT)
                var advancedExpanded by remember { mutableStateOf(false) }

                GroupedSection(title = "Display", footer = DISPLAY_FOOTER, modifier = Modifier.settingsEntrance(3)) {
                    GroupedRow(
                        title = "Glass",
                        subtitle = "Choose how glass reflects and refracts."
                    )
                    Box(modifier = Modifier.padding(start = 16.dp, end = 16.dp, bottom = 10.dp)) {
                        ShellSegmented(
                            items = listOf("Liquid", "Frost"),
                            selected = if (effectiveLook == GlassLook.Liquid) 0 else 1,
                            enabled = { idx -> if (idx == 0) !isBelowApi33 else true },
                            onSelect = { idx ->
                                val chosen = if (idx == 0) GlassLook.Liquid else GlassLook.Frost
                                onSettingsChange { it.copy(glassLook = chosen) }
                            }
                        )
                    }
                    if (isBelowApi33) {
                        Text(
                            text = "Liquid glass needs Android 13 or later.",
                            fontSize = 12.sp,
                            color = colors.t2,
                            modifier = Modifier.padding(start = 16.dp, end = 16.dp, bottom = 10.dp)
                        )
                    }

                    GroupedDivider()

                    GroupedRow(
                        title = "Effects",
                        subtitle = "Auto picks what your phone can draw."
                    )
                    Box(modifier = Modifier.padding(start = 16.dp, end = 16.dp, bottom = 10.dp)) {
                        ShellSegmented(
                            items = listOf("Auto", "Full", "Balanced", "Light"),
                            selected = settings.effects.ordinal,
                            onSelect = { idx ->
                                onSettingsChange { it.copy(effects = EffectsLevel.entries[idx]) }
                            }
                        )
                    }

                    GroupedDivider()

                    val chevron = animateFloatAsState(
                        targetValue = if (advancedExpanded) 90f else 0f,
                        animationSpec = ShellMotion.default.toSpec(),
                        label = "advanced_chevron"
                    )
                    GroupedRow(
                        title = "Advanced",
                        onClick = { advancedExpanded = !advancedExpanded },
                        trailing = {
                            Text(
                                text = "›",
                                color = colors.t2,
                                fontSize = 18.sp,
                                modifier = Modifier.graphicsLayer { rotationZ = chevron.value }
                            )
                        }
                    )

                    AnimatedVisibility(
                        visible = advancedExpanded,
                        enter = fadeIn(ShellMotion.default.toSpec()),
                        exit = fadeOut(tween(90))
                    ) {
                        Column(
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                        ) {
                            AnimatedContent(
                                targetState = effectiveLook,
                                transitionSpec = {
                                    fadeIn(tween(140, delayMillis = 60)) togetherWith fadeOut(tween(90))
                                },
                                label = "advanced_look"
                            ) { look ->
                            Column {
                            if (look == GlassLook.Liquid) {
                                // Liquid sliders: Refraction, Lens depth, Colour fringe, Frost, Tint, Edge light
                                val refPct = ((settings.liquidTuning.refraction - 0f) / 2f * 100).roundToInt().coerceIn(0, 100)
                                AdvancedSliderRow(
                                    label = "Refraction",
                                    value = refPct,
                                    onValueChange = { pct ->
                                        onSettingsChange {
                                            it.copy(liquidTuning = it.liquidTuning.copy(refraction = pct / 100f * 2f))
                                        }
                                    }
                                )

                                val depthPct = ((settings.liquidTuning.lensDepth - 0.5f) / 1.5f * 100).roundToInt().coerceIn(0, 100)
                                AdvancedSliderRow(
                                    label = "Lens depth",
                                    value = depthPct,
                                    onValueChange = { pct ->
                                        onSettingsChange {
                                            it.copy(liquidTuning = it.liquidTuning.copy(lensDepth = 0.5f + (pct / 100f) * 1.5f))
                                        }
                                    }
                                )

                                val fringePct = ((settings.liquidTuning.fringe - 0f) / 2f * 100).roundToInt().coerceIn(0, 100)
                                AdvancedSliderRow(
                                    label = "Colour fringe",
                                    value = fringePct,
                                    onValueChange = { pct ->
                                        onSettingsChange {
                                            it.copy(liquidTuning = it.liquidTuning.copy(fringe = pct / 100f * 2f))
                                        }
                                    }
                                )

                                val frostPct = ((settings.liquidTuning.frostDp - 0f) / 12f * 100).roundToInt().coerceIn(0, 100)
                                AdvancedSliderRow(
                                    label = "Frost",
                                    value = frostPct,
                                    onValueChange = { pct ->
                                        onSettingsChange {
                                            it.copy(liquidTuning = it.liquidTuning.copy(frostDp = pct / 100f * 12f))
                                        }
                                    }
                                )

                                val tintPct = ((settings.liquidTuning.tint - 0f) / 0.4f * 100).roundToInt().coerceIn(0, 100)
                                AdvancedSliderRow(
                                    label = "Tint",
                                    value = tintPct,
                                    onValueChange = { pct ->
                                        onSettingsChange {
                                            it.copy(liquidTuning = it.liquidTuning.copy(tint = pct / 100f * 0.4f))
                                        }
                                    }
                                )

                                val edgePct = ((settings.liquidTuning.edgeLight - 0f) / 1.5f * 100).roundToInt().coerceIn(0, 100)
                                AdvancedSliderRow(
                                    label = "Edge light",
                                    value = edgePct,
                                    onValueChange = { pct ->
                                        onSettingsChange {
                                            it.copy(liquidTuning = it.liquidTuning.copy(edgeLight = pct / 100f * 1.5f))
                                        }
                                    }
                                )
                            } else {
                                // Frost sliders: Frost, Tint, Edge light
                                val frostPct = ((settings.frostTuning.blur - 0.5f) / 1f * 100).roundToInt().coerceIn(0, 100)
                                AdvancedSliderRow(
                                    label = "Frost",
                                    value = frostPct,
                                    onValueChange = { pct ->
                                        onSettingsChange {
                                            it.copy(frostTuning = it.frostTuning.copy(blur = 0.5f + (pct / 100f) * 1f))
                                        }
                                    }
                                )

                                val tintPct = ((settings.frostTuning.tint - 0.5f) / 1f * 100).roundToInt().coerceIn(0, 100)
                                AdvancedSliderRow(
                                    label = "Tint",
                                    value = tintPct,
                                    onValueChange = { pct ->
                                        onSettingsChange {
                                            it.copy(frostTuning = it.frostTuning.copy(tint = 0.5f + (pct / 100f) * 1f))
                                        }
                                    }
                                )

                                val edgePct = ((settings.frostTuning.edgeLight - 0f) / 1.5f * 100).roundToInt().coerceIn(0, 100)
                                AdvancedSliderRow(
                                    label = "Edge light",
                                    value = edgePct,
                                    onValueChange = { pct ->
                                        onSettingsChange {
                                            it.copy(frostTuning = it.frostTuning.copy(edgeLight = pct / 100f * 1.5f))
                                        }
                                    }
                                )
                            }
                            }
                            }

                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "Reset to default",
                                color = EnkColors.accent,
                                fontSize = 14.sp,
                                modifier = Modifier
                                    .clickable {
                                        if (effectiveLook == GlassLook.Liquid) {
                                            onSettingsChange { it.copy(liquidTuning = LiquidTuning()) }
                                        } else {
                                            onSettingsChange { it.copy(frostTuning = FrostTuning()) }
                                        }
                                    }
                                    .padding(vertical = 8.dp)
                            )
                        }
                    }
                }
            }

            // 5. Data Group
            item(key = "group_data") {
                GroupedSection(title = "Data", modifier = Modifier.settingsEntrance(4)) {
                    GroupedRow(
                        title = "Export all notes",
                        trailing = {
                            GlassButton(
                                onClick = onExport,
                                modifier = Modifier.height(36.dp)
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
                            GlassButton(
                                onClick = onImport,
                                modifier = Modifier.height(36.dp)
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
                    modifier = Modifier.settingsEntrance(5),
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

                    GroupedDivider()

                    GroupedRow(
                        title = "Credits and licenses",
                        onClick = onOpenCredits,
                        trailing = {
                            Text(
                                text = "›",
                                color = colors.t2,
                                fontSize = 18.sp
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
    onOpenCredits: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val settings by viewModel.settings.collectAsState()
    val transfer by viewModel.transfer.collectAsState()

    val exportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/zip")
    ) { uri -> if (uri != null) viewModel.exportNotes(uri) }

    val importLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri -> if (uri != null) viewModel.importNotes(uri) }

    BackHandler { onNavigateBack() }

    Box(modifier = modifier.fillMaxSize()) {
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
                if (updated.effects != settings.effects) viewModel.setEffects(updated.effects)
                if (updated.glassLook != settings.glassLook) viewModel.setGlassLook(updated.glassLook)
                if (updated.liquidTuning != settings.liquidTuning) viewModel.setLiquidTuning(updated.liquidTuning)
                if (updated.frostTuning != settings.frostTuning) viewModel.setFrostTuning(updated.frostTuning)
                if (updated.darkPaper != settings.darkPaper) viewModel.setDarkPaper(updated.darkPaper)
                if (updated.keyboard != settings.keyboard) viewModel.setKeyboard(updated.keyboard)
            },
            onExport = { exportLauncher.launch(exportFileName(LocalDate.now())) },
            onImport = { importLauncher.launch(arrayOf("application/zip", "application/octet-stream")) },
            onOpenCredits = onOpenCredits,
            onNavigate = onNavigate,
            modifier = Modifier.fillMaxSize()
        )

        val message = when (val t = transfer) {
            TransferState.Idle -> null
            is TransferState.Running -> t.message
            is TransferState.Done -> t.message
            is TransferState.Failed -> t.message
        }
        if (message != null) {
            if (transfer !is TransferState.Running) {
                LaunchedEffect(transfer) {
                    delay(TRANSFER_MESSAGE_MS)
                    viewModel.clearTransfer()
                }
            }
            MessagePill(
                text = message,
                tag = "transfer_message",
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .windowInsetsPadding(WindowInsets.navigationBars)
                    .padding(bottom = 110.dp, start = 24.dp, end = 24.dp)
            )
        }
    }
}

internal const val TRANSFER_MESSAGE_MS = 4_000L

/** Design system 12.10 footers. */
internal const val APPEARANCE_FOOTER =
    "Themes colour menus and controls. The writing page keeps its paper and steel; the lamp is its night mode. Dynamic follows your Android wallpaper."
internal const val WRITING_FOOTER = "Strike-through keeps deleted letters on the paper, as a typewriter would."
internal const val DISPLAY_FOOTER =
    "Liquid bends light like a drop of water; Frost blurs it. Effects sets how much the phone may spend: Light is solid and still."

@Composable
private fun AdvancedSliderRow(
    label: String,
    value: Int,
    onValueChange: (Int) -> Unit
) {
    val colors = LocalShellColors.current
    val monoFont = LocalMonoFont.current
    Column(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(text = label, color = colors.t2, fontSize = 13.sp)
            Text(text = "$value%", color = colors.t1, fontSize = 12.sp, fontFamily = monoFont)
        }
        Spacer(modifier = Modifier.height(2.dp))
        GlassSlider(
            value = value,
            min = 0,
            max = 100,
            onValueChange = onValueChange,
            onDetent = {},
            accent = EnkColors.accent,
            label = label
        )
    }
}

/** Session flag: Setup's groups rise in once per app session, not on every tab revisit. */
private object SettingsEntrance {
    var played = false
}

@Composable
private fun Modifier.settingsEntrance(index: Int): Modifier {
    val skip = SettingsEntrance.played || LocalReduceEffects.current
    val progress = remember { Animatable(if (skip) 1f else 0f) }
    LaunchedEffect(Unit) {
        if (!skip) {
            delay(index * 25L)
            progress.animateTo(1f, ShellMotion.default.toSpec())
        }
    }
    LaunchedEffect(Unit) {
        delay(900)
        SettingsEntrance.played = true
    }
    val rise = with(LocalDensity.current) { 8.dp.toPx() }
    return this.graphicsLayer {
        alpha = progress.value
        translationY = rise * (1f - progress.value)
    }
}
