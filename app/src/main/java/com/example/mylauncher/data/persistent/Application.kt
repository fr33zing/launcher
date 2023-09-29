package com.example.mylauncher.data.persistent

import android.content.pm.LauncherActivityInfo
import androidx.room.Dao
import androidx.room.Entity
import androidx.room.Query

@Entity
class Application(
    payloadId: Int,
    nodeId: Int,
    val appName: String,
    val packageName: String,
    val activityClassName: String?,
    val userHandle: String,
) : Payload(payloadId, nodeId) {
    constructor(payloadId: Int, nodeId: Int, activityInfo: LauncherActivityInfo) : this(
        payloadId = payloadId,
        nodeId = nodeId,
        appName = activityInfo.label.toString(),
        packageName = activityInfo.applicationInfo.packageName,
        activityClassName = activityInfo.componentName.className,
        userHandle = activityInfo.user.toString()
    )
}

@Dao
abstract class ApplicationDao : PayloadDao<Application> {
    @Query("SELECT * FROM Application")
    abstract override fun getAll(): List<Application>

    @Query("SELECT * FROM Application WHERE nodeId = :nodeId")
    abstract override fun getByNodeId(nodeId: Int): Application
}