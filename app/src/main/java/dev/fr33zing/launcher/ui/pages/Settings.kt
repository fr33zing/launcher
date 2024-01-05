package dev.fr33zing.launcher.ui.pages

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import dev.fr33zing.launcher.data.persistent.AppDatabase
import dev.fr33zing.launcher.data.utility.createBackupArchive
import dev.fr33zing.launcher.data.utility.generateExportFilename
import dev.fr33zing.launcher.doNotGoHomeOnNextPause
import dev.fr33zing.launcher.ui.components.NoticeKind
import dev.fr33zing.launcher.ui.components.sendNotice
import dev.fr33zing.launcher.ui.theme.ScreenHorizontalPadding
import java.util.Date
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Composable
fun Settings(db: AppDatabase) {
    Column(
        modifier =
            Modifier.systemBarsPadding().padding(horizontal = ScreenHorizontalPadding).fillMaxSize()
    ) {
        BackupSection(db)
    }
}

@Composable
private fun Section(
    name: String,
    children: @Composable () -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(16.dp), modifier = Modifier.fillMaxWidth()) {
        Text(text = name)
        children()
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
