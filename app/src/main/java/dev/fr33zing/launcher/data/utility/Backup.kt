package dev.fr33zing.launcher.data.utility

import android.content.Context
import android.net.Uri
import dev.fr33zing.launcher.data.persistent.AppDatabase
import dev.fr33zing.launcher.data.persistent.Preferences
import dev.fr33zing.launcher.data.persistent.checkpoint
import java.io.BufferedInputStream
import java.io.BufferedOutputStream
import java.io.FileInputStream
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

fun generateExportFilename(context: Context): String {
    val packageName = context.packageName.replace('.', '-')
    val timestamp: String = SimpleDateFormat("yyyyMMdd-HHmmss", Locale.ROOT).format(Date())
    return listOf("BACKUP", packageName, timestamp, "zip").joinToString(".")
}

suspend fun createBackupArchive(context: Context, db: AppDatabase, outputFileUri: Uri) {
    val outputFile = outputFileUri.toString()
    val databaseFile = db.checkpoint()
    val preferencesYaml = Preferences(context).export().toYaml()

    ZipOutputStream(BufferedOutputStream(FileOutputStream(outputFile))).use { zipOutputStream ->
        // Add database to archive
        FileInputStream(databaseFile).use { fileInputStream ->
            BufferedInputStream(fileInputStream).use { bufferedInputStream ->
                val entry = ZipEntry(databaseFile.substring(databaseFile.lastIndexOf("/")))
                zipOutputStream.putNextEntry(entry)
                bufferedInputStream.copyTo(zipOutputStream, 1024)
            }
        }

        // Add preferences to archive
        zipOutputStream.putNextEntry(ZipEntry("preferences.yaml"))
        zipOutputStream.bufferedWriter().write(preferencesYaml)

        // Add README.txt
        zipOutputStream.putNextEntry(ZipEntry("README.txt"))
        zipOutputStream.bufferedWriter().write("Backup generated")
    }
}
