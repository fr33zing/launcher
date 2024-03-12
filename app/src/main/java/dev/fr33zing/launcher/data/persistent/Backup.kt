package dev.fr33zing.launcher.data.persistent

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.datastore.preferences.preferencesDataStoreFile
import com.charleskorn.kaml.Yaml
import dev.fr33zing.launcher.data.persistent.payloads.mainPackageManager
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import java.io.BufferedInputStream
import java.io.BufferedOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

@Serializable
data class BackupMetadata(
    val packageName: String,
    val packageVersion: String,
    val generatedEpochSecond: Long,
)

fun importBackupArchive(
    context: Context,
    db: AppDatabase,
    inputFileUri: Uri,
) {
    context.contentResolver.openInputStream(inputFileUri)?.use { fileInputStream ->
        ZipInputStream(BufferedInputStream(fileInputStream)).use { zipInputStream ->
            var zipEntry = zipInputStream.nextEntry
            while (zipEntry != null) {
                when (zipEntry.name) {
                    "database" -> {
                        db.getDatabaseFile().outputStream().use { outputStream ->
                            db.openHelper.close()
                            zipInputStream.copyTo(outputStream, 1024)
                        }
                    }
                    "preferences" -> {
                        context.preferencesDataStoreFile(PREFERENCES_DATASTORE_NAME).outputStream().use {
                                outputStream ->
                            zipInputStream.copyTo(outputStream, 1024)
                        }
                    }
                    else -> {}
                }
                zipEntry = zipInputStream.nextEntry
            }
        }

        restartApplication(context)
    }
}

fun generateExportFilename(
    context: Context,
    date: Date,
): String {
    val packageName = context.packageName.replace('.', '-')
    val timestamp = SimpleDateFormat("yyyyMMdd-HHmmss", Locale.ROOT).format(date)
    return listOf(timestamp, "backup", packageName, "zip").joinToString(".")
}

fun exportBackupArchive(
    context: Context,
    db: AppDatabase,
    outputFileUri: Uri,
    date: Date,
) {
    val copyFromStorageEntries =
        mapOf(
            "database" to db.getCheckpointedDatabaseFile(),
            "preferences" to context.preferencesDataStoreFile(PREFERENCES_DATASTORE_NAME),
        )
    val inMemoryEntries =
        mapOf(
            "README.html" to createBackupArchiveReadme(context, date),
            "metadata" to createBackupMetadata(context, date),
        )

    context.contentResolver.openOutputStream(outputFileUri)?.use { fileOutputStream ->
        ZipOutputStream(BufferedOutputStream(fileOutputStream)).use { zipOutputStream ->
            for ((name, file) in copyFromStorageEntries) {
                BufferedInputStream(file.inputStream()).use { bufferedInputStream ->
                    zipOutputStream.putNextEntry(ZipEntry(name))
                    bufferedInputStream.copyTo(zipOutputStream, 1024)
                }
            }
            for ((name, data) in inMemoryEntries) {
                data.byteInputStream().use { inputStream ->
                    zipOutputStream.putNextEntry(ZipEntry(name))
                    inputStream.copyTo(zipOutputStream)
                }
            }
        }
    } ?: throw Exception("Failed to open output stream")
}

private fun restartApplication(context: Context) {
    val launchIntent = mainPackageManager.getLaunchIntentForPackage(context.packageName)
    val restartIntent =
        Intent.makeRestartActivityTask(
            launchIntent?.component
                ?: throw Exception("Failed to restart application, intent component is null"),
        )
    context.startActivity(restartIntent)
    Runtime.getRuntime().exit(0)
}

private fun packageVersion(context: Context) = mainPackageManager.getPackageInfo(context.packageName, 0).versionName

private fun createBackupMetadata(
    context: Context,
    date: Date,
) = Yaml.default.encodeToString(
    BackupMetadata(
        packageName = context.packageName,
        packageVersion = packageVersion(context),
        generatedEpochSecond = date.toInstant().epochSecond,
    ),
)

private fun createBackupArchiveReadme(
    context: Context,
    date: Date,
): String {
    val title = "Backup"
    val timestamp = SimpleDateFormat("MMMM d, yyyy @ h:mm:ss a", Locale.US).format(date)
    return """
        <!DOCTYPE html>
        <html lang="en">
        <head>
            <meta charset="utf-8">
            <meta name="viewport" content="width=device-width, initial-scale=1">
            <title>$title</title>
            <style>
                body { font-family: sans-serif; }
                code { font-size: 1rem; }
                th, td { text-align: left; padding-right: 1em; }
            </style>
        </head>
        <body>
            <h1>$title</h1>
            <p>
                Package: <code>${context.packageName}</code><br/>
                Version: <code>${packageVersion(context)}</code><br/>
                Generated on: <code>$timestamp (${date.toInstant().epochSecond})</code>
            </p>
            <h2>Archive contents</h2>
            <table>
                <tr>
                    <th>File</th>
                    <th>Description</th>
                    <th>Format</th>
                </tr>
                <tr>
                    <td>README.html</td>
                    <td>Information for humans</td>
                    <td>HTML</td>
                </tr>
                <tr>
                    <td>metadata</td>
                    <td>Information for machines</td>
                    <td>YAML</td>
                </tr>
                <tr>
                    <td>database</td>
                    <td>Room database</td>
                    <td>SQLite db</td>
                </tr>
                <tr>
                    <td>preferences</td>
                    <td>User preferences</td>
                    <td>preferences_pb</td>
                </tr>
            </table>
        </body>
    """
        .trimIndent()
}
