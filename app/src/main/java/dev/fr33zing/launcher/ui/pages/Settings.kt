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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
import dev.fr33zing.launcher.data.utility.createBackupArchive
import dev.fr33zing.launcher.data.utility.generateExportFilename
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
private val sectionTitleSpacing = 16.dp
private val preferenceSpacing = 32.dp

@Composable
fun Settings(db: AppDatabase) {
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
            TextAndSpacingSection(preferences)
            BackupSection(db)
        }
    }
}

@Composable
private fun Section(
    name: String,
    children: @Composable () -> Unit,
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(sectionTitleSpacing),
        modifier = Modifier.fillMaxWidth()
    ) {
        Text(text = name, style = typography.titleLarge)
        Column(
            verticalArrangement = Arrangement.spacedBy(preferenceSpacing),
            modifier = Modifier.fillMaxWidth()
        ) {
            children()
        }
    }
}

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

    Column {
        Text(text = label, style = typography.bodyLarge)
        Row(
            horizontalArrangement = Arrangement.spacedBy(4.dp),
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
    }
}

@Composable
fun TextAndSpacingSection(preferences: Preferences) {
    Section("Node text and spacing") {
        PreferenceSlider(preferences::fontSize, "Font size", 12f..32f, "sp")
        PreferenceSlider(preferences::indent, "Indentation width", 12f..32f, "dp")
        PreferenceSlider(preferences::spacing, "Vertical spacing", 12f..32f, "dp")
    }
}

@Composable
fun BackupSection(db: AppDatabase) {
    Section("Database backup") {
        Row(
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            ExportButton(db)
        }
    }
}

@Composable
private fun ExportButton(
    db: AppDatabase,
) {
    var exportDate by remember { mutableStateOf<Date?>(null) }
    val context = LocalContext.current
    val createDocumentLauncher =
        rememberLauncherForActivityResult(
            ActivityResultContracts.CreateDocument("application/zip")
        ) { exportUri ->
            if (exportUri != null)
                CoroutineScope(Dispatchers.IO).launch {
                    createBackupArchive(context, db, exportUri, exportDate!!)
                }
            else
                sendNotice(
                    "backup-export-failed-uri-null",
                    "Backup failed: No export path was provided.",
                    NoticeKind.Error
                )
        }

    fun export() {
        doNotGoHomeOnNextPause()
        exportDate = Date()
        val filename = generateExportFilename(context, exportDate!!)
        createDocumentLauncher.launch(filename)
    }

    Button(::export) { Text(text = "Export") }
}
