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
import dev.fr33zing.launcher.data.persistent.moveToTrash
import dev.fr33zing.launcher.data.persistent.nodeLineage
import dev.fr33zing.launcher.data.utility.notNull
import dev.fr33zing.launcher.data.viewmodel.state.TreeNodeKey
import dev.fr33zing.launcher.data.viewmodel.state.TreeNodeState
import dev.fr33zing.launcher.data.viewmodel.state.TreeState
import dev.fr33zing.launcher.data.viewmodel.state.TreeStateHolder
import dev.fr33zing.launcher.data.utility.stagger
import javax.inject.Inject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.single
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@HiltViewModel
class TreeViewModel
@Inject
constructor(private val db: AppDatabase, savedStateHandle: SavedStateHandle) : ViewModel() {
    private val nodeId =
        savedStateHandle.get<String>("nodeId")?.toInt()
            ?: throw Exception("Invalid SavedStateHandle value for key: nodeId")
    private val stateHolder = TreeStateHolder(db, nodeId)
    private var shouldStaggerFlow = mutableStateOf(true)

    private val _scrollToKeyFlow = MutableStateFlow<TreeNodeKey?>(null)
    val scrollToKeyFlow =
        _scrollToKeyFlow.stateIn(viewModelScope, SharingStarted.WhileSubscribed(), null)

    val treeNodeListFlow =
        stateHolder.listFlow
            .stagger(shouldStaggerFlow)
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(), emptyList())

    val treeStateFlow =
        stateHolder.stateFlow.stateIn(viewModelScope, SharingStarted.WhileSubscribed(), TreeState())

    fun onScrolledToKey() = _scrollToKeyFlow.update { null }

    fun activatePayload(context: Context, treeNodeState: TreeNodeState) {
        stateHolder.onActivateNode(treeNodeState)
        treeNodeState.value.payload.activate(db, context)
    }

    fun selectNode(key: TreeNodeKey) {
        stateHolder.onSelectNode(key)
    }

    fun clearSelectedNode() {
        stateHolder.onClearSelectedNode()
    }

    fun createNode(position: RelativeNodePosition, kind: NodeKind, callback: (Int) -> Unit) {
        CoroutineScope(Dispatchers.Main).launch {
            flow {
                    val nodeId = db.createNode(position, kind)
                    val lineage = db.nodeLineage(nodeId)
                    val key = TreeNodeKey(lineage.map { it.nodeId })
                    _scrollToKeyFlow.update { key }
                    emit(nodeId)
                }
                .flowOn(Dispatchers.IO)
                .single()
                .let(callback)
        }
    }

    fun moveNodeToTrash(nodeId: Int) {
        CoroutineScope(Dispatchers.IO).launch {
            db.nodeDao().getNodeById(nodeId).notNull().let { db.moveToTrash(it) }
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
}
