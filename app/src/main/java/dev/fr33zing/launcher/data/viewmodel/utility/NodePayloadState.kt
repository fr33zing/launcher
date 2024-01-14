package dev.fr33zing.launcher.data.viewmodel.utility

import android.content.Context
import dev.fr33zing.launcher.data.persistent.AppDatabase
import dev.fr33zing.launcher.data.persistent.Node
import dev.fr33zing.launcher.data.persistent.payloads.Payload
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map

data class NodePayloadState(
    val node: Node,
    val payload: Payload,
    val activate: (Context) -> Unit = {}
) {
    companion object {
        suspend fun fromNode(
            db: AppDatabase,
            node: Node,
            activate: (Context) -> Unit = {}
        ): NodePayloadState {
            val payload =
                db.getPayloadByNodeId(node.kind, node.nodeId) ?: throw Exception("Payload is null")
            return NodePayloadState(node, payload, activate)
        }

        suspend fun fromNodeId(
            db: AppDatabase,
            nodeId: Int,
            activate: (Context) -> Unit = {}
        ): NodePayloadState {
            val node = db.nodeDao().getNodeById(nodeId) ?: throw Exception("Node is null")
            return fromNode(db, node, activate)
        }
    }
}

class NodePayloadStateHolder(
    db: AppDatabase,
    val node: Node,
) {
    val flow =
        db.getPayloadFlowByNodeId(node.kind, node.nodeId).map { payload ->
            NodePayloadState(node, payload ?: throw Exception("Payload is null")) { context ->
                payload.activate(db, context)
            }
        }
}

class NodePayloadListStateHolder(
    db: AppDatabase,
    nodes: List<Node>,
    filterPredicate: ((NodePayloadState) -> Boolean)? = null,
) {
    val flow =
        combine(
            nodes.map { node -> NodePayloadStateHolder(db, node).flow.maybeFilter(filterPredicate) }
        ) {
            it
        }
}
