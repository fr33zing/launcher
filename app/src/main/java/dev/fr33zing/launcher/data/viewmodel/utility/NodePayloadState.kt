package dev.fr33zing.launcher.data.viewmodel.utility

import android.content.Context
import dev.fr33zing.launcher.data.persistent.AppDatabase
import dev.fr33zing.launcher.data.persistent.Node
import dev.fr33zing.launcher.data.persistent.payloads.Payload
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map

data class NodePayloadState(val node: Node, val payload: Payload, val activate: (Context) -> Unit)

class NodePayloadStateHolder(db: AppDatabase, val node: Node) {
    val flow =
        db.getPayloadFlowByNodeId(node.kind, node.nodeId).map { payload ->
            NodePayloadState(node, payload ?: throw Exception("Payload is null")) { context ->
                payload.activate(db, context)
            }
        }
}

class NodePayloadListStateHolder(db: AppDatabase, nodes: List<Node>) {
    val flow = combine(nodes.map { node -> NodePayloadStateHolder(db, node).flow }) { it }
}
