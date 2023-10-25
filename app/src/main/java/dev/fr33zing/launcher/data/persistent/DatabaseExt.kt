package dev.fr33zing.launcher.data.persistent

import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.LauncherActivityInfo
import androidx.room.withTransaction
import dev.fr33zing.launcher.data.NodeKind
import dev.fr33zing.launcher.data.PermissionKind
import dev.fr33zing.launcher.data.PermissionScope
import dev.fr33zing.launcher.data.persistent.payloads.Application
import dev.fr33zing.launcher.data.persistent.payloads.Directory
import dev.fr33zing.launcher.data.persistent.payloads.Payload
import dev.fr33zing.launcher.data.persistent.payloads.mainPackageManager
import io.reactivex.rxjava3.subjects.PublishSubject
import kotlinx.coroutines.runBlocking

const val ROOT_NODE_ID = -1

val NodeCreatedSubject = PublishSubject.create<Pair<Int, Int>>() // createdNodeId, parentNodeId
val NodeUpdatedSubject = PublishSubject.create<Pair<Int, Int>>() // updatedNodeId, parentNodeId
val NodeDeletedSubject = PublishSubject.create<Pair<Int, Int>>() // deletedNodeId, parentNodeId
val NodeMovedSubject =
    PublishSubject.create<Triple<Int, Int, Int>>() // movedNodeId, fromParentNodeId, toParentNodeId

enum class RelativeNodeOffset(val orderOffset: Int) {
    Above(0),
    Within(0),
    Below(1),
}

data class RelativeNodePosition(val relativeToNodeId: Int, val offset: RelativeNodeOffset)

/** Create Nodes and Applications for newly installed apps. Returns the number of new apps added. */
suspend fun AppDatabase.createNewApplications(activityInfos: List<LauncherActivityInfo>): Int {
    var newApps = 0

    withTransaction {
        val newApplicationsDirectory =
            getOrCreateSingletonDirectory(Directory.SpecialMode.NewApplications)

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
                            parentId = newApplicationsDirectory.nodeId,
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

        if (newApps == 0) deleteRecursively(newApplicationsDirectory)
    }

    return newApps
}

suspend fun AppDatabase.autoCategorizeNewApplications(context: Context) {
    withTransaction {
        val newApplicationsDirectory =
            getOrCreateSingletonDirectory(Directory.SpecialMode.NewApplications)
        val nodesWithPayloads =
            nodeDao().getChildNodes(newApplicationsDirectory.nodeId).associateWith {
                (getPayloadByNodeId(it.kind, it.nodeId) ?: throw Exception("Node has no payload"))
                    as Application
            }
        val categoryDirectories =
            mutableMapOf<String, Pair<Node, Int>>() // category -> (directory, order)

        nodesWithPayloads.forEach { (node, payload) ->
            val applicationInfo = mainPackageManager.getApplicationInfo(payload.packageName, 0)
            val appCategory = applicationInfo.category
            var categoryTitle = "Uncategorized"
            try {
                categoryTitle = ApplicationInfo.getCategoryTitle(context, appCategory).toString()
            } catch (_: Exception) {}
            val (directory, order) =
                categoryDirectories[categoryTitle]
                    ?: Pair(
                        getOrCreateDirectoryByPath("Applications", categoryTitle) {
                            it.initialVisibility = Directory.InitialVisibility.Remember
                        },
                        0
                    )

            node.parentId = directory.nodeId
            node.order = order
            categoryDirectories[categoryTitle] = Pair(directory, order + 1)
        }

        updateMany(nodesWithPayloads.map { it.key })
        deleteRecursively(newApplicationsDirectory)
    }
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

    var event: Pair<Int, Int>? = null
    val createdNodeId = withTransaction {
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
        if (parentId != null) event = Pair(lastNodeId, parentId)
        lastNodeId
    }
    event?.let { NodeCreatedSubject.onNext(it) }
    return createdNodeId
}

suspend inline fun <reified T : Payload> AppDatabase.createNodeWithPayload(
    parentId: Int,
    label: String,
    crossinline mutateFunction: (T) -> Unit = {}
): Node = withTransaction {
    val nodeKind = nodeKindForPayloadClass<T>()
    insert(
        Node(
            nodeId = 0,
            parentId = parentId,
            kind = nodeKind,
            order = nodeDao().getLastNodeOrder(parentId),
            label = label,
        )
    )
    val lastNodeId = nodeDao().getLastNodeId()
    val payload = createDefaultPayloadForNode(nodeKind, lastNodeId)
    mutateFunction(payload as T)
    insert(payload)

    nodeDao().getNodeById(lastNodeId)!!
}

