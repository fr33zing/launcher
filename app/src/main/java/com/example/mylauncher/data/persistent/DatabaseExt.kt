package com.example.mylauncher.data.persistent

import android.content.pm.LauncherActivityInfo
import androidx.room.withTransaction
import com.example.mylauncher.data.NodeKind
import com.example.mylauncher.data.NodeRow
import com.example.mylauncher.data.persistent.payloads.Application

suspend fun AppDatabase.getFlatNodeList(): List<NodeRow> {
    val result = ArrayList<NodeRow>()

    suspend fun add(node: Node, parent: NodeRow?, depth: Int) {
        val row = NodeRow(node, parent, depth)
        result.add(row)

        nodeDao()
            .getChildNodes(node.nodeId)
            .sortedBy { it.order }
            .forEach { add(it, row, depth + 1) }
    }

    nodeDao().getTopLevelNodes().sortedBy { it.order }.forEach { add(it, null, 0) }

    return result
}

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
    val order =
        if (position.offset == RelativeNodeOffset.Within) 0
        else relativeToNode.order + position.offset.orderOffset

    return withTransaction {
        val siblings = nodeDao().getChildNodes(parentId).fixOrder()
        siblings.filter { it.order >= order }.forEach { it.order++ }
        updateMany(siblings)
        insert(
            Node(
                nodeId = 0,
                parentId = parentId,
                kind = newNodeKind,
                order = order,
                label = "New ${newNodeKind.label}"
            )
        )
        val lastNodeId = nodeDao().getLastNodeId()
        insert(createDefaultPayloadForNode(newNodeKind, lastNodeId))

        lastNodeId
    }
}
