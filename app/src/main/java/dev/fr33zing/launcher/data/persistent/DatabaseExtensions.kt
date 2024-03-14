package dev.fr33zing.launcher.data.persistent

import android.content.Context
import android.content.pm.LauncherActivityInfo
import androidx.room.withTransaction
import dev.fr33zing.launcher.data.NodeKind
import dev.fr33zing.launcher.data.PermissionKind
import dev.fr33zing.launcher.data.PermissionScope
import dev.fr33zing.launcher.data.persistent.payloads.Application
import dev.fr33zing.launcher.data.persistent.payloads.Directory
import dev.fr33zing.launcher.data.persistent.payloads.Payload
import dev.fr33zing.launcher.data.utility.DEFAULT_CATEGORY_NAME
import dev.fr33zing.launcher.data.utility.getApplicationCategoryName
import dev.fr33zing.launcher.data.utility.getApplicationCategoryOverrides
import dev.fr33zing.launcher.data.utility.notNull
import io.reactivex.rxjava3.subjects.PublishSubject
import kotlinx.coroutines.runBlocking
import java.io.File

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
        val filteredActivityInfos =
            activityInfos.filter { activityInfo ->
                applicationDao().getAllPayloads().find { app ->
                    app.appName == activityInfo.label.toString()
                } == null
            }

        if (filteredActivityInfos.isEmpty()) return@withTransaction

        val newApplicationsDirectory =
            getOrCreateSingletonDirectory(Directory.SpecialMode.NewApplications)

        filteredActivityInfos.forEachIndexed { index, activityInfo ->
            nodeDao()
                .insert(
                    Node(
                        nodeId = 0,
                        parentId = newApplicationsDirectory.nodeId,
                        kind = NodeKind.Application,
                        order = index,
                        label = activityInfo.label.toString(),
                    ),
                )
            applicationDao()
                .insert(
                    Application(
                        payloadId = 0,
                        nodeId = nodeDao().getLastNodeId(),
                        activityInfo = activityInfo,
                    ),
                )
            newApps++
        }
    }

    return newApps
}

// TODO make this handle interruption gracefully
suspend fun AppDatabase.autoCategorizeNewApplications(
    context: Context,
    onCategorized: () -> Unit,
) {
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
        val applicationCategoryOverrides = getApplicationCategoryOverrides()

        getOrCreateDirectoryByPath("Applications") { it.collapsed = false }

        nodesWithPayloads.forEach { (node, payload) ->
            val category =
                getApplicationCategoryName(context, payload.packageName, applicationCategoryOverrides)
            val (directory, order) =
                categoryDirectories[category]
                    ?: Pair(
                        getOrCreateDirectoryByPath("Applications", category) {
                            it.initialVisibility = Directory.InitialVisibility.Remember
                            it.collapsed = true
                        },
                        0,
                    )

            node.parentId = directory.nodeId
            node.order = order
            categoryDirectories[category] = Pair(directory, order + 1)

            onCategorized()
        }

        categoryDirectories.values
            .sortedBy {
                // HACK to force uncategorized apps directory to bottom
                // TODO create function: fixOrderRecursively
                if (it.first.label == DEFAULT_CATEGORY_NAME) "Z".repeat(256) else it.first.label
            }
            .forEachIndexed { index, (node, _) -> node.order = index }

        updateMany(categoryDirectories.values.map { it.first })
        updateMany(nodesWithPayloads.map { it.key })
    }
}

/** Create a new node relative to another node. Returns the new node's id. */
suspend fun AppDatabase.createNode(
    position: RelativeNodePosition,
    newNodeKind: NodeKind,
): Int {
    val relativeToNode =
        nodeDao().getNodeById(position.relativeToNodeId) ?: throw Exception("Node does not exist")
    val parentId =
        if (position.offset == RelativeNodeOffset.Within) {
            position.relativeToNodeId
        } else {
            relativeToNode.parentId
        }
    val order =
        if (position.offset == RelativeNodeOffset.Within) {
            0
        } else {
            relativeToNode.order + position.offset.orderOffset
        }
    val createdNodeId =
        withTransaction {
            val siblings = nodeDao().getChildNodes(parentId).fixOrder()
            siblings.filter { it.order >= order }.forEach { it.order++ }
            updateMany(siblings)
            insert(Node(nodeId = 0, parentId = parentId, kind = newNodeKind, order = order, label = ""))
            val lastNodeId = nodeDao().getLastNodeId()
            insert(createDefaultPayloadForNode(newNodeKind, lastNodeId))
            lastNodeId
        }
    return createdNodeId
}

suspend inline fun <reified T : Payload> AppDatabase.createNodeWithPayload(
    parentId: Int,
    label: String,
    crossinline nodeMutateFunction: (Node) -> Unit = {},
    crossinline payloadMutateFunction: (T) -> Unit = {},
): Node =
    withTransaction {
        val nodeKind = nodeKindForPayloadClass<T>()
        val node =
            Node(
                nodeId = 0,
                parentId = parentId,
                kind = nodeKind,
                order = nodeDao().getLastNodeOrder(parentId),
                label = label,
            )
        nodeMutateFunction(node)
        insert(node)
        val lastNodeId = nodeDao().getLastNodeId()
        val payload = createDefaultPayloadForNode(nodeKind, lastNodeId)
        payloadMutateFunction(payload as T)
        insert(payload)

        nodeDao().getNodeById(lastNodeId)!!
    }

