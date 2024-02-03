package dev.fr33zing.launcher.data.viewmodel.utility

import android.content.Context
import dev.fr33zing.launcher.data.NodeKind
import dev.fr33zing.launcher.data.persistent.AppDatabase
import dev.fr33zing.launcher.data.persistent.Node
import dev.fr33zing.launcher.data.persistent.payloads.Payload
import dev.fr33zing.launcher.data.persistent.payloads.Reference
import dev.fr33zing.launcher.data.utility.NullNodeException
import dev.fr33zing.launcher.data.utility.NullPayloadException
import dev.fr33zing.launcher.data.utility.PayloadClassMismatchException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.transform

open class NodePayloadState(
    val node: Node,
    val payload: Payload,
    val activate: (Context) -> Unit = {}
) {
    operator fun component1() = node

    operator fun component2() = payload

    operator fun component3() = activate

    companion object {
        suspend fun fromNode(
            db: AppDatabase,
            node: Node,
            activate: (Context) -> Unit = {}
        ): NodePayloadState {
            val payload =
                db.getPayloadByNodeId(node.kind, node.nodeId) ?: throw NullPayloadException(node)

            return NodePayloadState(node, payload, activate)
        }

        suspend fun fromNodeId(
            db: AppDatabase,
            nodeId: Int,
            activate: (Context) -> Unit = {}
        ): NodePayloadState {
            val node = db.nodeDao().getNodeById(nodeId) ?: throw NullNodeException()
            return fromNode(db, node, activate)
        }
    }
}

class NodePayloadWithReferenceTargetState(
    val underlyingState: NodePayloadState,
    targetState: NodePayloadState?,
    activate: (Context) -> Unit = {},
) :
    NodePayloadState(
        node = (targetState ?: underlyingState).node,
        payload = (targetState ?: underlyingState).payload,
        activate = activate
    ) {

    val isValidReference = targetState != null

    companion object {
        suspend fun fromNode(
            db: AppDatabase,
            underlyingNode: Node,
            activate: (Context) -> Unit = {}
        ): NodePayloadWithReferenceTargetState {
            val underlyingState = NodePayloadState.fromNode(db, underlyingNode, activate)
            val targetState =
                (underlyingState.payload as? Reference)?.targetId?.let { targetId ->
                    fromNodeId(db, targetId)
                }

            return NodePayloadWithReferenceTargetState(underlyingState, targetState)
        }
    }
}

class NodePayloadStateHolder(db: AppDatabase, val node: Node) {
    val flow: Flow<NodePayloadState> =
        db.getPayloadFlowByNodeId(node.kind, node.nodeId).map { payload ->
            NodePayloadState(
                node,
                payload ?: throw NullPayloadException(node),
            ) { context ->
                payload.activate(db, context)
            }
        }

    val flowWithReferenceTarget: Flow<NodePayloadWithReferenceTargetState> =
        flow.transform { state ->
            val targetFlow: Flow<NodePayloadWithReferenceTargetState>? =
                if (node.kind != NodeKind.Reference) null
                else {
                    (state.payload as? Reference ?: throw PayloadClassMismatchException(state.node))
                        .targetId
                        ?.let { targetId -> db.nodeDao().getNodeById(targetId) }
                        ?.let { targetNode ->
                            db.getPayloadFlowByNodeId(targetNode.kind, targetNode.nodeId)
                                .filterNotNull()
                                .map { targetPayload ->
                                    val targetState = NodePayloadState(targetNode, targetPayload)
                                    NodePayloadWithReferenceTargetState(state, targetState) {
                                        context ->
                                        targetPayload.activate(db, context)
                                    }
                                }
                        }
                }

            if (targetFlow != null) emitAll(targetFlow)
            else {
                emit(
                    NodePayloadWithReferenceTargetState(state, null) { context ->
                        state.payload.activate(db, context)
                    }
                )
            }
        }
}

class NodePayloadListStateHolder(
    db: AppDatabase,
    nodes: List<Node>,
    filterPredicate: ((NodePayloadWithReferenceTargetState) -> Boolean)? = null,
) {
    val flow =
        combine(
            nodes.map { node ->
                NodePayloadStateHolder(db, node)
                    .flowWithReferenceTarget
                    .maybeFilter(filterPredicate)
            }
        ) {
            it
        }
}
