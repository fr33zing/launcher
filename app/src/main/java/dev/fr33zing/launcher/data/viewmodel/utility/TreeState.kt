package dev.fr33zing.launcher.data.viewmodel.utility

import dev.fr33zing.launcher.data.NodeKind
import dev.fr33zing.launcher.data.persistent.AppDatabase
import dev.fr33zing.launcher.data.persistent.Node
import dev.fr33zing.launcher.data.persistent.ROOT_NODE_ID
import dev.fr33zing.launcher.data.persistent.payloads.Directory
import dev.fr33zing.launcher.data.utility.notNull
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.distinctUntilChangedBy
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.transformLatest

data class TreeNodeState(
    val depth: Int,
    val nodePayload: NodePayloadWithReferenceTargetState,
) {
    val underlyingNodeId
        get() = nodePayload.underlyingState.node.nodeId

    val key
        get() = "$depth > $underlyingNodeId"
}

@OptIn(ExperimentalCoroutinesApi::class)
class TreeStateHolder(db: AppDatabase, rootNodeId: Int = ROOT_NODE_ID) {
    private val showChildren = mutableMapOf<String, Boolean>()

    fun onActivateNode(state: TreeNodeState) {
        showChildren[state.key] = !showChildren[state.key]!!
    }

    val flow: Flow<List<TreeNodeState>> = flow {
        fun traverse(node: Node, depth: Int = -1): Flow<List<TreeNodeState>> {
            val parentStateHolder = NodePayloadStateHolder(db, node)
            val parentFlow =
                parentStateHolder.flowWithReferenceTarget.mapLatest { nodePayload ->
                    TreeNodeState(depth, nodePayload)
                }

            return if (!canHaveChildren(node)) parentFlow.mapLatest { listOf(it) }
            else {
                val childrenFlow =
                    parentFlow
                        .distinctUntilChangedBy { it.nodePayload.node.nodeId }
                        .transformLatest {
                            emitAll(
                                db.nodeDao()
                                    .getChildNodesFlow(it.nodePayload.node.nodeId)
                                    .distinctUntilChanged()
                                    .flatMapLatest { childNodes ->
                                        val childNodeFlows =
                                            childNodes.map { childNode ->
                                                traverse(childNode, depth + 1)
                                            }
                                        combine(childNodeFlows) { arrayOfLists ->
                                            arrayOfLists.toList().flatten()
                                        }
                                    }
                                    .onStart { emit(emptyList()) }
                            )
                        }

                parentFlow.combine(childrenFlow) { parent, children ->
                    val showParent = parent.depth >= 0 // Do not show root node
                    val showChildren =
                        showChildren.computeIfAbsent(parent.key) {
                            (parent.nodePayload.payload as? Directory)?.collapsed != true
                        }

                    val parentOrEmpty = if (showParent) listOf(parent) else emptyList()
                    val childrenOrEmpty = if (showChildren) children else emptyList()

                    parentOrEmpty + childrenOrEmpty
                }
            }
        }

        val rootNode = db.nodeDao().getNodeById(rootNodeId).notNull()
        val flow = traverse(rootNode)

        emitAll(flow)
    }
}

private fun canHaveChildren(node: Node) =
    node.kind == NodeKind.Directory || node.kind == NodeKind.Reference
