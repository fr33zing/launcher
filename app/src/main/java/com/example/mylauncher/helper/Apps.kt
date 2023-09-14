package com.example.mylauncher.helper

import android.content.Context
import android.content.pm.LauncherActivityInfo
import android.content.pm.LauncherApps
import android.os.UserManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

suspend fun getActivityInfos(context: Context): List<LauncherActivityInfo> {
    return withContext(Dispatchers.IO) {
        val userManager = context.getSystemService(Context.USER_SERVICE) as UserManager
        val launcherApps = context.getSystemService(Context.LAUNCHER_APPS_SERVICE) as LauncherApps

        userManager.userProfiles.map { launcherApps.getActivityList(null, it) }
            .reduce { acc, activityInfos -> acc + activityInfos }
            .sortedBy { it.label.toString() } //as ArrayList<LauncherActivityInfo>
    }
}