package dev.fr33zing.launcher.data.viewmodel.state

import android.os.Parcel
import android.os.Parcelable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.State
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.mutableStateMapOf
import dev.fr33zing.launcher.data.AllPermissions
import dev.fr33zing.launcher.data.NodeKind
import dev.fr33zing.launcher.data.PermissionMap
import dev.fr33zing.launcher.data.adjustChildPermissions
import dev.fr33zing.launcher.data.adjustOwnPermissions
import dev.fr33zing.launcher.data.clone
import dev.fr33zing.launcher.data.persistent.AppDatabase
import dev.fr33zing.launcher.data.persistent.Node
import dev.fr33zing.launcher.data.persistent.ROOT_NODE_ID
import dev.fr33zing.launcher.data.persistent.payloads.Directory
import dev.fr33zing.launcher.data.utility.notNull
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChangedBy
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.flow.transformLatest
import kotlinx.coroutines.flow.update

@Immutable
data class TreeNodeKey(val nodeId: Int, val depth: Int) : Parcelable {
    constructor(parcel: Parcel) : this(parcel.readInt(), parcel.readInt()) {}

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeInt(nodeId)
        parcel.writeInt(depth)
    }

    override fun describeContents(): Int = 0

    companion object CREATOR : Parcelable.Creator<TreeNodeKey> {
        override fun createFromParcel(parcel: Parcel): TreeNodeKey = TreeNodeKey(parcel)

        override fun newArray(size: Int): Array<TreeNodeKey?> = arrayOfNulls(size)
    }
}

@Immutable data class TreeState(val selectedKey: TreeNodeKey? = null)

@Immutable
data class TreeNodeState(
    val depth: Int,
    val showChildren: State<Boolean?>,
    val permissions: PermissionMap,
    val value: ReferenceFollowingNodePayloadState,
    val flow: Lazy<Flow<TreeNodeState>>
) {
    val key = TreeNodeKey(underlyingNodeId, depth)

    val underlyingNodeId
        get() = value.underlyingState.node.nodeId

    val underlyingNodeKind
        get() = value.underlyingState.node.kind
}

@OptIn(ExperimentalCoroutinesApi::class)
class TreeStateHolder(private val db: AppDatabase, rootNodeId: Int = ROOT_NODE_ID) {
    private val treeNodeStateFlows = mutableStateMapOf<TreeNodeKey, Flow<TreeNodeState>>()
    private val showChildren = mutableStateMapOf<TreeNodeKey, Boolean>()

    private val _stateFlow = MutableStateFlow(TreeState())
    val stateFlow = _stateFlow.asStateFlow()

    fun onActivateNode(state: TreeNodeState) {
        if (state.key in showChildren) showChildren[state.key] = !showChildren[state.key]!!
    }

    fun onSelectNode(key: TreeNodeKey) {
        _stateFlow.update { it.copy(selectedKey = key) }
    }

    fun onClearSelectedNode() {
        _stateFlow.update { it.copy(selectedKey = null) }
    }

    val listFlow: Flow<List<TreeNodeState>> = flow {
        fun traverse(
            depth: Int = -1,
            node: Node,
            permissions: PermissionMap = AllPermissions.clone()
        ): Flow<List<TreeNodeState>> {
            val parentFlow = getTreeNodeStateFlow(depth, node, permissions)

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
                                                val childPermissions =
                                                    permissions.adjustChildPermissions(
                                                        treeNode.value.payload
                                                    )
                                                traverse(depth + 1, childNode, childPermissions)
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

    private fun getTreeNodeStateFlow(
        depth: Int,
        node: Node,
        parentPermissions: PermissionMap
    ): Flow<TreeNodeState> =
        treeNodeStateFlows.computeIfAbsent(TreeNodeKey(node.nodeId, depth)) {
            val parentStateHolder = NodePayloadStateHolder(db, node)

            parentStateHolder.flowWithReferenceTarget.mapLatest { value ->
                val showChildren = derivedStateOf {
                    showChildren[TreeNodeKey(value.underlyingState.node.nodeId, depth)]
                }
                val permissions = parentPermissions.adjustOwnPermissions(value.payload)
                val flow = lazy { // TODO reuse current flow somehow
                    getTreeNodeStateFlow(depth, node, parentPermissions)
                }

                TreeNodeState(
                    depth,
                    showChildren,
                    permissions,
                    value,
                    flow,
                )
            }
        }
}

private fun canHaveChildren(node: Node) =
    node.kind == NodeKind.Directory || node.kind == NodeKind.Reference
