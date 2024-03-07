package dev.fr33zing.launcher.data.viewmodel.state

import android.os.Parcel
import android.os.Parcelable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.State
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import dev.fr33zing.launcher.data.AllPermissions
import dev.fr33zing.launcher.data.NodeKind
import dev.fr33zing.launcher.data.PermissionMap
import dev.fr33zing.launcher.data.adjustChildPermissions
import dev.fr33zing.launcher.data.adjustOwnPermissions
import dev.fr33zing.launcher.data.clone
import dev.fr33zing.launcher.data.persistent.AppDatabase
import dev.fr33zing.launcher.data.persistent.Node
import dev.fr33zing.launcher.data.persistent.ROOT_NODE_ID
import dev.fr33zing.launcher.data.persistent.deleteRecursively
import dev.fr33zing.launcher.data.persistent.nodeLineage
import dev.fr33zing.launcher.data.persistent.payloads.Directory
import dev.fr33zing.launcher.data.utility.castOrNull
import dev.fr33zing.launcher.data.utility.notNull
import kotlin.reflect.jvm.jvmName
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChangedBy
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.transformLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@Immutable
data class TreeNodeKey(val nodeLineage: List<Int>) : Parcelable {
    constructor(
        parcel: Parcel
    ) : this(
        mutableListOf<Int>().also { parcel.readList(it, it.javaClass.classLoader, Int::class.java) }
    )

    fun childKey(childNodeId: Int) = TreeNodeKey(nodeLineage + listOf(childNodeId))

    fun parentKey() =
        if (nodeLineage.size == 1) emptyKey()
        else TreeNodeKey(nodeLineage.subList(0, nodeLineage.size - 2))

    fun directDescendantOf(key: TreeNodeKey) = key == parentKey()

    override fun writeToParcel(parcel: Parcel, flags: Int) = parcel.writeList(nodeLineage)

    override fun describeContents(): Int = 0

    companion object CREATOR : Parcelable.Creator<TreeNodeKey> {
        fun rootKey(nodeId: Int) = TreeNodeKey(listOf(nodeId))

        fun emptyKey() = TreeNodeKey(emptyList())

        override fun createFromParcel(parcel: Parcel): TreeNodeKey = TreeNodeKey(parcel)

        override fun newArray(size: Int): Array<TreeNodeKey?> = arrayOfNulls(size)
    }
}

fun TreeState.BatchState?.selectedCount() = this?.selectedKeys?.count { it.value } ?: 0

