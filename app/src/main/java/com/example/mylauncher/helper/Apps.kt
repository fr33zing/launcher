package com.example.mylauncher.helper

import android.content.Context
import android.content.pm.LauncherApps
import android.os.UserManager
import com.example.mylauncher.data.AppModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

// TODO determine if this file should merge with AppModel.kt or something else

suspend fun getAppList(context: Context): ArrayList<AppModel> {
    return withContext(Dispatchers.IO) {
        val appList: ArrayList<AppModel> = ArrayList()

        val userManager = context.getSystemService(Context.USER_SERVICE) as UserManager
        val launcherApps = context.getSystemService(Context.LAUNCHER_APPS_SERVICE) as LauncherApps

        for (profile in userManager.userProfiles) {
            for (app in launcherApps.getActivityList(null, profile)) {
                val appLabel = app.label.toString()
                val appModel = AppModel(
                    appName = appLabel,
                    key = appLabel,
                    appPackageName = app.applicationInfo.packageName,
                    activityClassName = app.componentName.className,
                    userHandle = profile
                )
                appList.add(appModel)
            }
        }

        appList.sortBy { it.appName }
        appList
    }
}