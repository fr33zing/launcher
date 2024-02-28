package dev.fr33zing.launcher.data.utility

import android.app.SearchManager
import android.content.Context
import android.content.Intent
import android.content.pm.ActivityInfo
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Process
import android.provider.AlarmClock
import dev.fr33zing.launcher.data.persistent.payloads.launcherApps

private fun Intent.queryActivities(context: Context) =
    context.packageManager.queryIntentActivities(this, PackageManager.MATCH_DEFAULT_ONLY).map {
        it.activityInfo
    }

fun List<ActivityInfo>.toLauncherActivityInfos(context: Context) = map {
    val intent = context.packageManager.getLaunchIntentForPackage(it.packageName)
    launcherApps.resolveActivity(intent, Process.myUserHandle())
}

fun Context.queryContentUriActivities(uri: Uri): List<ActivityInfo> =
    Intent()
        .apply {
            action = Intent.ACTION_VIEW
            val mimeType = contentResolver.getType(uri)
            setDataAndType(uri, mimeType)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        .queryActivities(this)

fun Context.queryWebSearchActivities(): List<ActivityInfo> =
    Intent(Intent.ACTION_WEB_SEARCH).putExtra(SearchManager.QUERY, "").queryActivities(this)

fun Context.queryTimerActivities(): List<ActivityInfo> =
    Intent(AlarmClock.ACTION_SET_TIMER)
        .putExtra(AlarmClock.EXTRA_LENGTH, 0)
        .putExtra(AlarmClock.EXTRA_MESSAGE, "")
        .queryActivities(this)
