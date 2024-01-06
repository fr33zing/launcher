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
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.LocalMinimumInteractiveComponentEnforcement
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.PlatformTextStyle
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import dev.fr33zing.launcher.data.persistent.AppDatabase
import dev.fr33zing.launcher.data.persistent.Preference
import dev.fr33zing.launcher.data.persistent.Preferences
import dev.fr33zing.launcher.data.utility.exportBackupArchive
import dev.fr33zing.launcher.data.utility.generateExportFilename
import dev.fr33zing.launcher.data.utility.importBackupArchive
import dev.fr33zing.launcher.doNotGoHomeOnNextPause
import dev.fr33zing.launcher.ui.components.NoticeKind
import dev.fr33zing.launcher.ui.components.dialog.ApplicationPickerDialog
import dev.fr33zing.launcher.ui.components.sendNotice
import dev.fr33zing.launcher.ui.theme.Background
import dev.fr33zing.launcher.ui.theme.Catppuccin
import dev.fr33zing.launcher.ui.theme.Foreground
import dev.fr33zing.launcher.ui.theme.ScreenHorizontalPadding
import dev.fr33zing.launcher.ui.theme.outlinedTextFieldColors
import dev.fr33zing.launcher.ui.theme.typography
import dev.fr33zing.launcher.ui.utility.mix
import dev.fr33zing.launcher.ui.utility.wholeScreenVerticalScrollShadows
import java.util.Date
import kotlin.reflect.KProperty0
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private val sectionSpacing = 42.dp
private val sectionTitleSpacing = 4.dp
private val sectionHeaderSpacing = 18.dp
private val preferenceSpacing = 20.dp
private val inlineSpacing = 6.dp
private val lineSpacing = 8.dp

@Composable
fun Preferences(db: AppDatabase) {
    val preferences = Preferences(LocalContext.current)

    Box(
        Modifier.fillMaxSize().systemBarsPadding().imePadding().wholeScreenVerticalScrollShadows()
    ) {
        Box(modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
            Column(
                verticalArrangement = Arrangement.spacedBy(sectionSpacing),
                modifier =
                    Modifier.systemBarsPadding()
                        .padding(horizontal = ScreenHorizontalPadding)
                        .padding(top = lineSpacing, bottom = preferenceSpacing)
                        .fillMaxSize()
            ) {
                ItemAppearanceSection(preferences)
                HomeSection(preferences)
                ConfirmationDialogsSection(preferences)
                BackupSection(db)
            }
        }
    }
}

//
// Common components
//

@Composable
private fun Section(
    name: String,
    description: String,
    children: @Composable () -> Unit,
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(sectionHeaderSpacing),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(sectionTitleSpacing),
        ) {
            Text(text = name, style = typography.titleLarge)
            Text(
                text = description,
                style = typography.bodyMedium,
                color = Foreground.mix(Background, 0.5f)
            )
        }
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
        Row(horizontalArrangement = Arrangement.spacedBy(inlineSpacing)) {
            Text(
                text = "Default: ${if (default) "Checked" else "Unchecked"}",
                style = typography.bodyMedium,
                color = Foreground.mix(Background, 0.5f)
            )
            ResetButton(preference, default, state)
        }
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

@OptIn(ExperimentalComposeUiApi::class)
@Composable
private fun PreferenceTextField(
    property: KProperty0<Preference<String, String>>,
    placeholder: String? = null,
) {
    val preference = remember { property.get() }
    val default = remember { preference.default }
    val state by preference.flow.collectAsState(default)

    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current
    var enabled by remember { mutableStateOf(true) }

    fun clearFocus() {
        focusManager.clearFocus()
        keyboardController?.hide()

        // HACK: Quickly disable and enable to clear selection
        enabled = false
        CoroutineScope(Dispatchers.Main).launch {
            delay(25)
            enabled = true
        }
    }

    OutlinedTextField(
        value = state,
        onValueChange = { CoroutineScope(Dispatchers.IO).launch { preference.set(it) } },
        enabled = enabled,
        colors = outlinedTextFieldColors(),
        keyboardOptions =
            KeyboardOptions(
                capitalization = KeyboardCapitalization.None,
                autoCorrect = false,
                imeAction = ImeAction.Done,
                keyboardType = KeyboardType.Text,
            ),
        keyboardActions = KeyboardActions(onDone = { clearFocus() }),
        placeholder =
            placeholder?.let { { Text(placeholder, color = Foreground.mix(Background, 0.5f)) } },
        modifier = Modifier.fillMaxWidth()
    )
}

