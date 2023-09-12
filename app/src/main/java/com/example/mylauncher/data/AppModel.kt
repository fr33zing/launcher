package com.example.mylauncher.data

import android.os.UserHandle

// TODO determine if this intermediate class is even necessary at all
data class AppModel(
    val appName: String,
    val key: String,
    val appPackageName: String,
    val activityClassName: String?,
    val userHandle: UserHandle,
) : Comparable<AppModel> {
    override fun compareTo(other: AppModel): Int = key.compareTo(other.key)
}