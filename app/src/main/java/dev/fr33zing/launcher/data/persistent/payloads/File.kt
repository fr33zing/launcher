package dev.fr33zing.launcher.data.persistent.payloads

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import androidx.annotation.Keep
import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Ignore
import dev.fr33zing.launcher.data.persistent.AppDatabase
import dev.fr33zing.launcher.ui.components.NoticeKind
import dev.fr33zing.launcher.ui.components.sendNotice
import dev.fr33zing.launcher.ui.utility.UserEditable
import java.io.File
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Keep
@Entity
class File(
    payloadId: Int,
    nodeId: Int,
    @ColumnInfo(defaultValue = "")
    @UserEditable("File path", locked = true)
    var filePath: String = "",
    @ColumnInfo(defaultValue = "")
    @UserEditable("Open with package name", locked = true)
    var openWithPackageName: String = "",
) : Payload(payloadId, nodeId) {
    enum class Status(val reason: String) {
        Valid("<valid>"),
        MissingFile("the file is missing"),
        MissingPackage("the package is missing"),
    }

    val file
        get() = File(filePath)

    @Ignore private var _status: Status? = null

    val status
        get(): Status {
            if (_status == null) _status = computeStatus()
            return _status!!
        }

    private fun fileExists(): Boolean {
        try {
            val uri = Uri.parse(filePath)
            val inputStream =
                mainContentResolver.openInputStream(uri)
                    ?: throw Exception("failed to open input stream")
            inputStream.close()
        } catch (e: Exception) {
            return false
        }
        return true
    }

    // HACK: Don't check if the files exists here since it can block the main thread. Check on
    // activate instead.
    private fun computeStatus(): Status {
        if (filePath.isBlank()) return Status.MissingFile

        try {
            mainPackageManager.getPackageInfo(openWithPackageName, PackageManager.GET_ACTIVITIES)
        } catch (e: PackageManager.NameNotFoundException) {
            return Status.MissingPackage
        }

        return Status.Valid
    }

    override fun activate(db: AppDatabase, context: Context) {
        if (!fileExists()) {
            filePath = ""
            _status = Status.MissingFile
            this.also { payload -> CoroutineScope(Dispatchers.IO).launch { db.update(payload) } }
        }
        open(context)
    }

    private fun open(context: Context) {
        _status = computeStatus()

        if (_status != Status.Valid)
            sendNotice(
                "file-open-invalid:${nodeId}",
                "Cannot open file because ${_status!!.reason}."
            )
        else {
            try {
                val uri = Uri.parse(filePath)
                val mimeType = context.contentResolver.getType(uri)
                val intent =
                    Intent().apply {
                        action = Intent.ACTION_VIEW
                        `package` = openWithPackageName
                        setDataAndType(uri, mimeType)
                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    }
                context.startActivity(intent)
            } catch (e: Exception) {
                sendNotice(
                    "file-open-failed:${nodeId}",
                    "Failed to open file. Error: ${e.message}.",
                    NoticeKind.Error
                )
            }
        }
    }
}