@Composable
private fun TinyButton(
    text: String,
    onClick: () -> Unit,
    enabled: Boolean = true,
    color: Color? = null,
) {
    Button(
        onClick = { onClick() },
        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp),
        modifier = Modifier.height(24.dp),
        enabled = enabled,
        colors =
            if (color == null) ButtonDefaults.buttonColors()
            else
                ButtonDefaults.buttonColors(
                    containerColor = Catppuccin.Current.red,
                )
    ) {
        Text(
            text,
            style =
                typography.bodyMedium.copy(
                    platformStyle = PlatformTextStyle(includeFontPadding = false)
                )
        )
    }
}

@Composable
private fun <T> ResetButton(
    preference: Preference<T, *>,
    default: T,
    state: T,
) {
    TinyButton(
        text = "Reset",
        enabled = state != default,
        color = Catppuccin.Current.red,
        onClick = { CoroutineScope(Dispatchers.IO).launch { preference.set(default) } }
    )
}

//
// Section: Item appearance
//

@Composable
private fun ItemAppearanceSection(preferences: Preferences) {
    Section(
        "Item appearance",
        "Adjust the style and layout of items in the tree view and on the home screen."
    ) {
        PreferenceSlider(preferences.nodeAppearance::fontSize, "Font size", 12f..32f, "sp")
        PreferenceSlider(preferences.nodeAppearance::indent, "Indentation width", 12f..32f, "dp")
        PreferenceSlider(preferences.nodeAppearance::spacing, "Vertical spacing", 12f..32f, "dp")
    }
}

//
// Section: Home
//

@Composable
private fun HomeSection(preferences: Preferences) {
    Section("Home", "Adjust the appearance and function of the home screen.") {
        PreferenceCheckbox(property = preferences.home::use24HourTime, label = "Use 24-hour time")
        ApplicationPreference(preferences.home.defaultApplications::clock, "Clock application")
        ApplicationPreference(
            preferences.home.defaultApplications::calendar,
            "Calendar application"
        )
    }
}

@Composable
private fun ApplicationPreference(property: KProperty0<Preference<String, String>>, label: String) {
    val preference = remember { property.get() }
    val default = remember { preference.default }
    val state by preference.flow.collectAsState(default)

    val appPickerVisible = remember { mutableStateOf(false) }
    ApplicationPickerDialog(
        visible = appPickerVisible,
        onAppPicked = {
            CoroutineScope(Dispatchers.IO).launch { preference.set(it.componentName.packageName) }
        }
    )

    Column(verticalArrangement = Arrangement.spacedBy(lineSpacing)) {
        Text(text = label, style = typography.bodyLarge)
        PreferenceTextField(property, "Use system default")
        Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
            Text(
                text = "Default: ${default.ifEmpty { "Use system default" }}",
                style = typography.bodyMedium,
                color = Foreground.mix(Background, 0.5f)
            )
            Row(horizontalArrangement = Arrangement.spacedBy(inlineSpacing)) {
                TinyButton(text = "Pick app", onClick = { appPickerVisible.value = true })
                ResetButton(preference, default, state)
            }
        }
    }
}

//
// Section: Confirmation dialogs
//

@Composable
private fun ConfirmationDialogsSection(preferences: Preferences) {
    @Composable
    fun Subsection(label: String, children: @Composable () -> Unit) {
        Column(verticalArrangement = Arrangement.spacedBy(lineSpacing)) {
            Text(text = label, style = typography.bodyLarge)
            children()
        }
    }

    Section(
        "Confirmation dialogs",
        "Adjust which actions ask for additional confirmation, and under what circumstances."
    ) {
        with(preferences.confirmationDialogs) {
            Subsection(label = "Creating item") {
                PreferenceCheckbox(createNode::askOnAccept, "Accept")
                PreferenceCheckbox(createNode::askOnReject, "Reject")
            }
            Subsection(label = "Editing item") {
                PreferenceCheckbox(editNode::askOnAccept, "Accept")
                PreferenceCheckbox(editNode::askOnReject, "Reject")
            }
            Subsection(label = "Moving item") {
                PreferenceCheckbox(moveNode::askOnAccept, "Accept")
                PreferenceCheckbox(moveNode::askOnReject, "Reject")
            }
            Subsection(label = "Reordering item") {
                PreferenceCheckbox(reorderNodes::askOnAccept, "Accept")
                PreferenceCheckbox(reorderNodes::askOnReject, "Reject")
            }
        }
    }
}

//
// Section: Backup & restore
//

@Composable
private fun BackupSection(db: AppDatabase) {
    Section(
        "Backup & restore",
        "Backup database and preferences into a ZIP archive. Restoring a backup will cause the application to restart."
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            BackupExportButton(db, Modifier.weight(1f))
            BackupImportButton(db, Modifier.weight(1f))
        }
    }
}

@Composable
private fun BackupImportButton(db: AppDatabase, modifier: Modifier = Modifier) {
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
private fun BackupExportButton(db: AppDatabase, modifier: Modifier = Modifier) {
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
