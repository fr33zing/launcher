package dev.fr33zing.launcher.ui.pages

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.LocalMinimumInteractiveComponentEnforcement
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.PlatformTextStyle
import androidx.compose.ui.unit.dp
import dev.fr33zing.launcher.data.persistent.AppDatabase
import dev.fr33zing.launcher.data.persistent.Preference
import dev.fr33zing.launcher.data.persistent.Preferences
import dev.fr33zing.launcher.data.utility.exportBackupArchive
import dev.fr33zing.launcher.data.utility.generateExportFilename
import dev.fr33zing.launcher.data.utility.importBackupArchive
import dev.fr33zing.launcher.doNotGoHomeOnNextPause
import dev.fr33zing.launcher.ui.components.NoticeKind
import dev.fr33zing.launcher.ui.components.sendNotice
import dev.fr33zing.launcher.ui.theme.Background
import dev.fr33zing.launcher.ui.theme.Catppuccin
import dev.fr33zing.launcher.ui.theme.Foreground
import dev.fr33zing.launcher.ui.theme.ScreenHorizontalPadding
import dev.fr33zing.launcher.ui.theme.typography
import dev.fr33zing.launcher.ui.utility.mix
import java.util.Date
import kotlin.reflect.KProperty0
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

private val sectionSpacing = 42.dp
private val sectionHeaderSpacing = 18.dp
private val preferenceSpacing = 32.dp
private val inlineSpacing = 6.dp
private val lineSpacing = 8.dp

@Composable
fun Preferences(db: AppDatabase) {
    val preferences = Preferences(LocalContext.current)

    Box(modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
        Column(
            verticalArrangement = Arrangement.spacedBy(sectionSpacing),
            modifier =
                Modifier.systemBarsPadding()
                    .padding(horizontal = ScreenHorizontalPadding)
                    .padding(bottom = preferenceSpacing)
                    .fillMaxSize()
        ) {
            ConfirmationDialogsSection(preferences)
            TextAndSpacingSection(preferences)
            BackupSection(db)
        }
    }
}

@Composable
private fun Section(
    name: String,
    description: String? = null,
    children: @Composable () -> Unit,
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(sectionHeaderSpacing),
        modifier = Modifier.fillMaxWidth()
    ) {
        Text(text = name, style = typography.titleLarge)
        if (description != null) Text(text = description, style = typography.bodyMedium)

        Column(
            verticalArrangement = Arrangement.spacedBy(preferenceSpacing),
            modifier = Modifier.fillMaxWidth()
        ) {
            children()
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PreferenceCheckbox(
    property: KProperty0<Preference<Boolean, *>>,
    label: String,
) {
    val preference = remember { property.get() }
    val default = remember { preference.default }
    val state by preference.flow.collectAsState(default)

    Row(
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(inlineSpacing),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            CompositionLocalProvider(
                // Remove default padding
                LocalMinimumInteractiveComponentEnforcement provides false
            ) {
                Checkbox(
                    checked = state,
                    onCheckedChange = {
                        CoroutineScope(Dispatchers.IO).launch { preference.set(it) }
                    }
                )
            }
            Text(text = label, style = typography.bodyLarge)
        }
        Text(
            text = "Default: $default",
            style = typography.bodyMedium,
            color = Foreground.mix(Background, 0.5f)
        )
        ResetButton(preference, default, state)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PreferenceSlider(
    property: KProperty0<Preference<Int, *>>,
    label: String,
    range: ClosedFloatingPointRange<Float>,
    unit: String = ""
) {
    val preference = remember { property.get() }
    val default = remember { preference.default }
    val state by preference.flow.collectAsState(default)

    Column(verticalArrangement = Arrangement.spacedBy(lineSpacing)) {
        Text(text = label, style = typography.bodyLarge)
        Row(
            horizontalArrangement = Arrangement.spacedBy(inlineSpacing),
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                "$state$unit",
                style =
                    typography.labelMedium.copy(
                        platformStyle = PlatformTextStyle(includeFontPadding = false)
                    )
            )
            CompositionLocalProvider(
                // Remove default padding
                LocalMinimumInteractiveComponentEnforcement provides false
            ) {
                Slider(
                    value = state.toFloat(),
                    onValueChange = {
                        CoroutineScope(Dispatchers.IO).launch { preference.set(it.toInt()) }
                    },
                    colors =
                        SliderDefaults.colors(
                            thumbColor = MaterialTheme.colorScheme.primary,
                            activeTrackColor = MaterialTheme.colorScheme.primary,
                            inactiveTrackColor = Catppuccin.Current.crust
                        ),
                    valueRange = range
                )
            }
        }
        Row(
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Top,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                text = "Default: $default$unit",
                style = typography.bodyMedium,
                color = Foreground.mix(Background, 0.5f)
            )
            ResetButton(preference, default, state)
        }
    }
}

@Composable
private fun <T> ResetButton(
    preference: Preference<T, *>,
    default: T,
    state: T,
) {
    Button(
        onClick = { CoroutineScope(Dispatchers.IO).launch { preference.set(default) } },
        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp),
        modifier = Modifier.height(24.dp),
        enabled = state != default,
        colors =
            ButtonDefaults.buttonColors(
                containerColor = Catppuccin.Current.red,
            ),
    ) {
        Text(
            text = "Reset",
            style =
                typography.bodyMedium.copy(
                    platformStyle = PlatformTextStyle(includeFontPadding = false)
                )
        )
    }
}