suspend fun AppDatabase.getOrCreateDirectoryByPath(
    vararg path: String,
    mutateFunction: (Directory) -> Unit = {}
): Node {
    var current = getRootNode()
    path.forEach { pathSegment ->
        current =
            nodeDao().getChildNodeByLabel(current.nodeId, pathSegment)
                ?: createNodeWithPayload(current.nodeId, pathSegment, mutateFunction)
    }
    return current
}

suspend fun AppDatabase.getOrCreateSingletonDirectory(specialMode: Directory.SpecialMode): Node {
    val directories = directoryDao().getAllPayloads().filter { it.specialMode == specialMode }
    val nodeId =
        when (directories.size) {
            1 -> directories[0].nodeId
            0 -> {
                var event: Pair<Int, Int>? = null
                val createdNodeId = withTransaction {
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

                    if (parentId != null) event = Pair(lastNodeId, parentId)
                    lastNodeId
                }
                event?.let { NodeCreatedSubject.onNext(it) }
                createdNodeId
            }
            else -> throw Exception("Multiple directories exist with special mode: $specialMode")
        }
    return nodeDao().getNodeById(nodeId)!!
}

/** Convenience function to get or create the root node. */
suspend fun AppDatabase.getRootNode(): Node =
    getOrCreateSingletonDirectory(Directory.SpecialMode.Root)

suspend fun AppDatabase.moveNode(node: Node, newParentNodeId: Int) {
    val oldParentNodeId = withTransaction {
        val oldParentNodeId = node.parentId
        node.parentId = newParentNodeId
        node.order = -1
        update(node)
        val oldSiblings = nodeDao().getChildNodes(oldParentNodeId).fixOrder()
        val newSiblings = nodeDao().getChildNodes(newParentNodeId).fixOrder()
        updateMany(oldSiblings + newSiblings)

        oldParentNodeId
    }
    NodeMovedSubject.onNext(
        Triple(
            node.nodeId,
            oldParentNodeId ?: throw Exception("Parent is null (cannot move root node)"),
            newParentNodeId
        )
    )
}

suspend fun AppDatabase.moveToTrash(node: Node) {
    val trash = getOrCreateSingletonDirectory(Directory.SpecialMode.Trash)
    moveNode(node, trash.nodeId)
}

suspend fun AppDatabase.deleteRecursively(node: Node) {
    val nodes = mutableListOf<Node>()
    val payloads = mutableListOf<Payload>()

    suspend fun add(node: Node) {
        nodes.add(node)
        getPayloadByNodeId(node.kind, node.nodeId)?.let { payloads.add(it) }
        nodeDao().getChildNodes(node.nodeId).forEach { add(it) }
    }
    add(node)

    withTransaction {
        deleteMany(nodes)
        payloads.forEach { delete(it) }
    }
    NodeDeletedSubject.onNext(
        Pair(
            node.nodeId,
            node.parentId ?: throw Exception("Node has no parent (cannot delete root node)")
        )
    )
}

suspend fun AppDatabase.traverseUpward(
    node: Node,
    includeFirst: Boolean = false,
    action: (Node) -> Boolean
) {
    if (includeFirst) {
        val shouldContinue = action(node)
        if (!shouldContinue) return
    }

    if (node.parentId == null) return

    val parent = nodeDao().getNodeById(node.parentId!!) ?: return

    traverseUpward(parent, true, action)
}

suspend fun AppDatabase.traverseUpwardWithPayload(
    node: Node,
    includeFirst: Boolean = false,
    action: (Node, Payload) -> Boolean
) {
    traverseUpward(node, includeFirst) {
        val payload =
            runBlocking { getPayloadByNodeId(node.kind, node.nodeId) }
                ?: throw Exception("Payload is null")

        action(node, payload)
    }
}

/**
 * Traverses upward to determine if the node has the permission. Note that this method is
 * inefficient and should be used sparingly.
 */
suspend fun AppDatabase.checkPermission(
    kind: PermissionKind,
    scope: PermissionScope,
    node: Node,
): Boolean {
    val selfPayload =
        getPayloadByNodeId(node.kind, node.nodeId) ?: throw Exception("Payload is null")
    if (selfPayload is Directory && !selfPayload.hasPermission(kind, scope)) return false

    var parentsAllow = true
    traverseUpwardWithPayload(node) { _, parentPayload ->
        if (
            parentPayload is Directory &&
                !parentPayload.hasPermission(kind, PermissionScope.Recursive)
        ) {
            parentsAllow = false
            false
        } else true
    }
    return parentsAllow
}
