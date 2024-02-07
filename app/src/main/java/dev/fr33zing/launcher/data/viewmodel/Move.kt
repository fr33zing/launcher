package dev.fr33zing.launcher.data.viewmodel

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.fr33zing.launcher.data.NodeKind
import dev.fr33zing.launcher.data.PermissionKind
import dev.fr33zing.launcher.data.PermissionScope
import dev.fr33zing.launcher.data.persistent.AppDatabase
import dev.fr33zing.launcher.data.persistent.Node
import dev.fr33zing.launcher.data.persistent.ROOT_NODE_ID
import dev.fr33zing.launcher.data.persistent.checkPermission
import dev.fr33zing.launcher.data.persistent.moveNode
import dev.fr33zing.launcher.data.persistent.nodeLineage
import dev.fr33zing.launcher.data.utility.notNull
import dev.fr33zing.launcher.data.utility.unreachable
import dev.fr33zing.launcher.data.viewmodel.state.TreeBrowserStateHolder
import dev.fr33zing.launcher.nodeId
import dev.fr33zing.launcher.ui.components.sendNotice
import javax.inject.Inject
import kotlinx.coroutines.launch

@HiltViewModel
class MoveViewModel
@Inject
constructor(private val db: AppDatabase, savedStateHandle: SavedStateHandle) : ViewModel() {
    private val nodeToMoveId = savedStateHandle.nodeId()
    var nodeToMove by mutableStateOf<Node?>(null)
    val nodeToMoveLineage = mutableStateListOf<Node>()

    val treeBrowser =
        TreeBrowserStateHolder(
            db = db,
            scope = viewModelScope,
            traverseDirectories = false,
            containTraversalWithinInitialRoot = false,
            nodeVisiblePredicate = { (node) ->
                node.kind == NodeKind.Directory && node.nodeId != nodeToMoveId
            },
            initialRootNode = {
                setMovingNode() // HACK: setMovingNode should probably be called elsewhere
                db.nodeDao().getNodeById(nodeToMove?.parentId ?: ROOT_NODE_ID)
                    ?: throw Exception("parent is null")
            },
            onNodeSelected = { state ->
                val (node) = state
                if (node.kind != NodeKind.Directory)
                    unreachable { "non-directory nodes should be filtered by nodeVisiblePredicate" }

                viewModelScope.launch {
                    if (hasPermissions(node)) traverseInward(state)
                    else {
                        sendNotice(
                            "move-blocked",
                            "Cannot move into directory '${node.label}' due to insufficient permissions."
                        )
                    }
                }
            }
        )

    fun commitMove() {
        viewModelScope.launch {
            val newParentId = treeBrowser.currentRootNodeId!!
            db.moveNode(nodeToMove.notNull(), newParentId)
        }
    }

    private suspend fun setMovingNode() {
        if (nodeToMove != null) throw Exception("movingNode.value is not null")
        if (nodeToMoveLineage.isNotEmpty()) throw Exception("movingNodeLineage.value is not empty")

        nodeToMove = db.nodeDao().getNodeById(nodeToMoveId).notNull()
        nodeToMoveLineage.addAll(db.nodeLineage(nodeToMove.notNull()))
    }

    private suspend fun hasPermissions(node: Node) =
        db.checkPermission(PermissionKind.Move, PermissionScope.Recursive, node) &&
            db.checkPermission(PermissionKind.MoveIn, PermissionScope.Recursive, node)
}
