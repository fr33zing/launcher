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
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import dev.fr33zing.launcher.data.persistent.AppDatabase
import dev.fr33zing.launcher.data.utility.createBackupArchive
import dev.fr33zing.launcher.data.utility.generateExportFilename
import dev.fr33zing.launcher.doNotGoHomeOnNextPause
import dev.fr33zing.launcher.ui.theme.ScreenHorizontalPadding
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Composable
fun Settings(db: AppDatabase) {
    Column(
        modifier =
            Modifier.systemBarsPadding().padding(horizontal = ScreenHorizontalPadding).fillMaxSize()
    ) {
        Section("Database backup") {
            Row(
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Button(onClick = { /*TODO*/}) { Text(text = "Import") }
                ExportButton(db)
            }
        }
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
private fun ExportButton(db: AppDatabase) {
    val context = LocalContext.current
    val launcher =
        rememberLauncherForActivityResult(
            ActivityResultContracts.CreateDocument("application/zip")
        ) { exportFile ->
            exportFile ?: throw Exception("Export file is null")
            CoroutineScope(Dispatchers.IO).launch { createBackupArchive(context, db, exportFile) }
        }

    Button(
        onClick = {
            doNotGoHomeOnNextPause()
            launcher.launch(generateExportFilename(context))
        }
    ) {
        Text(text = "Export")
    }
}