@Immutable
data class TreeState(
    val mode: Mode = Mode.Normal,
    val normalState: NormalState = NormalState(),
    val batchState: BatchState? = null,
    val moveState: MoveState? = null,
) {
    //
    // Classes
    //

    enum class Mode(val relevance: (TreeState, TreeNodeState) -> NodeRelevance) {
        Normal({ _, _ -> NodeRelevance.Relevant }),
        Batch({ treeState, treeNodeState ->
            val nodeParentKey = treeNodeState.key.parentKey()
            val batchParentKey = treeState.batchState?.parentKey
            val relevant = batchParentKey != null && nodeParentKey == batchParentKey

            if (relevant) NodeRelevance.Relevant else NodeRelevance.Irrelevant
        }),
        Move({ treeState, treeNodeState ->
            if (treeNodeState.key == treeState.moveState!!.parentKey) NodeRelevance.Irrelevant
            else if (treeState.moveState.movingKeys.getOrDefault(treeNodeState.key, false))
                NodeRelevance.Irrelevant
            else if (treeNodeState.value.node.kind == NodeKind.Directory) NodeRelevance.Relevant
            else NodeRelevance.Disruptive
        })
    }

    @Immutable data class NormalState(val selectedKey: TreeNodeKey? = null)

    @Immutable
    data class BatchState(
        val parentKey: TreeNodeKey = TreeNodeKey.emptyKey(),
        val selectedKeys: Map<TreeNodeKey, Boolean> = emptyMap()
    )

    @Immutable
    data class MoveState(
        val parentKey: TreeNodeKey = TreeNodeKey.emptyKey(),
        val movingKeys: Map<TreeNodeKey, Boolean> = emptyMap()
    )

    //
    // Functions
    //

    fun isBatchSelected(key: TreeNodeKey): Boolean =
        modeState<BatchState>().selectedKeys.getOrDefault(key, false)

    fun isMoving(key: TreeNodeKey): Boolean =
        modeState<MoveState>().movingKeys.getOrDefault(key, false)

    inline fun <reified T> modeState(): T =
        when (T::class) {
            BatchState::class -> {
                expectMode(Mode.Batch)
                batchState as? T ?: throw InvalidModeStateValueException(Mode.Batch)
            }
            MoveState::class -> {
                expectMode(Mode.Move)
                moveState as? T ?: throw InvalidModeStateValueException(Mode.Move)
            }
            else -> throw InvalidModeStateClassException(T::class.simpleName ?: T::class.jvmName)
        }

    fun expectMode(expectedMode: Mode) {
        if (mode != expectedMode) throw UnexpectedModeException(mode, expectedMode)
    }

    fun changeMode(from: Mode, to: Mode): TreeState {
        expectMode(from)
        return changeMode(to)
    }

    private fun changeMode(nextMode: Mode): TreeState =
        when (mode) {
            Mode.Normal -> {
                when (nextMode) {
                    Mode.Normal -> sameMode()
                    Mode.Batch ->
                        copy(
                            normalState = NormalState(),
                            mode = Mode.Batch,
                            batchState = BatchState(),
                        )
                    else -> invalidModeChange(nextMode)
                }
            }
            Mode.Batch -> {
                when (nextMode) {
                    Mode.Batch -> sameMode()
                    Mode.Normal ->
                        copy(
                            batchState = null,
                            mode = Mode.Normal,
                            normalState = NormalState(),
                        )
                    Mode.Move ->
                        copy(
                            batchState = null,
                            mode = Mode.Move,
                            moveState =
                                MoveState(
                                    parentKey = batchState!!.parentKey,
                                    movingKeys = batchState.selectedKeys
                                )
                        )
                }
            }
            Mode.Move ->
                when (nextMode) {
                    Mode.Move -> sameMode()
                    Mode.Normal ->
                        copy(
                            moveState = null,
                            mode = Mode.Normal,
                        )
                    Mode.Batch ->
                        copy(
                            moveState = null,
                            mode = Mode.Batch,
                            batchState =
                                BatchState(
                                    parentKey = moveState!!.parentKey,
                                    selectedKeys = moveState.movingKeys
                                )
                        )
                }
        }

    private fun sameMode(): Nothing = throw SameModeException(mode)

    private fun invalidModeChange(nextMode: Mode): Nothing =
        throw InvalidModeChangeException(mode, nextMode)

    //
    // Exceptions
    //

    class UnexpectedModeException(mode: Mode, expectedMode: Mode) :
        Exception("Tree is in $mode mode when $expectedMode mode is expected")

    class SameModeException(mode: Mode) : Exception("Tree is already in $mode mode")

    class InvalidModeChangeException(mode: Mode, nextMode: Mode) :
        Exception("Tree cannot change from $mode mode to $nextMode mode")

    class InvalidModeStateValueException(mode: Mode) : Exception("Invalid state for $mode mode")

    class InvalidModeStateClassException(className: String) :
        Exception("Class $className does not match any modes")
}

@Immutable
data class TreeNodeState(
    val key: TreeNodeKey,
    val depth: Int = 0,
    val showChildren: State<Boolean?> = mutableStateOf(false),
    val lastChild: Boolean = false,
    val permissions: PermissionMap,
    val value: ReferenceFollowingNodePayloadState,
    val flow: Lazy<Flow<TreeNodeState>>
) {
    val underlyingNodeId
        get() = value.underlyingState.node.nodeId

    val underlyingNodeKind
        get() = value.underlyingState.node.kind

    val underlyingNodeParentId
        get() = value.underlyingState.node.parentId
}

