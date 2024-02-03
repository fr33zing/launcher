package dev.fr33zing.launcher.data.viewmodel.editform

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.fr33zing.launcher.data.persistent.AppDatabase
import dev.fr33zing.launcher.data.persistent.Node
import dev.fr33zing.launcher.data.persistent.ROOT_NODE_ID
import dev.fr33zing.launcher.data.persistent.nodeLineage
import dev.fr33zing.launcher.data.utility.notNull
import dev.fr33zing.launcher.data.viewmodel.utility.TreeBrowserStateHolder
import javax.inject.Inject
import kotlinx.coroutines.launch

@HiltViewModel
class EditReferenceViewModel
@Inject
constructor(private val db: AppDatabase, private val savedStateHandle: SavedStateHandle) :
    ViewModel() {
    var selectedNode by mutableStateOf<Node?>(null)
    var selectedNodePath by mutableStateOf<List<Node>>(emptyList())

    val nodeId: Int
        get() = savedStateHandle.get<String>("nodeId")?.toInt() ?: throw Exception("Invalid nodeId")

    val treeBrowser =
        TreeBrowserStateHolder(
            db = db,
            scope = viewModelScope,
            traverseDirectories = true,
            containTraversalWithinInitialRoot = false,
            nodeVisiblePredicate = { it.underlyingState.node.nodeId != nodeId },
            initialRootNode = ::getInitialRootNode,
            onNodeSelected = { state -> viewModelScope.launch { onNodeSelected(state.node) } },
            onTraverse = { node ->
                if (node.nodeId != ROOT_NODE_ID) viewModelScope.launch { onNodeSelected(node) }
            }
        )

    private suspend fun getInitialRootNode() = db.nodeDao().getParentByChildId(nodeId).notNull()

    private suspend fun onNodeSelected(node: Node) {
        selectedNode = node
        selectedNodePath = db.nodeLineage(node)
    }

    init {
        viewModelScope.launch { onNodeSelected(getInitialRootNode()) }
    }
}