suspend fun AppDatabase.getOrCreateDirectoryByPath(
    vararg path: String,
    payloadMutateFunction: (Directory) -> Unit = {},
): Node {
    var current = getRootNode()
    path.forEach { pathSegment ->
        current =
            nodeDao().getChildNodeByLabel(parentId = current.nodeId, label = pathSegment)
                ?: createNodeWithPayload(
                    parentId = current.nodeId,
                    label = pathSegment,
                    payloadMutateFunction = payloadMutateFunction,
                )
    }
    return current
}

suspend fun AppDatabase.getOrCreateSingletonDirectory(specialMode: Directory.SpecialMode): Node =
    withTransaction {
        val directory = directoryDao().getBySpecialMode(specialMode)
        val nodeId =
            directory?.nodeId
                ?: run {
                    val isRoot = specialMode == Directory.SpecialMode.Root
                    val nodeId = if (isRoot) ROOT_NODE_ID else 0
                    val parentId = if (isRoot) null else getRootNode().nodeId
                    insert(
                        Node(
                            nodeId = nodeId,
                            parentId = parentId,
                            kind = NodeKind.Directory,
                            order = -1,
                            label = specialMode.defaultDirectoryName,
                        ),
                    )
                    val lastNodeId = if (isRoot) ROOT_NODE_ID else nodeDao().getLastNodeId()
                    insert(
                        Directory(
                            payloadId = 0,
                            nodeId = lastNodeId,
                            specialMode = specialMode,
                            collapsed = specialMode.initiallyCollapsed,
                        ),
                    )
                    nodeDao().getChildNodes(parentId).fixOrder().let { updateMany(it) }
                    lastNodeId
                }
        nodeDao().getNodeById(nodeId).notNull()
    }

/** Convenience function to get or create the root node. */
suspend fun AppDatabase.getRootNode(): Node = getOrCreateSingletonDirectory(Directory.SpecialMode.Root)

suspend fun AppDatabase.moveNode(
    node: Node,
    newParentNodeId: Int,
) = withTransaction {
    val oldParentNodeId = node.parentId
    node.parentId = newParentNodeId
    node.order = -1
    update(node)
    val oldSiblings = nodeDao().getChildNodes(oldParentNodeId).fixOrder()
    val newSiblings = nodeDao().getChildNodes(newParentNodeId).fixOrder()
    updateMany(oldSiblings + newSiblings)
}

suspend fun AppDatabase.moveNodes(
    nodes: List<Node>,
    newParentNodeId: Int,
) = withTransaction {
    if (nodes.isEmpty()) throw Exception("List of nodes to move is empty")

    val oldParentNodeIds = nodes.map { it.parentId }.toSet()

    nodes.forEachIndexed { index, node ->
        node.parentId = newParentNodeId
        node.order = -index - 1
    }
    updateMany(nodes)
    val oldSiblings = oldParentNodeIds.flatMap { nodeDao().getChildNodes(it).fixOrder() }
    val newSiblings = nodeDao().getChildNodes(newParentNodeId).fixOrder()
    updateMany(oldSiblings + newSiblings)
}

suspend fun AppDatabase.moveNodeToTrash(node: Node) {
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
        nodeDao().getChildNodes(node.parentId ?: ROOT_NODE_ID).fixOrder().let { updateMany(it) }
    }
}

suspend fun AppDatabase.traverseUpward(
    node: Node,
    includeFirst: Boolean = false,
    action: (Node) -> Boolean,
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
    startNode: Node,
    includeFirst: Boolean = false,
    action: (Node, Payload) -> Boolean,
) {
    traverseUpward(startNode, includeFirst) { node ->
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
    val selfPayload = getPayloadByNodeId(node.kind, node.nodeId) ?: throw Exception("Payload is null")
    if (selfPayload is Directory && !selfPayload.hasPermission(kind, scope)) return false

    var parentsAllow = true
    traverseUpwardWithPayload(node) { _, parentPayload ->
        if (parentPayload is Directory &&
            !parentPayload.hasPermission(kind, PermissionScope.Recursive)
        ) {
            parentsAllow = false
            false
        } else {
            true
        }
    }
    return parentsAllow
}

suspend fun AppDatabase.nodeLineage(node: Node): ArrayDeque<Node> =
    ArrayDeque<Node>().also { stack ->
        stack.add(node)
        while (stack.first().nodeId != ROOT_NODE_ID) {
            val parentId = stack.first().parentId ?: break
            val parentNode = nodeDao().getNodeById(parentId) ?: throw Exception("Node is null")
            stack.addFirst(parentNode)
        }
    }

suspend fun AppDatabase.nodeLineage(nodeId: Int): ArrayDeque<Node> =
    nodeDao().getNodeById(nodeId).notNull().let {
        nodeLineage(it)
    }

fun AppDatabase.checkpoint() {
    if (!query("PRAGMA wal_checkpoint", arrayOf()).moveToFirst()) {
        throw Exception("Database checkpoint failed")
    }
}

fun AppDatabase.getDatabaseFile(): File {
    return File(openHelper.writableDatabase.path ?: throw Exception("Database path is null"))
}

/** Run wal_checkpoint and return the database file. */
fun AppDatabase.getCheckpointedDatabaseFile(): File {
    checkpoint()
    return getDatabaseFile()
}
