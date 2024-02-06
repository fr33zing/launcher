package dev.fr33zing.launcher.data.viewmodel.utility

import androidx.compose.runtime.State
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import dev.fr33zing.launcher.data.NodeKind
import dev.fr33zing.launcher.data.persistent.AppDatabase
import dev.fr33zing.launcher.data.persistent.Node
import dev.fr33zing.launcher.data.persistent.ROOT_NODE_ID
import dev.fr33zing.launcher.data.persistent.payloads.Directory
import dev.fr33zing.launcher.data.utility.notNull
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChangedBy
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.flow.transformLatest

data class TreeState(val selectedNodeKey: String?)

data class TreeNodeState(
    val depth: Int,
    val showChildren: State<Boolean?>,
    val value: NodePayloadWithReferenceTargetState,
    val flow: Lazy<Flow<TreeNodeState>>
) {
    val key
        get() = makeKey(underlyingNodeId, depth)

    val underlyingNodeId
        get() = value.underlyingState.node.nodeId

    val underlyingNodeKind
        get() = value.underlyingState.node.kind
}

private fun makeKey(nodeId: Int, depth: Int) = "$depth > $nodeId"

@OptIn(ExperimentalCoroutinesApi::class)
class TreeStateHolder(private val db: AppDatabase, rootNodeId: Int = ROOT_NODE_ID) {
    private val selectedNodeKey by mutableStateOf<String?>("")
    private val treeNodeStateFlows = mutableStateMapOf<String, Flow<TreeNodeState>>()
    private val showChildren = mutableStateMapOf<String, Boolean>()

    val state by derivedStateOf { TreeState(selectedNodeKey) }

    fun onActivateNode(state: TreeNodeState) {
        if (state.key in showChildren) showChildren[state.key] = !showChildren[state.key]!!
    }

    private fun getTreeNodeStateFlow(depth: Int, node: Node): Flow<TreeNodeState> =
        treeNodeStateFlows.computeIfAbsent(makeKey(node.nodeId, depth)) {
            val parentStateHolder = NodePayloadStateHolder(db, node)

            parentStateHolder.flowWithReferenceTarget.mapLatest { value ->
                val showChildren = derivedStateOf {
                    showChildren[makeKey(value.underlyingState.node.nodeId, depth)]
                }
                val flow = lazy { getTreeNodeStateFlow(depth, node) }

                TreeNodeState(depth, showChildren, value, flow)
            }
        }

    val flow: Flow<List<TreeNodeState>> = flow {
        fun traverse(depth: Int = -1, node: Node): Flow<List<TreeNodeState>> {
            val parentFlow = getTreeNodeStateFlow(depth, node)

            return if (!canHaveChildren(node)) parentFlow.mapLatest { listOf(it) }
            else {
                val childrenFlow =
                    parentFlow
                        .distinctUntilChangedBy { it.value.node.nodeId }
                        .transformLatest { treeNode: TreeNodeState ->
                            emitAll(
                                db.nodeDao()
                                    .getChildNodesFlow(treeNode.value.node.nodeId)
                                    .distinctUntilChangedBy { it.map { node -> node.nodeId } }
                                    .flatMapLatest { childNodes ->
                                        val childNodeFlows =
                                            childNodes.map { childNode ->
                                                traverse(depth + 1, childNode)
                                            }
                                        combine(childNodeFlows) { arrayOfLists ->
                                            arrayOfLists.toList().flatten()
                                        }
                                    }
                            )
                        }
                        .distinctUntilChangedBy { it.map { state -> state.underlyingNodeId } }

                parentFlow.combine(childrenFlow) { treeNode, children ->
                    val showParent = treeNode.depth >= 0 // Do not show root node
                    val showChildren =
                        showChildren.computeIfAbsent(treeNode.key) {
                            val directory = treeNode.value.payload as? Directory
                            (directory?.collapsed ?: directory?.initiallyCollapsed) == false
                        }

                    val parentOrEmpty = if (showParent) listOf(treeNode) else emptyList()
                    val childrenOrEmpty = if (showChildren) children else emptyList()

                    parentOrEmpty + childrenOrEmpty
                }
            }
        }

        val rootNode = db.nodeDao().getNodeById(rootNodeId).notNull()
        val flow = traverse(node = rootNode)

        emitAll(flow)
    }
}

private fun canHaveChildren(node: Node) =
    node.kind == NodeKind.Directory || node.kind == NodeKind.Reference
