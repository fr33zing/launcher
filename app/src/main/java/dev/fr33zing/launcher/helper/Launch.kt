package dev.fr33zing.launcher.helper

import android.content.ComponentName
import android.content.Context
import android.content.pm.LauncherApps
import android.os.UserHandle
import android.os.UserManager
import dev.fr33zing.launcher.data.persistent.payloads.Application

lateinit var launcherApps: LauncherApps
lateinit var userManager: UserManager

// TODO determine if this file should merge with Apps.kt or something else

fun getUserHandle(userHandleString: String): UserHandle {
    return userManager.userProfiles.firstOrNull { it.equals(userHandleString) }
        ?: android.os.Process.myUserHandle()
}

fun launchApp(app: Application) {
    val userHandle = getUserHandle(app.userHandle)
    val activityList = launcherApps.getActivityList(app.packageName, userHandle)
    val componentName = ComponentName(app.packageName, activityList[activityList.size - 1].name)
    launcherApps.startMainActivity(componentName, userHandle, null, null)
}
