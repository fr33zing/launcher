package dev.fr33zing.launcher.data.viewmodel

import android.content.Context
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.fr33zing.launcher.data.NodeKind
import dev.fr33zing.launcher.data.persistent.AppDatabase
import dev.fr33zing.launcher.data.persistent.RelativeNodePosition
import dev.fr33zing.launcher.data.persistent.createNode
import dev.fr33zing.launcher.data.persistent.deleteRecursively
import dev.fr33zing.launcher.data.persistent.moveNodeToTrash
import dev.fr33zing.launcher.data.persistent.nodeLineage
import dev.fr33zing.launcher.data.utility.notNull
import dev.fr33zing.launcher.data.utility.stagger
import dev.fr33zing.launcher.data.viewmodel.state.TreeNodeKey
import dev.fr33zing.launcher.data.viewmodel.state.TreeNodeState
import dev.fr33zing.launcher.data.viewmodel.state.TreeState
import dev.fr33zing.launcher.data.viewmodel.state.TreeStateHolder
import dev.fr33zing.launcher.nodeId
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.single
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

private data class JumpToNodeEvent(
    val nodeId: Int,
    val snap: Boolean,
    val highlight: Boolean,
    val afterNextUpdate: Boolean,
)

private data class JumpToKey(
    val key: TreeNodeKey,
    val snap: Boolean,
    val highlight: Boolean,
    val afterNextUpdate: Boolean,
)

data class ScrollToKeyEvent(
    val key: TreeNodeKey,
    val snap: Boolean,
)

private val jumpToNodeFlow = MutableSharedFlow<JumpToNodeEvent?>(1)

fun sendJumpToNode(
    nodeId: Int,
    snap: Boolean = true,
    highlight: Boolean = true,
    afterNextUpdate: Boolean = false,
) = jumpToNodeFlow.tryEmit(null) &&
    jumpToNodeFlow.tryEmit(JumpToNodeEvent(nodeId, snap, highlight, afterNextUpdate))

@HiltViewModel
class TreeViewModel
    @Inject
    constructor(
        private val db: AppDatabase,
        savedStateHandle: SavedStateHandle,
    ) : ViewModel() {
        private val rootNodeId = savedStateHandle.nodeId()
        private var queuedScrollToKeyEvent: ScrollToKeyEvent? = null

        // HACK: this controls showing the node, scrolling to it, and highlighting it
        val highlightKeyFlow: StateFlow<TreeNodeKey?> =
            jumpToNodeFlow
                .onEach { it?.let { (nodeId) -> stateHolder.ensureNodeIsShown(nodeId) } }
                .map {
                    it?.let { (nodeId, snap, highlight, afterNextUpdate) ->
                        val lineage = db.nodeLineage(nodeId)
                        val key = TreeNodeKey(lineage.map { node -> node.nodeId })
                        JumpToKey(key, snap, highlight, afterNextUpdate)
                    }
                }
                .onEach {
                    it?.let { (key, snap, _, afterNextUpdate) ->
                        val event = ScrollToKeyEvent(key, snap)
                        if (!afterNextUpdate) scrollToKeyCallback(event) else queuedScrollToKeyEvent = event
                    }
                }
                .map { if (it?.highlight == true) it.key else null }
                .stateIn(viewModelScope, SharingStarted.WhileSubscribed(), null)

        private val stateHolder = TreeStateHolder(db, rootNodeId, viewModelScope)
        private var shouldStaggerFlow = mutableStateOf(true)

        private val _scrollToKeyFlow = MutableStateFlow<ScrollToKeyEvent?>(null)
        val scrollToKeyFlow =
            _scrollToKeyFlow.stateIn(viewModelScope, SharingStarted.WhileSubscribed(), null)

        val treeNodeListFlow =
            stateHolder.listFlow
                .stagger(shouldStaggerFlow)
                .stateIn(viewModelScope, SharingStarted.WhileSubscribed(), emptyList())

        val treeStateFlow =
            stateHolder.stateFlow.stateIn(viewModelScope, SharingStarted.WhileSubscribed(), TreeState())

        var scrollToKeyCallback: (ScrollToKeyEvent) -> Unit = {}

        fun performQueuedScrollToKey() {
            queuedScrollToKeyEvent?.let {
                queuedScrollToKeyEvent = null
                scrollToKeyCallback(it)
            }
        }

        fun activatePayload(
            context: Context,
            treeNodeState: TreeNodeState,
        ) {
            stateHolder.onActivateNode(treeNodeState)
            treeNodeState.value.payload.activate(db, context)
        }

        fun selectNode(key: TreeNodeKey) = stateHolder.onSelectNode(key)

        fun clearSelectedNode() = stateHolder.onClearSelectedNode()

        fun beginBatchSelect() = stateHolder.onBeginBatchSelect()

        fun endBatchSelect() = stateHolder.onEndBatchSelect()

        fun batchSelectAll() = stateHolder.onBatchSelectAll()

        fun batchDeselectAll() = stateHolder.onBatchDeselectAll()

        fun toggleNodeBatchSelected(key: TreeNodeKey) = stateHolder.onToggleNodeBatchSelected(key)

        fun beginBatchMove() = stateHolder.onBeginBatchMove()

        fun endBatchMove() = stateHolder.onEndBatchMove()

        fun moveBatchSelectedNodes(newParent: TreeNodeState) = stateHolder.onMoveBatchSelectedNodes(newParent)

        fun createNode(
            position: RelativeNodePosition,
            kind: NodeKind,
            callback: (Int) -> Unit,
        ) {
            CoroutineScope(Dispatchers.Main).launch {
                flow {
                    val nodeId = db.createNode(position, kind)
                    emit(nodeId)
                }
                    .flowOn(Dispatchers.IO)
                    .single()
                    .let(callback)
            }
        }

        fun moveNodeToTrash(nodeId: Int) {
            CoroutineScope(Dispatchers.IO).launch {
                db.nodeDao().getNodeById(nodeId).notNull().let { db.moveNodeToTrash(it) }
            }
        }

        fun deleteNode(nodeId: Int) {
            CoroutineScope(Dispatchers.IO).launch {
                db.nodeDao().getNodeById(nodeId).notNull().let { db.deleteRecursively(it) }
            }
        }

        fun disableFlowStagger() {
            shouldStaggerFlow.value = false
        }

        fun clearHighlightedNode() {
            viewModelScope.launch { jumpToNodeFlow.emit(null) }
        }
    }