@OptIn(ExperimentalCoroutinesApi::class)
class TreeStateHolder(
    private val db: AppDatabase,
    rootNodeId: Int = ROOT_NODE_ID,
    coroutineScope: CoroutineScope,
) {
    private val treeNodeStateFlows = mutableStateMapOf<TreeNodeKey, Flow<TreeNodeState>>()
    private val showChildren = mutableStateMapOf<TreeNodeKey, Boolean>()
    private val ensureKeyIsShownFlow = MutableStateFlow<TreeNodeKey?>(null)

    private val _stateFlow = MutableStateFlow(TreeState())
    val stateFlow = _stateFlow.asStateFlow()

    fun onActivateNode(state: TreeNodeState) {
        if (state.key in showChildren) showChildren[state.key] = !showChildren[state.key]!!
    }

    fun onSelectNode(key: TreeNodeKey) =
        _stateFlow.update { treeState ->
            treeState.expectMode(TreeState.Mode.Normal)
            treeState.copy(normalState = treeState.normalState.copy(selectedKey = key))
        }

    fun onClearSelectedNode() =
        _stateFlow.update { treeState ->
            treeState.copy(normalState = treeState.normalState.copy(selectedKey = null))
        }

    fun onBeginBatchSelect() =
        _stateFlow.value.normalState.selectedKey.let { key ->
            _stateFlow.update { treeState ->
                key ?: throw Exception("selectedKey is null")
                treeState
                    .changeMode(from = TreeState.Mode.Normal, to = TreeState.Mode.Batch)
                    .copy(
                        batchState =
                            TreeState.BatchState(
                                parentKey = key.parentKey(),
                                selectedKeys = mapOf(key to true),
                            )
                    )
            }
        }

    fun onEndBatchSelect() =
        _stateFlow.update { it.changeMode(from = TreeState.Mode.Batch, to = TreeState.Mode.Normal) }

    fun onToggleNodeBatchSelected(key: TreeNodeKey) =
        _stateFlow.update { treeState ->
            val batchState = treeState.modeState<TreeState.BatchState>()
            val nextSelectedKeys =
                batchState.selectedKeys.toMutableMap().also {
                    it[key] = !it.computeIfAbsent(key) { false }
                }
            treeState.copy(batchState = batchState.copy(selectedKeys = nextSelectedKeys))
        }

    fun onBatchSelectAll() =
        _stateFlow.update { treeState ->
            treeState.copy(
                batchState =
                    treeState
                        .modeState<TreeState.BatchState>()
                        .copy(
                            selectedKeys =
                                listFlow.value
                                    .filter {
                                        TreeState.Mode.Batch.relevance(treeState, it) ==
                                            NodeRelevance.Relevant
                                    }
                                    .map { it.key }
                                    .associateWith { true }
                        )
            )
        }

    fun onBatchDeselectAll() =
        _stateFlow.update { treeState ->
            treeState.copy(
                batchState =
                    treeState.modeState<TreeState.BatchState>().copy(selectedKeys = emptyMap())
            )
        }

    fun onBeginBatchMove() =
        _stateFlow.update { treeState ->
            treeState.changeMode(TreeState.Mode.Batch, TreeState.Mode.Move)
        }

    fun onEndBatchMove() =
        _stateFlow.update { treeState ->
            treeState.changeMode(TreeState.Mode.Move, TreeState.Mode.Batch)
        }

    private fun ensureKeyIsShown(targetKey: TreeNodeKey) {
        var key = TreeNodeKey.emptyKey()
        targetKey.nodeLineage.forEachIndexed { index, nodeId ->
            if (index == targetKey.nodeLineage.lastIndex) return
            key = key.childKey(nodeId)
            showChildren[key] = true
            if (index == targetKey.nodeLineage.lastIndex - 1) ensureKeyIsShownFlow.update { key }
        }
    }

    fun ensureNodeIsShown(nodeId: Int) {
        CoroutineScope(Dispatchers.IO).launch {
            val nodeLineage = db.nodeLineage(nodeId)
            val key = TreeNodeKey(nodeLineage.map { it.nodeId })
            ensureKeyIsShown(key)
        }
    }

    val listFlow: StateFlow<List<TreeNodeState>> =
        flow {
                fun traverse(
                    key: TreeNodeKey,
                    node: Node,
                    depth: Int = -1,
                    lastChild: Boolean = false,
                    permissions: PermissionMap = AllPermissions.clone(),
                ): Flow<List<TreeNodeState>> {
                    val parentFlow = getTreeNodeStateFlow(key, depth, node, lastChild, permissions)

                    return if (!canHaveChildren(node)) parentFlow.mapLatest { listOf(it) }
                    else {
                        val childrenFlow =
                            parentFlow
                                .distinctUntilChangedBy { it.value.node.nodeId }
                                .combine(ensureKeyIsShownFlow) { treeNode, _ -> treeNode }
                                .transformLatest { treeNode: TreeNodeState ->
                                    db.nodeDao()
                                        .getChildNodesFlow(treeNode.value.node.nodeId)
                                        .distinctUntilChangedBy { it.map { node -> node.nodeId } }
                                        .flatMapLatest { childNodes ->
                                            if (childNodes.isEmpty()) flowOf(emptyList())
                                            else {
                                                val childNodeFlows =
                                                    childNodes.mapIndexed { index, childNode ->
                                                        traverse(
                                                            key = key.childKey(childNode.nodeId),
                                                            node = childNode,
                                                            depth = depth + 1,
                                                            lastChild =
                                                                index == childNodes.lastIndex,
                                                            permissions =
                                                                permissions.adjustChildPermissions(
                                                                    payload = treeNode.value.payload
                                                                )
                                                        )
                                                    }
                                                combine(childNodeFlows) { arrayOfLists ->
                                                    arrayOfLists.toList().flatten()
                                                }
                                            }
                                        }
                                        .also { emitAll(it) }
                                }
                                .distinctUntilChangedBy {
                                    it.map { state -> state.underlyingNodeId }
                                }

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
                traverse(key = TreeNodeKey.rootKey(rootNodeId), node = rootNode)
                    .onEach(::ensureSpecialDirectoryValidity)
                    .also { emitAll(it) }
            }
            .stateIn(coroutineScope, SharingStarted.WhileSubscribed(), emptyList())

    private suspend fun ensureSpecialDirectoryValidity(treeNodeStateList: List<TreeNodeState>) {
        treeNodeStateList.forEach { treeNodeState ->
            treeNodeState.value.underlyingState.payload.castOrNull<Directory>()?.specialMode?.let {
                specialMode ->
                val children =
                    db.nodeDao().getChildNodes(treeNodeState.underlyingNodeId).map { node ->
                        val payload = db.getPayloadByNodeId(node.kind, node.nodeId)
                        NodePayloadState(node, payload.notNull(node))
                    }
                val validChildren =
                    specialMode.isChildValid?.let { isChildValid ->
                        children
                            .associateWith { child -> isChildValid(child) }
                            .run {
                                forEach { (nodePayload, valid) ->
                                    if (!valid) db.deleteRecursively(nodePayload.node)
                                }
                                filter { (_, valid) -> valid }.map { (nodePayload) -> nodePayload }
                            }
                    } ?: children
                val valid = specialMode.isValid?.invoke(validChildren) ?: true

                if (!valid) db.deleteRecursively(treeNodeState.value.underlyingState.node)
            }
        }
    }

    private fun getTreeNodeStateFlow(
        key: TreeNodeKey,
        depth: Int,
        node: Node,
        lastChild: Boolean,
        parentPermissions: PermissionMap
    ): Flow<TreeNodeState> =
        treeNodeStateFlows.computeIfAbsent(key) {
            val parentStateHolder = NodePayloadStateHolder(db, node)

            parentStateHolder.flowWithReferenceTarget.mapLatest { value ->
                val showChildren = derivedStateOf { showChildren[key] }
                val permissions = parentPermissions.adjustOwnPermissions(value.payload)
                val flow = lazy { // TODO reuse current flow somehow
                    getTreeNodeStateFlow(key, depth, node, lastChild, parentPermissions)
                }

                TreeNodeState(key, depth, showChildren, lastChild, permissions, value, flow)
            }
        }
}

private fun canHaveChildren(node: Node) =
    node.kind == NodeKind.Directory || node.kind == NodeKind.Reference
