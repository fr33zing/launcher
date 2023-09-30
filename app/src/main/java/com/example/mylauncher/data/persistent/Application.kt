package com.example.mylauncher.data.persistent

import android.content.pm.LauncherActivityInfo
import androidx.room.Dao
import androidx.room.Entity
import androidx.room.Query
import com.example.mylauncher.ui.util.UserEditable

@Entity
class Application(
    payloadId: Int,
    nodeId: Int,
    @UserEditable("Application name", locked = true) var appName: String,
    @UserEditable("Package name", locked = true) var packageName: String,
    @UserEditable("Activity class name", locked = true, userCanUnlock = true)
    var activityClassName: String,
    @UserEditable("User handle", locked = true) var userHandle: String,
) : Payload(payloadId, nodeId) {
    constructor(
        payloadId: Int,
        nodeId: Int,
        activityInfo: LauncherActivityInfo
    ) : this(
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
    @Query("SELECT * FROM Application") abstract override fun getAll(): List<Application>

    @Query("SELECT * FROM Application WHERE nodeId = :nodeId")
    abstract override fun getByNodeId(nodeId: Int): Application?
}
