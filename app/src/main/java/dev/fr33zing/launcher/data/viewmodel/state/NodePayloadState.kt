package dev.fr33zing.launcher.data.viewmodel.state

import androidx.compose.runtime.Immutable
import dev.fr33zing.launcher.data.NodeKind
import dev.fr33zing.launcher.data.persistent.AppDatabase
import dev.fr33zing.launcher.data.persistent.Node
import dev.fr33zing.launcher.data.persistent.payloads.Payload
import dev.fr33zing.launcher.data.persistent.payloads.Reference
import dev.fr33zing.launcher.data.utility.NullNodeException
import dev.fr33zing.launcher.data.utility.NullPayloadException
import dev.fr33zing.launcher.data.utility.PayloadClassMismatchException
import dev.fr33zing.launcher.data.utility.maybeFilter
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.transform

@Immutable
open class NodePayloadState(
    val node: Node,
    val payload: Payload,
) {
    operator fun component1() = node

    operator fun component2() = payload

    companion object {
        suspend fun fromNode(
            db: AppDatabase,
            node: Node,
        ): NodePayloadState {
            val payload =
                db.getPayloadByNodeId(node.kind, node.nodeId) ?: throw NullPayloadException(node)

            return NodePayloadState(node, payload)
        }

        suspend fun fromNodeId(
            db: AppDatabase,
            nodeId: Int,
        ): NodePayloadState {
            val node = db.nodeDao().getNodeById(nodeId) ?: throw NullNodeException()
            return fromNode(db, node)
        }
    }
}

@Immutable
class ReferenceFollowingNodePayloadState(
    val underlyingState: NodePayloadState,
    targetState: NodePayloadState?,
) :
    NodePayloadState(
        node = (targetState ?: underlyingState).node,
        payload = (targetState ?: underlyingState).payload,
    ) {

    val isValidReference = targetState != null

    companion object {
        suspend fun fromNode(
            db: AppDatabase,
            underlyingNode: Node,
        ): ReferenceFollowingNodePayloadState {
            val underlyingState = NodePayloadState.fromNode(db, underlyingNode)
            val targetState =
                (underlyingState.payload as? Reference)?.targetId?.let { targetId ->
                    fromNodeId(db, targetId)
                }

            return ReferenceFollowingNodePayloadState(underlyingState, targetState)
        }
    }
}

class NodePayloadStateHolder(
    db: AppDatabase,
    val node: Node,
) {
    val flow: Flow<NodePayloadState> =
        db.getPayloadFlowByNodeId(node.kind, node.nodeId).filterNotNull().map { payload ->
            NodePayloadState(node, payload)
        }

    val flowWithReferenceTarget: Flow<ReferenceFollowingNodePayloadState> =
        flow.transform { state ->
            val targetFlow: Flow<ReferenceFollowingNodePayloadState>? =
                if (node.kind != NodeKind.Reference) null
                else {
                    (state.payload as? Reference ?: throw PayloadClassMismatchException(state.node))
                        .targetId
                        ?.let { targetId -> db.nodeDao().getNodeById(targetId) }
                        ?.let { targetNode ->
                            db.getPayloadFlowByNodeId(targetNode.kind, targetNode.nodeId)
                                .filterNotNull()
                                .map { targetPayload ->
                                    ReferenceFollowingNodePayloadState(
                                        underlyingState = state,
                                        targetState = NodePayloadState(targetNode, targetPayload)
                                    )
                                }
                        }
                }

            if (targetFlow != null) emitAll(targetFlow)
            else emit(ReferenceFollowingNodePayloadState(state, null))
        }
}

class NodePayloadListStateHolder(
    db: AppDatabase,
    nodes: List<Node>,
    filterPredicate: ((ReferenceFollowingNodePayloadState) -> Boolean)? = null,
) {
    val flow =
        combine(
            nodes.map { node ->
                NodePayloadStateHolder(db, node)
                    .flowWithReferenceTarget
                    .maybeFilter(filterPredicate)
            },
            transform = { it }
        )
}
