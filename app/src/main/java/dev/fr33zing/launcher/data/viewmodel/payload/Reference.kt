package dev.fr33zing.launcher.data.viewmodel.payload

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.fr33zing.launcher.data.NodeKind
import dev.fr33zing.launcher.data.persistent.AppDatabase
import dev.fr33zing.launcher.data.persistent.Node
import dev.fr33zing.launcher.data.persistent.ROOT_NODE_ID
import dev.fr33zing.launcher.data.persistent.nodeLineage
import dev.fr33zing.launcher.data.persistent.payloads.Reference
import dev.fr33zing.launcher.data.utility.cast
import dev.fr33zing.launcher.data.utility.notNull
import dev.fr33zing.launcher.data.viewmodel.state.TreeBrowserStateHolder
import dev.fr33zing.launcher.nodeId
import javax.inject.Inject
import kotlinx.coroutines.launch

@HiltViewModel
class EditReferenceViewModel
@Inject
constructor(private val db: AppDatabase, savedStateHandle: SavedStateHandle) : ViewModel() {
    var selectedNode by mutableStateOf<Node?>(null)
    var selectedNodePath by mutableStateOf<List<Node>>(emptyList())
    var cyclic by mutableStateOf(true)
    var nodeSelectedCallback: () -> Unit = {}

    private val editingNodeId = savedStateHandle.nodeId()

    val treeBrowser =
        TreeBrowserStateHolder(
            db = db,
            scope = viewModelScope,
            traverseDirectories = true,
            containTraversalWithinInitialRoot = false,
            nodeVisiblePredicate = { it.underlyingState.node.nodeId != editingNodeId },
            initialRootNode = ::getInitialRootNode,
            onNodeSelected = { state -> viewModelScope.launch { onNodeSelected(state.node) } },
            onTraverse = { node ->
                if (node.nodeId != ROOT_NODE_ID) viewModelScope.launch { onNodeSelected(node) }
            }
        )

    init {
        viewModelScope.launch { onNodeSelected(getInitialRootNode()) }
    }

    private suspend fun getInitialRootNode(): Node {
        val node = db.nodeDao().getNodeById(editingNodeId).notNull()
        val payload = db.getPayloadByNodeId(node.kind, node.nodeId).notNull(node).cast<Reference>()
        val targetId = payload.targetId ?: node.parentId ?: ROOT_NODE_ID
        val target = db.nodeDao().getNodeById(targetId).notNull()

        return if (target.kind == NodeKind.Directory) target
        else db.nodeDao().getNodeById(target.parentId ?: ROOT_NODE_ID).notNull()
    }

    private suspend fun onNodeSelected(node: Node) {
        selectedNode = node
        selectedNodePath = db.nodeLineage(node)
        cyclic = detectCycle()
        nodeSelectedCallback()
    }

    private suspend fun detectCycle(): Boolean {
        suspend fun isCyclic(node: Node): Boolean {
            var cursor = node

            if (node.nodeId == editingNodeId) return true
            if (node.kind == NodeKind.Reference)
                db.getPayloadByNodeId(node.kind, node.nodeId)
                    .notNull(node)
                    .cast<Reference>()
                    .targetId
                    ?.let { cursor = db.nodeDao().getNodeById(it).notNull() }
            if (cursor.kind == NodeKind.Directory)
                db.nodeDao().getChildNodes(cursor.nodeId).forEach { if (isCyclic(it)) return true }

            return false
        }

        return selectedNode?.let { isCyclic(it) } ?: false
    }
}
