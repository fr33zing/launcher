package dev.fr33zing.launcher.data.utility

import android.content.Context
import android.content.Intent
import android.content.pm.ActivityInfo
import android.content.pm.PackageManager
import android.net.Uri

fun Context.queryContentUriActivities(uri: Uri): List<ActivityInfo> =
    Intent()
        .apply {
            action = Intent.ACTION_VIEW
            val mimeType = contentResolver.getType(uri)
            setDataAndType(uri, mimeType)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        .let { packageManager.queryIntentActivities(it, PackageManager.MATCH_DEFAULT_ONLY) }
        .map { it.activityInfo }
