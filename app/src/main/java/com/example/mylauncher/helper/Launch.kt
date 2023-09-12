package com.example.mylauncher.helper

import android.content.ComponentName
import android.content.Context
import android.content.pm.LauncherApps
import android.os.UserHandle
import android.os.UserManager
import com.example.mylauncher.data.App

lateinit var launcherApps: LauncherApps
lateinit var userManager: UserManager

// TODO determine if this file should merge with Apps.kt or something else

fun getUserHandle(userHandleString: String): UserHandle {
    return userManager.userProfiles.firstOrNull { it.equals(userHandleString) }
        ?: android.os.Process.myUserHandle()
}

fun launchApp(context: Context, app: App) {
    val userHandle = getUserHandle(app.userHandle)
    val activityList = launcherApps.getActivityList(app.packageName, userHandle)
    val componentName = ComponentName(app.packageName, activityList[activityList.size - 1].name)
    launcherApps.startMainActivity(componentName, userHandle, null, null)
}