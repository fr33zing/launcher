package com.example.mylauncher.data.persistent

import android.content.pm.LauncherActivityInfo
import com.example.mylauncher.data.NodeKind

/**
 * Create Nodes and Applications for newly installed apps.
 * Returns the number of new apps added.
 */
suspend fun AppDatabase.createNewApplications(activityInfos: List<LauncherActivityInfo>): Int {
    var newApps = 0

    activityInfos
        .filter { activityInfo ->
            applicationDao().getAll()
                .find { app ->
                    app.appName == activityInfo.label.toString()
                } == null
        }
        .forEach { activityInfo ->
            nodeDao().insert(
                Node(
                    nodeId = 0,
                    parentId = nodeDao().getDefaultNode().nodeId,
                    dataId = null,
                    kind = NodeKind.Application,
                    label = activityInfo.label.toString()
                )
            )
            applicationDao().insert(
                Application(
                    payloadId = 0,
                    nodeId = nodeDao().getLastNodeId(),
                    activityInfo = activityInfo,
                )
            )
            newApps++
        }

    return newApps
}