@Composable
private fun ConfirmationDialogsSection(preferences: Preferences) {
    @Composable
    fun Subsection(label: String, children: @Composable () -> Unit) {
        Column(verticalArrangement = Arrangement.spacedBy(lineSpacing)) {
            Text(text = label, style = typography.bodyLarge)
            children()
        }
    }

    Section("Confirmation dialogs") {
        Subsection(label = "Creating node") {
            PreferenceCheckbox(preferences::askOnCreateNodeAccept, "Accept")
            PreferenceCheckbox(preferences::askOnCreateNodeReject, "Reject")
        }
        Subsection(label = "Editing node") {
            PreferenceCheckbox(preferences::askOnEditNodeAccept, "Accept")
            PreferenceCheckbox(preferences::askOnEditNodeReject, "Reject")
        }
        Subsection(label = "Moving nodes") {
            PreferenceCheckbox(preferences::askOnMoveNodeAccept, "Accept")
            PreferenceCheckbox(preferences::askOnMoveNodeReject, "Reject")
        }
        Subsection(label = "Reordering nodes") {
            PreferenceCheckbox(preferences::askOnReorderNodesAccept, "Accept")
            PreferenceCheckbox(preferences::askOnReorderNodesReject, "Reject")
        }
    }
}

@Composable
private fun TextAndSpacingSection(preferences: Preferences) {
    Section("Node text and spacing") {
        PreferenceSlider(preferences::fontSize, "Font size", 12f..32f, "sp")
        PreferenceSlider(preferences::indent, "Indentation width", 12f..32f, "dp")
        PreferenceSlider(preferences::spacing, "Vertical spacing", 12f..32f, "dp")
    }
}

@Composable
private fun BackupSection(db: AppDatabase) {
    Section(
        "Backup & restore",
        buildString {
            appendLine("Backup database and preferences into a ZIP archive.")
            append("Restoring a backup will cause the application to restart.")
        }
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            ExportButton(db, Modifier.weight(1f))
            ImportButton(db, Modifier.weight(1f))
        }
    }
}

@Composable
private fun ImportButton(db: AppDatabase, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val openDocumentLauncher =
        rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { importUri ->
            if (importUri == null) {
                sendNotice(
                    "backup-import-failed-uri-null",
                    "Backup import failed: No import path was provided.",
                    NoticeKind.Error
                )
            } else {
                CoroutineScope(Dispatchers.IO).launch {
                    importBackupArchive(context, db, importUri)
                }
            }
        }

    Button(
        onClick = {
            doNotGoHomeOnNextPause()
            openDocumentLauncher.launch(arrayOf("application/zip"))
        },
        modifier = modifier
    ) {
        Text(text = "Restore")
    }
}

@Composable
private fun ExportButton(db: AppDatabase, modifier: Modifier = Modifier) {
    var exportDate by remember { mutableStateOf<Date?>(null) }
    val context = LocalContext.current
    val createDocumentLauncher =
        rememberLauncherForActivityResult(
            ActivityResultContracts.CreateDocument("application/zip")
        ) { exportUri ->
            if (exportUri == null) {
                sendNotice(
                    "backup-export-failed-uri-null",
                    "Backup export failed: No export path was provided.",
                    NoticeKind.Error
                )
            } else {
                CoroutineScope(Dispatchers.IO).launch {
                    exportBackupArchive(context, db, exportUri, exportDate!!)
                }
            }
        }

    Button(
        onClick = {
            doNotGoHomeOnNextPause()
            exportDate = Date()
            val filename = generateExportFilename(context, exportDate!!)
            createDocumentLauncher.launch(filename)
        },
        modifier = modifier
    ) {
        Text(text = "Backup")
    }
}
