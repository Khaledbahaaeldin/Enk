package app.enk.about

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.BasicAlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.enk.design.shell.GroupedDivider
import app.enk.design.shell.GroupedRow
import app.enk.design.shell.GroupedSection
import app.enk.design.theme.LocalShellColors
import app.enk.design.theme.LocalShellMaterial
import app.enk.shell.Dest
import app.enk.shell.MessagePill
import app.enk.shell.ShellScaffold
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AboutScreen(
    onNavigate: (Dest) -> Unit,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier,
    listState: LazyListState = rememberLazyListState()
) {
    val context = LocalContext.current
    val colors = LocalShellColors.current
    val material = LocalShellMaterial.current

    val credits = remember(context) {
        try {
            context.assets.open("credits.json").bufferedReader().use { it.readText() }.let { loadCredits(it) }
        } catch (_: Exception) {
            emptyList()
        }
    }

    val libraries = remember(context) {
        try {
            context.assets.open("libraries.csv").bufferedReader().use { it.readText() }.let { loadLibraries(it) }
        } catch (_: Exception) {
            emptyList()
        }
    }

    val soundCredits = remember(credits) { groupSoundCredits(credits) }
    var shownLicense by remember { mutableStateOf<LicenseDoc?>(null) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    fun openUrl(url: String) {
        if (url.isBlank()) return
        try {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url)).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        } catch (_: ActivityNotFoundException) {
            errorMessage = "No browser available to open link"
        } catch (_: Exception) {
            errorMessage = "Could not open link"
        }
    }

    BackHandler { onNavigateBack() }

    Box(modifier = modifier.fillMaxSize()) {
        ShellScaffold(
            title = "Credits",
            meta = "Licenses & Sources",
            showNav = true,
            selected = Dest.Setup,
            onNavigate = onNavigate,
            listState = listState,
            modifier = Modifier.fillMaxSize()
        ) { _, contentPadding ->
            LazyColumn(
                state = listState,
                contentPadding = contentPadding,
                verticalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                // 1. App License Group
                item(key = "group_app_license") {
                    GroupedSection(
                        title = "App License",
                        modifier = Modifier.testTag("group_app_license"),
                        footer = "Enk is free and open-source software."
                    ) {
                        GroupedRow(
                            title = LicenseDocs.App.title,
                            subtitle = "Tap to read the full license",
                            onClick = { shownLicense = LicenseDocs.App },
                            trailing = {
                                Text(text = "›", color = colors.t2, fontSize = 18.sp)
                            }
                        )
                    }
                }

                // 2. Fonts Group
                item(key = "group_fonts") {
                    GroupedSection(
                        title = "Fonts",
                        footer = "Both typefaces are licensed under the SIL Open Font License, Version 1.1."
                    ) {
                        GroupedRow(
                            title = LicenseDocs.CourierPrime.title,
                            subtitle = "The Courier Prime Project Authors · SIL OFL 1.1",
                            onClick = { shownLicense = LicenseDocs.CourierPrime },
                            trailing = { Text(text = "›", color = colors.t2, fontSize = 18.sp) }
                        )
                        GroupedDivider()
                        GroupedRow(
                            title = LicenseDocs.Inter.title,
                            subtitle = "The Inter Project Authors · SIL OFL 1.1",
                            onClick = { shownLicense = LicenseDocs.Inter },
                            trailing = { Text(text = "›", color = colors.t2, fontSize = 18.sp) }
                        )
                    }
                }

                // 3. Sounds Group
                if (soundCredits.isNotEmpty()) {
                    item(key = "group_sounds") {
                        GroupedSection(
                            title = "Sounds",
                            footer = "Typewriter and keyboard recordings, each shared by its author."
                        ) {
                            soundCredits.forEachIndexed { index, credit ->
                                if (index > 0) GroupedDivider()
                                GroupedRow(
                                    title = credit.author,
                                    subtitle = "${credit.usedIn} · ${credit.license}",
                                    onClick = { openUrl(credit.source) },
                                    trailing = { Text(text = "↗", color = colors.t2, fontSize = 14.sp) }
                                )
                            }
                        }
                    }
                }

                // 4. Libraries Group
                if (libraries.isNotEmpty()) {
                    item(key = "group_libraries") {
                        GroupedSection(
                            title = "Libraries",
                            footer = "Third-party open-source components and dependencies."
                        ) {
                            libraries.forEachIndexed { index, lib ->
                                if (index > 0) GroupedDivider()
                                GroupedRow(
                                    title = lib.name,
                                    subtitle = "${lib.version} · ${lib.license}",
                                    onClick = { openUrl(lib.sourceUrl) },
                                    trailing = { Text(text = "↗", color = colors.t2, fontSize = 14.sp) }
                                )
                            }
                        }
                    }
                }

                item(key = "bottom_spacer") {
                    Spacer(modifier = Modifier.height(20.dp))
                }
            }
        }

        shownLicense?.let { doc ->
            LicenseDialog(doc = doc, onClose = { shownLicense = null })
        }

        if (errorMessage != null) {
            LaunchedEffect(errorMessage) {
                delay(3000L)
                errorMessage = null
            }
            MessagePill(
                text = errorMessage ?: "",
                tag = "credits_error_message",
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .windowInsetsPadding(WindowInsets.navigationBars)
                    .padding(bottom = 110.dp, start = 24.dp, end = 24.dp)
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun LicenseDialog(doc: LicenseDoc, onClose: () -> Unit) {
    val context = LocalContext.current
    val colors = LocalShellColors.current
    val material = LocalShellMaterial.current
    val text = remember(doc) {
        try {
            context.assets.open(doc.assetPath).bufferedReader().use { it.readText() }
        } catch (_: Exception) {
            "The license text could not be loaded."
        }
    }
    BasicAlertDialog(onDismissRequest = onClose) {
        val dialogShape = RoundedCornerShape(20.dp)
        Column(
            modifier = Modifier
                .fillMaxWidth(0.94f)
                .clip(dialogShape)
                .background(colors.g1)
                .padding(24.dp)
                .testTag("license_dialog")
        ) {
            Text(text = doc.title, fontSize = 18.sp, fontWeight = FontWeight.SemiBold, color = colors.t1)
            Text(text = doc.subtitle, fontSize = 13.sp, color = colors.t2)
            Spacer(modifier = Modifier.height(12.dp))
            Box(modifier = Modifier.heightIn(max = 420.dp).verticalScroll(rememberScrollState())) {
                Text(
                    text = text,
                    fontSize = 12.sp,
                    lineHeight = 17.sp,
                    color = colors.t1,
                    modifier = Modifier.testTag("license_text")
                )
            }
            Spacer(modifier = Modifier.height(16.dp))
            Box(
                modifier = Modifier
                    .align(Alignment.End)
                    .defaultMinSize(minWidth = 48.dp, minHeight = 48.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(material.field2)
                    .clickable(role = Role.Button, onClick = onClose)
                    .padding(horizontal = 16.dp, vertical = 8.dp)
                    .testTag("license_close"),
                contentAlignment = Alignment.Center
            ) {
                Text(text = "Close", color = colors.t1, fontSize = 14.sp, fontWeight = FontWeight.Medium)
            }
        }
    }
}
