package com.example.mylauncher.data.persistent

import android.content.pm.LauncherActivityInfo
import com.example.mylauncher.data.NodeKind
import com.example.mylauncher.data.persistent.payloads.Application

/** Create Nodes and Applications for newly installed apps. Returns the number of new apps added. */
suspend fun AppDatabase.createNewApplications(activityInfos: List<LauncherActivityInfo>): Int {
    var newApps = 0

    activityInfos
        .filter { activityInfo ->
            applicationDao().getAllPayloads().find { app ->
                app.appName == activityInfo.label.toString()
            } == null
        }
        .forEachIndexed { index, activityInfo ->
            nodeDao()
                .insert(
                    Node(
                        nodeId = 0,
                        parentId = nodeDao().getDefaultNode().nodeId,
                        kind = NodeKind.Application,
                        order = index,
                        label = activityInfo.label.toString()
                    )
                )
            applicationDao()
                .insert(
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

enum class RelativeNodeOffset(val orderOffset: Int) {
    Above(-1),
    Within(0),
    Below(1),
}

data class RelativeNodePosition(val relativeToNodeId: Int, val offset: RelativeNodeOffset)

/**
 * Create a new node (and its payload, if applicable) relative to another node. Returns the new
 * node's id.
 */
suspend fun AppDatabase.createNode(position: RelativeNodePosition, newNodeKind: NodeKind): Int {
    val relativeToNode =
        nodeDao().getNodeById(position.relativeToNodeId) ?: throw Exception("Node does not exist")
    val parentId =
        if (position.offset == RelativeNodeOffset.Within) position.relativeToNodeId
        else relativeToNode.parentId

    insert(
        Node(
            nodeId = 0,
            parentId = parentId,
            kind = newNodeKind,
            order = relativeToNode.order + position.offset.orderOffset,
            label = "New ${newNodeKind.label}"
        )
    )

    val lastNodeId = nodeDao().getLastNodeId()
    createDefaultPayloadForNode(newNodeKind, lastNodeId)?.let { insert(it) }

    return lastNodeId
}
