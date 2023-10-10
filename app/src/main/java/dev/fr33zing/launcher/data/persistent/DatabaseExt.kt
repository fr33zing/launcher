package dev.fr33zing.launcher.data.persistent

import android.content.pm.LauncherActivityInfo
import androidx.room.withTransaction
import dev.fr33zing.launcher.data.AllPermissions
import dev.fr33zing.launcher.data.NodeKind
import dev.fr33zing.launcher.data.NodeRow
import dev.fr33zing.launcher.data.PermissionKind
import dev.fr33zing.launcher.data.PermissionMap
import dev.fr33zing.launcher.data.PermissionScope
import dev.fr33zing.launcher.data.clone
import dev.fr33zing.launcher.data.persistent.payloads.Application
import dev.fr33zing.launcher.data.persistent.payloads.Directory
import dev.fr33zing.launcher.ui.components.refreshNodeList

const val ROOT_NODE_ID = -1

enum class RelativeNodeOffset(val orderOffset: Int) {
    Above(0),
    Within(0),
    Below(1),
}

data class RelativeNodePosition(val relativeToNodeId: Int, val offset: RelativeNodeOffset)

suspend fun AppDatabase.getFlatNodeList(rootNodeId: Int? = null): List<NodeRow> {
    val result = ArrayList<NodeRow>()

    suspend fun add(
        node: Node,
        parent: NodeRow?,
        depth: Int,
        parentPermissions: PermissionMap,
    ) {
        val payload = getPayloadByNodeId(node.kind, node.nodeId)

        val ownPermissions = parentPermissions.clone()
        if (payload is Directory) {
            PermissionKind.values().forEach { kind ->
                PermissionScope.values().forEach { scope ->
                    if (!payload.hasPermission(kind, scope)) ownPermissions[kind]!!.remove(scope)
                }
            }
        }

        val childPermissions = ownPermissions.clone()
        if (payload is Directory) {
            PermissionKind.values().forEach { kind ->
                if (!payload.hasPermission(kind, PermissionScope.Recursive))
                    ownPermissions[kind]!!.remove(PermissionScope.Self)
            }
        }

        val row = NodeRow(node, payload!!, parent, depth, this, ownPermissions)
        result.add(row)

        nodeDao()
            .getChildNodes(node.nodeId)
            .sortedBy { it.order }
            .forEach { add(it, row, depth + 1, childPermissions) }
    }

    nodeDao()
        .getChildNodes(rootNodeId ?: getRootNode().nodeId)
        .sortedBy { it.order }
        .forEach { add(it, null, 0, AllPermissions) }

    return result
}

/** Create Nodes and Applications for newly installed apps. Returns the number of new apps added. */
suspend fun AppDatabase.createNewApplications(activityInfos: List<LauncherActivityInfo>): Int {
    var newApps = 0
    val applicationsDirectory = getOrCreateSingletonDirectory(Directory.SpecialMode.NewApplications)

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
                        parentId = applicationsDirectory.nodeId,
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

suspend fun AppDatabase.getOrCreateSingletonDirectory(specialMode: Directory.SpecialMode): Node {
    val directories = directoryDao().getAllPayloads().filter { it.specialMode == specialMode }
    val nodeId =
        when (directories.size) {
            1 -> directories[0].nodeId
            0 -> {
                withTransaction {
                    val isRoot = specialMode == Directory.SpecialMode.Root
                    val nodeId = if (isRoot) ROOT_NODE_ID else 0
                    val parentId = if (isRoot) null else getRootNode().nodeId
                    insert(
                        Node(
                            nodeId = nodeId,
                            parentId = parentId,
                            kind = NodeKind.Directory,
                            order = 0,
                            label = specialMode.defaultDirectoryName,
                        )
                    )
                    val lastNodeId = if (isRoot) ROOT_NODE_ID else nodeDao().getLastNodeId()
                    insert(
                        Directory(
                            payloadId = 0,
                            nodeId = lastNodeId,
                            specialMode = specialMode,
                        )
                    )

                    lastNodeId
                }
            }
            else -> throw Exception("Multiple directories exist with special mode: $specialMode")
        }
    return nodeDao().getNodeById(nodeId)!!
}

/** Convenience function to get or create the root node. */
suspend fun AppDatabase.getRootNode(): Node =
    getOrCreateSingletonDirectory(Directory.SpecialMode.Root)

suspend fun AppDatabase.moveNode(node: Node, newParentNodeId: Int?) {
    node.parentId = newParentNodeId
    update(node)
    refreshNodeList()
}

suspend fun AppDatabase.moveToTrash(node: Node) {
    val trash = getOrCreateSingletonDirectory(Directory.SpecialMode.Trash)
    moveNode(node, trash.nodeId)
}
